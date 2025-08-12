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
 * Platform-agnostic utility class for RDF operations on platform:Milestone entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for milestones that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformMilestoneUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform Milestone Properties (from platform ontology v2)
    public static Node idProperty() {
        return uri(PLATFORM_NS + "id");
    }

    public static Node titleProperty() {
        return uri(PLATFORM_NS + "title");
    }

    public static Node descriptionProperty() {
        return uri(PLATFORM_NS + "description");
    }

    public static Node stateProperty() {
        return uri(PLATFORM_NS + "state");
    }

    public static Node urlProperty() {
        return uri(PLATFORM_NS + "url");
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

    public static Node dueDateProperty() {
        return uri(PLATFORM_NS + "dueDate");
    }

    // Relationship properties
    public static Node hasMilestoneProperty() {
        return uri(PLATFORM_NS + "hasMilestone");
    }

    public static Node milestoneOfProperty() {
        return uri(PLATFORM_NS + "milestoneOf");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String milestoneUri) {
        return Triple.create(uri(milestoneUri), rdfTypeProperty(), uri("platform:Milestone"));
    }

    public static Triple createIdProperty(String milestoneUri, String id) {
        return Triple.create(uri(milestoneUri), idProperty(), RdfUtils.stringLiteral(id));
    }

    public static Triple createTitleProperty(String milestoneUri, String title) {
        return Triple.create(uri(milestoneUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createDescriptionProperty(String milestoneUri, String description) {
        return Triple.create(uri(milestoneUri), descriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createStateProperty(String milestoneUri, String state) {
        return Triple.create(uri(milestoneUri), stateProperty(), RdfUtils.stringLiteral(state));
    }

    public static Triple createUrlProperty(String milestoneUri, String url) {
        return Triple.create(uri(milestoneUri), urlProperty(), RdfUtils.stringLiteral(url));
    }

    public static Triple createCreatedAtProperty(String milestoneUri, LocalDateTime createdAt) {
        return Triple.create(uri(milestoneUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createUpdatedAtProperty(String milestoneUri, LocalDateTime updatedAt) {
        return Triple.create(uri(milestoneUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createClosedAtProperty(String milestoneUri, LocalDateTime closedAt) {
        return Triple.create(uri(milestoneUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAt));
    }

    public static Triple createDueDateProperty(String milestoneUri, LocalDateTime dueDate) {
        return Triple.create(uri(milestoneUri), dueDateProperty(), RdfUtils.dateTimeLiteral(dueDate));
    }

    // Relationship methods
    public static Triple createHasMilestoneProperty(String resourceUri, String milestoneUri) {
        return Triple.create(uri(resourceUri), hasMilestoneProperty(), uri(milestoneUri));
    }

    public static Triple createMilestoneOfProperty(String milestoneUri, String resourceUri) {
        return Triple.create(uri(milestoneUri), milestoneOfProperty(), uri(resourceUri));
    }

    // Legacy method compatibility
    public static Triple createMilestoneTitleProperty(String milestoneUri, String title) {
        return createTitleProperty(milestoneUri, title);
    }

    public static Triple createMilestoneDescriptionProperty(String milestoneUri, String description) {
        return createDescriptionProperty(milestoneUri, description);
    }

    public static Triple createMilestoneDueDateProperty(String milestoneUri, LocalDateTime dueDate) {
        return createDueDateProperty(milestoneUri, dueDate);
    }

    public static Triple createMilestoneStateProperty(String milestoneUri, String state) {
        return createStateProperty(milestoneUri, state);
    }
}