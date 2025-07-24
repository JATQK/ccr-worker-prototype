package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

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

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform Repository Properties (from platform ontology lines 327-362)
    public static Node repositoryNameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "repositoryName");
    }

    public static Node repositoryOwnerProperty() {
        return RdfUtils.uri(PLATFORM_NS + "repositoryOwner");
    }

    public static Node repositoryDescriptionProperty() {
        return RdfUtils.uri(PLATFORM_NS + "repositoryDescription");
    }

    public static Node isPrivateProperty() {
        return RdfUtils.uri(PLATFORM_NS + "isPrivate");
    }

    public static Node defaultBranchProperty() {
        return RdfUtils.uri(PLATFORM_NS + "defaultBranch");
    }

    public static Node repositoryUrlProperty() {
        return RdfUtils.uri(PLATFORM_NS + "repositoryUrl");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String repositoryUri) {
        return Triple.create(RdfUtils.uri(repositoryUri), rdfTypeProperty(), RdfUtils.uri("platform:Repository"));
    }

    public static Triple createRepositoryNameProperty(String repositoryUri, String name) {
        return Triple.create(RdfUtils.uri(repositoryUri), repositoryNameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createRepositoryOwnerProperty(String repositoryUri, String ownerUri) {
        return Triple.create(RdfUtils.uri(repositoryUri), repositoryOwnerProperty(), RdfUtils.uri(ownerUri));
    }

    public static Triple createRepositoryDescriptionProperty(String repositoryUri, String description) {
        return Triple.create(RdfUtils.uri(repositoryUri), repositoryDescriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createIsPrivateProperty(String repositoryUri, boolean isPrivate) {
        return Triple.create(RdfUtils.uri(repositoryUri), isPrivateProperty(), RdfUtils.booleanLiteral(isPrivate));
    }

    public static Triple createDefaultBranchProperty(String repositoryUri, String defaultBranch) {
        return Triple.create(RdfUtils.uri(repositoryUri), defaultBranchProperty(), RdfUtils.stringLiteral(defaultBranch));
    }

    public static Triple createRepositoryUrlProperty(String repositoryUri, String url) {
        return Triple.create(RdfUtils.uri(repositoryUri), repositoryUrlProperty(), RdfUtils.anyUriLiteral(url));
    }
}