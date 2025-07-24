package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.git.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformRepositoryUtils;

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
        return RdfPlatformRepositoryUtils.repositoryNameProperty();
    }

    public static Node repositoryOwnerProperty() {
        return RdfPlatformRepositoryUtils.repositoryOwnerProperty();
    }

    public static Node commitGitHubUserProperty() {
        return uri(PLATFORM_NS + "submitter");
    }

    // Removed - use partOfIssueProperty() instead for proper GitHub ontology compliance

    public static Node partOfIssueProperty() {
        return uri(GITHUB_NS + "partOfIssue");
    }

    public static Node partOfPullRequestProperty() {
        return uri(GITHUB_NS + "partOfPullRequest");
    }

    public static Node isMergedProperty() {
        return uri(PLATFORM_NS + "merged");
    }

    public static Node mergedAtProperty() {
        return uri(PLATFORM_NS + "mergedAt");
    }

    public static Node mergedIntoIssueProperty() {
        return uri(GITHUB_NS + "mergedIntoIssue");
    }

    public static Triple createRepositoryRdfTypeProperty(String repoUri) {
        return Triple.create(RdfUtils.uri(repoUri), RdfCommitUtils.rdfTypeProperty(), RdfUtils.uri(GITHUB_NS + "GithubRepository"));
    }

    public static Triple createRepositoryOwnerProperty(String repoUri, String ownerUri) {
        return RdfPlatformRepositoryUtils.createRepositoryOwnerProperty(repoUri, ownerUri);
    }

    public static Triple createRepositoryNameProperty(String repoUri, String repositoryName) {
        return RdfPlatformRepositoryUtils.createRepositoryNameProperty(repoUri, repositoryName);
    }

    public static Triple createCommiterGitHubUserProperty(String commitUri, String commiterGitHubUser) {
        return Triple.create(uri(commitUri), commitGitHubUserProperty(), uri(commiterGitHubUser));
    }

    public static Triple createCommitIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), partOfIssueProperty(), uri(issueUri));
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