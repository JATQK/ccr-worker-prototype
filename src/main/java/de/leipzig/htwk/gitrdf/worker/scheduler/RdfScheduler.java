package de.leipzig.htwk.gitrdf.worker.scheduler;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.worker.database.repository.GitRepositoryOrderRepository;
import de.leipzig.htwk.gitrdf.worker.service.GitRdfConversionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class RdfScheduler {

    private static final int THREE_SECONDS = 3000;

    private static final int ONE_SECOND = 1000;

    private final Clock clock;

    private final LockRegistry lockRegistry;

    private final GitRepositoryOrderRepository gitRepositoryOrderRepository;

    private final GitRdfConversionService gitRdfConversionService;

    public RdfScheduler(
            Clock clock,
            JdbcLockRegistry jdbcLockRegistry,
            GitRepositoryOrderRepository gitRepositoryOrderRepository,
            GitRdfConversionService gitRdfConversionService) {

        this.clock = clock;
        this.lockRegistry = jdbcLockRegistry;
        this.gitRepositoryOrderRepository = gitRepositoryOrderRepository;
        this.gitRdfConversionService = gitRdfConversionService;
    }

    @Scheduled(fixedDelay = THREE_SECONDS)
    public void rdfTask() {

        // TODO: comment out in prod system -> will flood log -> maybe TRACE?
        log.info("Triggering rdf task run at {}", LocalDateTime.now(clock));

        List<GitRepositoryOrderEntity> entitiesInStatusReceived
                = gitRepositoryOrderRepository.findAllByStatus(GitRepositoryOrderStatus.RECEIVED);

        // TODO: comment out in prod system -> will flood log -> maybe TRACE?
        log.info("Found {} repositories in status 'RECEIVED'", entitiesInStatusReceived.size());

        Lock lock = null;

        boolean runPerformed = false;

        for (GitRepositoryOrderEntity entity : entitiesInStatusReceived) {

            if (runPerformed) break;

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

                    entity.setStatus(GitRepositoryOrderStatus.PROCESSING);
                    entity.setNumberOfTries(entity.getNumberOfTries() + 1);
                    gitRepositoryOrderRepository.save(entity);

                    gitRdfConversionService.performGitRepoToRdfConversion(entity.getId());

                } catch (Exception ex) {
                    log.warn("Exception during .git repository to rdf conversion. Error is {}", ex, ex);
                } finally {
                    runPerformed = true;
                    lock.unlock();
                }

            } else {
                log.info("Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.", lockId);
                continue;
            }

        }

    }

    @Scheduled(fixedDelay = ONE_SECOND)
    public void processingFailureCleanup() {

        // TODO: comment out in prod system -> will flood log -> maybe TRACE?
        log.info("Triggering failure cleanup run at {}", LocalDateTime.now(clock));

        List<GitRepositoryOrderEntity> entitiesInStatusProcessing
                = gitRepositoryOrderRepository.findAllByStatus(GitRepositoryOrderStatus.PROCESSING);

        // TODO: comment out in prod system -> will flood log -> maybe TRACE?
        log.info("Found {} repositories in status 'PROCESSING'", entitiesInStatusProcessing.size());

        Lock lock = null;

        for (GitRepositoryOrderEntity entity : entitiesInStatusProcessing) {

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

                    Optional<GitRepositoryOrderEntity> optionalGitRepoEntry
                            = gitRepositoryOrderRepository.findById(entity.getId());

                    if (optionalGitRepoEntry.isPresent()) {

                        GitRepositoryOrderEntity gitRepoEntry = optionalGitRepoEntry.get();

                        if (gitRepoEntry.getStatus().equals(GitRepositoryOrderStatus.PROCESSING)) {
                            gitRepoEntry.setStatus(GitRepositoryOrderStatus.RECEIVED);
                            gitRepositoryOrderRepository.save(gitRepoEntry);
                        }

                    } else {
                        throw new RuntimeException(String.format(
                                "Couldn't find git repository order entry for id '%s' while performing processing failure cleanup",
                                lockId));
                    }

                } catch (Exception ex) {
                    log.warn("Exception during processing failure cleanup. Error is {}", ex, ex);
                    continue;
                } finally {
                    lock.unlock();
                }

            } else {
                log.info("Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.", lockId);
                continue;
            }

        }

    }

}
