package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformCommentUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:Comment entities.
 * This class extends RdfPlatformCommentUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubCommentUtils extends RdfPlatformCommentUtils {

  private static final String PLATFORM_NS = PLATFORM_NAMESPACE + ":";

  // Use platform properties from v2 ontology
  public static Node hasCommentProperty() {
    return uri(PLATFORM_NS + "hasComment");
  }

  public static Node commentOfProperty() {
    return uri(PLATFORM_NS + "commentOf");
  }



  // Override platform method to create GitHub Comment type
  public static Triple createCommentRdfType(String commentUri) {
    return Triple.create(uri(commentUri), rdfTypeProperty(),
        uri("github:Comment"));
  }

  // Use inherited platform methods for common properties
  public static Triple createCommentId(String commentUri, String id) {
    return createIdProperty(commentUri, id);
  }

  public static Triple createCommentId(String commentUri, long id) {
    return createIdProperty(commentUri, String.valueOf(id));
  }

  public static Triple createCommentBody(String commentUri, String body) {
    return createBodyProperty(commentUri, body);
  }

  public static Triple createCommentUser(String commentUri, String userUri) {
    return createAuthorProperty(commentUri, userUri);
  }

  public static Triple createCommentCreatedAt(String commentUri, LocalDateTime createdAt) {
    return createCreatedAtProperty(commentUri, createdAt);
  }

  public static Triple createCommentUpdatedAt(String commentUri, LocalDateTime updatedAt) {
    return createUpdatedAtProperty(commentUri, updatedAt);
  }

  public static Triple createCommentUrl(String commentUri, String url) {
    return createUrlProperty(commentUri, url);
  }

  public static Triple createCommentApiUrl(String commentUri, String apiUrl) {
    return Triple.create(uri(commentUri), uri(PLATFORM_NS + "apiUrl"), RdfUtils.stringLiteral(apiUrl));
  }

  // Relationship creation methods

  public static Triple createCommentOf(String commentUri, String parentEntityUri) {
    return Triple.create(uri(commentUri), commentOfProperty(), uri(parentEntityUri));
  }

  public static Triple createCommentReaction(String commentUri, String reactionUri) {
    return Triple.create(uri(commentUri), hasReactionProperty(), uri(reactionUri));
  }

  public static Triple createHasCommentProperty(String parentEntityUri, String commentUri) {
    return Triple.create(uri(parentEntityUri), hasCommentProperty(), uri(commentUri));
  }


  // Convenience methods for creating complete comment entities
  public static Triple[] createBasicComment(String commentUri, String id, String body, String userUri,
      LocalDateTime createdAt) {
    return new Triple[] {
        createCommentRdfType(commentUri),
        createCommentId(commentUri, id),
        createCommentBody(commentUri, body),
        createCommentUser(commentUri, userUri),
        createCommentCreatedAt(commentUri, createdAt)
    };
  }

  public static Triple[] createBasicComment(String commentUri, long id, String body, String userUri,
      LocalDateTime createdAt) {
    return createBasicComment(commentUri, String.valueOf(id), body, userUri, createdAt);
  }
}