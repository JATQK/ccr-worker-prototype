package de.leipzig.htwk.gitrdf.worker.config;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;


@Configuration
@Getter
public class GithubConfig {

    private final List<GithubApiAccount> githubApiAccounts;

    private final int rateLimitRequestsLeftBorder;

    @Getter
    public static class GithubApiAccount {
        private final String pemPrivateBase64Key;
        private final String githubAppId;
        private final String githubAppInstallationId;
        private final String githubSystemUserName;
        private final String githubSystemUserPersonalAccessToken;
        private final int accountNumber;

        public GithubApiAccount(String pemPrivateBase64Key, String githubAppId, String githubAppInstallationId,
                              String githubSystemUserName, String githubSystemUserPersonalAccessToken, int accountNumber) {
            this.pemPrivateBase64Key = pemPrivateBase64Key;
            this.githubAppId = githubAppId;
            this.githubAppInstallationId = githubAppInstallationId;
            this.githubSystemUserName = githubSystemUserName;
            this.githubSystemUserPersonalAccessToken = githubSystemUserPersonalAccessToken;
            this.accountNumber = accountNumber;
        }
    }

    private static final IllegalArgumentException NoGithubApiAccountsFound = new IllegalArgumentException(
            "No github API accounts were configured for application startup. " +
                    "At least one complete set of GitHub API credentials is required. " +
                    "Please configure GitHub API accounts using environment variables with numbered suffixes: " +
                    "GITHUB_LOGIN_KEY_1, GITHUB_LOGIN_APP_ID_1, GITHUB_LOGIN_APP_INSTALLATION_ID_1, " +
                    "GITHUB_LOGIN_SYSTEM_USER_NAME_1, GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN_1. " +
                    "Additional accounts can be configured with _2, _3, etc. suffixes.");

    public GithubConfig(Environment environment,
                       @Value("${github.rate-limit.requests-left-border}") int rateLimitRequestLeftBorder) {
        
        this.githubApiAccounts = loadGithubApiAccounts(environment);
        this.rateLimitRequestsLeftBorder = rateLimitRequestLeftBorder;
        
        if (githubApiAccounts.isEmpty()) {
            throw NoGithubApiAccountsFound;
        }
    }

    private List<GithubApiAccount> loadGithubApiAccounts(Environment environment) {
        List<GithubApiAccount> accounts = new ArrayList<>();
        
        // First try to load legacy single account configuration
        String legacyKey = environment.getProperty("github.login.key");
        String legacyAppId = environment.getProperty("github.login.app.id");
        String legacyInstallationId = environment.getProperty("github.login.app.installation.id");
        String legacyUserName = environment.getProperty("github.login.system.user.name");
        String legacyToken = environment.getProperty("github.login.system.user.personal-access-token");
        
        if (isValidAccountConfig(legacyKey, legacyAppId, legacyInstallationId, legacyUserName, legacyToken)) {
            accounts.add(new GithubApiAccount(legacyKey, legacyAppId, legacyInstallationId, legacyUserName, legacyToken, 0));
        }
        
        // Now load numbered accounts
        for (int i = 1; i <= 20; i++) { // Support up to 20 accounts
            String key = environment.getProperty("github.login.key." + i);
            String appId = environment.getProperty("github.login.app.id." + i);
            String installationId = environment.getProperty("github.login.app.installation.id." + i);
            String userName = environment.getProperty("github.login.system.user.name." + i);
            String token = environment.getProperty("github.login.system.user.personal-access-token." + i);
            
            if (isValidAccountConfig(key, appId, installationId, userName, token)) {
                accounts.add(new GithubApiAccount(key, appId, installationId, userName, token, i));
            } else if (i == 1 && accounts.isEmpty()) {
                // If no legacy config and no account 1, this is an error
                break;
            }
        }
        
        return accounts;
    }
    
    private boolean isValidAccountConfig(String key, String appId, String installationId, String userName, String token) {
        return StringUtils.isNotBlank(key) && 
               StringUtils.isNotBlank(appId) && 
               StringUtils.isNotBlank(installationId) && 
               StringUtils.isNotBlank(userName) && 
               StringUtils.isNotBlank(token);
    }

    // Legacy compatibility methods
    public String getPemPrivateBase64Key() {
        return githubApiAccounts.isEmpty() ? null : githubApiAccounts.get(0).getPemPrivateBase64Key();
    }

    public String getGithubAppId() {
        return githubApiAccounts.isEmpty() ? null : githubApiAccounts.get(0).getGithubAppId();
    }

    public String getGithubAppInstallationId() {
        return githubApiAccounts.isEmpty() ? null : githubApiAccounts.get(0).getGithubAppInstallationId();
    }

    public String getGithubSystemUserName() {
        return githubApiAccounts.isEmpty() ? null : githubApiAccounts.get(0).getGithubSystemUserName();
    }

    public String getGithubSystemUserPersonalAccessToken() {
        return githubApiAccounts.isEmpty() ? null : githubApiAccounts.get(0).getGithubSystemUserPersonalAccessToken();
    }

}
