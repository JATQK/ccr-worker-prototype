package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformLabelUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    public static Node idProperty() {
        return uri(PLATFORM_NS + "id");
    }

    public static Node nameProperty() {
        return uri(PLATFORM_NS + "name");
    }

    public static Node descriptionProperty() {
        return uri(PLATFORM_NS + "description");
    }

    public static Node colorProperty() {
        return uri(PLATFORM_NS + "color");
    }

    public static Node urlProperty() {
        return uri(PLATFORM_NS + "url");
    }

    // Relationship properties
    public static Node hasLabelProperty() {
        return uri(PLATFORM_NS + "hasLabel");
    }

    public static Node labelOfProperty() {
        return uri(PLATFORM_NS + "labelOf");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String labelUri) {
        return Triple.create(uri(labelUri), rdfTypeProperty(), uri("platform:Label"));
    }

    public static Triple createIdProperty(String labelUri, String id) {
        return Triple.create(uri(labelUri), idProperty(), RdfUtils.stringLiteral(id));
    }

    public static Triple createNameProperty(String labelUri, String name) {
        return Triple.create(uri(labelUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createDescriptionProperty(String labelUri, String description) {
        return Triple.create(uri(labelUri), descriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createColorProperty(String labelUri, String color) {
        return Triple.create(uri(labelUri), colorProperty(), RdfUtils.stringLiteral(color));
    }

    public static Triple createUrlProperty(String labelUri, String url) {
        return Triple.create(uri(labelUri), urlProperty(), RdfUtils.stringLiteral(url));
    }

    // Relationship methods
    public static Triple createHasLabelProperty(String resourceUri, String labelUri) {
        return Triple.create(uri(resourceUri), hasLabelProperty(), uri(labelUri));
    }

    public static Triple createLabelOfProperty(String labelUri, String resourceUri) {
        return Triple.create(uri(labelUri), labelOfProperty(), uri(resourceUri));
    }
}