package de.leipzig.htwk.gitrdf.worker.model;

import de.leipzig.htwk.gitrdf.worker.service.impl.GithubHandlerService;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.net.URISyntaxException;

public class GithubHandle {

    public final static long FIFTY_MINUTES = 1000L * 60L * 50L;

    private GitHub gitHub;

    private long creationTime;

    private final GithubHandlerService githubHandlerService;

    public GithubHandle(GitHub gitHub, long creationTime, GithubHandlerService githubHandlerService) {
        this.gitHub = gitHub;
        this.creationTime = creationTime;
        this.githubHandlerService = githubHandlerService;
    }

    /**
     * Returns whether refresh happened
     */
    public boolean refreshGithubHandleOnEmergingExpiring() throws URISyntaxException, IOException, InterruptedException {

        long now = System.currentTimeMillis();

        long duration = now - this.creationTime;

        if (duration > FIFTY_MINUTES) {
            this.gitHub = this.githubHandlerService.getGithub();
            this.creationTime = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }

    }

    public GitHub getGitHubHandle() {
        return this.gitHub;
    }

}
