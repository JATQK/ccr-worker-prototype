package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueDiscussionUtils {

    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }
    public static Node identifierProperty() { return uri(GH_NS + "discussionId"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node isRootDiscussionProperty() { return uri(GH_NS + "isRootDiscussion"); }
    public static Node discussionReplyCountProperty() { return uri(GH_NS + "discussionReplyCount"); }
    public static Node hasDiscussionReplyProperty() { return uri(GH_NS + "hasDiscussionReply"); }
    public static Node reviewDiscussionOfProperty() { return uri(GH_NS + "discussionOf"); }
    public static Node reviewDiscussionReplyToProperty() { return uri(GH_NS + "discussionReplyTo"); }


    public static Triple createReviewDiscussionRdfTypeProperty(String discussionUri) {
        return Triple.create(RdfUtils.uri(discussionUri), rdfTypeProperty(),
                RdfUtils.uri("github:GithubIssueReviewDiscussion"));
    }

    public static Triple createDiscussionIdentifierProperty(String DiscussionUri, long id) {
        return Triple.create(uri(DiscussionUri), identifierProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createDiscussionDescriptionProperty(String DiscussionUri, String body) {
        return Triple.create(uri(DiscussionUri), descriptionProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createDiscussionUserProperty(String DiscussionUri, String userURI) {
        return Triple.create(uri(DiscussionUri), RdfGithubIssueUtils.userProperty(), uri(userURI));
    }

    public static Triple createDiscussionCreatedAtProperty(String DiscussionUri, LocalDateTime createdAt) {
        return Triple.create(uri(DiscussionUri), RdfGithubIssueUtils.submittedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createDiscussionIsRootProperty(String DiscussionUri, boolean root) {
        return Triple.create(uri(DiscussionUri), isRootDiscussionProperty(), RdfUtils.booleanLiteral(root));
    }

    public static Triple createDiscussionReplyCountProperty(String DiscussionUri, long count) {
        return Triple.create(uri(DiscussionUri), discussionReplyCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createHasDiscussionReplyProperty(String DiscussionUri, String replyUri) {
        return Triple.create(uri(DiscussionUri), hasDiscussionReplyProperty(), uri(replyUri));
    }

    public static Triple createReviewDiscussionOfProperty(String DiscussionUri, String reviewUri) {
        return Triple.create(uri(DiscussionUri), reviewDiscussionOfProperty(), uri(reviewUri));
    }

    public static Triple createReviewDiscussionReplyToProperty(String DiscussionUri, String parentUri) {
        return Triple.create(uri(DiscussionUri), reviewDiscussionReplyToProperty(), uri(parentUri));
    }



}
