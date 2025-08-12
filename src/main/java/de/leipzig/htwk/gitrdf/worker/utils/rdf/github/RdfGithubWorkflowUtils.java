package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHWorkflowRun;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformWorkflowUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:WorkflowRun entities.
 * This class extends RdfPlatformWorkflowUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowUtils extends RdfPlatformWorkflowUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // GitHub-specific workflow run properties

    public static Node workflowRunProperty() { return uri(GH_NS + "workflowRun"); }
    public static Node identifierProperty() { return uri(GH_NS + "workflowRunId"); }
    public static Node runAttemptProperty() { return uri(GH_NS + "runAttempt"); }
    public static Node statusProperty() { return uri("platform:executionStatus"); }
    public static Node conclusionProperty() { return uri("platform:executionConclusion"); }
    public static Node eventProperty() { return uri(GH_NS + "workflowEvent"); }
    public static Node commitShaProperty() { return uri(GH_NS + "workflowCommitSha"); }
    public static Node createdAtProperty() { return uri(GH_NS + "workflowCreatedAt"); }
    
    // New generalized properties (subproperties of platform equivalents)
    public static Node triggerEventProperty() { return uri(GH_NS + "triggerEvent"); }
    public static Node triggerCommitProperty() { return uri(GH_NS + "triggerCommit"); }
    public static Node executionCreatedAtProperty() { return uri(GH_NS + "executionCreatedAt"); }
    public static Node updatedAtProperty() { return uri("platform:updatedAt"); }

    // Triple creation
    public static Triple createWorkflowRunProperty(String issueUri, String runUri) {
        return Triple.create(uri(issueUri), workflowRunProperty(), uri(runUri));
    }

    // Override to create GitHub WorkflowRun type with platform inheritance
    public static Triple createWorkflowRunRdfTypeProperty(String runUri) {
        return Triple.create(uri(runUri), rdfTypeProperty(), uri("github:WorkflowRun"));
    }


    public static Triple createWorkflowRunIdProperty(String runUri, long id) {
        return Triple.create(uri(runUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createRunAttemptProperty(String runUri, long attempt) {
        return Triple.create(uri(runUri), runAttemptProperty(), RdfUtils.positiveIntegerLiteral(attempt));
    }

    // Use inherited platform method for workflow name
    public static Triple createWorkflowNameProperty(String runUri, String name) {
        return RdfPlatformWorkflowUtils.createWorkflowNameProperty(runUri, name);
    }

    public static Triple createWorkflowDescriptionProperty(String runUri, String description) {
        return RdfPlatformWorkflowUtils.createWorkflowDescriptionProperty(runUri, description);
    }

    public static Triple createWorkflowTriggerProperty(String runUri, String trigger) {
        return RdfPlatformWorkflowUtils.createWorkflowTriggerProperty(runUri, trigger);
    }

    public static Triple createWorkflowStatusProperty(String runUri, GHWorkflowRun.Status status) {
        // Use platform execution status values directly
        String mappedStatus = mapGitHubStatusToPlatform(status.toString());
        return Triple.create(uri(runUri), statusProperty(), uri("platform:" + mappedStatus));
    }

    public static Triple createWorkflowConclusionProperty(String runUri, GHWorkflowRun.Conclusion conclusion) {
        // Use platform execution conclusion values directly
        String mappedConclusion = mapGitHubConclusionToPlatform(conclusion.toString());
        return Triple.create(uri(runUri), conclusionProperty(), uri("platform:" + mappedConclusion));
    }

    private static String mapGitHubStatusToPlatform(String status) {
        // Map GitHub workflow status to platform execution status values
        switch (status.toLowerCase()) {
            case "completed":
                return "completed"; // Use platform:completed
            case "queued":
                return "queued"; // Use platform:queued
            case "in_progress":
                return "in_progress"; // Use platform:in_progress
            case "waiting":
                return "waiting"; // Use platform:waiting
            case "requested":
                return "requested"; // GitHub-specific, but inherits from platform:ExecutionStatus
            default:
                return status.toLowerCase();
        }
    }

    private static String mapGitHubConclusionToPlatform(String conclusion) {
        // Map GitHub workflow conclusion to platform execution conclusion values
        switch (conclusion.toLowerCase()) {
            case "success":
                return "success"; // Use platform:success
            case "failure":
                return "failure"; // Use platform:failure
            case "cancelled":
                return "cancelled"; // Use platform:cancelled
            case "skipped":
                return "skipped"; // Use platform:skipped
            case "timed_out":
                return "timed_out"; // Use platform:timed_out
            case "action_required":
                return "action_required"; // Use platform:action_required
            case "neutral":
                return "neutral"; // Map to platform value (no longer GitHub-specific)
            default:
                return conclusion.toLowerCase();
        }
    }

    public static Triple createWorkflowEventProperty(String runUri, String event) {
        return Triple.create(uri(runUri), eventProperty(), RdfUtils.stringLiteral(event));
    }


    public static Triple createWorkflowCommitShaProperty(String runUri, String sha) {
        return Triple.create(uri(runUri), commitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createWorkflowCreatedAtProperty(String runUri, LocalDateTime created) {
        return Triple.create(uri(runUri), createdAtProperty(), RdfUtils.dateTimeLiteral(created));
    }

    public static Triple createWorkflowUpdatedAtProperty(String runUri, LocalDateTime updated) {
        return Triple.create(uri(runUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updated));
    }

    public static Triple createWorkflowJobProperty(String runUri, String jobUri) {
        return RdfPlatformWorkflowUtils.createHasJobProperty(runUri, jobUri);
    }
    
    // New generalized property creation methods
    public static Triple createTriggerEventProperty(String runUri, String triggerEvent) {
        return Triple.create(uri(runUri), triggerEventProperty(), RdfUtils.stringLiteral(triggerEvent));
    }
    
    public static Triple createTriggerCommitProperty(String runUri, String triggerCommit) {
        return Triple.create(uri(runUri), triggerCommitProperty(), RdfUtils.stringLiteral(triggerCommit));
    }
    
    public static Triple createExecutionCreatedAtProperty(String runUri, LocalDateTime executionCreatedAt) {
        return Triple.create(uri(runUri), executionCreatedAtProperty(), RdfUtils.dateTimeLiteral(executionCreatedAt));
    }
}
