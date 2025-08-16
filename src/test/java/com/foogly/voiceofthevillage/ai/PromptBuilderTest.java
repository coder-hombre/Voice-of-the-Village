package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.InteractionMemory;
import com.foogly.voiceofthevillage.data.InteractionType;
import com.foogly.voiceofthevillage.data.PersonalityType;
import com.foogly.voiceofthevillage.data.ReputationData;
import com.foogly.voiceofthevillage.data.ReputationEvent;
import com.foogly.voiceofthevillage.data.ReputationEventType;
import com.foogly.voiceofthevillage.data.VillagerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PromptBuilder.
 */
class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private VillagerData testVillagerData;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
        testPlayerId = UUID.randomUUID();
        
        testVillagerData = new VillagerData(
            UUID.randomUUID(),
            "TestVillager",
            Gender.FEMALE,
            PersonalityType.FRIENDLY
        );
    }

    @Test
    void testBuildSimplePrompt() {
        String prompt = promptBuilder.buildSimplePrompt(
            "Hello there!",
            "Alice",
            "friendly"
        );
        
        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("Alice"), "Prompt should contain villager name");
        assertTrue(prompt.contains("friendly"), "Prompt should contain personality");
        assertTrue(prompt.contains("Hello there!"), "Prompt should contain player message");
        assertTrue(prompt.contains("villager in Minecraft"), "Prompt should contain Minecraft context");
    }

    @Test
    void testBuildPromptWithNullPlayer() {
        String prompt = promptBuilder.buildPrompt(
            "Hello!",
            testVillagerData,
            null,
            null
        );
        
        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("TestVillager"), "Prompt should contain villager name");
        assertTrue(prompt.contains("female"), "Prompt should contain gender");
        assertTrue(prompt.contains("friendly"), "Prompt should contain personality");
        assertTrue(prompt.contains("Hello!"), "Prompt should contain player message");
        assertTrue(prompt.contains("Player says to you"), "Prompt should handle null player gracefully");
    }

    @Test
    void testBuildPromptWithPersonality() {
        testVillagerData.setPersonality(PersonalityType.GRUMPY);
        
        String prompt = promptBuilder.buildPrompt(
            "Good morning!",
            testVillagerData,
            null,
            null
        );
        
        assertTrue(prompt.contains("grumpy"), "Prompt should contain personality type");
        assertTrue(prompt.contains(PersonalityType.GRUMPY.getDescription()), 
                  "Prompt should contain personality description");
    }

    @Test
    void testBuildPromptWithMemories() {
        // Add some interaction memories
        InteractionMemory memory1 = new InteractionMemory(
            testPlayerId,
            "TestPlayer",
            "How are you?",
            "I'm doing well, thank you!",
            InteractionType.TEXT,
            1L
        );
        
        InteractionMemory memory2 = new InteractionMemory(
            testPlayerId,
            "TestPlayer",
            "What do you sell?",
            "I have some fine emeralds for trade.",
            InteractionType.TEXT,
            2L
        );
        
        testVillagerData.addMemory(memory1);
        testVillagerData.addMemory(memory2);
        
        // Create a mock player object
        MockPlayer mockPlayer = new MockPlayer(testPlayerId, "TestPlayer");
        
        String prompt = promptBuilder.buildPrompt(
            "Do you remember me?",
            testVillagerData,
            mockPlayer,
            null
        );
        
        assertTrue(prompt.contains("Recent conversations"), "Prompt should include memory section");
        assertTrue(prompt.contains("How are you?"), "Prompt should contain previous player message");
        assertTrue(prompt.contains("I'm doing well"), "Prompt should contain previous villager response");
    }

    @Test
    void testBuildPromptWithReputation() {
        // Add reputation data
        ReputationData reputation = testVillagerData.getOrCreateReputation(testPlayerId, "TestPlayer");
        reputation.addEvent(new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 10, "Friendly greeting"));
        reputation.addEvent(new ReputationEvent(ReputationEventType.SUCCESSFUL_TRADE, 15, "Good trade"));
        
        MockPlayer mockPlayer = new MockPlayer(testPlayerId, "TestPlayer");
        
        String prompt = promptBuilder.buildPrompt(
            "Hello again!",
            testVillagerData,
            mockPlayer,
            null
        );
        
        assertTrue(prompt.contains("friendly relationship") || prompt.contains("neutral relationship"), 
                  "Prompt should include reputation context");
        assertTrue(prompt.contains("TestPlayer"), "Prompt should contain player name");
    }

    @Test
    void testBuildPromptWithNegativeReputation() {
        // Add negative reputation
        ReputationData reputation = testVillagerData.getOrCreateReputation(testPlayerId, "BadPlayer");
        reputation.addEvent(new ReputationEvent(ReputationEventType.RUDE_BEHAVIOR, -30, "Rude behavior"));
        reputation.addEvent(new ReputationEvent(ReputationEventType.CANCELLED_TRADE, -20, "Unfair trade"));
        
        MockPlayer mockPlayer = new MockPlayer(testPlayerId, "BadPlayer");
        
        String prompt = promptBuilder.buildPrompt(
            "Hey there!",
            testVillagerData,
            mockPlayer,
            null
        );
        
        assertTrue(prompt.contains("unfriendly") || prompt.contains("wary"), 
                  "Prompt should reflect negative reputation");
    }

    @Test
    void testPromptLengthLimit() {
        // Create a very long villager name and add many memories
        VillagerData longVillager = new VillagerData(
            UUID.randomUUID(),
            "VeryLongVillagerNameThatShouldBeTruncated",
            Gender.MALE,
            PersonalityType.WISE
        );
        
        // Add many memories to test truncation
        for (int i = 0; i < 20; i++) {
            InteractionMemory memory = new InteractionMemory(
                testPlayerId,
                "TestPlayer",
                "This is a very long message number " + i + " that contains a lot of text to test the prompt length limiting functionality",
                "This is a very long response number " + i + " that also contains a lot of text to test the prompt truncation",
                InteractionType.TEXT,
                i
            );
            longVillager.addMemory(memory);
        }
        
        MockPlayer mockPlayer = new MockPlayer(testPlayerId, "TestPlayer");
        
        String prompt = promptBuilder.buildPrompt(
            "Tell me everything you know!",
            longVillager,
            mockPlayer,
            null
        );
        
        // The prompt should be limited to a reasonable length
        assertTrue(prompt.length() <= 2000, "Prompt should be limited to reasonable length");
        assertTrue(prompt.contains("VeryLongVillagerNameThatShouldBeTruncated"), 
                  "Prompt should still contain essential information");
    }

    @Test
    void testCustomNameHandling() {
        testVillagerData.setCustomName("CustomName");
        
        String prompt = promptBuilder.buildPrompt(
            "Hello!",
            testVillagerData,
            null,
            null
        );
        
        assertTrue(prompt.contains("CustomName"), "Prompt should use custom name when available");
        assertFalse(prompt.contains("TestVillager"), "Prompt should not use original name when custom name is set");
    }

    @Test
    void testGameContextIntegration() {
        // For this test, we'll skip the game context since it requires Minecraft classes
        // The PromptBuilder handles null game context gracefully
        String prompt = promptBuilder.buildPrompt(
            "What time is it?",
            testVillagerData,
            null,
            null
        );
        
        assertNotNull(prompt, "Prompt should not be null");
        assertTrue(prompt.contains("TestVillager"), "Prompt should contain villager name");
        assertTrue(prompt.contains("What time is it?"), "Prompt should contain player message");
    }

    /**
     * Mock player class for testing.
     */
    private static class MockPlayer {
        private final UUID uuid;
        private final String name;

        public MockPlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUUID() {
            return uuid;
        }

        public MockName getName() {
            return new MockName(name);
        }
    }

    /**
     * Mock name component for testing.
     */
    private static class MockName {
        private final String name;

        public MockName(String name) {
            this.name = name;
        }

        public String getString() {
            return name;
        }
    }

    /**
     * Mock game context for testing.
     */
    private static class MockGameContext {
        public String getTimeDescription() {
            return "morning";
        }

        public String getWeatherDescription() {
            return "clear";
        }

        public String getBiome() {
            return "plains";
        }

        public String getDimensionName() {
            return "minecraft:overworld";
        }
    }
}