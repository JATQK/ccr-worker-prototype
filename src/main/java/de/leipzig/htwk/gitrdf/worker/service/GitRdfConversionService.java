package de.leipzig.htwk.gitrdf.worker.service;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.sql.SQLException;

public interface GitRdfConversionService {

    void performGitRepoToRdfConversion(long id) throws SQLException, IOException, GitAPIException;

}
