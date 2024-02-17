package de.leipzig.htwk.gitrdf.worker.service.impl;

import de.leipzig.htwk.gitrdf.worker.utils.GitUtils;
import de.leipzig.htwk.gitrdf.worker.utils.ZipUtils;
import lombok.RequiredArgsConstructor;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.function.InputStreamFunction;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubServiceImpl {

    private static final int TWENTY_FIVE_MEGABYTE = 1024 * 1024 * 25;

    private GitHub gitHub;

    public void testMethod() throws IOException {

        String owner = "";
        String repo = "";

        String targetRepoName = String.format("%s/%s", owner, repo);

        GHRepository targetRepo = gitHub.getRepository(targetRepoName);
        List<GHIssue> issues = targetRepo.getIssues(GHIssueState.ALL);




        File extractedZipFileDirectory = targetRepo.readZip(
                input -> ZipUtils.extractZip(input, TWENTY_FIVE_MEGABYTE),
                null);

        File gitFile
                = GitUtils.getDotGitFileFromParentDirectoryFileAndThrowExceptionIfNoOrMoreThanOneExists(extractedZipFileDirectory);



    }

}
