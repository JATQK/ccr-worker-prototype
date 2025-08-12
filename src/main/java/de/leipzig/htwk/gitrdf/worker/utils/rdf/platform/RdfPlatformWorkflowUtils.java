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
 * Platform-agnostic utility class for RDF operations on platform:Workflow entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for workflows that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform Workflow Properties (from platform ontology)
    public static Node workflowNameProperty() {
        return uri(PLATFORM_NS + "workflowName");
    }

    public static Node workflowDescriptionProperty() {
        return uri(PLATFORM_NS + "workflowDescription");
    }

    public static Node workflowTriggerProperty() {
        return uri(PLATFORM_NS + "workflowTrigger");
    }

    public static Node hasJobProperty() {
        return uri(PLATFORM_NS + "hasJob");
    }

    // Add missing platform properties for workflow execution
    public static Node executionStatusProperty() {
        return uri(PLATFORM_NS + "executionStatus");
    }

    public static Node executionConclusionProperty() {
        return uri(PLATFORM_NS + "executionConclusion");
    }

    public static Node updatedAtProperty() {
        return uri(PLATFORM_NS + "updatedAt");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String workflowUri) {
        return Triple.create(uri(workflowUri), rdfTypeProperty(), uri("platform:Workflow"));
    }

    public static Triple createWorkflowNameProperty(String workflowUri, String name) {
        return Triple.create(uri(workflowUri), workflowNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowDescriptionProperty(String workflowUri, String description) {
        return Triple.create(uri(workflowUri), workflowDescriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createWorkflowTriggerProperty(String workflowUri, String trigger) {
        return Triple.create(uri(workflowUri), workflowTriggerProperty(), RdfUtils.stringLiteral(trigger));
    }

    public static Triple createHasJobProperty(String workflowUri, String jobUri) {
        return Triple.create(uri(workflowUri), hasJobProperty(), uri(jobUri));
    }

    public static Triple createExecutionStatusProperty(String workflowUri, String status) {
        return Triple.create(uri(workflowUri), executionStatusProperty(), uri("platform:" + status.toLowerCase()));
    }

    public static Triple createExecutionConclusionProperty(String workflowUri, String conclusion) {
        return Triple.create(uri(workflowUri), executionConclusionProperty(), uri("platform:" + conclusion.toLowerCase()));
    }

    public static Triple createUpdatedAtProperty(String workflowUri, LocalDateTime updatedAt) {
        return Triple.create(uri(workflowUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }
}