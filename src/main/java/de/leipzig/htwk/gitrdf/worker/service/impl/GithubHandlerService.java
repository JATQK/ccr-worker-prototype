package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.config.GithubMultiAccountRateLimitHandler;
import de.leipzig.htwk.gitrdf.worker.provider.GithubAppInstallationProvider;
import de.leipzig.htwk.gitrdf.worker.provider.GithubJwtTokenProvider;
import de.leipzig.htwk.gitrdf.worker.ratelimit.MultiAccountRateLimitChecker;
import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class GithubHandlerService {

    private final GithubConfig githubConfig;

    private final GithubJwtTokenProvider githubJwtTokenProvider;

    private final GithubAppInstallationProvider githubAppInstallationProvider;
    
    private final GithubAccountRotationService githubAccountRotationService;

    public GithubHandlerService(
            GithubConfig githubConfig,
            GithubJwtTokenProvider githubJwtTokenProvider,
            GithubAppInstallationProvider githubAppInstallationProvider,
            GithubAccountRotationService githubAccountRotationService) {

        this.githubConfig = githubConfig;
        this.githubJwtTokenProvider = githubJwtTokenProvider;
        this.githubAppInstallationProvider = githubAppInstallationProvider;
        this.githubAccountRotationService = githubAccountRotationService;
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

}
