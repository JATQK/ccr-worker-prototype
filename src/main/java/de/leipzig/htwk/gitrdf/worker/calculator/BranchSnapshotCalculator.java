package de.leipzig.htwk.gitrdf.worker.calculator;

import de.leipzig.htwk.gitrdf.worker.utils.rdf.RdfCommitUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BranchSnapshotCalculator {

    private final StreamRDF writer;
    private final Repository gitRepository;
    private final String branchSnapshotUri;

    public BranchSnapshotCalculator(StreamRDF writer, Repository gitRepository, String targetCommitUri) {
        this.writer = writer;
        this.gitRepository = gitRepository;
        this.branchSnapshotUri = targetCommitUri;
    }

    public void calculateBranchSnapshot() throws IOException {

        writer.start();

        //ObjectId headCommitId = gitRepository.resolve("HEAD");
        //String commitUri = getGithubCommitUri(owner, repositoryName, headCommitId.getName());

        //String branchSnapshotUri = targetCommitUri;
        //String branchSnapshotUri = GIT_NS + ":blame";
        //String branchSnapshotUri = "";

        Resource branchSnapshotResource = ResourceFactory.createResource(branchSnapshotUri);
        Node branchSnapshotNode = branchSnapshotResource.asNode();

        writer.triple(RdfCommitUtils.createBranchSnapshotProperty(branchSnapshotNode));
        writer.triple(RdfCommitUtils.createBranchSnapshotDateProperty(branchSnapshotNode, LocalDateTime.now()));

        writer.finish();

        List<String> fileNames = listRepositoryContents(gitRepository);

        log.info("Listed {} files for branch snapshotting", fileNames.size());

        iterateThroughFilesAndWriteBranchSnapshottingEntries(branchSnapshotNode, fileNames);

    }

    private void iterateThroughFilesAndWriteBranchSnapshottingEntries(
            Node branchSnapshotNode, List<String> fileNames) throws IOException {

        BlameResultCalculator blameResultCalculator = new BlameResultCalculator(gitRepository);

        int branchSnapshotCounter = 0;

        for (int ii = 0; ii < fileNames.size(); ii++) {

            if (branchSnapshotCounter < 1) {
                log.info("Branch Snapshotting iteration {} started", ii);
                writer.start();
            }

            branchSnapshotCounter++;

            String fileName = fileNames.get(ii);

            Resource branchSnapshotFileResource = ResourceFactory.createResource();
            Node branchSnapshotFileNode = branchSnapshotFileResource.asNode();

            writer.triple(RdfCommitUtils.createBranchSnapshotFileEntryProperty(branchSnapshotNode, branchSnapshotFileNode));
            writer.triple(RdfCommitUtils.createBranchSnapshotFilenameProperty(branchSnapshotFileNode, fileName));

            BlameResult blameResult;

            try {

                blameResult = blameResultCalculator.getBlameResult(fileName);

            } catch (Exception e) {
                throw new IllegalStateException("Unable to blame file " + fileName, e);
            }

            if (blameResult == null) {
                continue;
            }

            String prevCommitHash = null;
            int linenumberBegin = 1;

            RawText blameText = blameResult.getResultContents();
            int lineCount = blameText.size();

            for (int lineIdx = 0; lineIdx < lineCount; lineIdx++) {

                RevCommit commit = blameResult.getSourceCommit(lineIdx);

                if (commit == null) {
                    continue;
                }

                String currentCommitHash = commit.getId().name();

                if (prevCommitHash == null) {
                    prevCommitHash = currentCommitHash;
                }

                boolean isAtEnd = lineIdx == lineCount - 1;
                boolean isNewCommit = !currentCommitHash.equals(prevCommitHash);

                if (isNewCommit || isAtEnd) {

                    Resource branchSnapshotLineEntryResource = ResourceFactory.createResource();
                    Node branchSnapshotLineEntryNode = branchSnapshotLineEntryResource.asNode();

                    int lineNumberEnd = lineIdx;

                    if (isAtEnd) {

                        lineNumberEnd += 1;

                        writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                        writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, prevCommitHash));
                        writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, linenumberBegin));
                        writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));

                        if (isNewCommit) {

                            writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                            writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, currentCommitHash));
                            writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, lineNumberEnd));
                            writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));
                        }
                    }

                    if (isNewCommit) {

                        writer.triple(RdfCommitUtils.createBranchSnapshotLineEntryProperty(branchSnapshotFileNode, branchSnapshotLineEntryNode));
                        writer.triple(RdfCommitUtils.createBranchSnapshotCommitHashProperty(branchSnapshotLineEntryNode, prevCommitHash));
                        writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberBeginProperty(branchSnapshotLineEntryNode, linenumberBegin));
                        writer.triple(RdfCommitUtils.createBranchSnapshotLinenumberEndProperty(branchSnapshotLineEntryNode, lineNumberEnd));

                        prevCommitHash = currentCommitHash;
                    }

                    linenumberBegin = lineIdx + 1;
                }
            }

            if (branchSnapshotCounter > 99) {
                writer.finish();
                branchSnapshotCounter = 0;
            }

        }

        if (branchSnapshotCounter > 0) {
            writer.finish();
        }

    }

    // See: https://stackoverflow.com/q/19941597/11341498
    private List<String> listRepositoryContents(Repository repository) throws IOException {

        Ref head = repository.getRef("HEAD");
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head.getObjectId());
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        List<String> fileNames = new ArrayList<String>();

        while (treeWalk.next()) {
            fileNames.add(treeWalk.getPathString());
        }

        return fileNames;
    }

}
