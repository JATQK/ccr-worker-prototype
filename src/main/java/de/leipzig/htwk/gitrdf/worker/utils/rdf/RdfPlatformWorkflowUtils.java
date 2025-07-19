package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Workflow entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for workflows that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform Workflow Properties (from platform ontology)
    public static Node workflowNameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "workflowName");
    }

    public static Node workflowDescriptionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "workflowDescription");
    }

    public static Node workflowTriggerProperty() {
        return RdfUtils.uri(PLATFORM_NS + "workflowTrigger");
    }

    public static Node hasJobProperty() {
        return RdfUtils.uri(PLATFORM_NS + "hasJob");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String workflowUri) {
        return Triple.create(RdfUtils.uri(workflowUri), rdfTypeProperty(), RdfUtils.uri("platform:Workflow"));
    }

    public static Triple createWorkflowNameProperty(String workflowUri, String name) {
        return Triple.create(RdfUtils.uri(workflowUri), workflowNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowDescriptionProperty(String workflowUri, String description) {
        return Triple.create(RdfUtils.uri(workflowUri), workflowDescriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createWorkflowTriggerProperty(String workflowUri, String trigger) {
        return Triple.create(RdfUtils.uri(workflowUri), workflowTriggerProperty(), RdfUtils.stringLiteral(trigger));
    }

    public static Triple createHasJobProperty(String workflowUri, String jobUri) {
        return Triple.create(RdfUtils.uri(workflowUri), hasJobProperty(), RdfUtils.uri(jobUri));
    }
}