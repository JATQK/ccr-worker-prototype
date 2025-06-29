package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueCommentUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }
    public static Node identifierProperty() { return uri(GH_NS + "commentId"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node isRootCommentProperty() { return uri(GH_NS + "isRootComment"); }
    public static Node dcommentReplyCountProperty() { return uri(GH_NS + "commentReplyCount"); }
    public static Node hasCommentReplyProperty() { return uri(GH_NS + "hasCommentReply"); }
    public static Node reviewCommentOfProperty() { return uri(GH_NS + "commentOf"); }
    public static Node reviewCommentReplyToProperty() { return uri(GH_NS + "commentReplyTo"); }


    public static Triple createReviewCommentRdfTypeProperty(String dcommentUri) {
        return Triple.create(RdfUtils.uri(dcommentUri), rdfTypeProperty(),
                RdfUtils.uri("github:GithubIssueReviewComment"));
    }

    public static Triple createCommentIdentifierProperty(String CommentUri, long id) {
        return Triple.create(uri(CommentUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createCommentDescriptionProperty(String CommentUri, String body) {
        return Triple.create(uri(CommentUri), descriptionProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createCommentUserProperty(String CommentUri, String userURI) {
        return Triple.create(uri(CommentUri), RdfGithubIssueUtils.userProperty(), uri(userURI));
    }

    public static Triple createCommentCreatedAtProperty(String CommentUri, LocalDateTime createdAt) {
        return Triple.create(uri(CommentUri), RdfGithubIssueUtils.submittedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createCommentIsRootProperty(String CommentUri, boolean root) {
        return Triple.create(uri(CommentUri), isRootCommentProperty(), RdfUtils.booleanLiteral(root));
    }

    public static Triple createCommentReplyCountProperty(String CommentUri, long count) {
        return Triple.create(uri(CommentUri), dcommentReplyCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createHasCommentReplyProperty(String CommentUri, String replyUri) {
        return Triple.create(uri(CommentUri), hasCommentReplyProperty(), uri(replyUri));
    }

    public static Triple createReviewCommentOfProperty(String CommentUri, String reviewUri) {
        return Triple.create(uri(CommentUri), reviewCommentOfProperty(), uri(reviewUri));
    }

    public static Triple createReviewCommentReplyToProperty(String CommentUri, String parentUri) {
        return Triple.create(uri(CommentUri), reviewCommentReplyToProperty(), uri(parentUri));
    }



}
