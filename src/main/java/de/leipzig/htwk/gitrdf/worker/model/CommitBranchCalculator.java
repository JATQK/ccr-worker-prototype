package de.leipzig.htwk.gitrdf.worker.model;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.*;

public class CommitBranchCalculator {

    private final Map<String, List<String>> commitToBranchMap;

    public CommitBranchCalculator(Iterable<Ref> branches, RevWalk gitRepositoryRevWalk) throws IOException {
        this.commitToBranchMap = new HashMap<>();
        iterateThroughBranchesAndBuildCommitHashmap(branches, gitRepositoryRevWalk);
    }

    public List<String> getBranchesForShaHashOfCommit(String shaCommitHash) {
        List<String> branches = commitToBranchMap.get(shaCommitHash);
        return branches == null ? Collections.emptyList() : branches;
    }

    private void iterateThroughBranchesAndBuildCommitHashmap(Iterable<Ref> branches, RevWalk gitRepositoryRevWalk) throws IOException {

        for (Ref branchRef : branches) {

            RevCommit branchTipCommit = gitRepositoryRevWalk.lookupCommit(branchRef.getObjectId());
            String branchName = branchRef.getName();

            gitRepositoryRevWalk.markStart(branchTipCommit);

            RevCommit commit = null;

            while ((commit = gitRepositoryRevWalk.next()) != null) {
                updateCommitWithBranch(commit, branchName);
            }

            // TODO: Look into the isMergedInto Method of RevWalk -> basically copy the logic, but: dont compare, instead traverse
            //  through entire graph and fill up hashmap
            // TODO (ccr): Continue here


        }

    }

    private void updateCommitWithBranch(RevCommit commit, String branchName) {

        String commitHash = commit.getName();

        List<String> branchNames = this.commitToBranchMap.get(commitHash);

        if (branchNames == null) {

            List<String> newBranchNameList = new ArrayList<>();
            newBranchNameList.add(branchName);

            this.commitToBranchMap.put(commitHash, newBranchNameList);

        } else {
            branchNames.add(branchName);
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