package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.database.common.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.database.common.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.database.common.entity.lob.GitRepositoryOrderEntityLobs;
import de.leipzig.htwk.gitrdf.worker.utils.GitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.ZipUtils;
import jakarta.persistence.EntityManager;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.hibernate.engine.jdbc.BlobProxy;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfTurtleTidier;
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

            Git gitHandler = new Git(gitRepository);

            for (int iteration = 0; iteration < Integer.MAX_VALUE; iteration++) {

                int skipCount = calculateSkipCountAndThrowExceptionIfIntegerOverflowIsImminent(iteration, commitsPerIteration);

                Iterable<RevCommit> commits = gitHandler.log().setSkip(skipCount).setMaxCount(commitsPerIteration).call();

                boolean finished = true;

                writer.start();

                for (RevCommit commit : commits) {

                    finished = false;

                    String gitHash = commit.getId().name();

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

        RdfTurtleTidier.tidyFile(tempFile);

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

}
