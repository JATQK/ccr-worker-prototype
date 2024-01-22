package de.leipzig.htwk.gitrdf.worker.scheduler;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.worker.database.repository.GitRepositoryOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class RdfScheduler {

    private static final int THREE_SECONDS = 3000;

    private final Clock clock;

    private final LockRegistry lockRegistry;

    private final GitRepositoryOrderRepository gitRepositoryOrderRepository;

    public RdfScheduler(
            Clock clock,
            JdbcLockRegistry jdbcLockRegistry,
            GitRepositoryOrderRepository gitRepositoryOrderRepository) {

        this.clock = clock;
        this.lockRegistry = jdbcLockRegistry;
        this.gitRepositoryOrderRepository = gitRepositoryOrderRepository;
    }

    @Scheduled(fixedDelay = THREE_SECONDS)
    public void rdfTask() {

        log.info("Triggering rdf task run at {}", LocalDateTime.now(clock));

        List<GitRepositoryOrderEntity> entitiesInStatusReceived
                = gitRepositoryOrderRepository.findAllByStatus(GitRepositoryOrderStatus.RECEIVED);

        Lock lock = null;

        for (GitRepositoryOrderEntity entity : entitiesInStatusReceived) {

            lock = null;
            String lockId = String.valueOf(entity.getId());

            try {
                lock = lockRegistry.obtain(lockId);
            } catch (Exception ex) {
                log.warn("Couldn't obtain lock. Exception is {}.", ex, ex);
                continue;
            }

            if (lock.tryLock()) {

                try {
                    // TODO (ccr): Perform git to rdf conversion
                } catch (Exception ex) {
                    // TODO (ccr): Do we need this? Check this
                } finally {
                    lock.unlock();
                }

            } else {
                log.info("Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.", lockId);
                continue;
            }

        }

    }

    // TODO (ccr)
    // TODO (ccr): Maybe extract method into extra class and mark as @Transactional
    private void performGitRepoToRdfConversion(long id) {

    }

}
