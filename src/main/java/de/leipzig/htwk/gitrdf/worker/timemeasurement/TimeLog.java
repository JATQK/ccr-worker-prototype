package de.leipzig.htwk.gitrdf.worker.timemeasurement;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@NoArgsConstructor
public class TimeLog {
    private String identifier;
    private long downloadTime;
    private long gitCommitConversionTime;
    private long gitBranchSnapshottingTime;
    private long githubIssueConversionTime;
    private long conversionTime;
    private long totalTime;

    public void printTimes() {

        log.info("Identifier for measurements is: '{}'", this.identifier);
        log.info("TIME MEASUREMENT DONE: Download time in milliseconds is: '{}'", this.downloadTime);
        log.info("TIME MEASUREMENT DONE: Git-Commit conversion time in milliseconds is: '{}'", this.gitCommitConversionTime);
        log.info("TIME MEASUREMENT DONE: Git branch-snapshotting conversion time in milliseconds is: '{}'", this.gitBranchSnapshottingTime);
        log.info("TIME MEASUREMENT DONE: Github-Issue conversion time in milliseconds is: '{}'", this.githubIssueConversionTime);
        log.info("TIME MEASUREMENT DONE: Conversion time in milliseconds is: '{}'", this.conversionTime);
        log.info("TIME MEASUREMENT DONE: Total time in milliseconds is: '{}'", this.totalTime);

    }

}