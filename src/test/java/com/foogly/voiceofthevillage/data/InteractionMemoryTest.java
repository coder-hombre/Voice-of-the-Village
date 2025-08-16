package com.foogly.voiceofthevillage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InteractionMemory class.
 */
class InteractionMemoryTest {
    private InteractionMemory memory;
    private UUID playerId;
    private Gson gson;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        memory = new InteractionMemory(playerId, "TestPlayer", "Hello villager", 
                                     "Hello there, traveler!", InteractionType.VOICE, 5L);
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    @Test
    void testConstructor() {
        assertNotNull(memory);
        assertEquals(playerId, memory.getPlayerId());
        assertEquals("TestPlayer", memory.getPlayerName());
        assertEquals("Hello villager", memory.getPlayerMessage());
        assertEquals("Hello there, traveler!", memory.getVillagerResponse());
        assertEquals(InteractionType.VOICE, memory.getInteractionType());
        assertEquals(5L, memory.getGameDay());
        assertTrue(memory.getTimestamp() > 0);
    }

    @Test
    void testDefaultConstructor() {
        InteractionMemory defaultMemory = new InteractionMemory();
        assertNotNull(defaultMemory);
        assertNull(defaultMemory.getPlayerId());
        assertNull(defaultMemory.getPlayerName());
        assertNull(defaultMemory.getPlayerMessage());
        assertNull(defaultMemory.getVillagerResponse());
        assertNull(defaultMemory.getInteractionType());
        assertEquals(0L, defaultMemory.getGameDay());
        assertEquals(0L, defaultMemory.getTimestamp());
    }

    @Test
    void testIsExpired() {
        // Memory from day 5, current day 35, retention 20 days
        // Should be expired because 35 - 5 = 30 > 20
        assertTrue(memory.isExpired(35L, 20));
        
        // Memory from day 5, current day 20, retention 20 days
        // Should not be expired because 20 - 5 = 15 <= 20
        assertFalse(memory.isExpired(20L, 20));
        
        // Memory from day 5, current day 25, retention 20 days
        // Should not be expired because 25 - 5 = 20 <= 20
        assertFalse(memory.isExpired(25L, 20));
        
        // Memory from day 5, current day 26, retention 20 days
        // Should be expired because 26 - 5 = 21 > 20
        assertTrue(memory.isExpired(26L, 20));
    }

    @Test
    void testSerialization() {
        // Serialize to JSON
        String json = gson.toJson(memory);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("TestPlayer"));
        assertTrue(json.contains("Hello villager"));
        assertTrue(json.contains("VOICE"));
        
        // Deserialize from JSON
        InteractionMemory deserialized = gson.fromJson(json, InteractionMemory.class);
        assertNotNull(deserialized);
        assertEquals(memory.getPlayerId(), deserialized.getPlayerId());
        assertEquals(memory.getPlayerName(), deserialized.getPlayerName());
        assertEquals(memory.getPlayerMessage(), deserialized.getPlayerMessage());
        assertEquals(memory.getVillagerResponse(), deserialized.getVillagerResponse());
        assertEquals(memory.getInteractionType(), deserialized.getInteractionType());
        assertEquals(memory.getGameDay(), deserialized.getGameDay());
        assertEquals(memory.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    void testSetters() {
        UUID newPlayerId = UUID.randomUUID();
        memory.setPlayerId(newPlayerId);
        memory.setPlayerName("NewPlayer");
        memory.setPlayerMessage("New message");
        memory.setVillagerResponse("New response");
        memory.setInteractionType(InteractionType.TEXT);
        memory.setGameDay(10L);
        memory.setTimestamp(12345L);
        
        assertEquals(newPlayerId, memory.getPlayerId());
        assertEquals("NewPlayer", memory.getPlayerName());
        assertEquals("New message", memory.getPlayerMessage());
        assertEquals("New response", memory.getVillagerResponse());
        assertEquals(InteractionType.TEXT, memory.getInteractionType());
        assertEquals(10L, memory.getGameDay());
        assertEquals(12345L, memory.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        InteractionMemory other = new InteractionMemory(playerId, "TestPlayer", "Hello villager",
                                                       "Hello there, traveler!", InteractionType.VOICE, 5L);
        other.setTimestamp(memory.getTimestamp()); // Make timestamps match
        
        assertEquals(memory, other);
        assertEquals(memory.hashCode(), other.hashCode());
        
        // Different player ID should make them not equal
        other.setPlayerId(UUID.randomUUID());
        assertNotEquals(memory, other);
        
        // Reset and test different message
        other.setPlayerId(playerId);
        other.setPlayerMessage("Different message");
        assertNotEquals(memory, other);
    }

    @Test
    void testToString() {
        String toString = memory.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("InteractionMemory"));
        assertTrue(toString.contains("TestPlayer"));
        assertTrue(toString.contains("VOICE"));
        assertTrue(toString.contains("gameDay=5"));
    }
}