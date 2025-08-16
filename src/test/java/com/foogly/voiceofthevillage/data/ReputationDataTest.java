package com.foogly.voiceofthevillage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReputationData class.
 */
class ReputationDataTest {
    private ReputationData reputationData;
    private UUID playerId;
    private Gson gson;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        reputationData = new ReputationData(playerId, "TestPlayer");
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    @Test
    void testConstructor() {
        assertNotNull(reputationData);
        assertEquals(playerId, reputationData.getPlayerId());
        assertEquals("TestPlayer", reputationData.getPlayerName());
        assertEquals(0, reputationData.getScore());
        assertTrue(reputationData.getEvents().isEmpty());
        assertFalse(reputationData.hasAttackedPlayer());
        assertFalse(reputationData.hasSpawnedIronGolem());
        assertTrue(reputationData.getLastUpdate() > 0);
    }

    @Test
    void testDefaultConstructor() {
        ReputationData defaultData = new ReputationData();
        assertNotNull(defaultData);
        assertNull(defaultData.getPlayerId());
        assertNull(defaultData.getPlayerName());
        assertEquals(0, defaultData.getScore());
        assertTrue(defaultData.getEvents().isEmpty());
        assertFalse(defaultData.hasAttackedPlayer());
        assertFalse(defaultData.hasSpawnedIronGolem());
    }

    @Test
    void testAddEvent() {
        ReputationEvent event = new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 5, "Nice chat");
        long initialUpdate = reputationData.getLastUpdate();
        
        reputationData.addEvent(event);
        
        assertEquals(5, reputationData.getScore());
        assertEquals(1, reputationData.getEvents().size());
        assertTrue(reputationData.getLastUpdate() >= initialUpdate);
    }

    @Test
    void testScoreClamping() {
        // Test upper bound
        ReputationEvent highEvent = new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 150, "Huge positive");
        reputationData.addEvent(highEvent);
        assertEquals(100, reputationData.getScore()); // Should be clamped to 100
        
        // Test lower bound
        ReputationEvent lowEvent = new ReputationEvent(ReputationEventType.NEGATIVE_CONVERSATION, -250, "Huge negative");
        reputationData.addEvent(lowEvent);
        assertEquals(-100, reputationData.getScore()); // Should be clamped to -100
    }

    @Test
    void testReputationThresholds() {
        // Test neutral threshold
        assertEquals(ReputationThreshold.NEUTRAL, reputationData.getThreshold());
        
        // Test friendly threshold
        reputationData.setScore(50);
        assertEquals(ReputationThreshold.FRIENDLY, reputationData.getThreshold());
        
        // Test beloved threshold
        reputationData.setScore(85);
        assertEquals(ReputationThreshold.BELOVED, reputationData.getThreshold());
        
        // Test unfriendly threshold
        reputationData.setScore(-50);
        assertEquals(ReputationThreshold.UNFRIENDLY, reputationData.getThreshold());
        
        // Test hostile threshold
        reputationData.setScore(-85);
        assertEquals(ReputationThreshold.HOSTILE, reputationData.getThreshold());
    }

    @Test
    void testShouldAttackPlayer() {
        // Should not attack at neutral reputation
        assertFalse(reputationData.shouldAttackPlayer());
        
        // Should attack at unfriendly reputation if hasn't attacked yet
        reputationData.setScore(-50);
        assertTrue(reputationData.shouldAttackPlayer());
        
        // Should not attack if already attacked
        reputationData.setHasAttackedPlayer(true);
        assertFalse(reputationData.shouldAttackPlayer());
        
        // Should not attack at hostile level (iron golem instead)
        reputationData.setScore(-85);
        reputationData.setHasAttackedPlayer(false);
        assertFalse(reputationData.shouldAttackPlayer());
    }

    @Test
    void testShouldSpawnIronGolem() {
        // Should not spawn at neutral reputation
        assertFalse(reputationData.shouldSpawnIronGolem());
        
        // Should not spawn at unfriendly reputation
        reputationData.setScore(-50);
        assertFalse(reputationData.shouldSpawnIronGolem());
        
        // Should spawn at hostile reputation if hasn't spawned yet
        reputationData.setScore(-85);
        assertTrue(reputationData.shouldSpawnIronGolem());
        
        // Should not spawn if already spawned
        reputationData.setHasSpawnedIronGolem(true);
        assertFalse(reputationData.shouldSpawnIronGolem());
    }

    @Test
    void testSerialization() {
        // Add some data to test serialization
        ReputationEvent event = new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 10, "Test event");
        reputationData.addEvent(event);
        reputationData.setHasAttackedPlayer(true);
        
        // Serialize to JSON
        String json = gson.toJson(reputationData);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("TestPlayer"));
        
        // Deserialize from JSON
        ReputationData deserialized = gson.fromJson(json, ReputationData.class);
        assertNotNull(deserialized);
        assertEquals(reputationData.getPlayerId(), deserialized.getPlayerId());
        assertEquals(reputationData.getPlayerName(), deserialized.getPlayerName());
        assertEquals(reputationData.getScore(), deserialized.getScore());
        assertEquals(reputationData.getEvents().size(), deserialized.getEvents().size());
        assertEquals(reputationData.hasAttackedPlayer(), deserialized.hasAttackedPlayer());
        assertEquals(reputationData.hasSpawnedIronGolem(), deserialized.hasSpawnedIronGolem());
    }

    @Test
    void testSetters() {
        UUID newPlayerId = UUID.randomUUID();
        reputationData.setPlayerId(newPlayerId);
        reputationData.setPlayerName("NewPlayer");
        reputationData.setScore(25);
        reputationData.setLastUpdate(12345L);
        reputationData.setHasAttackedPlayer(true);
        reputationData.setHasSpawnedIronGolem(true);
        
        assertEquals(newPlayerId, reputationData.getPlayerId());
        assertEquals("NewPlayer", reputationData.getPlayerName());
        assertEquals(25, reputationData.getScore());
        assertEquals(12345L, reputationData.getLastUpdate());
        assertTrue(reputationData.hasAttackedPlayer());
        assertTrue(reputationData.hasSpawnedIronGolem());
    }

    @Test
    void testEqualsAndHashCode() {
        ReputationData other = new ReputationData(playerId, "TestPlayer");
        
        assertEquals(reputationData, other);
        assertEquals(reputationData.hashCode(), other.hashCode());
        
        // Different player ID should make them not equal
        other.setPlayerId(UUID.randomUUID());
        assertNotEquals(reputationData, other);
        
        // Reset and test different score
        other.setPlayerId(playerId);
        other.setScore(50);
        assertNotEquals(reputationData, other);
    }

    @Test
    void testToString() {
        String toString = reputationData.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ReputationData"));
        assertTrue(toString.contains("TestPlayer"));
        assertTrue(toString.contains("score=0"));
        assertTrue(toString.contains("threshold=NEUTRAL"));
    }
}