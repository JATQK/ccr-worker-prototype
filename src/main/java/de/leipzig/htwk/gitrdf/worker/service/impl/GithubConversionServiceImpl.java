package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.handler.LockHandler;
import de.leipzig.htwk.gitrdf.worker.timemeasurement.TimeLog;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;

@Service
public class GithubConversionServiceImpl {

    private final GithubRdfConversionTransactionService githubRdfConversionTransactionService;

    public GithubConversionServiceImpl(GithubRdfConversionTransactionService githubRdfConversionTransactionService) {
        this.githubRdfConversionTransactionService = githubRdfConversionTransactionService;
    }

    //@Override
    public void performGithubRepoToRdfConversion(
            long id,
            TimeLog timelog,
            LockHandler lockHandler) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        InputStream needsToBeClosed = null;
        File gitWorkingDirectory = Files.createTempDirectory("git-working-directory").toFile();
        File rdfTempFile = Files.createTempFile("temp-rdf-write-file", ".dat").toFile();

        try {

            needsToBeClosed = githubRdfConversionTransactionService.performGithubRepoToRdfConversionWithGitCloningLogicAndReturnCloseableInputStream(
                    id,
                    gitWorkingDirectory,
                    rdfTempFile,
                    timelog,
                    lockHandler);

        } finally {

            if (needsToBeClosed != null) needsToBeClosed.close();
            FileUtils.deleteQuietly(gitWorkingDirectory);
            FileUtils.deleteQuietly(rdfTempFile);
        }


    }

}
