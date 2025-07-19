package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Label entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for labels that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformLabelUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform Label Properties (from platform ontology)
    public static Node labelNameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "labelName");
    }

    public static Node labelColorProperty() {
        return RdfUtils.uri(PLATFORM_NS + "labelColor");
    }

    public static Node labelDescriptionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "labelDescription");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String labelUri) {
        return Triple.create(RdfUtils.uri(labelUri), rdfTypeProperty(), RdfUtils.uri("platform:Label"));
    }

    public static Triple createLabelNameProperty(String labelUri, String name) {
        return Triple.create(RdfUtils.uri(labelUri), labelNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createLabelColorProperty(String labelUri, String color) {
        return Triple.create(RdfUtils.uri(labelUri), labelColorProperty(), RdfUtils.stringLiteral(color));
    }

    public static Triple createLabelDescriptionProperty(String labelUri, String description) {
        return Triple.create(RdfUtils.uri(labelUri), labelDescriptionProperty(), RdfUtils.stringLiteral(description));
    }
}