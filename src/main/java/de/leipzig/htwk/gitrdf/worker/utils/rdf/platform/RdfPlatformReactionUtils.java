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
 * Platform-agnostic utility class for RDF operations on platform:Reaction entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for reactions that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformReactionUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform Reaction Properties (from platform ontology v2)
    public static Node idProperty() {
        return uri(PLATFORM_NS + "id");
    }

    public static Node reactionTypeProperty() {
        return uri(PLATFORM_NS + "reactionType");
    }

    public static Node reactedByProperty() {
        return uri(PLATFORM_NS + "reactedBy");
    }

    public static Node reactedAtProperty() {
        return uri(PLATFORM_NS + "reactedAt");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String reactionUri) {
        return Triple.create(uri(reactionUri), rdfTypeProperty(), uri("platform:Reaction"));
    }

    public static Triple createIdProperty(String reactionUri, String id) {
        return Triple.create(uri(reactionUri), idProperty(), RdfUtils.stringLiteral(id));
    }

    public static Triple createReactionTypeProperty(String reactionUri, String reactionType) {
        return Triple.create(uri(reactionUri), reactionTypeProperty(), RdfUtils.stringLiteral(reactionType));
    }

    public static Triple createReactedByProperty(String reactionUri, String userUri) {
        return Triple.create(uri(reactionUri), reactedByProperty(), uri(userUri));
    }

    public static Triple createReactedAtProperty(String reactionUri, LocalDateTime reactedAt) {
        return Triple.create(uri(reactionUri), reactedAtProperty(), RdfUtils.dateTimeLiteral(reactedAt));
    }

    // Legacy method compatibility
    public static Triple createReactionContentProperty(String reactionUri, String content) {
        return createReactionTypeProperty(reactionUri, content);
    }

    public static Triple createReactionByProperty(String reactionUri, String userUri) {
        return createReactedByProperty(reactionUri, userUri);
    }

    public static Triple createReactionCreatedAtProperty(String reactionUri, LocalDateTime createdAt) {
        return createReactedAtProperty(reactionUri, createdAt);
    }
}