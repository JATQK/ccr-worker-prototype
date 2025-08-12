package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformTicketUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:Issue entities.
 * This class extends RdfPlatformTicketUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfGithubIssueUtils extends RdfPlatformTicketUtils {

    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Use platform ontology properties (v2)
    public static Node lockedProperty() {
        return uri(PLATFORM_NS + "locked");
    }

    public static Node requestedReviewerProperty() {
        return uri(PLATFORM_NS + "requestedReviewer");
    }

    public static Node draftProperty() {
        return uri(PLATFORM_NS + "draft");
    }

    public static Node nodeIdProperty() {
        return uri(PLATFORM_NS + "nodeId");
    }

    public static Node referencedByProperty() {
        return uri(PLATFORM_NS + "referencedBy");
    }


    // Use platform properties for relationships
    public static Node containsCommitProperty() {
        return uri("platform:containsCommit");
    }


    // Override platform method to create GitHub Issue type
    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(uri(issueUri), rdfTypeProperty(), uri("github:Issue"));
    }


    // v2.1: Use inherited platform method for number
    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return RdfPlatformTicketUtils.createNumberProperty(issueUri, number);
    }

    // Use platform id property
    public static Triple createIssueIdProperty(String issueUri, String issueId) {
        return RdfPlatformTicketUtils.createIdProperty(issueUri, issueId);
    }
    
    public static Triple createIssueIdProperty(String issueUri, long issueId) {
        return createIssueIdProperty(issueUri, String.valueOf(issueId));
    }

    public static Triple createIssueLockedProperty(String issueUri, boolean locked) {
        return Triple.create(uri(issueUri), lockedProperty(), RdfUtils.booleanLiteral(locked));
    }

    public static Triple createIssueDraftProperty(String issueUri, boolean draft) {
        return Triple.create(uri(issueUri), draftProperty(), RdfUtils.booleanLiteral(draft));
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
        return Triple.create(uri(issueUri), containsCommitProperty(), uri(commitUri));
    }

    public static Triple createIssueRequestedReviewerProperty(String issueUri, String reviewerUri) {
        return Triple.create(uri(issueUri), requestedReviewerProperty(), uri(reviewerUri));
    }

    public static Triple createIssueNodeIdProperty(String issueUri, String nodeId) {
        return Triple.create(uri(issueUri), nodeIdProperty(), RdfUtils.stringLiteral(nodeId));
    }

    public static Triple createIssueReferencedByProperty(String issueUri, String referencingUri) {
        return Triple.create(uri(issueUri), referencedByProperty(), uri(referencingUri));
    }

}


