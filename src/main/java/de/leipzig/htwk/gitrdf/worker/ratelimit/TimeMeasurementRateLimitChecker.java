package de.leipzig.htwk.gitrdf.worker.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.RateLimitChecker;

@Slf4j
public class TimeMeasurementRateLimitChecker extends RateLimitChecker.LiteralValue {

    public TimeMeasurementRateLimitChecker(int sleepAtOrBelow) {
        super(sleepAtOrBelow);
    }

    @Override
    protected boolean checkRateLimit(GHRateLimit.Record record, long count) throws InterruptedException {

        long currentMillis = System.currentTimeMillis();

        boolean result = super.checkRateLimit(record, count);

        if (result) {
            long sleepMilliseconds = record.getResetDate().getTime() - currentMillis;
            log.info("TIME MEASUREMENT: The rate limit checker performed a thread sleep operation to wait for " +
                    "the github api rate limiting to reset. The time waited was: '{}'", sleepMilliseconds);
        }

        return result;
    }
}
