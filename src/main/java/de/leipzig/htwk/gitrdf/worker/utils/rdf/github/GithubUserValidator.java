package de.leipzig.htwk.gitrdf.worker.utils.rdf.github;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    
    // Track users that have already been processed to avoid duplicates
    private static final Set<String> processedUsers = ConcurrentHashMap.newKeySet();
    
    /**
     * Validates that a GitHub user exists in RDF and creates the user if missing.
     * This should be called whenever referencing a user in issues, comments, etc.
     * 
     * @param writer The RDF writer to output triples
     * @param github The GitHub API client
     * @param ghUser The GitHub user to validate/create
     * @return The user URI if successful, null if user is invalid
     */
    public static String validateAndEnsureUser(StreamRDF writer, GitHub github, GHUser ghUser) {
        // Enhanced null safety checks
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
        } catch (Exception e) {
            log.warn("Failed to create user URI for login '{}': {}", login, e.getMessage());
            return null;
        }
        
        // Skip if already processed in this session
        if (processedUsers.contains(userUri)) {
            return userUri;
        }
        
        try {
            // Create the GitHub user RDF representation
            createGithubUserRdf(writer, ghUser, userUri);
            processedUsers.add(userUri);
            
            log.debug("Created GitHub user RDF for: {}", login);
            return userUri;
            
        } catch (Exception e) {
            log.warn("Failed to create GitHub user RDF for login '{}': {}", login, e.getMessage());
            // Try to create at least a basic user entry as fallback
            try {
                createBasicGithubUserRdf(writer, login, userUri);
                processedUsers.add(userUri);
                log.debug("Created fallback basic user RDF for: {}", login);
            } catch (Exception fallbackException) {
                log.error("Even fallback user creation failed for '{}': {}", login, fallbackException.getMessage());
            }
            return userUri; // Return URI even if some properties failed
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
        
        // Validate GitHub API client
        if (github == null) {
            log.warn("GitHub API client is null, cannot fetch user data for '{}'", login);
            return null;
        }
        
        // Validate writer
        if (writer == null) {
            log.error("StreamRDF writer is null, cannot create user RDF for '{}'", login);
            return null;
        }
        
        String userUri;
        try {
            userUri = GithubUriUtils.getUserUri(login);
        } catch (Exception e) {
            log.warn("Failed to create user URI for login '{}': {}", login, e.getMessage());
            return null;
        }
        
        // Skip if already processed
        if (processedUsers.contains(userUri)) {
            return userUri;
        }
        
        try {
            GHUser ghUser = github.getUser(login);
            if (ghUser == null) {
                log.warn("GitHub API returned null user for login '{}'", login);
                // Still create basic user entry with available info
                createBasicGithubUserRdf(writer, login, userUri);
                processedUsers.add(userUri);
                return userUri;
            }
            return validateAndEnsureUser(writer, github, ghUser);
        } catch (IOException e) {
            log.warn("Failed to fetch GitHub user '{}' from API: {}", login, e.getMessage());
            // Still create basic user entry with available info
            try {
                createBasicGithubUserRdf(writer, login, userUri);
                processedUsers.add(userUri);
                return userUri;
            } catch (Exception fallbackException) {
                log.error("Even basic user creation failed for '{}': {}", login, fallbackException.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("Unexpected error while processing user '{}': {}", login, e.getMessage());
            // Try basic fallback
            try {
                createBasicGithubUserRdf(writer, login, userUri);
                processedUsers.add(userUri);
                return userUri;
            } catch (Exception fallbackException) {
                log.error("Fallback user creation failed for '{}': {}", login, fallbackException.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Creates complete GitHub user RDF representation
     */
    private static void createGithubUserRdf(StreamRDF writer, GHUser ghUser, String userUri) throws IOException {
        // Null safety check for the user object itself
        if (ghUser == null) {
            log.warn("Attempted to create RDF for null GHUser, creating basic entry instead");
            createBasicGithubUserRdf(writer, "unknown", userUri);
            return;
        }
        
        // Create the GitHub user type
        writer.triple(RdfGithubUserUtils.createGitHubUserType(userUri));
        
        // Add user ID - handle potential null/exception from API
        try {
            long userId = ghUser.getId();
            writer.triple(RdfGithubUserUtils.createUserIdProperty(userUri, userId));
        } catch (Exception e) {
            log.debug("Failed to get user ID for {}: {}", userUri, e.getMessage());
        }
        
        // Add login (username) - safely handle null
        try {
            String login = ghUser.getLogin();
            if (login != null && !login.trim().isEmpty()) {
                writer.triple(RdfGithubUserUtils.createLoginProperty(userUri, login));
            }
        } catch (Exception e) {
            log.debug("Failed to get login for {}: {}", userUri, e.getMessage());
        }
        
        // Add name if available - safely handle null and API exceptions
        try {
            String name = ghUser.getName();
            if (name != null && !name.trim().isEmpty()) {
                writer.triple(RdfGithubUserUtils.createNameProperty(userUri, name));
            }
        } catch (Exception e) {
            log.debug("Failed to get name for {}: {}", userUri, e.getMessage());
        }
        
        // Add email if available - safely handle null and API exceptions
        try {
            String email = ghUser.getEmail();
            if (email != null && !email.trim().isEmpty()) {
                writer.triple(RdfGithubUserUtils.createEmailProperty(userUri, email));
            }
        } catch (Exception e) {
            log.debug("Failed to get email for {}: {}", userUri, e.getMessage());
        }
        
        // Add user type if available - safely handle null and API exceptions
        try {
            String userType = ghUser.getType();
            if (userType != null && !userType.trim().isEmpty()) {
                writer.triple(RdfGithubUserUtils.createUserTypeProperty(userUri, userType));
            }
        } catch (Exception e) {
            log.debug("Failed to get user type for {}: {}", userUri, e.getMessage());
        }
    }
    
    /**
     * Creates basic GitHub user RDF representation when full API data is not available
     */
    private static void createBasicGithubUserRdf(StreamRDF writer, String login, String userUri) {
        // Enhanced null safety checks
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
        
        try {
            // Create the GitHub user type
            writer.triple(RdfGithubUserUtils.createGitHubUserType(userUri));
            
            // Add login (username) - this is the minimum we need
            writer.triple(RdfGithubUserUtils.createLoginProperty(userUri, login.trim()));
            
            log.debug("Created basic GitHub user RDF for login: {}", login);
        } catch (Exception e) {
            log.error("Failed to create basic user RDF for '{}': {}", login, e.getMessage());
            throw e; // Re-throw to let caller handle
        }
    }
    
    /**
     * Clears the processed users cache - useful for testing or when starting fresh
     */
    public static void clearProcessedUsersCache() {
        processedUsers.clear();
    }
    
    /**
     * Gets the count of processed users in current session
     */
    public static int getProcessedUsersCount() {
        return processedUsers.size();
    }
}