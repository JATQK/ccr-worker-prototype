package de.leipzig.htwk.gitrdf.worker.calculator;

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

            gitRepositoryRevWalk.reset();
            gitRepositoryRevWalk.markStart(branchTipCommit);

            RevCommit commit = null;

            while ((commit = gitRepositoryRevWalk.next()) != null) {
                updateCommitWithBranch(commit, branchName);
            }

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