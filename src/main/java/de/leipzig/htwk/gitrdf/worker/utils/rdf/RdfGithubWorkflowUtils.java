package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHWorkflowRun;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node workflowRunProperty() { return RdfUtils.uri(GH_NS + "workflowRun"); }
    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowRunId"); }
    public static Node nameProperty() { return RdfUtils.uri(GH_NS + "workflowName"); }
    public static Node statusProperty() { return RdfUtils.uri(GH_NS + "workflowStatus"); }
    public static Node conclusionProperty() { return RdfUtils.uri(GH_NS + "workflowConclusion"); }
    public static Node eventProperty() { return RdfUtils.uri(GH_NS + "workflowEvent"); }
    public static Node runNumberProperty() { return RdfUtils.uri(GH_NS + "workflowRunNumber"); }
    public static Node commitShaProperty() { return RdfUtils.uri(GH_NS + "workflowCommitSha"); }
    public static Node htmlUrlProperty() { return RdfUtils.uri(GH_NS + "workflowHtmlUrl"); }
    public static Node createdAtProperty() { return RdfUtils.uri(GH_NS + "workflowCreatedAt"); }
    public static Node updatedAtProperty() { return RdfUtils.uri(GH_NS + "workflowUpdatedAt"); }
    public static Node jobProperty() { return RdfUtils.uri(GH_NS + "workflowJob"); }

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

    public static Triple createWorkflowStatusProperty(String runUri, GHWorkflowRun.Status status) {
        return Triple.create(RdfUtils.uri(runUri), statusProperty(), RdfUtils.uri(GH_NS + status));
    }

    public static Triple createWorkflowConclusionProperty(String runUri, GHWorkflowRun.Conclusion conclusion) {
        return Triple.create(RdfUtils.uri(runUri), conclusionProperty(), RdfUtils.uri(GH_NS + conclusion));
    }

    public static Triple createWorkflowEventProperty(String runUri, GHEvent event) {
        return Triple.create(RdfUtils.uri(runUri), eventProperty(), RdfUtils.uri(GH_NS + event));
    }

    public static Triple createWorkflowRunNumberProperty(String runUri, long runNumber) {
        return Triple.create(RdfUtils.uri(runUri), runNumberProperty(), RdfUtils.longLiteral(runNumber));
    }

    public static Triple createWorkflowCommitShaProperty(String runUri, String sha) {
        return Triple.create(RdfUtils.uri(runUri), commitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    public static Triple createWorkflowHtmlUrlProperty(String runUri, String url) {
        return Triple.create(RdfUtils.uri(runUri), htmlUrlProperty(), RdfUtils.uri(url));
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
