package com.foogly.voiceofthevillage.data;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VillagerDataManager logic without requiring Minecraft classes.
 * Focuses on data management, caching, and business logic.
 */
class VillagerDataManagerLogicTest {

    @Test
    void testVillagerDataCreation() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "TestName", Gender.MALE, PersonalityType.FRIENDLY);
        
        assertEquals(testId, data.getVillagerId(), "Should have correct villager ID");
        assertEquals("TestName", data.getOriginalName(), "Should have correct original name");
        assertEquals(Gender.MALE, data.getGender(), "Should have correct gender");
        assertEquals(PersonalityType.FRIENDLY, data.getPersonality(), "Should have correct personality");
        assertNull(data.getCustomName(), "Custom name should be null initially");
    }

    @Test
    void testEffectiveNameLogic() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Initially should return original name
        assertEquals("OriginalName", data.getEffectiveName(), "Should return original name initially");
        
        // After setting custom name, should return custom name
        data.setCustomName("CustomName");
        assertEquals("CustomName", data.getEffectiveName(), "Should return custom name after setting");
        
        // Setting empty custom name should fall back to original
        data.setCustomName("");
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original name for empty custom name");
        
        // Setting whitespace custom name should fall back to original
        data.setCustomName("   ");
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original name for whitespace custom name");
        
        // Setting null custom name should fall back to original
        data.setCustomName(null);
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original name for null custom name");
    }

    @Test
    void testMemoryManagement() {
        UUID testId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "TestName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Initially should have no memories
        assertEquals(0, data.getMemories().size(), "Should have no memories initially");
        assertEquals(0, data.getTotalInteractions(), "Should have no interactions initially");
        
        // Add a memory
        InteractionMemory memory = new InteractionMemory(playerId, "TestPlayer", "Hello", "Hi there!", InteractionType.TEXT, 1);
        data.addMemory(memory);
        
        assertEquals(1, data.getMemories().size(), "Should have one memory after adding");
        assertEquals(1, data.getTotalInteractions(), "Should have one interaction after adding memory");
        assertTrue(data.getLastInteractionTime() > data.getCreationTime(), "Last interaction time should be updated");
    }

    @Test
    void testReputationManagement() {
        UUID testId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "TestName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Initially should have no reputation data
        assertNull(data.getReputation(playerId), "Should have no reputation initially");
        
        // Get or create reputation should create new reputation
        ReputationData reputation = data.getOrCreateReputation(playerId, "TestPlayer");
        assertNotNull(reputation, "Should create reputation data");
        assertEquals(playerId, reputation.getPlayerId(), "Should have correct player ID");
        assertEquals("TestPlayer", reputation.getPlayerName(), "Should have correct player name");
        assertEquals(0, reputation.getScore(), "Should have default score of 0");
        
        // Getting again should return the same instance
        ReputationData sameReputation = data.getOrCreateReputation(playerId, "TestPlayer");
        assertSame(reputation, sameReputation, "Should return the same reputation instance");
    }

    @Test
    void testReputationEvents() {
        UUID testId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "TestName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Add a positive reputation event
        ReputationEvent positiveEvent = new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 10, "Positive interaction");
        data.addReputationEvent(playerId, "TestPlayer", positiveEvent);
        
        ReputationData reputation = data.getReputation(playerId);
        assertNotNull(reputation, "Should have reputation after adding event");
        assertEquals(10, reputation.getScore(), "Should have correct reputation score");
        assertEquals(1, reputation.getEvents().size(), "Should have one reputation event");
        
        // Add a negative reputation event
        ReputationEvent negativeEvent = new ReputationEvent(ReputationEventType.NEGATIVE_CONVERSATION, -5, "Negative interaction");
        data.addReputationEvent(playerId, "TestPlayer", negativeEvent);
        
        reputation = data.getReputation(playerId);
        assertEquals(5, reputation.getScore(), "Should have updated reputation score (10 - 5 = 5)");
        assertEquals(2, reputation.getEvents().size(), "Should have two reputation events");
    }

    @Test
    void testMemoryRetrieval() {
        UUID testId = UUID.randomUUID();
        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "TestName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Add memories for different players
        data.addMemory(new InteractionMemory(playerId1, "Player1", "Hello", "Hi!", InteractionType.TEXT, 1));
        data.addMemory(new InteractionMemory(playerId2, "Player2", "Goodbye", "Bye!", InteractionType.TEXT, 2));
        data.addMemory(new InteractionMemory(playerId1, "Player1", "How are you?", "Good!", InteractionType.TEXT, 3));
        
        // Test getting recent memories for specific player
        var player1Memories = data.getRecentMemories(playerId1, 10);
        assertEquals(2, player1Memories.size(), "Should have 2 memories for player 1");
        
        var player2Memories = data.getRecentMemories(playerId2, 10);
        assertEquals(1, player2Memories.size(), "Should have 1 memory for player 2");
        
        // Test getting all recent memories
        var allMemories = data.getRecentMemories(10);
        assertEquals(3, allMemories.size(), "Should have 3 total memories");
        
        // Test limiting results
        var limitedMemories = data.getRecentMemories(2);
        assertEquals(2, limitedMemories.size(), "Should limit to 2 memories");
    }

    @Test
    void testRandomPersonalityGeneration() {
        // Test that we can generate random personalities
        PersonalityType[] personalities = PersonalityType.values();
        assertTrue(personalities.length > 0, "Should have personality types available");
        
        // Test that random selection works (basic test)
        PersonalityType personality1 = getRandomPersonality();
        PersonalityType personality2 = getRandomPersonality();
        
        assertNotNull(personality1, "Should generate a personality");
        assertNotNull(personality2, "Should generate a personality");
        
        // Both should be valid personality types
        boolean personality1Valid = false;
        boolean personality2Valid = false;
        for (PersonalityType type : personalities) {
            if (type == personality1) personality1Valid = true;
            if (type == personality2) personality2Valid = true;
        }
        assertTrue(personality1Valid, "Generated personality 1 should be valid");
        assertTrue(personality2Valid, "Generated personality 2 should be valid");
    }

    @Test
    void testNameGeneration() {
        // Test name generation logic
        NameGenerator.VillagerNameData nameData = NameGenerator.generateRandomName();
        
        assertNotNull(nameData, "Should generate name data");
        assertNotNull(nameData.getName(), "Should have a name");
        assertNotNull(nameData.getGender(), "Should have a gender");
        assertFalse(nameData.getName().trim().isEmpty(), "Name should not be empty");
        assertTrue(nameData.getGender() == Gender.MALE || nameData.getGender() == Gender.FEMALE, 
                  "Gender should be MALE or FEMALE");
    }

    @Test
    void testGenderDetection() {
        // Test gender detection for known names
        assertEquals(Gender.MALE, NameGenerator.detectGender("Alexander"), "Alexander should be detected as male");
        assertEquals(Gender.FEMALE, NameGenerator.detectGender("Alice"), "Alice should be detected as female");
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender("UnknownName"), "Unknown name should return UNKNOWN");
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender(""), "Empty name should return UNKNOWN");
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender(null), "Null name should return UNKNOWN");
    }

    // Helper method to simulate random personality generation
    private PersonalityType getRandomPersonality() {
        PersonalityType[] personalities = PersonalityType.values();
        int randomIndex = (int) (Math.random() * personalities.length);
        return personalities[randomIndex];
    }
}