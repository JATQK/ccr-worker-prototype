package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.stringLiteral;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubCommitUtils {

    private static final String GITHUB_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

    public static Node repositoryNameProperty() {
        return uri(GITHUB_NS + "repositoryName");
    }

    public static Node repositoryOwnerProperty() {
        return uri(GITHUB_NS + "repositoryOwner");
    }

    public static Node commitGitHubUserProperty() {
        return uri(GITHUB_NS + "user");
    }

    public static Node commitIssueProperty() {
        return uri(GITHUB_NS + "issue");
    }

    public static Node partOfIssueProperty() {
        return uri(GITHUB_NS + "partOfIssue");
    }

    public static Node partOfPullRequestProperty() {
        return uri(GITHUB_NS + "partOfPullRequest");
    }

    public static Node isMergedProperty() {
        return uri(GITHUB_NS + "isMerged");
    }

    public static Node mergedAtProperty() {
        return uri(GITHUB_NS + "mergedAt");
    }

    public static Node mergedIntoIssueProperty() {
        return uri(GITHUB_NS + "mergedIntoIssue");
    }

    public static Triple createRepositoryRdfTypeProperty(String repoUri) {
        return Triple.create(RdfUtils.uri(repoUri), RdfCommitUtils.rdfTypeProperty(), RdfUtils.uri(GITHUB_NS + "GitRepository"));
    }

    public static Triple createRepositoryOwnerProperty(String repoUri, String ownerUri) {
        return Triple.create(uri(repoUri), repositoryOwnerProperty(), uri(ownerUri));
    }

    public static Triple createRepositoryNameProperty(String repoUri, String repositoryName) {
        return Triple.create(uri(repoUri), repositoryNameProperty(), stringLiteral(repositoryName));
    }

    public static Triple createCommiterGitHubUserProperty(String commitUri, String commiterGitHubUser) {
        return Triple.create(uri(commitUri), commitGitHubUserProperty(), uri(commiterGitHubUser));
    }

    public static Triple createCommitIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), commitIssueProperty(), uri(issueUri));
    }

    public static Triple createCommitPartOfIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), partOfIssueProperty(), uri(issueUri));
    }

    public static Triple createCommitPartOfPullRequestProperty(String commitUri, String prUri) {
        return Triple.create(uri(commitUri), partOfPullRequestProperty(), uri(prUri));
    }

    public static Triple createCommitIsMergedProperty(String commitUri, boolean merged) {
        return Triple.create(uri(commitUri), isMergedProperty(), RdfUtils.booleanLiteral(merged));
    }

    public static Triple createCommitMergedAtProperty(String commitUri, LocalDateTime mergedAt) {
        return Triple.create(uri(commitUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createCommitMergedIntoIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), mergedIntoIssueProperty(), uri(issueUri));
    }
}