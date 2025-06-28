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

    public static Node identifierProperty() { return uri(GH_NS + "identifier"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node isRootCommentProperty() { return uri(GH_NS + "isRootComment"); }
    public static Node commentReplyCountProperty() { return uri(GH_NS + "commentReplyCount"); }
    public static Node hasCommentReplyProperty() { return uri(GH_NS + "hasCommentReply"); }
    public static Node reviewCommentOfProperty() { return uri(GH_NS + "reviewCommentOf"); }
    public static Node reviewCommentReplyToProperty() { return uri(GH_NS + "reviewCommentReplyTo"); }

    public static Node reviewCommentClass() { return uri(GH_NS + "ReviewComment"); }
    public static Node reviewCommentContainerClass() { return uri(GH_NS + "ReviewCommentContainer"); }


    public static Triple createCommentIdentifierProperty(String commentUri, long id) {
        return Triple.create(uri(commentUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createCommentDescriptionProperty(String commentUri, String body) {
        return Triple.create(uri(commentUri), descriptionProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createCommentAuthorProperty(String commentUri, String authorUri) {
        return Triple.create(uri(commentUri), RdfGithubIssueUtils.authorProperty(), uri(authorUri));
    }

    public static Triple createCommentCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(uri(commentUri), RdfGithubIssueUtils.createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createCommentIsRootProperty(String commentUri, boolean root) {
        return Triple.create(uri(commentUri), isRootCommentProperty(), RdfUtils.booleanLiteral(root));
    }

    public static Triple createCommentReplyCountProperty(String commentUri, long count) {
        return Triple.create(uri(commentUri), commentReplyCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createHasCommentReplyProperty(String commentUri, String replyUri) {
        return Triple.create(uri(commentUri), hasCommentReplyProperty(), uri(replyUri));
    }

    public static Triple createReviewCommentOfProperty(String commentUri, String reviewUri) {
        return Triple.create(uri(commentUri), reviewCommentOfProperty(), uri(reviewUri));
    }

    public static Triple createReviewCommentReplyToProperty(String commentUri, String parentUri) {
        return Triple.create(uri(commentUri), reviewCommentReplyToProperty(), uri(parentUri));
    }


    public static Triple createReviewCommentRdfTypeProperty(String commentUri) {
        return Triple.create(uri(commentUri), RdfGithubIssueUtils.rdfTypeProperty(), reviewCommentClass());
    }

    public static Triple createReviewCommentContainerRdfTypeProperty(String containerUri) {
        return Triple.create(uri(containerUri), RdfGithubIssueUtils.rdfTypeProperty(), reviewCommentContainerClass());
    }

}
