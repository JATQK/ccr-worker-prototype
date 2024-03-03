package de.leipzig.htwk.gitrdf.worker.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitCommitRepositoryFilter {

    public static final GitCommitRepositoryFilter ENABLED = new GitCommitRepositoryFilter(
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true);

    public static final GitCommitRepositoryFilter DISABLED = new GitCommitRepositoryFilter(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false);

    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    //private Long id;

    private boolean enableCommitHash;

    private boolean enableAuthorName;

    private boolean enableAuthorEmail;

    private boolean enableAuthorDate;

    private boolean enableCommitDate;

    private boolean enableCommitterName;

    private boolean enableCommitterEmail;

    private boolean enableCommitMessage;


    private boolean enableCommitDiff;

    private boolean enableCommitBranch;

    private boolean enableCommitTag;
}
