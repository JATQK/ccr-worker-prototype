package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;

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

    public static Node userProperty() {
        return RdfUtils.uri(GH_NS + "user");
    }

    public static Node assigneeProperty() {
        return RdfUtils.uri(GH_NS + "assignee");
    }

    public static Node requestedReviewerProperty() {
        return RdfUtils.uri(GH_NS + "requestedReviewer");
    }

    public static Node milestoneProperty() {
        return RdfUtils.uri(GH_NS + "milestone");
    }

    public static Node submittedAtProperty() {
        return RdfUtils.uri(GH_NS + "submittedAt");
    }

    public static Node updatedAtProperty() {
        return RdfUtils.uri(GH_NS + "updatedAt");
    }

    public static Node closedAtProperty() {
        return RdfUtils.uri(GH_NS + "closedAt");
    }

    // Merge information
    public static Node mergedProperty() {
        return RdfUtils.uri(GH_NS + "merged");
    }

    public static Node mergedAtProperty() {
        return RdfUtils.uri(GH_NS + "mergedAt");
    }

    public static Node mergedByProperty() {
        return RdfUtils.uri(GH_NS + "mergedBy");
    }

    public static Node mergeCommitShaProperty() {
        return RdfUtils.uri(GH_NS + "mergeCommitSha");
    }

    // Commit linking
    public static Node containsCommitProperty() {
        return RdfUtils.uri(GH_NS + "containsCommit");
    }

    public static Node referencedByProperty() {
        return RdfUtils.uri(GH_NS + "referencedBy");
    }


    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri("github:GithubIssue"));
    }

    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return Triple.create(RdfUtils.uri(issueUri), numberProperty(), RdfUtils.integerLiteral(number));
    }

    public static Triple createIssueStateProperty(String issueUri, String state) {
        return Triple.create(RdfUtils.uri(issueUri), stateProperty(), RdfUtils.uri(GH_NS + state.toLowerCase()));
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

    public static Triple createIssueAssigneeProperty(String issueUri, String userUri) {
        return Triple.create(RdfUtils.uri(issueUri), assigneeProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueMilestoneProperty(String issueUri, String milestoneUri) {
        return Triple.create(RdfUtils.uri(issueUri), milestoneProperty(), RdfUtils.uri(milestoneUri));
    }

    public static Triple createIssueSubmittedAtProperty(String issueUri, LocalDateTime submittedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), submittedAtProperty(), RdfUtils.dateTimeLiteral(submittedAtDateTime));
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAtDateTime));
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAtDateTime));
    }

    // Merge information triples
    public static Triple createIssueMergedProperty(String issueUri, boolean merged) {
        return Triple.create(RdfUtils.uri(issueUri), mergedProperty(), RdfUtils.booleanLiteral(merged));
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

    public static Triple createIssueContainsCommitProperty(String issueUri, String commitUri) {
        return Triple.create(RdfUtils.uri(issueUri), containsCommitProperty(), RdfUtils.uri(commitUri));
    }

    public static Triple createIssueReferencedByProperty(String issueUri, String commitUri) {
        return Triple.create(RdfUtils.uri(issueUri), referencedByProperty(), RdfUtils.uri(commitUri));
    }

    public static Triple createIssueRequestedReviewerProperty(String issueUri, String reviewerUri) {
        return Triple.create(RdfUtils.uri(issueUri), requestedReviewerProperty(), RdfUtils.uri(reviewerUri));
    }

}


