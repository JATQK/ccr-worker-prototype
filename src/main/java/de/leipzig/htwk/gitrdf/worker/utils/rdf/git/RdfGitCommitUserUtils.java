package de.leipzig.htwk.gitrdf.worker.utils.rdf.git;

import java.io.IOException;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.GithubUserInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RdfGitCommitUserUtils {
    public String hash;
    public String gitHubUser;

    public RdfGitCommitUserUtils(String hash, String gitHubUser) {
        this.hash = hash;
        this.gitHubUser = gitHubUser;
    }


    // Method to retrieve GitHub user information from a commit hash
    public static GithubUserInfo getGitHubUserInfoFromCommit(GHRepository repo, String commitHash) {
        try {
            GHCommit commit = repo.getCommit(commitHash);
            GHUser author = commit.getAuthor();

            if (author == null) {
                return null;
            }

            String login = author.getLogin();
            if (login == null || login.isEmpty()) {
                return null;
            }
            
            // Note: Automated accounts (bots) should still get User entities created for SHACL validation
            // The isAutomatedAccount check was removed to ensure all users have corresponding RDF entities

            String uri = GithubUriUtils.getUserUri(login);
            long id = author.getId();
            String name = author.getName();
            String gitAuthorEmail = commit.getCommitShortInfo().getAuthor().getEmail();
            return new GithubUserInfo(uri, login, id, name, gitAuthorEmail);
        } catch (IOException e) {
            log.info("Could not retrieve github-user from commit hash '{}'", commitHash, e);
            return null;
        }
    }

    // Legacy method kept for compatibility
    public static String getGitHubUserFromCommit(GHRepository repo, String commitHash) {
        GithubUserInfo info = getGitHubUserInfoFromCommit(repo, commitHash);
        return info == null ? null : info.uri;
    }

}