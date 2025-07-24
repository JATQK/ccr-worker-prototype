package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils.uri;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.core.RdfUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.platform.RdfPlatformCommentUtils;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * GitHub-specific utility class for RDF operations on github:GithubComment entities.
 * This class extends RdfPlatformCommentUtils and adds GitHub-specific properties
 * as defined in the git2RDFLab-platform-github ontology.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubCommentUtils extends RdfPlatformCommentUtils {

  private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

  // GitHub-specific comment properties
  public static Node commentHtmlUrlProperty() {
    return uri(GH_NS + "commentUrl");
  }

  public static Node commentUpdatedAtProperty() {
    return uri(GH_NS + "updatedAt");
  }

  public static Node parentCommentProperty() {
    return uri(GH_NS + "parentComment");
  }

  public static Node hasReplyProperty() {
    return uri(GH_NS + "hasReply");
  }

  public static Node replyCountProperty() {
    return uri(GH_NS + "replyCount");
  }

  public static Node threadDepthProperty() {
    return uri(GH_NS + "threadDepth");
  }

  // GitHub-specific relationship to parent entities
  public static Node commentOfProperty() {
    return uri(GH_NS + "commentOf");
  }

  // Comment type classification
  public static Node commentTypeProperty() {
    return uri(GH_NS + "commentType");
  }

  // Additional metadata
  public static Node isEditedProperty() {
    return uri(GH_NS + "isEdited");
  }

  public static Node hasReactionProperty() {
    return uri(GH_NS + "hasReaction");
  }

  public static Node authorAssociationProperty() {
    return uri(GH_NS + "authorAssociation");
  }

  // Override platform method to create GitHub Comment type
  public static Triple createCommentRdfType(String commentUri) {
    return Triple.create(uri(commentUri), rdfTypeProperty(),
        uri("github:GithubComment"));
  }

  // Use inherited platform methods for common properties
  public static Triple createCommentId(String commentUri, long id) {
    return createCommentIdProperty(commentUri, id);
  }

  public static Triple createCommentBody(String commentUri, String body) {
    return createCommentBodyProperty(commentUri, body);
  }

  public static Triple createCommentUser(String commentUri, String userUri) {
    return createCommentAuthorProperty(commentUri, userUri);
  }

  public static Triple createCommentCreatedAt(String commentUri, LocalDateTime createdAt) {
    return createCommentedAtProperty(commentUri, createdAt);
  }

  // GitHub-specific property creation methods

  public static Triple createCommentUpdatedAt(String commentUri, LocalDateTime updatedAt) {
    return Triple.create(uri(commentUri), commentUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
  }


  public static Triple createParentComment(String commentUri, String parentUri) {
    return Triple.create(uri(commentUri), parentCommentProperty(), uri(parentUri));
  }

  public static Triple createHasReply(String commentUri, String replyUri) {
    return Triple.create(uri(commentUri), hasReplyProperty(), uri(replyUri));
  }

  public static Triple createReplyCount(String commentUri, long count) {
    return Triple.create(uri(commentUri), replyCountProperty(), RdfUtils.nonNegativeIntegerLiteral(count));
  }


  public static Triple createThreadDepth(String commentUri, int depth) {
    return Triple.create(uri(commentUri), threadDepthProperty(), RdfUtils.integerLiteral(depth));
  }

  // Relationship methods
  public static Triple createCommentOf(String commentUri, String parentEntityUri) {
    return Triple.create(uri(commentUri), commentOfProperty(), uri(parentEntityUri));
  }

  // Classification methods
  public static Triple createCommentType(String commentUri, String commentType) {
    return Triple.create(uri(commentUri), commentTypeProperty(), RdfUtils.stringLiteral(commentType));
  }

  // Additional metadata methods
  public static Triple createIsEdited(String commentUri, boolean isEdited) {
    return Triple.create(uri(commentUri), isEditedProperty(), RdfUtils.booleanLiteral(isEdited));
  }


  public static Triple createCommentReaction(String commentUri, String reactionUri) {
    return Triple.create(uri(commentUri), hasReactionProperty(), uri(reactionUri));
  }

  public static Triple createAuthorAssociation(String commentUri, String association) {
    return Triple.create(uri(commentUri), authorAssociationProperty(), RdfUtils.stringLiteral(association));
  }

  // Convenience methods for creating complete comment entities
  public static Triple[] createBasicComment(String commentUri, long id, String body, String userUri,
      LocalDateTime createdAt, String parentEntityUri) {
    return new Triple[] {
        createCommentRdfType(commentUri),
        createCommentId(commentUri, id),
        createCommentBody(commentUri, body),
        createCommentUser(commentUri, userUri),
        createCommentCreatedAt(commentUri, createdAt),
        createCommentOf(commentUri, parentEntityUri)
    };
  }

  public static Triple[] createThreadedComment(String commentUri, long id, String body, String userUri,
      LocalDateTime createdAt, String parentEntityUri,
      String parentCommentUri, int threadDepth) {
    return new Triple[] {
        createCommentRdfType(commentUri),
        createCommentId(commentUri, id),
        createCommentBody(commentUri, body),
        createCommentUser(commentUri, userUri),
        createCommentCreatedAt(commentUri, createdAt),
        createCommentOf(commentUri, parentEntityUri),
        createParentComment(commentUri, parentCommentUri),
        createThreadDepth(commentUri, threadDepth)
    };
  }
}