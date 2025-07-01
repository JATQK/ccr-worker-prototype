package de.leipzig.htwk.gitrdf.worker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility methods for creating GitHub related URIs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GithubUriUtils {

    private static final String GITHUB_BASE = "https://github.com/";

    public static String getWorkflowJobUri(String runUri, Long jobId) {
        return runUri + "/job/" + jobId;
    }

    public static String getWorkflowRunUri(String runUri) {
        // Example: https://github.com/dotnet/core/actions/runs/15571576096
        return runUri;
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
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992
        return issueUri.replace("github.com/", "api.github.com/repos/") + "/comments/" + commentId;
    }

    public static String getIssueCommentURL(String issueUri, String commentId) {
        // https://github.com/dotnet/core/issues/9938#issuecomment-2978560992
        return issueUri + "#issuecomment-" + commentId;
    }

    public static String getIssueCommentReactionUri(String commentUri, String reactionId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992/reactions/1234567890
        return commentUri + "/reactions/" + reactionId;
    }

    public static String getIssueReviewUri(String issueUri, String reviewId) {
        //https://api.github.com/repos/dotnet/core/pulls/9935/reviews/2922636653
        return issueUri.replace("github.com/", "api.github.com/repos/").replace("/pull/", "/pulls/") + "/reviews/"
                + reviewId;
    }

    public static String getIssueReviewCommentUri(String issueUri, String reviewId) {
        // https://api.github.com/repos/{owner}/{repo}/pulls/comments/{comment_id}
        return issueUri.replace("github.com/", "api.github.com/repos/").replace("pull", "pulls") + "/comments/"
                + reviewId;
    }
    
    public static String getIssueReviewCommentReactionUri(String commentUri, String reactionId) {
        // https://api.github.com/repos/dotnet/core/pulls/comments/2978560992/reactions/1234567890
        return commentUri + "/reactions/" + reactionId;
    }

    public static String getIssueReviewCommentURL(String issueUri, String commentId) {
        // https://github.com/dotnet/core/pull/9821#discussion_r2019076550
        return issueUri + "#discussion_r" + commentId;
    }


    
    public static String getUserUri(String userName) {
        return GITHUB_BASE + userName;
    }
}
