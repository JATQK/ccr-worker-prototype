package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHWorkflowRun;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PF_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform based properties
    public static Node nameProperty() { return RdfUtils.uri(PF_NS + "workflowName"); }
    public static Node descriptionProperty() { return RdfUtils.uri(PF_NS + "workflowDescription"); }
    public static Node triggerProperty() { return RdfUtils.uri(PF_NS + "workflowTrigger"); }
    public static Node jobProperty() { return RdfUtils.uri(PF_NS + "hasJob"); }

    // GitHub specific run related properties
    public static Node workflowRunProperty() { return RdfUtils.uri(GH_NS + "workflowRun"); }
    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowRunId"); }
    public static Node statusProperty() { return RdfUtils.uri(GH_NS + "workflowStatus"); }
    public static Node conclusionProperty() { return RdfUtils.uri(GH_NS + "workflowConclusion"); }
    public static Node eventProperty() { return RdfUtils.uri(GH_NS + "workflowEvent"); }
    public static Node runNumberProperty() { return RdfUtils.uri(GH_NS + "workflowRunNumber"); }
    public static Node commitShaProperty() { return RdfUtils.uri(GH_NS + "workflowCommitSha"); }
    public static Node createdAtProperty() { return RdfUtils.uri(GH_NS + "workflowCreatedAt"); }
    public static Node updatedAtProperty() { return RdfUtils.uri(GH_NS + "workflowUpdatedAt"); }

    // Triple creation
    public static Triple createWorkflowRunProperty(String issueUri, String runUri) {
        return Triple.create(RdfUtils.uri(issueUri), workflowRunProperty(), RdfUtils.uri(runUri));
    }

    public static Triple createWorkflowRunRdfTypeProperty(String runUri) {
        return Triple.create(RdfUtils.uri(runUri), rdfTypeProperty(), RdfUtils.uri("github:WorkflowRun"));
    }

    public static Triple createWorkflowRunIdProperty(String runUri, long id) {
        return Triple.create(RdfUtils.uri(runUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createWorkflowNameProperty(String runUri, String name) {
        return Triple.create(RdfUtils.uri(runUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowDescriptionProperty(String runUri, String description) {
        return Triple.create(RdfUtils.uri(runUri), descriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createWorkflowTriggerProperty(String runUri, String trigger) {
        return Triple.create(RdfUtils.uri(runUri), triggerProperty(), RdfUtils.stringLiteral(trigger));
    }

    public static Triple createWorkflowStatusProperty(String runUri, GHWorkflowRun.Status status) {
        return Triple.create(RdfUtils.uri(runUri), statusProperty(), RdfUtils.uri(GH_NS + status));
    }

    public static Triple createWorkflowConclusionProperty(String runUri, GHWorkflowRun.Conclusion conclusion) {
        return Triple.create(RdfUtils.uri(runUri), conclusionProperty(), RdfUtils.uri(GH_NS + conclusion));
    }

    public static Triple createWorkflowEventProperty(String runUri, String event) {
        return Triple.create(RdfUtils.uri(runUri), eventProperty(), RdfUtils.uri(GH_NS + event));
    }

    public static Triple createWorkflowRunNumberProperty(String runUri, long runNumber) {
        return Triple.create(RdfUtils.uri(runUri), runNumberProperty(), RdfUtils.longLiteral(runNumber));
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
        return Triple.create(RdfUtils.uri(runUri), jobProperty(), RdfUtils.uri(jobUri));
    }
}
