package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformTicketUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:GithubIssue entities.
 * This class extends RdfPlatformTicketUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfGithubIssueUtils extends RdfPlatformTicketUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PF_NS = PLATFORM_NAMESPACE + ":";

    // GitHub-specific Issue Properties (from github ontology)

    public static Node issueIdProperty() {
        return RdfUtils.uri(GH_NS + "issueId");
    }

    public static Node nodeIdProperty() {
        return RdfUtils.uri(GH_NS + "nodeId");
    }

    public static Node lockedProperty() {
        return RdfUtils.uri(PF_NS + "locked");
    }


    public static Node requestedReviewerProperty() {
        return RdfUtils.uri(GH_NS + "requestedReviewer");
    }


    // GitHub-specific commit linking
    public static Node containsCommitProperty() {
        return RdfUtils.uri(GH_NS + "containsCommit");
    }

    public static Node referencedByProperty() {
        return RdfUtils.uri(GH_NS + "referencedBy");
    }


    // Override platform method to create GitHub Issue type
    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri("github:GithubIssue"));
    }

    // v2.1: Use inherited platform method for number
    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return RdfPlatformTicketUtils.createNumberProperty(issueUri, number);
    }

    // GitHub-specific property creation methods
    public static Triple createIssueIdProperty(String issueUri, long issueId) {
        return Triple.create(RdfUtils.uri(issueUri), issueIdProperty(), RdfUtils.longLiteral(issueId));
    }
    
    public static Triple createIssueNodeIdProperty(String issueUri, String nodeId) {
        return Triple.create(RdfUtils.uri(issueUri), nodeIdProperty(), RdfUtils.stringLiteral(nodeId));
    }

    public static Triple createIssueLockedProperty(String issueUri, boolean locked) {
        return Triple.create(RdfUtils.uri(issueUri), lockedProperty(), RdfUtils.booleanLiteral(locked));
    }


    // v2.1: Use inherited platform methods for common properties
    public static Triple createIssueStateProperty(String issueUri, String state) {
        return RdfPlatformTicketUtils.createStateProperty(issueUri, state);
    }

    public static Triple createIssueTitleProperty(String issueUri, String title) {
        return RdfPlatformTicketUtils.createTitleProperty(issueUri, title);
    }

    public static Triple createIssueBodyProperty(String issueUri, String body) {
        return RdfPlatformTicketUtils.createBodyProperty(issueUri, body);
    }

    public static Triple createIssueUserProperty(String issueUri, String userUri) {
        return RdfPlatformTicketUtils.createSubmitterProperty(issueUri, userUri);
    }

    public static Triple createIssueAssigneeProperty(String issueUri, String userUri) {
        return RdfPlatformTicketUtils.createAssigneeProperty(issueUri, userUri);
    }

    public static Triple createIssueMilestoneProperty(String issueUri, String milestoneUri) {
        return RdfPlatformTicketUtils.createHasMilestoneProperty(issueUri, milestoneUri);
    }

    public static Triple createIssueSubmittedAtProperty(String issueUri, LocalDateTime submittedAtDateTime) {
        return RdfPlatformTicketUtils.createCreatedAtProperty(issueUri, submittedAtDateTime);
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        return RdfPlatformTicketUtils.createUpdatedAtProperty(issueUri, updatedAtDateTime);
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        return RdfPlatformTicketUtils.createClosedAtProperty(issueUri, closedAtDateTime);
    }


    public static Triple createIssueContainsCommitProperty(String issueUri, String commitUri) {
        return Triple.create(RdfUtils.uri(issueUri), containsCommitProperty(), RdfUtils.uri(commitUri));
    }

    public static Triple createIssueReferencedByProperty(String issueUri, String commitUri) {
        return Triple.create(RdfUtils.uri(issueUri), referencedByProperty(), RdfUtils.uri(commitUri));
    }

    public static Triple createIssueRequestedReviewerProperty(String issueUri, String reviewerUri) {
        return Triple.create(RdfUtils.uri(issueUri), requestedReviewerProperty(), RdfUtils.uri(reviewerUri));
    }

}


