package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

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
        return uri("rdf:type");
    }

    // Platform Comment Properties (from platform ontology v2)
    public static Node bodyProperty() {
        return uri(PLATFORM_NS + "body");
    }

    public static Node authorProperty() {
        return uri(PLATFORM_NS + "author");
    }

    public static Node idProperty() {
        return uri(PLATFORM_NS + "id");
    }

    public static Node createdAtProperty() {
        return uri(PLATFORM_NS + "createdAt");
    }

    public static Node updatedAtProperty() {
        return uri(PLATFORM_NS + "updatedAt");
    }

    public static Node urlProperty() {
        return uri(PLATFORM_NS + "url");
    }

    public static Node hasReactionProperty() {
        return uri(PLATFORM_NS + "hasReaction");
    }

    public static Node commentOnProperty() {
        return uri(PLATFORM_NS + "commentOn");
    }


    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String commentUri) {
        return Triple.create(uri(commentUri), rdfTypeProperty(), uri("platform:Comment"));
    }

    public static Triple createBodyProperty(String commentUri, String body) {
        return Triple.create(uri(commentUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createAuthorProperty(String commentUri, String authorUri) {
        return Triple.create(uri(commentUri), authorProperty(), uri(authorUri));
    }

    public static Triple createIdProperty(String commentUri, String commentId) {
        return Triple.create(uri(commentUri), idProperty(), RdfUtils.stringLiteral(commentId));
    }

    public static Triple createCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(uri(commentUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createUpdatedAtProperty(String commentUri, LocalDateTime updatedAt) {
        return Triple.create(uri(commentUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createUrlProperty(String commentUri, String url) {
        return Triple.create(uri(commentUri), urlProperty(), RdfUtils.stringLiteral(url));
    }

    public static Triple createHasReactionProperty(String commentUri, String reactionUri) {
        return Triple.create(uri(commentUri), hasReactionProperty(), uri(reactionUri));
    }

    public static Triple createCommentOnProperty(String commentUri, String targetUri) {
        return Triple.create(uri(commentUri), commentOnProperty(), uri(targetUri));
    }

    // Legacy method compatibility
    public static Triple createCommentBodyProperty(String commentUri, String body) {
        return createBodyProperty(commentUri, body);
    }

    public static Triple createCommentAuthorProperty(String commentUri, String authorUri) {
        return createAuthorProperty(commentUri, authorUri);
    }

    public static Triple createCommentIdProperty(String commentUri, String commentId) {
        return createIdProperty(commentUri, commentId);
    }

    public static Triple createCommentIdProperty(String commentUri, long commentId) {
        return createIdProperty(commentUri, String.valueOf(commentId));
    }

    public static Triple createCommentedAtProperty(String commentUri, LocalDateTime commentedAt) {
        return createCreatedAtProperty(commentUri, commentedAt);
    }
}