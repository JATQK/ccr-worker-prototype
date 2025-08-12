package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueReviewUtils {
    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return uri("rdf:type");
    }
    
    // v2 platform ontology properties for reviews
    public static Node hasReviewProperty() { 
        return uri(PLATFORM_NS + "hasReview"); 
    }
    
    public static Node reviewOfProperty() { 
        return uri(PLATFORM_NS + "reviewOf"); 
    }

    public static Node idProperty() { 
        return uri(PLATFORM_NS + "id");
    }
    
    public static Node urlProperty() { 
        return uri(PLATFORM_NS + "url"); 
    }
    
    public static Node reviewBodyProperty() { 
        return uri(PLATFORM_NS + "reviewBody"); 
    }
    
    public static Node reviewerProperty() { 
        return uri(PLATFORM_NS + "reviewer"); 
    }
    
    public static Node reviewStateProperty() { 
        return uri(PLATFORM_NS + "reviewState"); 
    }
    
    public static Node reviewedAtProperty() { 
        return uri(PLATFORM_NS + "reviewedAt"); 
    }

    public static Node apiUrlProperty() {
        return uri(PLATFORM_NS + "apiUrl");
    }

    public static Node commitIdProperty() {
        return uri(PLATFORM_NS + "commitId");
    }
    
    public static Node hasCommentProperty() { 
        return uri(PLATFORM_NS + "hasComment"); 
    }

    public static Node createdAtProperty() {
        return uri(PLATFORM_NS + "createdAt");
    }

    public static Node updatedAtProperty() {
        return uri(PLATFORM_NS + "updatedAt");
    }

    public static Triple createReviewCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(uri(reviewUri), hasCommentProperty(), uri(commentUri));
    }
    
    public static Triple createReviewUrlProperty(String reviewUri, String reviewUrl) {
        return Triple.create(uri(reviewUri), urlProperty(), RdfUtils.stringLiteral(reviewUrl));
    }

    // GitHub Review is a subclass of platform:Review
    public static Triple createIssueReviewRdfTypeProperty(String reviewUri) {
        return Triple.create(uri(reviewUri), rdfTypeProperty(), uri("github:Review"));
    }


    public static Triple createIssueReviewProperty(String issueUri, String reviewUri) {
        return Triple.create(uri(issueUri), hasReviewProperty(), uri(reviewUri));
    }

    public static Triple createReviewIdentifierProperty(String reviewUri, String reviewId) {
        return Triple.create(uri(reviewUri), idProperty(), RdfUtils.stringLiteral(reviewId));
    }

    public static Triple createReviewIdentifierProperty(String reviewUri, long reviewId) {
        return createReviewIdentifierProperty(reviewUri, String.valueOf(reviewId));
    }

    public static Triple createReviewOfProperty(String reviewUri, String issueUri) {
        return Triple.create(uri(reviewUri), reviewOfProperty(), uri(issueUri));
    }

    public static Triple createReviewDescriptionProperty(String reviewUri, String body) {
        return Triple.create(uri(reviewUri), reviewBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createReviewStateProperty(String reviewUri, String state) {
        return Triple.create(uri(reviewUri), reviewStateProperty(), uri(PLATFORM_NS + state.toUpperCase()));
    }

    public static Triple createReviewSubmittedAtProperty(String reviewUri, LocalDateTime submittedAt) {
        return Triple.create(uri(reviewUri), reviewedAtProperty(), RdfUtils.dateTimeLiteral(submittedAt));
    }

    public static Triple createReviewUpdatedAtProperty(String reviewUri, LocalDateTime updatedAt) {
        return Triple.create(uri(reviewUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createReviewUserProperty(String reviewUri, String reviewerUri) {
        return Triple.create(uri(reviewUri), reviewerProperty(), uri(reviewerUri));
    }

    public static Triple createReviewCreatedAtProperty(String reviewUri, LocalDateTime createdAt) {
        return Triple.create(uri(reviewUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createReviewApiUrlProperty(String reviewUri, String apiUrl) {
        return Triple.create(uri(reviewUri), apiUrlProperty(), RdfUtils.stringLiteral(apiUrl));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(uri(reviewUri), commitIdProperty(), RdfUtils.stringLiteral(commitId));
    }

}
