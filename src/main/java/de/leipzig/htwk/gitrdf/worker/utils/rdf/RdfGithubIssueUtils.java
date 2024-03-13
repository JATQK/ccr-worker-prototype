package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.*;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubIssueUtils {

    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";
    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

    // Base-Classes - Platform
    public static Node rdfTypeProperty() {
        return RdfUtils.uri("rdf:type");
    }

    public static Node titleProperty() {
        return RdfUtils.uri(PLATFORM_NS + "title");
    }

    public static Node bodyProperty() { return RdfUtils.uri(PLATFORM_NS + "body"); }

    // Platform - GitHub

    public static Node issueIdProperty() {
        return RdfUtils.uri(GH_NS + "issueId");
    }

    public static Node issueNumberProperty() {
        return RdfUtils.uri(GH_NS + "issueNumber");
    }

    public static Node stateProperty() {
        return RdfUtils.uri(GH_NS + "state");
    }

    public static Node userProperty() { return RdfUtils.uri(GH_NS + "user"); }

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


    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri( "github:GithubIssue" ));
    }

    public static Triple createIssueIdProperty(String issueUri, long id) {
        //return Triple.create(RdfUtils.uri(issueUri), issueIdProperty(), RdfUtils.longLiteral(id));
        return Triple.create(RdfUtils.uri(issueUri), issueIdProperty(), RdfUtils.stringLiteral(Long.toString(id)));
    }

    public static Triple createIssueNumberProperty(String issueUri, int number) {
        return Triple.create(RdfUtils.uri(issueUri), issueNumberProperty(), RdfUtils.stringLiteral(Integer.toString(number)));
    }

    public static Triple createIssueStateProperty(String issueUri, String state) {
        //return Triple.create(RdfUtils.uri(issueUri), stateProperty(), RdfUtils.stringLiteral(state));
        return Triple.create(RdfUtils.uri(issueUri), stateProperty(), uri(GH_NS + state.toLowerCase()));
    }

    public static Triple createIssueTitleProperty(String issueUri, String title) {
        return Triple.create(RdfUtils.uri(issueUri), titleProperty(), RdfUtils.stringLiteral(title));
    }

    public static Triple createIssueBodyProperty(String issueUri, String body) {
        return Triple.create(RdfUtils.uri(issueUri), bodyProperty(), RdfUtils.stringLiteral(body));
    }

    public static Triple createIssueUserProperty(String issueUri, String userUri) {
        //return Triple.create(RdfUtils.uri(issueUri), userProperty(), RdfUtils.uri(userUri));
        return Triple.create(RdfUtils.uri(issueUri), userProperty(), RdfUtils.stringLiteral(userUri));
    }

    public static Triple createIssueLabelProperty(String issueUri, String labelUri) {
        //return Triple.create(RdfUtils.uri(issueUri), labelProperty(), RdfUtils.uri(labelUri));
        return Triple.create(RdfUtils.uri(issueUri), labelProperty(), RdfUtils.stringLiteral(labelUri));
    }

    public static Triple createIssueAssigneeProperty(String issueUri, String userUri) {
        //return Triple.create(RdfUtils.uri(issueUri), assigneeProperty(), RdfUtils.uri(userUri));
        return Triple.create(RdfUtils.uri(issueUri), assigneeProperty(), RdfUtils.stringLiteral(userUri));
    }

    public static Triple createIssueMilestoneProperty(String issueUri, String milestoneUri) {
        //return Triple.create(RdfUtils.uri(issueUri), milestoneProperty(), RdfUtils.uri(milestoneUri));
        return Triple.create(RdfUtils.uri(issueUri), milestoneProperty(), RdfUtils.stringLiteral(milestoneUri));
    }

    public static Triple createIssueCreatedAtProperty(String issueUri, LocalDateTime createdAtDateTime) {
        //return Triple.create(RdfUtils.uri(issueUri), createdAtProperty(), RdfUtils.dateTimeLiteral(createdAtDateTime));
        return Triple.create(RdfUtils.uri(issueUri), createdAtProperty(), RdfUtils.stringLiteral(createdAtDateTime.toString()));
    }

    public static Triple createIssueUpdatedAtProperty(String issueUri, LocalDateTime updatedAtDateTime) {
        //return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAtDateTime));
        return Triple.create(RdfUtils.uri(issueUri), updatedAtProperty(), RdfUtils.stringLiteral(updatedAtDateTime.toString()));
    }

    public static Triple createIssueClosedAtProperty(String issueUri, LocalDateTime closedAtDateTime) {
        //return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.dateTimeLiteral(closedAtDateTime));
        return Triple.create(RdfUtils.uri(issueUri), closedAtProperty(), RdfUtils.stringLiteral(closedAtDateTime.toString()));
    }

}
