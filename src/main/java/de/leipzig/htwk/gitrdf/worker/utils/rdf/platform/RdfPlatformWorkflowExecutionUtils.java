package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:WorkflowExecution entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for workflow executions that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowExecutionUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform WorkflowExecution Properties (from platform ontology lines 364-417)
    public static Node executionIdProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionId");
    }

    public static Node executionNumberProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionNumber");
    }

    public static Node executionStatusProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionStatus");
    }

    public static Node executionConclusionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionConclusion");
    }

    public static Node triggerEventProperty() {
        return RdfUtils.uri(PLATFORM_NS + "triggerEvent");
    }

    public static Node triggerCommitProperty() {
        return RdfUtils.uri(PLATFORM_NS + "triggerCommit");
    }

    public static Node executionStartedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionStartedAt");
    }

    public static Node executionCompletedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionCompletedAt");
    }

    public static Node executionDurationProperty() {
        return RdfUtils.uri(PLATFORM_NS + "executionDuration");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String executionUri) {
        return Triple.create(RdfUtils.uri(executionUri), rdfTypeProperty(), RdfUtils.uri("platform:WorkflowExecution"));
    }

    public static Triple createExecutionIdProperty(String executionUri, String executionId) {
        return Triple.create(RdfUtils.uri(executionUri), executionIdProperty(), RdfUtils.stringLiteral(executionId));
    }

    // REMOVED: Overloaded method for long executionId - v2.1 uses only string IDs

    public static Triple createExecutionNumberProperty(String executionUri, int executionNumber) {
        return Triple.create(RdfUtils.uri(executionUri), executionNumberProperty(), RdfUtils.integerLiteral(executionNumber));
    }

    public static Triple createExecutionStatusProperty(String executionUri, String status) {
        // v2.1: Map to prefixed execution status instances for disambiguation
        String prefixedStatus = mapToExecutionStatus(status);
        return Triple.create(RdfUtils.uri(executionUri), executionStatusProperty(), RdfUtils.uri(PLATFORM_NS + prefixedStatus));
    }

    private static String mapToExecutionStatus(String status) {
        // v2.1: Map execution status values to prefixed instances
        switch (status.toLowerCase()) {
            case "completed":
                return "execution_completed";
            case "queued":
                return "execution_queued";
            case "pending":
                return "pending";
            case "running":
                return "running";
            case "waiting":
                return "waiting";
            default:
                return status.toLowerCase();
        }
    }

    public static Triple createExecutionConclusionProperty(String executionUri, String conclusion) {
        // v2.1: Map to prefixed execution conclusion instances (e.g., "success" → "execution_success")
        String prefixedConclusion = mapToPrefixedExecutionConclusion(conclusion);
        return Triple.create(RdfUtils.uri(executionUri), executionConclusionProperty(), RdfUtils.uri(PLATFORM_NS + prefixedConclusion));
    }

    private static String mapToPrefixedExecutionConclusion(String conclusion) {
        // v2.1: Map old conclusion instances to prefixed ones for disambiguation
        switch (conclusion.toLowerCase()) {
            case "success":
                return "execution_success";
            case "failure":
                return "execution_failure";
            case "cancelled":
                return "execution_cancelled";
            case "skipped":
                return "execution_skipped";
            default:
                return "execution_" + conclusion.toLowerCase();
        }
    }

    public static Triple createTriggerEventProperty(String executionUri, String triggerEvent) {
        return Triple.create(RdfUtils.uri(executionUri), triggerEventProperty(), RdfUtils.stringLiteral(triggerEvent));
    }

    public static Triple createTriggerCommitProperty(String executionUri, String triggerCommit) {
        return Triple.create(RdfUtils.uri(executionUri), triggerCommitProperty(), RdfUtils.stringLiteral(triggerCommit));
    }

    public static Triple createExecutionStartedAtProperty(String executionUri, LocalDateTime startedAt) {
        return Triple.create(RdfUtils.uri(executionUri), executionStartedAtProperty(), RdfUtils.dateTimeLiteral(startedAt));
    }

    public static Triple createExecutionCompletedAtProperty(String executionUri, LocalDateTime completedAt) {
        return Triple.create(RdfUtils.uri(executionUri), executionCompletedAtProperty(), RdfUtils.dateTimeLiteral(completedAt));
    }

    public static Triple createExecutionDurationProperty(String executionUri, long durationMillis) {
        // v2.1: Use xsd:duration instead of long for proper duration representation
        return Triple.create(RdfUtils.uri(executionUri), executionDurationProperty(), RdfUtils.durationLiteral(durationMillis));
    }
}