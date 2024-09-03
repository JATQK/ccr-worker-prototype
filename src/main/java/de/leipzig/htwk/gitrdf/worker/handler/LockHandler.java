package de.leipzig.htwk.gitrdf.worker.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.RenewableLockRegistry;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class LockHandler {

    public static final int THIRTY_MINUTES = 1000 * 60 * 30;

    private final long renewTime;

    private final Clock clock;

    private final RenewableLockRegistry renewableLockRegistry;

    private final String lockId;

    private LocalDateTime lastLock;

    public LockHandler(long renewTime, Clock clock, RenewableLockRegistry renewableLockRegistry, String lockId) {
        this.renewTime = renewTime;
        this.clock = clock;
        this.renewableLockRegistry = renewableLockRegistry;
        this.lockId = lockId;
        this.lastLock = LocalDateTime.MIN;
    }

    public void renewLockOnRenewTimeFulfillment() {

        LocalDateTime now = LocalDateTime.now(clock);
        Duration duration = Duration.between(lastLock, now);

        long millisDuration = duration.toMillis();

        if (millisDuration >= renewTime) {

            log.info("Renewing lock with id '{}'. Time fulfillment of '{}' milliseconds was reached", lockId, renewTime);

            this.lastLock = now;
            renewableLockRegistry.renewLock(lockId);
        }
    }



}
