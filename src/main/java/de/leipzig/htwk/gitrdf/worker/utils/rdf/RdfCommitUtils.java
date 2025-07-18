package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.GIT_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.OWL_SCHEMA_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.RDF_SCHEMA_NAMESPACE;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.dateTimeLiteral;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.stringLiteral;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.uri;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RdfCommitUtils {

    protected static final String GIT_NS = GIT_NAMESPACE + ":";
    protected static final String RDF_NS = RDF_SCHEMA_NAMESPACE + ":";
    protected static final String OWL_NS = OWL_SCHEMA_NAMESPACE + ":";

    public static Node rdfTypeProperty() {
        return RdfUtils.uri(RDF_NS + "type");
    }

    public static Node repositoryEncodingProperty() {
        return uri(GIT_NS + "encoding");
    }

    private static Node rdfSubmoduleProperty() { return uri(GIT_NS + "hasSubmodule"); }

    public static Node rdfSubmodulePathProperty() {
        return uri(GIT_NS + "submodulePath");
    }

    public static Node rdfSubmoduleCommitProperty() {
        return uri(GIT_NS + "submoduleHash");
    }

    public static Node rdfSubmoduleCommitEntryProperty() {
        return uri(GIT_NS + "submoduleCommitUri");
    }

    public static Node rdfSubmoduleRepositoryEntryProperty() {
        return uri(GIT_NS + "submoduleRepositoryUri");
    }

    public static Node commitHashProperty() { return uri(GIT_NS + "hash"); }
    
    public static Node authorNameProperty() {
       return uri(GIT_NS + "authorName");
    }

    public static Node authorEmailProperty() {
        return uri(GIT_NS + "authorEmail");
    }

    public static Node authorDateProperty() {
        return uri(GIT_NS + "authorDate");
    }

    public static Node commitDateProperty() {
        return uri(GIT_NS + "commitDate");
    }

    public static Node committerNameProperty() {
        return uri(GIT_NS + "committerName");
    }

    public static Node committerEmailProperty() {
        return uri(GIT_NS + "committerEmail");
    }

    public static Node commitMessageProperty() {
        return uri(GIT_NS + "message");
    }

    public static Node commitBranchNameProperty() {
        return uri(GIT_NS + "commitBranchName");
    }

    public static Node inBranchProperty() { return uri(GIT_NS + "inBranch"); }

    public static Node hasTagProperty() {
        return uri(GIT_NS + "hasTag");
    }

    public static Node repositoryHasCommitProperty() { return uri(GIT_NS + "hasCommit"); }

    public static Node repositoryHasBranchProperty() { return uri(GIT_NS + "hasBranch"); }

    public static Node branchHeadCommitProperty() { return uri(GIT_NS + "headCommit"); }

    public static Node tagPointsToProperty() { return uri(GIT_NS + "pointsTo"); }

    public static Node owlSameAsProperty() { return uri(OWL_NS + "sameAs"); }

    public static Node commitDiffEntryEditTypeProperty() {
        return uri(GIT_NS + "changeType");
    }

    public static Node commitResource() {
        return uri(GIT_NS + "commit");
    }

    public static Node branchResource() {
        return uri(GIT_NS + "branch");
    }

    public static Node isMergeCommitProperty() {
        return uri(GIT_NS + "isMergeCommit");
    }

    public static Node hasParentProperty() {
        return uri(GIT_NS + "hasParent");
    }
    public static Node tagResource() {
        return uri(GIT_NS + "tag");
    }

    public static Node commitDiffEntryResource() {
        return uri(GIT_NS + "hasDiffEntry");
    }

    public static Node commitDiffEntryOldFileNameProperty() {
        return uri(GIT_NS + "oldFileName");
    }

    public static Node commitDiffEntryNewFileNameProperty() {
        return uri(GIT_NS + "newFileName");
    }

    public static Node commitDiffEditResource() {
        return uri(GIT_NS + "hasEdit");
    }

    public static Node commitDiffEditTypeProperty() {
        return uri(GIT_NS + "editType");
    }

    public static Node editOldLinenumberBeginProperty() {
        return uri(GIT_NS + "oldLineStart");
    }

    public static Node editNewLinenumberBeginProperty() {
        return uri(GIT_NS + "newLineStart");
    }

    public static Node editOldLinenumberEndProperty() {
        return uri(GIT_NS + "oldLineEnd");
    }

    public static Node editNewLinenumberEndProperty() {
        return uri(GIT_NS + "newLineEnd");
    }

    public static Node branchSnapshotProperty() {
        return uri(GIT_NS + "branchSnapshot");
    }

    public static Node branchSnapshotLineEntryProperty() {
        return uri(GIT_NS + "hasLineEntry");
    }

    public static Node branchSnapshotFileEntryProperty() {
        return uri(GIT_NS + "hasFileEntry");
    }

    public static Node branchSnapshotLineProperty() {
        return uri(GIT_NS + "lineNumber");
    }

    public static Node branchSnapshotLinenumberBeginProperty() {
        return uri(GIT_NS + "branchSnapshotLinenumberBegin");
    }

    public static Node branchSnapshotLinenumberEndProperty() {
        return uri(GIT_NS + "branchSnapshotLinenumberEnd");
    }


    public static Node branchSnapshotFilenameProperty() {
        return uri(GIT_NS + "fileName");
    }

    public static Node branchSnapshotCommitHashProperty() {
        return uri(GIT_NS + "lineCommitHash");
    }

    public static Node branchSnapshotDateProperty() {
        return uri(GIT_NS + "snapshotDate");
    }

    public static Node commitTagNameProperty() {
        return uri(GIT_NS + "tagName");
    }

    public static Node branchNameProperty() { return uri(GIT_NS + "branchName"); }



    // Branch Snapshot

    public static Triple createBranchSnapshotProperty(Node snapshotNode) {
        return Triple.create(snapshotNode, rdfTypeProperty(), RdfUtils.uri(GIT_NS + "BranchSnapshot" ));
    }

    public static Triple createBranchSnapshotDateProperty(Node snapshotNode, LocalDateTime dateTimeValue) {
        return Triple.create(snapshotNode, branchSnapshotDateProperty(), stringLiteral(dateTimeValue.toString()));
    }

    public static Triple createBranchSnapshotFilenameProperty(Node snapshotFileEntryNode, String filename) {
        return Triple.create(snapshotFileEntryNode, branchSnapshotFilenameProperty(), stringLiteral(filename));
    }

    public static Triple createBranchSnapshotFileEntryProperty(Node snapshotNode, Node snapshotFileEntryNode) {
        return Triple.create(snapshotNode, branchSnapshotFileEntryProperty(), snapshotFileEntryNode);
    }

    public static Triple createBranchSnapshotLineEntryProperty(Node snapshotFileEntryNode, Node snapshotLineEntryNode) {
        return Triple.create(snapshotFileEntryNode, branchSnapshotLineEntryProperty(), snapshotLineEntryNode);
    }

    public static Triple createBranchSnapshotLineProperty(Node snapshotLineEntryNode, int line) {
        return Triple.create(snapshotLineEntryNode, branchSnapshotLineProperty(), stringLiteral(Integer.toString(line)));
    }

    public static Triple createBranchSnapshotLinenumberBeginProperty(Node snapshotLineEntryNode, int linenumberBegin) {
        return Triple.create(snapshotLineEntryNode, branchSnapshotLinenumberBeginProperty(), stringLiteral(Integer.toString(linenumberBegin)));
    }

    public static Triple createBranchSnapshotLinenumberEndProperty(Node snapshotLineEntryNode, int linenumberEnd) {
        return Triple.create(snapshotLineEntryNode, branchSnapshotLinenumberEndProperty(), stringLiteral(Integer.toString(linenumberEnd)));
    }


    public static Triple createBranchSnapshotCommitHashProperty(Node snapshotLineEntryNode, String commitHash) {
        return Triple.create(snapshotLineEntryNode, branchSnapshotCommitHashProperty(), stringLiteral(commitHash));
    }

    // Commit

    public static Triple createRdfTypeProperty(String commitUri) {
        return Triple.create(RdfUtils.uri(commitUri), rdfTypeProperty(), RdfUtils.uri( GIT_NS + "GitCommit" ));
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
        return Triple.create(diffEntryNode, commitDiffEntryEditTypeProperty(), uri(GIT_NS + changeType.toString().toLowerCase()));
    }

    public static Triple createCommitResource(String commitUri, Node commitNode) {
        return Triple.create(uri(commitUri), commitResource(), commitNode);
    }

    // Diff

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
        return Triple.create(editNode, commitDiffEntryEditTypeProperty(), uri(GIT_NS + editType.toString().toLowerCase()));
    }

    public static Triple createEditOldLinenumberBeginProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, editOldLinenumberBeginProperty(), stringLiteral(Integer.toString(lineNumberBegin)));
    }

    public static Triple createEditNewLinenumberBeginProperty(Node editNode, int lineNumberBegin ) {
        return Triple.create(editNode, editNewLinenumberBeginProperty(), stringLiteral(Integer.toString(lineNumberBegin)));
    }

    public static Triple createEditOldLinenumberEndProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, editOldLinenumberEndProperty(), stringLiteral(Integer.toString(lineNumberEnd)));
    }

    public static Triple createEditNewLinenumberEndProperty(Node editNode, int lineNumberEnd ) {
        return Triple.create(editNode, editNewLinenumberEndProperty(), stringLiteral(Integer.toString(lineNumberEnd)));
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

    // Tag

    public static Triple createCommitTagProperty(String commitUri, String tagName) {
        return Triple.create(uri(commitUri), commitTagNameProperty(), stringLiteral(tagName));
    }

    // New relationships using v2 schema
    public static Triple createCommitInBranchProperty(String commitUri, String branchUri) {
        return Triple.create(uri(commitUri), inBranchProperty(), uri(branchUri));
    }

    public static Triple createCommitHasTagProperty(String commitUri, String tagUri) {
        return Triple.create(uri(commitUri), hasTagProperty(), uri(tagUri));
    }

    public static Triple createRepositoryHasCommitProperty(String repoUri, String commitUri) {
        return Triple.create(uri(repoUri), repositoryHasCommitProperty(), uri(commitUri));
    }

    public static Triple createRepositoryHasBranchProperty(String repoUri, String branchUri) {
        return Triple.create(uri(repoUri), repositoryHasBranchProperty(), uri(branchUri));
    }

    public static Triple createBranchRdfTypeProperty(String branchUri) {
        return Triple.create(uri(branchUri), rdfTypeProperty(), uri(GIT_NS + "GitBranch"));
    }

    public static Triple createBranchNameProperty(String branchUri, String name) {
        return Triple.create(uri(branchUri), branchNameProperty(), stringLiteral(name));
    }

    public static Triple createBranchHeadCommitProperty(String branchUri, String commitUri) {
        return Triple.create(uri(branchUri), branchHeadCommitProperty(), uri(commitUri));
    }

    public static Triple createTagRdfTypeProperty(String tagUri) {
        return Triple.create(uri(tagUri), rdfTypeProperty(), uri(GIT_NS + "GitTag"));
    }

    public static Triple createTagNameProperty(String tagUri, String tagName) {
        return Triple.create(uri(tagUri), commitTagNameProperty(), stringLiteral(tagName));
    }

    public static Triple createTagPointsToProperty(String tagUri, String commitUri) {
        return Triple.create(uri(tagUri), tagPointsToProperty(), uri(commitUri));
    }

    public static Triple createTagSameAsProperty(String tagUri, String tagUrl) {
        return Triple.create(uri(tagUri), owlSameAsProperty(), uri(tagUrl));
    }


    public static Triple createCommitIsMergeCommitProperty(String commitUri, boolean isMergeCommit) {
        return Triple.create(uri(commitUri), isMergeCommitProperty(), RdfUtils.booleanLiteral(isMergeCommit));
    }

    public static Triple createCommitHasParentProperty(String commitUri, String parentCommitUri) {
        return Triple.create(uri(commitUri), hasParentProperty(), uri(parentCommitUri));
    }

    /**
     * Extract referenced issue numbers from a commit message.
     * Supports patterns like "Fixes #123" or "#123".
     */
    public static Set<String> extractIssueNumbers(String commitMessage) {
        if (commitMessage == null) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        Pattern p = Pattern.compile("(?i)(?:fixes|closes|resolves|addresses)?\\s*#(\\d+)");
        Matcher m = p.matcher(commitMessage);
        while (m.find()) {
            result.add(m.group(1));
        }
        return result;
    }

    // Metadata

    public static Triple createRepositoryEncodingProperty(String repoUri, String encoding) {
        return Triple.create(uri(repoUri), repositoryEncodingProperty(), stringLiteral(encoding));
    }

    // Submodule

    public static Triple createRepositorySubmoduleProperty(String repoUri, Node submoduleNode) {
        return Triple.create(uri(repoUri), rdfSubmoduleProperty(), submoduleNode);
    }

    public static Triple createSubmoduleRdfTypeProperty(Node submoduleNode) {
        return Triple.create(submoduleNode, rdfTypeProperty(), RdfUtils.uri( GIT_NS + "GitSubmodule" ));
    }

    public static Triple createSubmodulePathProperty(Node submoduleNode, String pathName) {
        return Triple.create(submoduleNode, rdfSubmodulePathProperty(), stringLiteral(pathName));
    }

    public static Triple createSubmoduleCommitProperty(Node submoduleNode, String commitHash) {
        return Triple.create(submoduleNode, rdfSubmoduleCommitProperty(), stringLiteral(commitHash));
    }

    public static Triple createSubmoduleCommitEntryProperty(Node submoduleNode, String commitUrl) {
        return Triple.create(submoduleNode, rdfSubmoduleCommitEntryProperty(), RdfUtils.uri(commitUrl));
    }

    public static Triple createSubmoduleRepositoryEntryProperty(Node submoduleNode, String submoduleUrl) {
        return Triple.create(submoduleNode, rdfSubmoduleRepositoryEntryProperty(), RdfUtils.uri(submoduleUrl));
    }
}
