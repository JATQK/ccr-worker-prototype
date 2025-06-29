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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHWorkflowJob;
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
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGitCommitUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueCommentUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueDiscussionUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueReviewUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubWorkflowJobUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubWorkflowUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfTurtleTidier;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GithubRdfConversionTransactionService {

    private static final int TWENTY_FIVE_MEGABYTE = 1024 * 1024 * 25;

    // TODO: replace PURL

    public static final String GIT_NAMESPACE = "git";
    public static final String GIT_URI = "https://purl.archive.org/git2rdflab/v1/git2RDFLab-git#";

    public static final String PLATFORM_NAMESPACE = "platform";
    public static final String PLATFORM_URI = "https://purl.archive.org/git2rdflab/v1/git2RDFLab-platform#";

    public static final String PLATFORM_GITHUB_URI = "https://purl.archive.org/git2rdflab/v1/git2RDFLab-platform-github#";
    public static final String PLATFORM_GITHUB_NAMESPACE = "github";

    public static final String XSD_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema#";
    public static final String XSD_SCHEMA_NAMESPACE = "xsd";

    public static final String RDF_SCHEMA_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_SCHEMA_NAMESPACE = "rdf";

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

    private void writeWorkflowRunInfo(GHPullRequest pr, StreamRDF writer, String issueUri)
            throws IOException, InterruptedException {
    if (pr == null ) {
        return;
    }

    try {
        if (!pr.isMerged()) {
            log.debug("Pull request {} is not merged, skipping workflow run info.", pr.getHtmlUrl());
            return;
        }
        // Approach 1: Get check runs directly from the PR's head commit
        String headSha = pr.getHead().getSha();
        GHRepository repo = pr.getRepository();

        // Get check runs for the specific commit
        List<GHCheckRun> checkRuns = executeWithRetry(
                () -> repo.getCommit(headSha).getCheckRuns().toList(),
                "getCheckRuns for " + headSha);
        
        for (GHCheckRun checkRun : checkRuns) {
            // Check runs have details_url that often points to workflow runs
            if (checkRun.getDetailsUrl() != null && 
                checkRun.getDetailsUrl().toString().contains("/actions/runs/")) {
                
                // Extract workflow run ID from URL
                String runId = extractRunIdFromUrl(checkRun.getDetailsUrl().toString());
                if (runId != null) {
                    GHWorkflowRun run = executeWithRetry(
                            () -> repo.getWorkflowRun(Long.parseLong(runId)),
                            "getWorkflowRun " + runId);
                    writeWorkflowRunData(run, writer, issueUri, headSha);
                }
            }
        }
        
    } catch (IOException e) {
        log.warn("Error fetching workflow runs via check runs: {}", e.getMessage());
        // Fallback to original approach if needed
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
            String repositoryUri = getGithubRepositoryUri(owner, repositoryName);

            GHRepository githubRepositoryHandle = gitHubHandle.getRepository(githubRepositoryName);
            // See: https://jena.apache.org/documentation/io/rdf-output.html#streamed-block-formats
            StreamRDF writer = StreamRDFWriter.getWriterStream(outputStream, RDFFormat.TURTLE_BLOCKS);

            writer.prefix(XSD_SCHEMA_NAMESPACE, XSD_SCHEMA_URI);
            writer.prefix(RDF_SCHEMA_NAMESPACE, RDF_SCHEMA_URI);
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

            Map<ObjectId, List<String>> commitToTags = getTagsForCommits(gitRepository);

            Map<String, PullRequestInfo> commitPrMap = buildCommitPrMap(githubRepositoryHandle);

            lockHandler.renewLockOnRenewTimeFulfillment();

            // Metadata

            writer.start();
            writer.triple(RdfCommitUtils.createRepositoryRdfTypeProperty(repositoryUri));
            writer.triple(RdfCommitUtils.createRepositoryOwnerProperty(repositoryUri, owner));
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

            // Submodules

            writer.start();

            SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(gitRepository);

            while (submoduleWalk.next()) {

                try {
                    String submodulePath = submoduleWalk.getPath();
                    String submoduleUrl = submoduleWalk.getModulesUrl();
                    String submoduleCommitHash = submoduleWalk.getObjectId().getName();
                    String submoduleCommitHashUri = getGithubCommitUri(owner, repositoryName, submoduleCommitHash);

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

            // git commits
            // ********************** ** REMOVE ON DEPLOYMENT ** **********************
            // REMOVE ON DEPLOYMENT
            var computeCommits = true; // Set to false to skip commit processing
            var maxComputedCommits = 200;  // limit the number of commits to process
            // ********************** ** ******************** ** **********************

            if (computeCommits) {
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
                        maxComputedCommits--;
                        if (maxComputedCommits < 0) {
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

                        String commitUri = getGithubCommitUri(owner, repositoryName, gitHash);
                        //String commitUri = GIT_NAMESPACE + ":GitCommit";

                        if (log.isDebugEnabled())
                            log.debug("Set rdf type property commitUri");

                        writer.triple(RdfCommitUtils.createRdfTypeProperty(commitUri));

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
                            String parentUri = getGithubCommitUri(owner, repositoryName, parent.getName());
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
                            calculateCommitBranch(commitBranchCalculator, writer, commit, commitUri);
                        }

                        List<String> tagNames = commitToTags.get(commitId);

                        if (tagNames != null && !tagNames.isEmpty()) {

                            for (String tagName : tagNames) {
                                writer.triple(RdfCommitUtils.createCommitTagProperty(commitUri, tagName));
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
                        getGithubCommitUri(owner, repositoryName, headCommitId.getName()),
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
            int issueOutsideCounter = 0;

            if (githubIssueRepositoryFilter.doesContainAtLeastOneEnabledFilterOption()) {

                if (githubRepositoryHandle.hasIssues()) {

                    log.info("Start issue processing");
                    int issueCounter = 0;
                    boolean doesWriterContainNonWrittenRdfStreamElements = false;

                    for (GHIssue ghIssue : githubRepositoryHandle.queryIssues().state(GHIssueState.ALL).pageSize(100)
                            .list()) {

                        if (issueCounter < 1) {
                            log.info("Start issue rdf conversion batch");
                            writer.start();
                            doesWriterContainNonWrittenRdfStreamElements = true;
                        }

                        int issueNumber = ghIssue.getNumber();
                        String issueUri = ghIssue.getHtmlUrl().toString();

                        if (issueUri == null || issueUri.isEmpty()) {
                            log.warn(
                                    "Issue with number {} fallback to githubRepositoryURI because its issueUri is null or empty",
                                    issueNumber);
                            issueUri = getGithubRepositoryUri(owner, repositoryName);
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
                        // REMOVE ON DEPLOYMENT
                        // if (!"9956".equals(String.valueOf(issueNumber)) && !"9954".equals(String.valueOf(issueNumber))) {
                        //     continue;
                        // }
                        // REMOVE ON DEPLOYMENT
                        // if (githubIssueRepositoryFilter.isEnableIssueState()
                        //         && ghIssue.getState() != GHIssueState.CLOSED) {
                        //     log.warn("Issue with number {} is not closed, skipping", issueNumber);
                        //     continue;
                        // }
                        // REMOVE ON DEPLOYMENT
                        // if (issueCounter >= 50) {
                        // continue;
                        // }
                        // REMOVE ON DEPLOYMENT
                        // if (issueCounter >= 100) {
                        //     break;
                        // }
                        //REMOVE ON DEPLOYMENT
                        if (issueOutsideCounter >= 200) {
                            log.warn("Skipping issue with number {} because outside counter reached limit",
                                    issueNumber);
                            break;
                        }
                        // ********************** ** **************** ** **********************

                        writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(issueUri));
                        writer.triple(RdfGithubIssueUtils.createIssueRepositoryProperty(
                                issueUri,
                                getGithubRepositoryUri(owner, repositoryName)));

                        if (githubIssueRepositoryFilter.isEnableIssueNumber()) {
                            writer.triple(RdfGithubIssueUtils.createIssueNumberProperty(issueUri, issueNumber));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueTitle() && ghIssue.getTitle() != null) {
                            writer.triple(
                                    RdfGithubIssueUtils.createIssueTitleProperty(issueUri, ghIssue.getTitle()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueBody() && ghIssue.getBody() != null) {
                            writer.triple(
                                    RdfGithubIssueUtils.createIssueBodyProperty(issueUri, ghIssue.getBody()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueStateProperty(issueUri,
                                    ghIssue.getState().toString()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUser() && ghIssue.getUser() != null) {
                            String githubIssueUserUri = ghIssue.getUser().getHtmlUrl().toString();
                            writer.triple(
                                    RdfGithubIssueUtils.createIssueUserProperty(issueUri, githubIssueUserUri));
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

                        if (githubIssueRepositoryFilter.isEnableIssueMergedInfo()) {
                            if (ghIssue.isPullRequest()) {
                                GHPullRequest pullRequest = getPullRequestCached(
                                        githubRepositoryHandle, issueNumber);
                                writeMergeInfo(ghIssue, pullRequest, writer, issueUri);
                                writeWorkflowRunInfo(pullRequest, writer, issueUri);
                            }
                        }

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
                                String reviewUri = issueUri + "/reviews/" + reviewId;

                                // Static Properties
                                writer.triple(RdfGithubIssueReviewUtils.createIssueReviewProperty(issueUri, reviewUri));
                                writer.triple(RdfGithubIssueReviewUtils.createIssueReviewRdfTypeProperty(reviewUri));
                                writer.triple(
                                        RdfGithubIssueReviewUtils.createReviewIdentifierProperty(reviewUri, reviewId));
                                writer.triple(RdfGithubIssueReviewUtils.createReviewOfProperty(reviewUri, issueUri));

                                // Dynamic Properties 
                                if (review.getBody() != null && !review.getBody().isEmpty()) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewDescriptionProperty(reviewUri,
                                            review.getBody()));
                                }
                                if (review.getState() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewStateProperty(reviewUri,
                                            review.getState().toString()));
                                }
                                if (review.getSubmittedAt() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewSubmittedAtProperty(reviewUri,
                                            localDateTimeFrom(review.getSubmittedAt())));
                                }
                                if (review.getUser() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewUserProperty(reviewUri,
                                            review.getUser().getHtmlUrl().toString()));
                                }
                                if (review.getCommitId() != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createReviewCommitIdProperty(reviewUri,
                                            review.getCommitId()));
                                }

                                // Review Discussions
                                List<GHPullRequestReviewComment> reviewComments = getReviewCommentsCached(review);
                                int reviewCommentCount = reviewComments.size();

                                // If there are no review comments, skip further processing about comments for this review
                                if (reviewCommentCount == 0) {
                                    continue;
                                }

                                String _discussionUri = issueUri + "#discussion_r";

                                int rootCommentCount = 0;
                                Set<Long> threadIds = new HashSet<>();
                                Map<Long, List<Long>> repliesByParent = new HashMap<>();
                                LocalDateTime firstCommentAt = null;
                                LocalDateTime lastCommentAt = null;

                                // Process the discussion of the review
                                for (GHPullRequestReviewComment c : reviewComments) {
                                    LocalDateTime created = localDateTimeFrom(c.getCreatedAt());
                                    LocalDateTime updated = c.getUpdatedAt() != null
                                            ? localDateTimeFrom(c.getUpdatedAt())
                                            : created;
                                    if (firstCommentAt == null || created.isBefore(firstCommentAt)) {
                                        firstCommentAt = created;
                                    }
                                    if (lastCommentAt == null || updated.isAfter(lastCommentAt)) {
                                        lastCommentAt = updated;
                                    }

                                    Long parentId = c.getInReplyToId();
                                    long threadId = parentId != null ? parentId : c.getId();
                                    threadIds.add(threadId);

                                    if (parentId == null || parentId == c.getId()) {
                                        rootCommentCount++;
                                    } else {
                                        repliesByParent.computeIfAbsent(parentId, k -> new ArrayList<>())
                                                .add(c.getId());
                                    }
                                }

                                // Static Properties
                                writer.triple(RdfGithubIssueReviewUtils.createReviewCommentCountProperty(reviewUri,
                                        reviewCommentCount));
                                writer.triple(RdfGithubIssueReviewUtils.createRootCommentCountProperty(reviewUri,
                                        rootCommentCount));
                                writer.triple(RdfGithubIssueReviewUtils.createThreadCountProperty(reviewUri,
                                        threadIds.size()));

                                if (firstCommentAt != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createFirstCommentAtProperty(reviewUri,
                                            firstCommentAt));
                                }
                                if (lastCommentAt != null) {
                                    writer.triple(RdfGithubIssueReviewUtils.createLastCommentAtProperty(reviewUri,
                                            lastCommentAt));
                                    writer.triple(RdfGithubIssueReviewUtils.createLastActivityProperty(reviewUri,
                                            lastCommentAt));
                                }

                                // Review Discussion Comments
                                for (GHPullRequestReviewComment c : reviewComments) {
                                    long cid = c.getId();
                                    String discussionUri = _discussionUri + cid;

                                    writer.triple(RdfGithubIssueReviewUtils.createReviewDiscussionProperty(reviewUri,
                                            discussionUri));

                                    writer.triple(RdfGithubIssueDiscussionUtils.createReviewDiscussionRdfTypeProperty(
                                            discussionUri));
                                    writer.triple(RdfGithubIssueDiscussionUtils
                                            .createDiscussionIdentifierProperty(discussionUri, cid));
                                    writer.triple(RdfGithubIssueDiscussionUtils
                                            .createReviewDiscussionOfProperty(discussionUri, reviewUri));
                                    if (c.getBody() != null && !c.getBody().isEmpty()) {
                                        writer.triple(RdfGithubIssueDiscussionUtils
                                                .createDiscussionDescriptionProperty(discussionUri, c.getBody()));
                                    }
                                    if (c.getUser() != null) {
                                        writer.triple(RdfGithubIssueDiscussionUtils.createDiscussionUserProperty(
                                                discussionUri, c.getUser().getHtmlUrl().toString()));
                                    }
                                    if (c.getCreatedAt() != null) {
                                        writer.triple(RdfGithubIssueDiscussionUtils.createDiscussionCreatedAtProperty(
                                                discussionUri, localDateTimeFrom(c.getCreatedAt())));
                                    }
                                }
                            }
                        }

                        // Issue Comments
                        if (githubIssueRepositoryFilter.isEnableIssueComments()) {
                            List<GHIssueComment> issueComments = getIssueCommentsCached(ghIssue);
                            for (GHIssueComment c : issueComments) {
                                long cid = c.getId();
                                String commentUri = issueUri + "#issuecomment-" + cid;

                                writer.triple(RdfGithubIssueCommentUtils.createReviewCommentRdfTypeProperty(commentUri));
                                writer.triple(RdfGithubIssueCommentUtils.createCommentIdentifierProperty(commentUri, cid));
                                writer.triple(RdfGithubIssueCommentUtils.createReviewCommentOfProperty(commentUri, issueUri));

                                if (c.getBody() != null && !c.getBody().isEmpty()) {
                                    writer.triple(RdfGithubIssueCommentUtils.createCommentDescriptionProperty(commentUri, c.getBody()));
                                }
                                if (c.getUser() != null) {
                                    writer.triple(RdfGithubIssueCommentUtils.createCommentUserProperty(commentUri,
                                            c.getUser().getHtmlUrl().toString()));
                                }
                                if (c.getCreatedAt() != null) {
                                    writer.triple(RdfGithubIssueCommentUtils.createCommentCreatedAtProperty(commentUri,
                                            localDateTimeFrom(c.getCreatedAt())));
                                }
                                // Issue comments have no threading, treat all as root
                                writer.triple(RdfGithubIssueCommentUtils.createCommentIsRootProperty(commentUri, true));
                            }
                        }

                        // Count the iteration of issues
                        issueCounter++;
                        // Count the outside of limit the number of issues to process
                        issueOutsideCounter++;

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
        PagedIterable<GHPullRequest> prs = executeWithRetry(
                () -> repo.queryPullRequests().state(GHIssueState.CLOSED).list(),
                "queryPullRequests");

        ZonedDateTime oneYearAgo = ZonedDateTime.now().minusYears(1);

        for (GHPullRequest pr : prs) {
            if (!pr.isMerged()) {
                continue;
            }

            Date merged = pr.getMergedAt();
            if (merged == null || merged.toInstant().isBefore(oneYearAgo.toInstant())) {
                continue;
            }

            String prUri = pr.getHtmlUrl().toString();
            LocalDateTime mergedAt = localDateTimeFrom(merged);

            PullRequestInfo info = new PullRequestInfo(prUri, pr.getMergeCommitSha(), mergedAt);

            if (pr.getMergeCommitSha() != null) {
                map.put(pr.getMergeCommitSha(), info);
            }

            for (GHPullRequestCommitDetail c : getCommitsCached(pr)) {
                map.put(c.getSha(), info);
            }
        }

        return map;
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

        if (uniqueGitCommiterWithHash.containsKey(email)) {

            if (log.isDebugEnabled())
                log.debug("Found github committer email in hash");

            RdfGitCommitUserUtils commitInfo = uniqueGitCommiterWithHash.get(email);
            if (commitInfo.gitHubUser != null && !commitInfo.gitHubUser.isEmpty()) {

                if (log.isDebugEnabled())
                    log.debug("Set RDF committer github user property after finding github committer email in hash");

                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, commitInfo.gitHubUser));
            }
        } else {

            if (log.isDebugEnabled())
                log.debug("Did not find github committer email in hash");

            String gitHubUser = RdfGitCommitUserUtils.getGitHubUserFromCommit(githubRepositoryHandle, gitHash);
            uniqueGitCommiterWithHash.put(email, new RdfGitCommitUserUtils(gitHash, gitHubUser));
            if (gitHubUser != null && !gitHubUser.isEmpty()) {

                if (log.isDebugEnabled())
                    log.debug(
                            "Set RDF committer github user property after not finding it in github committer email in hash");

                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, gitHubUser));
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
            String issueUri = getGithubIssueUri(owner, repositoryName, number);
            writer.triple(RdfCommitUtils.createCommitReferencesIssueProperty(commitUri, issueUri));
            writer.triple(RdfGithubIssueUtils.createIssueReferencedByCommitProperty(issueUri, commitUri));
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
            String commitUri) {

        String commitHash = currentCommit.getName();

        List<String> branches = commitBranchCalculator.getBranchesForShaHashOfCommit(commitHash);

        for (String branchName : branches) {
            writer.triple(RdfCommitUtils.createCommitBranchNameProperty(commitUri, branchName));
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

    private String getGithubRepositoryUri(String owner, String repository) {
        return "https://github.com/" + owner + "/" + repository + "/";
    }

    private String getGithubCommitBaseUri(String owner, String repository) {
        return "https://github.com/" + owner + "/" + repository + "/commit/";
    }

    private String getGithubCommitUri(String owner, String repository, String commitHash) {
        return getGithubCommitBaseUri(owner, repository) + commitHash;
    }

    private String getGithubIssueBaseUri(String owner, String repository) {
        return "https://github.com/" + owner + "/" + repository + "/issues/";
    }

    private String getGithubIssueUri(String owner, String repository, String issueNumber) {
        return getGithubIssueBaseUri(owner, repository) + issueNumber;
    }

    public String getGithubUserUri(String userName) {
        return "https://github.com/" + userName;
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

    private void writeWorkflowRunData(GHWorkflowRun run, StreamRDF writer, String issueUri, String mergeSha)
            throws IOException, InterruptedException {

        String runUri = run.getHtmlUrl().toString();

        // Write workflow run properties
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunProperty(issueUri, runUri));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunRdfTypeProperty(runUri));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunIdProperty(runUri, run.getId()));

        // Batch property writes to reduce overhead
        writeWorkflowRunProperties(run, writer, runUri, mergeSha);

        // Optimization 2: Only fetch jobs if needed, with pagination control
        if (shouldIncludeJobDetails(run)) {
            writeWorkflowJobData(run, writer, runUri);
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
            writer.triple(RdfGithubWorkflowUtils.createWorkflowEventProperty(runUri, run.getEvent()));
        }

        writer.triple(RdfGithubWorkflowUtils.createWorkflowRunNumberProperty(runUri, run.getRunNumber()));
        writer.triple(RdfGithubWorkflowUtils.createWorkflowCommitShaProperty(runUri, mergeSha));
        try {
            if (run.getHtmlUrl() != null) {
                writer.triple(
                        RdfGithubWorkflowUtils.createWorkflowHtmlUrlProperty(runUri, run.getHtmlUrl().toString()));
            }
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

    private void writeWorkflowJobData(GHWorkflowRun run, StreamRDF writer, String runUri) throws IOException, InterruptedException {
        // Optimization 3: Limit job fetching with early termination
        PagedIterable<GHWorkflowJob> jobIterable = executeWithRetry(
                () -> run.listJobs().withPageSize(25),
                "listJobs for run " + run.getId());

        int maxJobsToProcess = 30; // Reasonable limit on jobs per run
        int jobsProcessed = 0;

        for (GHWorkflowJob job : jobIterable) {
            if (jobsProcessed >= maxJobsToProcess) {
                log.debug("Reached max jobs limit ({}) for workflow run {}", maxJobsToProcess, run.getId());
                break;
            }

            writeJobProperties(job, writer, runUri);
            jobsProcessed++;
        }

        log.debug("Processed {} jobs for workflow run {}", jobsProcessed, run.getId());
    }

    private void writeJobProperties(GHWorkflowJob job, StreamRDF writer, String runUri) {
        String jobUri = runUri + "/jobs/" + job.getId();

        writer.triple(RdfGithubWorkflowUtils.createWorkflowJobProperty(runUri, jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobRdfTypeProperty(jobUri));
        writer.triple(RdfGithubWorkflowJobUtils.createWorkflowJobIdProperty(jobUri, job.getId()));

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
    }

    private boolean shouldIncludeJobDetails(GHWorkflowRun run) {
        // Optimization 4: Only fetch job details for certain conditions
        // You can customize this logic based on your needs
        return run.getConclusion() != null &&
                (run.getConclusion().equals("failure") ||
                        run.getConclusion().equals("success"));
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
