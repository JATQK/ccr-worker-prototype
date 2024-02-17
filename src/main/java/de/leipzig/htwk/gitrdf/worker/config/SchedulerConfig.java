package de.leipzig.htwk.gitrdf.worker.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SchedulerConfig {

    private final boolean isRdfGitRepoTaskEnabled;

    private final boolean isRdfGithubRepoTaskEnabled;

    public SchedulerConfig(
            @Value("${worker.task.rdf-git-repo.enabled}") boolean isRdfGitRepoTaskEnabled,
            @Value("${worker.task.rdf-github-repo.enabled}") boolean isRdfGithubRepoTaskEnabled) {

        this.isRdfGitRepoTaskEnabled = isRdfGitRepoTaskEnabled;
        this.isRdfGithubRepoTaskEnabled = isRdfGithubRepoTaskEnabled;
    }

}
