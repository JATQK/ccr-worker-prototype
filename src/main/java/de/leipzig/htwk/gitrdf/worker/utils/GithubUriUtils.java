package de.leipzig.htwk.gitrdf.worker.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility methods for creating GitHub related URIs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GithubUriUtils {

    private static final String GITHUB_BASE = "https://github.com/";

    private static final String GITHUB_API_BASE = "https://api.github.com/";

    /**
     * Percent-encode a string for use in URIs to avoid issues with special characters.
     * This ensures that characters like spaces, unicode characters, etc. are properly encoded.
     */
    private static String encodeUriComponent(String component) {
        try {
            return URLEncoder.encode(component, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20"); // URLEncoder uses + for spaces, but URIs should use %20
        } catch (UnsupportedEncodingException e) {
            // This should never happen with UTF-8
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * Percent-decode a string from URI format back to original format.
     * This is used to convert encoded usernames back to their original form for API calls.
     */
    public static String decodeUriComponent(String component) {
        try {
            return URLDecoder.decode(component, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // This should never happen with UTF-8
            throw new RuntimeException("UTF-8 encoding not supported", e);
        }
    }

    /**
     * Extract and decode the username from a GitHub user URI.
     * Converts https://github.com/netlify%5Bbot%5D back to netlify[bot]
     */
    public static String getUsernameFromUri(String userUri) {
        if (userUri == null || !userUri.startsWith(GITHUB_BASE)) {
            return null;
        }
        String encodedUsername = userUri.substring(GITHUB_BASE.length());
        return decodeUriComponent(encodedUsername);
    }

    public static String getUserUri(String userName) {
        return GITHUB_BASE + encodeUriComponent(userName);
    }

    public static String getRepositoryUri(String owner, String repository) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/";
    }

    public static String getCommitBaseUri(String owner, String repository) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/commit/";
    }

    public static String getCommitUri(String owner, String repository, String commitHash) {
        return getCommitBaseUri(owner, repository) + commitHash; // commit hashes are already safe
    }

    public static String getBranchUri(String owner, String repository, String branchName) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/tree/" + encodeUriComponent(branchName);
    }

    public static String getTagUri(String owner, String repository, String tagName) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/releases/tag/" + encodeUriComponent(tagName);
    }

    public static String getTagUrl(String owner, String repository, String tagName) {
        return GITHUB_API_BASE + "repos/" + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/releases/tags/" + encodeUriComponent(tagName);
    }

    public static String getPullRequestUri(String prUrl) {
        return prUrl;
    }

    public static String getIssueBaseUri(String owner, String repository) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/issues/";
    }

    public static String getIssueUri(String owner, String repository, String issueNumber) {
        return getIssueBaseUri(owner, repository) + issueNumber; // issue numbers are safe
    }

    public static String getPullRequestBaseUri(String owner, String repository) {
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/pull/";
    }

    public static String getPullRequestUri(String owner, String repository, String prNumber) {
        return getPullRequestBaseUri(owner, repository) + prNumber; // PR numbers are safe
    }

    public static String getIssueCommentUrl(String repoString, String commentId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        // Build the correct API URL for issue comments
        return repoString + "/issues/comments/" + commentId;
    }

    public static String getPullRequestCommentUrl(String repoString, String commentId) {
        // https://api.github.com/repos/dotnet/core/issues/comments/2978560992 (PR comments use same API endpoint)
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        // Build the correct API URL for pull request comments
        return repoString + "/issues/comments/" + commentId;
    }

    public static String getIssueCommentUri(String issueUri, String commentId) {
        // https://github.com/dotnet/core/issues/9938#issuecomment-2978560992
        return issueUri + "#issuecomment-" + commentId;
    }

    public static String getPullRequestCommentUri(String pullRequestUri, String commentId) {
        // https://github.com/dotnet/core/pull/9938#issuecomment-2978560992
        return pullRequestUri + "#issuecomment-" + commentId;
    }

    // Issue Review

    public static String getIssueReviewUrl(String repoString, String prNumber, String reviewId) {
        //https://api.github.com/repos/dotnet/core/pulls/9935/reviews/2922636653
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        return repoString + "/pulls/" + prNumber + "/reviews/" + reviewId;
    }

    public static String getIssueReviewUri(String issueUri, String reviewId) {
        // https://github.com/dotnet/core/issues/9935#pullrequestreview-2922636653
        return issueUri + "#pullrequestreview-" + reviewId;
    }

    public static String getPullRequestReviewUri(String pullRequestUri, String reviewId) {
        // https://github.com/dotnet/core/pull/9935#pullrequestreview-2922636653
        return pullRequestUri + "#pullrequestreview-" + reviewId;
    }

    // Issue Review Comments

    public static String getIssueReviewCommentUrl(String repoString, String commentId) {
        repoString = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        // Build the correct API URL for pull request review comments
        return repoString + "/pulls/comments/" + commentId;
    }

    public static String getIssueReviewCommentUri(String issueUri, String commentId) {
        // https://github.com/dotnet/core/pull/9821#discussion_r2019076550
        return issueUri + "#discussion_r" + commentId;
    }

    public static String getPullRequestReviewCommentUri(String pullRequestUri, String commentId) {
        // https://github.com/dotnet/core/pull/9821#discussion_r2019076550
        return pullRequestUri + "#discussion_r" + commentId;
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

    // Labels
    public static String getLabelUri(String owner, String repository, String labelName) {
        // https://github.com/dotnet/core/labels/area-System.Net
        // Percent-encode the label name to handle spaces and special characters
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/labels/" + encodeUriComponent(labelName);
    }

    public static String getLabelsApiUrl(String owner, String repository) {
        // https://api.github.com/repos/dotnet/core/labels
        return GITHUB_API_BASE + "repos/" + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/labels";
    }

    // Milestones  
    public static String getMilestoneUri(String owner, String repository, String milestoneNumber) {
        // https://github.com/dotnet/core/milestone/123
        return GITHUB_BASE + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/milestone/" + milestoneNumber; // milestone numbers are safe
    }

    public static String getMilestonesApiUrl(String owner, String repository) {
        // https://api.github.com/repos/dotnet/core/milestones
        return GITHUB_API_BASE + "repos/" + encodeUriComponent(owner) + "/" + encodeUriComponent(repository) + "/milestones";
    }

}
