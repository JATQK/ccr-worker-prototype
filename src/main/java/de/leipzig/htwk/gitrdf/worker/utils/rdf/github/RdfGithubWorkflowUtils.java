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
    public static Node commitShaProperty() { return uri(GH_NS + "workflowCommitSha"); }
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
    
    // TODO: Future catching of invalid states
    private static String mapGitHubStatusToPlatform(String status) {
        // Map GitHub workflow status to platform execution status values
        switch (status.toLowerCase()) {
            case "completed":
            case "queued":
            case "in_progress":
            case "waiting":
            case "requested":
                return status.toLowerCase();
            default:
                return status.toLowerCase();
        }
    }

    // TODO: Future catching of invalid states
    private static String mapGitHubConclusionToPlatform(String conclusion) {
        // Map GitHub workflow conclusion to platform execution conclusion values
        switch (conclusion.toLowerCase()) {
            case "success":
            case "failure":
            case "cancelled":
            case "skipped":
            case "timed_out":
            case "action_required":
            case "neutral":
                return conclusion.toLowerCase(); // Use platform value directly
            default:
                return conclusion.toLowerCase();
        }
    }

    public static Triple createWorkflowCommitShaProperty(String runUri, String sha) {
        return Triple.create(uri(runUri), commitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createWorkflowUpdatedAtProperty(String runUri, LocalDateTime updated) {
        return Triple.create(uri(runUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updated));
    }

    public static Triple createWorkflowJobProperty(String runUri, String jobUri) {
        return RdfPlatformWorkflowUtils.createHasJobProperty(runUri, jobUri);
    }

}
