package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformRepositoryUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    public static Node nameProperty() {
        return uri(PLATFORM_NS + "name");
    }

    public static Node ownerProperty() {
        return uri(PLATFORM_NS + "owner");
    }
    public static Triple createNameProperty(String repositoryUri, String name) {
        return Triple.create(uri(repositoryUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createOwnerProperty(String repositoryUri, String ownerUri) {
        return Triple.create(uri(repositoryUri), ownerProperty(), uri(ownerUri));
    }

}