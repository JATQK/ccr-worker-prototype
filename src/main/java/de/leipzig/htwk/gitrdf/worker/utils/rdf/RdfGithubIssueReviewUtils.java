package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueReviewUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node reviewProperty() { return uri(GH_NS + "review"); }
    public static Node reviewOfProperty() { return uri(GH_NS + "reviewOf"); }
    public static Node identifierProperty() { return uri(GH_NS + "identifier"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node commitIdProperty() { return uri(GH_NS + "commitId"); }
    public static Node discussionProperty() { return uri(GH_NS + "discussion"); }
    public static Node commentProperty() { return uri(GH_NS + "comment"); }

    public static Triple createReviewIdentifierProperty(String reviewUri, long id) {
        return Triple.create(uri(reviewUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createReviewOfProperty(String reviewUri, String issueUri) {
        return Triple.create(uri(reviewUri), reviewOfProperty(), uri(issueUri));
    }

    public static Triple createReviewDescriptionProperty(String reviewUri, String body) {
        return Triple.create(uri(reviewUri), descriptionProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createReviewStateProperty(String reviewUri, String state) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.stateProperty(), RdfUtils.uri(GH_NS + state.toLowerCase()));
    }

    public static Triple createReviewCreatedAtProperty(String reviewUri, LocalDateTime createdAt) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createReviewUpdatedAtProperty(String reviewUri, LocalDateTime updatedAt) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createReviewCreatorProperty(String reviewUri, String creatorUri) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.creatorProperty(), uri(creatorUri));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(uri(reviewUri), commitIdProperty(), RdfUtils.stringLiteral(commitId));
    }


    public static Triple createReviewCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(uri(reviewUri), commentProperty(), uri(commentUri));
    }


    public static Triple createDiscussionProperty(String parentUri, String discussionUri) {
        return Triple.create(uri(parentUri), discussionProperty(), uri(discussionUri));
    }


    

}
