package de.leipzig.htwk.gitrdf.worker.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import de.leipzig.htwk.gitrdf.worker.utils.GithubUriUtils;
import de.leipzig.htwk.gitrdf.worker.utils.rdf.github.GithubUserValidator;

/**
 * Comprehensive live integration test for GitHub user entity creation and RDF validation.
 * This test diagnoses issues with users not getting entities created properly.
 *
 * Test Configuration:
 * - Uses GitHub credentials from .env file (GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN_1)
 * - Tests various user types: regular users, bots, deleted accounts, non-existent users
 * - Validates both API fetch scenarios and RDF entity creation
 * - Includes comprehensive edge cases that commonly cause user entity creation failures
 *
 * Test Coverage:
 * - Regular GitHub users that should exist
 * - Bot accounts with special characters in names 
 * - Deleted/ghost accounts (404 responses)
 * - Non-existent users
 * - Users with Unicode characters
 * - URI format inputs vs plain usernames
 * - API failure scenarios
 */
@DisplayName("GitHub User Entity Creation Live Tests")
public class GithubUserLiveTest {

    private static GitHub github;

    // Comprehensive test cases covering different user types and edge cases
    private static final TestUser[] TEST_USERS = new TestUser[] {
        // Regular users (should exist and have full API data)
        new TestUser("octocat", UserType.REGULAR, true),
        new TestUser("torvalds", UserType.REGULAR, true),
        
        // Bot accounts (may have limited API data, special character encoding issues)
        new TestUser("dependabot[bot]", UserType.BOT, false),
        new TestUser("github-actions[bot]", UserType.BOT, false), 
        new TestUser("Copilot", UserType.BOT, false),
        new TestUser("renovate[bot]", UserType.BOT, false),
        new TestUser("codecov[bot]", UserType.BOT, false),
        
        // Deleted/ghost accounts (404 responses but should still create entities)
        new TestUser("ghost", UserType.GHOST, false),
        
        // Non-existent users (should create fallback entities)
        new TestUser("this-user-definitely-does-not-exist-12345", UserType.NONEXISTENT, false),
        
        // Users with special characters/Unicode (encoding test cases)
        new TestUser("user-with-dashes", UserType.REGULAR, false),
        new TestUser("user_with_underscores", UserType.REGULAR, false),
        
        // URI format inputs (should be auto-detected and decoded)
        new TestUser("https://github.com/octocat", UserType.REGULAR, true),
        new TestUser("https://github.com/dependabot%5Bbot%5D", UserType.BOT, false),
    };

    private enum UserType {
        REGULAR,    // Normal GitHub user
        BOT,        // Bot account 
        GHOST,      // Deleted account placeholder
        NONEXISTENT // User that doesn't exist
    }
    
    private static class TestUser {
        final String input;
        final UserType type;
        final boolean expectApiSuccess;
        
        TestUser(String input, UserType type, boolean expectApiSuccess) {
            this.input = input;
            this.type = type;
            this.expectApiSuccess = expectApiSuccess;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s)", input, type);
        }
    }

    @BeforeAll
    static void setupClient() throws IOException {
        github = buildGitHubClient();
        boolean isAuthenticated = github.isCredentialValid();
        System.out.println("GitHub client initialized. Authenticated: " + isAuthenticated);
        
        if (!isAuthenticated) {
            System.out.println("WARNING: Running without valid GitHub authentication. Rate limits will be very restrictive.");
        }
        
        // Display rate limit info
        try {
            var rateLimit = github.getRateLimit();
            System.out.println("Rate Limit - Core: " + rateLimit.getCore().getRemaining() + "/" + rateLimit.getCore().getLimit());
        } catch (IOException e) {
            System.out.println("Could not fetch rate limit info: " + e.getMessage());
        }
    }

    @BeforeEach
    void clearUserCache() {
        // Ensure each test starts with a fresh cache
        GithubUserValidator.clearProcessedUsersCache();
        System.out.println("Cleared user validation cache. Cache size: " + GithubUserValidator.getProcessedUsersCount());
    }

    @Test
    @DisplayName("Comprehensive User Entity Creation Test")
    void testComprehensiveUserEntityCreation() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE GITHUB USER ENTITY CREATION TEST");
        System.out.println("=".repeat(80));
        
        int totalTests = TEST_USERS.length;
        int successfulApiCalls = 0;
        int successfulRdfCreations = 0;
        int failedRdfCreations = 0;
        List<String> problemUsers = new ArrayList<>();

        for (int i = 0; i < TEST_USERS.length; i++) {
            TestUser testUser = TEST_USERS[i];
            System.out.println(String.format("\n[%d/%d] Testing: %s", i + 1, totalTests, testUser));
            System.out.println("-".repeat(60));

            try {
                boolean testResult = testSingleUser(testUser);
                if (testResult) {
                    successfulRdfCreations++;
                    if (testUser.expectApiSuccess) {
                        successfulApiCalls++;
                    }
                } else {
                    failedRdfCreations++;
                    problemUsers.add(testUser.toString());
                }
            } catch (Exception e) {
                System.out.println("CRITICAL ERROR testing " + testUser + ": " + e.getMessage());
                e.printStackTrace();
                failedRdfCreations++;
                problemUsers.add(testUser.toString() + " (Exception: " + e.getMessage() + ")");
            }
        }

        // Print comprehensive test summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Successful RDF Creations: " + successfulRdfCreations);
        System.out.println("Failed RDF Creations: " + failedRdfCreations);
        System.out.println("API Success Rate: " + successfulApiCalls + "/" + Arrays.stream(TEST_USERS).mapToInt(u -> u.expectApiSuccess ? 1 : 0).sum());
        System.out.println("RDF Success Rate: " + (successfulRdfCreations * 100 / totalTests) + "%");
        
        if (!problemUsers.isEmpty()) {
            System.out.println("\nPROBLEM USERS:");
            problemUsers.forEach(user -> System.out.println("  - " + user));
        }

        // CRITICAL: All RDF creations should succeed (this is the main issue to diagnose)
        if (failedRdfCreations > 0) {
            Assertions.fail(String.format(
                "RDF entity creation failed for %d out of %d users. " +
                "This indicates the core issue with users not getting entities created. " +
                "Problem users: %s", 
                failedRdfCreations, totalTests, problemUsers));
        }

        System.out.println("\n✅ ALL TESTS PASSED: User entity creation is working correctly for all test cases.");
    }

    /**
     * Tests a single user through the complete validation workflow
     * @param testUser The user to test
     * @return true if RDF creation succeeded, false otherwise
     */
    private boolean testSingleUser(TestUser testUser) {
        String input = testUser.input;
        
        // Determine expected username (decode URI if necessary)
        String expectedUsername = input.startsWith("https://github.com/") 
            ? GithubUriUtils.getUsernameFromUri(input)
            : input;
            
        System.out.println("Input: " + input);
        System.out.println("Expected Username: " + expectedUsername);
        System.out.println("Type: " + testUser.type);
        System.out.println("Expect API Success: " + testUser.expectApiSuccess);

        // Step 1: Test native GitHub API fetch
        GHUser apiUser = testNativeApiAccess(input, testUser);
        
        // Step 2: Test RDF creation via login string (primary method - should always work)
        boolean loginRdfSuccess = testRdfCreationViaLogin(input, expectedUsername);
        
        // Step 3: Test RDF creation via GHUser object (if API returned user)
        boolean ghUserRdfSuccess = true; // Default to true if not tested
        if (apiUser != null) {
            ghUserRdfSuccess = testRdfCreationViaGHUser(apiUser, expectedUsername);
        }

        // Step 4: Validate RDF output quality
        if (loginRdfSuccess) {
            validateRdfQuality(input, expectedUsername, testUser.type);
        }

        return loginRdfSuccess && ghUserRdfSuccess;
    }

    private GHUser testNativeApiAccess(String input, TestUser testUser) {
        System.out.println("\n🔍 STEP 1: Native GitHub API Access Test");
        
        try {
            // Use clean username for API call (not URI)
            String usernameForApi = input.startsWith("https://github.com/") 
                ? GithubUriUtils.getUsernameFromUri(input)
                : input;
                
            GHUser user = github.getUser(usernameForApi);
            if (user == null) {
                System.out.println("❌ API returned null user");
                return null;
            }

            System.out.println("✅ API Success:");
            System.out.println("  Login: " + safe(user.getLogin()));
            System.out.println("  ID: " + safeId(user));
            System.out.println("  Type: " + safe(user.getType()));
            System.out.println("  Name: " + safe(user.getName()));
            System.out.println("  Email: " + safe(user.getEmail()));
            
            if (testUser.expectApiSuccess) {
                System.out.println("✅ Expected API success - got it!");
            } else {
                System.out.println("⚠️  Unexpected API success (expected failure)");
            }
            
            return user;
            
        } catch (org.kohsuke.github.HttpException e) {
            System.out.println("❌ API HttpException: code=" + e.getResponseCode() + ", msg=" + e.getMessage());
            if (!testUser.expectApiSuccess) {
                System.out.println("✅ Expected API failure - got it!");
            }
            return null;
        } catch (IOException e) {
            System.out.println("❌ API IOException: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("❌ API Unexpected exception: " + e.getMessage());
            return null;
        }
    }

    private boolean testRdfCreationViaLogin(String input, String expectedUsername) {
        System.out.println("\n🏗️  STEP 2: RDF Creation via Login String Test");
        
        Model model = ModelFactory.createDefaultModel();
        StreamRDF writer = StreamRDFLib.graph(model.getGraph());
        
        try {
            writer.start();
            String userUri = GithubUserValidator.validateAndEnsureUser(writer, github, input);
            writer.finish();

            if (userUri == null) {
                System.out.println("❌ RDF Creation FAILED: validateAndEnsureUser returned null");
                return false;
            }

            System.out.println("✅ RDF Creation SUCCESS:");
            System.out.println("  Input: " + input);
            System.out.println("  Generated URI: " + userUri);
            System.out.println("  Expected Username: " + expectedUsername);
            System.out.println("  Model Size: " + model.size() + " triples");

            if (model.size() == 0) {
                System.out.println("❌ WARNING: RDF model is empty - no triples created!");
                return false;
            }

            // Print the RDF for inspection
            System.out.println("  Generated RDF:");
            RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
            
            return true;

        } catch (Exception e) {
            writer.finish();
            System.out.println("❌ RDF Creation EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean testRdfCreationViaGHUser(GHUser user, String expectedUsername) {
        System.out.println("\n👤 STEP 3: RDF Creation via GHUser Object Test");
        
        // Check cache state to understand behavior
        int cacheSize = GithubUserValidator.getProcessedUsersCount();
        System.out.println("  Cache size before GHUser test: " + cacheSize);
        
        Model model = ModelFactory.createDefaultModel();
        StreamRDF writer = StreamRDFLib.graph(model.getGraph());
        
        try {
            writer.start();
            String userUri = GithubUserValidator.validateAndEnsureUser(writer, github, user);
            writer.finish();

            if (userUri == null) {
                System.out.println("❌ GHUser RDF Creation FAILED: validateAndEnsureUser returned null");
                return false;
            }

            System.out.println("✅ GHUser RDF Creation SUCCESS:");
            System.out.println("  Generated URI: " + userUri);
            System.out.println("  Model Size: " + model.size() + " triples");

            if (model.size() == 0 && cacheSize > 0) {
                System.out.println("ℹ️  Empty model expected - user was already cached from previous step");
                System.out.println("   This is CORRECT behavior: cache prevents duplicate RDF creation");
            } else if (model.size() == 0 && cacheSize == 0) {
                System.out.println("❌ WARNING: GHUser RDF model is empty but cache was empty - possible validation bug!");
                return false;
            } else {
                // Print the RDF for inspection
                System.out.println("  Generated RDF (via GHUser):");
                RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
            }

            // The key test: URI should never be null regardless of cache state
            return userUri != null;

        } catch (Exception e) {
            writer.finish();
            System.out.println("❌ GHUser RDF Creation EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void validateRdfQuality(String input, String expectedUsername, UserType type) {
        System.out.println("\n🔍 STEP 4: RDF Quality Validation");
        
        // Test URI encoding/decoding consistency
        try {
            String generatedUri = GithubUriUtils.getUserUri(expectedUsername);
            String decodedUsername = GithubUriUtils.getUsernameFromUri(generatedUri);
            
            System.out.println("  Original Username: " + expectedUsername);
            System.out.println("  Generated URI: " + generatedUri);
            System.out.println("  Decoded Username: " + decodedUsername);
            
            if (!expectedUsername.equals(decodedUsername)) {
                System.out.println("❌ WARNING: URI encoding/decoding mismatch!");
                System.out.println("    Expected: '" + expectedUsername + "'");
                System.out.println("    Got: '" + decodedUsername + "'");
            } else {
                System.out.println("✅ URI encoding/decoding is consistent");
            }
            
        } catch (Exception e) {
            System.out.println("❌ URI validation failed: " + e.getMessage());
        }
    }

    private static GitHub buildGitHubClient() throws IOException {
        System.out.println("🔧 Initializing GitHub client...");

        // Try environment variables in order of preference
        String token = env("GITHUB_TOKEN");
        if (token == null) {
            token = env("GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN");
        }
        if (token == null) {
            token = env("GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN_1");
        }

        if (token != null && !token.isBlank()) {
            System.out.println("✅ Found GitHub token in environment (length: " + token.length() + ")");
            return new GitHubBuilder().withOAuthToken(token.trim()).build();
        }

        // Anonymous fallback with warning
        System.out.println("⚠️  No GitHub token found in environment variables:");
        System.out.println("   - GITHUB_TOKEN");
        System.out.println("   - GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN");
        System.out.println("   - GITHUB_LOGIN_SYSTEM_USER_PERSONALACCESSTOKEN_1");
        System.out.println("🔄 Using anonymous client (very limited rate limits - 60 requests/hour)");
        
        return GitHub.connectAnonymously();
    }

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null) {
            v = System.getProperty(key);
        }
        return v;
    }

    private static String safe(String s) {
        return s == null ? "<null>" : s;
    }

    private static String safeId(GHUser u) {
        try {
            return String.valueOf(u.getId());
        } catch (Exception e) {
            return "<err:" + e.getMessage() + ">";
        }
    }

    @Test
    @DisplayName("Quick Single User Test")
    void testQuickSingleUser() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("QUICK SINGLE USER TEST");
        System.out.println("=".repeat(50));
        
        // Test just one problematic user to debug quickly
        TestUser quickTestUser = new TestUser("dependabot[bot]", UserType.BOT, false);
        
        boolean result = testSingleUser(quickTestUser);
        
        if (result) {
            System.out.println("\n✅ Quick test PASSED for: " + quickTestUser);
        } else {
            System.out.println("\n❌ Quick test FAILED for: " + quickTestUser);
            Assertions.fail("Quick test failed for " + quickTestUser + " - this helps isolate the user entity creation issue");
        }
    }

    @Test
    @DisplayName("Production Realistic User Entity Test")
    void testProductionRealisticUserEntityCreation() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("PRODUCTION REALISTIC USER ENTITY TEST");
        System.out.println("=".repeat(50));
        
        // Clear cache
        GithubUserValidator.clearProcessedUsersCache();
        
        // Test exactly how production processes users:
        // 1. Gets GHUser from GitHub API
        // 2. Calls validateAndEnsureUser(writer, github, ghUser)  
        // 3. Expects user entity to be created
        
        String testUsername = "octocat";
        
        try {
            // Step 1: Fetch user from GitHub API (same as production)
            GHUser ghUser = github.getUser(testUsername);
            System.out.println("Fetched user from API: " + ghUser.getLogin());
            
            // Step 2: Create RDF model (same as production)
            Model model = ModelFactory.createDefaultModel();
            StreamRDF writer = StreamRDFLib.graph(model.getGraph());
            
            // Step 3: Call validateAndEnsureUser exactly as production does
            writer.start();
            String userUri = GithubUserValidator.validateAndEnsureUser(writer, github, ghUser);
            writer.finish();
            
            System.out.println("Returned userUri: " + userUri);
            System.out.println("Model size: " + model.size());
            
            // Step 4: Verify user entity was created
            if (userUri == null) {
                System.out.println("❌ CRITICAL: validateAndEnsureUser returned null");
                Assertions.fail("User validation returned null URI");
            }
            
            if (model.size() == 0) {
                System.out.println("❌ CRITICAL: No triples created - this is the production bug!");
                System.out.println("   - URI was returned: " + userUri);
                System.out.println("   - But user entity triples are missing");
                System.out.println("   - This explains why users don't get entities created");
                
                // Print RDF anyway to see what's there
                System.out.println("Generated RDF (should be empty):");
                RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
                
                Assertions.fail("Production bug reproduced: User URI returned but no entity triples created");
            } else {
                System.out.println("✅ User entity created successfully");
                System.out.println("Generated RDF:");
                RDFDataMgr.write(System.out, model, RDFFormat.TURTLE_PRETTY);
            }
            
            // Test pattern: simulate processing multiple entities referencing same user
            System.out.println("\n--- Testing Multiple References Pattern ---");
            
            // Simulate: Issue #1 references user
            Model issueModel1 = ModelFactory.createDefaultModel();
            StreamRDF issueWriter1 = StreamRDFLib.graph(issueModel1.getGraph());
            issueWriter1.start();
            String userUri1 = GithubUserValidator.validateAndEnsureUser(issueWriter1, github, ghUser);
            issueWriter1.finish();
            
            // Simulate: Issue #2 references same user  
            Model issueModel2 = ModelFactory.createDefaultModel();
            StreamRDF issueWriter2 = StreamRDFLib.graph(issueModel2.getGraph());
            issueWriter2.start();
            String userUri2 = GithubUserValidator.validateAndEnsureUser(issueWriter2, github, ghUser);
            issueWriter2.finish();
            
            System.out.println("Issue #1 model size: " + issueModel1.size());
            System.out.println("Issue #2 model size: " + issueModel2.size());
            
            boolean bothHaveUserEntity = issueModel1.size() > 0 && issueModel2.size() > 0;
            boolean cacheSkippedSecond = issueModel1.size() > 0 && issueModel2.size() == 0;
            
            if (cacheSkippedSecond) {
                System.out.println("🎯 PRODUCTION BUG CONFIRMED:");
                System.out.println("   - Issue #1: User entity created (" + issueModel1.size() + " triples)");
                System.out.println("   - Issue #2: User entity SKIPPED due to cache (0 triples)");
                System.out.println("   - This is why 'some users do not get entities created'");
                Assertions.fail("Cache prevents user entities from being created in multiple models");
            } else if (bothHaveUserEntity) {
                System.out.println("✅ Both issues have user entities - cache working correctly");
            } else {
                System.out.println("❓ Unexpected pattern - both models empty");
                Assertions.fail("Neither model got user entities");
            }
            
        } catch (IOException e) {
            System.out.println("❌ GitHub API error: " + e.getMessage());
            Assertions.fail("GitHub API call failed: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("URI vs Username Cache Issue Test")
    void testUriVsUsernameCacheIssue() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("URI vs USERNAME CACHE ISSUE TEST");
        System.out.println("=".repeat(50));
        
        // Clear cache
        GithubUserValidator.clearProcessedUsersCache();
        System.out.println("Initial cache size: " + GithubUserValidator.getProcessedUsersCount());
        
        String username = "octocat";
        String uri = "https://github.com/octocat";
        
        // Test 1: Create user via plain username
        System.out.println("\n--- Test 1: Plain username '" + username + "' ---");
        Model model1 = ModelFactory.createDefaultModel();
        StreamRDF writer1 = StreamRDFLib.graph(model1.getGraph());
        writer1.start();
        String userUri1 = GithubUserValidator.validateAndEnsureUser(writer1, github, username);
        writer1.finish();
        
        System.out.println("Result 1 - URI: " + userUri1);
        System.out.println("Result 1 - Model size: " + model1.size());
        System.out.println("Cache size after test 1: " + GithubUserValidator.getProcessedUsersCount());
        
        // Test 2: Create user via URI format (same user)
        System.out.println("\n--- Test 2: URI format '" + uri + "' ---");
        Model model2 = ModelFactory.createDefaultModel();
        StreamRDF writer2 = StreamRDFLib.graph(model2.getGraph());
        writer2.start();
        String userUri2 = GithubUserValidator.validateAndEnsureUser(writer2, github, uri);
        writer2.finish();
        
        System.out.println("Result 2 - URI: " + userUri2);
        System.out.println("Result 2 - Model size: " + model2.size());
        System.out.println("Cache size after test 2: " + GithubUserValidator.getProcessedUsersCount());
        
        // Analysis
        System.out.println("\n--- ANALYSIS ---");
        System.out.println("Same user URI returned: " + userUri1.equals(userUri2));
        System.out.println("Model 1 has triples: " + (model1.size() > 0));
        System.out.println("Model 2 has triples: " + (model2.size() > 0));
        
        if (model1.size() > 0 && model2.size() == 0) {
            System.out.println("✅ CONFIRMED: This is the cache issue!");
            System.out.println("   - Plain username creates RDF triples and adds to cache");
            System.out.println("   - URI format gets cached hit and creates NO triples in new model");
            System.out.println("   - This explains why 'some users do not get entities created'");
        } else {
            System.out.println("❓ Unexpected behavior - need further investigation");
        }
        
        // Test 3: Fresh cache test - URI format first
        System.out.println("\n--- Test 3: Fresh cache, URI format first ---");
        GithubUserValidator.clearProcessedUsersCache();
        Model model3 = ModelFactory.createDefaultModel();
        StreamRDF writer3 = StreamRDFLib.graph(model3.getGraph());
        writer3.start();
        String userUri3 = GithubUserValidator.validateAndEnsureUser(writer3, github, uri);
        writer3.finish();
        
        System.out.println("Result 3 - URI: " + userUri3);
        System.out.println("Result 3 - Model size: " + model3.size());
        
        if (model3.size() > 0) {
            System.out.println("✅ URI format works when cache is empty");
        } else {
            System.out.println("❌ URI format fails even with empty cache - different issue!");
        }
    }
    
    @Test 
    @DisplayName("Cache Behavior Test")
    void testCacheBehavior() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("CACHE BEHAVIOR TEST");
        System.out.println("=".repeat(50));
        
        String testUsername = "octocat";
        
        // Clear cache and verify
        GithubUserValidator.clearProcessedUsersCache();
        int initialCacheSize = GithubUserValidator.getProcessedUsersCount();
        System.out.println("Initial cache size: " + initialCacheSize);
        
        // First creation should add to cache
        Model model1 = ModelFactory.createDefaultModel();
        StreamRDF writer1 = StreamRDFLib.graph(model1.getGraph());
        writer1.start();
        String uri1 = GithubUserValidator.validateAndEnsureUser(writer1, github, testUsername);
        writer1.finish();
        
        int cacheAfterFirst = GithubUserValidator.getProcessedUsersCount();
        System.out.println("Cache size after first creation: " + cacheAfterFirst);
        System.out.println("First URI: " + uri1);
        System.out.println("First model size: " + model1.size());
        
        // Second creation should use cache (same user)
        Model model2 = ModelFactory.createDefaultModel();
        StreamRDF writer2 = StreamRDFLib.graph(model2.getGraph());
        writer2.start();
        String uri2 = GithubUserValidator.validateAndEnsureUser(writer2, github, testUsername);
        writer2.finish();
        
        int cacheAfterSecond = GithubUserValidator.getProcessedUsersCount();
        System.out.println("Cache size after second creation: " + cacheAfterSecond);
        System.out.println("Second URI: " + uri2);
        System.out.println("Second model size: " + model2.size());
        
        // Verify cache behavior
        Assertions.assertEquals(0, initialCacheSize, "Initial cache should be empty");
        Assertions.assertEquals(1, cacheAfterFirst, "Cache should contain 1 user after first creation");
        Assertions.assertEquals(1, cacheAfterSecond, "Cache should still contain 1 user after second creation");
        Assertions.assertEquals(uri1, uri2, "Both calls should return same URI");
        
        // Note: Second model might be smaller if caching prevents duplicate triple creation
        if (model2.size() < model1.size()) {
            System.out.println("✅ Cache prevented duplicate triple creation (expected behavior)");
        } else if (model2.size() == model1.size()) {
            System.out.println("ℹ️  Cache allowed duplicate triple creation (also valid)");
        } else {
            System.out.println("⚠️  Second model larger than first - unexpected");
        }
        
        System.out.println("\n✅ Cache behavior test completed");
    }
}
