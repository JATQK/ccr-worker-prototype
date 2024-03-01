package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.worker.database.entity.lob.GitRepositoryOrderEntityLobs;
import de.leipzig.htwk.gitrdf.worker.utils.GitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.ZipUtils;
import jakarta.persistence.EntityManager;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GitRdfConversionTransactionService {

    public static final String GIT_URI = "git://";
    public static final String GIT_NAMESPACE = "git";
    public static final String GIT_NAMESPACE_PREFIX = GIT_NAMESPACE + ":";

    private final EntityManager entityManager;

    private final int commitsPerIteration;

    public GitRdfConversionTransactionService(
            EntityManager entityManager, @Value("${worker.commits-per-iteration}") int commitsPerIteration) {

        this.entityManager = entityManager;
        this.commitsPerIteration = commitsPerIteration;
    }

    @Transactional(rollbackFor = {SQLException.class, IOException.class, GitAPIException.class}) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGitRepoToRdfConversionAndReturnCloseableInputStream(long id) throws SQLException, IOException, GitAPIException {

        GitRepositoryOrderEntityLobs gitRepositoryOrderEntityLobs
                = entityManager.find(GitRepositoryOrderEntityLobs.class, id);

        GitRepositoryOrderEntity gitRepositoryOrderEntity = entityManager.find(GitRepositoryOrderEntity.class, id);

        File tempGitRepositoryParentFile = ZipUtils.extractZip(gitRepositoryOrderEntityLobs.getGitZipFile());

        File gitFile
                = GitUtils.getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(tempGitRepositoryParentFile);

        // set Ascii stream is not supported -> use ClobProxy instead, wrap outputstream in reader?
        //writeRdf(gitFile, gitRepositoryOrderEntityLobs.getRdfFile().setAsciiStream(POSITION_START));

        InputStream needsToBeClosedOutsideOfTransaction = writeRdf(gitFile, gitRepositoryOrderEntityLobs);

        gitRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

        return needsToBeClosedOutsideOfTransaction;
    }

    //private void writeRdf(File gitFile, OutputStream targetOutputStream) throws GitAPIException, IOException {
    private InputStream writeRdf(File gitFile, GitRepositoryOrderEntityLobs entityLobs) throws GitAPIException, IOException {

        File tempFile = Files.createTempFile("temp-rdf-write-file", ".dat").toFile();

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile))) {

            StreamRDF writer = StreamRDFWriter.getWriterStream(outputStream, RDFFormat.TURTLE_BLOCKS);
            writer.prefix(GIT_NAMESPACE, GIT_URI);

            Repository gitRepository = new FileRepositoryBuilder().setGitDir(gitFile).build();
            ObjectReader reader = gitRepository.newObjectReader();

            Git gitHandler = new Git(gitRepository);

            DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
            diffFormatter.setRepository( gitRepository );

            for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration, commitsPerIteration);

                Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration).call();

                boolean finished = true;

                writer.start();

                for (RevCommit commit : commits) {

                    finished = false;

                    ObjectId commitId = commit.getId();
                    String gitHash = commitId.name();

                    writer.triple(createAuthorNameProperty(gitHash, commit.getAuthorIdent().getName()));
                    writer.triple(createAuthorEmailProperty(gitHash, commit.getAuthorIdent().getEmailAddress()));

                    Instant instant = Instant.ofEpochSecond(commit.getCommitTime());
                    LocalDateTime commitDateTime = instant.atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();

                    writer.triple(createAuthorDateProperty(gitHash, commitDateTime.toString()));
                    writer.triple(createCommitDateProperty(gitHash, commitDateTime.toString()));

                    writer.triple(createCommitterNameProperty(gitHash, commit.getCommitterIdent().getName()));
                    writer.triple(createCommitterEmailProperty(gitHash, commit.getCommitterIdent().getEmailAddress()));
                    writer.triple(createCommitMessageProperty(gitHash, commit.getFullMessage()));

                    // No possible solution found yet for merge commits -> maybe traverse to parent? Maybe both
                    //Property mergeCommitProperty = rdfModel.createProperty(gitUri + "MergeCommit");
                    //commit.getParent()


                    // Commit Diffs
                    // See: https://www.codeaffine.com/2016/06/16/jgit-diff/
                    // TODO: check if merges with more than 1 parent exist?

                    RevCommit parentCommit = commit.getParent(0);

                    if(parentCommit != null)
                    {
                        CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
                        CanonicalTreeParser currentTreeParser = new CanonicalTreeParser();

                        parentTreeParser.reset(reader, parentCommit.getTree());
                        currentTreeParser.reset(reader, commit.getTree());

                        Resource commitResource = ResourceFactory.createResource();
                        Node commitNode = commitResource.asNode();

                        writer.triple(createCommitResource(gitHash, commitNode));

                        List<DiffEntry> diffEntries = diffFormatter.scan(parentTreeParser, currentTreeParser);

                        for(DiffEntry diffEntry : diffEntries)
                        {
                            Resource diffEntryResource = ResourceFactory.createResource();
                            Node diffEntryNode = diffEntryResource.asNode();
                            writer.triple(createCommitDiffEntryResource(commitNode, diffEntryNode));

                            DiffEntry.ChangeType changeType = diffEntry.getChangeType(); // ADD,DELETE,MODIFY
                            writer.triple(createCommitDiffEntryEditTypeProperty(diffEntryNode, changeType));

                            FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);
                            writer.triple(createCommitDiffEntryFileNameProperty(diffEntryNode, fileHeader));

                            // Diff Lines (added/changed/removed)
                            EditList editList = fileHeader.toEditList();

                            for(Edit edit : editList)
                            {
                                Resource editResource = ResourceFactory.createResource();
                                Node editNode = editResource.asNode();

                                writer.triple(createCommitDiffEditResource(diffEntryNode, editNode));

                                Edit.Type editType = edit.getType(); // INSERT,DELETE,REPLACE

                                writer.triple(createCommitDiffEditTypeProperty(editNode, editType));

                                int beginA = edit.getBeginA();
                                int beginB = edit.getBeginB();
                                int endA = edit.getEndA();
                                int endB = edit.getEndB();

                                writer.triple(createCommitDiffEditBeginAProperty(editNode, beginA));
                                writer.triple(createCommitDiffEditBeginBProperty(editNode, beginB));
                                writer.triple(createCommitDiffEditEndAProperty(editNode, endA));
                                writer.triple(createCommitDiffEditEndBProperty(editNode, endB));
                            }
                        }
                    }

                }

                writer.finish();

                if (finished) {
                    break;
                }

                if (iteration + 1 == Integer.MAX_VALUE) {
                    throw new RuntimeException(
                            "While iterating through commit log and transforming log to rdf: Exceeded iteration max count (integer overflow)");
                }

            }

        }

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(tempFile));

        entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, tempFile.length()));

        return bufferedInputStream;

        //try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(tempFile))) {
        //entityLobs.setRdfFile(BlobProxy.generateProxy(bufferedInputStream, tempFile.length()));
        //}

        /*
        // also old, uses clob
        try (Reader reader = new BufferedReader(new FileReader(tempFile))) {
            entityLobs.setRdfFile(ClobProxy.generateProxy(reader, ));
        }

        // OLD
        // lets try writing to a file output stream, which is a tempory file on the hard memory; if bytearrayoutputstream doesnt work
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {


            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);

            try (pipedInputStream) {
                new Thread(() -> {
                    try (pipedOutputStream) {
                        byteArrayOutputStream.writeTo(pipedOutputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                //entityLobs.setRdfFile(BlobProxy.generateProxy(pipedInputStream, byteArrayOutputStream.size()));

                entityLobs.setRdfFile(ClobProxy.generateProxy());

                //entityLobs.setRdfFile(BlobProxy.generateProxy(byteArrayOutputStream.toByteArray()));

            }
        }
         */
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

    private static Node literal(String value) {
        return NodeFactory.createLiteral(value);
    }

    private static Node uri(String value) {
        return NodeFactory.createURI(value);
    }

    private Triple createAuthorNameProperty(String gitHash, String authorNameValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "AuthorName"), literal(authorNameValue));
    }

    private Triple createAuthorEmailProperty(String gitHash, String authorEmailValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "AuthorEmail"), literal(authorEmailValue));
    }

    private Triple createAuthorDateProperty(String gitHash, String authorDateValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "AuthorDate"), literal(authorDateValue));
    }

    private Triple createCommitDateProperty(String gitHash, String commitDateValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "CommitDate"), literal(commitDateValue));
    }

    private Triple createCommitterNameProperty(String gitHash, String committerNameValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "CommitterName"), literal(committerNameValue));
    }

    private Triple createCommitterEmailProperty(String gitHash, String committerEmailValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "CommitterEmail"), literal(committerEmailValue));
    }

    private Triple createCommitMessageProperty(String gitHash, String commitMessageValue) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "CommitMessage"), literal(commitMessageValue));
    }

    private Triple createCommitDiffEntryEditTypeProperty(Node diffEntryNode, DiffEntry.ChangeType changeType) {
        return Triple.create(diffEntryNode, uri(GIT_URI + "ChangeType"), literal(changeType.toString()));
    }

    private Triple createCommitResource(String gitHash, Node commitNode) {
        return Triple.create(literal(gitHash), uri(GIT_URI + "Commit"), commitNode);
    }

    private Triple createCommitDiffEntryResource(Node commitNode, Node diffEntryNode) {
        return Triple.create(commitNode, uri(GIT_URI + "CommitDiffEntry"), diffEntryNode);
    }

    private Triple createCommitDiffEntryFileNameProperty(Node diffEntryNode, FileHeader fileHeader) {
        return Triple.create(diffEntryNode, uri(GIT_URI + "FileName"), literal(fileHeader.toString()));
    }

    private Triple createCommitDiffEditResource(Node diffEntryNode, Node diffEditNode) {
        return Triple.create(diffEntryNode, uri(GIT_URI + "CommitDiffEdit"), diffEditNode);
    }

    private Triple createCommitDiffEditTypeProperty(Node editNode, Edit.Type editType) {
        return Triple.create(editNode, uri(GIT_URI + "CommitDiffEditType"), literal(editType.toString()));
    }

    private Triple createCommitDiffEditBeginAProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, uri(GIT_URI + "CommitDiffEditBeginA"), literal(Integer.toString(lineNumberBegin)));
    }

    private Triple createCommitDiffEditBeginBProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, uri(GIT_URI + "CommitDiffEditBeginB"), literal(Integer.toString(lineNumberBegin)));
    }

    private Triple createCommitDiffEditEndAProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, uri(GIT_URI + "CommitDiffEditEndA"), literal(Integer.toString(lineNumberEnd)));
    }

    private Triple createCommitDiffEditEndBProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, uri(GIT_URI + "CommitDiffEditEndB"), literal(Integer.toString(lineNumberEnd)));
    }
}
