package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:WorkflowJob entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for workflow jobs that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformWorkflowJobUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform WorkflowJob Properties (from platform ontology)
    public static Node jobNameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "jobName");
    }

    public static Node jobStatusProperty() {
        return RdfUtils.uri(PLATFORM_NS + "jobStatus");
    }

    public static Node jobStartedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "jobStartedAt");
    }

    public static Node jobCompletedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "jobCompletedAt");
    }

    public static Node jobConclusionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "jobConclusion");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String jobUri) {
        return Triple.create(RdfUtils.uri(jobUri), rdfTypeProperty(), RdfUtils.uri("platform:WorkflowJob"));
    }

    public static Triple createJobNameProperty(String jobUri, String name) {
        return Triple.create(RdfUtils.uri(jobUri), jobNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createJobStatusProperty(String jobUri, String status) {
        return Triple.create(RdfUtils.uri(jobUri), jobStatusProperty(), RdfUtils.uri(PLATFORM_NS + status.toLowerCase()));
    }

    public static Triple createJobStartedAtProperty(String jobUri, LocalDateTime startedAt) {
        return Triple.create(RdfUtils.uri(jobUri), jobStartedAtProperty(), RdfUtils.dateTimeLiteral(startedAt));
    }

    public static Triple createJobCompletedAtProperty(String jobUri, LocalDateTime completedAt) {
        return Triple.create(RdfUtils.uri(jobUri), jobCompletedAtProperty(), RdfUtils.dateTimeLiteral(completedAt));
    }

    public static Triple createJobConclusionProperty(String jobUri, String conclusion) {
        return Triple.create(RdfUtils.uri(jobUri), jobConclusionProperty(), RdfUtils.uri(PLATFORM_NS + conclusion.toLowerCase()));
    }
}