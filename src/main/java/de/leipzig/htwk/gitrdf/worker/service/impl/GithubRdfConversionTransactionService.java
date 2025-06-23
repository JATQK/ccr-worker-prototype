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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.hibernate.engine.jdbc.BlobProxy;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
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
import de.leipzig.htwk.gitrdf.worker.utils.GitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.ZipUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGitCommitUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueUtils;
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

    // TODO (ccr): Refactor -> code is currently not in use -> maybe delete or refactor to a cleaner state
    @Transactional(rollbackFor = {IOException.class, GitAPIException.class, URISyntaxException.class, InterruptedException.class}) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGithubRepoToRdfConversionAndReturnCloseableInputStream(
            long id, File rdfTempFile, LockHandler lockHandler) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        GitHub githubHandle = githubHandlerService.getGithub();

        // du kriegst die .git nicht als Teil der Zip ->
        //gitHubHandle.getRepository("").listCommits()

        GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs
                = entityManager.find(GithubRepositoryOrderEntityLobs.class, id);

        GithubRepositoryOrderEntity githubRepositoryOrderEntity
                = entityManager.find(GithubRepositoryOrderEntity.class, id);

        String owner = githubRepositoryOrderEntity.getOwnerName();
        String repo = githubRepositoryOrderEntity.getRepositoryName();

        GHRepository targetRepo = getGithubRepositoryHandle(owner, repo, githubHandle);

        File gitFile = getDotGitFileFromGithubRepositoryHandle(targetRepo, id, owner, repo);

        InputStream needsToBeClosedOutsideOfTransaction
                = writeRdf(gitFile, githubRepositoryOrderEntity, githubRepositoryOrderEntityLobs, rdfTempFile, new TimeLog(false), lockHandler);

        githubRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

        return needsToBeClosedOutsideOfTransaction;
    }

    @Transactional(rollbackFor = {IOException.class, GitAPIException.class, URISyntaxException.class, InterruptedException.class}) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGithubRepoToRdfConversionWithGitCloningLogicAndReturnCloseableInputStream(
            long id,
            File gitWorkingDirectory,
            File rdfTempFile,
            TimeLog timeLog,
            LockHandler lockHandler) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        Git gitHandler = null;

        try {

            GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs
                    = entityManager.find(GithubRepositoryOrderEntityLobs.class, id);

            GithubRepositoryOrderEntity githubRepositoryOrderEntity
                    = entityManager.find(GithubRepositoryOrderEntity.class, id);

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

            InputStream needsToBeClosedOutsideOfTransaction
                    = writeRdf(gitHandler, githubRepositoryOrderEntity, githubRepositoryOrderEntityLobs, rdfTempFile, timeLog, lockHandler);

            conversionWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Conversion time in milliseconds is: '{}'", conversionWatch.getTime());
            timeLog.setConversionTime(conversionWatch.getTime());

            githubRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

            return needsToBeClosedOutsideOfTransaction;

        } finally {

            if (gitHandler != null) gitHandler.close();

        }

    }

    private Git performGitClone(String ownerName, String repositoryName, File gitWorkingDirectory) throws GitAPIException {

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

    private void throwExceptionIfThereIsNotExactlyOneFileInTheExtractedZipDirectory(
            File[] filesOfExtractedZipDirectory,
            long entityId,
            String ownerName,
            String repoName) {

        int fileAmountInExtractedZipDirectory = filesOfExtractedZipDirectory.length;

        throwExceptionIfThereAreNoFilesInTheExtractedZipDirectory(
                fileAmountInExtractedZipDirectory, entityId, ownerName, repoName);

        throwExceptionIfThereIsMoreThanOneFileInTheExtractedZipDirectory(
                filesOfExtractedZipDirectory, entityId, ownerName, repoName);
    }

    private void throwExceptionIfThereAreNoFilesInTheExtractedZipDirectory(
            int fileAmountInExtractedZipDirectory,
            long entityId,
            String ownerName,
            String repoName) {

        if (fileAmountInExtractedZipDirectory < 1) {
            String exceptionMessage = String.format("Error extracting files from zip for github repository with id " +
                    "'%d', owner '%s' and repository name '%s'. Expected exactly one single file in the " +
                    "extracted github repository zip. But found no files instead.", entityId, ownerName, repoName);

            throw new RuntimeException(exceptionMessage);
        }
    }

    private void throwExceptionIfThereIsMoreThanOneFileInTheExtractedZipDirectory(
            File[] filesOfExtractedZipDirectory,
            long entityId,
            String ownerName,
            String repoName) {

        int fileAmountInExtractedZipDirectory = filesOfExtractedZipDirectory.length;

        if (fileAmountInExtractedZipDirectory > 1) {
            String exceptionMessage = String.format("Error extracting files from zip for github repository with id " +
                            "'%d', owner '%s' and repository name '%s'. Expected only one single file in the " +
                            "extracted github repository zip. But found %d files. Names of the files are: '%s'",
                    entityId,
                    ownerName,
                    repoName,
                    fileAmountInExtractedZipDirectory,
                    getNamesOfFiles(filesOfExtractedZipDirectory));

            throw new RuntimeException(exceptionMessage);
        }
    }

    private String getNamesOfFiles(File[] files) {

        StringBuilder fileNameBuilder = new StringBuilder("[");

        boolean firstEntry = true;

        for (File file : files) {

            if (firstEntry) {
                fileNameBuilder.append(String.format("%s", file.getName()));
                firstEntry = false;
            } else {
                fileNameBuilder.append(String.format(", %s", file.getName()));
            }

        }

        fileNameBuilder.append("]");
        return fileNameBuilder.toString();
    }

    private GHRepository getGithubRepositoryHandle(String ownerName, String repositoryName, GitHub gitHubHandle) throws IOException {
        String targetRepoName = String.format("%s/%s", ownerName, repositoryName);
        return gitHubHandle.getRepository(targetRepoName);
    }

    private File getDotGitFileFromGithubRepositoryHandle(
            GHRepository githubRepositoryHandle,
            long entityId,
            String ownerName,
            String repositoryName) throws IOException {

        File extractedZipFileDirectory = githubRepositoryHandle.readZip(
                input -> ZipUtils.extractZip(input, TWENTY_FIVE_MEGABYTE),
                null);

        File[] filesOfExtractedZipDirectory = extractedZipFileDirectory.listFiles();

        if (filesOfExtractedZipDirectory == null) {

            String exceptionMessage = String.format("Error while trying to list files of extracted zip file directory. " +
                            "Returned value is null. Id is: '%d', owner is: '%s' and repository name is: '%s'",
                    entityId, ownerName, repositoryName);

            throw new RuntimeException(exceptionMessage);
        }

        throwExceptionIfThereIsNotExactlyOneFileInTheExtractedZipDirectory(
                filesOfExtractedZipDirectory, entityId, ownerName, repositoryName);

        File parentFolderOfDotGitFile = filesOfExtractedZipDirectory[0];

        return GitUtils.getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(
                parentFolderOfDotGitFile);
    }

    // TODO (ccr): duplicate code -> change that!
    private InputStream writeRdf(
            File gitFile,
            GithubRepositoryOrderEntity entity,
            GithubRepositoryOrderEntityLobs entityLobs,
            File rdfTempFile,
            TimeLog timeLog,
            LockHandler lockHandler) throws GitAPIException, IOException, URISyntaxException, InterruptedException {

        Repository gitRepository = new FileRepositoryBuilder().setGitDir(gitFile).build();
        Git gitHandler = new Git(gitRepository);

        return writeRdf(gitHandler, entity, entityLobs, rdfTempFile, timeLog, lockHandler);
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

        GitCommitRepositoryFilter gitCommitRepositoryFilter
                = entity.getGithubRepositoryFilter().getGitCommitRepositoryFilter();

        GithubIssueRepositoryFilter githubIssueRepositoryFilter
                = entity.getGithubRepositoryFilter().getGithubIssueRepositoryFilter();

        String githubCommitPrefixValue = getGithubCommitBaseUri(owner, repositoryName);
        String githubIssuePrefixValue = getGithubIssueBaseUri(owner, repositoryName);

        Map<String, RdfGitCommitUserUtils> uniqueGitCommiterWithHash = new HashMap<>();
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(rdfTempFile))) {

            //GithubHandle githubHandle = githubHandlerService.getGithubHandle();
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
            diffFormatter.setRepository( gitRepository );
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

            //// Tags (annotated only)
            //List<Ref> tags = gitHandler.tagList().call();
            //Map<ObjectId,String> tagNames = new HashMap<>();
            //
            //for (Ref tag : tags) {
            //    ObjectId tagObjectId = tag.getObjectId();
            //
            //    if (tagObjectId == null) continue;
            //
            //    String tagName = tag.getName();
            //    String tagCommitHash = tagObjectId.getName();
            //
            //    tagNames.put(tagObjectId, tagName);
            //
            //    log.info("Added Tag '{}' #{}", tagName, tagCommitHash);
            //}

            Map<ObjectId, List<String>> commitToTags = getTagsForCommits(gitRepository);

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
                    writer.triple(RdfCommitUtils.createSubmoduleCommitEntryProperty(submoduleNode, submoduleCommitHashUri));
                    writer.triple(RdfCommitUtils.createSubmoduleCommitProperty(submoduleNode, submoduleCommitHash));

                    log.info("Submodule: path: {} url: {} commit-hash: {}", submodulePath, submoduleUrl, submoduleCommitHash);
                }
                catch (ConfigInvalidException e) {
                    log.error("Submodule Invalid Config Error: {}", e.getMessage());
                }
                catch (Exception e) {
                    log.error("Submodule Error: {}", e.getMessage());
                }
            }

            writer.finish();

            lockHandler.renewLockOnRenewTimeFulfillment();

            // git commits
            for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                log.info("Start iterations of git commits. Current iteration count: {}", iteration);

                if (log.isDebugEnabled()) log.debug("Check whether github installation token needs refresh");

                int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration, commitsPerIteration);

                log.info("Calculated skip count for this iteration is: {}", skipCount);

                Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration).call();

                boolean finished = true;

                if (log.isDebugEnabled()) log.debug("Starting commit writer rdf");

                writer.start();

                if (log.isDebugEnabled()) log.debug("Starting commit loop");

                for (RevCommit commit : commits) {

                    finished = false;

                    ObjectId commitId = commit.getId();
                    String gitHash = commitId.name();

                    PersonIdent authorIdent = null;
                    PersonIdent committerIdent = null;

                    try {

                        authorIdent = commit.getAuthorIdent();

                    } catch (RuntimeException ex) {

                        log.warn("Error while trying to identify the author for the git commit '{}'. " +
                                "Skipping author email and name entry. Error is '{}'", gitHash, ex.getMessage(), ex);

                    }

                    try {

                        committerIdent = commit.getCommitterIdent();

                    } catch (RuntimeException ex) {

                        log.warn("Error while trying to identify the committer for the git commit '{}'. " +
                                "Skipping committer email and name entry. Error is '{}'", gitHash, ex.getMessage(), ex);

                    }

                    String commitUri = getGithubCommitUri(owner, repositoryName, gitHash);
                    //String commitUri = GIT_NAMESPACE + ":GitCommit";

                    if (log.isDebugEnabled()) log.debug("Set rdf type property commitUri");

                    writer.triple(RdfCommitUtils.createRdfTypeProperty(commitUri));

                    if (log.isDebugEnabled()) log.debug("Set rdf commit hash");

                    if (gitCommitRepositoryFilter.isEnableCommitHash()) {
                        writer.triple(RdfCommitUtils.createCommitHashProperty(commitUri, gitHash));
                    }

                    if (log.isDebugEnabled()) log.debug("Set rdf author email");

                    if (gitCommitRepositoryFilter.isEnableAuthorEmail()) {
                        calculateAuthorEmail( // Also brings github identifier into rdf
                                authorIdent, uniqueGitCommiterWithHash, writer, commitUri, gitHash, githubRepositoryHandle);
                    }

                    if (log.isDebugEnabled()) log.debug("Set RDF author name");

                    if (gitCommitRepositoryFilter.isEnableAuthorName()) {
                        calculateAuthorName(writer, commitUri, authorIdent);
                    }


                    boolean isAuthorDateEnabled = gitCommitRepositoryFilter.isEnableAuthorDate();
                    boolean isCommitDateEnabled = gitCommitRepositoryFilter.isEnableCommitDate();

                    if (isAuthorDateEnabled || isCommitDateEnabled) {

                        LocalDateTime commitDateTime = localDateTimeFrom(commit.getCommitTime());

                        if (log.isDebugEnabled()) log.debug("Set RDF author date");

                        if (isAuthorDateEnabled) {
                            writer.triple(RdfCommitUtils.createAuthorDateProperty(commitUri, commitDateTime));
                        }

                        if (log.isDebugEnabled()) log.debug("Set RDF commit date");

                        if (isCommitDateEnabled) {
                            writer.triple(RdfCommitUtils.createCommitDateProperty(commitUri, commitDateTime));
                        }
                    }

                    if (log.isDebugEnabled()) log.debug("Set RDF committer name");

                    if (gitCommitRepositoryFilter.isEnableCommitterName()) {
                        calculateCommitterName(writer, commitUri, committerIdent);
                    }

                    if (log.isDebugEnabled()) log.debug("Set RDF committer email");

                    if (gitCommitRepositoryFilter.isEnableCommitterEmail()) {
                        calculateCommitterEmail(writer, commitUri, committerIdent);
                    }

                    if (log.isDebugEnabled()) log.debug("Set RDF commit message for commit with hash '{}'", gitHash);

                    if (gitCommitRepositoryFilter.isEnableCommitMessage()) {
                        calculateCommitMessage(writer, commitUri, commit);
                    }

                    // No possible solution found yet for merge commits -> maybe traverse to parent? Maybe both
                    //Property mergeCommitProperty = rdfModel.createProperty(gitUri + "MergeCommit");
                    //commit.getParent()

                    // Branch
                    // TODO: better way to handle merges? (so commit could have multiple branches)
                    if(gitCommitRepositoryFilter.isEnableCommitBranch()) {
                        calculateCommitBranch(commitBranchCalculator, writer, commit, commitUri);
                    }

                    // Tag

                    //if(gitCommitRepositoryFilter.isEnableCommitTag()) {
                    //    if (tagNames.containsKey(commitId)) {
                    //        String tagName = tagNames.get(commitId);
                    //        writer.triple(RdfCommitUtils.createCommitTagProperty(commitUri, tagName));
                    //    }
                    //}

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

                    if (log.isDebugEnabled()) log.debug("Check commit diff");

                    if(gitCommitRepositoryFilter.isEnableCommitDiff()) {
                        calculateCommitDiff(commit, reader, diffFormatter, writer, commitUri);
                    }
                }

                if (log.isDebugEnabled()) log.debug("Ending commit writer rdf - loop finished");

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

            log.info("Git commit iterations finished");

            commitConversionWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Git-Commit conversion time in milliseconds is: '{}'", commitConversionWatch.getTime());
            timeLog.setGitCommitConversionTime(commitConversionWatch.getTime());

            lockHandler.renewLockOnRenewTimeFulfillment();

            // Submodules


            // branch-snapshot
            // TODO: rename to 'blame'?


            if (gitCommitRepositoryFilter.isEnableBranchSnapshot() ) {

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

            if (githubIssueRepositoryFilter.doesContainAtLeastOneEnabledFilterOption()) {

                // request new github handle, so that we prevent installation token expiration during the process
                //GithubHandle githubIssueHandle = githubHandlerService.getGithubHandle();
                //GHRepository githubRepositoryIssueHandle = githubIssueHandle.getGitHubHandle().getRepository(githubRepositoryName);

                if (githubRepositoryHandle.hasIssues()) {

                    //githubRepositoryHandle.queryIssues().state(GHIssueState.ALL).pageSize(100).list()

                    log.info("Start issue processing");

                    int issueCounter = 0;

                    boolean doesWriterContainNonWrittenRdfStreamElements = false;

                    for (GHIssue ghIssue : githubRepositoryHandle.queryIssues().state(GHIssueState.ALL).pageSize(100).list()) {
                        if (issueCounter >= 100) {
                            continue;}
                        if (issueCounter < 1) {
                            log.info("Start issue rdf conversion batch");
                            writer.start();
                            doesWriterContainNonWrittenRdfStreamElements = true;
                        }

                        long issueId = ghIssue.getId(); // ID seems to be an internal ID, not the actual issue number
                        int issueNumber = ghIssue.getNumber(); // number is used in shortcuts like #123 or the URL

                        //String githubIssueUri = getGithubIssueUri(owner, repositoryName, issueId);
                        String githubIssueUri = ghIssue.getHtmlUrl().toString();
                        //String githubIssueUri = PLATFORM_GITHUB_NAMESPACE + ":GithubIssue";

                        if (ghIssue.isPullRequest()) {
                            boolean reviewersEnabled = false;
                            boolean mergedByEnabled = false;
                            try {
                                reviewersEnabled = githubIssueRepositoryFilter.isEnableIssueReviewers();
                                mergedByEnabled = githubIssueRepositoryFilter.isEnableIssueMergedBy();
                            } catch (Exception ignore) {
                                // optional filter flags may not be present
                            }

                            if (reviewersEnabled || mergedByEnabled) {
                                GHPullRequest pr = githubRepositoryHandle.getPullRequest(issueNumber);

                                if (reviewersEnabled) {
                                    writeReviewersAsTripleToIssue(writer, githubIssueUri, pr.getRequestedReviewers());
                                }

                                if (mergedByEnabled && pr.getMergedBy() != null) {
                                    writer.triple(RdfGithubIssueUtils.createIssueMergedByProperty(
                                            githubIssueUri,
                                            pr.getMergedBy().getHtmlUrl().toString()));
                                }
                            }
                        }

                        writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(githubIssueUri));
                        writer.triple(RdfGithubIssueUtils.createIssueRepositoryProperty(
                                githubIssueUri,
                                getGithubRepositoryUri(owner, repositoryName)));

                        if (githubIssueRepositoryFilter.isEnableIssueId()) {
                            writer.triple(RdfGithubIssueUtils.createIssueIdProperty(githubIssueUri, issueId));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueNumber()) {
                            writer.triple(RdfGithubIssueUtils.createIssueNumberProperty(githubIssueUri, issueNumber));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueTitle() && ghIssue.getTitle() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueTitleProperty(githubIssueUri, ghIssue.getTitle()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueBody() && ghIssue.getBody() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueBodyProperty(githubIssueUri, ghIssue.getBody()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueState() && ghIssue.getState() != null) {
                            writer.triple(RdfGithubIssueUtils.createIssueStateProperty(githubIssueUri, ghIssue.getState().toString()));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUser() && ghIssue.getUser() != null) {

                            //String githubIssueUserUri = getGithubUserUri(ghIssue.getUser().getLogin());
                            String githubIssueUserUri = ghIssue.getUser().getHtmlUrl().toString();
                            writer.triple(RdfGithubIssueUtils.createIssueUserProperty(githubIssueUri, githubIssueUserUri));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueLabels()) {
                            writeLabelCollectionAsTriplesToIssue(writer, githubIssueUri, ghIssue.getLabels());
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueAssignees()) {
                            writeAssigneesAsTripleToIssue(writer, githubIssueUri, ghIssue.getAssignees());
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueMilestone()) {

                            GHMilestone issueMilestone = ghIssue.getMilestone();
                            if (issueMilestone != null) {
                                writer.triple(RdfGithubIssueUtils.createIssueMilestoneProperty(githubIssueUri, ghIssue.getMilestone().getHtmlUrl().toString()));
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueCreatedAt() && ghIssue.getCreatedAt() != null) {

                            LocalDateTime createdAt = localDateTimeFrom(ghIssue.getCreatedAt());
                            writer.triple(RdfGithubIssueUtils.createIssueCreatedAtProperty(githubIssueUri, createdAt));
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueUpdatedAt()) {

                            Date updatedAtUtilDate = ghIssue.getUpdatedAt();
                            if (updatedAtUtilDate != null) {
                                LocalDateTime updatedAt = localDateTimeFrom(updatedAtUtilDate);
                                writer.triple(RdfGithubIssueUtils.createIssueUpdatedAtProperty(githubIssueUri, updatedAt));
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueClosedAt()) {

                            Date closedAtUtilDate = ghIssue.getClosedAt();
                            if (closedAtUtilDate != null) {
                                LocalDateTime closedAt = localDateTimeFrom(closedAtUtilDate);
                                writer.triple(RdfGithubIssueUtils.createIssueClosedAtProperty(githubIssueUri, closedAt));
                            }
                        }

                        if (githubIssueRepositoryFilter.isEnableIssueComments()) {
                            writeCommentsAsTriplesToIssue(writer, githubIssueUri, ghIssue.listComments());
                        }

                        issueCounter++;

                        if (issueCounter > 100) {
                            log.info("Finish issue rdf conversion batch");
                            writer.finish();
                            doesWriterContainNonWrittenRdfStreamElements = false;
                            // Limit the Issue processing to 100 issues 
                            //issueCounter = 0;
                            lockHandler.renewLockOnRenewTimeFulfillment();
                        } else {
                            log.info("Processed issue #{} with id {} and uri '{}'", issueCounter, issueId, githubIssueUri);
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

            //log.info("TIME MEASUREMENT DONE: Github-Issue conversion time in milliseconds is: '{}'", issueWatch.getTime());
            timeLog.setGithubIssueConversionTime(issueWatch.getTime());

        }

        log.info("Finished overall processing. Start to load rdf file into postgres blob storage");

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(rdfTempFile));

        entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, rdfTempFile.length()));

        return bufferedInputStream;
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

        if (log.isDebugEnabled()) log.debug("Set rdf github user in commit");

        if (uniqueGitCommiterWithHash.containsKey(email)) {

            if (log.isDebugEnabled()) log.debug("Found github committer email in hash");

            RdfGitCommitUserUtils commitInfo = uniqueGitCommiterWithHash.get(email);
            if (commitInfo.gitHubUser != null && !commitInfo.gitHubUser.isEmpty()) {

                if (log.isDebugEnabled()) log.debug("Set RDF committer github user property after finding github committer email in hash");

                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, commitInfo.gitHubUser));
            }
        } else {

            if (log.isDebugEnabled()) log.debug("Did not find github committer email in hash");

            String gitHubUser = RdfGitCommitUserUtils.getGitHubUserFromCommit(githubRepositoryHandle, gitHash);
            uniqueGitCommiterWithHash.put(email, new RdfGitCommitUserUtils(gitHash, gitHubUser));
            if (gitHubUser != null && !gitHubUser.isEmpty()) {

                if (log.isDebugEnabled()) log.debug("Set RDF committer github user property after not finding it in github committer email in hash");

                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, gitHubUser));
            }
        }

        if (log.isDebugEnabled()) log.debug("Set RDF author email property");

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

        if (log.isDebugEnabled()) log.debug("Commit diff is enabled - parent count is '{}'", parentCommitCount);

        if (parentCommitCount > 0) {

            RevCommit parentCommit = commit.getParent(0);

            if (log.isDebugEnabled()) log.debug("Check if parent commit is null");

            if (parentCommit != null) {
                CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
                CanonicalTreeParser currentTreeParser = new CanonicalTreeParser();

                if (log.isDebugEnabled()) log.debug("Reset tree parsers - starting with parent tree parser");
                parentTreeParser.reset(currentRepositoryObjectReader, parentCommit.getTree());

                if (log.isDebugEnabled()) log.debug("Reset tree parsers - continuing with current tree parser");
                currentTreeParser.reset(currentRepositoryObjectReader, commit.getTree());

                //Resource commitResource = ResourceFactory.createResource(gitHash); // TODO: use proper uri?
                //Node commitNode = commitResource.asNode();
                //writer.triple(RdfCommitUtils.createCommitResource(commitUri, commitNode));

                if (log.isDebugEnabled()) log.debug("Scan diff entries");

                List<DiffEntry> diffEntries = currentRepositoryDiffFormatter.scan(parentTreeParser, currentTreeParser);

                if (log.isDebugEnabled()) log.debug("Loop through diff entries. Diff entry list size is '{}'", diffEntries.size());

                for (DiffEntry diffEntry : diffEntries) {
                    Resource diffEntryResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":entry"*/);
                    Node diffEntryNode = diffEntryResource.asNode();
                    //writer.triple(RdfCommitUtils.createCommitDiffEntryResource(commitNode, diffEntryNode));

                    if (log.isDebugEnabled()) log.debug("Set RDF commit diff entry property");

                    writer.triple(RdfCommitUtils.createCommitDiffEntryProperty(commitUri, diffEntryNode));

                    DiffEntry.ChangeType changeType = diffEntry.getChangeType(); // ADD,DELETE,MODIFY,RENAME,COPY

                    if (log.isDebugEnabled()) log.debug("Set RDF commit diff entry edit type property");

                    writer.triple(RdfCommitUtils.createCommitDiffEntryEditTypeProperty(diffEntryNode, changeType));

                    FileHeader fileHeader = currentRepositoryDiffFormatter.toFileHeader(diffEntry);

                    if (log.isDebugEnabled()) log.debug("Switch through diff entry change type");

                    // See: org.eclipse.jgit.diff.DiffEntry.ChangeType.toString()
                    switch (changeType) {
                        case ADD:
                            if (log.isDebugEnabled()) log.debug("Set RDF ADD commit diff entry new file name property");
                            writer.triple(RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        case COPY:
                        case RENAME:
                            if (log.isDebugEnabled()) log.debug("Set RDF COPY/RENAME commit diff entry new file name property");
                            writer.triple(RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                            writer.triple(RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        case DELETE:
                        case MODIFY:
                            if (log.isDebugEnabled()) log.debug("Set RDF DELETE/MODIFY commit diff entry new file name property");
                            writer.triple(RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected changeType: " + changeType);
                    }

                    // Diff Lines (added/changed/removed)

                    if (log.isDebugEnabled()) log.debug("Retrieve file header edit list");

                    EditList editList = fileHeader.toEditList();

                    if (log.isDebugEnabled()) log.debug("Loop trough edit list. There are '{}' edit list entries", editList.size());

                    for (Edit edit : editList) {
                        Resource editResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":edit"*/);
                        Node editNode = editResource.asNode();

                        if (log.isDebugEnabled()) log.debug("Set RDF commit diff edit resource");

                        writer.triple(RdfCommitUtils.createCommitDiffEditResource(diffEntryNode, editNode));

                        Edit.Type editType = edit.getType(); // INSERT,DELETE,REPLACE

                        if (log.isDebugEnabled()) log.debug("Set RDF commit diff edit type property");

                        writer.triple(RdfCommitUtils.createCommitDiffEditTypeProperty(editNode, editType));

                        if (log.isDebugEnabled()) log.debug("Retrieve for file diffs the old and new line number beginnings and endings");

                        final int oldLinenumberBegin = edit.getBeginA();
                        final int newLinenumberBegin = edit.getBeginB();
                        final int oldLinenumberEnd = edit.getEndA();
                        final int newLinenumberEnd = edit.getEndB();

                        if (log.isDebugEnabled()) log.debug("Set RDF edit old line number begin property");
                        writer.triple(RdfCommitUtils.createEditOldLinenumberBeginProperty(editNode, oldLinenumberBegin));

                        if (log.isDebugEnabled()) log.debug("Set RDF edit new line number begin property");
                        writer.triple(RdfCommitUtils.createEditNewLinenumberBeginProperty(editNode, newLinenumberBegin));

                        if (log.isDebugEnabled()) log.debug("Set RDF edit old line number end property");
                        writer.triple(RdfCommitUtils.createEditOldLinenumberEndProperty(editNode, oldLinenumberEnd));

                        if (log.isDebugEnabled()) log.debug("Set RDF edit new line number end property");
                        writer.triple(RdfCommitUtils.createEditNewLinenumberEndProperty(editNode, newLinenumberEnd));
                    }
                }
            }
        }

    }

    private void writeLabelCollectionAsTriplesToIssue(
            StreamRDF writer,
            String issueUri,
            Collection<GHLabel> labels) {

        for (GHLabel label : labels) {
            writer.triple(RdfGithubIssueUtils.createIssueLabelProperty(issueUri, label.getUrl()));
        }

    }

    private void writeAssigneesAsTripleToIssue(
            StreamRDF writer,
            String issueUri,
            List<GHUser> assignees) {

        for (GHUser assignee : assignees) {
            writer.triple(RdfGithubIssueUtils.createIssueAssigneeProperty(issueUri, assignee.getHtmlUrl().toString()));
        }

    }

    private void writeReviewersAsTripleToIssue(
            StreamRDF writer,
            String issueUri,
            Collection<GHUser> reviewers) {

        for (GHUser reviewer : reviewers) {
            writer.triple(RdfGithubIssueUtils.createIssueReviewerProperty(issueUri, reviewer.getHtmlUrl().toString()));
        }

    }

    private void writeCommentsAsTriplesToIssue(StreamRDF writer,
            String issueUri,
            PagedIterable<GHIssueComment> comments)
            throws IOException { // <-- propagate
        for (GHIssueComment comment : comments) {
            String commentUri = comment.getHtmlUrl().toString();
            writer.triple(RdfGithubIssueUtils.createIssueCommentProperty(issueUri, commentUri));
            writer.triple(RdfGithubIssueUtils.createCommentRdfTypeProperty(commentUri));
            writer.triple(RdfGithubIssueUtils.createIssueCommentOfProperty(commentUri, issueUri));

            writer.triple(RdfGithubIssueUtils.createIssueCommentIdProperty(commentUri, comment.getId()));

            GHUser user = comment.getUser();
            if (user != null) {
                writer.triple(RdfGithubIssueUtils.createIssueCommentUserProperty(
                        commentUri, user.getHtmlUrl().toString()));
            }

            String body = comment.getBody();
            if (body != null) {
                writer.triple(RdfGithubIssueUtils.createIssueCommentBodyProperty(commentUri, body));
            }

            if (comment.getCreatedAt() != null) {
                LocalDateTime created = localDateTimeFrom(comment.getCreatedAt());
                writer.triple(RdfGithubIssueUtils.createIssueCommentCreatedAtProperty(commentUri, created));
            }

            if (comment.getUpdatedAt() != null) {
                LocalDateTime updated = localDateTimeFrom(comment.getUpdatedAt());
                writer.triple(RdfGithubIssueUtils.createIssueCommentUpdatedAtProperty(commentUri, updated));
            }
        }
    }

    private int calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(
            int currentIteration, int commitsPerIteration) {

        long longCurrentIteration = (long) currentIteration;
        long longCommitsPerIteration = (long) commitsPerIteration;

        long skips = longCurrentIteration * longCommitsPerIteration;

        if (skips > Integer.MAX_VALUE) {
            throw new RuntimeException("While iterating through commit log and transforming log to rdf: Exceeded skip max count (integer overflow)");
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

    private String getGithubIssueUri(String owner, String repository, long issueId) {
        return getGithubIssueBaseUri(owner, repository) + issueId;
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

}
