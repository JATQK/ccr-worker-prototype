package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import java.io.IOException;

@Slf4j
public class RdfGitCommitUserUtils {
    public String hash;
    public String gitHubUser;

    public RdfGitCommitUserUtils(String hash, String gitHubUser) {
        this.hash = hash;
        this.gitHubUser = gitHubUser;
    }

    // Method to retrieve GitHub user from commit hash
    public static String getGitHubUserFromCommit(GHRepository repo, String commitHash) {
        try {
            GHCommit commit = repo.getCommit(commitHash);
            GHUser author = commit.getAuthor();
            return author != null ? "https://github.com/" + author.getLogin() : null;
        } catch (IOException e) {
            log.info("Could not retrieve github-user from commit hash '{}'", commitHash, e);
            return null;
        }
    }
}