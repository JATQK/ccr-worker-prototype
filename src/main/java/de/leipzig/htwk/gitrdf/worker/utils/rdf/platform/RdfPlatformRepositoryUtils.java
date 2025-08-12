package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Repository entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for repositories that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformRepositoryUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }

    // v2 ontology properties
    public static Node nameProperty() {
        return uri(PLATFORM_NS + "name");
    }

    public static Node ownerProperty() {
        return uri(PLATFORM_NS + "owner");
    }

    public static Node descriptionProperty() {
        return uri(PLATFORM_NS + "description");
    }

    public static Node urlProperty() {
        return uri(PLATFORM_NS + "url");
    }

    public static Node defaultBranchProperty() {
        return uri(PLATFORM_NS + "defaultBranch");
    }

    public static Node isPrivateProperty() {
        return uri(PLATFORM_NS + "isPrivate");
    }

    public static Triple createRdfTypeProperty(String repositoryUri) {
        return Triple.create(uri(repositoryUri), rdfTypeProperty(), uri("platform:Repository"));
    }

    // v2 ontology triple creation methods
    public static Triple createNameProperty(String repositoryUri, String name) {
        return Triple.create(uri(repositoryUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createOwnerProperty(String repositoryUri, String ownerUri) {
        return Triple.create(uri(repositoryUri), ownerProperty(), uri(ownerUri));
    }

    public static Triple createDescriptionProperty(String repositoryUri, String description) {
        return Triple.create(uri(repositoryUri), descriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createUrlProperty(String repositoryUri, String url) {
        return Triple.create(uri(repositoryUri), urlProperty(), RdfUtils.stringLiteral(url));
    }

    public static Triple createIsPrivateProperty(String repositoryUri, boolean isPrivate) {
        return Triple.create(uri(repositoryUri), isPrivateProperty(), RdfUtils.booleanLiteral(isPrivate));
    }

    public static Triple createDefaultBranchProperty(String repositoryUri, String defaultBranch) {
        return Triple.create(uri(repositoryUri), defaultBranchProperty(), RdfUtils.stringLiteral(defaultBranch));
    }

    // Legacy method compatibility
    public static Triple createRepositoryNameProperty(String repositoryUri, String name) {
        return createNameProperty(repositoryUri, name);
    }

    public static Triple createRepositoryOwnerProperty(String repositoryUri, String ownerUri) {
        return createOwnerProperty(repositoryUri, ownerUri);
    }

    public static Triple createRepositoryDescriptionProperty(String repositoryUri, String description) {
        return createDescriptionProperty(repositoryUri, description);
    }

    // Legacy property methods
    public static Node repositoryNameProperty() {
        return nameProperty();
    }

    public static Node repositoryOwnerProperty() {
        return ownerProperty();
    }

    public static Node repositoryDescriptionProperty() {
        return descriptionProperty();
    }

}