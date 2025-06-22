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

    public static Node bodyProperty() { return RdfUtils.uri(PLATFORM_NS + "ticketBody"); }

    // Platform - GitHub

    public static Node issueIdProperty() {
        return RdfUtils.uri(GH_NS + "issueId");
    }

    public static Node issueNumberProperty() {
        return RdfUtils.uri(GH_NS + "issueNumber");
    }

    public static Node stateProperty() {
        return RdfUtils.uri(GH_NS + "issueState");
    }

    public static Node userProperty() { return RdfUtils.uri(GH_NS + "user"); }

    public static Node labelProperty() {
        return RdfUtils.uri(GH_NS + "issueLabel");
    }

    public static Node assigneeProperty() {
        return RdfUtils.uri(GH_NS + "issueAssignee");
    }


    public static Node milestoneProperty() {
        return RdfUtils.uri(GH_NS + "issueMilestone");
    }

    public static Node createdAtProperty() {
        return RdfUtils.uri(GH_NS + "issueCreatedAt");
    }

    public static Node updatedAtProperty() {
        return RdfUtils.uri(GH_NS + "issueUpdatedAt");
    }

    public static Node closedAtProperty() {
        return RdfUtils.uri(GH_NS + "issueClosedAt");
    }

    public static Node reviewerProperty() {
        return RdfUtils.uri(GH_NS + "issueReviewer");
    }

    public static Node mergedByProperty() {
        return RdfUtils.uri(GH_NS + "issueMergedBy");
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


    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri( "github:GithubIssue" ));
    }

    public static Triple createIssueIdProperty(String issueUri, long id) {
        return Triple.create(RdfUtils.uri(issueUri), issueIdProperty(), RdfUtils.stringLiteral(Long.toString(id)));
    }

    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return Triple.create(RdfUtils.uri(issueUri), issueNumberProperty(), RdfUtils.stringLiteral(Integer.toString(number)));
    }

    public static Triple createIssueStateProperty(String issueUri, String state) {
        return Triple.create(RdfUtils.uri(issueUri), stateProperty(), uri(GH_NS + state.toLowerCase()));
    }

    public static Triple createIssueTitleProperty(String issueUri, String title) {
        return Triple.create(RdfUtils.uri(issueUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createIssueBodyProperty(String issueUri, String body) {
        return Triple.create(RdfUtils.uri(issueUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueUserProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), userProperty(), RdfUtils.uri(userUri));
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

    public static Triple createIssueMergedByProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), mergedByProperty(), RdfUtils.uri(userUri));
    }

    // Comment related triple creators

    public static Triple createIssueCommentProperty(String issueUri, String commentUri) {
        return Triple.create(RdfUtils.uri(issueUri), issueCommentProperty(), RdfUtils.uri(commentUri));
    }

    public static Triple createIssueCommentIdProperty(String commentUri, long id) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentIdProperty(), RdfUtils.stringLiteral(Long.toString(id)));
    }

    public static Triple createIssueCommentBodyProperty(String commentUri, String body) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentBodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueCommentUserProperty(String commentUri, String userUri) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentUserProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueCommentCreatedAtProperty(String commentUri, LocalDateTime createdAt) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
    }

    public static Triple createIssueCommentUpdatedAtProperty(String commentUri, LocalDateTime updatedAt) {
        return Triple.create(RdfUtils.uri(commentUri), issueCommentUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
    }

}
