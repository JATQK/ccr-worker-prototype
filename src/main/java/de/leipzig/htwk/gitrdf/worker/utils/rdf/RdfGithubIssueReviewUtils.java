package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueReviewUtils {
    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }
    
    public static Node reviewProperty() { return uri(GH_NS + "review"); }
    public static Node reviewOfProperty() { return uri(GH_NS + "reviewOf"); }

    public static Node identifierProperty() { return uri(GH_NS + "id");}
    public static Node urlProperty() { return uri(GH_NS + "url"); }
    public static Node descriptionProperty() { return uri(GH_NS + "description"); }
    public static Node commitIdProperty() { return uri(GH_NS + "commitId"); }
    
    public static Node commentProperty() {
        return uri(GH_NS + "comment");
    }

    public static Node rootCommentsProperty() { return uri(GH_NS + "rootComment"); }
    public static Node reviewCommentCountProperty() { return uri(GH_NS + "reviewCommentCount"); }
    public static Node rootCommentCountProperty() { return uri(GH_NS + "rootCommentCount"); }
    public static Node threadCountProperty() { return uri(GH_NS + "threadCount"); }
    public static Node firstCommentAtProperty() { return uri(GH_NS + "firstCommentAt"); }
    public static Node lastCommentAtProperty() { return uri(GH_NS + "lastCommentAt"); }

    public static Node lastActivityProperty() {
        return uri(GH_NS + "lastActivity");
    }

    public static Triple createReviewCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(uri(reviewUri), commentProperty(), uri(commentUri));
    }
    public static Triple createReviewUrlProperty(String reviewUri, String reviewUrl) {
        return Triple.create(uri(reviewUri), urlProperty(), RdfUtils.uri(reviewUrl));
    }

    public static Triple createIssueReviewRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(),
                RdfUtils.uri("github:GithubIssueReview"));
    }

    public static Triple createIssueReviewProperty(String issueUri, String reviewId) {
        return Triple.create(RdfUtils.uri(issueUri), RdfGithubIssueReviewUtils.reviewProperty(), RdfUtils.uri(
                reviewId));
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
        return Triple.create(uri(reviewUri), RdfPlatformTicketUtils.stateProperty(), RdfUtils.uri(PLATFORM_NS + state.toLowerCase()));
    }

    public static Triple createReviewSubmittedAtProperty(String reviewUri, LocalDateTime submittedAt) {
        return Triple.create(uri(reviewUri), RdfPlatformTicketUtils.createdAtProperty(), RdfUtils.dateTimeLiteral(submittedAt));
    }

    public static Triple createReviewUpdatedAtProperty(String reviewUri, LocalDateTime updatedAt) {
        return Triple.create(uri(reviewUri), RdfPlatformTicketUtils.updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createReviewUserProperty(String reviewUri, String creatorUri) {
        return Triple.create(uri(reviewUri), RdfPlatformTicketUtils.submitterProperty(), uri(creatorUri));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(uri(reviewUri), commitIdProperty(), RdfUtils.stringLiteral(commitId));
    }

    public static Triple createRootCommentsProperty(String reviewUri, String commentUri) {
        return Triple.create(uri(reviewUri), rootCommentsProperty(), uri(commentUri));
    }

    public static Triple createReviewCommentCountProperty(String reviewUri, long count) {
        return Triple.create(uri(reviewUri), reviewCommentCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createRootCommentCountProperty(String reviewUri, long count) {
        return Triple.create(uri(reviewUri), rootCommentCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createThreadCountProperty(String reviewUri, long count) {
        return Triple.create(uri(reviewUri), threadCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createFirstCommentAtProperty(String reviewUri, LocalDateTime dateTime) {
        return Triple.create(uri(reviewUri), firstCommentAtProperty(), RdfUtils.dateTimeLiteral(dateTime));
    }

    public static Triple createLastCommentAtProperty(String reviewUri, LocalDateTime dateTime) {
        return Triple.create(uri(reviewUri), lastCommentAtProperty(), RdfUtils.dateTimeLiteral(dateTime));
    }

    public static Triple createLastActivityProperty(String reviewUri, LocalDateTime dateTime) {
        return Triple.create(uri(reviewUri), lastActivityProperty(), RdfUtils.dateTimeLiteral(dateTime));
    }

}
