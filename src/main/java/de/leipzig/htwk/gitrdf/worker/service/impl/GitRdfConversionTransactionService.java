package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.database.entity.GitRepositoryOrderEntity;
import de.leipzig.htwk.gitrdf.worker.database.entity.enums.GitRepositoryOrderStatus;
import de.leipzig.htwk.gitrdf.worker.database.entity.lob.GitRepositoryOrderEntityLobs;
import jakarta.persistence.EntityManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.hibernate.engine.jdbc.BlobProxy;
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

    private final EntityManager entityManager;

    public GitRdfConversionTransactionService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(rollbackFor = {SQLException.class, IOException.class, GitAPIException.class}) // Runtime-Exceptions are rollbacked by default; Checked-Exceptions not
    public InputStream performGitRepoToRdfConversionAndReturnCloseableInputStream(long id) throws SQLException, IOException, GitAPIException {

        GitRepositoryOrderEntityLobs gitRepositoryOrderEntityLobs
                = entityManager.find(GitRepositoryOrderEntityLobs.class, id);

        GitRepositoryOrderEntity gitRepositoryOrderEntity = entityManager.find(GitRepositoryOrderEntity.class, id);

        File tempGitRepositoryParentFile = extractZip(gitRepositoryOrderEntityLobs.getGitZipFile());

        File gitFile
                = getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(tempGitRepositoryParentFile);

        // set Ascii stream is not supported -> use ClobProxy instead, wrap outputstream in reader?
        //writeRdf(gitFile, gitRepositoryOrderEntityLobs.getRdfFile().setAsciiStream(POSITION_START));

        InputStream needsToBeClosedOutsideOfTransaction = writeRdf(gitFile, gitRepositoryOrderEntityLobs);

        gitRepositoryOrderEntity.setStatus(GitRepositoryOrderStatus.DONE);

        return needsToBeClosedOutsideOfTransaction;
    }

    private File extractZip(Blob blob) throws SQLException, IOException {

        File gitRepositoryTempDirectory = Files.createTempDirectory("GitRepositoryTempDirectory").toFile();

        byte[] buffer = new byte[1024];

        try (ZipInputStream zipStream = new ZipInputStream(blob.getBinaryStream())) {

            ZipEntry zipEntry = zipStream.getNextEntry();

            while (zipEntry != null) {

                File newFile = newFile(gitRepositoryTempDirectory, zipEntry);

                if (zipEntry.isDirectory()) {

                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }

                } else {

                    File parent = newFile.getParentFile();

                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent);
                    }

                    // write file content
                    try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {

                        int len;

                        while ((len = zipStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, len);
                        }
                    }

                }

                zipEntry = zipStream.getNextEntry();

            }

            zipStream.closeEntry();
        }

        return gitRepositoryTempDirectory;

    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {

        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        // Protect against a ZIP-Slip
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target directory: " + zipEntry.getName());
        }

        return destFile;
    }

    private File getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(File parentDirectoryFile) {

        File[] files = parentDirectoryFile.listFiles((dir, name) -> name.equals(".git"));

        if (files.length < 1) {
            throw new RuntimeException("Expected git repository parent directory file doesn't contain '.git' directory");
        }

        if (files.length > 1) {
            throw new RuntimeException("Expected git repository parent directory file contains more than a single '.git' directory");
        }

        return files[0];
    }

    //private void writeRdf(File gitFile, OutputStream targetOutputStream) throws GitAPIException, IOException {
    private InputStream writeRdf(File gitFile, GitRepositoryOrderEntityLobs entityLobs) throws GitAPIException, IOException {

        Model rdfModel = ModelFactory.createDefaultModel();
        String gitUri = "git://";
        String gitNamespaceName = "git";

        Property authorNameProperty = rdfModel.createProperty(gitUri + "AuthorName");
        Property authorEmailProperty = rdfModel.createProperty(gitUri + "AuthorEmail");
        Property authorDateProperty = rdfModel.createProperty(gitUri + "AuthorDate");
        Property commitDateProperty = rdfModel.createProperty(gitUri + "CommitDate");
        Property committerNameProperty = rdfModel.createProperty(gitUri + "CommitterName");
        Property committerEmailProperty = rdfModel.createProperty(gitUri + "CommitterEmail");
        Property commitMessageProperty = rdfModel.createProperty(gitUri + "CommitMessage");

        // No possible solution found yet -> maybe traverse to parent? Maybe both
        //Property mergeCommitProperty = rdfModel.createProperty(gitUri + "MergeCommit");

        Repository gitRepository = new FileRepositoryBuilder().setGitDir(gitFile).build();

        Git gitHandler = new Git(gitRepository);

        Iterable<RevCommit> logs = gitHandler.log().call();

        for (RevCommit commit : logs) {

            Resource gitHash = rdfModel.createResource(gitUri + commit.getId().name());

            gitHash.addProperty(authorNameProperty, commit.getAuthorIdent().getName());
            gitHash.addProperty(authorEmailProperty, commit.getAuthorIdent().getEmailAddress());

            Instant instant = Instant.ofEpochSecond(commit.getCommitTime());
            LocalDateTime commitDateTime = instant.atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();

            gitHash.addProperty(authorDateProperty, commitDateTime.toString());
            gitHash.addProperty(commitDateProperty, commitDateTime.toString());
            gitHash.addProperty(committerNameProperty, commit.getCommitterIdent().getName());
            gitHash.addProperty(committerEmailProperty, commit.getCommitterIdent().getEmailAddress());
            gitHash.addProperty(commitMessageProperty, commit.getFullMessage());

            //commit.getParent()
        }

        rdfModel.setNsPrefix(gitNamespaceName, gitUri);

        File tempFile = Files.createTempFile("temp-rdf-write-file", ".dat").toFile();

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile))) {
            RDFDataMgr.write(outputStream, rdfModel, Lang.TURTLE);
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


}
