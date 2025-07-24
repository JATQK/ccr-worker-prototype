package de.leipzig.htwk.gitrdf.worker.utils.rdf.platform;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Platform-agnostic utility class for RDF operations on platform:Person entities.
 * This class implements the base properties defined in the git2RDFLab-platform ontology
 * for persons/users that are common across all platform implementations.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RdfPlatformPersonUtils {

    protected static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    // Core RDF properties
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    // Platform Person Properties (from platform ontology lines 59-101)
    public static Node nameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "name");
    }

    public static Node emailProperty() {
        return RdfUtils.uri(PLATFORM_NS + "email");
    }

    public static Node usernameProperty() {
        return RdfUtils.uri(PLATFORM_NS + "username");
    }

    public static Node profileUrlProperty() {
        return RdfUtils.uri(PLATFORM_NS + "profileUrl");
    }

    public static Node userIdProperty() {
        return RdfUtils.uri(PLATFORM_NS + "userId");
    }

    public static Node avatarUrlProperty() {
        return RdfUtils.uri(PLATFORM_NS + "avatarUrl");
    }

    public static Node userTypeProperty() {
        return RdfUtils.uri(PLATFORM_NS + "userType");
    }

    // Triple creation methods for platform properties
    public static Triple createRdfTypeProperty(String personUri) {
        return Triple.create(RdfUtils.uri(personUri), rdfTypeProperty(), RdfUtils.uri("platform:Person"));
    }

    public static Triple createNameProperty(String personUri, String name) {
        return Triple.create(RdfUtils.uri(personUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createEmailProperty(String personUri, String email) {
        return Triple.create(RdfUtils.uri(personUri), emailProperty(), RdfUtils.stringLiteral(email));
    }

    public static Triple createUsernameProperty(String personUri, String username) {
        return Triple.create(RdfUtils.uri(personUri), usernameProperty(), RdfUtils.stringLiteral(username));
    }

    public static Triple createProfileUrlProperty(String personUri, String profileUrl) {
        return Triple.create(RdfUtils.uri(personUri), profileUrlProperty(), RdfUtils.anyUriLiteral(profileUrl));
    }

    public static Triple createUserIdProperty(String personUri, String userId) {
        // v2.1: Changed from long to String to handle alphanumeric IDs across platforms
        return Triple.create(RdfUtils.uri(personUri), userIdProperty(), RdfUtils.stringLiteral(userId));
    }

    public static Triple createAvatarUrlProperty(String personUri, String avatarUrl) {
        return Triple.create(RdfUtils.uri(personUri), avatarUrlProperty(), RdfUtils.anyUriLiteral(avatarUrl));
    }

    public static Triple createUserTypeProperty(String personUri, String userType) {
        return Triple.create(RdfUtils.uri(personUri), userTypeProperty(), RdfUtils.stringLiteral(userType));
    }
}