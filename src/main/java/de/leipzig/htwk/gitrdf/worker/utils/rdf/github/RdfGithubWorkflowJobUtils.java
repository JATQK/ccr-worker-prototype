package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.kohsuke.github.GHWorkflowRun;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformWorkflowJobUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:GithubWorkflowJob entities.
 * This class extends RdfPlatformWorkflowJobUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubWorkflowJobUtils extends RdfPlatformWorkflowJobUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // GitHub-specific workflow job properties
    public static Node identifierProperty() { return RdfUtils.uri(GH_NS + "workflowJobId"); }
    public static Node jobUrlProperty() { return RdfUtils.uri(GH_NS + "workflowJobUrl"); }
    public static Node hasStepProperty() { return RdfUtils.uri(GH_NS + "hasStep"); }
    

    // Override to create GitHub WorkflowJob type
    public static Triple createWorkflowJobRdfTypeProperty(String jobUri) {
        return Triple.create(RdfUtils.uri(jobUri), rdfTypeProperty(), RdfUtils.uri("github:GithubWorkflowJob"));
    }

    public static Triple createWorkflowJobIdProperty(String jobUri, long id) {
        return Triple.create(RdfUtils.uri(jobUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    // Use inherited platform methods for common properties
    public static Triple createWorkflowJobNameProperty(String jobUri, String name) {
        return createJobNameProperty(jobUri, name);
    }

    public static Triple createWorkflowJobStatusProperty(String jobUri, GHWorkflowRun.Status status) {
        return createJobStatusProperty(jobUri, status.toString());
    }

    public static Triple createWorkflowJobConclusionProperty(String jobUri, GHWorkflowRun.Conclusion conclusion) {
        return createJobConclusionProperty(jobUri, conclusion.toString());
    }

    public static Triple createWorkflowJobStartedAtProperty(String jobUri, LocalDateTime started) {
        return createJobStartedAtProperty(jobUri, started);
    }

    public static Triple createWorkflowJobCompletedAtProperty(String jobUri, LocalDateTime completed) {
        return createJobCompletedAtProperty(jobUri, completed);
    }

    public static Triple createWorkflowJobStepProperty(String jobUri, String stepUri) {
        return Triple.create(RdfUtils.uri(jobUri), hasStepProperty(), RdfUtils.uri(stepUri));
    }
    public static Triple createWorkflowJobUrlProperty(String jobUri, String jobUrl) {
        return Triple.create(RdfUtils.uri(jobUri), jobUrlProperty(), RdfUtils.uri(jobUrl));
    }
}
