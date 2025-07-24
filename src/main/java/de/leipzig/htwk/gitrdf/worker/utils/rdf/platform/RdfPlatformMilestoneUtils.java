package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

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
        return RdfUtils.uri("rdf:type");
    }

    // Platform Milestone Properties (from platform ontology)
    public static Node milestoneTitleProperty() {
        return RdfUtils.uri(PLATFORM_NS + "milestoneTitle");
    }

    public static Node milestoneDescriptionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "milestoneDescription");
    }

    public static Node milestoneDueDateProperty() {
        return RdfUtils.uri(PLATFORM_NS + "milestoneDueDate");
    }

    public static Node milestoneStateProperty() {
        return RdfUtils.uri(PLATFORM_NS + "milestoneState");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String milestoneUri) {
        return Triple.create(RdfUtils.uri(milestoneUri), rdfTypeProperty(), RdfUtils.uri("platform:Milestone"));
    }

    public static Triple createMilestoneTitleProperty(String milestoneUri, String title) {
        return Triple.create(RdfUtils.uri(milestoneUri), milestoneTitleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createMilestoneDescriptionProperty(String milestoneUri, String description) {
        return Triple.create(RdfUtils.uri(milestoneUri), milestoneDescriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createMilestoneDueDateProperty(String milestoneUri, LocalDateTime dueDate) {
        return Triple.create(RdfUtils.uri(milestoneUri), milestoneDueDateProperty(), RdfUtils.dateTimeLiteral(dueDate));
    }

    public static Triple createMilestoneStateProperty(String milestoneUri, String state) {
        // v2.1: Map to prefixed milestone state instances for disambiguation
        String prefixedState = mapToMilestoneState(state);
        return Triple.create(RdfUtils.uri(milestoneUri), milestoneStateProperty(), RdfUtils.uri(PLATFORM_NS + prefixedState));
    }

    private static String mapToMilestoneState(String state) {
        // v2.1: Map milestone states to prefixed instances
        switch (state.toLowerCase()) {
            case "open":
                return "milestone_open";
            case "closed":
                return "milestone_closed";
            default:
                return "milestone_" + state.toLowerCase();
        }
    }
}