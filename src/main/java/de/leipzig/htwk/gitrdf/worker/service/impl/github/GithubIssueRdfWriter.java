package de.leipzig.htwk.gitrdf.worker.service.impl.github;

import org.apache.jena.riot.system.StreamRDF;
import org.kohsuke.github.GHIssue;

import de.leipzig.htwk.gitrdf.database.common.entity.GithubIssueRepositoryFilter;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils.RdfGithubIssueUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
public class GithubIssueRdfWriter {

  public static void writeIssue(StreamRDF writer,
      GHIssue ghIssue,
      String issueUri,
      GithubIssueRepositoryFilter filter) {
    if (ghIssue == null || writer == null || issueUri == null || issueUri.isEmpty()) {
      log.error("Invalid parameters for writing GitHub issue.");
      return;
    }
    // (Example: always write rdf:type)
    writer.triple(RdfGithubIssueUtils.createRdfTypeProperty(issueUri));

    writeIssueFields(writer, ghIssue, issueUri, filter);
  }

  public static void writeIssueFields(StreamRDF writer, GHIssue ghIssue, String issueUri,
      GithubIssueRepositoryFilter issueFilter) {
    // Basic null checks
    if (ghIssue == null || issueUri == null || issueUri.isEmpty() || writer == null) {
      log.error("Invalid parameters for writing GitHub issue to RDF.");
      return;
    }

    if (issueFilter.isEnableIssueId()) {
      writer.triple(RdfGithubIssueUtils.createIssueIdProperty(issueUri, ghIssue.getId()));
    }
    if (issueFilter.isEnableIssueNumber()) {
      writer.triple(RdfGithubIssueUtils.createIssueNumberProperty(issueUri, ghIssue.getNumber()));
    }

    if (issueFilter.isEnableIssueTitle() && ghIssue.getTitle() != null) {
      writer.triple(RdfGithubIssueUtils.createIssueTitleProperty(issueUri, ghIssue.getTitle()));
    }
    if (issueFilter.isEnableIssueBody() && ghIssue.getBody() != null) {
      writer.triple(RdfGithubIssueUtils.createIssueBodyProperty(issueUri, ghIssue.getBody()));
    }
    if (issueFilter.isEnableIssueState() && ghIssue.getState() != null) {
      writer.triple(RdfGithubIssueUtils.createIssueStateProperty(issueUri, ghIssue.getState().toString()));
    }

    // Write the issue creator
    try {
      if (issueFilter.isEnableIssueUser() && ghIssue.getUser() != null && ghIssue.getUser().getHtmlUrl() != null) {
        writer.triple(RdfGithubIssueUtils.createIssueCreatedByProperty(
            issueUri, ghIssue.getUser().getHtmlUrl().toString()));
      }
    } catch (IOException e) {
      log.error("Error while getting user HTML URL", e);
    }

    try {
      if (ghIssue.getClosedBy() != null && ghIssue.getClosedBy().getHtmlUrl() != null) {
        writer.triple(RdfGithubIssueUtils.createIssueUpdatedByProperty(
            issueUri, ghIssue.getClosedBy().getHtmlUrl().toString()));
      }
    } catch (IOException e) {
      log.error("Error while getting closedBy HTML URL", e);
    }

    // Timestamps
    try {
      if (issueFilter.isEnableIssueCreatedAt() && ghIssue.getCreatedAt() != null) {
        LocalDateTime created = localDateTimeFrom(ghIssue.getCreatedAt());
        writer.triple(RdfGithubIssueUtils.createIssueCreatedAtProperty(issueUri, created));
      }
    } catch (IOException e) {
      log.error("Error while getting issue creation date", e);
    }
    try {
      if (issueFilter.isEnableIssueUpdatedAt() && ghIssue.getUpdatedAt() != null) {
        LocalDateTime updated = localDateTimeFrom(ghIssue.getUpdatedAt());
        writer.triple(RdfGithubIssueUtils.createIssueUpdatedAtProperty(issueUri, updated));
      }
    } catch (IOException e) {
      log.error("Error while getting issue update date", e);
    }
    if (issueFilter.isEnableIssueClosedAt() && ghIssue.getClosedAt() != null) {
      LocalDateTime closed = localDateTimeFrom(ghIssue.getClosedAt());
      writer.triple(RdfGithubIssueUtils.createIssueClosedAtProperty(issueUri, closed));
    }

    // // Labels
    // if (issueFilter.isEnableIssueLabels()) {
    // for (var label : ghIssue.getLabels()) {
    // writer.triple(RdfGithubIssueUtils.createIssueLabelProperty(issueUri,
    // label.getUrl()));
    // }
    // }

    // // Assignees
    // if (issueFilter.isEnableIssueAssignees()) {
    // for (var assignee : ghIssue.getAssignees()) {
    // if (assignee != null && assignee.getHtmlUrl() != null) {
    // writer.triple(RdfGithubIssueUtils.createIssueAssigneeProperty(issueUri,
    // assignee.getHtmlUrl().toString()));
    // }
    // }
    // }

    // // Milestone
    // if (issueFilter.isEnableIssueMilestone() && ghIssue.getMilestone() != null) {
    // writer.triple(
    // RdfGithubIssueUtils.createIssueMilestoneProperty(issueUri,
    // ghIssue.getMilestone().getHtmlUrl().toString()));
    // }
  }

  private static LocalDateTime localDateTimeFrom(java.util.Date date) {
    // same logic you used before in your service
    return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
  }
}
