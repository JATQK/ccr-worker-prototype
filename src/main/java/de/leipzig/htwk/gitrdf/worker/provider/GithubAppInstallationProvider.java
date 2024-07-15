package de.leipzig.htwk.gitrdf.worker.provider;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GithubAppInstallationProvider implements AppInstallationAuthorizationProvider.AppInstallationProvider {

    private final GithubConfig githubConfig;

    public GithubAppInstallationProvider(GithubConfig githubConfig) {
        this.githubConfig = githubConfig;
    }

    @Override
    public GHAppInstallation getAppInstallation(GHApp app) throws IOException {

        try {

            long installationId = Long.parseLong(githubConfig.getGithubAppInstallationId());
            return app.getInstallationById(installationId);

        } catch (NumberFormatException ex) {
            throw new RuntimeException("Failed to convert the given installation id originating as a string to a long id. " +
                    "Can't retrieve the github app installation.", ex);
        }
    }

}
