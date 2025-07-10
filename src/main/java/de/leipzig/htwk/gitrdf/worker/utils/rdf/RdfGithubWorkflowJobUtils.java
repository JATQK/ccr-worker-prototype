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
public final class RdfGithubWorkflowJobUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PF_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowJobId"); }
    public static Node nameProperty() { return RdfUtils.uri(PF_NS + "jobName"); }
    public static Node statusProperty() { return RdfUtils.uri(PF_NS + "jobStatus"); }
    public static Node conclusionProperty() { return RdfUtils.uri(PF_NS + "jobConclusion"); }
    public static Node startedAtProperty() { return RdfUtils.uri(PF_NS + "jobStartedAt"); }
    public static Node completedAtProperty() { return RdfUtils.uri(PF_NS + "jobCompletedAt"); }
    public static Node jobUrlProperty() { return RdfUtils.uri(GH_NS + "workflowJobUrl"); }
    public static Node stepProperty() { return RdfUtils.uri(GH_NS + "workflowStep"); }
    

    // Triple creation
    public static Triple createWorkflowJobRdfTypeProperty(String jobUri) {
        return Triple.create(RdfUtils.uri(jobUri), rdfTypeProperty(), RdfUtils.uri("github:WorkflowJob"));
    }

    public static Triple createWorkflowJobIdProperty(String jobUri, long id) {
        return Triple.create(RdfUtils.uri(jobUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createWorkflowJobNameProperty(String jobUri, String name) {
        return Triple.create(RdfUtils.uri(jobUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowJobStatusProperty(String jobUri, GHWorkflowRun.Status status) {
        return Triple.create(RdfUtils.uri(jobUri), statusProperty(), RdfUtils.uri(PF_NS + status));
    }

    public static Triple createWorkflowJobConclusionProperty(String jobUri, GHWorkflowRun.Conclusion conclusion) {
        return Triple.create(RdfUtils.uri(jobUri), conclusionProperty(), RdfUtils.uri(PF_NS + conclusion));
    }

    public static Triple createWorkflowJobStartedAtProperty(String jobUri, LocalDateTime started) {
        return Triple.create(RdfUtils.uri(jobUri), startedAtProperty(), RdfUtils.dateTimeLiteral(started));
    }

    public static Triple createWorkflowJobCompletedAtProperty(String jobUri, LocalDateTime completed) {
        return Triple.create(RdfUtils.uri(jobUri), completedAtProperty(), RdfUtils.dateTimeLiteral(completed));
    }

    public static Triple createWorkflowJobStepProperty(String jobUri, String stepUri) {
        return Triple.create(RdfUtils.uri(jobUri), stepProperty(), RdfUtils.uri(stepUri));
    }
    public static Triple createWorkflowJobUrlProperty(String jobUri, String jobUrl) {
        return Triple.create(RdfUtils.uri(jobUri), jobUrlProperty(), RdfUtils.uri(jobUrl));
    }
}
