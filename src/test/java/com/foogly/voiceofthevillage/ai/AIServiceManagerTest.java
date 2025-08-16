package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;
import com.foogly.voiceofthevillage.data.VillagerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIServiceManager.
 */
class AIServiceManagerTest {

    private AIServiceManager aiServiceManager;
    private VillagerData testVillagerData;

    @BeforeEach
    void setUp() {
        // Create test data
        testVillagerData = new VillagerData(
            UUID.randomUUID(),
            "TestVillager",
            Gender.MALE,
            PersonalityType.FRIENDLY
        );

        // Get fresh instance for each test
        aiServiceManager = AIServiceManager.getInstance();
    }

    @Test
    void testSingletonInstance() {
        AIServiceManager instance1 = AIServiceManager.getInstance();
        AIServiceManager instance2 = AIServiceManager.getInstance();
        
        assertSame(instance1, instance2, "AIServiceManager should be a singleton");
    }

    @Test
    void testIsConfiguredWithoutValidConfig() {
        // Without proper configuration, should not be configured
        // This depends on the actual config values, but in test environment it should be false
        boolean isConfigured = aiServiceManager.isConfigured();
        
        // In test environment, we expect this to be false since no real API key is configured
        assertFalse(isConfigured, "AIServiceManager should not be configured without valid API key");
    }

    @Test
    void testGetCurrentProviderName() {
        String providerName = aiServiceManager.getCurrentProviderName();
        
        assertNotNull(providerName, "Provider name should not be null");
        assertTrue(providerName.equals("OpenAI") || providerName.equals("None"), 
                  "Provider name should be OpenAI or None");
    }

    @Test
    void testGetCurrentModel() {
        String model = aiServiceManager.getCurrentModel();
        
        assertNotNull(model, "Model should not be null");
    }

    @Test
    void testGenerateResponseWithoutConfiguration() throws ExecutionException, InterruptedException {
        // Test response generation when not configured
        CompletableFuture<AIResponse> future = aiServiceManager.generateResponse(
            "Hello", testVillagerData, null, null
        );

        AIResponse response = future.get();
        
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isSuccess(), "Response should not be successful without configuration");
        assertNotNull(response.getErrorMessage(), "Error message should be provided");
        assertTrue(response.getErrorMessage().contains("not properly configured"), 
                  "Error message should indicate configuration issue");
    }

    @Test
    void testRegisterProvider() {
        // Create a mock provider
        MockAIProvider mockProvider = new MockAIProvider(true, "Hello there, friend!");

        // Register the provider
        aiServiceManager.registerProvider("TestProvider", mockProvider);

        // Verify it was registered (we can't directly test this without exposing internals,
        // but we can test switching to it)
        boolean switched = aiServiceManager.switchProvider("TestProvider");
        assertTrue(switched, "Should be able to switch to registered provider");
        
        assertEquals("MockProvider", aiServiceManager.getCurrentProviderName(),
                    "Current provider should be the one we switched to");
    }

    @Test
    void testSwitchToNonExistentProvider() {
        boolean switched = aiServiceManager.switchProvider("NonExistentProvider");
        
        assertFalse(switched, "Should not be able to switch to non-existent provider");
    }

    @Test
    void testGetStatistics() {
        String statistics = aiServiceManager.getStatistics();
        
        assertNotNull(statistics, "Statistics should not be null");
        assertTrue(statistics.contains("AI Service Statistics"), 
                  "Statistics should contain expected header");
        assertTrue(statistics.contains("Provider="), 
                  "Statistics should contain provider information");
        assertTrue(statistics.contains("Active Requests="), 
                  "Statistics should contain request information");
    }

    @Test
    void testReloadConfiguration() {
        // This should not throw an exception
        assertDoesNotThrow(() -> aiServiceManager.reloadConfiguration(),
                          "Reloading configuration should not throw exception");
    }

    @Test
    void testShutdown() {
        // This should not throw an exception
        assertDoesNotThrow(() -> aiServiceManager.shutdown(),
                          "Shutdown should not throw exception");
    }

    /**
     * Mock AI provider for testing purposes.
     */
    private static class MockAIProvider extends AIProvider {
        private final boolean configured;
        private final String responseText;

        public MockAIProvider(boolean configured, String responseText) {
            super("test-key", "test-model", 30);
            this.configured = configured;
            this.responseText = responseText;
        }

        @Override
        public CompletableFuture<AIResponse> generateResponse(String playerMessage, VillagerData villagerData,
                                                             Object player, GameContext gameContext) {
            if (!configured) {
                return CompletableFuture.completedFuture(
                    AIResponse.failure("Mock provider not configured")
                );
            }
            
            return CompletableFuture.completedFuture(
                AIResponse.success(responseText)
            );
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String getProviderName() {
            return "MockProvider";
        }

        @Override
        protected boolean isValidApiKey(String apiKey) {
            return "test-key".equals(apiKey);
        }

        @Override
        protected boolean isValidModel(String model) {
            return "test-model".equals(model);
        }
    }

    @Test
    void testWithMockProvider() throws ExecutionException, InterruptedException {
        // Create and register a mock provider
        MockAIProvider mockProvider = new MockAIProvider(true, "Hello there, friend!");
        aiServiceManager.registerProvider("MockProvider", mockProvider);
        aiServiceManager.switchProvider("MockProvider");

        // Test response generation
        CompletableFuture<AIResponse> future = aiServiceManager.generateResponse(
            "Hello", testVillagerData, null, null
        );

        AIResponse response = future.get();
        
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isSuccess(), "Response should be successful");
        assertEquals("Hello there, friend!", response.getResponseText(),
                    "Response text should match mock provider response");
    }
}