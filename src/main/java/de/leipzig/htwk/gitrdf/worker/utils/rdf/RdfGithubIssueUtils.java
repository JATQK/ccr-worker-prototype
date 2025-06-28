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

    public static Node repositoryProperty() {
        return RdfUtils.uri(GH_NS + "repository");
    }


    // Merge information
    public static Node mergedProperty() { return RdfUtils.uri(GH_NS + "merged"); }

    public static Node mergedAtProperty() { return RdfUtils.uri(GH_NS + "mergedAt"); }

    public static Node mergedByProperty() { return RdfUtils.uri(GH_NS + "mergedBy"); }

    public static Node mergeCommitShaProperty() { return RdfUtils.uri(GH_NS + "mergeCommitSha"); }

    // Review linkage
    public static Node hasReviewProperty() { return RdfUtils.uri(GH_NS + "hasReview"); }

    public static Node reviewCountProperty() { return RdfUtils.uri(GH_NS + "reviewCount"); }

    public static Node reviewContainerProperty() { return RdfUtils.uri(GH_NS + "reviewContainer"); }

    // Review details
    public static Node reviewOfProperty() { return RdfUtils.uri(GH_NS + "reviewOf"); }

    public static Node reviewIdentifierProperty() { return RdfUtils.uri(GH_NS + "reviewIdentifier"); }

    public static Node reviewStateProperty() { return RdfUtils.uri(GH_NS + "reviewState"); }

    public static Node reviewAuthorProperty() { return RdfUtils.uri(GH_NS + "reviewAuthor"); }

    public static Node reviewCreatedAtProperty() { return RdfUtils.uri(GH_NS + "reviewCreatedAt"); }

    public static Node reviewUpdatedAtProperty() { return RdfUtils.uri(GH_NS + "reviewUpdatedAt"); }

    public static Node reviewCommitIdProperty() { return RdfUtils.uri(GH_NS + "reviewCommitId"); }

    public static Node authorAssociationProperty() { return RdfUtils.uri(GH_NS + "authorAssociation"); }

    public static Node reviewCommentCountProperty() { return RdfUtils.uri(GH_NS + "reviewCommentCount"); }

    public static Node hasReviewCommentProperty() { return RdfUtils.uri(GH_NS + "hasReviewComment"); }

    // Comment linkage
    public static Node hasCommentProperty() { return RdfUtils.uri(GH_NS + "hasComment"); }

    public static Node commentCountProperty() { return RdfUtils.uri(GH_NS + "commentCount"); }

    public static Node discussionProperty() { return RdfUtils.uri(GH_NS + "discussion"); }

    // Comment details
    public static Node commentOfProperty() { return RdfUtils.uri(GH_NS + "commentOf"); }

    public static Node commentIdentifierProperty() { return RdfUtils.uri(GH_NS + "commentIdentifier"); }

    public static Node commentDescriptionProperty() { return RdfUtils.uri(GH_NS + "commentDescription"); }

    public static Node commentCreatedAtProperty() { return RdfUtils.uri(GH_NS + "commentCreatedAt"); }

    public static Node commentAuthorProperty() { return RdfUtils.uri(GH_NS + "commentAuthor"); }

    public static Node isRootCommentProperty() { return RdfUtils.uri(GH_NS + "isRootComment"); }

    public static Node commentReplyCountProperty() { return RdfUtils.uri(GH_NS + "commentReplyCount"); }

    public static Node hasCommentReplyProperty() { return RdfUtils.uri(GH_NS + "hasCommentReply"); }

    public static Node commentReplyToProperty() { return RdfUtils.uri(GH_NS + "commentReplyTo"); }



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

    public static Triple createIssueAssigneeProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), assigneeProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueMilestoneProperty(String issueUri, String milestoneUri) {
        return Triple.create(RdfUtils.uri(issueUri), milestoneProperty(), RdfUtils.uri(milestoneUri));
    }


    public static Triple createIssueCreatedAtProperty(String issueUri, LocalDateTime createdAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAtDateTime));
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAtDateTime));
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAtDateTime));
    }

    public static Triple createIssueRepositoryProperty(String issueUri, String repoUri) {
        return Triple.create(RdfUtils.uri(issueUri), repositoryProperty(), RdfUtils.uri(repoUri));
    }


    // Merge information triples
    public static Triple createIssueMergedProperty(String issueUri, boolean merged) {
        return Triple.create(RdfUtils.uri(issueUri), mergedProperty(), RdfUtils.stringLiteral(Boolean.toString(merged)));
    }
  
    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), mergedByProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueMergedAtProperty(String issueUri, LocalDateTime mergedAt) {
        return Triple.create(RdfUtils.uri(issueUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createIssueMergeCommitShaProperty(String issueUri, String sha) {
        return Triple.create(RdfUtils.uri(issueUri), mergeCommitShaProperty(), RdfUtils.stringLiteral(sha));
    }

    // Review linkage triples
    public static Triple createIssueHasReviewProperty(String issueUri, String reviewUri) {
        return Triple.create(RdfUtils.uri(issueUri), hasReviewProperty(), RdfUtils.uri(reviewUri));
    }

    public static Triple createIssueReviewCountProperty(String issueUri, long count) {
        return Triple.create(RdfUtils.uri(issueUri), reviewCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createIssueReviewContainerProperty(String issueUri, String containerUri) {
        return Triple.create(RdfUtils.uri(issueUri), reviewContainerProperty(), RdfUtils.uri(containerUri));
    }

    // Review detail triples
    public static Triple createReviewOfProperty(String reviewUri, String issueUri) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewOfProperty(), RdfUtils.uri(issueUri));
    }

    public static Triple createReviewIdentifierProperty(String reviewUri, long identifier) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewIdentifierProperty(), RdfUtils.stringLiteral(Long.toString(identifier)));
    }

    public static Triple createReviewStateProperty(String reviewUri, String state) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewStateProperty(), uri(state.toLowerCase()));
    }

    public static Triple createReviewAuthorProperty(String reviewUri, String userUri) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewAuthorProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createReviewCreatedAtProperty(String reviewUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createReviewUpdatedAtProperty(String reviewUri, LocalDateTime updatedAt) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

    public static Triple createReviewCommitIdProperty(String reviewUri, String commitId) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewCommitIdProperty(), RdfUtils.stringLiteral(commitId));
    }

    public static Triple createReviewAuthorAssociationProperty(String reviewUri, String association) {
        return Triple.create(RdfUtils.uri(reviewUri), authorAssociationProperty(), RdfUtils.stringLiteral(association));
    }

    public static Triple createReviewCommentCountProperty(String reviewUri, long count) {
        return Triple.create(RdfUtils.uri(reviewUri), reviewCommentCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createReviewHasCommentProperty(String reviewUri, String commentUri) {
        return Triple.create(RdfUtils.uri(reviewUri), hasReviewCommentProperty(), RdfUtils.uri(commentUri));
    }

    // Comment linkage triples
    public static Triple createIssueHasCommentProperty(String issueUri, String commentUri) {
        return Triple.create(RdfUtils.uri(issueUri), hasCommentProperty(), RdfUtils.uri(commentUri));
    }

    public static Triple createIssueCommentCountProperty(String issueUri, long count) {
        return Triple.create(RdfUtils.uri(issueUri), commentCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createIssueDiscussionProperty(String issueUri, String discussionUri) {
        return Triple.create(RdfUtils.uri(issueUri), discussionProperty(), RdfUtils.uri(discussionUri));
    }

    // Comment detail triples
    public static Triple createCommentOfProperty(String commentUri, String parentUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentOfProperty(), RdfUtils.uri(parentUri));
    }

    public static Triple createCommentIdentifierProperty(String commentUri, long identifier) {
        return Triple.create(RdfUtils.uri(commentUri), commentIdentifierProperty(), RdfUtils.stringLiteral(Long.toString(identifier)));
    }

    public static Triple createCommentDescriptionProperty(String commentUri, String description) {
        return Triple.create(RdfUtils.uri(commentUri), commentDescriptionProperty(), RdfUtils.stringLiteral(description));
    }

    public static Triple createCommentCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(commentUri), commentCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createCommentAuthorProperty(String commentUri, String authorUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentAuthorProperty(), RdfUtils.uri(authorUri));
    }

    public static Triple createIsRootCommentProperty(String commentUri, boolean isRoot) {
        return Triple.create(RdfUtils.uri(commentUri), isRootCommentProperty(), RdfUtils.stringLiteral(Boolean.toString(isRoot)));
    }

    public static Triple createCommentReplyCountProperty(String commentUri, long count) {
        return Triple.create(RdfUtils.uri(commentUri), commentReplyCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
    }

    public static Triple createHasCommentReplyProperty(String commentUri, String replyUri) {
        return Triple.create(RdfUtils.uri(commentUri), hasCommentReplyProperty(), RdfUtils.uri(replyUri));
    }

    public static Triple createCommentReplyToProperty(String commentUri, String parentUri) {
        return Triple.create(RdfUtils.uri(commentUri), commentReplyToProperty(), RdfUtils.uri(parentUri));
    }

}
