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
 * Unit tests for OpenAIProvider.
 */
class OpenAIProviderTest {

    private VillagerData testVillagerData;

    @BeforeEach
    void setUp() {
        // Create test data
        testVillagerData = new VillagerData(
            UUID.randomUUID(),
            "TestVillager",
            Gender.FEMALE,
            PersonalityType.CHEERFUL
        );
    }

    @Test
    void testValidApiKeyValidation() {
        OpenAIProvider provider = new OpenAIProvider("sk-1234567890123456789012345678901234567890123456789", "gpt-3.5-turbo", 30);
        
        assertTrue(provider.isValidApiKey("sk-1234567890123456789012345678901234567890123456789"),
                  "Valid OpenAI API key should be accepted");
        
        assertFalse(provider.isValidApiKey("invalid-key"),
                   "Invalid API key should be rejected");
        
        assertFalse(provider.isValidApiKey("sk-short"),
                   "Short API key should be rejected");
        
        assertFalse(provider.isValidApiKey(null),
                   "Null API key should be rejected");
        
        assertFalse(provider.isValidApiKey(""),
                   "Empty API key should be rejected");
    }

    @Test
    void testValidModelValidation() {
        OpenAIProvider provider = new OpenAIProvider("test-key", "gpt-3.5-turbo", 30);
        
        assertTrue(provider.isValidModel("gpt-3.5-turbo"),
                  "GPT-3.5-turbo should be valid");
        
        assertTrue(provider.isValidModel("gpt-4"),
                  "GPT-4 should be valid");
        
        assertTrue(provider.isValidModel("gpt-4-turbo"),
                  "GPT-4-turbo should be valid");
        
        assertTrue(provider.isValidModel("gpt-4o"),
                  "GPT-4o should be valid");
        
        assertFalse(provider.isValidModel("invalid-model"),
                   "Invalid model should be rejected");
        
        assertFalse(provider.isValidModel(null),
                   "Null model should be rejected");
        
        assertFalse(provider.isValidModel(""),
                   "Empty model should be rejected");
        
        assertFalse(provider.isValidModel("claude-3-sonnet"),
                   "Non-OpenAI model should be rejected");
    }

    @Test
    void testProviderName() {
        OpenAIProvider provider = new OpenAIProvider("test-key", "gpt-3.5-turbo", 30);
        
        assertEquals("OpenAI", provider.getProviderName(),
                    "Provider name should be OpenAI");
    }

    @Test
    void testGetModel() {
        String testModel = "gpt-4-turbo";
        OpenAIProvider provider = new OpenAIProvider("test-key", testModel, 30);
        
        assertEquals(testModel, provider.getModel(),
                    "Model should match the one provided in constructor");
    }

    @Test
    void testGetTimeoutSeconds() {
        int testTimeout = 45;
        OpenAIProvider provider = new OpenAIProvider("test-key", "gpt-3.5-turbo", testTimeout);
        
        assertEquals(testTimeout, provider.getTimeoutSeconds(),
                    "Timeout should match the one provided in constructor");
    }

    @Test
    void testIsConfiguredWithValidCredentials() {
        OpenAIProvider provider = new OpenAIProvider("sk-1234567890123456789012345678901234567890123456789", "gpt-3.5-turbo", 30);
        
        assertTrue(provider.isConfigured(),
                  "Provider should be configured with valid API key and model");
    }

    @Test
    void testIsConfiguredWithInvalidCredentials() {
        OpenAIProvider provider1 = new OpenAIProvider("invalid-key", "gpt-3.5-turbo", 30);
        assertFalse(provider1.isConfigured(),
                   "Provider should not be configured with invalid API key");
        
        OpenAIProvider provider2 = new OpenAIProvider("sk-1234567890123456789012345678901234567890123456789", "invalid-model", 30);
        assertFalse(provider2.isConfigured(),
                   "Provider should not be configured with invalid model");
        
        OpenAIProvider provider3 = new OpenAIProvider("invalid-key", "invalid-model", 30);
        assertFalse(provider3.isConfigured(),
                   "Provider should not be configured with invalid API key and model");
    }

    @Test
    void testGenerateResponseWithInvalidConfiguration() throws ExecutionException, InterruptedException {
        OpenAIProvider provider = new OpenAIProvider("invalid-key", "gpt-3.5-turbo", 30);
        
        CompletableFuture<AIResponse> future = provider.generateResponse(
            "Hello", testVillagerData, null, null
        );

        AIResponse response = future.get();
        
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isSuccess(), "Response should not be successful with invalid configuration");
        assertNotNull(response.getErrorMessage(), "Error message should be provided");
        assertTrue(response.getErrorMessage().contains("not properly configured"),
                  "Error message should indicate configuration issue");
    }

    @Test
    void testGenerateResponseWithValidConfiguration() {
        // Note: This test won't make actual API calls since we don't have a real API key
        // It will test the configuration validation and early return path
        OpenAIProvider provider = new OpenAIProvider("sk-1234567890123456789012345678901234567890123456789", "gpt-3.5-turbo", 30);
        
        // This should not throw an exception and should return a future
        CompletableFuture<AIResponse> future = provider.generateResponse(
            "Hello", testVillagerData, null, null
        );
        
        assertNotNull(future, "Future should not be null");
        assertFalse(future.isDone() || future.isCompletedExceptionally(),
                   "Future should be processing (will fail due to invalid API key, but that's expected)");
    }

    @Test
    void testConstructorParameters() {
        String apiKey = "sk-1234567890123456789012345678901234567890123456789";
        String model = "gpt-4";
        int timeout = 60;
        
        OpenAIProvider provider = new OpenAIProvider(apiKey, model, timeout);
        
        assertEquals(model, provider.getModel(), "Model should be set correctly");
        assertEquals(timeout, provider.getTimeoutSeconds(), "Timeout should be set correctly");
        assertEquals("OpenAI", provider.getProviderName(), "Provider name should be OpenAI");
    }

    @Test
    void testEdgeCaseApiKeys() {
        OpenAIProvider provider = new OpenAIProvider("test", "gpt-3.5-turbo", 30);
        
        // Test various edge cases for API key validation
        assertFalse(provider.isValidApiKey("sk-"), "API key with just prefix should be invalid");
        assertFalse(provider.isValidApiKey("sk-123"), "Short API key should be invalid");
        assertFalse(provider.isValidApiKey("pk-1234567890123456789012345678901234567890123456789"), "Wrong prefix should be invalid");
        assertFalse(provider.isValidApiKey("SK-1234567890123456789012345678901234567890123456789"), "Wrong case prefix should be invalid");
    }

    @Test
    void testEdgeCaseModels() {
        OpenAIProvider provider = new OpenAIProvider("test", "test", 30);
        
        // Test various edge cases for model validation
        assertFalse(provider.isValidModel("gpt"), "Incomplete model name should be invalid");
        assertFalse(provider.isValidModel("gpt-"), "Model with just prefix should be invalid");
        assertTrue(provider.isValidModel("gpt-3.5-turbo-16k"), "Extended model names should be valid");
        assertTrue(provider.isValidModel("gpt-4-32k"), "Extended GPT-4 models should be valid");
        assertFalse(provider.isValidModel("text-davinci-003"), "Legacy models should be invalid");
    }
}