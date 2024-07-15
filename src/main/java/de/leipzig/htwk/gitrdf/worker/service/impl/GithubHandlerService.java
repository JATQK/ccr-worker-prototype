package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.config.GithubRateLimitHandlerExceptionAdvice;
import de.leipzig.htwk.gitrdf.worker.provider.GithubAppInstallationProvider;
import de.leipzig.htwk.gitrdf.worker.provider.GithubJwtTokenProvider;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitChecker;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GithubHandlerService {

    private final GithubConfig githubConfig;

    private final GithubJwtTokenProvider githubJwtTokenProvider;

    private final GithubAppInstallationProvider githubAppInstallationProvider;

    public GithubHandlerService(
            GithubConfig githubConfig,
            GithubJwtTokenProvider githubJwtTokenProvider,
            GithubAppInstallationProvider githubAppInstallationProvider) {

        this.githubConfig = githubConfig;
        this.githubJwtTokenProvider = githubJwtTokenProvider;
        this.githubAppInstallationProvider = githubAppInstallationProvider;
    }

    public GitHub getGithub() throws IOException {

        AppInstallationAuthorizationProvider appInstallationAuthorizationProvider
                = new AppInstallationAuthorizationProvider(this.githubAppInstallationProvider, this.githubJwtTokenProvider);

        return new GitHubBuilder()
                .withAuthorizationProvider(appInstallationAuthorizationProvider)
                .withRateLimitChecker(new RateLimitChecker.LiteralValue(githubConfig.getRateLimitRequestsLeftBorder()))
                .withRateLimitHandler(new GithubRateLimitHandlerExceptionAdvice())
                .build();
    }

}
