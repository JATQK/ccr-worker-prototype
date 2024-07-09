package de.leipzig.htwk.gitrdf.worker.model;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitBranchCalculator {

    private Map<String, List<String>> commitToBranchMap;

    public CommitBranchCalculator(Iterable<Ref> branches, RevWalk gitRepositoryRevWalk) {
        this.commitToBranchMap = new HashMap<>();
        iterateThroughBranchesAndBuildCommitHashmap(branches, gitRepositoryRevWalk);
    }

    public List<String> getBranchesForShaHashOfCommit(String shaCommitHash) {
        return commitToBranchMap.get(shaCommitHash);
    }

    private void iterateThroughBranchesAndBuildCommitHashmap(Iterable<Ref> branches, RevWalk gitRepositoryRevWalk) {

        for (Ref branchRef : branches) {

            RevCommit branchTipCommit = gitRepositoryRevWalk.lookupCommit(branchRef.getObjectId());
            gitRepositoryRevWalk.markStart();
            gitRepositoryRevWalk.next()
            String branchName = branchRef.getName();

            // TODO: Look into the isMergedInto Method of RevWalk -> basically copy the logic, but: dont compare, instead traverse
            //  through entire graph and fill up hashmap
            // TODO (ccr): Continue here


        }

    }

}

/**
 *
 *
 *
 private void calculateCommitBranch(
 Iterable<Ref> branches,
 RevWalk gitRepositoryRevWalk,
 StreamRDF writer,
 RevCommit currentCommit,
 String commitUri) throws IOException {

 for (Ref branchRef : branches) {

 RevCommit commitRev = gitRepositoryRevWalk.lookupCommit(currentCommit.getId());
 RevCommit branchRev = gitRepositoryRevWalk.lookupCommit(branchRef.getObjectId());

 if (gitRepositoryRevWalk.isMergedInto(commitRev, branchRev)) {
 writer.triple(RdfCommitUtils.createCommitBranchNameProperty(commitUri, branchRef.getName()));
 }
 }

 }
 */