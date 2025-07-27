package de.leipzig.htwk.gitrdf.worker.provider;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class GithubAppInstallationProvider implements AppInstallationAuthorizationProvider.AppInstallationProvider {

    private final GithubAccountRotationService githubAccountRotationService;

    public GithubAppInstallationProvider(GithubAccountRotationService githubAccountRotationService) {
        this.githubAccountRotationService = githubAccountRotationService;
    }

    @Override
    public GHAppInstallation getAppInstallation(GHApp app) throws IOException {

        try {
            GithubConfig.GithubApiAccount currentAccount = githubAccountRotationService.getCurrentAccount();
            long installationId = Long.parseLong(currentAccount.getGithubAppInstallationId());
            
            log.debug("Using GitHub API account {} for app installation", currentAccount.getAccountNumber());
            
            return app.getInstallationById(installationId);

        } catch (NumberFormatException ex) {
            throw new RuntimeException("Failed to convert the given installation id originating as a string to a long id. " +
                    "Can't retrieve the github app installation.", ex);
        }
    }

}
