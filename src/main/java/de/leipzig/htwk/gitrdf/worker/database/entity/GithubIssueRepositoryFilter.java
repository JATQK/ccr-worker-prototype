package de.leipzig.htwk.gitrdf.worker.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubIssueRepositoryFilter {

    public static final GithubIssueRepositoryFilter ENABLED = new GithubIssueRepositoryFilter(
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

    public static final GithubIssueRepositoryFilter DISABLED = new GithubIssueRepositoryFilter(
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

    private boolean enableIssueId;

    private boolean enableIssueState;

    private boolean enableIssueTitle;

    private boolean enableIssueBody;

    private boolean enableIssueUser;

    private boolean enableIssueLabels;

    private boolean enableIssueAssignees;

    private boolean enableIssueMilestone;

    private boolean enableIssueCreatedAt;

    private boolean enableIssueUpdatedAt;

    private boolean enableIssueClosedAt;

    public boolean doesContainAtLeastOneEnabledFilterOption() {
        return this.isEnableIssueId()
                || this.isEnableIssueState()
                || this.isEnableIssueTitle()
                || this.isEnableIssueBody()
                || this.isEnableIssueUser()
                || this.isEnableIssueLabels()
                || this.isEnableIssueAssignees()
                || this.isEnableIssueMilestone()
                || this.isEnableIssueCreatedAt()
                || this.isEnableIssueUpdatedAt()
                || this.isEnableIssueClosedAt();
    }

}
