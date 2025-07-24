package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformPersonUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubUserUtils extends RdfPlatformPersonUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // GitHub-specific properties (extending platform base)
    public static Node gitAuthorEmailProperty() {
        return RdfUtils.uri(GH_NS + "gitAuthorEmail");
    }


    // Override platform method to create GitHub User type
    public static Triple createGitHubUserType(String userUri) {
        return Triple.create(RdfUtils.uri(userUri), rdfTypeProperty(), RdfUtils.uri("github:GithubUser"));
    }

    // Use inherited platform methods (for backward compatibility, these delegate to parent)
    public static Triple createLoginProperty(String userUri, String login) {
        return createUsernameProperty(userUri, login);
    }

    public static Triple createEmailProperty(String userUri, String email) {
        return RdfPlatformPersonUtils.createEmailProperty(userUri, email);
    }

    // v2.1: Use inherited platform methods for common properties - updated for string user IDs
    public static Triple createUserIdProperty(String userUri, String userId) {
        return RdfPlatformPersonUtils.createUserIdProperty(userUri, userId);
    }

    // Legacy method for backward compatibility - converts long to string
    public static Triple createUserIdProperty(String userUri, long id) {
        return RdfPlatformPersonUtils.createUserIdProperty(userUri, String.valueOf(id));
    }

    public static Triple createNameProperty(String userUri, String name) {
        return RdfPlatformPersonUtils.createNameProperty(userUri, name);
    }

    public static Triple createUserTypeProperty(String userUri, String userType) {
        return RdfPlatformPersonUtils.createUserTypeProperty(userUri, userType);
    }

    // GitHub-specific property creation
    public static Triple createGitAuthorEmailProperty(String userUri, String gitAuthorEmail) {
        return Triple.create(RdfUtils.uri(userUri), gitAuthorEmailProperty(), RdfUtils.stringLiteral(gitAuthorEmail));
    }
}
