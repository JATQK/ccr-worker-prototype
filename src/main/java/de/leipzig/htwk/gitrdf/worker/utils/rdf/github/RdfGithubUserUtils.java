package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformPersonUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubUserUtils extends RdfPlatformPersonUtils {

    // GitHub-specific properties - most are now handled by platform ontology
    // Keep only GitHub-specific extensions if needed


    // Override platform method to create GitHub User type
    public static Triple createGitHubUserType(String userUri) {
        return Triple.create(uri(userUri), rdfTypeProperty(), uri("github:User"));
    }

    // Create platform username property (authoritative identifier replacing github:login)
    public static Triple createUsernamePropertyForGithub(String userUri, String username) {
        return createUsernameProperty(userUri, username);
    }

    public static Triple createEmailProperty(String userUri, String email) {
        return RdfPlatformPersonUtils.createEmailProperty(userUri, email);
    }

    public static Triple createUserIdProperty(String userUri, String userId) {
        return createIdProperty(userUri, userId);
    }

    public static Triple createUserIdProperty(String userUri, long id) {
        return createUserIdProperty(userUri, String.valueOf(id));
    }

    public static Triple createNameProperty(String userUri, String name) {
        return RdfPlatformPersonUtils.createNameProperty(userUri, name);
    }

    // Additional GitHub user methods
    public static Triple createGitAuthorEmailProperty(String userUri, String email) {
        return Triple.create(uri(userUri), uri("git:authorEmail"), RdfUtils.stringLiteral(email));
    }

    public static Triple createUserTypeProperty(String userUri, String userType) {
        return Triple.create(uri(userUri), uri("github:userType"), RdfUtils.stringLiteral(userType));
    }
}
