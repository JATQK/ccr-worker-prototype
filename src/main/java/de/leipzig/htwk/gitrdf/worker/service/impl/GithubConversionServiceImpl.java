package de.leipzig.htwk.gitrdf.worker.service.impl;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Service
public class GithubConversionServiceImpl {

    private final GithubRdfConversionTransactionService githubRdfConversionTransactionService;

    public GithubConversionServiceImpl(GithubRdfConversionTransactionService githubRdfConversionTransactionService) {
        this.githubRdfConversionTransactionService = githubRdfConversionTransactionService;
    }

    //@Override
    public void performGithubRepoToRdfConversion(long id) throws IOException, GitAPIException {

        InputStream needsToBeClosed = null;
        File gitWorkingDirectory = Files.createTempDirectory("git-working-directory").toFile();
        File rdfTempFile = Files.createTempFile("temp-rdf-write-file", ".dat").toFile();

        try {

            needsToBeClosed = githubRdfConversionTransactionService.performGithubRepoToRdfConversionWithGitCloningLogicAndReturnCloseableInputStream(
                    id,
                    gitWorkingDirectory,
                    rdfTempFile);

        } finally {

            if (needsToBeClosed != null) needsToBeClosed.close();
            FileUtils.deleteQuietly(gitWorkingDirectory);
            FileUtils.deleteQuietly(rdfTempFile);
        }


    }

}
