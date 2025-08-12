package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.config.GithubMultiAccountRateLimitHandler;
import de.leipzig.htwk.gitrdf.worker.provider.GithubAppInstallationProvider;
import de.leipzig.htwk.gitrdf.worker.provider.GithubJwtTokenProvider;
import de.leipzig.htwk.gitrdf.worker.ratelimit.MultiAccountRateLimitChecker;
import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class GithubHandlerService {

    private final GithubConfig githubConfig;

    private final GithubJwtTokenProvider githubJwtTokenProvider;

    private final GithubAppInstallationProvider githubAppInstallationProvider;
    
    private final GithubAccountRotationService githubAccountRotationService;
    
    private final int maxCommitPages;

    public GithubHandlerService(
            GithubConfig githubConfig,
            GithubJwtTokenProvider githubJwtTokenProvider,
            GithubAppInstallationProvider githubAppInstallationProvider,
            GithubAccountRotationService githubAccountRotationService,
            @Value("${worker.max-commit-pages:500}") int maxCommitPages) {

        this.githubConfig = githubConfig;
        this.githubJwtTokenProvider = githubJwtTokenProvider;
        this.githubAppInstallationProvider = githubAppInstallationProvider;
        this.githubAccountRotationService = githubAccountRotationService;
        this.maxCommitPages = maxCommitPages;
    }

    private volatile GitHub currentGithubClient;
    private volatile int lastUsedAccountNumber = -1;

    public GitHub getGithub() throws IOException {
        int currentAccountNumber = githubAccountRotationService.getCurrentAccountNumber();
        
        // Create a new client if account has changed or no client exists
        if (currentGithubClient == null || lastUsedAccountNumber != currentAccountNumber) {
            synchronized (this) {
                // Double-check pattern
                if (currentGithubClient == null || lastUsedAccountNumber != currentAccountNumber) {
                    currentGithubClient = createGithubClient();
                    lastUsedAccountNumber = currentAccountNumber;
                    log.info("Created new GitHub client for account {}", currentAccountNumber);
                }
            }
        }
        
        return currentGithubClient;
    }
    
    private GitHub createGithubClient() throws IOException {
        AppInstallationAuthorizationProvider appInstallationAuthorizationProvider
                = new AppInstallationAuthorizationProvider(this.githubAppInstallationProvider, this.githubJwtTokenProvider);

        MultiAccountRateLimitChecker rateLimitChecker
                = new MultiAccountRateLimitChecker(githubConfig.getRateLimitRequestsLeftBorder(), githubAccountRotationService);

        return new GitHubBuilder()
                .withAuthorizationProvider(appInstallationAuthorizationProvider)
                .withRateLimitChecker(rateLimitChecker)
                .withRateLimitHandler(new GithubMultiAccountRateLimitHandler(githubAccountRotationService))
                .build();
    }

    /**
     * Get the total number of issues (including pull requests) in a repository
     * Uses GitHub search API for efficiency
     */
    public int getTotalIssuesCount(String owner, String repoName) throws IOException {
        GitHub github = getGithub();
        
        try {
            // Use GitHub search API which is more efficient for getting counts
            // Search for all issues in the repository (includes PRs)
            String query = "repo:" + owner + "/" + repoName + " is:issue";
            org.kohsuke.github.GHIssueSearchBuilder searchBuilder = github.searchIssues().q(query);
            
            // Get just the first page to access total count
            org.kohsuke.github.PagedSearchIterable<org.kohsuke.github.GHIssue> searchResults = searchBuilder.list();
            int totalCount = searchResults.getTotalCount();
            
            log.info("Repository {}/{} has {} total issues (via search API)", owner, repoName, totalCount);
            return totalCount;
            
        } catch (Exception e) {
            log.warn("Failed to get issue count via search API for {}/{}, falling back to repository metadata: {}", owner, repoName, e.getMessage());
            
            // Fallback to repository's open_issues count (which includes PRs but only open ones)
            GHRepository repo = github.getRepository(owner + "/" + repoName);
            int openCount = repo.getOpenIssueCount();
            log.info("Repository {}/{} has {} open issues (fallback method)", owner, repoName, openCount);
            return openCount;
        }
    }
    
    /**
     * Get the total number of commits in a repository using pagination headers
     * This is an expensive operation and should be used carefully
     */
    public int getTotalCommitsCount(String owner, String repoName) throws IOException {
        GitHub github = getGithub();
        GHRepository repo = github.getRepository(owner + "/" + repoName);
        
        try {
            // Get repository statistics which includes commit count per contributor
            log.info("Fetching contributor statistics for {}/{} to get commit count", owner, repoName);
            
            try {
                // Use contributor stats API to get total commits
                PagedIterable<org.kohsuke.github.GHRepositoryStatistics.ContributorStats> stats = repo.getStatistics().getContributorStats();
                
                int totalCommits = 0;
                for (org.kohsuke.github.GHRepositoryStatistics.ContributorStats contributorStats : stats) {
                    totalCommits += contributorStats.getTotal();
                }
                
                if (totalCommits > 0) {
                    log.info("Repository {}/{} has {} total commits (via contributor stats)", owner, repoName, totalCommits);
                    return totalCommits;
                }
            } catch (Exception statsException) {
                log.warn("Failed to get commit count via contributor stats for {}/{}: {}", owner, repoName, statsException.getMessage());
            }
            
            // Fallback: estimate based on commit pagination with configurable limits
            log.info("Falling back to pagination-based commit count estimation for {}/{} (max {} pages)", owner, repoName, maxCommitPages);
            PagedIterable<org.kohsuke.github.GHCommit> commits = repo.listCommits().withPageSize(100);
            
            // Calculate maximum commits to check based on configuration
            int maxCommitsToCount = maxCommitPages * 100; // 100 commits per page
            int estimatedCount = 0;
            int pagesChecked = 0;
            
            try {
                for (org.kohsuke.github.GHCommit ignored : commits) {
                    estimatedCount++;
                    
                    // Check if we've hit the page limit
                    if (estimatedCount % 100 == 0) {
                        pagesChecked++;
                        if (pagesChecked >= maxCommitPages) {
                            log.warn("Repository {}/{} has more than {} commits ({} pages), stopping count at configured limit", 
                                    owner, repoName, maxCommitsToCount, maxCommitPages);
                            break;
                        }
                    }
                    
                    // Safety check to prevent runaway counting
                    if (estimatedCount >= maxCommitsToCount) {
                        break;
                    }
                }
                
                if (estimatedCount >= maxCommitsToCount) {
                    log.warn("Repository {}/{} has {} or more commits, returning configured maximum", owner, repoName, maxCommitsToCount);
                    return maxCommitsToCount;
                }
                
            } catch (Exception paginationException) {
                log.error("Error during commit pagination for {}/{}: {}", owner, repoName, paginationException.getMessage());
                // Return what we counted so far
                if (estimatedCount == 0) {
                    estimatedCount = 1000; // Reasonable default
                }
            }
            
            log.info("Repository {}/{} has {} commits (estimated via pagination)", owner, repoName, estimatedCount);
            return estimatedCount;
            
        } catch (Exception e) {
            log.error("Failed to get commit count for {}/{}: {}", owner, repoName, e.getMessage(), e);
            
            // Check if it's a rate limit or API error that should cause the service to pause
            if (e.getMessage() != null && e.getMessage().contains("API rate limit exceeded")) {
                log.warn("GitHub API rate limit exceeded, service should pause processing");
                throw new IOException("GitHub API rate limit exceeded", e);
            }
            
            // For other errors, return a reasonable default to allow processing to continue
            log.warn("Returning default commit count of 1000 for {}/{} due to error", owner, repoName);
            return 1000; // Default estimate for repositories
        }
    }
    
    /**
     * Check if a repository should be skipped due to size constraints
     */
    public boolean shouldSkipRepository(String owner, String repoName, int commitCount, int issueCount) {
        int maxCommitsToCount = maxCommitPages * 100;
        
        if (commitCount > maxCommitsToCount) {
            log.warn("Skipping repository {}/{} - too many commits: {} (limit: {})", 
                    owner, repoName, commitCount, maxCommitsToCount);
            return true;
        }
        
        // Also check for repositories with excessive issues (optional additional check)
        int maxIssues = 50000; // Configurable limit for issues
        if (issueCount > maxIssues) {
            log.warn("Skipping repository {}/{} - too many issues: {} (limit: {})", 
                    owner, repoName, issueCount, maxIssues);
            return true;
        }
        
        return false;
    }

}
