package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowStepUtils {

    private static final String GITHUB_BASE = "https://github.com/";

    private static final String GITHUB_API_BASE = "https://api.github.com/";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    public static Node createWorkflowStepUrl(String repoString, Long jobNumber, Integer runAttempt) {
        // https://api.github.com/repos/dotnet/core/actions/jobs/42157363512#step-2
        String baseUri = repoString.replace(GITHUB_BASE, GITHUB_API_BASE + "repos/");
        return uri(baseUri + "actions/jobs/" + jobNumber + "#step-" + runAttempt);
    }
    
    public static Node createWorkflowStepUri(String jobUri, Integer runAttempt) {
        // https://github.com/dotnet/core/actions/runs/15620640086/job/44004361815#step:1:1
        return uri(jobUri + "#step:" + runAttempt + ":1");
    }
    // Use platform properties from v2 ontology
    public static Node idProperty() { return uri("platform:id"); }
    public static Node nameProperty() { return uri("platform:name"); }
    public static Node createdAtProperty() { return uri("platform:createdAt"); }
    public static Node updatedAtProperty() { return uri("platform:updatedAt"); }
    public static Node urlProperty() { return uri("platform:url"); }

    public static Triple createWorkflowStepRdfTypeProperty(String stepUri) {
        return Triple.create(uri(stepUri), rdfTypeProperty(), uri("github:WorkflowStep"));
    }


    public static Triple createWorkflowStepIdProperty(String stepUri, long id) {
        return Triple.create(uri(stepUri), idProperty(), RdfUtils.stringLiteral(String.valueOf(id)));
    }

    public static Triple createWorkflowStepNameProperty(String stepUri, String name) {
        return Triple.create(uri(stepUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createWorkflowStepStartedAtProperty(String stepUri, LocalDateTime startedAt) {
        return Triple.create(uri(stepUri), createdAtProperty(), RdfUtils.dateTimeLiteral(startedAt));
    }

    public static Triple createWorkflowStepCompletedAtProperty(String stepUri, LocalDateTime completedAt) {
        return Triple.create(uri(stepUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(completedAt));
    }

    public static Triple createWorkflowStepUrlProperty(String stepUri, String url) {
        return Triple.create(uri(stepUri), urlProperty(), RdfUtils.stringLiteral(url));
    }

    public static Triple createWorkflowStepJobUrlProperty(String stepUri, String jobUrl) {
        return Triple.create(uri(stepUri), uri("platform:jobUrl"), RdfUtils.stringLiteral(jobUrl));
    }

    public static Triple createWorkflowStepNumberProperty(String stepUri, int runAttempt) {
        return Triple.create(uri(stepUri), uri("platform:runAttempt"), RdfUtils.positiveIntegerLiteral(runAttempt));
    }

}
