package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
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

    // Merge properties are now inherited from RdfPlatformTicketUtils via RdfGithubIssueUtils

    // Override to create GitHub Pull Request type
    public static Triple createRdfTypeProperty(String pullRequestUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), rdfTypeProperty(), RdfUtils.uri("github:GithubPullRequest"));
    }

    // Pull Request specific property creation methods (delegate to platform base class)
    public static Triple createPullRequestMergedProperty(String pullRequestUri, boolean merged) {
        return createMergedProperty(pullRequestUri, merged);
    }

    public static Triple createPullRequestMergeCommitShaProperty(String pullRequestUri, String sha) {
        return createMergeCommitShaProperty(pullRequestUri, sha);
    }

    public static Triple createPullRequestMergedAtProperty(String pullRequestUri, LocalDateTime mergedAt) {
        return createMergedAtProperty(pullRequestUri, mergedAt);
    }

    public static Triple createPullRequestMergedByProperty(String pullRequestUri, String userUri) {
        return createMergedByProperty(pullRequestUri, userUri);
    }

    // Convenience methods for backward compatibility with existing transaction service
    public static Triple createIssueMergedProperty(String issueUri, boolean merged) {
        return createMergedProperty(issueUri, merged);
    }

    public static Triple createIssueMergeCommitShaProperty(String issueUri, String sha) {
        return createMergeCommitShaProperty(issueUri, sha);
    }

    public static Triple createIssueMergedAtProperty(String issueUri, LocalDateTime mergedAt) {
        return createMergedAtProperty(issueUri, mergedAt);
    }

    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return createMergedByProperty(issueUri, userUri);
    }
}