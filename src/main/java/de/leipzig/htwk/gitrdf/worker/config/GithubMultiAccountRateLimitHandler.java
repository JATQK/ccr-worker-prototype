package de.leipzig.htwk.gitrdf.worker.config;

import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHubRateLimitHandler;
import org.kohsuke.github.connector.GitHubConnectorResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
public class GithubMultiAccountRateLimitHandler extends GitHubRateLimitHandler {
    
    private final GithubAccountRotationService githubAccountRotationService;
    
    public GithubMultiAccountRateLimitHandler(GithubAccountRotationService githubAccountRotationService) {
        this.githubAccountRotationService = githubAccountRotationService;
    }

    @Override
    public void onError(GitHubConnectorResponse connectorResponse) throws IOException {
        
        int currentAccountNumber = githubAccountRotationService.getCurrentAccountNumber();
        String body = getBodyFrom(connectorResponse);
        
        log.warn("GitHub rate limit exceeded for account {}. Status code: {}. Response body: {}", 
                currentAccountNumber, connectorResponse.statusCode(), body);

        // Extract rate limit reset time from headers if available
        Instant resetTime = extractRateLimitResetTime(connectorResponse);
        if (resetTime == null) {
            // Default to 1 hour if we can't determine reset time
            resetTime = Instant.now().plusSeconds(3600);
        }
        
        // Mark current account as rate limited and rotate to next account
        githubAccountRotationService.markAccountRateLimited(currentAccountNumber, resetTime);
        
        long availableAccounts = githubAccountRotationService.getAvailableAccountsCount();
        if (availableAccounts > 0) {
            log.info("Rotated to next available GitHub API account. {} accounts still available.", availableAccounts);
            // Don't throw exception - let the request retry with the new account
            return;
        } else {
            // No accounts available, throw exception as before
            String errorMessage = String.format("All GitHub API accounts (%d total) are rate limited. " +
                    "Status code is '%d'. " +
                    "Http body is (hard cut after 256 characters): '%s'",
                    githubAccountRotationService.getTotalAccountsConfigured(),
                    connectorResponse.statusCode(),
                    body);
            
            throw new RuntimeException(errorMessage);
        }
    }
    
    private Instant extractRateLimitResetTime(GitHubConnectorResponse connectorResponse) {
        try {
            // Try to get the X-RateLimit-Reset header
            String rateLimitReset = connectorResponse.header("X-RateLimit-Reset");
            if (rateLimitReset != null && !rateLimitReset.isEmpty()) {
                long resetTimeSeconds = Long.parseLong(rateLimitReset);
                return Instant.ofEpochSecond(resetTimeSeconds);
            }
            
            // Try to get the Retry-After header (in seconds)
            String retryAfter = connectorResponse.header("Retry-After");
            if (retryAfter != null && !retryAfter.isEmpty()) {
                long retryAfterSeconds = Long.parseLong(retryAfter);
                return Instant.now().plusSeconds(retryAfterSeconds);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse rate limit reset time from response headers", e);
        }
        
        return null;
    }

    /**
     * Body is hard cut after 256 characters.
     */
    private String getBodyFrom(GitHubConnectorResponse connectorResponse) throws IOException {

        StringBuilder builder = new StringBuilder();

        int characterCounter = 0;
        int characterLimit = 256;

        try (Reader reader = new BufferedReader(
                new InputStreamReader(connectorResponse.bodyStream(), StandardCharsets.UTF_8))) {

            int c = 0;

            while ((c = reader.read()) != -1) {

                builder.append((char) c);

                characterCounter++;
                if (characterCounter >= characterLimit) {
                    break;
                }

            }

        }

        return builder.toString();

    }
}