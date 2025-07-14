package de.leipzig.htwk.gitrdf.worker.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.IllegalCharsetNameException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.hibernate.engine.jdbc.BlobProxy;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.GHReaction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowJob.Step;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.leipzig.htwk.gitrdf.database.common.entity.GitCommitRepositoryFilter;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubIssueRepositoryFilter;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.database.common.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.database.common.entity.lob.GithubRepositoryOrderEntityLobs;
import de.leipzig.htwk.gitrdf.worker.calculator.BranchSnapshotCalculator;
import de.leipzig.htwk.gitrdf.worker.calculator.CommitBranchCalculator;
import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.handler.LockHandler;
import de.leipzig.htwk.gitrdf.worker.timemeasurement.TimeLog;
import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.GithubUserInfo;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGitCommitUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubCommentUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueReviewUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubReactionUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubWorkflowJobUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubWorkflowStepUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubWorkflowUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfTurtleTidier;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubRdfConversionTransactionService {

    private static final int TWENTY_FIVE_MEGABYTE = 1024 * 1024 * 25;

    private static final int PROCESS_ISSUE_LIMIT = 10; // Limit for the number of issues to process
    private static final String[] PROCESS_ISSUE_ONLY = {}; // Only process these issues // "9946", "9947", "9948",
                                                           // "9949", "9950"
    private static final int PROCESS_COMMIT_LIMIT = 10; // Limit for the number of commits to process

    // TODO: replace PURL

    public static final String GIT_NAMESPACE = "git";
    public static final String GIT_URI = "https://purl.archive.org/git2rdftest/v2/git2RDFLab-git#";

    public static final String PLATFORM_NAMESPACE = "platform";
    public static final String PLATFORM_URI = "https://purl.archive.org/git2rdftest/v2/git2RDFLab-platform#";

    public static final String PLATFORM_GITHUB_NAMESPACE = "github";
    public static final String PLATFORM_GITHUB_URI = "https://purl.archive.org/git2rdftest/v2/git2RDFLab-platform-github#";

    public static final String XSD_SCHEMA_NAMESPACE = "xsd";
    public static final String XSD_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema#";

    public static final String RDF_SCHEMA_NAMESPACE = "rdf";
    public static final String RDF_SCHEMA_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String OWL_SCHEMA_NAMESPACE = "owl";
    public static final String OWL_SCHEMA_URI = "http://www.w3.org/2002/07/owl#";

    private final GithubHandlerService githubHandlerService;

    private final GithubConfig githubConfig;

    private final EntityManager entityManager;

    private final int commitsPerIteration;

    private static final int RETRY_DELAY_MS = 1000;

    /**
     * Track already written pull request review IDs to avoid duplicate triples
     * when GitHub returns the same review multiple times.
     */
    private final Set<Long> seenReviewIds = new HashSet<>();

    /**
     * Caches for GitHub API objects to avoid unnecessary API calls during a
     * conversion run.
     */
    private final Map<Integer, GHPullRequest> pullRequestCache = new HashMap<>();
    private final Map<Integer, List<GHPullRequestReview>> reviewCache = new HashMap<>();
    private final Map<Long, List<GHPullRequestReviewComment>> reviewCommentsCache = new HashMap<>();
    private final Map<Integer, List<GHIssueComment>> issueCommentsCache = new HashMap<>();
    private final Map<Long, List<GHReaction>> reviewCommentReactionsCache = new HashMap<>();
    private final Map<Long, List<GHReaction>> issueCommentReactionsCache = new HashMap<>();
    private final Map<Integer, List<GHPullRequestCommitDetail>> commitCache = new HashMap<>();

    private static class PullRequestInfo {
        final String issueUri;
        final String mergeCommitSha;
        final LocalDateTime mergedAt;

        PullRequestInfo(String issueUri, String mergeCommitSha, LocalDateTime mergedAt) {
            this.issueUri = issueUri;
            this.mergeCommitSha = mergeCommitSha;
            this.mergedAt = mergedAt;
        }
    }

    public GithubRdfConversionTransactionService(
            GithubHandlerService githubHandlerService,
            EntityManager entityManager,
            GithubConfig githubConfig,
            @Value("${worker.commits-per-iteration}") int commitsPerIteration) {

        this.githubHandlerService = githubHandlerService;
        this.githubConfig = githubConfig;
        this.entityManager = entityManager;
        this.commitsPerIteration = commitsPerIteration;
    }

    @Transactional(rollbackFor = { IOException.class, GitAPIException.class, URISyntaxException.class,
            InterruptedException.class }) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGithubRepoToRdfConversionWithGitCloningLogicAndReturnCloseableInputStream(
            long id,
            File gitWorkingDirectory,
            File rdfTempFile,
            TimeLog timeLog,
            LockHandler lockHandler) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        // Clear caches for this conversion run
        pullRequestCache.clear();
        reviewCache.clear();
        reviewCommentsCache.clear();
        issueCommentsCache.clear();
        reviewCommentReactionsCache.clear();
        issueCommentReactionsCache.clear();
        commitCache.clear();
        seenReviewIds.clear();

        Git gitHandler = null;

        try {

            GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs = entityManager
                    .find(GithubRepositoryOrderEntityLobs.class, id);

            GithubRepositoryOrderEntity githubRepositoryOrderEntity = entityManager
                    .find(GithubRepositoryOrderEntity.class, id);

            String owner = githubRepositoryOrderEntity.getOwnerName();
            String repo = githubRepositoryOrderEntity.getRepositoryName();

            lockHandler.renewLockOnRenewTimeFulfillment();

            StopWatch downloadWatch = new StopWatch();

            downloadWatch.start();

            gitHandler = performGitClone(owner, repo, gitWorkingDirectory);

            downloadWatch.stop();

            lockHandler.renewLockOnRenewTimeFulfillment();

            //log.info("TIME MEASUREMENT DONE: Download time in milliseconds is: '{}'", downloadWatch.getTime());
            timeLog.setDownloadTime(downloadWatch.getTime());

            StopWatch conversionWatch = new StopWatch();

            conversionWatch.start();

            InputStream needsToBeClosedOutsideOfTransaction = writeRdf(gitHandler, githubRepositoryOrderEntity,
                    githubRepositoryOrderEntityLobs, rdfTempFile, timeLog, lockHandler);

            conversionWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Conversion time in milliseconds is: '{}'", conversionWatch.getTime());
            timeLog.setConversionTime(conversionWatch.getTime());

            githubRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

            return needsToBeClosedOutsideOfTransaction;

        } finally {

            if (gitHandler != null)
                gitHandler.close();

        }

    }

    private void writeMergeInfo(GHIssue issue, GHPullRequest pr, StreamRDF writer, String issueUri) {
        if (issue == null || !issue.isPullRequest() || pr == null) {
            return;
        }
        try {
            writer.triple(RdfGithubIssueUtils.createIssueMergedProperty(issueUri, pr.isMerged()));

            Date mergedAt = pr.getMergedAt();

            if (mergedAt != null) {
                writer.triple(RdfGithubIssueUtils.createIssueMergedAtProperty(issueUri, localDateTimeFrom(mergedAt)));
            }

            if (pr.getMergedBy() != null) {
                writer.triple(RdfGithubIssueUtils.createIssueMergedByProperty(issueUri,
                        pr.getMergedBy().getHtmlUrl().toString()));
            }

            if (pr.getMergeCommitSha() != null) {
                writer.triple(RdfGithubIssueUtils.createIssueMergeCommitShaProperty(issueUri, pr.getMergeCommitSha()));
            }
        } catch (IOException e) {
            log.warn("Error while writing merge info for issue {}: {}", issueUri, e.getMessage());
        }
        // TODO: Add addional merge information if available
    }

    private void writeWorkflowRunInfo(GHPullRequest pr, StreamRDF writer, String issueUri, String repositoryUri)
            throws IOException, InterruptedException {

        // Skip workflow processing if we're not processing issues or if limits are very
        // small
        if (pr == null || PROCESS_ISSUE_LIMIT <= 0) {
            return;
        }

        try {
            if (!pr.isMerged()) {
                log.debug("Pull request {} is not merged, skipping workflow run info.", pr.getHtmlUrl());
                return;
            }

            // For small limits, skip expensive workflow processing entirely
            if (PROCESS_ISSUE_LIMIT <= 5) {
                log.debug("Issue limit is small ({}), skipping workflow processing for performance",
                        PROCESS_ISSUE_LIMIT);
                return;
            }

            String headSha = pr.getHead().getSha();
            GHRepository repo = pr.getRepository();

            // Limit check runs processing
            List<GHCheckRun> checkRuns = executeWithRetry(
                    () -> repo.getCommit(headSha).getCheckRuns().withPageSize(10).toList(),
                    "getCheckRuns for " + headSha);

            // Process only first few check runs to avoid excessive API calls
            int maxCheckRuns = Math.min(checkRuns.size(), 5);

            for (int i = 0; i < maxCheckRuns; i++) {
                GHCheckRun checkRun = checkRuns.get(i);

                if (checkRun.getDetailsUrl() != null &&
                        checkRun.getDetailsUrl().toString().contains("/actions/runs/")) {

                    String runId = extractRunIdFromUrl(checkRun.getDetailsUrl().toString());
                    if (runId != null) {
                        GHWorkflowRun run = executeWithRetry(
                                () -> repo.getWorkflowRun(Long.parseLong(runId)),
                                "getWorkflowRun " + runId);
                        writeWorkflowRunData(repositoryUri, run, writer, issueUri, headSha);
                    }
                }
            }

        } catch (IOException e) {
            log.warn("Error fetching workflow runs via check runs: {}", e.getMessage());
        }
    }

    private String extractRunIdFromUrl(String detailsUrl) {
        // Extract run ID from URL like: https://github.com/owner/repo/actions/runs/12345
        Pattern pattern = Pattern.compile(".*/actions/runs/(\\d+)");
        Matcher matcher = pattern.matcher(detailsUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Git performGitClone(String ownerName, String repositoryName, File gitWorkingDirectory)
            throws GitAPIException {

        String gitRepoTargetUrl = String.format("https://github.com/%s/%s.git", ownerName, repositoryName);

        int timeoutSecondsOnNoDataTransfer = 60;

        return Git.cloneRepository()
                .setURI(gitRepoTargetUrl)
                .setDirectory(gitWorkingDirectory)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        githubConfig.getGithubSystemUserName(),
                        githubConfig.getGithubSystemUserPersonalAccessToken())) // ---> maybe we dont even need this for public repositories?
                .setTimeout(timeoutSecondsOnNoDataTransfer)
                .call();
    }

    private InputStream writeRdf(
            Git gitHandler,
            GithubRepositoryOrderEntity entity,
            GithubRepositoryOrderEntityLobs entityLobs,
            File rdfTempFile,
            TimeLog timeLog,
            LockHandler lockHandler) throws GitAPIException, IOException, URISyntaxException, InterruptedException {

        String owner = entity.getOwnerName();
        String repositoryName = entity.getRepositoryName();

        Repository gitRepository = gitHandler.getRepository();

        GitCommitRepositoryFilter gitCommitRepositoryFilter = entity.getGithubRepositoryFilter()
                .getGitCommitRepositoryFilter();

        GithubIssueRepositoryFilter githubIssueRepositoryFilter = entity.getGithubRepositoryFilter()
                .getGithubIssueRepositoryFilter();

        Map<String, RdfGitCommitUserUtils> uniqueGitCommiterWithHash = new HashMap<>();
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(rdfTempFile))) {

            GitHub gitHubHandle = githubHandlerService.getGithub();

            lockHandler.renewLockOnRenewTimeFulfillment();

            String githubRepositoryName = String.format("%s/%s", owner, repositoryName);
            String repositoryUri = GithubUriUtils.getRepositoryUri(owner, repositoryName);

            GHRepository githubRepositoryHandle = gitHubHandle.getRepository(githubRepositoryName);
            // See: https://jena.apache.org/documentation/io/rdf-output.html#streamed-block-formats
            StreamRDF writer = StreamRDFWriter.getWriterStream(outputStream, RDFFormat.TURTLE_BLOCKS);

            writer.prefix(XSD_SCHEMA_NAMESPACE, XSD_SCHEMA_URI);
            writer.prefix(RDF_SCHEMA_NAMESPACE, RDF_SCHEMA_URI);
            writer.prefix(OWL_SCHEMA_NAMESPACE, OWL_SCHEMA_URI);
            writer.prefix(GIT_NAMESPACE, GIT_URI);
            writer.prefix(PLATFORM_NAMESPACE, PLATFORM_URI);
            writer.prefix(PLATFORM_GITHUB_NAMESPACE, PLATFORM_GITHUB_URI);
            //writer.prefix(GITHUB_COMMIT_NAMESPACE, githubCommitPrefixValue);
            //writer.prefix(GITHUB_ISSUE_NAMESPACE, githubIssuePrefixValue);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(gitRepository);
            ObjectReader reader = gitRepository.newObjectReader();

            Iterable<Ref> branches = gitHandler.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            RevWalk revWalk = new RevWalk(gitRepository);

            CommitBranchCalculator commitBranchCalculator = null;
            if (gitCommitRepositoryFilter.isEnableCommitBranch()) {
                log.info("Start constructing commit branch calculator");
                commitBranchCalculator = new CommitBranchCalculator(branches, revWalk);
            }

            lockHandler.renewLockOnRenewTimeFulfillment();

            log.info("Start conversion run of '{}' repository", repositoryName);

            StopWatch commitConversionWatch = new StopWatch();

            commitConversionWatch.start();


            lockHandler.renewLockOnRenewTimeFulfillment();

            // Metadata

            writer.start();
            writer.triple(RdfCommitUtils.createRepositoryRdfTypeProperty(repositoryUri));
            String ownerUri = GithubUriUtils.getUserUri(owner);
            writer.triple(RdfCommitUtils.createRepositoryOwnerProperty(repositoryUri, ownerUri));
            GHUser repoOwner = githubRepositoryHandle.getOwner();
            if (repoOwner != null) {
                writer.triple(RdfGithubUserUtils.createGitHubUserType(ownerUri));
                writer.triple(RdfGithubUserUtils.createLoginProperty(ownerUri, repoOwner.getLogin()));
                writer.triple(RdfGithubUserUtils.createUserIdProperty(ownerUri, repoOwner.getId()));
                if (repoOwner.getName() != null && !repoOwner.getName().isEmpty()) {
                    writer.triple(RdfGithubUserUtils.createNameProperty(ownerUri, repoOwner.getName()));
                }
            }
            writer.triple(RdfCommitUtils.createRepositoryNameProperty(repositoryUri, repositoryName));

            Config config = gitRepository.getConfig();

            // Metadata: encoding
            String commitEncoding = config.getString("i18n", null, "commitEncoding");
            String defaultEncoding = "UTF-8";
            String encoding = commitEncoding != null ? commitEncoding : defaultEncoding;
            writer.triple(RdfCommitUtils.createRepositoryEncodingProperty(repositoryUri, encoding));

            log.info("Repository Metadata - Encoding: {}", encoding);

            writer.finish();

            lockHandler.renewLockOnRenewTimeFulfillment();

            // Branches
            writer.start();
            for (Ref branchRef : branches) {
                String branchName = branchRef.getName();
                String branchUri = GithubUriUtils.getBranchUri(owner, repositoryName, branchName);
                String headCommitUri = GithubUriUtils.getCommitUri(owner, repositoryName, branchRef.getObjectId().getName());
                writer.triple(RdfCommitUtils.createRepositoryHasBranchProperty(repositoryUri, branchUri));
                writer.triple(RdfCommitUtils.createBranchRdfTypeProperty(branchUri));
                writer.triple(RdfCommitUtils.createBranchNameProperty(branchUri, branchName));
                writer.triple(RdfCommitUtils.createBranchHeadCommitProperty(branchUri, headCommitUri));
            }
            writer.finish();

            lockHandler.renewLockOnRenewTimeFulfillment();

            // Submodules

            writer.start();

            SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(gitRepository);

            while (submoduleWalk.next()) {

                try {
                    String submodulePath = submoduleWalk.getPath();
                    String submoduleUrl = submoduleWalk.getModulesUrl();
                    String submoduleCommitHash = submoduleWalk.getObjectId().getName();
                    String submoduleCommitHashUri = GithubUriUtils.getCommitUri(owner, repositoryName, submoduleCommitHash);

                    Resource submoduleResource = ResourceFactory.createResource();
                    Node submoduleNode = submoduleResource.asNode();

                    writer.triple(RdfCommitUtils.createRepositorySubmoduleProperty(repositoryUri, submoduleNode));
                    writer.triple(RdfCommitUtils.createSubmoduleRdfTypeProperty(submoduleNode));

                    writer.triple(RdfCommitUtils.createSubmodulePathProperty(submoduleNode, submodulePath));
                    writer.triple(RdfCommitUtils.createSubmoduleRepositoryEntryProperty(submoduleNode, submoduleUrl));
                    writer.triple(
                            RdfCommitUtils.createSubmoduleCommitEntryProperty(submoduleNode, submoduleCommitHashUri));
                    writer.triple(RdfCommitUtils.createSubmoduleCommitProperty(submoduleNode, submoduleCommitHash));

                    log.info("Submodule: path: {} url: {} commit-hash: {}", submodulePath, submoduleUrl,
                            submoduleCommitHash);
                } catch (ConfigInvalidException e) {
                    log.error("Submodule Invalid Config Error: {}", e.getMessage());
                } catch (Exception e) {
                    log.error("Submodule Error: {}", e.getMessage());
                }
            }

            writer.finish();

            lockHandler.renewLockOnRenewTimeFulfillment();

            if (PROCESS_COMMIT_LIMIT > 0) {
                log.info("Processing commits with limit: {}", PROCESS_COMMIT_LIMIT);

                int commitsProcessed = 0;

                Map<ObjectId, List<String>> commitToTags = getTagsForCommits(gitRepository);

                Map<String, PullRequestInfo> commitPrMap = buildCommitPrMap(githubRepositoryHandle);

                for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                    log.info("Start iterations of git commits. Current iteration count: {}", iteration);

                    if (log.isDebugEnabled())
                        log.debug("Check whether github installation token needs refresh");

                    int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration,
                            commitsPerIteration);

                    log.info("Calculated skip count for this iteration is: {}", skipCount);

                    Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration)
                            .call();

                    boolean finished = true;

                    if (log.isDebugEnabled())
                        log.debug("Starting commit writer rdf");

                    writer.start();

                    if (log.isDebugEnabled())
                        log.debug("Starting commit loop");

                    for (RevCommit commit : commits) {
                        commitsProcessed++;
                        if (commitsProcessed > PROCESS_COMMIT_LIMIT && PROCESS_COMMIT_LIMIT > 0) {
                            log.info("Max computed commits reached, stopping commit processing.");
                            finished = true;
                            break;
                        }
                        finished = false;

                        ObjectId commitId = commit.getId();
                        String gitHash = commitId.name();

                        PersonIdent authorIdent = null;
                        PersonIdent committerIdent = null;

                        try {
                            authorIdent = commit.getAuthorIdent();
                        } catch (RuntimeException ex) {
                            log.warn("Error while trying to identify the author for the git commit '{}'. " +
                                    "Skipping author email and name entry. Error is '{}'", gitHash, ex.getMessage(),
                                    ex);
                        }

                        try {
                            committerIdent = commit.getCommitterIdent();
                        } catch (RuntimeException ex) {
                            log.warn("Error while trying to identify the committer for the git commit '{}'. " +
                                    "Skipping committer email and name entry. Error is '{}'", gitHash, ex.getMessage(),
                                    ex);
                        }

                        String commitUri = GithubUriUtils.getCommitUri(owner, repositoryName, gitHash);
                        //String commitUri = GIT_NAMESPACE + ":GitCommit";

                        if (log.isDebugEnabled())
                            log.debug("Set rdf type property commitUri");

                        writer.triple(RdfCommitUtils.createRdfTypeProperty(commitUri));
                        writer.triple(RdfCommitUtils.createRepositoryHasCommitProperty(repositoryUri, commitUri));

                        if (log.isDebugEnabled())
                            log.debug("Set rdf commit hash");

                        if (gitCommitRepositoryFilter.isEnableCommitHash()) {
                            writer.triple(RdfCommitUtils.createCommitHashProperty(commitUri, gitHash));
                        }

                        if (log.isDebugEnabled())
                            log.debug("Set rdf author email");

                        if (gitCommitRepositoryFilter.isEnableAuthorEmail()) {
                            calculateAuthorEmail( // Also brings github identifier into rdf
                                    authorIdent, uniqueGitCommiterWithHash, writer, commitUri, gitHash,
                                    githubRepositoryHandle);
                        }

                        if (log.isDebugEnabled())
                            log.debug("Set RDF author name");

                        if (gitCommitRepositoryFilter.isEnableAuthorName()) {
                            calculateAuthorName(writer, commitUri, authorIdent);
                        }

                        boolean isAuthorDateEnabled = gitCommitRepositoryFilter.isEnableAuthorDate();
                        boolean isCommitDateEnabled = gitCommitRepositoryFilter.isEnableCommitDate();

                        if (isAuthorDateEnabled || isCommitDateEnabled) {

                            LocalDateTime commitDateTime = localDateTimeFrom(commit.getCommitTime());

                            if (log.isDebugEnabled())
                                log.debug("Set RDF author date");

                            if (isAuthorDateEnabled) {
                                writer.triple(RdfCommitUtils.createAuthorDateProperty(commitUri, commitDateTime));
                            }

                            if (log.isDebugEnabled())
                                log.debug("Set RDF commit date");

                            if (isCommitDateEnabled) {
                                writer.triple(RdfCommitUtils.createCommitDateProperty(commitUri, commitDateTime));
                            }
                        }

                        if (log.isDebugEnabled())
                            log.debug("Set RDF committer name");

                        if (gitCommitRepositoryFilter.isEnableCommitterName()) {
                            calculateCommitterName(writer, commitUri, committerIdent);
                        }

                        if (log.isDebugEnabled())
                            log.debug("Set RDF committer email");

                        if (gitCommitRepositoryFilter.isEnableCommitterEmail()) {
                            calculateCommitterEmail(writer, commitUri, committerIdent);
                        }

                        if (log.isDebugEnabled())
                            log.debug("Set RDF commit message for commit with hash '{}'", gitHash);

                        if (gitCommitRepositoryFilter.isEnableCommitMessage()) {
                            calculateCommitMessage(writer, commitUri, commit);
                        }

                        if (commit.getParentCount() > 1) {
                            writer.triple(RdfCommitUtils.createCommitIsMergeCommitProperty(commitUri, true));
                        }
                        for (RevCommit parent : commit.getParents()) {
                            String parentUri = GithubUriUtils.getCommitUri(owner, repositoryName, parent.getName());
                            writer.triple(RdfCommitUtils.createCommitHasParentProperty(commitUri, parentUri));
                        }

                        PullRequestInfo prInfo = commitPrMap.get(gitHash);
                        calculateCommitIssues(writer, commitUri, commit.getFullMessage(), owner, repositoryName, prInfo);
                        if (prInfo != null) {
                            writer.triple(RdfCommitUtils.createCommitPartOfPullRequestProperty(commitUri, prInfo.issueUri));
                            writer.triple(RdfCommitUtils.createCommitPartOfIssueProperty(commitUri, prInfo.issueUri));
                            writer.triple(RdfGithubIssueUtils.createIssueContainsCommitProperty(prInfo.issueUri, commitUri));
                            writer.triple(RdfCommitUtils.createCommitIsMergedProperty(commitUri, true));
                            if (prInfo.mergedAt != null) {
                                writer.triple(RdfCommitUtils.createCommitMergedAtProperty(commitUri, prInfo.mergedAt));
                            }
                            if (gitHash.equals(prInfo.mergeCommitSha)) {
                                writer.triple(RdfCommitUtils.createCommitMergedIntoIssueProperty(commitUri, prInfo.issueUri));
                            }
                        }

                        // Branch
                        // TODO: better way to handle merges? (so commit could have multiple branches)
                        if (gitCommitRepositoryFilter.isEnableCommitBranch()) {
                            calculateCommitBranch(commitBranchCalculator, writer, commit, commitUri, owner, repositoryName);
                        }

                        List<String> tagNames = commitToTags.get(commitId);

                        if (tagNames != null && !tagNames.isEmpty()) {
                            for (String tagName : tagNames) {
                                String tagUri = GithubUriUtils.getTagUri(owner, repositoryName, tagName);
                                String tagUrl = GithubUriUtils.getTagUrl(owner, repositoryName, tagName);
                                writer.triple(RdfCommitUtils.createCommitHasTagProperty(commitUri, tagUri));
                                writer.triple(RdfCommitUtils.createTagRdfTypeProperty(tagUri));
                                writer.triple(RdfCommitUtils.createTagNameProperty(tagUri, tagName));
                                writer.triple(RdfCommitUtils.createTagPointsToProperty(tagUri, commitUri));
                                writer.triple(RdfCommitUtils.createTagSameAsProperty(tagUri, tagUrl));
                                log.debug("Added Tag '{}' to commit #{}", tagName, commitId.getName());
                            }
                        }

                        // Commit Diffs
                        // See: https://www.codeaffine.com/2016/06/16/jgit-diff/
                        // TODO: check if merges with more than 1 parent exist?

                        if (log.isDebugEnabled())
                            log.debug("Check commit diff");

                        if (gitCommitRepositoryFilter.isEnableCommitDiff()) {
                            calculateCommitDiff(commit, reader, diffFormatter, writer, commitUri);
                        }
                    }

                    if (log.isDebugEnabled())
                        log.debug("Ending commit writer rdf - loop finished");

                    writer.finish();

                    lockHandler.renewLockOnRenewTimeFulfillment();

                    if (finished) {
                        break;
                    }

                    if (iteration + 1 == Integer.MAX_VALUE) {
                        throw new RuntimeException(
                                "While iterating through commit log and transforming log to rdf: Exceeded iteration max count (integer overflow)");
                    }
                }
            }
            log.info("Git commit iterations finished");

            commitConversionWatch.stop();

            timeLog.setGitCommitConversionTime(commitConversionWatch.getTime());

            lockHandler.renewLockOnRenewTimeFulfillment();

            // Submodules

            // branch-snapshot
            // TODO: rename to 'blame'?

            if (gitCommitRepositoryFilter.isEnableBranchSnapshot()) {

                StopWatch branchSnapshottingWatch = new StopWatch();

                branchSnapshottingWatch.start();

                log.info("Start branch snapshotting");

                ObjectId headCommitId = gitRepository.resolve("HEAD");

                BranchSnapshotCalculator branchSnapshotCalculator = new BranchSnapshotCalculator(
                        writer,
                        gitRepository,
                        GithubUriUtils.getCommitUri(owner, repositoryName, headCommitId.getName()),
                        lockHandler);

                branchSnapshotCalculator.calculateBranchSnapshot();

                branchSnapshottingWatch.stop();

                timeLog.setGitBranchSnapshottingTime(branchSnapshottingWatch.getTime());
            } else {
                timeLog.setGitBranchSnapshottingTime(0L);

            }

            lockHandler.renewLockOnRenewTimeFulfillment();

            // issues
            StopWatch issueWatch = new StopWatch();
            issueWatch.start();
            int issuesProcessed = 0;

            if (githubIssueRepositoryFilter.doesContainAtLeastOneEnabledFilterOption()) {

                if (githubRepositoryHandle.hasIssues() && PROCESS_ISSUE_LIMIT > 0) {

                    log.info("Start issue processing with limit of {} issues", PROCESS_ISSUE_LIMIT);
                    int issueCounter = 0;
                    boolean doesWriterContainNonWrittenRdfStreamElements = false;

                    // Use smaller page size when processing few items
                    int pageSize = Math.min(PROCESS_ISSUE_LIMIT * 2, 100);

                    PagedIterable<GHIssue> issues = githubRepositoryHandle.queryIssues()
                            .state(GHIssueState.ALL)
                            .pageSize(pageSize)
                            .list();

                    for (GHIssue ghIssue : issues) {

                        if (issueCounter < 1) {
                            log.info("Start issue rdf conversion batch");
                            writer.start();
                            doesWriterContainNonWrittenRdfStreamElements = true;
                        }

                        // Early exit if we've hit our limit
                        if (issuesProcessed >= PROCESS_ISSUE_LIMIT) {
                            log.info("Reached issue processing limit of {}", PROCESS_ISSUE_LIMIT);
                            break;
                        }

                        int issueNumber = ghIssue.getNumber();
                        String issueUri = ghIssue.getHtmlUrl().toString();

                        if (issueUri == null || issueUri.isEmpty()) {
                            log.warn(
                                    "Issue with number {} fallback to githubRepositoryURI because its issueUri is null or empty",
                                    issueNumber);
                            issueUri = GithubUriUtils.getRepositoryUri(owner, repositoryName);
                        }

                        if (!githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() == null) {
                            continue;
                        }

                        // ********************** ** REMOVE ON DEPLOYMENT ** **********************
                        ZonedDateTime oneYearAgo = ZonedDateTime.now().minusYears(1);
                        // REMOVE ON DEPLOYMENT
                        Date dcreatedAt = ghIssue.getCreatedAt();
                        if (dcreatedAt == null || dcreatedAt.toInstant().isBefore(oneYearAgo.toInstant())) {
                            continue;
                        }

                        if ( PROCESS_ISSUE_ONLY.length > 0) {
                            boolean shouldProcessIssue = false;
                            for (String issueId : PROCESS_ISSUE_ONLY) {
                                if (issueId.equals(String.valueOf(issueNumber))) {
                                    shouldProcessIssue = true;
                                    break;
                                }
                            }
                            if (!shouldProcessIssue) {
                                continue; // Skip this issue
                            }
                        }
                        // ********************** ** **************** ** **********************

                        writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(issueUri));

                        if (githubIssueRepositoryFilter.isEnableIssueNumber()) {
                            writer.triple(RdfGithubIssueUtils.createIssueNumberProperty(issueUri, issueNumber));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueTitle() && ghIssue.getTitle() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueTitleProperty(issueUri, ghIssue.getTitle()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueBody() && ghIssue.getBody() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueBodyProperty(issueUri, ghIssue.getBody()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueStateProperty(
                                    issueUri, ghIssue.getState().toString()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUser() && ghIssue.getUser() != null) {
                            String githubIssueUserUri = ghIssue.getUser().getHtmlUrl().toString();
                            writer.triple(
                                    RdfGithubIssueUtils.createIssueUserProperty(issueUri, githubIssueUserUri));
                        }



                        if (githubIssueRepositoryFilter.isEnableIssueReviewers() && ghIssue.isPullRequest()) {
                            GHPullRequest pr = getPullRequestCached(
                                    ghIssue.getRepository(), issueNumber);
                            List<GHUser> reviewers = pr.getRequestedReviewers();
                            for (GHUser reviewer : reviewers) {
                                String reviewerUri = reviewer.getHtmlUrl().toString();
                                writer.triple(RdfGithubIssueUtils.createIssueRequestedReviewerProperty(issueUri, reviewerUri));
                            }
                        }


                        if (githubIssueRepositoryFilter.isEnableIssueMilestone()) {
                            GHMilestone issueMilestone = ghIssue.getMilestone();
                            if (issueMilestone != null) {
                                writer.triple(RdfGithubIssueUtils.createIssueMilestoneProperty(issueUri,
                                        ghIssue.getMilestone().getHtmlUrl().toString()));
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueCreatedAt() && ghIssue.getCreatedAt() != null) {
                            LocalDateTime createdAt = localDateTimeFrom(ghIssue.getCreatedAt());
                            writer.triple(RdfGithubIssueUtils.createIssueSubmittedAtProperty(issueUri, createdAt));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUpdatedAt()) {
                            Date updatedAtUtilDate = ghIssue.getUpdatedAt();
                            if (updatedAtUtilDate != null) {
                                LocalDateTime updatedAt = localDateTimeFrom(updatedAtUtilDate);
                                writer.triple(
                                        RdfGithubIssueUtils.createIssueUpdatedAtProperty(issueUri, updatedAt));
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueClosedAt()) {
                            Date closedAtUtilDate = ghIssue.getClosedAt();
                            if (closedAtUtilDate != null) {
                                LocalDateTime closedAt = localDateTimeFrom(closedAtUtilDate);
                                writer.triple(
                                        RdfGithubIssueUtils.createIssueClosedAtProperty(issueUri, closedAt));
                            }
                        }
                        // SHIT
                        if (githubIssueRepositoryFilter.isEnableIssueMergedBy()) {
                            if (ghIssue.isPullRequest()) {
                                GHPullRequest pullRequest = getPullRequestCached(
                                        githubRepositoryHandle, issueNumber);
                                writeMergeInfo(ghIssue, pullRequest, writer, issueUri);
                                writeWorkflowRunInfo(pullRequest, writer, issueUri, repositoryUri);
                            }
                        }

                        // GitHub issues can have multiple assignees
                        if (githubIssueRepositoryFilter.isEnableIssueAssignees()) {
                            List<GHUser> assignees = ghIssue.getAssignees();
                            for (GHUser assignee : assignees) {
                                String assigneeUri = assignee.getHtmlUrl().toString();
                                writer.triple(RdfGithubIssueUtils.createIssueAssigneeProperty(issueUri, assigneeUri));
                            }
                        }



                        // Reviews
                        if (githubIssueRepositoryFilter.isEnableIssueReviewers() && ghIssue.isPullRequest()) {
                            GHPullRequest pr = getPullRequestCached(
                                    ghIssue.getRepository(), issueNumber);
                            List<GHPullRequestReview> reviews = getReviewsCached(pr);

                            for (GHPullRequestReview review : reviews) {
                                long reviewId = review.getId();
                                if (!seenReviewIds.add(reviewId)) {
                                    continue;
                                }
                                String reviewURI = GithubUriUtils.getIssueReviewUri(issueUri, String.valueOf(pr.getId()), String.valueOf(reviewId));
                                String reviewUrl = GithubUriUtils.getIssueReviewUrl(issueUri, String.valueOf(reviewId));
                                // String reviewURL = review.getUrl().toString();
                                // String reviewUri = issueUri + "/reviews/" + reviewId;

                                // Static Properties
                                writer.triple(RdfGithubIssueReviewUtils.createIssueReviewProperty(issueUri, reviewURI));
                                writer.triple(RdfGithubIssueReviewUtils.createReviewUrlProperty(reviewURI, reviewUrl));
                                writer.triple(RdfGithubIssueReviewUtils.createIssueReviewRdfTypeProperty(reviewURI));
                                writer.triple(
                                        RdfGithubIssueReviewUtils.createReviewIdentifierProperty(reviewURI, reviewId));
                                writer.triple(RdfGithubIssueReviewUtils.createReviewOfProperty(reviewURI, issueUri));

                                // Dynamic Properties 
                                if (review.getBody() != null && !review.getBody().isEmpty()) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewDescriptionProperty(
                                        reviewURI, review.getBody()));
                                }
                                if (review.getState() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewStateProperty(
                                        reviewURI, review.getState().toString()));
                                }
                                if (review.getSubmittedAt() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewSubmittedAtProperty(
                                        reviewURI, localDateTimeFrom(review.getSubmittedAt())));
                                }
                                if (review.getUser() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewUserProperty(
                                        reviewURI, review.getUser().getHtmlUrl().toString()));
                                }
                                if (review.getCommitId() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewCommitIdProperty(
                                        reviewURI, review.getCommitId()));
                                }

                                // Review Comments
                                List<GHPullRequestReviewComment> reviewComments = getReviewCommentsCached(review);
                                int reviewCommentCount = reviewComments.size();

                                // If there are no review comments, skip further processing about comments for this review
                                if (reviewCommentCount == 0) {
                                    continue;
                                }

                                // Process the comments of the review
                                for (GHPullRequestReviewComment c : reviewComments) {
                                    long cid = c.getId();

                                    // Setup the URI and URL for a review comment of a pull request
                                    String reviewCommentURI = GithubUriUtils.getIssueReviewCommentUri(
                                            issueUri, String.valueOf(cid));
                                    String reviewCommentURL = GithubUriUtils.getIssueReviewCommentUrl(
                                        repositoryUri, String.valueOf(cid));
                                

                                    // Link into the Review the Comment
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewCommentProperty(
                                        reviewURI,
                                            reviewCommentURI));

                                    // Create the RDF triples for the review comment
                                    writer.triple(
                                            RdfGithubCommentUtils.createCommentHtmlUrl(reviewCommentURI, reviewCommentURL));
                                    writer.triple(RdfGithubCommentUtils.createCommentRdfType(reviewCommentURI));
                                    writer.triple(RdfGithubCommentUtils.createCommentId(reviewCommentURI, cid));
                                    writer.triple(RdfGithubCommentUtils.createCommentOf(reviewCommentURI, reviewURI));

                                    // Content and user
                                    if (c.getBody() != null && !c.getBody().isEmpty()) {
                                        writer.triple(
                                                RdfGithubCommentUtils.createCommentBody(reviewCommentURI, c.getBody()));
                                    }
                                    if (c.getUser() != null) {
                                        writer.triple(RdfGithubCommentUtils.createCommentUser(
                                                reviewCommentURI, c.getUser().getHtmlUrl().toString()));
                                    }
                                    if (c.getCreatedAt() != null) {
                                        writer.triple(RdfGithubCommentUtils.createCommentCreatedAt(
                                                reviewCommentURI, localDateTimeFrom(c.getCreatedAt())));
                                    }

                                    // HIERARCHY HANDLING - This was missing!
                                    Long parentId = c.getInReplyToId();
                                    boolean isRoot = (parentId == null || parentId.equals(cid) || parentId <= 0);

                                    // Mark if this is a root discussion
                                    writer.triple(RdfGithubCommentUtils.createIsRootComment(reviewCommentURI, isRoot));

                                    // If this is a reply, link to parent
                                    if (!isRoot && parentId != null && parentId > 0) {
                                        String parentCommentUri = GithubUriUtils.getIssueReviewCommentUri(
                                                repositoryUri, String.valueOf(parentId));

                                        writer.triple(RdfGithubCommentUtils.createParentComment(
                                                reviewCommentURI, parentCommentUri));

                                        // Also create the reverse relationship - parent has this as a reply
                                        writer.triple(RdfGithubCommentUtils.createHasReply(
                                                parentCommentUri, reviewCommentURI));
                                    }
                                    
                                    // Reactions
                                    List<GHReaction> reactions = getReviewCommentReactionsCached(c);
                                    writer.triple(
                                            RdfGithubCommentUtils.createReactionCount(
                                                    reviewCommentURI, reactions.size()));
                                    for (GHReaction r : reactions) {
                                        String reactionURI = GithubUriUtils.getIssueReviewCommentReactionUri(
                                                reviewCommentURI, String.valueOf(r.getId()));
                    
                                        writer.triple(RdfGithubCommentUtils.createCommentReaction(
                                                reviewCommentURI,
                                                reactionURI));
                                        writer.triple(
                                                RdfGithubReactionUtils.createReactionRdfTypeProperty(reactionURI));
                                        writer.triple(RdfGithubReactionUtils.createReactionIdProperty(reactionURI,
                                                r.getId()));
                                        writer.triple(RdfGithubReactionUtils.createReactionOfProperty(reactionURI,
                                                reviewCommentURI));
                                        
                                        if (r.getContent() != null) {
                                            writer.triple(RdfGithubReactionUtils.createReactionContentProperty(
                                                    reactionURI, r.getContent().toString()));
                                        }
                                        if (r.getUser() != null) {
                                            writer.triple(RdfGithubReactionUtils.createReactionUserProperty(reactionURI,
                                                    r.getUser().getHtmlUrl().toString()));
                                        }
                                        if (r.getCreatedAt() != null) {
                                            writer.triple(RdfGithubReactionUtils.createReactionCreatedAtProperty(
                                                    reactionURI, localDateTimeFrom(r.getCreatedAt())));
                                        }
                                    }
                                }
                            }
                        }

                        // Normal Issue Comments
                        if (githubIssueRepositoryFilter.isEnableIssueComments()) {
                            List<GHIssueComment> issueComments = getIssueCommentsCached(ghIssue);
                            for (GHIssueComment c : issueComments) {
                                long cid = c.getId();
                                String issueCommentURI = GithubUriUtils.getIssueCommentUri(repositoryUri,
                                        String.valueOf(cid));
                                String issueCommentURL = GithubUriUtils.getIssueCommentUrl(issueUri, String.valueOf(cid));


                                // Link in Issue to Comment
                                writer.triple(RdfGithubIssueReviewUtils.createReviewCommentProperty(issueUri,
                                        issueCommentURI));
                                writer.triple(
                                        RdfGithubCommentUtils.createCommentHtmlUrl(issueCommentURI, issueCommentURL));
                                writer.triple(RdfGithubCommentUtils.createCommentRdfType(issueCommentURI));
                                writer.triple(RdfGithubCommentUtils.createCommentId(issueCommentURI, cid));
                                writer.triple(RdfGithubCommentUtils.createCommentOf(issueCommentURI, issueUri));

                                if (c.getBody() != null && !c.getBody().isEmpty()) {
                                    writer.triple(RdfGithubCommentUtils.createCommentBody(
                                            issueCommentURI, c.getBody()));
                                }
                                if (c.getUser() != null) {
                                    writer.triple(RdfGithubCommentUtils.createCommentUser(
                                            issueCommentURI,
                                            c.getUser().getHtmlUrl().toString()));
                                }
                                if (c.getCreatedAt() != null) {
                                    writer.triple(RdfGithubCommentUtils.createCommentCreatedAt(
                                            issueCommentURI,
                                            localDateTimeFrom(c.getCreatedAt())));
                                }
                                // Issue comments have no threading, treat all as root
                                writer.triple(RdfGithubCommentUtils.createIsRootComment(issueCommentURI, true));

                                // Reactions
                                List<GHReaction> reactions = getIssueCommentReactionsCached(c);
                                writer.triple(RdfGithubCommentUtils.createReactionCount(issueCommentURI, reactions.size()));
                                for (GHReaction r : reactions) {

                                    String reactionURI = GithubUriUtils.getIssueCommentReactionUri(issueCommentURI,
                                            String.valueOf(r.getId()));
                                    
                                    writer.triple(RdfGithubCommentUtils.createCommentReaction(issueCommentURI, 
                                            reactionURI));
                                    writer.triple(RdfGithubReactionUtils.createReactionRdfTypeProperty(reactionURI));
                                    writer.triple(RdfGithubReactionUtils.createReactionIdProperty(
                                            reactionURI, r.getId()));
                                    writer.triple(RdfGithubReactionUtils.createReactionOfProperty(
                                            reactionURI, issueCommentURI));
                                    if (r.getContent() != null) {
                                        writer.triple(RdfGithubReactionUtils.createReactionContentProperty(
                                                reactionURI, r.getContent().toString()));
                                    }
                                    if (r.getUser() != null) {
                                        writer.triple(RdfGithubReactionUtils.createReactionUserProperty(
                                                reactionURI, r.getUser().getHtmlUrl().toString()));
                                    }
                                    if (r.getCreatedAt() != null) {
                                        writer.triple(RdfGithubReactionUtils.createReactionCreatedAtProperty(
                                                reactionURI, localDateTimeFrom(r.getCreatedAt())));
                                    }
                                }
                            }
                        }

                        // Count the iteration of issues
                        issueCounter++;
                        // Count the outside of limit the number of issues to process
                        issuesProcessed++;

                        if (issueCounter > 100) {
                            log.info("Finish issue rdf conversion batch");
                            writer.finish();
                            doesWriterContainNonWrittenRdfStreamElements = false;
                            issueCounter = 0;
                            lockHandler.renewLockOnRenewTimeFulfillment();
                        } else {
                            log.info("Processed issue #{} with id {} and uri '{}'", issueCounter, issueUri);
                        }
                    }

                    if (doesWriterContainNonWrittenRdfStreamElements) {
                        log.info("Finish last issue rdf conversion batch");
                        writer.finish();
                    }
                }
            }

            issueWatch.stop();
            lockHandler.renewLockOnRenewTimeFulfillment();
            timeLog.setGithubIssueConversionTime(issueWatch.getTime());

        }

        log.info("Finished overall processing. Start to load rdf file into postgres blob storage");
        // Tidy up the Turtle RDF
        RdfTurtleTidier.tidyFile(rdfTempFile);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(rdfTempFile));

        entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, rdfTempFile.length()));

        return bufferedInputStream;
    }

    private Map<String, PullRequestInfo> buildCommitPrMap(GHRepository repo) throws IOException, InterruptedException {
        Map<String, PullRequestInfo> map = new HashMap<>();

        try {
            // If we're not processing any issues, don't build the map at all
            if (PROCESS_ISSUE_LIMIT <= 0) {
                log.info("Issue processing disabled, skipping commit-PR mapping");
                return map;
            }

            // Limit PR fetching based on issue limits
            int maxPRsToFetch = Math.min(PROCESS_ISSUE_LIMIT * 2, 100); // Reasonable multiplier

            PagedIterable<GHPullRequest> prs = executeWithRetry(
                    () -> repo.queryPullRequests().state(GHIssueState.CLOSED).list(),
                    "queryPullRequests");

            ZonedDateTime oneYearAgo = ZonedDateTime.now().minusYears(1);
            int prsProcessed = 0;

            for (GHPullRequest pr : prs) {
                if (prsProcessed >= maxPRsToFetch) {
                    log.info("Reached max PRs limit ({}) for commit mapping", maxPRsToFetch);
                    break;
                }

                if (!pr.isMerged()) {
                    continue;
                }

                Date merged = pr.getMergedAt();
                if (merged == null || merged.toInstant().isBefore(oneYearAgo.toInstant())) {
                    continue;
                }

                // If we have specific issues to process, only map those PRs
                if (PROCESS_ISSUE_ONLY.length > 0) {
                    boolean shouldIncludePR = false;
                    String prNumber = String.valueOf(pr.getNumber());
                    for (String issueId : PROCESS_ISSUE_ONLY) {
                        if (issueId.equals(prNumber)) {
                            shouldIncludePR = true;
                            break;
                        }
                    }
                    if (!shouldIncludePR) {
                        continue;
                    }
                }

                String prUri = GithubUriUtils.getIssueUri(repo.getOwnerName(), repo.getName(),
                        String.valueOf(pr.getNumber()));
                LocalDateTime mergedAt = localDateTimeFrom(merged);

                PullRequestInfo info = new PullRequestInfo(prUri, pr.getMergeCommitSha(), mergedAt);

                if (pr.getMergeCommitSha() != null) {
                    map.put(pr.getMergeCommitSha(), info);
                }

                // Only fetch commits if we're processing commits too
                if (PROCESS_COMMIT_LIMIT > 0) {
                    try {
                        for (GHPullRequestCommitDetail c : getCommitsCached(pr)) {
                            map.put(c.getSha(), info);
                        }
                    } catch (IOException | InterruptedException e) {
                        log.warn("Error fetching commits for PR {}: {}", pr.getNumber(), e.getMessage());
                        // Continue processing other PRs
                    }
                }

                prsProcessed++;
            }

            log.info("Built commit-PR mapping with {} PRs", prsProcessed);
            return map;

        } catch (IOException e) {
            log.error("IO error while building commit-PR mapping: {}", e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            log.error("Interrupted while building commit-PR mapping: {}", e.getMessage());
            throw e;
        }
    }

    public static Map<ObjectId, List<String>> getTagsForCommits(Repository repository) throws IOException {

        Map<ObjectId, List<String>> commitToTags = new HashMap<>();
        Map<String, Ref> tags = repository.getRefDatabase().getRefs(Constants.R_TAGS);
        RevWalk revWalk = new RevWalk(repository);

        try {
            for (Map.Entry<String, Ref> entry : tags.entrySet()) {

                String tagName = entry.getKey();
                Ref tagRef = entry.getValue();

                try {
                    RevObject obj = revWalk.parseAny(tagRef.getObjectId());
                    if (obj instanceof RevTag) {
                        RevTag tag = (RevTag) obj;
                        obj = revWalk.peel(tag);
                    }

                    if (obj instanceof RevCommit) {
                        ObjectId commitId = obj.getId();
                        commitToTags.computeIfAbsent(commitId, k -> new ArrayList<>()).add(tagName);
                        log.debug("Found Tag '{}' for commit #{}", tagName, commitId.getName());
                    }
                } catch (IOException e) {
                    log.error("Error processing tag " + tagName + ": " + e.getMessage());
                }
            }
        } finally {
            revWalk.dispose();
        }

        return commitToTags;
    }

    private void calculateAuthorEmail(
            PersonIdent authorIdent,
            Map<String, RdfGitCommitUserUtils> uniqueGitCommiterWithHash,
            StreamRDF writer,
            String commitUri,
            String gitHash,
            GHRepository githubRepositoryHandle) {

        if (authorIdent == null) {
            return;
        }

        String email = authorIdent.getEmailAddress();

        if (log.isDebugEnabled())
            log.debug("Set rdf github user in commit");

        GithubUserInfo info = RdfGitCommitUserUtils.getGitHubUserInfoFromCommit(githubRepositoryHandle, gitHash);

        if (!uniqueGitCommiterWithHash.containsKey(email)) {
            String uri = info == null ? null : info.uri;
            uniqueGitCommiterWithHash.put(email, new RdfGitCommitUserUtils(gitHash, uri));
        }

        String userUri = info == null ? null : info.uri;
        if (userUri != null && !userUri.isEmpty()) {
            writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, userUri));
            writer.triple(RdfGithubUserUtils.createGitHubUserType(userUri));
            if (info != null) {
                writer.triple(RdfGithubUserUtils.createUserIdProperty(userUri, info.userId));
                if (info.login != null && !info.login.isEmpty()) {
                    writer.triple(RdfGithubUserUtils.createLoginProperty(userUri, info.login));
                }  
                if (info.name != null && !info.name.isEmpty()) {
                    writer.triple(RdfGithubUserUtils.createNameProperty(userUri, info.name));
                }
            }
        }

        if (log.isDebugEnabled())
            log.debug("Set RDF author email property");

        writer.triple(RdfCommitUtils.createAuthorEmailProperty(commitUri, email));
    }

    private void calculateAuthorName(StreamRDF writer, String commitUri, PersonIdent authorIdent) {

        if (authorIdent == null) {
            return;
        }

        writer.triple(RdfCommitUtils.createAuthorNameProperty(commitUri, authorIdent.getName()));
    }

    private void calculateCommitterName(StreamRDF writer, String commitUri, PersonIdent committerIdent) {

        if (committerIdent == null) {
            return;
        }

        writer.triple(RdfCommitUtils.createCommitterNameProperty(commitUri, committerIdent.getName()));
    }

    private void calculateCommitMessage(StreamRDF writer, String commitUri, RevCommit commit) {

        try {

            writer.triple(RdfCommitUtils.createCommitMessageProperty(commitUri, commit.getFullMessage()));

        } catch (IllegalCharsetNameException ex) {

            log.warn("Skipping commit message for commit with hash '{}'. " +
                    "Error occurred regarding an illegal charset. " +
                    "Error is '{}'",
                    commit.getName(),
                    ex.getMessage(),
                    ex);

        }

    }

    private void calculateCommitIssues(StreamRDF writer, String commitUri, String message,
            String owner, String repositoryName, PullRequestInfo prInfo) {
        Set<String> issueNumbers = RdfCommitUtils.extractIssueNumbers(message);

        if (prInfo != null) {
            String prNumber = extractIssueNumberFromUri(prInfo.issueUri);
            issueNumbers.remove(prNumber);
        }

        for (String number : issueNumbers) {
            String issueUri = GithubUriUtils.getIssueUri(owner, repositoryName, number);
            writer.triple(RdfGithubIssueUtils.createIssueReferencedByProperty(issueUri, commitUri));
            // keep legacy predicate
            writer.triple(RdfCommitUtils.createCommitIssueProperty(commitUri, issueUri));
        }
    }

    private String extractIssueNumberFromUri(String uri) {
        int idx = uri.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    private void calculateCommitterEmail(StreamRDF writer, String commitUri, PersonIdent committerIdent) {

        if (committerIdent == null) {
            return;
        }

        writer.triple(RdfCommitUtils.createCommitterEmailProperty(commitUri, committerIdent.getEmailAddress()));
    }

    private void calculateCommitBranch(
            CommitBranchCalculator commitBranchCalculator,
            StreamRDF writer,
            RevCommit currentCommit,
            String commitUri,
            String owner,
            String repositoryName) {

        String commitHash = currentCommit.getName();

        List<String> branches = commitBranchCalculator.getBranchesForShaHashOfCommit(commitHash);

        for (String branchName : branches) {
            String branchUri = GithubUriUtils.getBranchUri(owner, repositoryName, branchName);
            writer.triple(RdfCommitUtils.createCommitInBranchProperty(commitUri, branchUri));
        }

    }

    private void calculateCommitDiff(
            RevCommit commit,
            ObjectReader currentRepositoryObjectReader,
            DiffFormatter currentRepositoryDiffFormatter,
            StreamRDF writer,
            String commitUri) throws IOException {

        int parentCommitCount = commit.getParentCount();

        if (log.isDebugEnabled())
            log.debug("Commit diff is enabled - parent count is '{}'", parentCommitCount);

        if (parentCommitCount > 0) {

            RevCommit parentCommit = commit.getParent(0);

            if (log.isDebugEnabled())
                log.debug("Check if parent commit is null");

            if (parentCommit != null) {
                CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
                CanonicalTreeParser currentTreeParser = new CanonicalTreeParser();

                if (log.isDebugEnabled())
                    log.debug("Reset tree parsers - starting with parent tree parser");
                parentTreeParser.reset(currentRepositoryObjectReader, parentCommit.getTree());

                if (log.isDebugEnabled())
                    log.debug("Reset tree parsers - continuing with current tree parser");
                currentTreeParser.reset(currentRepositoryObjectReader, commit.getTree());

                //Resource commitResource = ResourceFactory.createResource(gitHash); // TODO: use proper uri?
                //Node commitNode = commitResource.asNode();
                //writer.triple(RdfCommitUtils.createCommitResource(commitUri, commitNode));

                if (log.isDebugEnabled())
                    log.debug("Scan diff entries");

                List<DiffEntry> diffEntries = currentRepositoryDiffFormatter.scan(parentTreeParser, currentTreeParser);

                if (log.isDebugEnabled())
                    log.debug("Loop through diff entries. Diff entry list size is '{}'", diffEntries.size());

                for (DiffEntry diffEntry : diffEntries) {
                    Resource diffEntryResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":entry"*/);
                    Node diffEntryNode = diffEntryResource.asNode();
                    //writer.triple(RdfCommitUtils.createCommitDiffEntryResource(commitNode, diffEntryNode));

                    if (log.isDebugEnabled())
                        log.debug("Set RDF commit diff entry property");

                    writer.triple(RdfCommitUtils.createCommitDiffEntryProperty(commitUri, diffEntryNode));

                    DiffEntry.ChangeType changeType = diffEntry.getChangeType(); // ADD,DELETE,MODIFY,RENAME,COPY

                    if (log.isDebugEnabled())
                        log.debug("Set RDF commit diff entry edit type property");

                    writer.triple(RdfCommitUtils.createCommitDiffEntryEditTypeProperty(diffEntryNode, changeType));

                    FileHeader fileHeader = currentRepositoryDiffFormatter.toFileHeader(diffEntry);

                    if (log.isDebugEnabled())
                        log.debug("Switch through diff entry change type");

                    // See: org.eclipse.jgit.diff.DiffEntry.ChangeType.toString()
                    switch (changeType) {
                        case ADD:
                            if (log.isDebugEnabled())
                                log.debug("Set RDF ADD commit diff entry new file name property");
                            writer.triple(
                                    RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        case COPY:
                        case RENAME:
                            if (log.isDebugEnabled())
                                log.debug("Set RDF COPY/RENAME commit diff entry new file name property");
                            writer.triple(
                                    RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                            writer.triple(
                                    RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        case DELETE:
                        case MODIFY:
                            if (log.isDebugEnabled())
                                log.debug("Set RDF DELETE/MODIFY commit diff entry new file name property");
                            writer.triple(
                                    RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected changeType: " + changeType);
                    }

                    // Diff Lines (added/changed/removed)

                    if (log.isDebugEnabled())
                        log.debug("Retrieve file header edit list");

                    EditList editList = fileHeader.toEditList();

                    if (log.isDebugEnabled())
                        log.debug("Loop trough edit list. There are '{}' edit list entries", editList.size());

                    for (Edit edit : editList) {
                        Resource editResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":edit"*/);
                        Node editNode = editResource.asNode();

                        if (log.isDebugEnabled())
                            log.debug("Set RDF commit diff edit resource");

                        writer.triple(RdfCommitUtils.createCommitDiffEditResource(diffEntryNode, editNode));

                        Edit.Type editType = edit.getType(); // INSERT,DELETE,REPLACE

                        if (log.isDebugEnabled())
                            log.debug("Set RDF commit diff edit type property");

                        writer.triple(RdfCommitUtils.createCommitDiffEditTypeProperty(editNode, editType));

                        if (log.isDebugEnabled())
                            log.debug("Retrieve for file diffs the old and new line number beginnings and endings");

                        final int oldLinenumberBegin = edit.getBeginA();
                        final int newLinenumberBegin = edit.getBeginB();
                        final int oldLinenumberEnd = edit.getEndA();
                        final int newLinenumberEnd = edit.getEndB();

                        if (log.isDebugEnabled())
                            log.debug("Set RDF edit old line number begin property");
                        writer.triple(
                                RdfCommitUtils.createEditOldLinenumberBeginProperty(editNode, oldLinenumberBegin));

                        if (log.isDebugEnabled())
                            log.debug("Set RDF edit new line number begin property");
                        writer.triple(
                                RdfCommitUtils.createEditNewLinenumberBeginProperty(editNode, newLinenumberBegin));

                        if (log.isDebugEnabled())
                            log.debug("Set RDF edit old line number end property");
                        writer.triple(RdfCommitUtils.createEditOldLinenumberEndProperty(editNode, oldLinenumberEnd));

                        if (log.isDebugEnabled())
                            log.debug("Set RDF edit new line number end property");
                        writer.triple(RdfCommitUtils.createEditNewLinenumberEndProperty(editNode, newLinenumberEnd));
                    }
                }
            }
        }

    }

    private int calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(
            int currentIteration, int commitsPerIteration) {

        long longCurrentIteration = (long) currentIteration;
        long longCommitsPerIteration = (long) commitsPerIteration;

        long skips = longCurrentIteration * longCommitsPerIteration;

        if (skips > Integer.MAX_VALUE) {
            throw new RuntimeException(
                    "While iterating through commit log and transforming log to rdf: Exceeded skip max count (integer overflow)");
        }

        return (int) skips;
    }



    private LocalDateTime localDateTimeFrom(Date utilDate) {
        return localDateTimeFrom(utilDate.getTime());
    }

    private LocalDateTime localDateTimeFrom(int secondsSinceEpoch) {
        Instant instant = Instant.ofEpochSecond(secondsSinceEpoch);
        return instant.atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();
    }

    private LocalDateTime localDateTimeFrom(long milliSecondsSinceEpoch) {
        Instant instant = Instant.ofEpochMilli(milliSecondsSinceEpoch);
        return instant.atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();
    }

    // Github Workflows

    private void writeWorkflowRunData(String repositoryUri, GHWorkflowRun run, StreamRDF writer, String issueUri, String mergeSha)
            throws IOException, InterruptedException {

        String runUri = GithubUriUtils.getWorkflowRunUri(repositoryUri, String.valueOf(run.getId()));

        // Write workflow run properties
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunProperty(issueUri, runUri));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunRdfTypeProperty(runUri));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunIdProperty(runUri, run.getId()));

        // Batch property writes to reduce overhead
        writeWorkflowRunProperties(run, writer, runUri, mergeSha);

        // Optimization 2: Only fetch jobs if needed, with pagination control
        if (shouldIncludeJobDetails(run)) {
            writeWorkflowJobData(run, writer, runUri, repositoryUri);
        }
    }

    private void writeWorkflowRunProperties(GHWorkflowRun run, StreamRDF writer, String runUri, String mergeSha) {
        // Group property writes together for better performance
        if (run.getName() != null) {
            writer.triple(RdfGithubWorkflowUtils.createWorkflowNameProperty(runUri, run.getName()));
        }
        if (run.getStatus() != null) {
            writer.triple(RdfGithubWorkflowUtils.createWorkflowStatusProperty(runUri, run.getStatus()));
        }
        if (run.getConclusion() != null) {
            writer.triple(RdfGithubWorkflowUtils.createWorkflowConclusionProperty(runUri, run.getConclusion()));
        }
        if (run.getEvent() != null) {
            writer.triple(RdfGithubWorkflowUtils.createWorkflowEventProperty(runUri, run.getEvent().toString()));
        }

        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunNumberProperty(runUri, run.getRunNumber()));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowCommitShaProperty(runUri, mergeSha));
        try {
            if (run.getCreatedAt() != null) {
                writer.triple(RdfGithubWorkflowUtils.createWorkflowCreatedAtProperty(runUri,
                        localDateTimeFrom(run.getCreatedAt())));
            }
            if (run.getUpdatedAt() != null) {
                writer.triple(RdfGithubWorkflowUtils.createWorkflowUpdatedAtProperty(runUri,
                        localDateTimeFrom(run.getUpdatedAt())));
            }
        } catch (IOException e) {
            log.warn("Error while writing workflow run properties for run {}: {}", run.getId(), e.getMessage());
        }
    }

    private void writeWorkflowJobData(GHWorkflowRun run, StreamRDF writer, String runUri, String repositoryUri)
            throws IOException, InterruptedException {

        // Further limit job processing based on issue limits
        int maxJobsToProcess = Math.min(PROCESS_ISSUE_LIMIT * 5, 50);

        PagedIterable<GHWorkflowJob> jobIterable = executeWithRetry(
                () -> run.listJobs().withPageSize(10), // Smaller page size
                "listJobs for run " + run.getId());

        int jobsProcessed = 0;

        for (GHWorkflowJob job : jobIterable) {
            if (jobsProcessed >= maxJobsToProcess) {
                log.debug("Reached max jobs limit ({}) for workflow run {}", maxJobsToProcess, run.getId());
                break;
            }

            writeJobProperties(job, writer, runUri, repositoryUri);
            jobsProcessed++;
        }

        log.debug("Processed {} jobs for workflow run {}", jobsProcessed, run.getId());
    }

    private void writeJobProperties(GHWorkflowJob job, StreamRDF writer, String runUri, String repositoryUri) {
        String jobUri = GithubUriUtils.getWorkflowJobUri(runUri, job.getId());
        String jobUrl = GithubUriUtils.getWorkflowJobUrl(repositoryUri, job.getId());

        writer.triple(RdfGithubWorkflowUtils.createWorkflowJobProperty(runUri, jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobRdfTypeProperty(jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobIdProperty(jobUri, job.getId()));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobUrlProperty(jobUri, jobUrl));

        if (job.getName() != null) {
            writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobNameProperty(jobUri, job.getName()));
        }
        if (job.getStatus() != null) {
            writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobStatusProperty(jobUri, job.getStatus()));
        }
        if (job.getConclusion() != null) {
            writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobConclusionProperty(jobUri, job.getConclusion()));
        }
        if (job.getStartedAt() != null) {
            writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobStartedAtProperty(jobUri,
                    localDateTimeFrom(job.getStartedAt())));
        }
        if (job.getCompletedAt() != null) {
            writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobCompletedAtProperty(jobUri,
                    localDateTimeFrom(job.getCompletedAt())));
        }

        // Write step names with minimal information
        List<Step> steps = job.getSteps();

        if (steps != null) {
            steps.stream()
                    .filter(s -> s.getName() != null) // skip null names
                    .forEachOrdered(s -> {
                        // build an IRI that is unique inside the job
                        // use the step number if the API provides it; fall back to the stream index
                        String stepUri = RdfGithubWorkflowStepUtils
                                .createWorkflowStepUri(jobUri, s.getNumber()).toString();
                        writer.triple(
                                RdfGithubWorkflowJobUtils.createWorkflowJobStepProperty(jobUri, stepUri));
                    });
        }


        if (steps != null) {
            for (Step step : steps) {
                String stepUrl = RdfGithubWorkflowStepUtils.createWorkflowStepUrl(repositoryUri, job.getId(), step.getNumber()).toString();
                String stepUri = RdfGithubWorkflowStepUtils.createWorkflowStepUri(jobUri, step.getNumber()).toString();

                writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepJobUrlProperty(stepUri, stepUrl));
                writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepRdfTypeProperty(stepUri));
                writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepNumberProperty(stepUri, step.getNumber()));
                if (step.getName() != null) {
                    writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepNameProperty(stepUri, step.getName()));
                }
                if (step.getStartedAt() != null) {
                    writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepStartedAtProperty(stepUri,
                            step.getStartedAt().toString()));
                }
                if (step.getCompletedAt() != null) {
                    writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepCompletedAtProperty(stepUri,
                            step.getCompletedAt().toString()));
                }
            }
        }
    }

    private boolean shouldIncludeJobDetails(GHWorkflowRun run) {
        return run.getConclusion() != null &&
                (run.getConclusion().equals(
                        GHWorkflowRun.Conclusion.SUCCESS)
                        || run.getConclusion().equals(GHWorkflowRun.Conclusion.FAILURE)
                        );
    }

    private GHPullRequest getPullRequestCached(GHRepository repo, int number)
            throws IOException, InterruptedException {
        GHPullRequest pr = pullRequestCache.get(number);
        if (pr == null) {
            pr = executeWithRetry(() -> repo.getPullRequest(number), "getPullRequest " + number);
            pullRequestCache.put(number, pr);
        }
        return pr;
    }

    private List<GHPullRequestReview> getReviewsCached(GHPullRequest pr)
            throws IOException, InterruptedException {
        int number = pr.getNumber();
        List<GHPullRequestReview> reviews = reviewCache.get(number);
        if (reviews == null) {
            reviews = executeWithRetry(() -> pr.listReviews().toList(), "listReviews for PR " + number);
            reviewCache.put(number, reviews);
        }
        return reviews;
    }

    private List<GHPullRequestReviewComment> getReviewCommentsCached(GHPullRequestReview review)
            throws IOException, InterruptedException {
        long id = review.getId();
        List<GHPullRequestReviewComment> comments = reviewCommentsCache.get(id);
        if (comments == null) {
            comments = executeWithRetry(() -> review.listReviewComments().toList(),
                    "listReviewComments for review " + id);
            reviewCommentsCache.put(id, comments);
        }
        return comments;
    }

    private List<GHReaction> getReviewCommentReactionsCached(GHPullRequestReviewComment comment)
            throws IOException, InterruptedException {
        long id = comment.getId();
        List<GHReaction> reactions = reviewCommentReactionsCache.get(id);
        if (reactions == null) {
            reactions = executeWithRetry(() -> comment.listReactions().toList(),
                    "listReactions for review comment " + id);
            reviewCommentReactionsCache.put(id, reactions);
        }
        return reactions;
    }

    private List<GHIssueComment> getIssueCommentsCached(GHIssue issue)
            throws IOException, InterruptedException {
        int number = issue.getNumber();
        List<GHIssueComment> comments = issueCommentsCache.get(number);
        if (comments == null) {
            comments = executeWithRetry(() -> issue.listComments().toList(),
                    "listComments for issue " + number);
            issueCommentsCache.put(number, comments);
        }
        return comments;
    }

    private List<GHReaction> getIssueCommentReactionsCached(GHIssueComment comment)
            throws IOException, InterruptedException {
        long id = comment.getId();
        List<GHReaction> reactions = issueCommentReactionsCache.get(id);
        if (reactions == null) {
            reactions = executeWithRetry(() -> comment.listReactions().toList(),
                    "listReactions for issue comment " + id);
            issueCommentReactionsCache.put(id, reactions);
        }
        return reactions;
    }

    private List<GHPullRequestCommitDetail> getCommitsCached(GHPullRequest pr)
            throws IOException, InterruptedException {
        int number = pr.getNumber();
        List<GHPullRequestCommitDetail> commits = commitCache.get(number);
        if (commits == null) {
            commits = executeWithRetry(() -> pr.listCommits().toList(), "listCommits for PR " + pr.getNumber());
            commitCache.put(number, commits);
        }
        return commits;
    }

    private <T> T executeWithRetry(java.util.concurrent.Callable<T> callable, String description)
            throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return callable.call();
            } catch (IOException e) {
                last = e;
                if (attempt == 2) {
                    throw e;
                }
                log.warn("{} failed on attempt {}/2: {}. Retrying after delay...", description, attempt, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            } catch (Exception e) {
                if (attempt == 2) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    throw new RuntimeException(e);
                }
                log.warn("{} failed on attempt {}/2: {}. Retrying after delay...", description, attempt, e.getMessage());
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
        throw last == null ? new IOException("Unknown error in executeWithRetry") : last;
    }
}
