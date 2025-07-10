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
    public static Node nameProperty() { return RdfUtils.uri(PF_NS + "name"); }

    public static Triple createGitHubUserType(String userUri) {
        return Triple.create(RdfUtils.uri(userUri), rdfTypeProperty(), RdfUtils.uri("github:GitHubUser"));
    }

    public static Triple createLoginProperty(String userUri, String login) {
        return Triple.create(RdfUtils.uri(userUri), loginProperty(), RdfUtils.stringLiteral(login));
    }

    public static Triple createUserIdProperty(String userUri, long id) {
        return Triple.create(RdfUtils.uri(userUri), userIdProperty(), RdfUtils.nonNegativeIntegerLiteral(id));
    }

    public static Triple createNameProperty(String userUri, String name) {
        return Triple.create(RdfUtils.uri(userUri), nameProperty(), RdfUtils.stringLiteral(name));
    }
}
