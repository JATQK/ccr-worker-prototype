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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
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
import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import de.leipzig.htwk.gitrdf.worker.timemeasurement.TimeLog;
import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfTurtleTidier;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.git.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.git.RdfGitCommitUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.GithubUserInfo;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.GithubUserValidator;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubCommentUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubIssueReviewUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubIssueUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubPullRequestUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubReactionUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubWorkflowJobUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubWorkflowStepUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.RdfGithubWorkflowUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformCommentUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformLabelUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformMilestoneUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformTicketUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformWorkflowExecutionUtils;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubRdfConversionTransactionService {

    private static final int PROCESS_ISSUE_LIMIT = 30000; // Limit for the number of issues to process

    private static final int PROCESS_COMMIT_LIMIT = 30000; // Limit for the number of commits to process

    private static final boolean PROCESS_COMMENT_REACTIONS = true;

    public static final String GIT_NAMESPACE = "git";
    public static final String GIT_URI = "https://purl.archive.org/git2rdf/v2/git2RDFLab-git#";

    public static final String PLATFORM_NAMESPACE = "platform";
    public static final String PLATFORM_URI = "https://purl.archive.org/git2rdf/v2/git2RDFLab-platform#";

    public static final String PLATFORM_GITHUB_NAMESPACE = "github";
    public static final String PLATFORM_GITHUB_URI = "https://purl.archive.org/git2rdf/v2/git2RDFLab-platform-github#";

    public static final String XSD_SCHEMA_NAMESPACE = "xsd";
    public static final String XSD_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema#";

    public static final String RDF_SCHEMA_NAMESPACE = "rdf";
    public static final String RDF_SCHEMA_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String OWL_SCHEMA_NAMESPACE = "owl";
    public static final String OWL_SCHEMA_URI = "http://www.w3.org/2002/07/owl#";

    // REMOVED: SPDX constants no longer needed in v2.1

    private final GithubHandlerService githubHandlerService;

    private final GithubConfig githubConfig;

    private final EntityManager entityManager;

    private final int commitsPerIteration;
    
    private final GithubAccountRotationService githubAccountRotationService;

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
    private final Map<Long, GHPullRequestReviewComment> individualReviewCommentsCache = new HashMap<>();
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
            GithubAccountRotationService githubAccountRotationService,
            @Value("${worker.commits-per-iteration}") int commitsPerIteration) {

        this.githubHandlerService = githubHandlerService;
        this.githubConfig = githubConfig;
        this.entityManager = entityManager;
        this.commitsPerIteration = commitsPerIteration;
        this.githubAccountRotationService = githubAccountRotationService;
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
        
        // Clear GitHub user cache to ensure users are created in each repository's RDF model
        GithubUserValidator.clearProcessedUsersCache();
        
        Git gitHandler = null;

        try {

            GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs = entityManager
                    .find(GithubRepositoryOrderEntityLobs.class, id);

            GithubRepositoryOrderEntity githubRepositoryOrderEntity = entityManager
                    .find(GithubRepositoryOrderEntity.class, id);

            String owner = githubRepositoryOrderEntity.getOwnerName();
            String repo = githubRepositoryOrderEntity.getRepositoryName();

            // Initialize processing limits for rate limit logging
            githubAccountRotationService.setProcessingLimits(PROCESS_ISSUE_LIMIT, PROCESS_COMMIT_LIMIT);
            githubAccountRotationService.updateProcessedIssuesCount(0);
            githubAccountRotationService.updateProcessedCommitsCount(0);
            
            // Fetch actual repository totals from GitHub API for accurate progress tracking
            try {
                log.info("Fetching repository totals for {}/{}", owner, repo);
                int totalIssues = githubHandlerService.getTotalIssuesCount(owner, repo);
                int totalCommits = githubHandlerService.getTotalCommitsCount(owner, repo);
                
                githubAccountRotationService.setRepositoryTotals(totalIssues, totalCommits);
            } catch (Exception e) {
                log.warn("Failed to fetch repository totals, will use processing limits for progress tracking: {}", e.getMessage());
                // Continue with processing - the service will fall back to using limits
            }

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

    private void writeMergeInfo(GHIssue issue, GHPullRequest pr, StreamRDF writer, String issueUri, GitHub gitHubHandle) {
        if (issue == null || !issue.isPullRequest() || pr == null) {
            return;
        }
        try {
            writer.triple(RdfGithubPullRequestUtils.createIssueMergedProperty(issueUri, pr.isMerged()));

            Date mergedAt = pr.getMergedAt();

            if (mergedAt != null) {
                writer.triple(RdfGithubPullRequestUtils.createIssueMergedAtProperty(issueUri, localDateTimeFrom(mergedAt)));
            }

            if (pr.getMergedBy() != null && pr.getMergedBy().getLogin() != null) {
                // Validate and ensure GitHub user exists in RDF
                String mergedByUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, pr.getMergedBy());
                if (mergedByUserUri != null) {
                    writer.triple(RdfGithubPullRequestUtils.createIssueMergedByProperty(issueUri, mergedByUserUri));
                } else {
                    log.warn("User validation failed for PR mergedBy user '{}', creating fallback", pr.getMergedBy().getLogin());
                    try {
                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, pr.getMergedBy().getLogin());
                        if (fallbackUserUri != null) {
                            writer.triple(RdfGithubPullRequestUtils.createIssueMergedByProperty(issueUri, fallbackUserUri));
                        }
                    } catch (Exception e) {
                        log.warn("Fallback user creation failed for PR mergedBy: {}", e.getMessage());
                    }
                }
            }

            if (pr.getMergeCommitSha() != null) {
                writer.triple(RdfGithubPullRequestUtils.createIssueMergeCommitShaProperty(issueUri, pr.getMergeCommitSha()));
            }

            // Add source and target branch information
            if (pr.getHead() != null && pr.getHead().getRef() != null) {
                String sourceBranchUri = createBranchUri(pr.getRepository().getOwner().getLogin(), 
                                                       pr.getRepository().getName(), 
                                                       pr.getHead().getRef());
                String repositoryUri = GithubUriUtils.getRepositoryUri(pr.getRepository().getOwner().getLogin(), 
                                                                      pr.getRepository().getName());
                
                // Create source branch entity
                writer.triple(RdfCommitUtils.createBranchRdfTypeProperty(sourceBranchUri));
                writer.triple(RdfCommitUtils.createBranchNameProperty(sourceBranchUri, pr.getHead().getRef()));
                writer.triple(RdfCommitUtils.createBranchOfProperty(sourceBranchUri, repositoryUri));
                writer.triple(RdfCommitUtils.createRepositoryHasBranchProperty(repositoryUri, sourceBranchUri));
                
                writer.triple(RdfGithubPullRequestUtils.createSourceBranchProperty(issueUri, sourceBranchUri));
            }

            if (pr.getBase() != null && pr.getBase().getRef() != null) {
                String targetBranchUri = createBranchUri(pr.getRepository().getOwner().getLogin(), 
                                                       pr.getRepository().getName(), 
                                                       pr.getBase().getRef());
                String repositoryUri = GithubUriUtils.getRepositoryUri(pr.getRepository().getOwner().getLogin(), 
                                                                      pr.getRepository().getName());
                
                // Create target branch entity
                writer.triple(RdfCommitUtils.createBranchRdfTypeProperty(targetBranchUri));
                writer.triple(RdfCommitUtils.createBranchNameProperty(targetBranchUri, pr.getBase().getRef()));
                writer.triple(RdfCommitUtils.createBranchOfProperty(targetBranchUri, repositoryUri));
                writer.triple(RdfCommitUtils.createRepositoryHasBranchProperty(repositoryUri, targetBranchUri));
                
                writer.triple(RdfGithubPullRequestUtils.createTargetBranchProperty(issueUri, targetBranchUri));
            }
        } catch (IOException e) {
            log.warn("Error while writing merge info for issue {}: {}", issueUri, e.getMessage());
        }
        // TODO: Add addional merge information if available
    }

    private void writeWorkflowRunInfo(GHPullRequest pr, StreamRDF writer, String issueUri, String repositoryUri)
            throws IOException, InterruptedException {

        // Skip workflow processing if no PR provided
        if (pr == null) {
            return;
        }

        try {
            if (!pr.isMerged()) {
                log.debug("Pull request {} is not merged, skipping workflow run info.", pr.getHtmlUrl());
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
            Model model = ModelFactory.createDefaultModel();
            StreamRDF writer = StreamRDFLib.graph(model.getGraph());

            writer.prefix(XSD_SCHEMA_NAMESPACE, XSD_SCHEMA_URI);
            writer.prefix(RDF_SCHEMA_NAMESPACE, RDF_SCHEMA_URI);
            writer.prefix(OWL_SCHEMA_NAMESPACE, OWL_SCHEMA_URI);

            writer.prefix(GIT_NAMESPACE, GIT_URI);
            writer.prefix(PLATFORM_NAMESPACE, PLATFORM_URI);
            writer.prefix(PLATFORM_GITHUB_NAMESPACE, PLATFORM_GITHUB_URI);

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
            writer.triple(RdfGithubCommitUtils.createRepositoryRdfTypeProperty(repositoryUri));
            
            // Validate and ensure GitHub user exists in RDF first, then create repository owner property
            GHUser repoOwner = githubRepositoryHandle.getOwner();
            if (repoOwner != null && repoOwner.getLogin() != null) {
                String ownerUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, repoOwner);
                if (ownerUserUri != null) {
                    // Only create repository owner property if user validation succeeded
                    writer.triple(RdfGithubCommitUtils.createRepositoryOwnerProperty(repositoryUri, ownerUserUri));
                    log.info("Repository owner user created/validated and linked: {}", ownerUserUri);
                } else {
                    log.warn("User validation failed for repository owner '{}', creating fallback", repoOwner.getLogin());
                    try {
                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, repoOwner.getLogin());
                        if (fallbackUserUri != null) {
                            writer.triple(RdfGithubCommitUtils.createRepositoryOwnerProperty(repositoryUri, fallbackUserUri));
                        }
                    } catch (Exception e) {
                        log.warn("Fallback user creation failed for repository owner: {}", e.getMessage());
                    }
                }
            } else {
                log.warn("Repository owner is null or has no login, skipping repository owner property");
            }
            writer.triple(RdfGithubCommitUtils.createRepositoryNameProperty(repositoryUri, repositoryName));

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
                String fullBranchName = branchRef.getName();
                String cleanBranchName = getCleanBranchName(branchRef);

                // Skip remote tracking branches that are duplicates of local branches
                if (fullBranchName.startsWith("refs/remotes/origin/")) {
                    // Check if we have a local branch with the same name
                    boolean hasLocalBranch = false;
                    for (Ref otherRef : branches) {
                        if (otherRef.getName().equals("refs/heads/" + cleanBranchName)) {
                            hasLocalBranch = true;
                            break;
                        }
                    }
                    if (hasLocalBranch) {
                        continue; // Skip this remote tracking branch
                    }
                }

                String branchUri = GithubUriUtils.getBranchUri(owner, repositoryName, cleanBranchName);
                String headCommitUri = GithubUriUtils.getCommitUri(owner, repositoryName,
                        branchRef.getObjectId().getName());

                writer.triple(RdfCommitUtils.createRepositoryHasBranchProperty(repositoryUri, branchUri));
                writer.triple(RdfCommitUtils.createBranchRdfTypeProperty(branchUri));
                writer.triple(RdfCommitUtils.createBranchNameProperty(branchUri, cleanBranchName));
                writer.triple(RdfCommitUtils.createBranchHeadCommitProperty(branchUri, headCommitUri));
                writer.triple(RdfCommitUtils.createBranchOfProperty(branchUri, repositoryUri));
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
                    String submoduleCommitHashUri = GithubUriUtils.getCommitUri(owner, repositoryName,
                            submoduleCommitHash);

                    Resource submoduleResource = ResourceFactory.createResource();
                    Node submoduleNode = submoduleResource.asNode();

                    writer.triple(RdfCommitUtils.createRepositorySubmoduleProperty(repositoryUri, submoduleNode));
                    writer.triple(RdfCommitUtils.createSubmoduleRdfTypeProperty(submoduleNode));

                    writer.triple(RdfCommitUtils.createSubmodulePathProperty(submoduleNode, submodulePath));
                    writer.triple(RdfCommitUtils.createSubmoduleRepositoryEntryProperty(submoduleNode, submoduleUrl));
                    writer.triple(
                            RdfCommitUtils.createSubmoduleCommitEntryProperty(submoduleNode, submoduleCommitHashUri));
                    writer.triple(RdfCommitUtils.createSubmoduleCommitProperty(submoduleNode, submoduleCommitHash));
                    
                    // v2.1: No longer need SPDX CheckSum triples - hash is now a plain string

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

                log.info("Building tags map for commits...");
                Map<ObjectId, List<String>> commitToTags = getTagsForCommits(gitRepository);
                log.info("Completed building tags map with {} entries", commitToTags.size());

                log.info("Building pull request map for commits...");
                Map<String, PullRequestInfo> commitPrMap = buildCommitPrMap(githubRepositoryHandle, gitRepository);
                log.info("Completed building PR map with {} entries", commitPrMap.size());

                for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                    log.info("Start iterations of git commits. Current iteration count: {}", iteration);

                    log.info("Checking whether github installation token needs refresh");

                    int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration,
                            commitsPerIteration);

                    log.info("Calculated skip count for this iteration is: {}", skipCount);

                    Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration)
                            .call();

                    boolean finished = true;

                    log.info("Starting commit writer rdf");

                    writer.start();

                    log.info("Starting commit loop");

                    for (RevCommit commit : commits) {
                        commitsProcessed++;
                        githubAccountRotationService.updateProcessedCommitsCount(commitsProcessed);
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
                                    githubRepositoryHandle, gitHubHandle);
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
                        calculateCommitIssues(writer, commitUri, commit.getFullMessage(), owner, repositoryName,
                                prInfo);
                        if (prInfo != null) {
                            writer.triple(
                                    RdfGithubCommitUtils.createCommitPartOfPullRequestProperty(commitUri, prInfo.issueUri));
                            writer.triple(RdfGithubCommitUtils.createCommitPartOfIssueProperty(commitUri, prInfo.issueUri));
                            writer.triple(
                                    RdfGithubIssueUtils.createIssueContainsCommitProperty(prInfo.issueUri, commitUri));
                            writer.triple(RdfGithubCommitUtils.createCommitIsMergedProperty(commitUri, true));
                            if (prInfo.mergedAt != null) {
                                writer.triple(RdfGithubCommitUtils.createCommitMergedAtProperty(commitUri, prInfo.mergedAt));
                            }
                            if (gitHash.equals(prInfo.mergeCommitSha)) {
                                writer.triple(
                                        RdfGithubCommitUtils.createCommitMergedIntoIssueProperty(commitUri, prInfo.issueUri));
                            }
                        }

                        // Branch
                        // TODO: better way to handle merges? (so commit could have multiple branches)
                        if (gitCommitRepositoryFilter.isEnableCommitBranch()) {
                            calculateCommitBranch(commitBranchCalculator, writer, commit, commitUri, owner,
                                    repositoryName);
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

                    log.info("Ending commit writer rdf - loop finished");

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
                        String issueUri;
                        if (ghIssue.isPullRequest()) {
                            issueUri = GithubUriUtils.getPullRequestUri(owner, repositoryName, String.valueOf(issueNumber));
                        } else {
                            issueUri = GithubUriUtils.getIssueUri(owner, repositoryName, String.valueOf(issueNumber));
                        }

                        if (issueUri == null || issueUri.isEmpty()) {
                            log.warn(
                                    "Issue with number {} fallback to githubRepositoryURI because its issueUri is null or empty",
                                    issueNumber);
                            issueUri = GithubUriUtils.getRepositoryUri(owner, repositoryName);
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() == null) {
                            continue;
                        }

                        // // ********************** ** REMOVE ON DEPLOYMENT ** **********************
                        // // ZonedDateTime oneYearAgo = ZonedDateTime.now().minusYears(1);
                        // // // REMOVE ON DEPLOYMENT
                        // // Date dcreatedAt = ghIssue.getCreatedAt();
                        // // if (dcreatedAt == null || dcreatedAt.toInstant().isBefore(oneYearAgo.toInstant())) {
                        // //     continue;
                        // // }

                        // if (PROCESS_ISSUE_ONLY.length > 0) {
                        //     boolean shouldProcessIssue = false;
                        //     for (String issueId : PROCESS_ISSUE_ONLY) {
                        //         if (issueId.equals(String.valueOf(issueNumber))) {
                        //             shouldProcessIssue = true;
                        //             break;
                        //         }
                        //     }
                        //     if (!shouldProcessIssue) {
                        //         continue; // Skip this issue
                        //     }
                        // }
                        // // ********************** ** **************** ** **********************

                        // Create correct RDF type based on whether this is an issue or pull request
                        if (ghIssue.isPullRequest()) {
                            writer.triple(RdfGithubPullRequestUtils.createRdfTypeProperty(issueUri));
                        } else {
                            writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(issueUri));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueNumber()) {
                            writer.triple(RdfPlatformTicketUtils.createNumberProperty(issueUri, issueNumber));
                        }

                        // Add GitHub issue ID (internal ID)
                        if (githubIssueRepositoryFilter.isEnableIssueNumber()) {
                            writer.triple(RdfGithubIssueUtils.createIssueIdProperty(issueUri, ghIssue.getId()));
                        }


                        if (ghIssue.getNodeId() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueNodeIdProperty(issueUri, ghIssue.getNodeId()));
                        }
                        
                        writer.triple(RdfPlatformTicketUtils.createLockedProperty(issueUri, ghIssue.isLocked()));

                        if (githubIssueRepositoryFilter.isEnableIssueTitle() && ghIssue.getTitle() != null) {
                            writer.triple(RdfPlatformTicketUtils.createTitleProperty(issueUri, ghIssue.getTitle()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueBody() && ghIssue.getBody() != null) {
                            writer.triple(RdfPlatformTicketUtils.createBodyProperty(issueUri, ghIssue.getBody()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() != null) {
                            writer.triple(RdfPlatformTicketUtils.createStateProperty(
                                    issueUri, ghIssue.getState().toString()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUser()) {
                            if (ghIssue.getUser() != null && ghIssue.getUser().getLogin() != null) {
                                // Validate and ensure GitHub user exists in RDF
                                String githubIssueUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, ghIssue.getUser());
                                if (githubIssueUserUri != null) {
                                    writer.triple(
                                            RdfPlatformTicketUtils.createSubmitterProperty(issueUri, githubIssueUserUri));
                                } else {
                                    log.warn("User validation failed for issue submitter '{}' in issue #{}, creating fallback", 
                                            ghIssue.getUser().getLogin(), issueNumber);
                                    try {
                                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, ghIssue.getUser().getLogin());
                                        if (fallbackUserUri != null) {
                                            writer.triple(RdfPlatformTicketUtils.createSubmitterProperty(issueUri, fallbackUserUri));
                                        }
                                    } catch (Exception e) {
                                        log.warn("Fallback user creation failed for issue submitter: {}", e.getMessage());
                                    }
                                }
                            } else {
                                // Try to handle cases where user or login is null, including deleted users
                                if (ghIssue.getUser() == null) {
                                    log.warn("Cannot create submitter for issue #{}: GitHub user object is null (possibly deleted account)", issueNumber);
                                } else if (ghIssue.getUser().getLogin() == null) {
                                    log.warn("Cannot create submitter for issue #{}: GitHub user login is null (user ID: {})", 
                                            issueNumber, ghIssue.getUser().getId());
                                    // Attempt to create a fallback submitter if we have user ID but no login
                                    try {
                                        String fallbackLogin = "user-" + ghIssue.getUser().getId();
                                        String githubIssueUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, fallbackLogin);
                                        if (githubIssueUserUri != null) {
                                            writer.triple(RdfPlatformTicketUtils.createSubmitterProperty(issueUri, githubIssueUserUri));
                                            log.info("Created fallback submitter for issue #{} using ID-based login: {}", issueNumber, fallbackLogin);
                                        }
                                    } catch (Exception e) {
                                        log.warn("Failed to create fallback submitter for issue #{}: {}", issueNumber, e.getMessage());
                                    }
                                }
                            }
                        } else {
                            log.debug("Submitter creation skipped for issue #{}: issue user processing is disabled", issueNumber);
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueReviewers() && ghIssue.isPullRequest()) {
                            try {
                                GHPullRequest pr = getPullRequestCached(
                                        ghIssue.getRepository(), issueNumber);
                                if (pr != null) {
                                    List<GHUser> reviewers = pr.getRequestedReviewers();
                                    if (reviewers != null && !reviewers.isEmpty()) {
                                        for (GHUser reviewer : reviewers) {
                                            if (reviewer != null && reviewer.getLogin() != null) {
                                                try {
                                                    // Validate and ensure GitHub user exists in RDF
                                                    String reviewerUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, reviewer);
                                                    if (reviewerUri != null) {
                                                        writer.triple(RdfGithubIssueUtils.createIssueRequestedReviewerProperty(issueUri,
                                                                reviewerUri));
                                                    } else {
                                                        log.warn("User validation failed for requested reviewer '{}' in issue #{}, creating fallback", 
                                                                reviewer.getLogin(), issueNumber);
                                                        try {
                                                            String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, reviewer.getLogin());
                                                            if (fallbackUserUri != null) {
                                                                writer.triple(RdfGithubIssueUtils.createIssueRequestedReviewerProperty(issueUri, fallbackUserUri));
                                                            }
                                                        } catch (Exception e) {
                                                            log.warn("Fallback user creation failed for requested reviewer: {}", e.getMessage());
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    log.warn("Error processing requested reviewer {} for issue {}: {}", 
                                                            reviewer.getLogin(), issueNumber, e.getMessage());
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error processing reviewers for issue {}: {}", issueNumber, e.getMessage());
                            }
                        }

                        // Milestone - process issue/PR milestone with proper milestone entity
                        if (githubIssueRepositoryFilter.isEnableIssueMilestone()) {
                            try {
                                GHMilestone milestone = ghIssue.getMilestone();
                                if (milestone != null) {
                                    String milestoneUri = GithubUriUtils.getMilestoneUri(owner, repositoryName, String.valueOf(milestone.getNumber()));
                                    
                                    // Create milestone entity
                                    writer.triple(RdfPlatformMilestoneUtils.createRdfTypeProperty(milestoneUri));
                                    writer.triple(RdfPlatformMilestoneUtils.createIdProperty(milestoneUri, String.valueOf(milestone.getId())));
                                    
                                    if (milestone.getTitle() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createTitleProperty(milestoneUri, milestone.getTitle()));
                                    }
                                    
                                    if (milestone.getDescription() != null && !milestone.getDescription().isEmpty()) {
                                        writer.triple(RdfPlatformMilestoneUtils.createDescriptionProperty(milestoneUri, milestone.getDescription()));
                                    }
                                    
                                    if (milestone.getState() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createStateProperty(milestoneUri, milestone.getState().toString()));
                                    }
                                    
                                    if (milestone.getHtmlUrl() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createUrlProperty(milestoneUri, milestone.getHtmlUrl().toString()));
                                    }
                                    
                                    if (milestone.getCreatedAt() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createCreatedAtProperty(milestoneUri, localDateTimeFrom(milestone.getCreatedAt())));
                                    }
                                    
                                    if (milestone.getUpdatedAt() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createUpdatedAtProperty(milestoneUri, localDateTimeFrom(milestone.getUpdatedAt())));
                                    }
                                    
                                    if (milestone.getClosedAt() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createClosedAtProperty(milestoneUri, localDateTimeFrom(milestone.getClosedAt())));
                                    }
                                    
                                    if (milestone.getDueOn() != null) {
                                        writer.triple(RdfPlatformMilestoneUtils.createDueDateProperty(milestoneUri, localDateTimeFrom(milestone.getDueOn())));
                                    }
                                    
                                    // Link milestone to issue/PR
                                    writer.triple(RdfPlatformMilestoneUtils.createHasMilestoneProperty(issueUri, milestoneUri));
                                    writer.triple(RdfPlatformMilestoneUtils.createMilestoneOfProperty(milestoneUri, issueUri));
                                }
                            } catch (Exception e) {
                                log.warn("Error processing milestone for issue {}: {}", issueNumber, e.getMessage());
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueCreatedAt() && ghIssue.getCreatedAt() != null) {
                            try {
                                LocalDateTime createdAt = localDateTimeFrom(ghIssue.getCreatedAt());
                                writer.triple(RdfPlatformTicketUtils.createCreatedAtProperty(issueUri, createdAt));
                            } catch (Exception e) {
                                log.warn("Error processing created date for issue {}: {}", issueNumber, e.getMessage());
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUpdatedAt()) {
                            try {
                                Date updatedAtUtilDate = ghIssue.getUpdatedAt();
                                if (updatedAtUtilDate != null) {
                                    LocalDateTime updatedAt = localDateTimeFrom(updatedAtUtilDate);
                                    writer.triple(
                                            RdfPlatformTicketUtils.createUpdatedAtProperty(issueUri, updatedAt));
                                }
                            } catch (Exception e) {
                                log.warn("Error processing updated date for issue {}: {}", issueNumber, e.getMessage());
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueClosedAt()) {
                            Date closedAtUtilDate = ghIssue.getClosedAt();
                            if (closedAtUtilDate != null) {
                                LocalDateTime closedAt = localDateTimeFrom(closedAtUtilDate);
                                writer.triple(
                                        RdfPlatformTicketUtils.createClosedAtProperty(issueUri, closedAt));
                            }
                        }
                        // SHIT
                        if (githubIssueRepositoryFilter.isEnableIssueMergedBy()) {
                            if (ghIssue.isPullRequest()) {
                                GHPullRequest pullRequest = getPullRequestCached(
                                        githubRepositoryHandle, issueNumber);
                                writeMergeInfo(ghIssue, pullRequest, writer, issueUri, gitHubHandle);
                                writeWorkflowRunInfo(pullRequest, writer, issueUri, repositoryUri);
                            }
                        }

                        // GitHub issues can have multiple assignees
                        if (githubIssueRepositoryFilter.isEnableIssueAssignees()) {
                            List<GHUser> assignees = ghIssue.getAssignees();
                            for (GHUser assignee : assignees) {
                                if (assignee != null && assignee.getLogin() != null) {
                                    // Validate and ensure GitHub user exists in RDF
                                    String assigneeUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, assignee);
                                    if (assigneeUri != null) {
                                        writer.triple(RdfPlatformTicketUtils.createAssigneeProperty(issueUri, assigneeUri));
                                    } else {
                                        log.warn("User validation failed for assignee '{}' in issue #{}, creating fallback", 
                                                assignee.getLogin(), issueNumber);
                                        try {
                                            String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, assignee.getLogin());
                                            if (fallbackUserUri != null) {
                                                writer.triple(RdfPlatformTicketUtils.createAssigneeProperty(issueUri, fallbackUserUri));
                                            }
                                        } catch (Exception e) {
                                            log.warn("Fallback user creation failed for assignee: {}", e.getMessage());
                                        }
                                    }
                                }
                            }
                        }

                        // Labels - process issue/PR labels
                        if (githubIssueRepositoryFilter.isEnableIssueLabels()) {
                            try {
                                Collection<org.kohsuke.github.GHLabel> labels = ghIssue.getLabels();
                                if (labels != null && !labels.isEmpty()) {
                                    for (org.kohsuke.github.GHLabel label : labels) {
                                        if (label != null && label.getName() != null) {
                                            String labelUri = GithubUriUtils.getLabelUri(owner, repositoryName, label.getName());
                                            
                                            // Create label entity
                                            writer.triple(RdfPlatformLabelUtils.createRdfTypeProperty(labelUri));
                                            writer.triple(RdfPlatformLabelUtils.createNameProperty(labelUri, label.getName()));
                                            
                                            if (label.getDescription() != null && !label.getDescription().isEmpty()) {
                                                writer.triple(RdfPlatformLabelUtils.createDescriptionProperty(labelUri, label.getDescription()));
                                            }
                                            
                                            if (label.getColor() != null) {
                                                writer.triple(RdfPlatformLabelUtils.createColorProperty(labelUri, label.getColor()));
                                            }
                                            
                                            if (label.getUrl() != null) {
                                                writer.triple(RdfPlatformLabelUtils.createUrlProperty(labelUri, label.getUrl().toString()));
                                            }
                                            
                                            // Link label to issue/PR
                                            writer.triple(RdfPlatformLabelUtils.createHasLabelProperty(issueUri, labelUri));
                                            writer.triple(RdfPlatformLabelUtils.createLabelOfProperty(labelUri, issueUri));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error processing labels for issue {}: {}", issueNumber, e.getMessage());
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
                                String reviewURI;
                                String reviewUrl;
                                if (ghIssue.isPullRequest()) {
                                    reviewURI = GithubUriUtils.getPullRequestReviewUri(issueUri, String.valueOf(reviewId));
                                    reviewUrl = GithubUriUtils.getIssueReviewUrl("https://github.com/" + owner + "/" + repositoryName, String.valueOf(issueNumber), String.valueOf(reviewId));
                                } else {
                                    reviewURI = GithubUriUtils.getIssueReviewUri(issueUri, String.valueOf(reviewId));
                                    reviewUrl = GithubUriUtils.getIssueReviewUrl("https://github.com/" + owner + "/" + repositoryName, String.valueOf(issueNumber), String.valueOf(reviewId));
                                }
                                // String reviewURL = review.getUrl().toString();
                                // String reviewUri = issueUri + "/reviews/" + reviewId;

                                // Static Properties
                                writer.triple(RdfGithubIssueReviewUtils.createIssueReviewProperty(issueUri, reviewURI));
                                writer.triple(RdfGithubIssueReviewUtils.createReviewApiUrlProperty(reviewURI, reviewUrl));
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
                                if (review.getUser() != null && review.getUser().getLogin() != null) {
                                    // Validate and ensure GitHub user exists in RDF
                                    String reviewUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, review.getUser());
                                    if (reviewUserUri != null) {
                                        writer.triple(RdfGithubIssueReviewUtils.createReviewUserProperty(
                                                reviewURI, reviewUserUri));
                                    } else {
                                        log.warn("User validation failed for review user '{}' in review {}, creating fallback", 
                                                review.getUser().getLogin(), review.getId());
                                        try {
                                            String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, review.getUser().getLogin());
                                            if (fallbackUserUri != null) {
                                                writer.triple(RdfGithubIssueReviewUtils.createReviewUserProperty(reviewURI, fallbackUserUri));
                                            }
                                        } catch (Exception e) {
                                            log.warn("Fallback user creation failed for review user: {}", e.getMessage());
                                        }
                                    }
                                } else {
                                    log.warn("Cannot create review user for review {} in issue #{}: user is {} or login is {}", 
                                            review.getId(), issueNumber,
                                            review.getUser() == null ? "null" : "valid",
                                            review.getUser() == null ? "null" : (review.getUser().getLogin() == null ? "null" : "valid"));
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

                                // Create a mutable copy and sort comments by creation time to identify the earliest (first) comment
                                reviewComments = new ArrayList<>(reviewComments);
                                reviewComments.sort((c1, c2) -> {
                                    try {
                                        if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
                                        if (c1.getCreatedAt() == null) return 1;
                                        if (c2.getCreatedAt() == null) return -1;
                                        return c1.getCreatedAt().compareTo(c2.getCreatedAt());
                                    } catch (IOException e) {
                                        log.warn("IOException while comparing comment creation times: {}", e.getMessage());
                                        return 0; // treat as equal if comparison fails
                                    }
                                });

                                String firstCommentURI = null;
                                if (!reviewComments.isEmpty()) {
                                    long firstCommentId = reviewComments.get(0).getId();
                                    firstCommentURI = GithubUriUtils.getIssueReviewCommentUri(
                                            issueUri, String.valueOf(firstCommentId));
                                    // Link review to first comment only using platform:hasComment
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewCommentProperty(
                                            reviewURI, firstCommentURI));
                                }

                                // Process the comments of the review
                                for (GHPullRequestReviewComment c : reviewComments) {
                                    long cid = c.getId();
                                    
                                    // Cache individual comment for potential parent lookups
                                    individualReviewCommentsCache.put(cid, c);

                                    // Setup the URI for a review comment of a pull request
                                    String reviewCommentURI = GithubUriUtils.getPullRequestReviewCommentUri(
                                            issueUri, String.valueOf(cid));

                                    // Create the RDF triples for the review comment
                                    writer.triple(RdfGithubCommentUtils.createCommentRdfType(reviewCommentURI));
                                    writer.triple(RdfGithubCommentUtils.createCommentId(reviewCommentURI, cid));
                                    
                                    // Add comment API URL (review comments use pulls/comments API)
                                    String repoString = "https://github.com/" + owner + "/" + repositoryName;
                                    String commentApiUrl = GithubUriUtils.getIssueReviewCommentUrl(repoString, String.valueOf(cid));
                                    writer.triple(RdfGithubCommentUtils.createCommentApiUrl(reviewCommentURI, commentApiUrl));

                                    // Content and user
                                    if (c.getBody() != null && !c.getBody().isEmpty()) {
                                        writer.triple(
                                                RdfGithubCommentUtils.createCommentBody(reviewCommentURI, c.getBody()));
                                    }
                                    if (c.getUser() != null && c.getUser().getLogin() != null) {
                                        // Validate and ensure GitHub user exists in RDF
                                        String commentUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, c.getUser());
                                        if (commentUserUri != null) {
                                            writer.triple(RdfGithubCommentUtils.createCommentUser(
                                                    reviewCommentURI, commentUserUri));
                                        } else {
                                            log.warn("User validation failed for review comment author '{}', creating fallback", c.getUser().getLogin());
                                            try {
                                                String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, c.getUser().getLogin());
                                                if (fallbackUserUri != null) {
                                                    writer.triple(RdfGithubCommentUtils.createCommentUser(reviewCommentURI, fallbackUserUri));
                                                }
                                            } catch (Exception e) {
                                                log.warn("Fallback user creation failed for review comment author: {}", e.getMessage());
                                            }
                                        }
                                    }
                                    if (c.getCreatedAt() != null) {
                                        writer.triple(RdfGithubCommentUtils.createCommentCreatedAt(
                                                reviewCommentURI, localDateTimeFrom(c.getCreatedAt())));
                                    }

                                    // NEW COMMENT CHAIN STRUCTURE
                                    Long parentId = c.getInReplyToId();
                                    boolean isFirstComment = reviewCommentURI.equals(firstCommentURI);
                                    boolean isRoot = (parentId == null || parentId.equals(cid) || parentId <= 0);

                                    if (isFirstComment) {
                                        // First comment comments on the review itself
                                        writer.triple(RdfPlatformCommentUtils.createCommentOnProperty(
                                                reviewCommentURI, reviewURI));
                                    } else if (!isRoot && parentId != null && parentId > 0) {
                                        // This is a reply, it comments on the parent comment
                                        String parentCommentUri = GithubUriUtils.getPullRequestReviewCommentUri(
                                                issueUri, String.valueOf(parentId));

                                        // Validate and ensure parent comment exists before creating relationships
                                        if (validateAndEnsureParentReviewComment(writer, githubRepositoryHandle, 
                                                parentId, repositoryUri, issueUri, gitHubHandle)) {
                                            writer.triple(RdfPlatformCommentUtils.createCommentOnProperty(
                                                    reviewCommentURI, parentCommentUri));
                                            
                                            // Create platform:hasComment link from parent to this comment
                                            writer.triple(RdfGithubCommentUtils.createHasCommentProperty(
                                                    parentCommentUri, reviewCommentURI));
                                        }
                                    } else {
                                        // Root comment that's not the first - comments on the review
                                        writer.triple(RdfPlatformCommentUtils.createCommentOnProperty(
                                                reviewCommentURI, reviewURI));
                                    }

                                    // Reactions
                                    if (PROCESS_COMMENT_REACTIONS) {
                                        List<GHReaction> reactions = getReviewCommentReactionsCached(c);
                                        for (GHReaction r : reactions) {
                                            String reactionURI = GithubUriUtils.getIssueReviewCommentReactionUri(
                                                    issueUri, String.valueOf(cid), String.valueOf(r.getId()));

                                            writer.triple(RdfGithubCommentUtils.createCommentReaction(
                                                    reviewCommentURI,
                                                    reactionURI));
                                            writer.triple(
                                                    RdfGithubReactionUtils.createReactionRdfTypeProperty(reactionURI));
                                            writer.triple(RdfGithubReactionUtils.createReactionIdProperty(reactionURI,
                                                    r.getId()));
                                            // Reaction relationship handled via hasReaction from comment

                                            if (r.getContent() != null) {
                                                writer.triple(RdfGithubReactionUtils.createReactionContentProperty(
                                                        reactionURI, r.getContent().toString()));
                                            }
                                            if (r.getUser() != null && r.getUser().getLogin() != null) {
                                                // Validate and ensure GitHub user exists in RDF
                                                String reactionByUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, r.getUser());
                                                if (reactionByUri != null) {
                                                    writer.triple(
                                                            RdfGithubReactionUtils.createReactionByProperty(reactionURI,
                                                                    reactionByUri));
                                                } else {
                                                    log.warn("User validation failed for reaction user '{}', creating fallback", r.getUser().getLogin());
                                                    try {
                                                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, r.getUser().getLogin());
                                                        if (fallbackUserUri != null) {
                                                            writer.triple(RdfGithubReactionUtils.createReactionByProperty(reactionURI, fallbackUserUri));
                                                        }
                                                    } catch (Exception e) {
                                                        log.warn("Fallback user creation failed for reaction user: {}", e.getMessage());
                                                    }
                                                }
                                            }
                                            if (r.getCreatedAt() != null) {
                                                writer.triple(RdfGithubReactionUtils.createReactionCreatedAtProperty(
                                                        reactionURI, localDateTimeFrom(r.getCreatedAt())));
                                            }
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
                                String issueCommentURI;
                                String commentApiUrl;
                                String repoString = "https://github.com/" + owner + "/" + repositoryName;
                                
                                if (ghIssue.isPullRequest()) {
                                    issueCommentURI = GithubUriUtils.getPullRequestCommentUri(issueUri, String.valueOf(cid));
                                    commentApiUrl = GithubUriUtils.getPullRequestCommentUrl(repoString, String.valueOf(cid));
                                } else {
                                    issueCommentURI = GithubUriUtils.getIssueCommentUri(issueUri, String.valueOf(cid));
                                    commentApiUrl = GithubUriUtils.getIssueCommentUrl(repoString, String.valueOf(cid));
                                }

                                // Link in Issue to Comment
                                writer.triple(RdfGithubIssueReviewUtils.createReviewCommentProperty(issueUri,
                                        issueCommentURI));
                                writer.triple(RdfGithubCommentUtils.createCommentRdfType(issueCommentURI));
                                writer.triple(RdfGithubCommentUtils.createCommentId(issueCommentURI, cid));
                                writer.triple(RdfGithubCommentUtils.createHasCommentProperty(issueUri, issueCommentURI));
                                
                                // Add platform:commentOn to link comment back to the issue
                                writer.triple(RdfPlatformCommentUtils.createCommentOnProperty(issueCommentURI, issueUri));
                                
                                // Add comment API URL
                                writer.triple(RdfGithubCommentUtils.createCommentApiUrl(issueCommentURI, commentApiUrl));

                                if (c.getBody() != null && !c.getBody().isEmpty()) {
                                    writer.triple(RdfGithubCommentUtils.createCommentBody(
                                            issueCommentURI, c.getBody()));
                                }
                                try {
                                    if (c.getUser() != null && c.getUser().getLogin() != null) {
                                        // Validate and ensure GitHub user exists in RDF
                                        String commentUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, c.getUser());
                                        if (commentUserUri != null) {
                                            writer.triple(RdfGithubCommentUtils.createCommentUser(
                                                    issueCommentURI,
                                                    commentUserUri));
                                        } else {
                                            log.warn("User validation failed for comment author '{}', creating fallback", c.getUser().getLogin());
                                            String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, c.getUser().getLogin());
                                            if (fallbackUserUri != null) {
                                                writer.triple(RdfGithubCommentUtils.createCommentUser(issueCommentURI, fallbackUserUri));
                                            }
                                        }
                                    }
                                } catch (org.kohsuke.github.HttpException e) {
                                    log.warn("GitHub API error (HTTP {}) fetching user for issue comment {}. Creating fallback user.", 
                                            e.getResponseCode(), cid);
                                    try {
                                        String fallbackLogin = "unknown-user-comment-" + cid;
                                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, fallbackLogin);
                                        if (fallbackUserUri != null) {
                                            writer.triple(RdfGithubCommentUtils.createCommentUser(issueCommentURI, fallbackUserUri));
                                        }
                                    } catch (Exception fallbackException) {
                                        log.error("Failed to create fallback user for comment {}: {}", cid, fallbackException.getMessage());
                                    }
                                }
                                if (c.getCreatedAt() != null) {
                                    writer.triple(RdfGithubCommentUtils.createCommentCreatedAt(
                                            issueCommentURI,
                                            localDateTimeFrom(c.getCreatedAt())));
                                }

                                // Reactions
                                List<GHReaction> reactions = getIssueCommentReactionsCached(c);
                                for (GHReaction r : reactions) {

                                    String reactionURI = GithubUriUtils.getIssueCommentReactionUri(issueUri,
                                            String.valueOf(cid), String.valueOf(r.getId()));

                                    writer.triple(RdfGithubCommentUtils.createCommentReaction(issueCommentURI,
                                            reactionURI));
                                    writer.triple(RdfGithubReactionUtils.createReactionRdfTypeProperty(reactionURI));
                                    writer.triple(RdfGithubReactionUtils.createReactionIdProperty(
                                            reactionURI, r.getId()));
                                    // Reaction relationship handled via hasReaction from comment
                                    if (r.getContent() != null) {
                                        writer.triple(RdfGithubReactionUtils.createReactionContentProperty(
                                                reactionURI, r.getContent().toString()));
                                    }
                                    if (r.getUser() != null && r.getUser().getLogin() != null) {
                                        // Validate and ensure GitHub user exists in RDF
                                        String reactionByUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, r.getUser());
                                        if (reactionByUri != null) {
                                            writer.triple(RdfGithubReactionUtils.createReactionByProperty(
                                                    reactionURI, reactionByUri));
                                        } else {
                                            log.warn("User validation failed for reaction user '{}', creating fallback", r.getUser().getLogin());
                                            try {
                                                String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, r.getUser().getLogin());
                                                if (fallbackUserUri != null) {
                                                    writer.triple(RdfGithubReactionUtils.createReactionByProperty(reactionURI, fallbackUserUri));
                                                }
                                            } catch (Exception e) {
                                                log.warn("Fallback user creation failed for reaction user: {}", e.getMessage());
                                            }
                                        }
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
                        githubAccountRotationService.updateProcessedIssuesCount(issuesProcessed);

                        if (issueCounter > 100) {
                            log.info("Finish issue rdf conversion batch");
                            writer.finish();
                            doesWriterContainNonWrittenRdfStreamElements = false;
                            issueCounter = 0;
                            lockHandler.renewLockOnRenewTimeFulfillment();
                            
                            // Restart writer for next batch if there are more issues to process
                            if (issuesProcessed < PROCESS_ISSUE_LIMIT) {
                                log.info("Start next issue rdf conversion batch");
                                writer.start();
                                doesWriterContainNonWrittenRdfStreamElements = true;
                            }
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

            log.info("Finished overall processing. Start to load rdf file into postgres blob storage");
            
            // Write the final collected model to the output stream using RDFDataMgr
            RDFDataMgr.write(outputStream, model, RDFFormat.TURTLE);
            
        } // Close the outputStream try-with-resources block
        
        // Tidy up the Turtle RDF
        RdfTurtleTidier.tidyFile(rdfTempFile);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(rdfTempFile));

        entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, rdfTempFile.length()));

        return bufferedInputStream;
    }
    

    // Helper and Component Functions

    private Map<String, PullRequestInfo> buildCommitPrMap(GHRepository repo, Repository gitRepository)
            throws IOException, InterruptedException {
        Map<String, PullRequestInfo> map = new HashMap<>();

        try {

            int maxPRsToFetch = PROCESS_ISSUE_LIMIT;
            log.info("Fetching pull requests for commit mapping (limit: {})...", maxPRsToFetch);
            PagedIterable<GHPullRequest> prs = executeWithRetry(
                    () -> repo.queryPullRequests().state(GHIssueState.ALL).list(),
                    "queryPullRequests");
            log.info("Successfully retrieved pull request list from GitHub API");

            int prsProcessed = 0;

            for (GHPullRequest pr : prs) {
                if (prsProcessed >= maxPRsToFetch) {
                    log.info("Reached max PRs limit ({}) for commit mapping", maxPRsToFetch);
                    break;
                }

                try {
                    prsProcessed++;
                    if (prsProcessed % 100 == 0) {
                        log.info("Processed {} PRs for commit mapping...", prsProcessed);
                    }
                    
                    if (!pr.isMerged()) {
                        continue;
                    }

                    Date merged = pr.getMergedAt();

                // Sanitize PR number to remove any invisible Unicode characters
                String prNumber = String.valueOf(pr.getNumber()).replaceAll("[\\p{C}&&[^\\r\\n\\t]]", "");
                String prUri = GithubUriUtils.getIssueUri(repo.getOwnerName(), repo.getName(), prNumber);
                LocalDateTime mergedAt = localDateTimeFrom(merged);

                PullRequestInfo info = new PullRequestInfo(prUri, pr.getMergeCommitSha(), mergedAt);

                // Validate merge commit SHA exists in repository using ObjectReader
                if (pr.getMergeCommitSha() != null) {
                    try {
                        ObjectId commitId = ObjectId.fromString(pr.getMergeCommitSha());
                        // Simple existence check - if this doesn't throw, the object exists
                        if (gitRepository.getObjectDatabase().has(commitId)) {
                            map.put(pr.getMergeCommitSha(), info);
                        } else {
                            log.warn("Merge commit SHA {} for PR {} not found in repository",
                                    pr.getMergeCommitSha(), pr.getNumber());
                        }
                    } catch (Exception e) {
                        log.warn("Invalid merge commit SHA {} for PR {}: {}",
                                pr.getMergeCommitSha(), pr.getNumber(), e.getMessage());
                    }
                }

                // Process individual commits
                if (PROCESS_COMMIT_LIMIT > 0) {
                    try {
                        if (prsProcessed % 50 == 0) {
                            log.info("Fetching commits for PR #{} (processed {} PRs so far)...", pr.getNumber(), prsProcessed);
                        }
                        for (GHPullRequestCommitDetail c : getCommitsCached(pr)) {
                            try {
                                ObjectId commitId = ObjectId.fromString(c.getSha());
                                // Simple existence check
                                if (gitRepository.getObjectDatabase().has(commitId)) {
                                    map.put(c.getSha(), info);
                                }
                            } catch (Exception e) {
                                log.debug("Commit {} from PR {} not accessible in current repository state",
                                        c.getSha(), pr.getNumber());
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        log.warn("Error fetching commits for PR {}: {}", pr.getNumber(), e.getMessage());
                    }
                }

                    prsProcessed++;
                } catch (IOException e) {
                    log.warn("GitHub API error processing PR {}: {}. Skipping this PR.", pr.getNumber(), e.getMessage());
                    prsProcessed++;
                    continue;
                } catch (Exception e) {
                    log.warn("Unexpected error processing PR {}: {}. Skipping this PR.", pr.getNumber(), e.getMessage());
                    prsProcessed++;
                    continue;
                }
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
            GHRepository githubRepositoryHandle,
            GitHub gitHubHandle) {

        if (authorIdent == null) {
            return;
        }

        String email = authorIdent.getEmailAddress();

        log.info("Set rdf github user in commit");

        GithubUserInfo info = RdfGitCommitUserUtils.getGitHubUserInfoFromCommit(githubRepositoryHandle, gitHash);

        if (!uniqueGitCommiterWithHash.containsKey(email)) {
            String uri = info == null ? null : info.uri;
            uniqueGitCommiterWithHash.put(email, new RdfGitCommitUserUtils(gitHash, uri));
        }

        String userUri = info == null ? null : info.uri;
        if (userUri != null && !userUri.isEmpty() && info != null && info.login != null && !info.login.isEmpty()) {
            // Validate and ensure GitHub user exists in RDF using the login from commit info
            String validatedUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, info.login);
            if (validatedUserUri != null) {
                writer.triple(RdfGithubCommitUtils.createCommiterGitHubUserProperty(commitUri, validatedUserUri));
                // Add additional commit-specific properties that aren't handled by the validator
                if (info.gitAuthorEmail != null && !info.gitAuthorEmail.isEmpty()) {
                    writer.triple(RdfGithubUserUtils.createGitAuthorEmailProperty(validatedUserUri, info.gitAuthorEmail));
                }
            } else {
                log.warn("User validation failed for commit author '{}', creating fallback", info.login);
                try {
                    String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, info.login);
                    if (fallbackUserUri != null) {
                        writer.triple(RdfGithubCommitUtils.createCommiterGitHubUserProperty(commitUri, fallbackUserUri));
                        if (info.gitAuthorEmail != null && !info.gitAuthorEmail.isEmpty()) {
                            writer.triple(RdfGithubUserUtils.createGitAuthorEmailProperty(fallbackUserUri, info.gitAuthorEmail));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Fallback user creation failed for commit author: {}", e.getMessage());
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
            writer.triple(RdfGithubCommitUtils.createCommitIssueProperty(commitUri, issueUri));
        }
    }

    private String extractIssueNumberFromUri(String uri) {
        int idx = uri.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < uri.length()) {
            return uri.substring(idx + 1);
        }
        return uri;
    }

    /**
     * Creates a branch URI for GitHub branches following the git2RDF pattern
     */
    private String createBranchUri(String owner, String repository, String branchName) {
        return String.format("https://github.com/%s/%s/tree/%s", owner, repository, branchName);
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
                    writer.triple(RdfCommitUtils.createCommitDiffEntryRdfTypeProperty(diffEntryNode));

                    DiffEntry.ChangeType changeType = diffEntry.getChangeType(); // ADD,DELETE,MODIFY,RENAME,COPY

                    if (log.isDebugEnabled())
                        log.debug("Set RDF commit diff entry edit type property");

                    writer.triple(RdfCommitUtils.createCommitDiffEntryChangeTypeProperty(diffEntryNode, changeType));

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
                        writer.triple(RdfCommitUtils.createCommitDiffEditRdfTypeProperty(editNode));

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
        return instant.atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime localDateTimeFrom(long milliSecondsSinceEpoch) {
        Instant instant = Instant.ofEpochMilli(milliSecondsSinceEpoch);
        return instant.atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    // Github Workflows
    private void writeWorkflowRunData(String repositoryUri, GHWorkflowRun run, StreamRDF writer, String issueUri, String mergeSha)
            throws IOException, InterruptedException {

        String runUri = GithubUriUtils.getWorkflowRunUri(repositoryUri, String.valueOf(run.getId()));

        // Write workflow run properties
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunProperty(issueUri, runUri));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunRdfTypeProperty(runUri));
        
        // Use platform properties instead of GitHub-specific ones
        writer.triple(RdfPlatformWorkflowExecutionUtils.createRunIdProperty(runUri, run.getId()));
        
        // Add executionOf property to link workflow run back to the pull request
        writer.triple(RdfPlatformWorkflowExecutionUtils.createExecutionOfProperty(runUri, issueUri));

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
            // Use platform triggerEvent property instead of GitHub-specific workflowEvent
            writer.triple(RdfPlatformWorkflowExecutionUtils.createTriggerEventProperty(runUri, run.getEvent().toString()));
        }

        writer.triple(RdfGithubWorkflowUtils.createWorkflowCommitShaProperty(runUri, mergeSha));
        
        // Add GitHub run attempt (natively supported by GitHub API)
        writer.triple(RdfGithubWorkflowUtils.createRunAttemptProperty(runUri, run.getRunAttempt()));
        
        try {
            if (run.getCreatedAt() != null) {
                // Use platform createdAt property instead of GitHub-specific workflowCreatedAt
                writer.triple(RdfPlatformWorkflowExecutionUtils.createCreatedAtProperty(runUri,
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

        try {
            PagedIterable<GHWorkflowJob> jobIterable = executeWithRetry(
                    () -> run.listJobs().withPageSize(10), // Smaller page size
                    "listJobs for run " + run.getId());

            int jobsProcessed = 0;

            for (GHWorkflowJob job : jobIterable) {
                if (jobsProcessed >= maxJobsToProcess) {
                    log.info("Reached max jobs limit ({}) for workflow run {}", maxJobsToProcess, run.getId());
                    break;
                }

                try {
                    writeJobProperties(job, writer, runUri, repositoryUri);
                    jobsProcessed++;
                } catch (Exception e) {
                    log.warn("Error processing job {} for workflow run {}: {}", 
                            job.getId(), run.getId(), e.getMessage());
                    // Continue processing other jobs
                }
            }

            log.info("Processed {} jobs for workflow run {}", jobsProcessed, run.getId());
        } catch (Exception e) {
            log.warn("Error fetching jobs for workflow run {}: {}", run.getId(), e.getMessage());
            // Don't rethrow - continue processing without jobs data
        }
    }

    private void writeJobProperties(GHWorkflowJob job, StreamRDF writer, String runUri, String repositoryUri) {
        String jobUri = GithubUriUtils.getWorkflowJobUri(runUri, job.getId());
        String jobUrl = GithubUriUtils.getWorkflowJobUrl(repositoryUri, job.getId());

        writer.triple(RdfGithubWorkflowUtils.createWorkflowJobProperty(runUri, jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobRdfTypeProperty(jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobIdProperty(jobUri, job.getId()));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobApiUrlProperty(jobUri, jobUrl));

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
                            step.getStartedAt().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()));
                }
                if (step.getCompletedAt() != null) {
                    writer.triple(RdfGithubWorkflowStepUtils.createWorkflowStepCompletedAtProperty(stepUri,
                            step.getCompletedAt().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()));
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

    private List<GHPullRequestReview> getReviewsCached(GHPullRequest pr) {
        int number = pr.getNumber();
        List<GHPullRequestReview> reviews = reviewCache.get(number);
        if (reviews == null) {
            try {
                reviews = executeWithRetry(() -> pr.listReviews().toList(), "listReviews for PR " + number);
                reviewCache.put(number, reviews);
            } catch (org.kohsuke.github.HttpException e) {
                // Handle GitHub API errors gracefully - don't crash the entire processing
                if (e.getResponseCode() >= 500) {
                    log.warn("GitHub server error (HTTP {}) fetching reviews for PR {}. Skipping reviews for this PR.", 
                            e.getResponseCode(), number);
                } else if (e.getResponseCode() == 403) {
                    log.warn("GitHub API access denied (HTTP 403) fetching reviews for PR {}. This may be due to rate limits, permissions, or app restrictions. Skipping reviews for this PR.", number);
                } else if (e.getResponseCode() == 404) {
                    log.warn("GitHub API not found (HTTP 404) fetching reviews for PR {}. PR may have been deleted. Skipping reviews for this PR.", number);
                } else {
                    log.warn("GitHub API error (HTTP {}) fetching reviews for PR {}: {}. Skipping reviews for this PR.", 
                            e.getResponseCode(), number, e.getMessage());
                }
                reviews = new ArrayList<>(); // Return empty list to continue processing
                reviewCache.put(number, reviews); // Cache empty result to avoid retry
            } catch (Exception e) {
                log.warn("Unexpected error fetching reviews for PR {}: {}. Skipping reviews for this PR.", number, e.getMessage());
                reviews = new ArrayList<>(); // Return empty list to continue processing
                reviewCache.put(number, reviews); // Cache empty result to avoid retry
            }
        }
        return reviews;
    }

    private List<GHPullRequestReviewComment> getReviewCommentsCached(GHPullRequestReview review) {
        long reviewId = review.getId();
        List<GHPullRequestReviewComment> comments = reviewCommentsCache.get(reviewId);
        if (comments == null) {
            try {
                comments = executeWithRetry(() -> review.listReviewComments().toList(),
                        "listReviewComments for review " + reviewId);
                reviewCommentsCache.put(reviewId, comments);
            } catch (org.kohsuke.github.HttpException e) {
                // Handle GitHub API errors gracefully - don't crash the entire processing
                if (e.getResponseCode() >= 500) {
                    log.warn("GitHub server error (HTTP {}) fetching review comments for review {}. Skipping review comments for this review.", 
                            e.getResponseCode(), reviewId);
                } else if (e.getResponseCode() == 403) {
                    log.warn("GitHub API access denied (HTTP 403) fetching review comments for review {}. This may be due to rate limits, permissions, or app restrictions. Skipping review comments for this review.", reviewId);
                } else if (e.getResponseCode() == 404) {
                    log.warn("GitHub API not found (HTTP 404) fetching review comments for review {}. Review may have been deleted. Skipping review comments for this review.", reviewId);
                } else {
                    log.warn("GitHub API error (HTTP {}) fetching review comments for review {}: {}. Skipping review comments for this review.", 
                            e.getResponseCode(), reviewId, e.getMessage());
                }
                comments = new ArrayList<>(); // Return empty list to continue processing
                reviewCommentsCache.put(reviewId, comments); // Cache empty result to avoid retry
            } catch (Exception e) {
                log.warn("Unexpected error fetching review comments for review {}: {}. Skipping review comments for this review.", reviewId, e.getMessage());
                comments = new ArrayList<>(); // Return empty list to continue processing
                reviewCommentsCache.put(reviewId, comments); // Cache empty result to avoid retry
            }
        }
        return comments;
    }

    private List<GHReaction> getReviewCommentReactionsCached(GHPullRequestReviewComment comment) {
        long id = comment.getId();
        List<GHReaction> reactions = reviewCommentReactionsCache.get(id);
        if (reactions == null) {
            try {
                reactions = executeWithRetry(() -> comment.listReactions().toList(),
                        "listReactions for review comment " + id);
                reviewCommentReactionsCache.put(id, reactions);
            } catch (org.kohsuke.github.HttpException e) {
                // Handle GitHub API errors gracefully - don't crash the entire processing
                if (e.getResponseCode() >= 500) {
                    log.warn("GitHub server error (HTTP {}) fetching reactions for review comment {}. Skipping reactions for this comment.", 
                            e.getResponseCode(), id);
                } else if (e.getResponseCode() == 403) {
                    log.warn("GitHub API access denied (HTTP 403) fetching reactions for review comment {}. This may be due to rate limits, permissions, or app restrictions. Skipping reactions for this comment.", id);
                } else if (e.getResponseCode() == 404) {
                    log.warn("GitHub API not found (HTTP 404) fetching reactions for review comment {}. Comment may have been deleted. Skipping reactions for this comment.", id);
                } else {
                    log.warn("GitHub API error (HTTP {}) fetching reactions for review comment {}: {}. Skipping reactions for this comment.", 
                            e.getResponseCode(), id, e.getMessage());
                }
                reactions = new ArrayList<>(); // Return empty list to continue processing
                reviewCommentReactionsCache.put(id, reactions); // Cache empty result to avoid retry
            } catch (Exception e) {
                log.error("Unexpected error fetching reactions for review comment {}: {}", id, e.getMessage());
                throw new RuntimeException("Failed to fetch reactions for review comment " + id, e);
            }
        }
        return reactions;
    }

    /**
     * Fetches an individual review comment by ID from GitHub API
     * Since GitHub API doesn't provide direct access to review comments by ID,
     * we check our existing cache first, then return null if not found
     */
    private GHPullRequestReviewComment getIndividualReviewCommentCached(GHRepository repo, long commentId)
            throws IOException, InterruptedException {
        GHPullRequestReviewComment comment = individualReviewCommentsCache.get(commentId);
        if (comment != null) {
            return comment;
        }
        
        // GitHub API doesn't provide direct access to review comments by ID
        // The comment should have been cached during normal processing
        // If it's not in cache, it means it wasn't processed yet or doesn't exist
        log.debug("Review comment {} not found in cache, may not exist or not yet processed", commentId);
        return null;
    }

    /**
     * Validates that a parent review comment exists and creates its full RDF representation if needed
     */
    private boolean validateAndEnsureParentReviewComment(StreamRDF writer, GHRepository repo, 
            long parentCommentId, String repositoryUri, String issueUri, GitHub gitHubHandle) {
        try {
            // Check if we already have this comment in our cache
            GHPullRequestReviewComment parentComment = getIndividualReviewCommentCached(repo, parentCommentId);
            
            if (parentComment == null) {
                log.warn("Parent review comment {} not found or inaccessible", parentCommentId);
                return false;
            }

            // Create the full RDF representation of the parent comment
            String parentCommentUri = GithubUriUtils.getPullRequestReviewCommentUri(
                    issueUri, String.valueOf(parentCommentId));

            // Create basic comment triples
            writer.triple(RdfGithubCommentUtils.createCommentRdfType(parentCommentUri));
            writer.triple(RdfGithubCommentUtils.createCommentId(parentCommentUri, parentComment.getId()));

            // Content and user
            if (parentComment.getBody() != null && !parentComment.getBody().isEmpty()) {
                writer.triple(RdfGithubCommentUtils.createCommentBody(parentCommentUri, parentComment.getBody()));
            }
            
            if (parentComment.getUser() != null && parentComment.getUser().getLogin() != null) {
                // Validate and ensure GitHub user exists in RDF
                String commentUserUri = GithubUserValidator.validateAndEnsureUser(writer, gitHubHandle, parentComment.getUser());
                if (commentUserUri != null) {
                    writer.triple(RdfGithubCommentUtils.createCommentUser(parentCommentUri, commentUserUri));
                } else {
                    log.warn("User validation failed for parent comment author '{}', creating fallback", parentComment.getUser().getLogin());
                    try {
                        String fallbackUserUri = GithubUserValidator.createSafeUserEntity(writer, parentComment.getUser().getLogin());
                        if (fallbackUserUri != null) {
                            writer.triple(RdfGithubCommentUtils.createCommentUser(parentCommentUri, fallbackUserUri));
                        }
                    } catch (Exception e) {
                        log.warn("Fallback user creation failed for parent comment author: {}", e.getMessage());
                    }
                }
            }
            
            if (parentComment.getCreatedAt() != null) {
                writer.triple(RdfGithubCommentUtils.createCommentCreatedAt(
                        parentCommentUri, localDateTimeFrom(parentComment.getCreatedAt())));
            }

            // Handle nested parent relationships recursively using new comment chain structure
            Long grandParentId = parentComment.getInReplyToId();
            if (grandParentId != null && grandParentId > 0 && !grandParentId.equals(parentCommentId)) {
                String grandParentCommentUri = GithubUriUtils.getPullRequestReviewCommentUri(
                        issueUri, String.valueOf(grandParentId));
                
                // Recursively ensure grandparent exists
                if (validateAndEnsureParentReviewComment(writer, repo, grandParentId, repositoryUri, issueUri, gitHubHandle)) {
                    // Use new comment chain structure - parent comments on grandparent
                    writer.triple(RdfPlatformCommentUtils.createCommentOnProperty(
                            parentCommentUri, grandParentCommentUri));
                    
                    // Grandparent has this parent as a comment
                    writer.triple(RdfGithubCommentUtils.createHasCommentProperty(
                            grandParentCommentUri, parentCommentUri));
                }
            }

            log.debug("Successfully ensured parent review comment {} exists in RDF", parentCommentId);
            return true;

        } catch (Exception e) {
            log.error("Error validating/ensuring parent review comment {}: {}", parentCommentId, e.getMessage(), e);
            return false;
        }
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

    private List<GHReaction> getIssueCommentReactionsCached(GHIssueComment comment) {
        long id = comment.getId();
        List<GHReaction> reactions = issueCommentReactionsCache.get(id);
        if (reactions == null) {
            try {
                reactions = executeWithRetry(() -> comment.listReactions().toList(),
                        "listReactions for issue comment " + id);
                issueCommentReactionsCache.put(id, reactions);
            } catch (org.kohsuke.github.HttpException e) {
                // Handle GitHub API errors gracefully - don't crash the entire processing
                if (e.getResponseCode() >= 500) {
                    log.warn("GitHub server error (HTTP {}) fetching reactions for issue comment {}. Skipping reactions for this comment.", 
                            e.getResponseCode(), id);
                } else if (e.getResponseCode() == 403) {
                    log.warn("GitHub API access denied (HTTP 403) fetching reactions for issue comment {}. This may be due to rate limits, permissions, or app restrictions. Skipping reactions for this comment.", id);
                } else if (e.getResponseCode() == 404) {
                    log.warn("GitHub API not found (HTTP 404) fetching reactions for issue comment {}. Comment may have been deleted. Skipping reactions for this comment.", id);
                } else {
                    log.warn("GitHub API error (HTTP {}) fetching reactions for issue comment {}: {}. Skipping reactions for this comment.", 
                            e.getResponseCode(), id, e.getMessage());
                }
                reactions = new ArrayList<>(); // Return empty list to continue processing
                issueCommentReactionsCache.put(id, reactions); // Cache empty result to avoid retry
            } catch (Exception e) {
                log.warn("Unexpected error fetching reactions for issue comment {}: {}. Skipping reactions for this comment.", id, e.getMessage());
                reactions = new ArrayList<>(); // Return empty list to continue processing
                issueCommentReactionsCache.put(id, reactions); // Cache empty result to avoid retry
            }
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

    private String getCleanBranchName(Ref branchRef) {
        String fullName = branchRef.getName();
        if (fullName.startsWith("refs/heads/")) {
            return fullName.substring("refs/heads/".length());
        } else if (fullName.startsWith("refs/remotes/origin/")) {
            return fullName.substring("refs/remotes/origin/".length());
        } else if (fullName.startsWith("refs/remotes/")) {
            // Handle other remotes by taking everything after the last '/'
            return fullName.substring(fullName.lastIndexOf('/') + 1);
        } else if (fullName.startsWith("refs/tags/")) {
            return fullName.substring("refs/tags/".length());
        }
        return fullName;
    }

    private <T> T executeWithRetry(java.util.concurrent.Callable<T> callable, String description)
            throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return callable.call();
            } catch (org.kohsuke.github.HttpException e) {
                last = e;
                if (attempt == 2) {
                    throw e;
                }
                // Enhanced GitHub HttpException logging
                String errorDetails = String.format("HTTP %d: %s", e.getResponseCode(), e.getMessage());
                if (e.getResponseHeaderFields() != null && !e.getResponseHeaderFields().isEmpty()) {
                    errorDetails += " Headers: " + e.getResponseHeaderFields();
                }
                log.warn("{} failed on attempt {}/2: {}. Retrying after delay...", description, attempt, errorDetails);
                Thread.sleep(RETRY_DELAY_MS);
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
