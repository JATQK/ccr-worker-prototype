package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHWorkflowRun;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowStepUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node numberProperty() { return RdfUtils.uri(GH_NS + "workflowStepNumber"); }
    public static Node nameProperty() { return RdfUtils.uri(GH_NS + "workflowStepName"); }
    public static Node statusProperty() { return RdfUtils.uri(GH_NS + "workflowStepStatus"); }
    public static Node conclusionProperty() { return RdfUtils.uri(GH_NS + "workflowStepConclusion"); }
    public static Node startedAtProperty() { return RdfUtils.uri(GH_NS + "workflowStepStartedAt"); }
    public static Node completedAtProperty() { return RdfUtils.uri(GH_NS + "workflowStepCompletedAt"); }
    public static Node nextStepProperty() { return RdfUtils.uri(GH_NS + "workflowNextStep"); }

    // Triple creation
    public static Triple createWorkflowStepRdfTypeProperty(String stepUri) {
        return Triple.create(RdfUtils.uri(stepUri), rdfTypeProperty(), RdfUtils.uri("github:WorkflowStep"));
    }

    public static Triple createWorkflowStepNumberProperty(String stepUri, int number) {
        return Triple.create(RdfUtils.uri(stepUri), numberProperty(), RdfUtils.integerLiteral(number));
    }

    public static Triple createWorkflowStepNameProperty(String stepUri, String name) {
        return Triple.create(RdfUtils.uri(stepUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowStepStatusProperty(String stepUri, GHWorkflowRun.Status status) {
        return Triple.create(RdfUtils.uri(stepUri), statusProperty(), RdfUtils.uri(GH_NS + status));
    }

    public static Triple createWorkflowStepConclusionProperty(String stepUri, GHWorkflowRun.Conclusion conclusion) {
        return Triple.create(RdfUtils.uri(stepUri), conclusionProperty(), RdfUtils.uri(GH_NS + conclusion));
    }

    public static Triple createWorkflowStepStartedAtProperty(String stepUri, LocalDateTime started) {
        return Triple.create(RdfUtils.uri(stepUri), startedAtProperty(), RdfUtils.dateTimeLiteral(started));
    }

    public static Triple createWorkflowStepCompletedAtProperty(String stepUri, LocalDateTime completed) {
        return Triple.create(RdfUtils.uri(stepUri), completedAtProperty(), RdfUtils.dateTimeLiteral(completed));
    }

    public static Triple createWorkflowNextStepProperty(String stepUri, String nextStepUri) {
        return Triple.create(RdfUtils.uri(stepUri), nextStepProperty(), RdfUtils.uri(nextStepUri));
    }
}
