package de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils;

import java.io.IOException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RdfGithubCommitListUtils {

    /**
     * Writes an ordered list (rdf:Seq) of commit URIs for the pull request.
     * Each commit URI is computed from the commit SHA along with the repository
     * owner and name.
     */
    public static void writeCommitList(StreamRDF writer,
            String pullRequestUri,
            GHPullRequest pr,
            String ownerName,
            String repositoryName) {
        if (writer == null || pullRequestUri == null || pullRequestUri.isEmpty() || pr == null) {
            log.error("Invalid parameters for writing commit list.");
            return;
        }

        // Create a blank node to serve as the RDF container.
        Node commitListContainer = NodeFactory.createBlankNode();

        // Assert that the container is an rdf:Seq.
        writer.triple(Triple.create(commitListContainer, RdfUtils.uri("rdf:type"), RdfUtils.uri("rdf:Seq")));

        // Link the pull request to this container using your pull request property.
        writer.triple(RdfGithubPullRequestUtils.createCommitListProperty(pullRequestUri, commitListContainer));

        int index = 1;
        // Iterate over all commits of the pull request.
        for (GHPullRequestCommitDetail commit : pr.listCommits()) {
            String commitSha = commit.getSha();
            if (commitSha == null || commitSha.isEmpty()) {
                log.warn("Skipping commit with empty SHA in pull request {}.", pullRequestUri);
                continue;
            }
            // Build the canonical commit URI (for example:
            // "https://github.com/microsoft/openvmm/commit/<sha>")
            String commitUri = "https://github.com/" + ownerName + "/" + repositoryName + "/commit/" + commitSha;
            if (commitUri == null || commitUri.isEmpty()) {
                log.warn("Commit URI is null/empty for SHA {} in pull request {}.", commitSha, pullRequestUri);
                continue;
            }

            // Build the container membership property: rdf:_1, rdf:_2, etc.
            Node containerIndexProperty = RdfUtils.uri("rdf:_" + index);
            writer.triple(Triple.create(commitListContainer, containerIndexProperty, RdfUtils.uri(commitUri)));
            index++;
        }
    }

    // Helper method to compute a canonical commit URI.
    private static String getGithubCommitUri(String owner, String repository, String commitSha) {
        return "https://github.com/" + owner + "/" + repository + "/commit/" + commitSha;
    }
}
