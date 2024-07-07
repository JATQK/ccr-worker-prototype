package de.leipzig.htwk.gitrdf.worker.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.model.GithubHandle;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class GithubHandlerService {

    private final PrivateKey privateKey;

    private final GithubConfig githubConfig;

    private final Clock clock;

    private final String installationAccessTokenEndpointUrl;

    private final ObjectMapper objectMapper;

    public GithubHandlerService(
            GithubConfig githubConfig,
            Clock clock,
            ObjectMapper objectMapper) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // weird cryptography java adapter dependency
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // not really pkcs8 encoded (its actually pkcs1), but with the bouncy castle provider above,
        // the key can be generated successfully nevertheless.
        // Sadly github only provides pkcs1 keys
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(githubConfig.getPemPrivateBase64Key());

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        this.privateKey = keyFactory.generatePrivate(keySpec);

        this.githubConfig = githubConfig;

        this.clock = clock;

        this.installationAccessTokenEndpointUrl
                = String.format("https://api.github.com/app/installations/%s/access_tokens", githubConfig.getGithubAppInstallationId());

        this.objectMapper = objectMapper;
    }

    public GithubHandle getGithubHandle() throws URISyntaxException, IOException, InterruptedException {

        GitHub gitHub = getGithub();
        long creationTime = System.currentTimeMillis();

        return new GithubHandle(gitHub, creationTime, this);
    }

    public GitHub getGithub() throws URISyntaxException, IOException, InterruptedException {

        String signedJwt = Jwts.builder()
                .issuer(githubConfig.getGithubAppId())
                .issuedAt(getDateOneMinuteInThePast())
                .expiration(getDateThreeMinutesInTheFuture())
                .signWith(this.privateKey, Jwts.SIG.RS256)
                .compact();

        String installationAccessToken = getInstallationAccessToken(signedJwt);

        return new GitHubBuilder().withAppInstallationToken(installationAccessToken).build();
    }

    private Date getDateOneMinuteInThePast() {
        return Date.from(Instant.now(clock).minusSeconds(60));
    }

    private Date getDateThreeMinutesInTheFuture() {
        return Date.from(Instant.now(clock).plusSeconds(180));
    }

    private String getInstallationAccessToken(String signedJwt) throws URISyntaxException, IOException, InterruptedException {

        String authorizationHeaderValue = String.format("Bearer %s", signedJwt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(this.installationAccessTokenEndpointUrl))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", authorizationHeaderValue)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        String httpBody;

        try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.of(5, ChronoUnit.SECONDS)).build()) {

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            httpBody = response.body();

        }

        return getInstallationAccessTokenResponseFrom(httpBody).getToken();
    }

    private InstallationAccessTokenResponse getInstallationAccessTokenResponseFrom(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, InstallationAccessTokenResponse.class);
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class InstallationAccessTokenResponse {

        private final String token;

        @ConstructorProperties({"token"})
        public InstallationAccessTokenResponse(String token) {
            this.token = token;
        }
    }

}
