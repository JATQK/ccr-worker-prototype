package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueUtils {

    public static Node issueIdProperty() {
        return RdfUtils.uri("github://IssueId");
    }

    public static Node stateProperty() {
        return RdfUtils.uri("github://IssueState");
    }

    public static Node titleProperty() {
        return RdfUtils.uri("github://IssueTitle");
    }

    public static Node bodyProperty() {
        return RdfUtils.uri("github://IssueBody");
    }

    public static Node userProperty() {
        return RdfUtils.uri("github://IssueUser"); //maybe creator? -> but issue api also calls it user and not creator
    }

    public static Node labelProperty() {
        return RdfUtils.uri("github://IssueLabel");
    }

    public static Node assigneeProperty() {
        return RdfUtils.uri("github://IssueAssignee");
    }

    public static Node milestoneProperty() {
        return RdfUtils.uri("github://IssueMilestone");
    }

    public static Node createdAtProperty() {
        return RdfUtils.uri("github://CreatedAt");
    }

    public static Node updatedAtProperty() {
        return RdfUtils.uri("github://UpdatedAt");
    }

    public static Node closedAtProperty() {
        return RdfUtils.uri("github://ClosedAt");
    }

    public static Triple createIssueIdProperty(String issueUri, long id) {
        return Triple.create(RdfUtils.uri(issueUri), issueIdProperty(), RdfUtils.longLiteral(id));
    }

    public static Triple createIssueStateProperty(String issueUri, String state) {
        return Triple.create(RdfUtils.uri(issueUri), stateProperty(), RdfUtils.stringLiteral(state));
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

    public static Triple createIssueCreatedAtProperty(String issueUri, LocalDateTime createdAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAtDateTime));
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAtDateTime));
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAtDateTime));
    }

}
