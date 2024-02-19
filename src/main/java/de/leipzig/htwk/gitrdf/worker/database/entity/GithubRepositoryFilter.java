package de.leipzig.htwk.gitrdf.worker.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepositoryFilter {

    public static final GithubRepositoryFilter ENABLED = new GithubRepositoryFilter(
            GitCommitRepositoryFilter.ENABLED, GithubIssueRepositoryFilter.ENABLED);

    public static final GithubRepositoryFilter DISABLED = new GithubRepositoryFilter(
            GitCommitRepositoryFilter.DISABLED, GithubIssueRepositoryFilter.DISABLED);

    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //private Long id;

    @Embedded
    private GitCommitRepositoryFilter gitCommitRepositoryFilter;

    @Embedded
    private GithubIssueRepositoryFilter githubIssueRepositoryFilter;

}
