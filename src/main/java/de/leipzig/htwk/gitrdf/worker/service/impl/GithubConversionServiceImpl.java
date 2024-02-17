package de.leipzig.htwk.gitrdf.worker.service.impl;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;

@Service
public class GithubConversionServiceImpl {

    private final GithubRdfConversionTransactionService githubRdfConversionTransactionService;

    public GithubConversionServiceImpl(GithubRdfConversionTransactionService githubRdfConversionTransactionService) {
        this.githubRdfConversionTransactionService = githubRdfConversionTransactionService;
    }

    //@Override
    public void performGithubRepoToRdfConversion(long id) throws IOException, GitAPIException, URISyntaxException, InterruptedException {

        InputStream needsToBeClosed = null;

        try {
            needsToBeClosed
                    = githubRdfConversionTransactionService.performGithubRepoToRdfConversionAndReturnCloseableInputStream(id);
        } finally {
            if (needsToBeClosed != null) {
                needsToBeClosed.close();
            }
        }


    }

}
