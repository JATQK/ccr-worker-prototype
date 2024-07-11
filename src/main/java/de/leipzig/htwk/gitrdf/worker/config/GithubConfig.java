package de.leipzig.htwk.gitrdf.worker.config;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Getter
public class GithubConfig {

    private final String pemPrivateBase64Key;

    private final String githubAppId;

    private final String githubAppInstallationId;

    private final String githubSystemUserName;

    private final String githubSystemUserPersonalAccessToken;

    private final int rateLimitRequestsLeftBorder;

    private static final IllegalArgumentException NoGithubPrivateKeyGiven = new IllegalArgumentException(
            "No github private pem key was specified for application startup. " +
                    "Specifying a github private pem key is mandatory for application startup. " +
                    "Without a github private pem key no jwt token generation can be performed. " +
                    "Therefore no authentication can be performed with the github api. " +
                    "It is recommended to specify the private pem key in the following environment variable: " +
                    "'GITHUB_LOGIN_KEY'.");

    private static final IllegalArgumentException NoGithubAppInstallationIdGiven = new IllegalArgumentException(
            "No github app installation id was specified for application startup. " +
                    "Specifying a github app installation id is mandatory for application startup. " +
                    "Without a github app installation id (referring to your installed github app, probably on your user account) " +
                    "no installation token generation can be performed. " +
                    "Therefore no authentication can be performed with the github api. " +
                    "It is recommended to specify the github app installation id in the following environment variable: " +
                    "'GITHUB_LOGIN_APP_INSTALLATION_ID'.");

    private static final IllegalArgumentException NoGithubAppIdGiven = new IllegalArgumentException(
            "No github app id was specified for application startup. " +
                    "Specifying a github app id is mandatory for application startup. " +
                    "Without a github app id no jwt token generation can be performed. " +
                    "Therefore no authentication can be performed with the github api. " +
                    "It is recommended to specify the github app id in the following environment variable: " +
                    "'GITHUB_LOGIN_APP_ID'.");

    private static final IllegalArgumentException NoGithubSystemUserNameGiven = new IllegalArgumentException(
            "No github system user name was specified for application startup. " +
                    "Specifying a github system user name is mandatory for application startup. " +
                    "Without a github system user name no cloning of target repositories can be performed. " +
                    "It is recommended to specify the github system user name in the following environment variable: " +
                    "'GITHUB_LOGIN_SYSTEM_USER_NAME'.");

    private static final IllegalArgumentException NoGithubPersonalAccessTokenGiven = new IllegalArgumentException(
            "No github personal access token was specified for application startup. " +
                    "Specifying a github personal access token is mandatory for application startup. " +
                    "Without a github personal access token no cloning of target repositories can be performed. " +
                    "It is recommended to specify the github personal access token in the following environment variable: " +
                    "'GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN'.");

    public GithubConfig(
            @Value("${github.login.key}") String pemPrivateBase64Key,
            @Value("${github.login.app.id}") String githubAppId,
            @Value("${github.login.app.installation.id}") String githubAppInstallationId,
            @Value("${github.login.system.user.name}") String githubSystemUserName,
            @Value("${github.login.system.user.personal-access-token}") String githubSystemUserPersonalAccessToken,
            @Value("${github.rate-limit.requests-left-border}") int rateLimitRequestLeftBorder) {

        if (StringUtils.isBlank(pemPrivateBase64Key)) throw NoGithubPrivateKeyGiven;
        if (StringUtils.isBlank(githubAppInstallationId)) throw NoGithubAppInstallationIdGiven;
        if (StringUtils.isBlank(githubAppId)) throw NoGithubAppIdGiven;
        if (StringUtils.isBlank(githubSystemUserName)) throw NoGithubSystemUserNameGiven;
        if (StringUtils.isBlank(githubSystemUserPersonalAccessToken)) throw NoGithubPersonalAccessTokenGiven;

        this.pemPrivateBase64Key = pemPrivateBase64Key;
        this.githubAppId = githubAppId;
        this.githubAppInstallationId = githubAppInstallationId;
        this.githubSystemUserName = githubSystemUserName;
        this.githubSystemUserPersonalAccessToken = githubSystemUserPersonalAccessToken;
        this.rateLimitRequestsLeftBorder = rateLimitRequestLeftBorder;
    }

}
