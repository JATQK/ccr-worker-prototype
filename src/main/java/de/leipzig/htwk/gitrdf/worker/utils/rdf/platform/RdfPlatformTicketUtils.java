package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

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
        return RdfUtils.uri("rdf:type");
    }

    // Platform Ticket Properties (from platform ontology)
    public static Node titleProperty() {
        return RdfUtils.uri(PLATFORM_NS + "title");
    }

    public static Node bodyProperty() {
        return RdfUtils.uri(PLATFORM_NS + "body");
    }

    public static Node numberProperty() {
        return RdfUtils.uri(PLATFORM_NS + "number");
    }

    public static Node stateProperty() {
        return RdfUtils.uri(PLATFORM_NS + "state");
    }

    public static Node submitterProperty() {
        return RdfUtils.uri(PLATFORM_NS + "submitter");
    }

    public static Node assigneeProperty() {
        return RdfUtils.uri(PLATFORM_NS + "assignee");
    }

    public static Node createdAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "createdAt");
    }

    public static Node updatedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "updatedAt");
    }

    public static Node closedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "closedAt");
    }

    public static Node hasLabelProperty() {
        return RdfUtils.uri(PLATFORM_NS + "hasLabel");
    }

    public static Node hasMilestoneProperty() {
        return RdfUtils.uri(PLATFORM_NS + "hasMilestone");
    }

    public static Node hasCommentProperty() {
        return RdfUtils.uri(PLATFORM_NS + "hasComment");
    }

    // Merge properties (pull request foundation - lines 158-181 in ontology)
    public static Node mergedProperty() {
        return RdfUtils.uri(PLATFORM_NS + "merged");
    }

    public static Node mergedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "mergedAt");
    }

    public static Node mergeCommitShaProperty() {
        return RdfUtils.uri(PLATFORM_NS + "mergeCommitSha");
    }

    public static Node mergedByProperty() {
        return RdfUtils.uri(PLATFORM_NS + "mergedBy");
    }

    public static Node lockedProperty() {
        return RdfUtils.uri(PLATFORM_NS + "locked");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String ticketUri) {
        return Triple.create(RdfUtils.uri(ticketUri), rdfTypeProperty(), RdfUtils.uri("platform:Ticket"));
    }

    public static Triple createTitleProperty(String ticketUri, String title) {
        return Triple.create(RdfUtils.uri(ticketUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createBodyProperty(String ticketUri, String body) {
        return Triple.create(RdfUtils.uri(ticketUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createNumberProperty(String ticketUri, int number) {
        return Triple.create(RdfUtils.uri(ticketUri), numberProperty(), RdfUtils.integerLiteral(number));
    }

    public static Triple createStateProperty(String ticketUri, String state) {
        return Triple.create(RdfUtils.uri(ticketUri), stateProperty(), RdfUtils.uri(PLATFORM_NS + state.toLowerCase()));
    }

    public static Triple createSubmitterProperty(String ticketUri, String userUri) {
        return Triple.create(RdfUtils.uri(ticketUri), submitterProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createAssigneeProperty(String ticketUri, String userUri) {
        return Triple.create(RdfUtils.uri(ticketUri), assigneeProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createCreatedAtProperty(String ticketUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(ticketUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createUpdatedAtProperty(String ticketUri, LocalDateTime updatedAt) {
        return Triple.create(RdfUtils.uri(ticketUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createClosedAtProperty(String ticketUri, LocalDateTime closedAt) {
        return Triple.create(RdfUtils.uri(ticketUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAt));
    }

    public static Triple createHasLabelProperty(String ticketUri, String labelUri) {
        return Triple.create(RdfUtils.uri(ticketUri), hasLabelProperty(), RdfUtils.uri(labelUri));
    }

    public static Triple createHasMilestoneProperty(String ticketUri, String milestoneUri) {
        return Triple.create(RdfUtils.uri(ticketUri), hasMilestoneProperty(), RdfUtils.uri(milestoneUri));
    }

    public static Triple createHasCommentProperty(String ticketUri, String commentUri) {
        return Triple.create(RdfUtils.uri(ticketUri), hasCommentProperty(), RdfUtils.uri(commentUri));
    }

    // Merge property creation methods (platform ticket foundation)
    public static Triple createMergedProperty(String ticketUri, boolean merged) {
        return Triple.create(RdfUtils.uri(ticketUri), mergedProperty(), RdfUtils.booleanLiteral(merged));
    }

    public static Triple createMergedAtProperty(String ticketUri, LocalDateTime mergedAt) {
        return Triple.create(RdfUtils.uri(ticketUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createMergeCommitShaProperty(String ticketUri, String sha) {
        return Triple.create(RdfUtils.uri(ticketUri), mergeCommitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createMergedByProperty(String ticketUri, String userUri) {
        return Triple.create(RdfUtils.uri(ticketUri), mergedByProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createLockedProperty(String ticketUri, boolean locked) {
        return Triple.create(RdfUtils.uri(ticketUri), lockedProperty(), RdfUtils.booleanLiteral(locked));
    }
}