package de.leipzig.htwk.gitrdf.worker.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility methods for creating GitHub related URIs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GithubUriUtils {

    private static final String GITHUB_BASE = "https://github.com/";

    private static final String GITHUB_API_BASE = "https://api.github.com/";

    public static String getUserUri(String userName) {
        return GITHUB_BASE + userName;
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

    public static String getBranchUri(String owner, String repository, String branchName) {
        return GITHUB_BASE + owner + "/" + repository + "/tree/" + branchName;
    }

    public static String getTagUri(String owner, String repository, String tagName) {
        return GITHUB_BASE + owner + "/" + repository + "/tags/" + tagName;
    }

    public static String getTagUrl(String owner, String repository, String tagName) {
        return GITHUB_API_BASE + "repos/" + owner + "/" + repository + "/git/tags/" + tagName;
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

    public static String getIssueCommentUrl(String repoString, String commentId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        // Build the correct API URL for pull request comments
        return repoString + "issues/comments/" + commentId;
    }

    public static String getIssueCommentUri(String issueUri, String commentId) {
        // https://github.com/dotnet/core/issues/9938#issuecomment-2978560992
        return issueUri + "#issuecomment-" + commentId;
    }

    // Issue Review

    public static String getIssueReviewUrl(String repoString, String reviewId) {
        //https://api.github.com/repos/dotnet/core/pulls/9935/reviews/2922636653
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/").replace("/pull/", "/pulls/");
        return repoString + "/reviews/" + reviewId;
    }

    public static String getIssueReviewUri(String repoString, String pullId,  String reviewId) {
        // https://github.com/dotnet/core/pull/9935#pullrequestreview-2922636653
        return repoString + pullId + "#pullrequestreview-" + reviewId;

    }

    // Issue Review Comments

    public static String getIssueReviewCommentUrl(String repoString, String commentId) {
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        // Build the correct API URL for pull request comments
        return repoString + "pulls/comments/" + commentId;
    }

    public static String getIssueReviewCommentUri(String issueUri, String commentId) {
        // https://github.com/dotnet/core/pull/9821#discussion_r2019076550
        return issueUri + "#discussion_r" + commentId;
    }

    // Reaction
    public static String getIssueCommentReactionUri(String commentUri, String reactionId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992/reactions/1234567890
        return commentUri + "/reactions#" + reactionId;
    }
    
    public static String getIssueReviewCommentReactionUri(String commentUri, String reactionId) {
        // https://api.github.com/repos/dotnet/core/pulls/comments/2978560992/reactions/1234567890
        return commentUri + "/reactions/" + reactionId;
    }

    // Workflow
    public static String getWorkflowJobUri(String runUri, Long jobId) {
        // https://api.github.com/repos/dotnet/core/actions/jobs/42158567819
        return runUri + "/job/" + jobId;
    }

    public static String getWorkflowJobUrl(String repoString, Long jobId) {
        // https://api.github.com/repos/dotnet/core/actions/jobs/38982110516
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        return repoString + "actions/jobs/" + jobId;
    }

    public static String getWorkflowRunUri(String repoString, String runUri) {
        // Example: https://github.com/dotnet/core/actions/runs/15571576096
        return repoString + "actions/runs/" + runUri;
    }

    public static String getWorkflowRunUrl(String repoString, String runUri) {
        // Example: https://github.com/dotnet/core/actions/runs/15571576096
        return repoString + "actions/runs/" + runUri;
    }


}
