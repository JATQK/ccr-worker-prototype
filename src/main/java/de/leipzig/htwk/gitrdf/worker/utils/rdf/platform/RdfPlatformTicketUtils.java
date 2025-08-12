package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Ticket entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for tickets/issues that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformTicketUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform Ticket Properties (from platform ontology)
    public static Node idProperty() {
        return uri(PLATFORM_NS + "id");
    }

    public static Node titleProperty() {
        return uri(PLATFORM_NS + "title");
    }

    public static Node bodyProperty() {
        return uri(PLATFORM_NS + "body");
    }

    public static Node numberProperty() {
        return uri(PLATFORM_NS + "number");
    }

    public static Node stateProperty() {
        return uri(PLATFORM_NS + "state");
    }

    public static Node submitterProperty() {
        return uri(PLATFORM_NS + "submitter");
    }

    public static Node assigneeProperty() {
        return uri(PLATFORM_NS + "assignee");
    }

    public static Node createdAtProperty() {
        return uri(PLATFORM_NS + "createdAt");
    }

    public static Node updatedAtProperty() {
        return uri(PLATFORM_NS + "updatedAt");
    }

    public static Node closedAtProperty() {
        return uri(PLATFORM_NS + "closedAt");
    }

    public static Node hasLabelProperty() {
        return uri(PLATFORM_NS + "hasLabel");
    }

    public static Node hasMilestoneProperty() {
        return uri(PLATFORM_NS + "hasMilestone");
    }

    // Add missing platform properties for ticket-commit relationships
    public static Node partOfTicketProperty() {
        return uri(PLATFORM_NS + "partOfTicket");
    }

    public static Node containsCommitProperty() {
        return uri(PLATFORM_NS + "containsCommit");
    }

    public static Node hasCommentProperty() {
        return uri(PLATFORM_NS + "hasComment");
    }

    // Merge properties (pull request foundation - lines 158-181 in ontology)
    public static Node mergedProperty() {
        return uri(PLATFORM_NS + "merged");
    }

    public static Node mergedAtProperty() {
        return uri(PLATFORM_NS + "mergedAt");
    }

    public static Node mergeCommitShaProperty() {
        return uri(PLATFORM_NS + "mergeCommitSha");
    }

    public static Node mergedByProperty() {
        return uri(PLATFORM_NS + "mergedBy");
    }

    public static Node lockedProperty() {
        return uri(PLATFORM_NS + "locked");
    }

    // New generalized properties based on ontology changes
    public static Node referencedByProperty() {
        return uri(PLATFORM_NS + "referencedBy");
    }

    public static Node hasReactionProperty() {
        return uri(PLATFORM_NS + "hasReaction");
    }

    public static Node commitShaProperty() {
        return uri(PLATFORM_NS + "commitSha");
    }

    // Pull Request branch properties (lines 176-184 in ontology)
    public static Node sourceBranchProperty() {
        return uri(PLATFORM_NS + "sourceBranch");
    }

    public static Node targetBranchProperty() {
        return uri(PLATFORM_NS + "targetBranch");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String ticketUri) {
        return Triple.create(uri(ticketUri), rdfTypeProperty(), uri("platform:Ticket"));
    }

    public static Triple createIdProperty(String ticketUri, String id) {
        return Triple.create(uri(ticketUri), idProperty(), RdfUtils.stringLiteral(id));
    }

    public static Triple createTitleProperty(String ticketUri, String title) {
        return Triple.create(uri(ticketUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createBodyProperty(String ticketUri, String body) {
        return Triple.create(uri(ticketUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createNumberProperty(String ticketUri, int number) {
        return Triple.create(uri(ticketUri), numberProperty(), RdfUtils.nonNegativeIntegerLiteral(number));
    }

    public static Triple createStateProperty(String ticketUri, String state) {
        return Triple.create(uri(ticketUri), stateProperty(), uri("platform:" + state.toLowerCase()));
    }

    public static Triple createSubmitterProperty(String ticketUri, String userUri) {
        return Triple.create(uri(ticketUri), submitterProperty(), uri(userUri));
    }

    public static Triple createAssigneeProperty(String ticketUri, String userUri) {
        return Triple.create(uri(ticketUri), assigneeProperty(), uri(userUri));
    }

    public static Triple createCreatedAtProperty(String ticketUri, LocalDateTime createdAt) {
        return Triple.create(uri(ticketUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createUpdatedAtProperty(String ticketUri, LocalDateTime updatedAt) {
        return Triple.create(uri(ticketUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createClosedAtProperty(String ticketUri, LocalDateTime closedAt) {
        return Triple.create(uri(ticketUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAt));
    }

    public static Triple createHasLabelProperty(String ticketUri, String labelUri) {
        return Triple.create(uri(ticketUri), hasLabelProperty(), uri(labelUri));
    }

    public static Triple createHasMilestoneProperty(String ticketUri, String milestoneUri) {
        return Triple.create(uri(ticketUri), hasMilestoneProperty(), uri(milestoneUri));
    }

    public static Triple createHasCommentProperty(String ticketUri, String commentUri) {
        return Triple.create(uri(ticketUri), hasCommentProperty(), uri(commentUri));
    }

    public static Triple createPartOfTicketProperty(String commitUri, String ticketUri) {
        return Triple.create(uri(commitUri), partOfTicketProperty(), uri(ticketUri));
    }

    public static Triple createContainsCommitProperty(String ticketUri, String commitUri) {
        return Triple.create(uri(ticketUri), containsCommitProperty(), uri(commitUri));
    }

    // Merge property creation methods (platform ticket foundation)
    public static Triple createMergedProperty(String ticketUri, boolean merged) {
        return Triple.create(uri(ticketUri), mergedProperty(), RdfUtils.booleanLiteral(merged));
    }

    public static Triple createMergedAtProperty(String ticketUri, LocalDateTime mergedAt) {
        return Triple.create(uri(ticketUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createMergeCommitShaProperty(String ticketUri, String sha) {
        return Triple.create(uri(ticketUri), mergeCommitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createMergedByProperty(String ticketUri, String userUri) {
        return Triple.create(uri(ticketUri), mergedByProperty(), uri(userUri));
    }

    public static Triple createLockedProperty(String ticketUri, boolean locked) {
        return Triple.create(uri(ticketUri), lockedProperty(), RdfUtils.booleanLiteral(locked));
    }

    // New generalized property creation methods
    public static Triple createReferencedByProperty(String ticketUri, String commitUri) {
        return Triple.create(uri(ticketUri), referencedByProperty(), uri(commitUri));
    }

    public static Triple createHasReactionProperty(String resourceUri, String reactionUri) {
        return Triple.create(uri(resourceUri), hasReactionProperty(), uri(reactionUri));
    }

    public static Triple createCommitShaProperty(String resourceUri, String sha) {
        return Triple.create(uri(resourceUri), commitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    // Pull Request branch property creation methods
    public static Triple createSourceBranchProperty(String pullRequestUri, String branchUri) {
        return Triple.create(uri(pullRequestUri), sourceBranchProperty(), uri(branchUri));
    }

    public static Triple createTargetBranchProperty(String pullRequestUri, String branchUri) {
        return Triple.create(uri(pullRequestUri), targetBranchProperty(), uri(branchUri));
    }
}