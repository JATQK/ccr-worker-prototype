package de.leipzig.htwk.gitrdf.worker.ratelimit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;

import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiAccountRateLimitChecker extends RateLimitChecker {

    private final int sleepAtOrBelow;
    private final GithubAccountRotationService githubAccountRotationService;

    public MultiAccountRateLimitChecker(int sleepAtOrBelow, GithubAccountRotationService githubAccountRotationService) {
        this.sleepAtOrBelow = sleepAtOrBelow;
        this.githubAccountRotationService = githubAccountRotationService;
    }

    @Override
    protected boolean checkRateLimit(GHRateLimit.Record record, long count) throws InterruptedException {
        if (record == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis() / 1000L;
        int remaining = record.getRemaining();
        long resetTime = record.getResetDate().getTime() / 1000L;

        String localResetTimeDebug = formatDateInLocalTimezone(record.getResetDate().toInstant());
        log.debug("GitHub API - Current quota has {} remaining of {}. Reset at {} (local time)", 
                remaining, record.getLimit(), localResetTimeDebug);

        // If we're at or below the threshold, try to switch accounts instead of sleeping
        if (remaining <= sleepAtOrBelow) {
            log.warn("GitHub API rate limit threshold reached. Remaining: {}, Threshold: {}. Processing progress: {}", 
                    remaining, sleepAtOrBelow, githubAccountRotationService.getProcessingStatistics());
            
            // Check if we have other accounts available
            long availableAccounts = githubAccountRotationService.getAvailableAccountsCount();
            int currentAccountNumber = githubAccountRotationService.getCurrentAccountNumber();
            
            if (availableAccounts > 1) {
                log.info("Marking current account {} as rate limited and rotating to next account", currentAccountNumber);
                
                // Mark current account as rate limited
                Instant resetInstant = Instant.ofEpochSecond(resetTime);
                githubAccountRotationService.markAccountRateLimited(currentAccountNumber, resetInstant);
                
                // Don't sleep - let the request retry with the new account
                return false;
            } else {
                // No other accounts available, fall back to sleeping until the earliest reset time
                log.warn("No other GitHub API accounts available. Falling back to waiting for earliest rate limit reset. Processing progress: {}", 
                        githubAccountRotationService.getProcessingStatistics());
                java.time.Instant earliestReset = githubAccountRotationService.getEarliestRateLimitResetTime();
                long sleepTime = 0;
                if (earliestReset != null) {
                    sleepTime = Math.max(0, earliestReset.getEpochSecond() - currentTime);
                }
                if (sleepTime > 0) {
                    String localResetTime = earliestReset != null
                        ? formatDateInLocalTimezone(earliestReset)
                        : formatDateInLocalTimezone(record.getResetDate().toInstant());
                    log.info("GitHub API - Current quota has {} remaining of {}. Waiting for quota to reset at {} (local time)", 
                            remaining, record.getLimit(), localResetTime);
                    Thread.sleep(sleepTime * 1000);
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
     * Format date in UTC timezone for consistency in Docker containers
     * Docker containers should use UTC time to avoid timezone confusion
     */
    private String formatDateInLocalTimezone(Instant instant) {
        LocalDateTime utcDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy 'GMT'");
        return utcDateTime.format(formatter);
    }
}
