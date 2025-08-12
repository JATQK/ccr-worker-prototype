package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Triple;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformReactionUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubReactionUtils extends RdfPlatformReactionUtils {
    // Use platform properties from v2 ontology
    

    public static Triple createReactionRdfTypeProperty(String reactionUri) {
        return Triple.create(uri(reactionUri), rdfTypeProperty(), uri("github:Reaction"));
    }

    // Use inherited platform methods for common properties
    public static Triple createReactionIdProperty(String reactionUri, String id) {
        return createIdProperty(reactionUri, id);
    }

    public static Triple createReactionIdProperty(String reactionUri, long id) {
        return createIdProperty(reactionUri, String.valueOf(id));
    }

    public static Triple createReactionContentProperty(String reactionUri, String content) {
        return createReactionTypeProperty(reactionUri, content);
    }

    public static Triple createReactionByProperty(String reactionUri, String userUri) {
        return createReactedByProperty(reactionUri, userUri);
    }

    public static Triple createReactionCreatedAtProperty(String reactionUri, LocalDateTime createdAt) {
        return createReactedAtProperty(reactionUri, createdAt);
    }

    // Removed createReactionOfProperty - use hasReaction from parent entity instead
}
