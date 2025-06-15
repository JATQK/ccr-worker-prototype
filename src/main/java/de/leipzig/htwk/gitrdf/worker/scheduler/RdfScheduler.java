package de.leipzig.htwk.gitrdf.worker.scheduler;

import de.leipzig.htwk.gitrdf.database.common.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.database.common.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.database.common.repository.GitRepositoryOrderRepository;
import de.leipzig.htwk.gitrdf.database.common.repository.GithubRepositoryOrderRepository;
import de.leipzig.htwk.gitrdf.worker.config.SchedulerConfig;
import de.leipzig.htwk.gitrdf.worker.handler.LockHandler;
import de.leipzig.htwk.gitrdf.worker.service.GitRdfConversionService;
import de.leipzig.htwk.gitrdf.worker.service.impl.GithubConversionServiceImpl;
import de.leipzig.htwk.gitrdf.worker.service.impl.GithubHandlerService;
import de.leipzig.htwk.gitrdf.worker.timemeasurement.TimeLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.RenewableLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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

    private static final String GIT_TO_RDF_LOCK_ID_SUFFIX = "-git-repo-lock";

    private static final String GITHUB_TO_RDF_LOCK_ID_SUFFIX = "-github-repo-lock";

    private final Clock clock;

    private final LockRegistry lockRegistry;

    private final GitRepositoryOrderRepository gitRepositoryOrderRepository;

    private final GitRdfConversionService gitRdfConversionService;

    private final GithubRepositoryOrderRepository githubRepositoryOrderRepository;

    private final GithubConversionServiceImpl githubConversionService;

    private final GithubHandlerService githubHandlerService;

    private final SchedulerConfig schedulerConfig;

    public RdfScheduler(
            Clock clock,
            JdbcLockRegistry jdbcLockRegistry,
            GitRepositoryOrderRepository gitRepositoryOrderRepository,
            GitRdfConversionService gitRdfConversionService,
            GithubRepositoryOrderRepository githubRepositoryOrderRepository,
            GithubConversionServiceImpl githubConversionService,
            GithubHandlerService githubHandlerService,
            SchedulerConfig schedulerConfig) {

        this.clock = clock;
        this.lockRegistry = jdbcLockRegistry;
        this.gitRepositoryOrderRepository = gitRepositoryOrderRepository;
        this.gitRdfConversionService = gitRdfConversionService;
        this.githubRepositoryOrderRepository = githubRepositoryOrderRepository;
        this.githubConversionService = githubConversionService;
        this.githubHandlerService = githubHandlerService;
        this.schedulerConfig = schedulerConfig;
    }

    @Scheduled(fixedDelay = THREE_SECONDS)
    public void rdfGitRepoTask() {

        if (!schedulerConfig.isRdfGitRepoTaskEnabled())
            return;

        log.trace("Triggering git repository rdf task run at {}", LocalDateTime.now(clock));

        List<GitRepositoryOrderEntity> entitiesInStatusReceived = gitRepositoryOrderRepository
                .findAllByStatus(GitRepositoryOrderStatus.RECEIVED);

        log.trace("Found {} repositories in status 'RECEIVED'", entitiesInStatusReceived.size());

        Lock lock = null;

        boolean runPerformed = false;

        for (GitRepositoryOrderEntity entity : entitiesInStatusReceived) {

            if (runPerformed)
                break;

            lock = null;
            String lockId = getGitToRdfLockId(entity.getId());

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
                    log.warn("Exception during .git repository to rdf conversion. Error is {}", ex.getMessage(), ex);
                } finally {
                    runPerformed = true;
                    lock.unlock();
                }

            } else {
                log.info("Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.",
                        lockId);
                continue;
            }

        }

    }

    @Scheduled(fixedDelay = THREE_SECONDS)
    public void rdfGithubRepoTask() throws NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException,
            IOException, InterruptedException {

        if (!schedulerConfig.isRdfGithubRepoTaskEnabled())
            return;

        log.trace("Triggering github repository rdf task run at {}", LocalDateTime.now(clock));

        List<GithubRepositoryOrderEntity> entitiesInStatusReceived = githubRepositoryOrderRepository
                .findAllByStatus(GitRepositoryOrderStatus.RECEIVED);

        log.trace("Found {} repositories in status 'RECEIVED'", entitiesInStatusReceived.size());

        Lock lock = null;

        boolean runPerformed = false;

        for (GithubRepositoryOrderEntity entity : entitiesInStatusReceived) {

            if (runPerformed)
                break;

            lock = null;
            String lockId = getGithubToRdfLockId(entity.getId());

            try {
                lock = lockRegistry.obtain(lockId);
            } catch (Exception ex) {
                log.warn("Couldn't obtain lock. Exception is {}.", ex, ex);
                continue;
            }

            if (lock.tryLock()) {

                try {

                    log.info("Start processing of '{}' repository", entity.getRepositoryName());

                    // Fetch repository again and check, that the OrderStatus is still 'Received'
                    Optional<GithubRepositoryOrderEntity> optionalWorkEntity = githubRepositoryOrderRepository
                            .findById(entity.getId());

                    if (optionalWorkEntity.isEmpty()) {
                        throw new RuntimeException("Failed to retrieve github repository order entry again after " +
                                "locking, to check whether the repo is still in status 'Received'");
                    }

                    GithubRepositoryOrderEntity workEntity = optionalWorkEntity.get();

                    if (workEntity.getStatus() != GitRepositoryOrderStatus.RECEIVED) {
                        log.info("Aborting processing of '{}' repository. " +
                                "Repository is not in status 'Received' anymore",
                                workEntity.getRepositoryName());
                        continue;
                    }

                    if (workEntity.getNumberOfTries() > 9) {

                        log.warn("Processing of '{}' repository aborted. " +
                                "There are already more than 9 conversion attempts. " +
                                "Setting status to '{}'",
                                workEntity.getRepositoryName(),
                                GitRepositoryOrderStatus.FAILED);

                        workEntity.setStatus(GitRepositoryOrderStatus.FAILED);
                        githubRepositoryOrderRepository.save(workEntity);

                    } else {

                        TimeLog timeLog = new TimeLog(false);
                        timeLog.setIdentifier(workEntity.getOwnerName() + " " + workEntity.getRepositoryName());

                        RenewableLockRegistry renewableLockRegistry = getRenewableLockRegistryOrThrowException();

                        LockHandler lockHandler = new LockHandler(LockHandler.THIRTY_MINUTES, clock,
                                renewableLockRegistry, lockId);

                        StopWatch watch = new StopWatch();
                        watch.start();

                        workEntity.setStatus(GitRepositoryOrderStatus.PROCESSING);
                        workEntity.setNumberOfTries(workEntity.getNumberOfTries() + 1);
                        githubRepositoryOrderRepository.save(workEntity);

                        githubConversionService.performGithubRepoToRdfConversion(
                                workEntity.getId(), timeLog, lockHandler);

                        watch.stop();

                        timeLog.setTotalTime(watch.getTime());
                        // log.info("TIME MEASUREMENT DONE: Total time in milliseconds is: '{}'",
                        // watch.getTime());
                        timeLog.printTimes();

                    }

                } catch (Exception ex) {
                    log.warn("Exception during .git repository to rdf conversion. Error is {}", ex.getMessage(), ex);
                } finally {
                    runPerformed = true;
                    lock.unlock();
                }

            } else {
                log.info(
                        "Github conversion scheduler: Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.",
                        lockId);
                continue;
            }

        }

    }

    @Scheduled(fixedDelay = ONE_SECOND)
    public void processingFailureCleanupForGitToRdf() {

        log.trace("Triggering failure cleanup run at {}", LocalDateTime.now(clock));

        List<GitRepositoryOrderEntity> entitiesInStatusProcessing = gitRepositoryOrderRepository
                .findAllByStatus(GitRepositoryOrderStatus.PROCESSING);

        log.trace("Found {} repositories in status 'PROCESSING'", entitiesInStatusProcessing.size());

        Lock lock = null;

        for (GitRepositoryOrderEntity entity : entitiesInStatusProcessing) {

            lock = null;
            String lockId = getGitToRdfLockId(entity.getId());

            try {
                lock = lockRegistry.obtain(lockId);
            } catch (Exception ex) {
                log.warn("Couldn't obtain lock. Exception is {}.", ex, ex);
                continue;
            }

            if (lock.tryLock()) {

                try {

                    Optional<GitRepositoryOrderEntity> optionalGitRepoEntry = gitRepositoryOrderRepository
                            .findById(entity.getId());

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
                log.info("Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.",
                        lockId);
                continue;
            }

        }

    }

    @Scheduled(fixedDelay = ONE_SECOND)
    public void processingFailureCleanupForGithubToRdf() {

        log.trace("Triggering failure cleanup run at {}", LocalDateTime.now(clock));

        List<GithubRepositoryOrderEntity> entitiesInStatusProcessing = githubRepositoryOrderRepository
                .findAllByStatus(GitRepositoryOrderStatus.PROCESSING);

        log.trace("Found {} repositories in status 'PROCESSING'", entitiesInStatusProcessing.size());

        Lock lock = null;

        for (GithubRepositoryOrderEntity entity : entitiesInStatusProcessing) {

            lock = null;
            String lockId = getGithubToRdfLockId(entity.getId());

            try {
                lock = lockRegistry.obtain(lockId);
            } catch (Exception ex) {
                log.warn("Couldn't obtain lock. Exception is {}.", ex, ex);
                continue;
            }

            if (lock.tryLock()) {

                try {

                    Optional<GithubRepositoryOrderEntity> optionalGithubRepoEntry = githubRepositoryOrderRepository
                            .findById(entity.getId());

                    if (optionalGithubRepoEntry.isPresent()) {

                        GithubRepositoryOrderEntity githubRepoEntry = optionalGithubRepoEntry.get();

                        if (githubRepoEntry.getStatus().equals(GitRepositoryOrderStatus.PROCESSING)) {
                            githubRepoEntry.setStatus(GitRepositoryOrderStatus.RECEIVED);
                            githubRepositoryOrderRepository.save(githubRepoEntry);
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
                log.info(
                        "Github cleanup: Lock with the id '{}' was already acquired. Skipping this specific lock and continuing.",
                        lockId);
                continue;
            }

        }

    }

    private String getGitToRdfLockId(long id) {
        return id + GIT_TO_RDF_LOCK_ID_SUFFIX;
    }

    private String getGithubToRdfLockId(long id) {
        return id + GITHUB_TO_RDF_LOCK_ID_SUFFIX;
    }

    private RenewableLockRegistry getRenewableLockRegistryOrThrowException() {

        if (!(this.lockRegistry instanceof RenewableLockRegistry)) {
            throw new RuntimeException("Github conversion scheduler: Failed cast the used lock " +
                    "registry to a renewable lock registry (for future lock renewal if needed)." +
                    "This should be possible, as the used registry should be a renewable lock registry");
        }

        return (RenewableLockRegistry) this.lockRegistry;
    }

}
