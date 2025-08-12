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
 * Platform-agnostic utility class for RDF operations on platform:WorkflowStep entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for workflow steps that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowStepUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform WorkflowStep Properties (from platform ontology)
    public static Node stepNameProperty() {
        return uri(PLATFORM_NS + "stepName");
    }

    public static Node stepNumberProperty() {
        return uri(PLATFORM_NS + "stepNumber");
    }

    public static Node stepStartedAtProperty() {
        return uri(PLATFORM_NS + "stepStartedAt");
    }

    public static Node stepCompletedAtProperty() {
        return uri(PLATFORM_NS + "stepCompletedAt");
    }

    public static Node stepConclusionProperty() {
        return uri(PLATFORM_NS + "stepConclusion");
    }

    public static Node apiUrlProperty() {
        return uri(PLATFORM_NS + "apiUrl");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String stepUri) {
        return Triple.create(uri(stepUri), rdfTypeProperty(), uri("platform:WorkflowStep"));
    }

    public static Triple createStepNameProperty(String stepUri, String name) {
        return Triple.create(uri(stepUri), stepNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createStepNumberProperty(String stepUri, int number) {
        return Triple.create(uri(stepUri), stepNumberProperty(), RdfUtils.integerLiteral(number));
    }

    public static Triple createStepStartedAtProperty(String stepUri, LocalDateTime startedAt) {
        return Triple.create(uri(stepUri), stepStartedAtProperty(), RdfUtils.dateTimeLiteral(startedAt));
    }

    public static Triple createStepCompletedAtProperty(String stepUri, LocalDateTime completedAt) {
        return Triple.create(uri(stepUri), stepCompletedAtProperty(), RdfUtils.dateTimeLiteral(completedAt));
    }

    public static Triple createStepConclusionProperty(String stepUri, String conclusion) {
        return Triple.create(uri(stepUri), stepConclusionProperty(), RdfUtils.stringLiteral(conclusion));
    }

    public static Triple createApiUrlProperty(String stepUri, String apiUrl) {
        return Triple.create(uri(stepUri), apiUrlProperty(), uri(apiUrl));
    }
}