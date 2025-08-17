package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformTicketUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:PullRequest entities.
 * This class extends RdfGithubIssueUtils and adds pull request-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubPullRequestUtils extends RdfGithubIssueUtils {

    // Pull request properties are inherited from platform ontology

    // Override to create GitHub Pull Request type
    public static Triple createRdfTypeProperty(String pullRequestUri) {
        return Triple.create(uri(pullRequestUri), rdfTypeProperty(), uri("github:PullRequest"));
    }

    // Convenience methods for backward compatibility with existing transaction service
    public static Triple createIssueMergedProperty(String issueUri, boolean merged) {
        return RdfPlatformTicketUtils.createMergedProperty(issueUri, merged);
    }

    public static Triple createIssueMergeCommitShaProperty(String issueUri, String sha) {
        return RdfPlatformTicketUtils.createMergeCommitShaProperty(issueUri, sha);
    }

    public static Triple createIssueMergedAtProperty(String issueUri, LocalDateTime mergedAt) {
        return RdfPlatformTicketUtils.createMergedAtProperty(issueUri, mergedAt);
    }

    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return RdfPlatformTicketUtils.createMergedByProperty(issueUri, userUri);
    }

    // Pull Request branch property creation methods
    public static Triple createSourceBranchProperty(String pullRequestUri, String branchUri) {
        return RdfPlatformTicketUtils.createSourceBranchProperty(pullRequestUri, branchUri);
    }

    public static Triple createTargetBranchProperty(String pullRequestUri, String branchUri) {
        return RdfPlatformTicketUtils.createTargetBranchProperty(pullRequestUri, branchUri);
    }

}