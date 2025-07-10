package de.leipzig.htwk.gitrdf.worker.utils.rdf;

/**
 * Simple container for GitHub user details.
 */
public class GithubUserInfo {
    public final String uri;
    public final String login;
    public final long userId;
    public final String name;

    public GithubUserInfo(String uri, String login, long userId, String name) {
        this.uri = uri;
        this.login = login;
        this.userId = userId;
        this.name = name;
    }
}
