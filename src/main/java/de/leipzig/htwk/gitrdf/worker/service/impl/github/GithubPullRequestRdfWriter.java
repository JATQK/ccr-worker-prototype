package de.leipzig.htwk.gitrdf.worker.service.impl.github;

import org.apache.jena.riot.system.StreamRDF;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import de.leipzig.htwk.gitrdf.database.common.entity.GithubIssueRepositoryFilter;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils.RdfGithubBranchUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils.RdfGithubCommitListUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils.RdfGithubPullRequestUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
public class GithubPullRequestRdfWriter {

  /**
   * Writes a pull request's standard issue fields (by calling
   * GithubIssueRdfWriter),
   * then writes pull request-specific fields (branch info, commit list, etc.).
   */
  public static void writePullRequest(StreamRDF writer,
      GHIssue ghIssue,
      GHRepository ghRepo,
      String pullRequestUri,
      GithubIssueRepositoryFilter issueFilter) {
    if (ghIssue == null || ghRepo == null || pullRequestUri == null || pullRequestUri.isEmpty()) {
      log.error("Invalid parameters for writing pull request entry.");
      return;
    }
    try {
      // Write rdf:type for the pull request
      writer.triple(RdfGithubPullRequestUtils.createRdfTypeProperty(pullRequestUri));

      // 1) Write standard issue fields
      GithubIssueRdfWriter.writeIssueFields(writer, ghIssue, pullRequestUri, issueFilter);

      // 2) Retrieve pull request object
      GHPullRequest pr = ghRepo.getPullRequest(ghIssue.getNumber());
      if (pr == null) {
        log.warn("No pull request found for issue #{} in repository {}.", ghIssue.getNumber(), ghRepo.getName());
        return;
      }

      // 3) Write pull request-specific fields (e.g., branches)

      // Base branch
      if (pr.getBase() != null && pr.getBase().getRef() != null && !pr.getBase().getRef().isEmpty()) {
        String baseBranchUri = generateBranchUri(ghRepo.getOwnerName(), ghRepo.getName(), pr.getBase().getRef());
        // Write branch type
        writer.triple(RdfGithubBranchUtils.createRdfTypeProperty(baseBranchUri));
        // Write branch name
        writer.triple(RdfGithubBranchUtils.createBranchNameProperty(baseBranchUri,
            RdfUtils.stringLiteral(pr.getBase().getRef())));
        // Write branch repository
        String baseRepo = ghRepo.getOwnerName() + "/" + ghRepo.getName();
        writer.triple(
            RdfGithubBranchUtils.createBranchRepositoryProperty(baseBranchUri, RdfUtils.stringLiteral(baseRepo)));
        // Link pull request to base branch
        writer.triple(RdfGithubPullRequestUtils.createBaseBranchProperty(pullRequestUri, RdfUtils.uri(baseBranchUri)));
      }

      // Head branch
      if (pr.getHead() != null && pr.getHead().getRef() != null && !pr.getHead().getRef().isEmpty()) {
        String headRepoOwner = pr.getHead().getRepository() != null ? pr.getHead().getRepository().getOwnerName()
            : ghRepo.getOwnerName();
        String headRepoName = pr.getHead().getRepository() != null ? pr.getHead().getRepository().getName()
            : ghRepo.getName();
        String headBranchUri = generateBranchUri(headRepoOwner, headRepoName, pr.getHead().getRef());
        // Write branch type
        writer.triple(RdfGithubBranchUtils.createRdfTypeProperty(headBranchUri));
        // Write branch name
        writer.triple(RdfGithubBranchUtils.createBranchNameProperty(headBranchUri,
            RdfUtils.stringLiteral(pr.getHead().getRef())));
        // Write branch repository
        String headRepo = headRepoOwner + "/" + headRepoName;
        writer.triple(
            RdfGithubBranchUtils.createBranchRepositoryProperty(headBranchUri, RdfUtils.stringLiteral(headRepo)));
        // Link pull request to head branch
        writer.triple(RdfGithubPullRequestUtils.createHeadBranchProperty(pullRequestUri, RdfUtils.uri(headBranchUri)));
      }

      // Commit list
      RdfGithubCommitListUtils.writeCommitList(writer, pullRequestUri, pr,
          ghRepo.getOwnerName(), ghRepo.getName());

      // Additional sub-lists (assignees, reviews, etc.) can be added here.

    } catch (IOException e) {
      log.error("IOException while processing pull request {}: {}", ghIssue.getNumber(), e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error while processing pull request {}: {}", ghIssue.getNumber(), e.getMessage());
    }
  }

  /**
   * Generates a URI for a branch based on the repository owner, repository name,
   * and branch name.
   */
  private static String generateBranchUri(String repoOwner, String repoName, String branchName) {
    return String.format("https://github.com/%s/%s/tree/%s", repoOwner, repoName, branchName);
  }
}
