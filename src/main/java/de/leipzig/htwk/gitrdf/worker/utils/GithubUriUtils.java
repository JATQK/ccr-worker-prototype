package de.leipzig.htwk.gitrdf.worker.utils;

import java.net.URL;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility methods for creating GitHub related URIs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GithubUriUtils {

    private static final String GITHUB_BASE = "https://github.com/";


    public static String getWorkflowRunUri(URL runUri) {
        return runUri.toString();
    }

    public static String getWorkflowJobUri(String runUri, Long jobId) {
        return runUri + "/job/" + jobId;
    }
    public static String getRepositoryUri(String owner, String repository) {
        return GITHUB_BASE + owner + "/" + repository + "/";
    }

    public static String getCommitBaseUri(String owner, String repository) {
        return GITHUB_BASE + owner + "/" + repository + "/commit/";
    }

    public static String getCommitUri(String owner, String repository, String commitHash) {
        return getCommitBaseUri(owner, repository) + commitHash;
    }

    public static String getPullRequestUri(String prUrl) {
        return prUrl;
    }

    public static String getIssueBaseUri(String owner, String repository) {
        return GITHUB_BASE + owner + "/" + repository + "/issues/";
    }

    public static String getIssueUri(String owner, String repository, String issueNumber) {
        return getIssueBaseUri(owner, repository) + issueNumber;
    }

    public static String getIssueCommentUri(String issueUri, String commentId) {
        return issueUri + "#issuecomment-" + commentId;
    }

    public static String getUserUri(String userName) {
        return GITHUB_BASE + userName;
    }
}
