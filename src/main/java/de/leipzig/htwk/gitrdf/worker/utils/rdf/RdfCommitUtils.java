package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;

import java.time.LocalDateTime;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfCommitUtils {

    private static final String NS = GIT_NAMESPACE + "://";
    
    // org.apache.jena.datatypes.xsd.XSDDatatype -> static xsd Datatype collection from apache jena

    // somewhat of an applicable base uri to the contents of git: https://git-scm.com/docs/gitglossary
    
    public static Node commitHashProperty() {
        return uri("https://git-scm.com/docs/gitglossary#Documentation/gitglossary.txt-aiddefSHA1aSHA-1");
    }
    
    public static Node authorNameProperty() {
       return uri(NS + "hasAuthorName");
    }

    public static Node authorEmailProperty() {
        return uri(NS + "hasAuthorEmail");
    }

    public static Node authorDateProperty() {
        return uri(NS + "hasAuthorDate");
    }

    public static Node commitDateProperty() {
        return uri(NS + "hasCommitDate");
    }

    public static Node committerNameProperty() {
        return uri(NS + "hasCommitterName");
    }

    public static Node committerEmailProperty() {
        return uri(NS + "hasCommitterEmail");
    }

    public static Node commitMessageProperty() {
        return uri(NS + "hasCommitMessage");
    }

    public static Node commitBranchNameProperty() {
        return uri(NS + "hasBranchName");
    }

    public static Node commitDiffEntryEditTypeProperty() {
        return uri(NS + "hasChangeType");
    }

    public static Node commitResource() {
        return uri(NS + "hasCommit");
    }

    public static Node branchResource() {
        return uri(NS + "hasBranch");
    }

    public static Node tagResource() {
        return uri(NS + "hasTag");
    }

    public static Node commitDiffEntryResource() {
        return uri(NS + "hasCommitDiffEntry");
    }

    public static Node commitDiffEntryOldFileNameProperty() {
        return uri(NS + "hasCommitDiffEntryOldFileName");
    }

    public static Node commitDiffEntryNewFileNameProperty() {
        return uri(NS + "hasCommitDiffEntryNewFileName");
    }

    public static Node commitDiffEditResource() {
        return uri(NS + "hasCommitDiffEdit");
    }

    public static Node commitDiffEditTypeProperty() {
        return uri(NS + "hasCommitDiffEditType");
    }

    public static Node editOldLineNumberBeginProperty() {
        return uri(NS + "hasEditOldLineNumberBegin");
    }

    public static Node editNewLineNumberBeginProperty() {
        return uri(NS + "hasEditNewLineNumberBegin");
    }

    public static Node editOldLineNumberEndProperty() {
        return uri(NS + "hasEditOldLineNumberEnd");
    }

    public static Node editNewLineNumberEndProperty() {
        return uri(NS + "hasEditNewLineNumberEnd");
    }

    public static Triple createCommitHashProperty(String commitUri, String commitHash) {
        return Triple.create(uri(commitUri), commitHashProperty(), stringLiteral(commitHash));
    }

    public static Triple createAuthorNameProperty(String commitUri, String authorNameValue) {
        return Triple.create(uri(commitUri), authorNameProperty(), stringLiteral(authorNameValue));
    }

    public static Triple createAuthorEmailProperty(String commitUri, String authorEmailValue) {
        return Triple.create(uri(commitUri), authorEmailProperty(), stringLiteral(authorEmailValue));
    }

    public static Triple createAuthorDateProperty(String commitUri, LocalDateTime authorDateTimeValue) {
        return Triple.create(uri(commitUri), authorDateProperty(), dateTimeLiteral(authorDateTimeValue));
    }

    public static Triple createCommitDateProperty(String commitUri, LocalDateTime commitDateTimeValue) {
        return Triple.create(uri(commitUri), commitDateProperty(), dateTimeLiteral(commitDateTimeValue));
    }

    public static Triple createCommitterNameProperty(String commitUri, String committerNameValue) {
        return Triple.create(uri(commitUri), committerNameProperty(), stringLiteral(committerNameValue));
    }

    public static Triple createCommitterEmailProperty(String commitUri, String committerEmailValue) {
        return Triple.create(uri(commitUri), committerEmailProperty(), stringLiteral(committerEmailValue));
    }

    public static Triple createCommitMessageProperty(String commitUri, String commitMessageValue) {
        return Triple.create(uri(commitUri), commitMessageProperty(), stringLiteral(commitMessageValue));
    }

    public static Triple createCommitDiffEntryEditTypeProperty(Node diffEntryNode, DiffEntry.ChangeType changeType) {
        return Triple.create(diffEntryNode, commitDiffEntryEditTypeProperty(), stringLiteral(changeType.toString())); // TODO: changetype-literal?
    }

    public static Triple createCommitResource(String commitUri, Node commitNode) {
        return Triple.create(uri(commitUri), commitResource(), commitNode);
    }

    public static Triple createCommitDiffEntryResource(Node commitNode, Node diffEntryNode) {
        return Triple.create(commitNode, commitDiffEntryResource(), diffEntryNode);
    }

    public static Triple createCommitDiffEntryProperty(String commitUri, Node diffEntryNode) {
        return Triple.create(uri(commitUri), commitDiffEntryResource(), diffEntryNode);
    }

    public static Triple createCommitDiffEntryOldFileNameProperty(Node diffEntryNode, FileHeader fileHeader) {
        return Triple.create(diffEntryNode, commitDiffEntryOldFileNameProperty(), stringLiteral(fileHeader.getOldPath()));
    }

    public static Triple createCommitDiffEntryNewFileNameProperty(Node diffEntryNode, FileHeader fileHeader) {
        return Triple.create(diffEntryNode, commitDiffEntryNewFileNameProperty(), stringLiteral(fileHeader.getNewPath()));
    }

    public static Triple createCommitDiffEditResource(Node diffEntryNode, Node diffEditNode) {
        return Triple.create(diffEntryNode, commitDiffEditResource(), diffEditNode);
    }

    public static Triple createCommitDiffEditTypeProperty(Node editNode, Edit.Type editType) {
        return Triple.create(editNode, commitDiffEditTypeProperty(), stringLiteral(editType.toString())); // TODO: edittype-literal?
    }

    public static Triple createEditOldLineNumberBeginProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, editOldLineNumberBeginProperty(), longLiteral(lineNumberBegin));
    }

    public static Triple createEditNewLineNumberBeginProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, editNewLineNumberBeginProperty(), longLiteral(lineNumberBegin));
    }

    public static Triple createEditOldLineNumberEndProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, editOldLineNumberEndProperty(), longLiteral(lineNumberEnd));
    }

    public static Triple createEditNewLineNumberEndProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, editNewLineNumberEndProperty(), longLiteral(lineNumberEnd));
    }

    public static Triple createCommitBranchNameProperty(String commitUri, String branchName) {
        return Triple.create(uri(commitUri), commitBranchNameProperty(), stringLiteral(branchName));
    }

    public static Triple createBranchResource(Node branchNode, String branchName) {
        return Triple.create(branchNode, branchResource(), stringLiteral(branchName));
    }

    public static Triple createTagResource(Node tagNode, String tagName) {
        return Triple.create(tagNode, tagResource(), stringLiteral(tagName));
    }
}
