package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformPersonUtils {

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

    public static Node emailProperty() {
        return uri(PLATFORM_NS + "email");
    }

    public static Node usernameProperty() {
        return uri(PLATFORM_NS + "username");
    }


    public static Triple createNameProperty(String personUri, String name) {
        return Triple.create(uri(personUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createEmailProperty(String personUri, String email) {
        return Triple.create(uri(personUri), emailProperty(), RdfUtils.stringLiteral(email));
    }

    public static Triple createUsernameProperty(String personUri, String username) {
        return Triple.create(uri(personUri), usernameProperty(), RdfUtils.stringLiteral(username));
    }

    public static Triple createIdProperty(String personUri, String id) {
        return Triple.create(uri(personUri), idProperty(), RdfUtils.stringLiteral(id));
    }

}