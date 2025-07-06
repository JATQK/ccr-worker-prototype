package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowStepUtils {

    private static final String GITHUB_BASE = "https://github.com/";

    private static final String GITHUB_API_BASE = "https://api.github.com/";

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node createWorkflowStepUrl(String repoString, Long jobNumber, Integer stepNumber) {
        // https://api.github.com/repos/dotnet/core/actions/jobs/42157363512#step-2
        String baseUri = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        return RdfUtils.uri(baseUri + "actions/jobs/" + jobNumber + "#step-" + stepNumber);
    }
    
    public static Node createWorkflowStepUri(String jobUri, Integer stepNumber) {
        // https://github.com/dotnet/core/actions/runs/15620640086/job/44004361815#step:1:1
        return RdfUtils.uri(jobUri + "#step:" + stepNumber + ":1");
    }
    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "id"); }
    public static Node nameProperty() { return RdfUtils.uri(GH_NS + "name"); }

    public static Node numberProperty() {
        return RdfUtils.uri(GH_NS + "number");
    }
    
    public static Node startedAtProperty() {
        return RdfUtils.uri(GH_NS + "startedAt");
    }

    public static Node completedAtProperty() {
        return RdfUtils.uri(GH_NS + "completedAt");
    }

    public static Node urlProperty() {
        return RdfUtils.uri(GH_NS + "url");
    }

    public static Triple createWorkflowStepRdfTypeProperty(String stepUri) {
        return Triple.create(RdfUtils.uri(stepUri), rdfTypeProperty(), RdfUtils.uri("github:WorkflowStep"));
    }

    public static Triple createWorkflowStepIdProperty(String stepUri, long id) {
        return Triple.create(RdfUtils.uri(stepUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createWorkflowStepNameProperty(String stepUri, String name) {
        return Triple.create(RdfUtils.uri(stepUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowStepNumberProperty(String stepUri, int number) {
        return Triple.create(RdfUtils.uri(stepUri), numberProperty(), RdfUtils.integerLiteral(number));
    }

    public static Triple createWorkflowStepStartedAtProperty(String stepUri, String startedAt) {
        return Triple.create(RdfUtils.uri(stepUri), startedAtProperty(), RdfUtils.stringLiteral(startedAt));
    }

    public static Triple createWorkflowStepCompletedAtProperty(String stepUri, String completedAt) {
        return Triple.create(RdfUtils.uri(stepUri), completedAtProperty(), RdfUtils.stringLiteral(completedAt));
    }

    public static Triple createWorkflowStepJobUrlProperty(String stepUri, String jobUri) {
        return Triple.create(RdfUtils.uri(stepUri), urlProperty(), RdfUtils.uri(jobUri));
    }

}
