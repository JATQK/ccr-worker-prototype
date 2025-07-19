package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubUserUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PF_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() { return RdfUtils.uri("rdf:type"); }
    public static Node loginProperty() { return RdfUtils.uri(GH_NS + "login"); }
    public static Node userIdProperty() { return RdfUtils.uri(GH_NS + "userId"); }

    public static Node userTypeProperty() {
        return RdfUtils.uri(GH_NS + "userType");
    }
    public static Node nameProperty() {
        return RdfUtils.uri(PF_NS + "name");
    }
    
    public static Node emailProperty() {
        return RdfUtils.uri(PF_NS + "email");
    }
    
    public static Node gitAuthorEmailProperty() {
        return RdfUtils.uri(GH_NS + "gitAuthorEmail");
    }


    public static Triple createGitHubUserType(String userUri) {
        return Triple.create(RdfUtils.uri(userUri), rdfTypeProperty(), RdfUtils.uri("github:GithubUser"));
    }

    public static Triple createLoginProperty(String userUri, String login) {
        return Triple.create(RdfUtils.uri(userUri), loginProperty(), RdfUtils.stringLiteral(login));
    }

    public static Triple createEmailProperty(String userUri, String email) {
        return Triple.create(RdfUtils.uri(userUri), emailProperty(), RdfUtils.stringLiteral(email));
    }

    public static Triple createUserIdProperty(String userUri, long id) {
        return Triple.create(RdfUtils.uri(userUri), userIdProperty(), RdfUtils.nonNegativeIntegerLiteral(id));
    }

    public static Triple createNameProperty(String userUri, String name) {
        return Triple.create(RdfUtils.uri(userUri), nameProperty(), RdfUtils.stringLiteral(name));
    }

    public static Triple createUserTypeProperty(String userUri, String userType) {
        return Triple.create(RdfUtils.uri(userUri), userTypeProperty(), RdfUtils.stringLiteral(userType));
    }

    public static Triple createGitAuthorEmailProperty(String userUri, String gitAuthorEmail) {
        return Triple.create(RdfUtils.uri(userUri), gitAuthorEmailProperty(), RdfUtils.stringLiteral(gitAuthorEmail));
    }
}
