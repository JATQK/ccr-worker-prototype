package de.leipzig.htwk.gitrdf.worker.timemeasurement;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class TimeLog {
    private String identifier;
    private long downloadTime;
    private long gitCommitConversionTime;
    private long gitBranchSnapshottingTime;
    private long githubIssueConversionTime;
    private long conversionTime;
    private long totalTime;

    private boolean enabled;

    public TimeLog(boolean enabled) {

        this.identifier = "Not set";
        this.downloadTime = 0;
        this.gitCommitConversionTime = 0;
        this.gitBranchSnapshottingTime = 0;
        this.githubIssueConversionTime = 0;
        this.conversionTime = 0;
        this.totalTime = 0;

        if (!enabled) {
            log.info("Time measurement logging is disabled.");
        }

        this.enabled = enabled;
    }

    public void printTimes() {

        if (!this.enabled) {
            log.info("Time measurement logging is disabled. No time measurement output will be provided.");
            return;
        }

        log.info("Identifier for measurements is: '{}'", this.identifier);
        log.info("TIME MEASUREMENT DONE: Download time in milliseconds is: '{}'", this.downloadTime);
        log.info("TIME MEASUREMENT DONE: Git-Commit conversion time in milliseconds is: '{}'", this.gitCommitConversionTime);
        log.info("TIME MEASUREMENT DONE: Git branch-snapshotting conversion time in milliseconds is: '{}'", this.gitBranchSnapshottingTime);
        log.info("TIME MEASUREMENT DONE: Github-Issue conversion time in milliseconds is: '{}'", this.githubIssueConversionTime);
        log.info("TIME MEASUREMENT DONE: Conversion time in milliseconds is: '{}'", this.conversionTime);
        log.info("TIME MEASUREMENT DONE: Total time in milliseconds is: '{}'", this.totalTime);

    }

}