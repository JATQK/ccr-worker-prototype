package de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils;

public class RdfGithubUriUtils {

  public static String getGithubRepositoryUri(String owner, String repository) {
    return "https://github.com/" + owner + "/" + repository + "/";
  }

  public static String getGithubCommitBaseUri(String owner, String repository) {
    return "https://github.com/" + owner + "/" + repository + "/commit/";
  }

  public static String getGithubCommitUri(String owner, String repository, String commitHash) {
    return getGithubCommitBaseUri(owner, repository) + commitHash;
  }

  public static String getGithubIssueBaseUri(String owner, String repository) {
    return "https://github.com/" + owner + "/" + repository + "/issues/";
  }

  public static String getGithubIssueUri(String owner, String repository, long issueId) {
    return getGithubIssueBaseUri(owner, repository) + issueId;
  }

  public static String getGithubUserUri(String userName) {
    return "https://github.com/" + userName;
  }
}
