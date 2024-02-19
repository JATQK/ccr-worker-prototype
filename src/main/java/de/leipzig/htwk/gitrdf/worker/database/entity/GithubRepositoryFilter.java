package de.leipzig.htwk.gitrdf.worker.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "githubrepositoryfilter")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepositoryFilter {

    public static final GithubRepositoryFilter ENABLED
            = new GithubRepositoryFilter(null, GitCommitRepositoryFilter.ENABLED);

    public static final GithubRepositoryFilter DISABLED
            = new GithubRepositoryFilter(null, GitCommitRepositoryFilter.DISABLED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "gitcommitrepositoryfilter_id")
    private GitCommitRepositoryFilter gitCommitRepositoryFilter;

}
