package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Comment entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for comments that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformCommentUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform Comment Properties (from platform ontology)
    public static Node commentBodyProperty() {
        return RdfUtils.uri(PLATFORM_NS + "commentBody");
    }

    public static Node commentAuthorProperty() {
        return RdfUtils.uri(PLATFORM_NS + "commentAuthor");
    }

    public static Node commentIdProperty() {
        return RdfUtils.uri(PLATFORM_NS + "commentId");
    }

    public static Node commentedAtProperty() {
        return RdfUtils.uri(PLATFORM_NS + "commentedAt");
    }



    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String commentUri) {
        return Triple.create(RdfUtils.uri(commentUri), rdfTypeProperty(), RdfUtils.uri("platform:Comment"));
    }

    public static Triple createCommentBodyProperty(String commentUri, String body) {
        return Triple.create(RdfUtils.uri(commentUri), commentBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createCommentAuthorProperty(String commentUri, String authorUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentAuthorProperty(), RdfUtils.uri(authorUri));
    }

    public static Triple createCommentIdProperty(String commentUri, long commentId) {
        return Triple.create(RdfUtils.uri(commentUri), commentIdProperty(), RdfUtils.longLiteral(commentId));
    }

    public static Triple createCommentedAtProperty(String commentUri, LocalDateTime commentedAt) {
        return Triple.create(RdfUtils.uri(commentUri), commentedAtProperty(), RdfUtils.dateTimeLiteral(commentedAt));
    }


}