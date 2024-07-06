package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.database.common.entity.GitCommitRepositoryFilter;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubIssueRepositoryFilter;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.database.common.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.database.common.entity.lob.GithubRepositoryOrderEntityLobs;
import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.timemeasurement.TimeLog;
import de.leipzig.htwk.gitrdf.worker.utils.GitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.ZipUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGitCommitUserUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfGithubIssueUtils;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObjectList;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.hibernate.engine.jdbc.BlobProxy;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.hibernate.id.SequenceMismatchStrategy.LOG;

@Service
@Slf4j
public class GithubRdfConversionTransactionService {

    private static final int TWENTY_FIVE_MEGABYTE = 1024 * 1024 * 25;

    // TODO: replace PURL

    public static final String GIT_NAMESPACE = "git";
    public static final String GIT_URI = "https://purl.archive.org/git2rdflab-test/v1/git2RDFLab-git#";


    public static final String PLATFORM_NAMESPACE = "platform";
    public static final String PLATFORM_URI = "https://purl.archive.org/git2rdflab-test/v1/git2RDFLab-platform#";


    public static final String PLATFORM_GITHUB_URI = "https://purl.archive.org/git2rdflab-test/v1/git2RDFLab-platform-github#";
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
            long id, File rdfTempFile) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        GitHub gitHubHandle = githubHandlerService.getGithubHandle();

        // du kriegst die .git nicht als Teil der Zip ->
        //gitHubHandle.getRepository("").listCommits()

        GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs
                = entityManager.find(GithubRepositoryOrderEntityLobs.class, id);

        GithubRepositoryOrderEntity githubRepositoryOrderEntity
                = entityManager.find(GithubRepositoryOrderEntity.class, id);

        String owner = githubRepositoryOrderEntity.getOwnerName();
        String repo = githubRepositoryOrderEntity.getRepositoryName();

        GHRepository targetRepo = getGithubRepositoryHandle(owner, repo, gitHubHandle);

        File gitFile = getDotGitFileFromGithubRepositoryHandle(targetRepo, id, owner, repo);

        InputStream needsToBeClosedOutsideOfTransaction
                = writeRdf(gitFile, githubRepositoryOrderEntity, githubRepositoryOrderEntityLobs, rdfTempFile, new TimeLog());

        githubRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

        return needsToBeClosedOutsideOfTransaction;
    }

    @Transactional(rollbackFor = {IOException.class, GitAPIException.class, URISyntaxException.class, InterruptedException.class}) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGithubRepoToRdfConversionWithGitCloningLogicAndReturnCloseableInputStream(
            long id, File gitWorkingDirectory, File rdfTempFile, TimeLog timeLog) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        Git gitHandler = null;

        try {

            GithubRepositoryOrderEntityLobs githubRepositoryOrderEntityLobs
                    = entityManager.find(GithubRepositoryOrderEntityLobs.class, id);

            GithubRepositoryOrderEntity githubRepositoryOrderEntity
                    = entityManager.find(GithubRepositoryOrderEntity.class, id);

            String owner = githubRepositoryOrderEntity.getOwnerName();
            String repo = githubRepositoryOrderEntity.getRepositoryName();

            StopWatch downloadWatch = new StopWatch();

            downloadWatch.start();

            gitHandler = performGitClone(owner, repo, gitWorkingDirectory);

            downloadWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Download time in milliseconds is: '{}'", downloadWatch.getTime());
            timeLog.setDownloadTime(downloadWatch.getTime());

            StopWatch conversionWatch = new StopWatch();

            conversionWatch.start();

            InputStream needsToBeClosedOutsideOfTransaction
                    = writeRdf(gitHandler, githubRepositoryOrderEntity, githubRepositoryOrderEntityLobs, rdfTempFile, timeLog);

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
            TimeLog timeLog) throws GitAPIException, IOException, URISyntaxException, InterruptedException {

        Repository gitRepository = new FileRepositoryBuilder().setGitDir(gitFile).build();
        Git gitHandler = new Git(gitRepository);

        return writeRdf(gitHandler, entity, entityLobs, rdfTempFile, timeLog);
    }

    private InputStream writeRdf(
            Git gitHandler,
            GithubRepositoryOrderEntity entity,
            GithubRepositoryOrderEntityLobs entityLobs,
            File rdfTempFile,
            TimeLog timeLog) throws GitAPIException, IOException, URISyntaxException, InterruptedException {

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

            GitHub githubHandle = githubHandlerService.getGithubHandle();

            String githubRepositoryName = String.format("%s/%s", owner, repositoryName);

            GHRepository githubRepositoryHandle = githubHandle.getRepository(githubRepositoryName);
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

            log.info("Start conversion run of '{}' repository", repositoryName);

            StopWatch commitConversionWatch = new StopWatch();

            commitConversionWatch.start();

            // git commits
            for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                log.info("Start iterations of git commits. Current iteration count: {}", iteration);

                int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration, commitsPerIteration);

                log.info("Calculated skip count for this iteration is: {}", skipCount);

                Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration).call();

                boolean finished = true;

                writer.start();

                for (RevCommit commit : commits) {

                    finished = false;

                    ObjectId commitId = commit.getId();
                    String gitHash = commitId.name();

                    String commitUri = getGithubCommitUri(owner, repositoryName, gitHash);
                    //String commitUri = GIT_NAMESPACE + ":GitCommit";

                    writer.triple(RdfCommitUtils.createRdfTypeProperty(commitUri));

                    if (gitCommitRepositoryFilter.isEnableCommitHash()) {
                        writer.triple(RdfCommitUtils.createCommitHashProperty(commitUri, gitHash));
                    }

                    if (gitCommitRepositoryFilter.isEnableAuthorEmail()) {
                        String email = commit.getAuthorIdent().getEmailAddress();

                        if (uniqueGitCommiterWithHash.containsKey(email)) {
                            RdfGitCommitUserUtils commitInfo = uniqueGitCommiterWithHash.get(email);
                            if (commitInfo.gitHubUser != null && !commitInfo.gitHubUser.isEmpty()) {
                                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, commitInfo.gitHubUser));
                            }
                        } else {
                            String gitHubUser = RdfGitCommitUserUtils.getGitHubUserFromCommit(githubRepositoryHandle, gitHash);
                            uniqueGitCommiterWithHash.put(email, new RdfGitCommitUserUtils(gitHash, gitHubUser));
                            if (gitHubUser != null && !gitHubUser.isEmpty()) {
                                writer.triple(RdfCommitUtils.createCommiterGitHubUserProperty(commitUri, gitHubUser));
                            }
                        }
                        writer.triple(RdfCommitUtils.createAuthorEmailProperty(commitUri, email));
                    }
                    if (gitCommitRepositoryFilter.isEnableAuthorName()) {
                        writer.triple(RdfCommitUtils.createAuthorNameProperty(commitUri, commit.getAuthorIdent().getName()));
                    }


                    boolean isAuthorDateEnabled = gitCommitRepositoryFilter.isEnableAuthorDate();
                    boolean isCommitDateEnabled = gitCommitRepositoryFilter.isEnableCommitDate();

                    if (isAuthorDateEnabled || isCommitDateEnabled) {

                        LocalDateTime commitDateTime = localDateTimeFrom(commit.getCommitTime());

                        if (isAuthorDateEnabled) {
                            writer.triple(RdfCommitUtils.createAuthorDateProperty(commitUri, commitDateTime));
                        }

                        if (isCommitDateEnabled) {
                            writer.triple(RdfCommitUtils.createCommitDateProperty(commitUri, commitDateTime));
                        }
                    }

                    if (gitCommitRepositoryFilter.isEnableCommitterName()) {
                        writer.triple(RdfCommitUtils.createCommitterNameProperty(commitUri, commit.getCommitterIdent().getName()));
                    }

                    if (gitCommitRepositoryFilter.isEnableCommitterEmail()) {
                        writer.triple(RdfCommitUtils.createCommitterEmailProperty(commitUri, commit.getCommitterIdent().getEmailAddress()));
                    }

                    if (gitCommitRepositoryFilter.isEnableCommitMessage()) {
                        writer.triple(RdfCommitUtils.createCommitMessageProperty(commitUri, commit.getFullMessage()));
                    }

                    // No possible solution found yet for merge commits -> maybe traverse to parent? Maybe both
                    //Property mergeCommitProperty = rdfModel.createProperty(gitUri + "MergeCommit");
                    //commit.getParent()

                    // Branch
                    // TODO: better way to handle merges? (so commit could have multiple branches)
                    if(gitCommitRepositoryFilter.isEnableCommitBranch()) {

                        for (Ref branchRef : branches) {

                            RevCommit commitRev = revWalk.lookupCommit(commit.getId());
                            RevCommit branchRev = revWalk.lookupCommit(branchRef.getObjectId());

                            if (revWalk.isMergedInto(commitRev, branchRev)) {
                                writer.triple(RdfCommitUtils.createCommitBranchNameProperty(commitUri, branchRef.getName()));
                            }
                        }
                    }

                    // Commit Diffs
                    // See: https://www.codeaffine.com/2016/06/16/jgit-diff/
                    // TODO: check if merges with more than 1 parent exist?

                    if(gitCommitRepositoryFilter.isEnableCommitDiff()) {

                        int parentCommitCount = commit.getParentCount();

                        if (parentCommitCount > 0) {

                            RevCommit parentCommit = commit.getParent(0);

                            if (parentCommit != null) {
                                CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
                                CanonicalTreeParser currentTreeParser = new CanonicalTreeParser();

                                parentTreeParser.reset(reader, parentCommit.getTree());
                                currentTreeParser.reset(reader, commit.getTree());

                                //Resource commitResource = ResourceFactory.createResource(gitHash); // TODO: use proper uri?
                                //Node commitNode = commitResource.asNode();
                                //writer.triple(RdfCommitUtils.createCommitResource(commitUri, commitNode));

                                List<DiffEntry> diffEntries = diffFormatter.scan(parentTreeParser, currentTreeParser);

                                for (DiffEntry diffEntry : diffEntries) {
                                    Resource diffEntryResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":entry"*/);
                                    Node diffEntryNode = diffEntryResource.asNode();
                                    //writer.triple(RdfCommitUtils.createCommitDiffEntryResource(commitNode, diffEntryNode));
                                    writer.triple(RdfCommitUtils.createCommitDiffEntryProperty(commitUri, diffEntryNode));

                                    DiffEntry.ChangeType changeType = diffEntry.getChangeType(); // ADD,DELETE,MODIFY,RENAME,COPY
                                    writer.triple(RdfCommitUtils.createCommitDiffEntryEditTypeProperty(diffEntryNode, changeType));

                                    FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);

                                    // See: org.eclipse.jgit.diff.DiffEntry.ChangeType.toString()
                                    switch (changeType) {
                                        case ADD:
                                            writer.triple(RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                                            break;
                                        case COPY:
                                        case RENAME:
                                            writer.triple(RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                                            writer.triple(RdfCommitUtils.createCommitDiffEntryNewFileNameProperty(diffEntryNode, fileHeader));
                                            break;
                                        case DELETE:
                                        case MODIFY:
                                            writer.triple(RdfCommitUtils.createCommitDiffEntryOldFileNameProperty(diffEntryNode, fileHeader));
                                            break;
                                        default:
                                            throw new IllegalStateException("Unexpected changeType: " + changeType);
                                    }

                                    // Diff Lines (added/changed/removed)
                                    EditList editList = fileHeader.toEditList();

                                    for (Edit edit : editList) {
                                        Resource editResource = ResourceFactory.createResource(/*GIT_NAMESPACE + ":edit"*/);
                                        Node editNode = editResource.asNode();

                                        writer.triple(RdfCommitUtils.createCommitDiffEditResource(diffEntryNode, editNode));

                                        Edit.Type editType = edit.getType(); // INSERT,DELETE,REPLACE

                                        writer.triple(RdfCommitUtils.createCommitDiffEditTypeProperty(editNode, editType));

                                        final int oldLinenumberBegin = edit.getBeginA();
                                        final int newLinenumberBegin = edit.getBeginB();
                                        final int oldLinenumberEnd = edit.getEndA();
                                        final int newLinenumberEnd = edit.getEndB();

                                        writer.triple(RdfCommitUtils.createEditOldLinenumberBeginProperty(editNode, oldLinenumberBegin));
                                        writer.triple(RdfCommitUtils.createEditNewLinenumberBeginProperty(editNode, newLinenumberBegin));
                                        writer.triple(RdfCommitUtils.createEditOldLinenumberEndProperty(editNode, oldLinenumberEnd));
                                        writer.triple(RdfCommitUtils.createEditNewLinenumberEndProperty(editNode, newLinenumberEnd));
                                    }
                                }
                            }
                        }
                    }
                }

                writer.finish();

                if (finished) {
                    log.info("Git commit iterations finished");
                    break;
                }

                if (iteration + 1 == Integer.MAX_VALUE) {
                    throw new RuntimeException(
                            "While iterating through commit log and transforming log to rdf: Exceeded iteration max count (integer overflow)");
                }
            }

            commitConversionWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Git-Commit conversion time in milliseconds is: '{}'", commitConversionWatch.getTime());
            timeLog.setGitCommitConversionTime(commitConversionWatch.getTime());

            // branch-snapshot
            // TODO: rename to 'blame'?

            if (gitCommitRepositoryFilter.isEnableBranchSnapshot() ) {

                log.info("Start branch snapshotting");

                writer.start();

                ObjectId headCommitId = gitRepository.resolve("HEAD");
                String commitUri = getGithubCommitUri(owner, repositoryName, headCommitId.getName());

                String branchSnapshotUri = commitUri;
                //String branchSnapshotUri = GIT_NS + ":blame";
                //String branchSnapshotUri = "";

                Resource branchSnapshotResource = ResourceFactory.createResource(branchSnapshotUri);
                Node branchSnapshotNode = branchSnapshotResource.asNode();

                writer.triple(RdfCommitUtils.createBranchSnapshotProperty(branchSnapshotNode));
                writer.triple(RdfCommitUtils.createBranchSnapshotDateProperty(branchSnapshotNode, LocalDateTime.now()));

                writer.finish();

                List<String> fileNames = listRepositoryContents(gitRepository);

                log.info("Listed {} files for branch snapshotting", fileNames.size());

                int branchSnapshotCounter = 0;

                for (int ii = 0; ii < fileNames.size(); ii++) {

                    if (branchSnapshotCounter < 1) {
                        log.info("Branch Snapshotting iteration {} started", ii);
                        writer.start();
                    }

                    branchSnapshotCounter++;

                    String fileName = fileNames.get(ii);

                    Resource branchSnapshotFileResource = ResourceFactory.createResource();
                    Node branchSnapshotFileNode = branchSnapshotFileResource.asNode();

                    writer.triple(RdfCommitUtils.createBranchSnapshotFileEntryProperty(branchSnapshotNode, branchSnapshotFileNode));
                    writer.triple(RdfCommitUtils.createBranchSnapshotFilenameProperty(branchSnapshotFileNode, fileName));

                    BlameCommand blameCommand = new BlameCommand(gitRepository);
                    blameCommand.setFilePath(fileName);
                    BlameResult blameResult;

                    try {
                        blameResult = blameCommand
                                .setTextComparator(RawTextComparator.WS_IGNORE_ALL) // Equivalent to -w command line option
                                .setFilePath(fileName).call();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to blame file " + fileName, e);
                    }

                    if (blameResult == null) {
                        continue;
                    }

                    HashMap<String,List<Integer>> commitLineMap = new HashMap<>();

                    String prevCommitHash = null;
                    int linenumberBegin = 1;

                    RawText blameText = blameResult.getResultContents();
                    int lineCount = blameText.size();

                    for (int lineIdx = 0; lineIdx < lineCount; lineIdx++) {

                        RevCommit commit = blameResult.getSourceCommit(lineIdx);

                        if (commit == null) {
                            continue;
                        }

                        String currentCommitHash = commit.getId().name();

                        if (prevCommitHash == null) {
                            prevCommitHash = currentCommitHash;
                        }

                        boolean isAtEnd = lineIdx == lineCount - 1;
                        boolean isNewCommit = !currentCommitHash.equals(prevCommitHash);

                        if (isNewCommit || isAtEnd) {

                            Resource branchSnapshotLineEntryResource = ResourceFactory.createResource();
                            Node branchSnapshotLineEntryNode = branchSnapshotLineEntryResource.asNode();

                            int lineNumberEnd = lineIdx;

                            if (isAtEnd) {

                                lineNumberEnd += 1;

                                writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                                writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, prevCommitHash));
                                writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, linenumberBegin));
                                writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));

                                if (isNewCommit) {

                                    writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                                    writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, currentCommitHash));
                                    writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, lineNumberEnd));
                                    writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));
                                }
                            }

                            if (isNewCommit) {

                                writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                                writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, prevCommitHash));
                                writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, linenumberBegin));
                                writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));

                                prevCommitHash = currentCommitHash;
                            }

                            linenumberBegin = lineIdx + 1;
                        }
                    }

                    if (branchSnapshotCounter > 99) {
                        writer.finish();
                        branchSnapshotCounter = 0;
                    }

                }

                if (branchSnapshotCounter > 0) {
                    writer.finish();
                }

            }

            // issues

            StopWatch issueWatch = new StopWatch();

            issueWatch.start();

            if (githubIssueRepositoryFilter.doesContainAtLeastOneEnabledFilterOption()) {

                if (githubRepositoryHandle.hasIssues()) {

                    //githubRepositoryHandle.queryIssues().state(GHIssueState.ALL).pageSize(100).list()

                    log.info("Start issue processing");

                    int issueCounter = 0;

                    boolean doesWriterContainNonWrittenRdfStreamElements = false;

                    for (GHIssue ghIssue : githubRepositoryHandle.queryIssues().state(GHIssueState.ALL).pageSize(100).list()) {

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

                        writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(githubIssueUri));

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

                        issueCounter++;

                        if (issueCounter > 99) {
                            log.info("Finish issue rdf conversion batch");
                            writer.finish();
                            doesWriterContainNonWrittenRdfStreamElements = false;
                            issueCounter = 0;
                        }
                    }

                    if (doesWriterContainNonWrittenRdfStreamElements) {
                        log.info("Finish last issue rdf conversion batch");
                        writer.finish();
                    }
                }
            }

            issueWatch.stop();

            //log.info("TIME MEASUREMENT DONE: Github-Issue conversion time in milliseconds is: '{}'", issueWatch.getTime());
            timeLog.setGithubIssueConversionTime(issueWatch.getTime());

        }

        log.info("Finished overall processing. Start to load rdf file into postgres blob storage");

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(rdfTempFile));

        entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, rdfTempFile.length()));

        return bufferedInputStream;
    }

    // See: https://stackoverflow.com/q/19941597/11341498
    private List<String> listRepositoryContents(Repository repository) throws IOException {

        Ref head = repository.getRef("HEAD");
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head.getObjectId());
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        List<String> fileNames = new ArrayList<String>();

        while (treeWalk.next()) {
            fileNames.add(treeWalk.getPathString());
        }

        return fileNames;
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
