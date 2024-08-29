package de.leipzig.htwk.gitrdf.worker.utils.rdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;

import java.time.LocalDateTime;
import java.util.Map;

import static de.leipzig.htwk.gitrdf.worker.service.impl.GithubRdfConversionTransactionService.*;
import static de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfUtils.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfCommitUtils {

    private static final String NS = GIT_NAMESPACE + ":";
    private static final String GH_NS = PLATFORM_GITHUB_NAMESPACE + ":";
    private static final String PF_NS = PLATFORM_NAMESPACE + ":";
    private static final String RDF_NS = RDF_SCHEMA_NAMESPACE + ":";


    // org.apache.jena.datatypes.xsd.XSDDatatype -> static xsd Datatype collection from apache jena

    // somewhat of an applicable base uri to the contents of git: https://git-scm.com/docs/gitglossary

    public static Node rdfTypeProperty() {
        return RdfUtils.uri(RDF_NS + "type");
    }

    public static Node repositoryNameProperty() {
        return uri(PF_NS + "name");
    }

    public static Node repositoryOwnerProperty() {
        return uri(GH_NS + "owner");
    }

    public static Node repositoryEncodingProperty() {
        return uri(NS + "encoding");
    }

    private static Node rdfSubmoduleProperty() { return uri(NS + "submodule"); }

    public static Node rdfSubmoduleNameProperty() {
        return uri(NS + "submoduleName");
    }

    public static Node rdfSubmodulePathProperty() {
        return uri(NS + "submodulePath");
    }

    public static Node rdfSubmoduleUrlProperty() {
        return uri(NS + "submoduleUrl");
    }

    public static Node rdfSubmoduleCommitProperty() {
        return uri(NS + "submoduleCommitHash");
    }

    public static Node rdfSubmoduleCommitEntryProperty() {
        return uri(NS + "submoduleCommitUri");
    }

    public static Node rdfSubmoduleRepositoryEntryProperty() {
        return uri(NS + "submoduleRepositoryUri");
    }

    public static Node commitHashProperty() { return uri(NS + "commitHash"); }
    
    public static Node authorNameProperty() {
       return uri(NS + "authorName");
    }

    public static Node authorEmailProperty() {
        return uri(NS + "authorEmail");
    }

    public static Node authorDateProperty() {
        return uri(NS + "authorDate");
    }

    public static Node commitDateProperty() {
        return uri(NS + "commitDate");
    }

    public static Node committerNameProperty() {
        return uri(NS + "committerName");
    }

    public static Node committerEmailProperty() {
        return uri(NS + "committerEmail");
    }

    public static Node commitMessageProperty() {
        return uri(NS + "commitMessage");
    }

    public static Node commitBranchNameProperty() {
        return uri(NS + "branchName");
    }

    public static Node commitDiffEntryEditTypeProperty() {
        return uri(NS + "changeType");
    }

    public static Node commitResource() {
        return uri(NS + "commit");
    }

    public static Node branchResource() {
        return uri(NS + "branch");
    }

    public static Node commitGitHubUserProperty() {
        return uri(GH_NS + "user");
    }
    public static Node tagResource() {
        return uri(NS + "tag");
    }

    public static Node commitDiffEntryResource() {
        return uri(NS + "diffEntry");
    }

    public static Node commitDiffEntryOldFileNameProperty() {
        return uri(NS + "oldFileName");
    }

    public static Node commitDiffEntryNewFileNameProperty() {
        return uri(NS + "newFileName");
    }

    public static Node commitDiffEditResource() {
        return uri(NS + "diffEdit");
    }

    public static Node commitDiffEditTypeProperty() {
        return uri(NS + "editType");
    }

    public static Node editOldLinenumberBeginProperty() {
        return uri(NS + "oldLinenumberBegin");
    }

    public static Node editNewLinenumberBeginProperty() {
        return uri(NS + "newLinenumberBegin");
    }

    public static Node editOldLinenumberEndProperty() {
        return uri(NS + "oldLinenumberEnd");
    }

    public static Node editNewLinenumberEndProperty() {
        return uri(NS + "newLinenumberEnd");
    }

    public static Node branchSnapshotProperty() {
        return uri(NS + "branchSnapshot");
    }

    public static Node branchSnapshotLineEntryProperty() {
        return uri(NS + "branchSnapshotLineEntry");
    }

    public static Node branchSnapshotFileEntryProperty() {
        return uri(NS + "branchSnapshotFileEntry");
    }

    public static Node branchSnapshotLineProperty() {
        return uri(NS + "branchSnapshotLine");
    }

    public static Node branchSnapshotLinenumberBeginProperty() {
        return uri(NS + "branchSnapshotLinenumberBegin");
    }

    public static Node branchSnapshotLinenumberEndProperty() {
        return uri(NS + "branchSnapshotLinenumberEnd");
    }

    public static Node branchSnapshotFilenameProperty() {
        return uri(NS + "branchSnapshotFilename");
    }

    public static Node branchSnapshotCommitHashProperty() {
        return uri(NS + "branchSnapshotCommitHash");
    }

    public static Node branchSnapshotDateProperty() {
        return uri(NS + "branchSnapshotDate");
    }

    public static Node commitTagNameProperty() {
        return uri(NS + "tagName");
    }


    // Branch Snapshot

    public static Triple createBranchSnapshotProperty(Node snapshotNode) {
        return Triple.create(snapshotNode, rdfTypeProperty(), RdfUtils.uri(NS + "BranchSnapshot" ));
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

    public static Triple createRdfTypeProperty(String issueUri) {
        return Triple.create(RdfUtils.uri(issueUri), rdfTypeProperty(), RdfUtils.uri( GH_NS + "GitCommit" ));
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
        //return Triple.create(diffEntryNode, commitDiffEntryEditTypeProperty(), changeTypeLiteral(changeType));
        return Triple.create(diffEntryNode, commitDiffEntryEditTypeProperty(), uri(NS + changeType.toString().toLowerCase()));
    }

    public static Triple createCommiterGitHubUserProperty(String commitUri, String commiterGitHubUser) {
        return Triple.create(uri(commitUri), commitGitHubUserProperty(), stringLiteral(commiterGitHubUser));
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
        return Triple.create(editNode, commitDiffEntryEditTypeProperty(), uri(NS + editType.toString().toLowerCase()));
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

    // Metadata

    public static Triple createRepositoryRdfTypeProperty(String repoUri) {
        return Triple.create(RdfUtils.uri(repoUri), rdfTypeProperty(), RdfUtils.uri( GH_NS + "GitRepository" ));
    }

    public static Triple createRepositoryEncodingProperty(String repoUri, String encoding) {
        return Triple.create(uri(repoUri), repositoryEncodingProperty(), stringLiteral(encoding));
    }

    public static Triple createRepositoryOwnerProperty(String repoUri, String ownerName) {
        return Triple.create(uri(repoUri), repositoryOwnerProperty(), stringLiteral(ownerName));
    }

    public static Triple createRepositoryNameProperty(String repoUri, String repositoryName) {
        return Triple.create(uri(repoUri), repositoryNameProperty(), stringLiteral(repositoryName));
    }

    // Submodule

    public static Triple createRepositorySubmoduleProperty(String repoUri, Node submoduleNode) {
        return Triple.create(uri(repoUri), rdfSubmoduleProperty(), submoduleNode);
    }

    public static Triple createSubmoduleRdfTypeProperty(Node submoduleNode) {
        return Triple.create(submoduleNode, rdfTypeProperty(), RdfUtils.uri( NS + "Submodule" ));
    }

    public static Triple createSubmoduleNameProperty(Node submoduleNode, String name) {
        return Triple.create(submoduleNode, rdfSubmoduleNameProperty(), stringLiteral(name));
    }

    public static Triple createSubmodulePathProperty(Node submoduleNode, String pathName) {
        return Triple.create(submoduleNode, rdfSubmodulePathProperty(), stringLiteral(pathName));
    }

    public static Triple createSubmoduleUrlProperty(Node submoduleNode, String submoduleUrl) {
        return Triple.create(submoduleNode, rdfSubmoduleUrlProperty(), stringLiteral(submoduleUrl));
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
