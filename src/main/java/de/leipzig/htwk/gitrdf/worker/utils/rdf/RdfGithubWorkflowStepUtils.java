package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowStepUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }
    
    public static Node createWorkflowStepUri(String jobUri, Integer stepNumber) {
        return RdfUtils.uri(jobUri + "#step:" + stepNumber + ":1");
    }

    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowStepId"); }
    public static Node nameProperty() { return RdfUtils.uri(GH_NS + "workflowStepName"); }
    public static Node numberProperty() { return RdfUtils.uri(GH_NS + "workflowStepNumber"); }

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

}
