package de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.*;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubPullRequestUtils {

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

    public static Node userProperty() {
        return RdfUtils.uri(GH_NS + "user");
    }

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

    public static Node baseBranchProperty() {
        return RdfUtils.uri(GH_NS + "baseBranch");
    }

    public static Node headBranchProperty() {
        return RdfUtils.uri(GH_NS + "headBranch");
    }

    public static Node mergeCommitProperty() {
        return RdfUtils.uri(GH_NS + "mergeCommitEntry");
    }

    public static Node assigneeListProperty() {
        return RdfUtils.uri(GH_NS + "assigneeList");
    }

    public static Node reviewListProperty() {
        return RdfUtils.uri(GH_NS + "reviewList");
    }

    public static Node commitListProperty() {
        return RdfUtils.uri(GH_NS + "commitList");
    }

    public static Node checkListProperty() {
        return RdfUtils.uri(GH_NS + "checkList");
    }

    public static Node commentListProperty() {
        return RdfUtils.uri(GH_NS + "commentList");
    }

    // Existing Triple Creator Methods

    public static Triple createRdfTypeProperty(String pullRequestUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), rdfTypeProperty(), RdfUtils.uri("github:GithubPullRequest"));
    }

    public static Triple createIssueTitleProperty(String pullRequestUri, String title) {
        return Triple.create(RdfUtils.uri(pullRequestUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createIssueBodyProperty(String pullRequestUri, String body) {
        return Triple.create(RdfUtils.uri(pullRequestUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueUserProperty(String pullRequestUri, String userUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), userProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueLabelProperty(String pullRequestUri, String labelUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), labelProperty(), RdfUtils.uri(labelUri));
    }

    public static Triple createIssueAssigneeProperty(String pullRequestUri, String userUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), assigneeProperty(), RdfUtils.uri(userUri));
    }

    public static Triple createIssueMilestoneProperty(String pullRequestUri, String milestoneUri) {
        return Triple.create(RdfUtils.uri(pullRequestUri), milestoneProperty(), RdfUtils.uri(milestoneUri));
    }

    public static Triple createIssueCreatedAtProperty(String pullRequestUri, LocalDateTime createdAtDateTime) {
        return Triple.create(RdfUtils.uri(pullRequestUri), createdAtProperty(),
                RdfUtils.dateTimeLiteral(createdAtDateTime));
    }

    public static Triple createIssueUpdatedAtProperty(String pullRequestUri, LocalDateTime updatedAtDateTime) {
        return Triple.create(RdfUtils.uri(pullRequestUri), updatedAtProperty(),
                RdfUtils.dateTimeLiteral(updatedAtDateTime));
    }

    public static Triple createIssueClosedAtProperty(String pullRequestUri, LocalDateTime closedAtDateTime) {
        return Triple.create(RdfUtils.uri(pullRequestUri), closedAtProperty(),
                RdfUtils.dateTimeLiteral(closedAtDateTime));
    }

    public static Triple createMergeCommitEntryProperty(String pullRequestUri, Node commitEntryNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), mergeCommitProperty(), commitEntryNode);
    }

    public static Triple createHeadBranchProperty(String pullRequestUri, Node headBranchNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), headBranchProperty(), headBranchNode);
    }

    public static Triple createBaseBranchProperty(String pullRequestUri, Node baseBranchNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), baseBranchProperty(), baseBranchNode);
    }

    public static Triple createAssigneeListProperty(String pullRequestUri, Node assigneeListNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), assigneeListProperty(), assigneeListNode);
    }

    public static Triple createReviewListProperty(String pullRequestUri, Node reviewListNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), reviewListProperty(), reviewListNode);
    }

    public static Triple createCommitListProperty(String pullRequestUri, Node commitListNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), commitListProperty(), commitListNode);
    }

    public static Triple createCheckListProperty(String pullRequestUri, Node checkListNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), checkListProperty(), checkListNode);
    }

    public static Triple createCommentListProperty(String pullRequestUri, Node commentListNode) {
        return Triple.create(RdfUtils.uri(pullRequestUri), commentListProperty(), commentListNode);
    }

}