package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubReactionUtils {
    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node identifierProperty() { return uri(GH_NS + "reactionId"); }
    public static Node contentProperty() { return uri(GH_NS + "reactionContent"); }
    public static Node reactionByProperty() { return uri(GH_NS + "reactionBy"); }
    public static Node createdAtProperty() { return uri(GH_NS + "reactionCreatedAt"); }
    // Removed reactionOfProperty - reactions are linked via hasReaction from parent entity
    

    public static Triple createReactionRdfTypeProperty(String reactionUri) {
        return Triple.create(uri(reactionUri), rdfTypeProperty(), uri("github:GithubReaction"));
    }

    public static Triple createReactionIdProperty(String reactionUri, long id) {
        return Triple.create(uri(reactionUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createReactionContentProperty(String reactionUri, String content) {
        return Triple.create(uri(reactionUri), contentProperty(), RdfUtils.stringLiteral(content));
    }

    public static Triple createReactionByProperty(String reactionUri, String userUri) {
        return Triple.create(uri(reactionUri), reactionByProperty(), uri(userUri));
    }

    public static Triple createReactionCreatedAtProperty(String reactionUri, LocalDateTime createdAt) {
        return Triple.create(uri(reactionUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    // Removed createReactionOfProperty - use hasReaction from parent entity instead
}
