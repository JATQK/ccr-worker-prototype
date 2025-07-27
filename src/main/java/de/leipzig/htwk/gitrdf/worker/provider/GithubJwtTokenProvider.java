package de.leipzig.htwk.gitrdf.worker.provider;

import de.leipzig.htwk.gitrdf.worker.config.GithubConfig;
import de.leipzig.htwk.gitrdf.worker.service.GithubAccountRotationService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GithubJwtTokenProvider implements AuthorizationProvider {

    private final GithubAccountRotationService githubAccountRotationService;

    private final Clock clock;

    private final Map<Integer, PrivateKey> privateKeys = new ConcurrentHashMap<>();

    public GithubJwtTokenProvider(
            GithubConfig githubConfig, 
            GithubAccountRotationService githubAccountRotationService,
            Clock clock) throws NoSuchAlgorithmException, InvalidKeySpecException {

        // weird cryptography java adapter dependency
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider());

        this.githubAccountRotationService = githubAccountRotationService;
        this.clock = clock;
        
        // Initialize private keys for all accounts
        for (GithubConfig.GithubApiAccount account : githubConfig.getGithubApiAccounts()) {
            PrivateKey privateKey = createPrivateKey(account.getPemPrivateBase64Key());
            privateKeys.put(account.getAccountNumber(), privateKey);
        }
    }
    
    private PrivateKey createPrivateKey(String pemPrivateBase64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // not really pkcs8 encoded (its actually pkcs1), but with the bouncy castle provider above,
        // the key can be generated successfully nevertheless.
        // Sadly github only provides pkcs1 keys
        byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(pemPrivateBase64Key);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }

    @Override
    public String getEncodedAuthorization() throws IOException {
        log.info("Retrieving signed jwt token");
        return "Bearer " + getSignedJwtTokenToFetchInstallationToken();
    }

    public String getSignedJwtTokenToFetchInstallationToken() {
        GithubConfig.GithubApiAccount currentAccount = githubAccountRotationService.getCurrentAccount();
        PrivateKey currentPrivateKey = privateKeys.get(currentAccount.getAccountNumber());
        
        log.debug("Using GitHub API account {} for JWT token generation", currentAccount.getAccountNumber());

        return Jwts.builder()
                .issuer(currentAccount.getGithubAppId())
                .issuedAt(getDateOneMinuteInThePast())
                .expiration(getDateThreeMinutesInTheFuture())
                .signWith(currentPrivateKey, Jwts.SIG.RS256)
                .compact();
    }

    private Date getDateOneMinuteInThePast() {
        return Date.from(Instant.now(clock).minusSeconds(60));
    }

    private Date getDateThreeMinutesInTheFuture() {
        return Date.from(Instant.now(clock).plusSeconds(180));
    }

}
