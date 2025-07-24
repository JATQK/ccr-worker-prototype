package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

/**
 * Simple container for GitHub user details.
 */
public class GithubUserInfo {
    public final String uri;
    public final String login;
    public final long userId;
    public final String name;
    public final String gitAuthorEmail;

    public GithubUserInfo(String uri, String login, long userId, String name) {
        this(uri, login, userId, name, null);
    }

    public GithubUserInfo(String uri, String login, long userId, String name, String gitAuthorEmail) {
        this.uri = uri;
        this.login = login;
        this.userId = userId;
        this.name = name;
        this.gitAuthorEmail = gitAuthorEmail;
    }
}
