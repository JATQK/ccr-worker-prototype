package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

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

    public static Node workflowRunProperty() { return RdfUtils.uri(GH_NS + "workflowRun"); }
    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowRunId"); }
    public static Node statusProperty() { return RdfUtils.uri(GH_NS + "workflowStatus"); }
    public static Node conclusionProperty() { return RdfUtils.uri(GH_NS + "workflowConclusion"); }
    public static Node eventProperty() { return RdfUtils.uri(GH_NS + "workflowEvent"); }
    public static Node commitShaProperty() { return RdfUtils.uri(GH_NS + "workflowCommitSha"); }
    public static Node createdAtProperty() { return RdfUtils.uri(GH_NS + "workflowCreatedAt"); }
    public static Node updatedAtProperty() { return RdfUtils.uri(GH_NS + "workflowUpdatedAt"); }

    // Triple creation
    public static Triple createWorkflowRunProperty(String issueUri, String runUri) {
        return Triple.create(RdfUtils.uri(issueUri), workflowRunProperty(), RdfUtils.uri(runUri));
    }

    // Override to create GitHub WorkflowRun type with platform inheritance
    public static Triple createWorkflowRunRdfTypeProperty(String runUri) {
        return Triple.create(RdfUtils.uri(runUri), rdfTypeProperty(), RdfUtils.uri("github:WorkflowRun"));
    }

    // v2.1: Add platform type for cross-platform queries
    public static Triple createWorkflowRunPlatformTypeProperty(String runUri) {
        return Triple.create(RdfUtils.uri(runUri), rdfTypeProperty(), RdfUtils.uri("platform:WorkflowExecution"));
    }

    public static Triple createWorkflowRunIdProperty(String runUri, long id) {
        return Triple.create(RdfUtils.uri(runUri), identifierProperty(), RdfUtils.longLiteral(id));
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
        // v2.1: Map GitHub workflow status to platform execution status with sameAs relationships
        String mappedStatus = mapGitHubStatusToPlatform(status.toString());
        return Triple.create(RdfUtils.uri(runUri), statusProperty(), RdfUtils.uri(GH_NS + mappedStatus));
    }

    public static Triple createWorkflowConclusionProperty(String runUri, GHWorkflowRun.Conclusion conclusion) {
        // v2.1: Map GitHub workflow conclusion to platform execution conclusion with sameAs relationships
        String mappedConclusion = mapGitHubConclusionToPlatform(conclusion.toString());
        return Triple.create(RdfUtils.uri(runUri), conclusionProperty(), RdfUtils.uri(GH_NS + mappedConclusion));
    }

    private static String mapGitHubStatusToPlatform(String status) {
        // v2.1: GitHub workflow status instances are sameAs platform execution status
        switch (status.toLowerCase()) {
            case "completed":
                return "completed"; // github:completed sameAs platform:execution_completed
            case "queued":
                return "queued"; // github:queued sameAs platform:execution_queued
            case "in_progress":
                return "in_progress"; // github:in_progress sameAs platform:running
            case "waiting":
                return "waiting"; // github:waiting sameAs platform:waiting
            default:
                return status.toLowerCase();
        }
    }

    private static String mapGitHubConclusionToPlatform(String conclusion) {
        // v2.1: GitHub workflow conclusion instances are sameAs platform execution conclusion
        switch (conclusion.toLowerCase()) {
            case "success":
                return "success"; // github:success sameAs platform:execution_success
            case "failure":
                return "failure"; // github:failure sameAs platform:execution_failure
            case "cancelled":
                return "cancelled"; // github:cancelled sameAs platform:execution_cancelled
            case "skipped":
                return "skipped"; // github:skipped sameAs platform:execution_skipped
            case "neutral":
                return "neutral"; // github:neutral (GitHub-specific)
            default:
                return conclusion.toLowerCase();
        }
    }

    public static Triple createWorkflowEventProperty(String runUri, String event) {
        return Triple.create(RdfUtils.uri(runUri), eventProperty(), RdfUtils.stringLiteral(event));
    }


    public static Triple createWorkflowCommitShaProperty(String runUri, String sha) {
        return Triple.create(RdfUtils.uri(runUri), commitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createWorkflowCreatedAtProperty(String runUri, LocalDateTime created) {
        return Triple.create(RdfUtils.uri(runUri), createdAtProperty(), RdfUtils.dateTimeLiteral(created));
    }

    public static Triple createWorkflowUpdatedAtProperty(String runUri, LocalDateTime updated) {
        return Triple.create(RdfUtils.uri(runUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updated));
    }

    public static Triple createWorkflowJobProperty(String runUri, String jobUri) {
        return RdfPlatformWorkflowUtils.createHasJobProperty(runUri, jobUri);
    }
}
