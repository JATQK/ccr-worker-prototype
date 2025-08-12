package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHWorkflowRun;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformWorkflowJobUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:WorkflowJob entities.
 * This class extends RdfPlatformWorkflowJobUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowJobUtils extends RdfPlatformWorkflowJobUtils {

    // Use platform properties from v2 ontology
    

    // Override to create GitHub WorkflowJob type
    public static Triple createWorkflowJobRdfTypeProperty(String jobUri) {
        return Triple.create(uri(jobUri), rdfTypeProperty(), uri("github:WorkflowJob"));
    }

    public static Triple createWorkflowJobIdProperty(String jobUri, long id) {
        return RdfPlatformWorkflowJobUtils.createJobIdProperty(jobUri, String.valueOf(id));
    }

    public static Triple createWorkflowJobIdProperty(String jobUri, String id) {
        return RdfPlatformWorkflowJobUtils.createJobIdProperty(jobUri, id);
    }

    // Use inherited platform methods for common properties
    public static Triple createWorkflowJobNameProperty(String jobUri, String name) {
        return RdfPlatformWorkflowJobUtils.createJobNameProperty(jobUri, name);
    }

    public static Triple createWorkflowJobStatusProperty(String jobUri, GHWorkflowRun.Status status) {
        String mappedStatus = mapGitHubStatusToPlatform(status.toString());
        return RdfPlatformWorkflowJobUtils.createJobStatusProperty(jobUri, mappedStatus);
    }

    public static Triple createWorkflowJobConclusionProperty(String jobUri, GHWorkflowRun.Conclusion conclusion) {
        String mappedConclusion = mapGitHubConclusionToPlatform(conclusion.toString());
        return RdfPlatformWorkflowJobUtils.createJobConclusionProperty(jobUri, mappedConclusion);
    }

    private static String mapGitHubStatusToPlatform(String status) {
        // Map GitHub workflow status to platform execution status values
        switch (status.toLowerCase()) {
            case "completed":
                return "completed";
            case "queued":
                return "queued";
            case "in_progress":
                return "in_progress";
            case "waiting":
                return "waiting";
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
                return "success";
            case "failure":
                return "failure";
            case "cancelled":
                return "cancelled";
            case "skipped":
                return "skipped";
            case "timed_out":
                return "timed_out";
            case "action_required":
                return "action_required";
            case "neutral":
                return "neutral";
            default:
                return conclusion.toLowerCase();
        }
    }

    public static Triple createWorkflowJobStartedAtProperty(String jobUri, LocalDateTime started) {
        return RdfPlatformWorkflowJobUtils.createJobStartedAtProperty(jobUri, started);
    }

    public static Triple createWorkflowJobCompletedAtProperty(String jobUri, LocalDateTime completed) {
        return RdfPlatformWorkflowJobUtils.createJobCompletedAtProperty(jobUri, completed);
    }

    public static Triple createWorkflowJobUrlProperty(String jobUri, String jobUrl) {
        return RdfPlatformWorkflowJobUtils.createJobUrlProperty(jobUri, jobUrl);
    }

    public static Triple createWorkflowJobApiUrlProperty(String jobUri, String apiUrl) {
        return Triple.create(uri(jobUri), uri("github:apiUrl"), RdfUtils.stringLiteral(apiUrl));
    }

    public static Triple createWorkflowJobStepProperty(String jobUri, String stepUri) {
        return RdfPlatformWorkflowJobUtils.createHasJobStepProperty(jobUri, stepUri);
    }
}
