package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.PLATFORM_GITHUB_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfGithubCommentUtils {

  private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";

  // Core RDF properties
  public static Node rdfTypeProperty() {
    return RdfUtils.uri("rdf:type");
  }

  // Comment identification and basic properties
  public static Node commentIdProperty() {
    return uri(GH_NS + "commentId");
  }

  public static Node commentHtmlUrlProperty() {
    return uri(GH_NS + "commentHtmlUrl");
  }

  public static Node commentBodyProperty() {
    return uri(GH_NS + "commentBody");
  }

  public static Node commentUserProperty() {
    return uri(GH_NS + "user");
  }

  public static Node commentCreatedAtProperty() {
    return uri(GH_NS + "submittedAt");
  }

  public static Node commentUpdatedAtProperty() {
    return uri(GH_NS + "updatedAt");
  }

  // Threading and hierarchy properties
  public static Node isRootCommentProperty() {
    return uri(GH_NS + "isRootComment");
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

  // Relationship to parent entities
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

  public static Node reactionCountProperty() {
    return uri(GH_NS + "reactionCount");
  }

  public static Node authorAssociationProperty() {
    return uri(GH_NS + "authorAssociation");
  }

  // RDF Type creation method - single unified type
  public static Triple createCommentRdfType(String commentUri) {
    return Triple.create(uri(commentUri), rdfTypeProperty(),
        uri("github:GithubComment"));
  }

  // Basic property creation methods
  public static Triple createCommentId(String commentUri, long id) {
    return Triple.create(uri(commentUri), commentIdProperty(), RdfUtils.longLiteral(id));
  }

  public static Triple createCommentHtmlUrl(String commentUri, String htmlUrl) {
    return Triple.create(uri(commentUri), commentHtmlUrlProperty(), uri(htmlUrl));
  }

  public static Triple createCommentBody(String commentUri, String body) {
    return Triple.create(uri(commentUri), commentBodyProperty(), RdfUtils.stringLiteral(body));
  }

  public static Triple createCommentUser(String commentUri, String userUri) {
    return Triple.create(uri(commentUri), commentUserProperty(), uri(userUri));
  }

  public static Triple createCommentCreatedAt(String commentUri, LocalDateTime createdAt) {
    return Triple.create(uri(commentUri), commentCreatedAtProperty(), RdfUtils.dateTimeLiteral(createdAt));
  }

  public static Triple createCommentUpdatedAt(String commentUri, LocalDateTime updatedAt) {
    return Triple.create(uri(commentUri), commentUpdatedAtProperty(), RdfUtils.dateTimeLiteral(updatedAt));
  }

  // Threading and hierarchy methods
  public static Triple createIsRootComment(String commentUri, boolean isRoot) {
    return Triple.create(uri(commentUri), isRootCommentProperty(), RdfUtils.booleanLiteral(isRoot));
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

  public static Triple createReactionCount(String commentUri, long reactionCount) {
    return Triple.create(uri(commentUri), reactionCountProperty(), RdfUtils.nonNegativeIntegerLiteral(reactionCount));
  }

  public static Triple createAuthorAssociation(String commentUri, String association) {
    return Triple.create(uri(commentUri), authorAssociationProperty(), RdfUtils.stringLiteral(association));
  }

  // Convenience methods for creating complete comment entities
  public static Triple[] createBasicComment(String commentUri, long id, String body, String userUri,
      LocalDateTime createdAt, String parentEntityUri, boolean isRoot) {
    return new Triple[] {
        createCommentRdfType(commentUri),
        createCommentId(commentUri, id),
        createCommentBody(commentUri, body),
        createCommentUser(commentUri, userUri),
        createCommentCreatedAt(commentUri, createdAt),
        createCommentOf(commentUri, parentEntityUri),
        createIsRootComment(commentUri, isRoot)
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
        createIsRootComment(commentUri, false),
        createThreadDepth(commentUri, threadDepth)
    };
  }
}