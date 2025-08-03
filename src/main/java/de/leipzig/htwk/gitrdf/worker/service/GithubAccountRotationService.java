package de.leipzig.htwk.gitrdf.worker.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubAccountRotationService {
    
    private final GithubConfig githubConfig;
    private final AtomicInteger currentAccountNumber = new AtomicInteger(-1);
    private final Map<Integer, Instant> accountRateLimitResetTimes = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> accountAvailability = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> accountValidCredentials = new ConcurrentHashMap<>();
    
    // Processing statistics
    private final AtomicInteger processedIssuesCount = new AtomicInteger(0);
    private final AtomicInteger processedCommitsCount = new AtomicInteger(0);
    private volatile int maxIssuesLimit = 0;
    private volatile int maxCommitsLimit = 0;
    
    // Actual repository totals
    private volatile int actualTotalIssues = 0;
    private volatile int actualTotalCommits = 0;
    
    public GithubAccountRotationService(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
        // Initialize all accounts as available and with valid credentials
        for (GithubConfig.GithubApiAccount account : githubConfig.getGithubApiAccounts()) {
            accountAvailability.put(account.getAccountNumber(), true);
            accountValidCredentials.put(account.getAccountNumber(), true);
        }
    }
    
    /**
     * Get the current active GitHub API account
     */
    public GithubConfig.GithubApiAccount getCurrentAccount() {
        int accountNumber = getCurrentAvailableAccountNumber();
        return findAccountByNumber(accountNumber);
    }
    
    /**
     * Find account by account number
     */
    private GithubConfig.GithubApiAccount findAccountByNumber(int accountNumber) {
        return githubConfig.getGithubApiAccounts().stream()
                .filter(account -> account.getAccountNumber() == accountNumber)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account number " + accountNumber + " not found"));
    }
    
    /**
     * Mark an account as rate limited and rotate to the next available account
     */
    public void markAccountRateLimited(int accountIndex, Instant resetTime) {
        log.warn("Account {} hit rate limit, will reset at {}", accountIndex, resetTime);
        accountRateLimitResetTimes.put(accountIndex, resetTime);
        accountAvailability.put(accountIndex, false);
        
        // Force rotation to next available account
        rotateToNextAvailableAccount();
    }
    
    /**
     * Get the current available account number, rotating through accounts
     */
    private synchronized int getCurrentAvailableAccountNumber() {
        int totalAccounts = githubConfig.getGithubApiAccounts().size();
        
        if (totalAccounts == 1) {
            GithubConfig.GithubApiAccount singleAccount = githubConfig.getGithubApiAccounts().get(0);
            checkAndResetAccountAvailability(singleAccount.getAccountNumber());
            return singleAccount.getAccountNumber();
        }
        
        int currentNumber = currentAccountNumber.get();
        
        // Initialize to first account if not set
        if (currentNumber == -1) {
            currentNumber = githubConfig.getGithubApiAccounts().get(0).getAccountNumber();
            currentAccountNumber.set(currentNumber);
        }
        
        // Check if current account is available
        if (isAccountAvailable(currentNumber)) {
            return currentNumber;
        }
        
        // Current account not available, find next available
        return rotateToNextAvailableAccount();
    }
    
    /**
     * Rotate to the next available account
     */
    private synchronized int rotateToNextAvailableAccount() {
        List<GithubConfig.GithubApiAccount> accounts = githubConfig.getGithubApiAccounts();
        int currentNumber = currentAccountNumber.get();
        
        // Find current account index in the list
        int currentIndex = -1;
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).getAccountNumber() == currentNumber) {
                currentIndex = i;
                break;
            }
        }
        
        // If current account not found, start from beginning
        if (currentIndex == -1) {
            currentIndex = 0;
        }
        
        // Try to find next available account
        for (int i = 0; i < accounts.size(); i++) {
            int candidateIndex = (currentIndex + i + 1) % accounts.size();
            GithubConfig.GithubApiAccount candidateAccount = accounts.get(candidateIndex);
            int candidateNumber = candidateAccount.getAccountNumber();
            
            if (isAccountAvailable(candidateNumber)) {
                currentAccountNumber.set(candidateNumber);
                log.info("Rotated to GitHub API account {}", candidateNumber);
                return candidateNumber;
            }
        }
        
        // No accounts available, check if any rate limits have expired
        for (GithubConfig.GithubApiAccount account : accounts) {
            checkAndResetAccountAvailability(account.getAccountNumber());
        }
        
        // Try again after checking rate limit resets
        for (int i = 0; i < accounts.size(); i++) {
            int candidateIndex = (currentIndex + i + 1) % accounts.size();
            GithubConfig.GithubApiAccount candidateAccount = accounts.get(candidateIndex);
            int candidateNumber = candidateAccount.getAccountNumber();
            
            if (isAccountAvailable(candidateNumber)) {
                currentAccountNumber.set(candidateNumber);
                log.info("Rotated to GitHub API account {} after rate limit reset", candidateNumber);
                return candidateNumber;
            }
        }
        
        // If still no accounts available, use the first one and log warning
        GithubConfig.GithubApiAccount firstAccount = accounts.get(0);
        int firstAccountNumber = firstAccount.getAccountNumber();
        log.warn("All GitHub API accounts are rate limited, using account {} anyway", firstAccountNumber);
        checkAndResetAccountAvailability(firstAccountNumber);
        currentAccountNumber.set(firstAccountNumber);
        return firstAccountNumber;
    }
    
    /**
     * Check if an account is available (not rate limited and has valid credentials)
     */
    private boolean isAccountAvailable(int accountNumber) {
        // Check if credentials are valid first
        if (!accountValidCredentials.getOrDefault(accountNumber, true)) {
            return false;
        }
        
        if (!accountAvailability.getOrDefault(accountNumber, true)) {
            checkAndResetAccountAvailability(accountNumber);
        }
        return accountAvailability.getOrDefault(accountNumber, true);
    }
    
    /**
     * Check if rate limit has expired and reset account availability
     */
    private void checkAndResetAccountAvailability(int accountNumber) {
        Instant resetTime = accountRateLimitResetTimes.get(accountNumber);
        if (resetTime != null && Instant.now().isAfter(resetTime)) {
            log.info("Rate limit reset for GitHub API account {}", accountNumber);
            accountAvailability.put(accountNumber, true);
            accountRateLimitResetTimes.remove(accountNumber);
        }
    }
    
    /**
     * Get the account number for the current account (for logging purposes)
     */
    public int getCurrentAccountNumber() {
        return getCurrentAccount().getAccountNumber();
    }

    /**
     * Get the earliest rate limit reset time among all rate-limited accounts.
     * Returns null if no accounts are rate limited.
     */
    public Instant getEarliestRateLimitResetTime() {
        return accountRateLimitResetTimes.values().stream()
            .min(Comparator.naturalOrder())
            .orElse(null);
    }
    
    /**
     * Get total number of configured accounts
     */
    public int getTotalAccountsConfigured() {
        return githubConfig.getGithubApiAccounts().size();
    }
    
    /**
     * Check how many accounts are currently available
     */
    public long getAvailableAccountsCount() {
        // Check and reset expired rate limits first
        for (GithubConfig.GithubApiAccount account : githubConfig.getGithubApiAccounts()) {
            checkAndResetAccountAvailability(account.getAccountNumber());
        }
        
        return githubConfig.getGithubApiAccounts().size() - 
               accountAvailability.values().stream().mapToLong(available -> available ? 0 : 1).sum();
    }
    
    /**
     * Mark an account as having invalid credentials (JWT/auth failures)
     */
    public void markAccountInvalidCredentials(int accountNumber) {
        log.error("Account {} has invalid credentials (JWT/auth failure), marking as unavailable", accountNumber);
        accountValidCredentials.put(accountNumber, false);
        
        // Force rotation to next available account
        rotateToNextAvailableAccount();
    }
    
    /**
     * Check how many accounts have valid credentials
     */
    public long getAccountsWithValidCredentialsCount() {
        return accountValidCredentials.values().stream()
                .mapToLong(valid -> valid ? 1 : 0)
                .sum();
    }
    
    /**
     * Set the maximum limits for processing
     */
    public void setProcessingLimits(int maxIssues, int maxCommits) {
        this.maxIssuesLimit = maxIssues;
        this.maxCommitsLimit = maxCommits;
    }
    
    /**
     * Set the actual repository totals from GitHub API
     */
    public void setRepositoryTotals(int totalIssues, int totalCommits) {
        this.actualTotalIssues = totalIssues;
        this.actualTotalCommits = totalCommits;
        
        log.info("Repository totals updated: {} issues, {} commits", totalIssues, totalCommits);
    }
    
    /**
     * Update the count of processed issues
     */
    public void updateProcessedIssuesCount(int count) {
        this.processedIssuesCount.set(count);
    }
    
    /**
     * Update the count of processed commits
     */
    public void updateProcessedCommitsCount(int count) {
        this.processedCommitsCount.set(count);
    }
    
    /**
     * Get current processing statistics for logging when rate limit is hit
     */
    public String getProcessingStatistics() {
        int currentIssues = processedIssuesCount.get();
        int currentCommits = processedCommitsCount.get();
        
        // Use actual repository totals if available, otherwise fall back to processing limits
        int totalIssues = actualTotalIssues > 0 ? actualTotalIssues : maxIssuesLimit;
        int totalCommits = actualTotalCommits > 0 ? actualTotalCommits : maxCommitsLimit;
        
        int remainingIssues = Math.max(0, totalIssues - currentIssues);
        int remainingCommits = Math.max(0, totalCommits - currentCommits);
        
        // Indicate whether we're using actual totals or limits
        String issueSource = actualTotalIssues > 0 ? "total" : "limit";
        String commitSource = actualTotalCommits > 0 ? "total" : "limit";
        
        return String.format("Issues: %d processed, %d remaining (%s: %d). Commits: %d processed, %d remaining (%s: %d)", 
                currentIssues, remainingIssues, issueSource, totalIssues,
                currentCommits, remainingCommits, commitSource, totalCommits);
    }
}
