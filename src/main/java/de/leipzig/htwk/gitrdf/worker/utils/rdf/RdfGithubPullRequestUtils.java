package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:GithubPullRequest entities.
 * This class extends RdfGithubIssueUtils and adds pull request-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubPullRequestUtils extends RdfGithubIssueUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // Pull Request specific merge properties (from github ontology)
    public static Node mergedProperty() {
        return RdfUtils.uri(GH_NS + "merged");
    }

    public static Node mergeCommitShaProperty() {
        return RdfUtils.uri(GH_NS + "mergeCommitSha");
    }

    public static Node mergedAtProperty() {
        return RdfUtils.uri(GH_NS + "mergedAt");
    }

    public static Node mergedByProperty() {
        return RdfUtils.uri(GH_NS + "mergedBy");
    }

    // Override to create GitHub Pull Request type
    public static Triple createRdfTypeProperty(String pullRequestUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), rdfTypeProperty(), RdfUtils.uri("github:GithubPullRequest"));
    }

    // Pull Request specific property creation methods
    public static Triple createPullRequestMergedProperty(String pullRequestUri, boolean merged) {
        return Triple.create(RdfUtils.uri(pullRequestUri), mergedProperty(), RdfUtils.booleanLiteral(merged));
    }

    public static Triple createPullRequestMergeCommitShaProperty(String pullRequestUri, String sha) {
        return Triple.create(RdfUtils.uri(pullRequestUri), mergeCommitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createPullRequestMergedAtProperty(String pullRequestUri, LocalDateTime mergedAt) {
        return Triple.create(RdfUtils.uri(pullRequestUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createPullRequestMergedByProperty(String pullRequestUri, String userUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), mergedByProperty(), RdfUtils.uri(userUri));
    }

    // Convenience methods for backward compatibility with existing transaction service
    public static Triple createIssueMergedProperty(String issueUri, boolean merged) {
        return createPullRequestMergedProperty(issueUri, merged);
    }

    public static Triple createIssueMergeCommitShaProperty(String issueUri, String sha) {
        return createPullRequestMergeCommitShaProperty(issueUri, sha);
    }

    public static Triple createIssueMergedAtProperty(String issueUri, LocalDateTime mergedAt) {
        return createPullRequestMergedAtProperty(issueUri, mergedAt);
    }

    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return createPullRequestMergedByProperty(issueUri, userUri);
    }
}