package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.service.GitRdfConversionService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@Service
public class GitRdfConversionServiceImpl implements GitRdfConversionService {

    private final GitRdfConversionTransactionService gitRdfConversionTransactionService;

    public GitRdfConversionServiceImpl(GitRdfConversionTransactionService gitRdfConversionTransactionService) {
        this.gitRdfConversionTransactionService = gitRdfConversionTransactionService;
    }

    @Override
    public void performGitRepoToRdfConversion(long id) throws SQLException, IOException, GitAPIException {

        InputStream needsToBeClosed = null;

        try {
            needsToBeClosed
                    = gitRdfConversionTransactionService.performGitRepoToRdfConversionAndReturnCloseableInputStream(id);
        } finally {
            if (needsToBeClosed != null) {
                needsToBeClosed.close();
            }
        }


    }
}
