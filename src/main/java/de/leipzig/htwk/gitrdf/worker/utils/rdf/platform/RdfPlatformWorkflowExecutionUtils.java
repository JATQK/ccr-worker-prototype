package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowExecutionUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    public static Node executionIdProperty() {
        return uri(PLATFORM_NS + "executionId");
    }

    public static Node runIdProperty() {
        return uri(PLATFORM_NS + "runId");
    }

    public static Node executionNumberProperty() {
        return uri(PLATFORM_NS + "executionNumber");
    }

    public static Node executionStatusProperty() {
        return uri(PLATFORM_NS + "executionStatus");
    }

    public static Node executionConclusionProperty() {
        return uri(PLATFORM_NS + "executionConclusion");
    }

    public static Node triggerEventProperty() {
        return uri(PLATFORM_NS + "triggerEvent");
    }

    public static Node triggerCommitProperty() {
        return uri(PLATFORM_NS + "triggerCommit");
    }

    public static Node createdAtProperty() {
        return uri(PLATFORM_NS + "createdAt");
    }

    public static Node executionStartedAtProperty() {
        return uri(PLATFORM_NS + "executionStartedAt");
    }

    public static Node executionCompletedAtProperty() {
        return uri(PLATFORM_NS + "executionCompletedAt");
    }

    public static Node executionDurationProperty() {
        return uri(PLATFORM_NS + "executionDuration");
    }

    public static Node executionOfProperty() {
        return uri(PLATFORM_NS + "executionOf");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String executionUri) {
        return Triple.create(uri(executionUri), rdfTypeProperty(), uri("platform:WorkflowExecution"));
    }

    public static Triple createExecutionIdProperty(String executionUri, String executionId) {
        return Triple.create(uri(executionUri), executionIdProperty(), RdfUtils.stringLiteral(executionId));
    }

    public static Triple createRunIdProperty(String executionUri, long runId) {
        return Triple.create(uri(executionUri), runIdProperty(), RdfUtils.longLiteral(runId));
    }

    // REMOVED: Overloaded method for long executionId - v2.1 uses only string IDs

    public static Triple createExecutionNumberProperty(String executionUri, int executionNumber) {
        return Triple.create(uri(executionUri), executionNumberProperty(), RdfUtils.integerLiteral(executionNumber));
    }

    public static Triple createExecutionStatusProperty(String executionUri, String status) {
        String prefixedStatus = mapToExecutionStatus(status);
        return Triple.create(uri(executionUri), executionStatusProperty(), uri(PLATFORM_NS + prefixedStatus));
    }

    private static String mapToExecutionStatus(String status) {
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
        return Triple.create(uri(executionUri), executionConclusionProperty(), uri(PLATFORM_NS + prefixedConclusion));
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
        // v2.1: Map GitHub-specific trigger events to standardized platform events
        String standardizedEvent = mapToStandardizedTriggerEvent(triggerEvent);
        return Triple.create(uri(executionUri), triggerEventProperty(), RdfUtils.stringLiteral(standardizedEvent));
    }

    private static String mapToStandardizedTriggerEvent(String githubEvent) {
        if (githubEvent == null) return "unknown";
        
        String event = githubEvent.toLowerCase();
        switch (event) {
            case "pull_request_target": return "pull_request";
            case "workflow_dispatch": return "manual";
            case "repository_dispatch": return "api_trigger";
            case "check_run":
            case "check_suite": return "check_trigger";
            case "deployment_status": return "deployment";
            case "create":
            case "delete": return "ref_change";
            case "gollum": return "wiki_change";
            case "issue_comment":
            case "issues": return "issue_activity";
            case "project":
            case "project_card":
            case "project_column": return "project_activity";
            case "public": return "visibility_change";
            case "registry_package": return "package_activity";
            case "star":
            case "watch": return "repository_activity";
            default: return event;
        }
    }

    public static Triple createTriggerCommitProperty(String executionUri, String triggerCommit) {
        return Triple.create(uri(executionUri), triggerCommitProperty(), RdfUtils.stringLiteral(triggerCommit));
    }

    public static Triple createCreatedAtProperty(String executionUri, LocalDateTime createdAt) {
        return Triple.create(uri(executionUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createExecutionStartedAtProperty(String executionUri, LocalDateTime startedAt) {
        return Triple.create(uri(executionUri), executionStartedAtProperty(), RdfUtils.dateTimeLiteral(startedAt));
    }

    public static Triple createExecutionCompletedAtProperty(String executionUri, LocalDateTime completedAt) {
        return Triple.create(uri(executionUri), executionCompletedAtProperty(), RdfUtils.dateTimeLiteral(completedAt));
    }

    public static Triple createExecutionDurationProperty(String executionUri, long durationMillis) {
        // v2.1: Use xsd:duration instead of long for proper duration representation
        return Triple.create(uri(executionUri), executionDurationProperty(), RdfUtils.durationLiteral(durationMillis));
    }

    public static Triple createExecutionOfProperty(String executionUri, String targetUri) {
        return Triple.create(uri(executionUri), executionOfProperty(), uri(targetUri));
    }
}