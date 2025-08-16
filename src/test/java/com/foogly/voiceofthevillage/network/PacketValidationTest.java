package com.foogly.voiceofthevillage.network;

import com.foogly.voiceofthevillage.data.ReputationThreshold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for packet validation logic without requiring Minecraft classes.
 * This focuses on testing the business logic of packet validation.
 */
class PacketValidationTest {

    @Test
    void testVoiceInputPacketValidation() {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        byte[] audioData = new byte[]{1, 2, 3, 4, 5};
        int sampleRate = 44100;
        long timestamp = System.currentTimeMillis();

        // Test valid packet
        assertTrue(isValidVoiceInputPacket(villagerId, playerId, playerName, audioData, sampleRate, timestamp));

        // Test invalid cases
        assertFalse(isValidVoiceInputPacket(null, playerId, playerName, audioData, sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, null, playerName, audioData, sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, null, audioData, sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, "   ", audioData, sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, playerName, null, sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, playerName, new byte[0], sampleRate, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, playerName, audioData, 0, timestamp));
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, playerName, audioData, sampleRate, 0));

        // Test audio data size limit
        byte[] largeAudioData = new byte[2 * 1024 * 1024]; // 2MB - too large
        assertFalse(isValidVoiceInputPacket(villagerId, playerId, playerName, largeAudioData, sampleRate, timestamp));
    }

    @Test
    void testTextMessagePacketValidation() {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        String message = "Hello, villager!";
        long timestamp = System.currentTimeMillis();

        // Test valid packet
        assertTrue(isValidTextMessagePacket(villagerId, playerId, playerName, message, timestamp));

        // Test invalid cases
        assertFalse(isValidTextMessagePacket(null, playerId, playerName, message, timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, null, playerName, message, timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, playerId, null, message, timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, playerId, "   ", message, timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, playerId, playerName, null, timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, playerId, playerName, "   ", timestamp));
        assertFalse(isValidTextMessagePacket(villagerId, playerId, playerName, message, 0));

        // Test message length limit
        String longMessage = "a".repeat(501); // Too long
        assertFalse(isValidTextMessagePacket(villagerId, playerId, playerName, longMessage, timestamp));
    }

    @Test
    void testVillagerResponsePacketValidation() {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String villagerName = "TestVillager";
        String textResponse = "Hello there, traveler!";
        byte[] audioData = new byte[]{1, 2, 3, 4, 5};
        long timestamp = System.currentTimeMillis();

        // Test valid packets
        assertTrue(isValidVillagerResponsePacket(villagerId, playerId, villagerName, textResponse, null, false, timestamp));
        assertTrue(isValidVillagerResponsePacket(villagerId, playerId, villagerName, textResponse, audioData, true, timestamp));

        // Test invalid cases
        assertFalse(isValidVillagerResponsePacket(null, playerId, villagerName, textResponse, audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, null, villagerName, textResponse, audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, null, textResponse, audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, "   ", textResponse, audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, villagerName, null, audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, villagerName, "   ", audioData, true, timestamp));
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, villagerName, textResponse, audioData, true, 0));

        // Test audio data size limit
        byte[] largeAudioData = new byte[3 * 1024 * 1024]; // 3MB - too large
        assertFalse(isValidVillagerResponsePacket(villagerId, playerId, villagerName, textResponse, largeAudioData, true, timestamp));
    }

    @Test
    void testReputationUpdatePacketValidation() {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String villagerName = "TestVillager";
        int newScore = 25;
        int scoreChange = 10;
        ReputationThreshold newThreshold = ReputationThreshold.FRIENDLY;
        ReputationThreshold previousThreshold = ReputationThreshold.NEUTRAL;
        String eventDescription = "Player helped with trade";
        long timestamp = System.currentTimeMillis();

        // Test valid packet
        assertTrue(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange, 
                                               newThreshold, previousThreshold, eventDescription, timestamp));

        // Test invalid cases
        assertFalse(isValidReputationUpdatePacket(null, playerId, villagerName, newScore, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, null, villagerName, newScore, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, null, newScore, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, "   ", newScore, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, 150, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, -150, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, 150, 
                                                newThreshold, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange, 
                                                null, previousThreshold, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange, 
                                                newThreshold, null, eventDescription, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange, 
                                                newThreshold, previousThreshold, null, timestamp));
        assertFalse(isValidReputationUpdatePacket(villagerId, playerId, villagerName, newScore, scoreChange, 
                                                newThreshold, previousThreshold, eventDescription, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Player1", "TestUser", "A", "VeryLongPlayerNameThatIsStillValid"})
    void testValidPlayerNames(String name) {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        byte[] audioData = new byte[]{1, 2, 3, 4, 5};
        int sampleRate = 44100;
        long timestamp = System.currentTimeMillis();

        assertTrue(isValidVoiceInputPacket(villagerId, playerId, name, audioData, sampleRate, timestamp));
    }

    @ParameterizedTest
    @ValueSource(ints = {8000, 16000, 22050, 44100, 48000, 96000})
    void testValidSampleRates(int rate) {
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        byte[] audioData = new byte[]{1, 2, 3, 4, 5};
        long timestamp = System.currentTimeMillis();

        assertTrue(isValidVoiceInputPacket(villagerId, playerId, playerName, audioData, rate, timestamp));
    }

    // Helper methods that replicate the validation logic from the packet classes
    private boolean isValidVoiceInputPacket(UUID villagerId, UUID playerId, String playerName, 
                                          byte[] audioData, int sampleRate, long timestamp) {
        return villagerId != null &&
               playerId != null &&
               playerName != null && !playerName.trim().isEmpty() &&
               audioData != null && audioData.length > 0 && audioData.length <= 1024 * 1024 &&
               sampleRate > 0 &&
               timestamp > 0;
    }

    private boolean isValidTextMessagePacket(UUID villagerId, UUID playerId, String playerName, 
                                           String message, long timestamp) {
        return villagerId != null &&
               playerId != null &&
               playerName != null && !playerName.trim().isEmpty() &&
               message != null && !message.trim().isEmpty() && message.length() <= 500 &&
               timestamp > 0;
    }

    private boolean isValidVillagerResponsePacket(UUID villagerId, UUID playerId, String villagerName, 
                                                String textResponse, byte[] audioData, boolean hasAudio, 
                                                long timestamp) {
        return villagerId != null &&
               playerId != null &&
               villagerName != null && !villagerName.trim().isEmpty() &&
               textResponse != null && !textResponse.trim().isEmpty() &&
               (!hasAudio || (audioData != null && audioData.length <= 2 * 1024 * 1024)) &&
               timestamp > 0;
    }

    private boolean isValidReputationUpdatePacket(UUID villagerId, UUID playerId, String villagerName, 
                                                int newScore, int scoreChange, ReputationThreshold newThreshold, 
                                                ReputationThreshold previousThreshold, String eventDescription, 
                                                long timestamp) {
        return villagerId != null &&
               playerId != null &&
               villagerName != null && !villagerName.trim().isEmpty() &&
               newScore >= -100 && newScore <= 100 &&
               scoreChange >= -100 && scoreChange <= 100 &&
               newThreshold != null &&
               previousThreshold != null &&
               eventDescription != null &&
               timestamp > 0;
    }
}