package com.foogly.voiceofthevillage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VillagerData class.
 */
class VillagerDataTest {
    private VillagerData villagerData;
    private UUID villagerId;
    private UUID playerId;
    private Gson gson;

    @BeforeEach
    void setUp() {
        villagerId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        villagerData = new VillagerData(villagerId, "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    @Test
    void testConstructor() {
        assertNotNull(villagerData);
        assertEquals(villagerId, villagerData.getVillagerId());
        assertEquals("TestVillager", villagerData.getOriginalName());
        assertEquals(Gender.MALE, villagerData.getGender());
        assertEquals(PersonalityType.FRIENDLY, villagerData.getPersonality());
        assertTrue(villagerData.getMemories().isEmpty());
        assertTrue(villagerData.getPlayerReputations().isEmpty());
        assertEquals(0, villagerData.getTotalInteractions());
    }

    @Test
    void testEffectiveName() {
        // Should return original name when no custom name is set
        assertEquals("TestVillager", villagerData.getEffectiveName());
        
        // Should return custom name when set
        villagerData.setCustomName("CustomName");
        assertEquals("CustomName", villagerData.getEffectiveName());
        
        // Should return original name when custom name is empty
        villagerData.setCustomName("");
        assertEquals("TestVillager", villagerData.getEffectiveName());
        
        villagerData.setCustomName("   ");
        assertEquals("TestVillager", villagerData.getEffectiveName());
    }

    @Test
    void testAddMemory() {
        long initialTime = villagerData.getLastInteractionTime();
        
        InteractionMemory memory = new InteractionMemory(
            playerId, "TestPlayer", "Hello", "Hi there!", InteractionType.TEXT, 1L
        );
        
        villagerData.addMemory(memory);
        
        assertEquals(1, villagerData.getMemories().size());
        assertEquals(1, villagerData.getTotalInteractions());
        assertTrue(villagerData.getLastInteractionTime() >= initialTime);
    }

    @Test
    void testGetRecentMemories() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        // Add memories for different players
        villagerData.addMemory(new InteractionMemory(player1, "Player1", "Hello", "Hi!", InteractionType.TEXT, 1L));
        villagerData.addMemory(new InteractionMemory(player2, "Player2", "Hey", "Hello!", InteractionType.TEXT, 1L));
        villagerData.addMemory(new InteractionMemory(player1, "Player1", "How are you?", "Good!", InteractionType.TEXT, 2L));
        
        // Test getting memories for specific player
        var player1Memories = villagerData.getRecentMemories(player1, 10);
        assertEquals(2, player1Memories.size());
        
        var player2Memories = villagerData.getRecentMemories(player2, 10);
        assertEquals(1, player2Memories.size());
        
        // Test getting all recent memories
        var allMemories = villagerData.getRecentMemories(2);
        assertEquals(2, allMemories.size());
    }

    @Test
    void testCleanupExpiredMemories() {
        // Add memories with different game days
        villagerData.addMemory(new InteractionMemory(playerId, "Player", "Old message", "Old response", InteractionType.TEXT, 1L));
        villagerData.addMemory(new InteractionMemory(playerId, "Player", "Recent message", "Recent response", InteractionType.TEXT, 25L));
        
        // Cleanup memories older than 20 days (current day = 30)
        int removed = villagerData.cleanupExpiredMemories(30L, 20);
        
        assertEquals(1, removed);
        assertEquals(1, villagerData.getMemories().size());
    }

    @Test
    void testReputationManagement() {
        // Test getting or creating reputation
        ReputationData reputation = villagerData.getOrCreateReputation(playerId, "TestPlayer");
        assertNotNull(reputation);
        assertEquals(playerId, reputation.getPlayerId());
        assertEquals("TestPlayer", reputation.getPlayerName());
        assertEquals(0, reputation.getScore());
        
        // Test getting existing reputation
        ReputationData sameReputation = villagerData.getOrCreateReputation(playerId, "TestPlayer");
        assertSame(reputation, sameReputation);
        
        // Test adding reputation event
        ReputationEvent event = new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 5, "Nice chat");
        villagerData.addReputationEvent(playerId, "TestPlayer", event);
        
        assertEquals(5, reputation.getScore());
        assertEquals(1, reputation.getEvents().size());
    }

    @Test
    void testSerialization() {
        // Add some data to test serialization
        villagerData.setCustomName("SerializedVillager");
        villagerData.addMemory(new InteractionMemory(playerId, "Player", "Test", "Response", InteractionType.TEXT, 1L));
        villagerData.addReputationEvent(playerId, "Player", 
            new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 5, "Test event"));
        
        // Serialize to JSON
        String json = gson.toJson(villagerData);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Deserialize from JSON
        VillagerData deserialized = gson.fromJson(json, VillagerData.class);
        assertNotNull(deserialized);
        assertEquals(villagerData.getVillagerId(), deserialized.getVillagerId());
        assertEquals(villagerData.getOriginalName(), deserialized.getOriginalName());
        assertEquals(villagerData.getCustomName(), deserialized.getCustomName());
        assertEquals(villagerData.getGender(), deserialized.getGender());
        assertEquals(villagerData.getPersonality(), deserialized.getPersonality());
        assertEquals(villagerData.getMemories().size(), deserialized.getMemories().size());
        assertEquals(villagerData.getPlayerReputations().size(), deserialized.getPlayerReputations().size());
    }

    @Test
    void testEqualsAndHashCode() {
        VillagerData other = new VillagerData(villagerId, "OtherName", Gender.FEMALE, PersonalityType.GRUMPY);
        
        // Should be equal if villager IDs are the same
        assertEquals(villagerData, other);
        assertEquals(villagerData.hashCode(), other.hashCode());
        
        // Should not be equal if villager IDs are different
        VillagerData different = new VillagerData(UUID.randomUUID(), "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
        assertNotEquals(villagerData, different);
    }
}