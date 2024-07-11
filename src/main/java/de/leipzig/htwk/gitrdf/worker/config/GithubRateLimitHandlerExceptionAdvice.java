package de.leipzig.htwk.gitrdf.worker.config;

import org.kohsuke.github.GitHubRateLimitHandler;
import org.kohsuke.github.connector.GitHubConnectorResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class GithubRateLimitHandlerExceptionAdvice extends GitHubRateLimitHandler {

    @Override
    public void onError(GitHubConnectorResponse connectorResponse) throws IOException {

        String body = getBodyFrom(connectorResponse);

        String errorMessage = String.format("Github rate limit was exceeded. " +
                "Status code is '%d'. " +
                "Http body is (hard cut after 256 characters): '%s'",
                connectorResponse.statusCode(),
                body);

        throw new RuntimeException(errorMessage);

    }

    /**
     * Body is hard cut after 256 characters.
     */
    private String getBodyFrom(GitHubConnectorResponse connectorResponse) throws IOException {

        StringBuilder builder = new StringBuilder();

        int characterCounter = 0;
        int characterLimit = 256;

        try (Reader reader = new BufferedReader(
                new InputStreamReader(connectorResponse.bodyStream(), StandardCharsets.UTF_8))) {

            int c = 0;

            while ((c = reader.read()) != -1) {

                builder.append((char) c);

                characterCounter++;
                if (characterCounter >= characterLimit) {
                    break;
                }

            }

        }

        return builder.toString();

    }
}
