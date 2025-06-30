package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import java.io.IOException;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;

import lombok.extern.slf4j.Slf4j;

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

            if (author == null) {
                return null;
            }

            // Check if this is a valid GitHub user account
            String login = author.getLogin();
            if (login == null || login.isEmpty() || isAutomatedAccount(login)) {
                return null;
            }

            return GithubUriUtils.getUserUri(login);
        } catch (IOException e) {
            log.info("Could not retrieve github-user from commit hash '{}'", commitHash, e);
            return null;
        }
    }

    private static boolean isAutomatedAccount(String login) {
        login = login.toLowerCase();
        // Common patterns for automated accounts
        return login.contains("[bot]") ||
                // Add other known automation patterns
                login.contains("copilot");
    }
}