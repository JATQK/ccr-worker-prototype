package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to validate and ensure GitHub users are properly created in RDF
 * when they are referenced in issues, comments, pull requests, etc.
 */
@Slf4j
public class GithubUserValidator {
    
    // Cache GitHub API user data to prevent duplicate API calls (but always write RDF to each model)
    private static final Map<String, GHUser> apiUserCache = new ConcurrentHashMap<>();

    public static String validateAndEnsureUser(StreamRDF writer, GitHub github, GHUser ghUser) {
        // Enhanced null safety checks - allow null writer for validation-only mode
        if (writer == null) {
            log.debug("StreamRDF writer is null, running in validation-only mode (no RDF output)");
        }
        
        if (ghUser == null) {
            log.debug("Received null GHUser, skipping user creation");
            return null;
        }
        
        String login;
        try {
            login = ghUser.getLogin();
        } catch (Exception e) {
            log.warn("Failed to get login from GHUser: {}", e.getMessage());
            return null;
        }
        
        if (login == null || login.trim().isEmpty()) {
            log.debug("GHUser has null or empty login, skipping user creation");
            return null;
        }
        
        String userUri;
        try {
            userUri = GithubUriUtils.getUserUri(login);
            // Extra logging for bot accounts to help debug encoding issues
            if (isBotAccount(login.toLowerCase())) {
                log.debug("Bot account detected: login='{}' -> URI='{}' (properly encoded)", login, userUri);
            }
        } catch (Exception e) {
            log.warn("Failed to create user URI for login '{}': {}", login, e.getMessage());
            return null;
        }
        
        try {
            // ALWAYS create the GitHub user RDF representation when writer is provided
            // (Cache only prevents duplicate API calls, not duplicate RDF creation)
            if (writer != null) {
                createGithubUserRdf(writer, ghUser, userUri);
                log.info("Created GitHub user RDF for: {}", login);
            } else {
                log.debug("Validation-only mode: Skipping RDF creation for: {}", login);
            }
            return userUri;
            
        } catch (Exception e) {
            log.warn("Failed to create GitHub user RDF for login '{}' using embedded user object: {}", login, e.getMessage());
            
            // FALLBACK: Try to fetch user directly from GitHub API if the embedded object failed
            if (github != null) {
                log.debug("Attempting fallback: fetching user '{}' directly from GitHub API", login);
                try {
                    GHUser directUser = github.getUser(login);
                    if (directUser != null) {
                        // Try again with the directly fetched user (only if writer is provided)
                        if (writer != null) {
                            createGithubUserRdf(writer, directUser, userUri);
                            log.info("Successfully created GitHub user RDF for '{}' using direct API fallback", login);
                        } else {
                            log.info("Validation-only mode: Direct API fallback successful for: {}", login);
                        }
                        return userUri;
                    }
                } catch (org.kohsuke.github.HttpException directFetchException) {
                    // Handle specific HTTP errors for direct fetch fallback
                    if (directFetchException.getResponseCode() == -1) {
                        log.warn("Direct API fallback connection failed for user '{}' (response code -1): {}", login, directFetchException.getMessage());
                    } else {
                        log.warn("Direct API fallback HTTP error {} for user '{}': {}", directFetchException.getResponseCode(), login, directFetchException.getMessage());
                    }
                } catch (Exception directFetchException) {
                    log.warn("Direct API fallback also failed for user '{}': {}", login, directFetchException.getMessage());
                }
            }
            
            // ALWAYS create a basic user entry as final fallback to ensure User entity exists
            if (writer != null) {
                createBasicGithubUserRdf(writer, login, userUri);
                log.info("Created fallback basic user RDF for: {} (API failures occurred but User entity is required)", login);
                return userUri;
            } else {
                // In validation-only mode, don't create RDF but still return URI
                log.debug("Validation-only mode: Basic fallback for: {}", login);
                return userUri;
            }
        }
    }
    
    /**
     * Overloaded method that fetches user details from GitHub API by login
     */
    public static String validateAndEnsureUser(StreamRDF writer, GitHub github, String login) {
        // Enhanced null safety checks
        if (login == null || login.trim().isEmpty()) {
            log.debug("Received null or empty login, skipping user creation");
            return null;
        }
        
        // Auto-detect and decode URI format usernames
        String actualLogin = login.trim();
        if (actualLogin.startsWith("https://github.com/")) {
            String decodedLogin = GithubUriUtils.getUsernameFromUri(actualLogin);
            if (decodedLogin != null) {
                log.info("Detected URI format login '{}', decoded to '{}'", actualLogin, decodedLogin);
                actualLogin = decodedLogin;
            }
        }
        
        // Validate GitHub API client
        if (github == null) {
            log.warn("GitHub API client is null, cannot fetch user data for '{}'", actualLogin);
            return null;
        }
        
        // Validate writer
        if (writer == null) {
            log.error("StreamRDF writer is null, cannot create user RDF for '{}'", actualLogin);
            return null;
        }
        
        String userUri;
        try {
            userUri = GithubUriUtils.getUserUri(actualLogin);
            // Extra logging for bot accounts to help debug encoding issues
            if (isBotAccount(actualLogin.toLowerCase())) {
                log.debug("Bot account detected: actualLogin='{}' -> URI='{}' (properly encoded)", actualLogin, userUri);
            }
        } catch (Exception e) {
            log.warn("Failed to create user URI for login '{}': {}", actualLogin, e.getMessage());
            return null;
        }
        
        try {
            // Check API cache first to prevent duplicate API calls
            GHUser ghUser = apiUserCache.get(actualLogin);
            if (ghUser == null) {
                // Not in cache - make API call
                ghUser = github.getUser(actualLogin);
                if (ghUser != null) {
                    // Cache the API result for future use
                    apiUserCache.put(actualLogin, ghUser);
                    log.debug("Cached GitHub user data for: {}", actualLogin);
                } else {
                    log.warn("GitHub API returned null user for login '{}'", actualLogin);
                    // Still create basic user entry with available info
                    createBasicGithubUserRdf(writer, actualLogin, userUri);
                    return userUri;
                }
            } else {
                log.debug("Using cached GitHub user data for: {}", actualLogin);
            }
            
            return validateAndEnsureUser(writer, github, ghUser);
        } catch (org.kohsuke.github.HttpException e) {
            // Create fallback users for all HTTP errors to ensure User entities always exist
            if (e.getResponseCode() == 404) {
                log.warn("GitHub user '{}' not found (404) - likely deleted account. Creating basic user entry.", actualLogin);
            } else {
                log.warn("GitHub API HTTP error {} for user '{}': {}. Creating fallback user entry to ensure User entity exists.", 
                         e.getResponseCode(), actualLogin, e.getMessage());
            }
            createBasicGithubUserRdf(writer, actualLogin, userUri);
            return userUri;
        } catch (IOException e) {
            // Create fallback users for IO errors to ensure User entities always exist
            log.warn("IO error while fetching GitHub user '{}': {}. Creating fallback user entry to ensure User entity exists.", 
                     actualLogin, e.getMessage());
            createBasicGithubUserRdf(writer, actualLogin, userUri);
            return userUri;
        } catch (Exception e) {
            // Create fallback users for all unexpected errors to ensure User entities always exist
            log.warn("Unexpected error while processing user '{}': {}. Creating fallback user entry to ensure User entity exists.", 
                     actualLogin, e.getMessage());
            createBasicGithubUserRdf(writer, actualLogin, userUri);
            return userUri;
        }
    }
    
    /**
     * Creates complete GitHub user RDF representation with rigorous validation
     */
    private static void createGithubUserRdf(StreamRDF writer, GHUser ghUser, String userUri) throws IOException {
        if (ghUser == null) {
            log.warn("Attempted to create RDF for null GHUser, creating basic entry instead");
            createBasicGithubUserRdf(writer, "unknown", userUri);
            return;
        }
        // ensure all required properties are created
        createGithubUserRdfAtomic(writer, ghUser, userUri);
    }
    
    /**
     * Creates complete GitHub user RDF representation ensuring all required triples succeed atomically
     */
    private static void createGithubUserRdfAtomic(StreamRDF writer, GHUser ghUser, String userUri) throws IOException {
        // Collect all triples to be written
        List<Triple> requiredTriples = new ArrayList<>();
        List<Triple> optionalTriples = new ArrayList<>();
        
        // REQUIRED: User type
        requiredTriples.add(RdfGithubUserUtils.createGitHubUserType(userUri));
        
        String login = null;
        try {
            login = ghUser.getLogin();
        } catch (Exception e) {
            log.debug("Failed to get login from GHUser: {}", e.getMessage());
        }
        
        // REQUIRED: At minimum login/username to proceed
        if (login != null && !login.trim().isEmpty()) {
            // github:login removed; only write platform:username
            requiredTriples.add(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, login));
            requiredTriples.add(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, login));
        } else {
            throw new IllegalArgumentException("Cannot create user RDF without valid login");
        }
        
        // OPTIONAL: Other properties
        try {
            long userId = ghUser.getId();
            optionalTriples.add(RdfGithubUserUtils.createUserIdProperty(userUri, userId));
        } catch (Exception e) {
            log.debug("Failed to get user ID for {}: {}", userUri, e.getMessage());
        }
        
        try {
            String name = ghUser.getName();
            if (name != null && !name.trim().isEmpty()) {
                optionalTriples.add(RdfGithubUserUtils.createNameProperty(userUri, name));
            }
        } catch (Exception e) {
            log.debug("Failed to get name for {}: {}", userUri, e.getMessage());
        }
        
        try {
            String email = ghUser.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                optionalTriples.add(RdfGithubUserUtils.createEmailProperty(userUri, email));
            }
        } catch (Exception e) {
            log.debug("Failed to get email for {}: {}", userUri, e.getMessage());
        }
        
        try {
            String userType = ghUser.getType();
            if (userType != null && !userType.trim().isEmpty()) {
                optionalTriples.add(RdfGithubUserUtils.createUserTypeProperty(userUri, userType));
            } else if (login != null && isBotAccount(login.toLowerCase())) {
                optionalTriples.add(RdfGithubUserUtils.createUserTypeProperty(userUri, "Bot"));
            }
        } catch (Exception e) {
            log.debug("Failed to get user type for {}: {}", userUri, e.getMessage());
            if (login != null && isBotAccount(login.toLowerCase())) {
                optionalTriples.add(RdfGithubUserUtils.createUserTypeProperty(userUri, "Bot"));
            }
        }
        
        // Write all required triples first
        for (Triple triple : requiredTriples) {
            writer.triple(triple);
        }
        
        // Write optional triples
        for (Triple triple : optionalTriples) {
            try {
                writer.triple(triple);
            } catch (Exception e) {
                log.debug("Failed to write optional triple for {}: {}", userUri, e.getMessage());
            }
        }
        
        log.info("Created complete GitHub user RDF with {} required + {} optional properties for: {}", 
            requiredTriples.size(), optionalTriples.size(), login);
    }
    
    private static void createBasicGithubUserRdf(StreamRDF writer, String login, String userUri) {
        if (writer == null) {
            log.error("Cannot create basic user RDF: writer is null");
            return;
        }
        if (login == null || login.trim().isEmpty()) {
            log.warn("Cannot create basic user RDF: login is null or empty");
            return;
        }
        if (userUri == null || userUri.trim().isEmpty()) {
            log.warn("Cannot create basic user RDF: userUri is null or empty");
            return;
        }


        List<Triple> requiredTriples = new ArrayList<>();
        List<Triple> optionalTriples = new ArrayList<>();
        
        try {
            // REQUIRED: User type
            requiredTriples.add(RdfGithubUserUtils.createGitHubUserType(userUri));
            
            // REQUIRED: Login and username properties
            requiredTriples.add(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, login.trim()));
            requiredTriples.add(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, login.trim()));
            
            // OPTIONAL: Bot type detection
            String normalizedLogin = login.trim().toLowerCase();
            if (isBotAccount(normalizedLogin)) {
                optionalTriples.add(RdfGithubUserUtils.createUserTypeProperty(userUri, "Bot"));
            }
            
            // Write required triples
            int successfulTriples = 0;
            for (Triple triple : requiredTriples) {
                try {
                    writer.triple(triple);
                    successfulTriples++;
                } catch (Exception e) {
                    log.error("CRITICAL: Failed to write required user triple for {}: {}", userUri, e.getMessage());
                    // Continue trying other triples
                }
            }
            
            if (successfulTriples == 0) {
                log.error("CRITICAL: Could not write ANY required triples user '{}' with URI '{}'. User entity may be incomplete!", login, userUri);
            }
            
            // Write optional triples
            for (Triple triple : optionalTriples) {
                try {
                    writer.triple(triple);
                } catch (Exception e) {
                    log.debug("Failed to write optional basic user triple for {}: {}", userUri, e.getMessage());
                }
            }
            
            // Log success based on account type
            if ("ghost".equals(normalizedLogin)) {
                log.debug("Created basic GitHub user RDF for deleted account (ghost user): {} ({} required triples)", login, successfulTriples);
            } else if (isBotAccount(normalizedLogin)) {
                log.debug("Created basic GitHub user RDF for bot account: {} ({} required triples)", login, successfulTriples);
            } else {
                log.debug("Created basic GitHub user RDF for login: {} ({} required triples)", login, successfulTriples);
            }
            
        } catch (Exception e) {
            log.error("CRITICAL: Unexpected error creating basic user RDF for '{}': {}. Continuing anyway to prevent RDF violations.", login, e.getMessage());
        }
    }
    
    /**
     * Determines if a login belongs to a known bot account
     */
    private static boolean isBotAccount(String normalizedLogin) {
        return "copilot".equals(normalizedLogin) || 
               "github-actions".equals(normalizedLogin) ||
               "dependabot".equals(normalizedLogin) ||
               "renovate".equals(normalizedLogin) ||
               "codecov".equals(normalizedLogin) ||
               "sonarcloud".equals(normalizedLogin) ||
               normalizedLogin.endsWith("-bot") ||
               normalizedLogin.endsWith("[bot]") ||
               normalizedLogin.contains("bot-") ||
               normalizedLogin.contains("-ci-") ||
               normalizedLogin.contains("automation");
    }

    /**
     * Clears the GitHub API user cache
     */
    public static void clearProcessedUsersCache() {
        apiUserCache.clear();
    }
    

    public static String createSafeUserEntity(StreamRDF writer, String decodedLogin) {
        if (decodedLogin == null || decodedLogin.trim().isEmpty()) {
            log.warn("Cannot create safe user entity: decodedLogin is null or empty");
            return null;
        }
        
        String cleanLogin = decodedLogin.trim();
        String userUri;
        
        try {
            // Create properly encoded URI from decoded login
            userUri = GithubUriUtils.getUserUri(cleanLogin);
            
            // always create RDF triples for current model
            
            // Log for bot accounts to aid debugging
            if (isBotAccount(cleanLogin.toLowerCase())) {
                log.info("Creating safe bot user entity: decodedLogin='{}' -> encodedURI='{}' (encoding: {})", 
                        cleanLogin, userUri, cleanLogin.equals(GithubUriUtils.getUsernameFromUri(userUri)) ? "VALID" : "INVALID");
            }
            
            // Create essential user entity triples with decoded username
            writer.triple(RdfGithubUserUtils.createGitHubUserType(userUri));
            // github:login removed; only write platform:username
            writer.triple(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, cleanLogin));  // Use decoded for RDF property
            writer.triple(RdfGithubUserUtils.createUsernamePropertyForGithub(userUri, cleanLogin));  // Use decoded for RDF property
            
            // Add bot type if applicable
            if (isBotAccount(cleanLogin.toLowerCase())) {
                writer.triple(RdfGithubUserUtils.createUserTypeProperty(userUri, "Bot"));
            }
            
            log.debug("Successfully created safe user entity for login '{}' with URI '{}'", cleanLogin, userUri);
            return userUri;
            
        } catch (Exception e) {
            log.error("Failed to create safe user entity for login '{}': {}", cleanLogin, e.getMessage());
            return null;
        }
    }
}