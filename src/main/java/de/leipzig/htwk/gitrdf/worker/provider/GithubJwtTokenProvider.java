package de.leipzig.htwk.gitrdf.worker.provider;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.authorization.AuthorizationProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class GithubJwtTokenProvider implements AuthorizationProvider {

    private final GithubConfig githubConfig;

    private final Clock clock;

    private final PrivateKey privateKey;

    public GithubJwtTokenProvider(
            GithubConfig githubConfig, Clock clock) throws NoSuchAlgorithmException, InvalidKeySpecException {

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
    }

    @Override
    public String getEncodedAuthorization() throws IOException {
        log.info("Retrieving signed jwt token");
        return "Bearer " + getSignedJwtTokenToFetchInstallationToken();
    }

    public String getSignedJwtTokenToFetchInstallationToken() {

        return Jwts.builder()
                .issuer(githubConfig.getGithubAppId())
                .issuedAt(getDateOneMinuteInThePast())
                .expiration(getDateThreeMinutesInTheFuture())
                .signWith(this.privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private Date getDateOneMinuteInThePast() {
        return Date.from(Instant.now(clock).minusSeconds(60));
    }

    private Date getDateThreeMinutesInTheFuture() {
        return Date.from(Instant.now(clock).plusSeconds(180));
    }

}
