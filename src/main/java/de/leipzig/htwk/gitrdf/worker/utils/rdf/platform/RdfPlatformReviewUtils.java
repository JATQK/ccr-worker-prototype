package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfPlatformReviewUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // Platform Review Properties (from platform ontology)
    public static Node reviewIdProperty() {
        return uri(PLATFORM_NS + "reviewId");
    }

    public static Node reviewOfProperty() {
        return uri(PLATFORM_NS + "reviewOf");
    }

    public static Triple createReviewIdProperty(String reviewUri, String reviewId) {
        return Triple.create(uri(reviewUri), reviewIdProperty(), RdfUtils.stringLiteral(reviewId));
    }

    public static Triple createReviewOfProperty(String reviewUri, String ticketUri) {
        return Triple.create(uri(reviewUri), reviewOfProperty(), uri(ticketUri));
    }
}