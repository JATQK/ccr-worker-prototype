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

    public static Node reviewsProperty() { return uri(GH_NS + "reviews"); }
    public static Node hasReviewProperty() { return uri(GH_NS + "hasReview"); }
    public static Node reviewOfProperty() { return uri(GH_NS + "reviewOf"); }
    public static Node identifierProperty() { return uri(GH_NS + "identifier"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node commitIdProperty() { return uri(GH_NS + "commitId"); }
    public static Node hasReviewCommentProperty() { return uri(GH_NS + "hasReviewComment"); }
    public static Node discussionProperty() { return uri(GH_NS + "discussion"); }

    public static Node reviewClass() { return uri(GH_NS + "Review"); }



    public static Triple createIssueHasReviewProperty(String issueUri, String reviewUri) {
        return Triple.create(uri(issueUri), hasReviewProperty(), uri(reviewUri));
    }


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

    public static Triple createReviewCreatorProperty(String reviewUri, String creatorUri) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.creatorProperty(), uri(creatorUri));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(uri(reviewUri), commitIdProperty(), RdfUtils.stringLiteral(commitId));
    }

    public static Triple createReviewHasCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(uri(reviewUri), hasReviewCommentProperty(), uri(commentUri));
    }

    public static Triple createDiscussionProperty(String parentUri, String discussionUri) {
        return Triple.create(uri(parentUri), discussionProperty(), uri(discussionUri));
    }


    public static Triple createReviewRdfTypeProperty(String reviewUri) {
        return Triple.create(uri(reviewUri), RdfGithubIssueUtils.rdfTypeProperty(), reviewClass());
    }

}
