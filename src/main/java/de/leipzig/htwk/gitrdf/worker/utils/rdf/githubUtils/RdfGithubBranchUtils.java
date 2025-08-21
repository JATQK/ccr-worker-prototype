package de.leipzig.htwk.gitrdf.worker.utils.rdf.githubUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubBranchUtils {

  private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

  // Base-Classes - Platform
  public static Node rdfTypeProperty() {
    return RdfUtils.uri("rdf:type");
  }

  public static Node branchNameProperty() {
    return RdfUtils.uri(GH_NS + "branchName");
  }

  public static Node branchRepositoryProperty() {
    return RdfUtils.uri(GH_NS + "branchRepository");
  }

  // Existing Triple Creator Methods
  public static Triple createRdfTypeProperty(String branchUri) {
    return Triple.create(RdfUtils.uri(branchUri), rdfTypeProperty(), RdfUtils.uri("github:Branch"));
  }

  public static Triple createBranchNameProperty(String pullRequestUri, Node branchName) {
    return Triple.create(RdfUtils.uri(pullRequestUri), branchNameProperty(), branchName);
  }

  public static Triple createBranchRepositoryProperty(String pullRequestUri, Node branchRepository) {
    return Triple.create(RdfUtils.uri(pullRequestUri), branchRepositoryProperty(), branchRepository);
  }
}