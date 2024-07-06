package de.leipzig.htwk.gitrdf.worker.utils;

import de.leipzig.htwk.gitrdf.worker.service.impl.GithubHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service // TODO: rename to service, if not util
@Slf4j
public class GithubIssueUtils {

    private static final long ONE_AND_A_HALF_MINUTE = 1000L * 90L;

    private final GithubHandlerService githubHandlerService;

    private final int secondaryRateLimitCountBorder;

    private final int secondsToSleep;

    private final int issuePageSize;

    public GithubIssueUtils(
            GithubHandlerService githubHandlerService,
            @Value("${worker.issues.pre-secondary-rate-limit-check}") int secondaryRateLimitCountBorder,
            @Value("${worker.issues.seconds-to-sleep}") int secondsToSleep,
            @Value("${worker.issues.issue-page-size}") int issuePageSize) {

        this.githubHandlerService = githubHandlerService;
        this.secondaryRateLimitCountBorder = secondaryRateLimitCountBorder;
        this.secondsToSleep = secondsToSleep;
        this.issuePageSize = issuePageSize;
    }

    public void downloadGithubIssuesIntoFile(
            File issueDownloadTempFile,
            String githubRepositoryName) throws URISyntaxException, IOException, InterruptedException {

        GitHub githubIssueHandle = githubHandlerService.getGithubHandle();
        GHRepository githubRepositoryIssueHandle = githubIssueHandle.getRepository(githubRepositoryName);

        if (githubRepositoryIssueHandle.hasIssues()) {

            int issuesDownloadedSecondaryRateLimitCounter = 0;
            long issueDownloadStartTime = System.currentTimeMillis();

            List<GHIssue> githubIssues = new ArrayList<>();

            for (GHIssue ghIssue : githubRepositoryIssueHandle.queryIssues().state(GHIssueState.ALL).pageSize(issuePageSize).list()) {

                issuesDownloadedSecondaryRateLimitCounter++;

                gi

                int issueRequestCounter = issuesDownloadedSecondaryRateLimitCounter % issuePageSize;

                if (issueRequestCounter > secondaryRateLimitCountBorder) {

                    long duration = System.currentTimeMillis() - issueDownloadStartTime;

                    if (duration > ONE_AND_A_HALF_MINUTE) {
                        issuesDownloadedSecondaryRateLimitCounter = 0;
                        issueDownloadStartTime = System.currentTimeMillis();
                    } else {
                        issuesDownloadedSecondaryRateLimitCounter = 0;
                        makeThreadSleepForGithubSecondaryRateLimit();
                    }

                }

            }

        }

    }

    private void makeThreadSleepForGithubSecondaryRateLimit() {

        try {

            log.info("Prevent github secondary limit during github issue download: Sleeping for '{}' seconds", secondsToSleep);
            Thread.sleep(secondsToSleep * 1000L);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();
            throw new RuntimeException("Sleeping thread for github issue download was interrupted", ex);

        }

    }

}
