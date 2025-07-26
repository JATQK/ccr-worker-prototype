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
        return GITHUB_BASE + owner + "/" + repository + "/releases/tag/" + tagName;
    }

    public static String getTagUrl(String owner, String repository, String tagName) {
        return GITHUB_API_BASE + "repos/" + owner + "/" + repository + "/releases/tags/" + tagName;
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

    public static String getIssueReviewUri(String issueUri, String reviewId) {
        // https://github.com/dotnet/core/issues/9935#pullrequestreview-2922636653
        return issueUri + "#pullrequestreview-" + reviewId;
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
    public static String getIssueCommentReactionUri(String issueUri, String commentId, String reactionId) {
        // Extract issue number from issueUri (e.g., from https://github.com/dotnet/core/issues/7455)
        String issueNumber = issueUri.substring(issueUri.lastIndexOf('/') + 1);
        // github:reaction-i7455-c1207081381-r178832322
        return "github:reaction-i" + issueNumber + "-c" + commentId + "-r" + reactionId;
    }
    
    public static String getIssueReviewCommentReactionUri(String issueUri, String commentId, String reactionId) {
        // Extract issue number from issueUri (e.g., from https://github.com/dotnet/core/issues/7455)
        String issueNumber = issueUri.substring(issueUri.lastIndexOf('/') + 1);
        // github:reaction-i7455-c1207081381-r178832322
        return "github:reaction-i" + issueNumber + "-c" + commentId + "-r" + reactionId;
    }

    public static String getIssueCommentReactionsApiUrl(String owner, String repository, String commentId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/1207081381/reactions
        return GITHUB_API_BASE + "repos/" + owner + "/" + repository + "/issues/comments/" + commentId + "/reactions";
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
