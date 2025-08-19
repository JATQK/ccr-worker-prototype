package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.git.RdfCommitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformRepositoryUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubCommitUtils {


    // Use platform properties for repository relationships
    public static Node partOfTicketProperty() {
        return uri("platform:partOfTicket");
    }


    // Use git ontology for repository type
    public static Triple createRepositoryRdfTypeProperty(String repoUri) {
        return Triple.create(uri(repoUri), RdfCommitUtils.rdfTypeProperty(), uri("github:Repository"));
    }

    // Delegate to platform utils for common repository properties
    public static Triple createRepositoryOwnerProperty(String repoUri, String ownerUri) {
        return RdfPlatformRepositoryUtils.createOwnerProperty(repoUri, ownerUri);
    }

    public static Triple createRepositoryNameProperty(String repoUri, String repositoryName) {
        return RdfPlatformRepositoryUtils.createNameProperty(repoUri, repositoryName);
    }

    // Commit-ticket relationships using platform ontology
    public static Triple createCommitPartOfTicketProperty(String commitUri, String ticketUri) {
        return Triple.create(uri(commitUri), partOfTicketProperty(), uri(ticketUri));
    }

    // Convenience methods for backward compatibility
    public static Triple createCommitPartOfIssueProperty(String commitUri, String issueUri) {
        return createCommitPartOfTicketProperty(commitUri, issueUri);
    }

    public static Triple createCommitPartOfPullRequestProperty(String commitUri, String prUri) {
        return createCommitPartOfTicketProperty(commitUri, prUri);
    }

    // GitHub-specific commit properties
    public static Node isMergedProperty() {
        return uri("github:isMerged");
    }

    public static Node mergedAtProperty() {
        return uri("github:mergedAt");
    }

    public static Node mergedIntoProperty() {
        return uri("github:mergedInto");
    }

    public static Node committerProperty() {
        return uri("git:committer");
    }

    public static Node issueReferencedByProperty() {
        return uri("github:issueReferencedBy");
    }

    // Methods for the missing commit properties
    public static Triple createCommitIsMergedProperty(String commitUri, boolean isMerged) {
        return Triple.create(uri(commitUri), isMergedProperty(), RdfUtils.booleanLiteral(isMerged));
    }

    public static Triple createCommitMergedAtProperty(String commitUri, LocalDateTime mergedAt) {
        return Triple.create(uri(commitUri), mergedAtProperty(), RdfUtils.dateTimeLiteral(mergedAt));
    }

    public static Triple createCommitMergedIntoIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), mergedIntoProperty(), uri(issueUri));
    }

    public static Triple createCommiterGitHubUserProperty(String commitUri, String userUri) {
        return Triple.create(uri(commitUri), committerProperty(), uri(userUri));
    }

    public static Triple createCommitIssueProperty(String commitUri, String issueUri) {
        return Triple.create(uri(commitUri), issueReferencedByProperty(), uri(issueUri));
    }
}