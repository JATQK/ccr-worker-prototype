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
public final class RdfGithubIssueUtils {

    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";
    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // Base-Classes - Platform
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node titleProperty() {
        return RdfUtils.uri(PLATFORM_NS + "ticketTitle");
    }

    public static Node bodyProperty() {
        return RdfUtils.uri(PLATFORM_NS + "ticketBody");
    }

    // Platform - GitHub



    public static Node numberProperty() {
        return RdfUtils.uri(GH_NS + "number");
    }

    public static Node stateProperty() {
        return RdfUtils.uri(GH_NS + "state");
    }

    public static Node authorProperty() { return RdfUtils.uri(GH_NS + "author"); }

    public static Node labelProperty() {
        return RdfUtils.uri(GH_NS + "label");
    }

    public static Node assigneeProperty() {
        return RdfUtils.uri(GH_NS + "assignee");
    }


    public static Node milestoneProperty() {
        return RdfUtils.uri(GH_NS + "milestone");
    }

    public static Node createdAtProperty() {
        return RdfUtils.uri(GH_NS + "createdAt");
    }

    public static Node updatedAtProperty() {
        return RdfUtils.uri(GH_NS + "updatedAt");
    }

    public static Node closedAtProperty() {
        return RdfUtils.uri(GH_NS + "closedAt");
    }

    public static Node reviewerProperty() {
        return RdfUtils.uri(GH_NS + "reviewer");
    }


    public static Node reviewProperty() {
        return RdfUtils.uri(GH_NS + "review");
    }

    public static Node reviewOfProperty() {
        return RdfUtils.uri(GH_NS + "reviewOf");
    }

    public static Node reviewIdProperty() {
        return RdfUtils.uri(GH_NS + "reviewId");
    }


    public static Node reviewHtmlUrlProperty() {
        return RdfUtils.uri(GH_NS + "reviewHtmlUrl");
    }

    public static Node reviewCommitIdProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommitId");
    }


    public static Node reviewBodyProperty() {
        return RdfUtils.uri(GH_NS + "reviewBody");
    }

    public static Node reviewStateProperty() {
        return RdfUtils.uri(GH_NS + "reviewState");
    }

    // Review state resources
    public static Node reviewStateApproved() {
        return RdfUtils.uri(GH_NS + "reviewStateApproved");
    }

    public static Node reviewStateChangesRequested() {
        return RdfUtils.uri(GH_NS + "reviewStateChangesRequested");
    }

    public static Node reviewStateCommented() {
        return RdfUtils.uri(GH_NS + "reviewStateCommented");
    }

    public static Node reviewStateDismissed() {
        return RdfUtils.uri(GH_NS + "reviewStateDismissed");
    }

    public static Node reviewStatePending() {
        return RdfUtils.uri(GH_NS + "reviewStatePending");
    }

    public static Node reviewUserProperty() {
        return RdfUtils.uri(GH_NS + "reviewUser");
    }

    public static Node reviewSubmittedAtProperty() {
        return RdfUtils.uri(GH_NS + "reviewSubmittedAt");
    }

    public static Node mergedByProperty() {
        return RdfUtils.uri(GH_NS + "mergedBy");
    }

    public static Node repositoryProperty() {
        return RdfUtils.uri(GH_NS + "repository");
    }

    // Review container related nodes
    public static Node reviewsProperty() { return RdfUtils.uri(GH_NS + "reviews"); }
    public static Node reviewContainerType() { return RdfUtils.uri(GH_NS + "ReviewContainer"); }

    // Comment related nodes
    public static Node commentsProperty() {
        return RdfUtils.uri(GH_NS + "comments");
    }

    public static Node commentOfProperty() {
        return RdfUtils.uri(GH_NS + "commentOf");
    }

    // Discussion container related nodes
    public static Node discussionProperty() {
        return RdfUtils.uri(GH_NS + "discussion");
    }

    public static Node discussionOfProperty() {
        return RdfUtils.uri(GH_NS + "discussionOf");
    }
    public static Node commentBodyProperty() {
        return bodyProperty();
    }

    public static Node commentAuthorProperty() {
        return authorProperty();
    }

    public static Node commentCreatedAtProperty() {
        return createdAtProperty();
    }

    public static Node commentUpdatedAtProperty() {
        return updatedAtProperty();
    }

    // Comment related nodes
    public static Node issueCommentProperty() {
        return RdfUtils.uri(GH_NS + "issueComment");
    }

    public static Node issueCommentIdProperty() {
        return RdfUtils.uri(GH_NS + "issueCommentId");
    }

    public static Node issueCommentBodyProperty() {
        return RdfUtils.uri(GH_NS + "issueCommentBody");
    }

    public static Node issueCommentUserProperty() {
        return RdfUtils.uri(GH_NS + "issueCommentUser");
    }

    public static Node issueCommentCreatedAtProperty() {
        return RdfUtils.uri(GH_NS + "issueCommentCreatedAt");
    }

    public static Node issueCommentUpdatedAtProperty() {
        return RdfUtils.uri(GH_NS + "issueCommentUpdatedAt");
    }

    // Review comment related nodes
    public static Node reviewCommentProperty() {
        return RdfUtils.uri(GH_NS + "reviewComment");
    }

    public static Node reviewCommentOfProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommentOf");
    }

    public static Node reviewCommentBodyProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommentBody");
    }

    public static Node reviewCommentUserProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommentUser");
    }

    public static Node reviewCommentCreatedAtProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommentCreatedAt");
    }

    public static Node reviewCommentUpdatedAtProperty() {
        return RdfUtils.uri(GH_NS + "reviewCommentUpdatedAt");
    }

    public static Node bagItemProperty(int index) {
        return RdfUtils.uri("rdf:_" + index);
    }



    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri("github:GithubIssue"));
    }

    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return Triple.create(RdfUtils.uri(issueUri), numberProperty(), RdfUtils.stringLiteral(Integer.toString(number)));
    }

    public static Triple createIssueStateProperty(String issueUri, String state) {
        return Triple.create(RdfUtils.uri(issueUri), stateProperty(), uri(state.toLowerCase()));
    }

    public static Triple createIssueTitleProperty(String issueUri, String title) {
        return Triple.create(RdfUtils.uri(issueUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createIssueBodyProperty(String issueUri, String body) {
        return Triple.create(RdfUtils.uri(issueUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueUserProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), authorProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueLabelProperty(String issueUri, String labelUri) {
        return Triple.create(RdfUtils.uri(issueUri), labelProperty(), RdfUtils.uri(labelUri));
    }

    public static Triple createIssueAssigneeProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), assigneeProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueMilestoneProperty(String issueUri, String milestoneUri) {
        return Triple.create(RdfUtils.uri(issueUri), milestoneProperty(), RdfUtils.uri(milestoneUri));
    }

    // TODO: should this be changed later on? Use schema instead of ^^literalType
    public static Triple createIssueCreatedAtProperty(String issueUri, LocalDateTime createdAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAtDateTime));
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAtDateTime));
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAtDateTime));
    }

    public static Triple createIssueReviewerProperty(String issueUri, String reviewerUri) {
        return Triple.create(RdfUtils.uri(issueUri), reviewerProperty(), RdfUtils.uri(reviewerUri));
    }



    public static Triple createReviewsCollectionTypeBag(String collectionUri) {
        return Triple.create(RdfUtils.uri(collectionUri), rdfTypeProperty(), RdfUtils.uri("rdf:Bag"));
    }

    public static Triple createReviewsCollectionMemberProperty(String collectionUri, int index, String reviewUri) {
        return Triple.create(RdfUtils.uri(collectionUri), RdfUtils.uri("rdf:_" + index), RdfUtils.uri(reviewUri));
    }

    // kept for direct links if required
    public static Triple createIssueReviewProperty(String issueUri, String reviewUri) {
        return Triple.create(RdfUtils.uri(issueUri), reviewProperty(), RdfUtils.uri(reviewUri));
    }

    public static Triple createReviewRdfTypeProperty(String reviewUri) {
        return Triple.create(RdfUtils.uri(reviewUri), rdfTypeProperty(), RdfUtils.uri("github:GithubReview"));
    }

    public static Triple createReviewOfProperty(String reviewUri, String issueUri) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewOfProperty(), RdfUtils.uri(issueUri));
    }

    public static Triple createReviewIdProperty(String reviewUri, long id) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewIdProperty(), RdfUtils.longLiteral(id));
    }


    public static Triple createReviewHtmlUrlProperty(String reviewUri, String url) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewHtmlUrlProperty(), RdfUtils.uri(url));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewCommitIdProperty(), RdfUtils.stringLiteral(commitId));
    }


    public static Triple createReviewBodyProperty(String reviewUri, String body) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createReviewStateProperty(String reviewUri, String state) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewStateProperty(), reviewStateResource(state));
    }

    private static Node reviewStateResource(String state) {
        return switch (state.toUpperCase()) {
            case "APPROVED" -> reviewStateApproved();
            case "CHANGES_REQUESTED" -> reviewStateChangesRequested();
            case "COMMENTED" -> reviewStateCommented();
            case "DISMISSED" -> reviewStateDismissed();
            case "PENDING" -> reviewStatePending();
            default -> RdfUtils.uri(GH_NS + state.toLowerCase());
        };
    }

    public static Triple createReviewUserProperty(String reviewUri, String userUri) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewUserProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createReviewSubmittedAtProperty(String reviewUri, LocalDateTime submittedAt) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewSubmittedAtProperty(), RdfUtils.dateTimeLiteral(submittedAt));
    }

    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), mergedByProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueRepositoryProperty(String issueUri, String repoUri) {
        return Triple.create(RdfUtils.uri(issueUri), repositoryProperty(), RdfUtils.uri(repoUri));
    }

    // Comment related triple creators

    // Comment related triple creators
    public static Triple createIssueCommentProperty(String issueUri, String commentUri) {
        return Triple.create(RdfUtils.uri(issueUri), commentsProperty(), RdfUtils.uri(commentUri));
    }

    public static Triple createIssueCommentIdProperty(String commentUri, long id) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentIdProperty(),
                RdfUtils.stringLiteral(Long.toString(id)));
    }

    public static Triple createCommentRdfTypeProperty(String commentUri) {
        return Triple.create(RdfUtils.uri(commentUri), rdfTypeProperty(), RdfUtils.uri("github:GithubComment"));
    }

    public static Triple createIssueCommentOfProperty(String commentUri, String issueUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentOfProperty(), RdfUtils.uri(issueUri));
    }

    public static Triple createIssueCommentBodyProperty(String commentUri, String body) {
        return Triple.create(RdfUtils.uri(commentUri), commentBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueCommentUserProperty(String commentUri, String userUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentAuthorProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueCommentCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(commentUri), commentCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createIssueCommentUpdatedAtProperty(String commentUri, LocalDateTime updatedAt) {
        return Triple.create(RdfUtils.uri(commentUri), commentUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));

    }

    // Review comment related triple creators
    public static Triple createReviewCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewCommentProperty(), RdfUtils.uri(commentUri));
    }

    public static Triple createReviewCommentOfProperty(String commentUri, String reviewUri) {
        return Triple.create(RdfUtils.uri(commentUri), reviewCommentOfProperty(), RdfUtils.uri(reviewUri));
    }

    public static Triple createReviewCommentBodyProperty(String commentUri, String body) {
        return Triple.create(RdfUtils.uri(commentUri), reviewCommentBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createReviewCommentUserProperty(String commentUri, String userUri) {
        return Triple.create(RdfUtils.uri(commentUri), reviewCommentUserProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createReviewCommentCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(commentUri), reviewCommentCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createReviewCommentUpdatedAtProperty(String commentUri, LocalDateTime updatedAt) {
        return Triple.create(RdfUtils.uri(commentUri), reviewCommentUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }


    // Review container triple creators
    public static Triple createIssueReviewsProperty(String issueUri, String containerUri) {
        return Triple.create(RdfUtils.uri(issueUri), reviewsProperty(), RdfUtils.uri(containerUri));
    }

    public static Triple createReviewContainerTypeProperty(String containerUri) {
        return Triple.create(RdfUtils.uri(containerUri), rdfTypeProperty(), reviewContainerType());

    }

}
