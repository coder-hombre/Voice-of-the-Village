package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;
import com.foogly.voiceofthevillage.data.VillagerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests for VillagerVoiceManager functionality.
 */
class VillagerVoiceManagerTest {
    
    private VillagerVoiceManager voiceManager;
    private UUID testVillagerId;
    private VillagerData testVillagerData;
    
    @BeforeEach
    void setUp() {
        voiceManager = VillagerVoiceManager.getInstance();
        testVillagerId = UUID.randomUUID();
        testVillagerData = new VillagerData();
        testVillagerData.setCustomName("TestVillager");
        testVillagerData.setGender(Gender.MALE);
        testVillagerData.setPersonality(PersonalityType.FRIENDLY);
        
        // Clear any existing voice profiles
        voiceManager.clearVoiceCache();
    }
    
    @Test
    void testSingletonInstance() {
        VillagerVoiceManager instance1 = VillagerVoiceManager.getInstance();
        VillagerVoiceManager instance2 = VillagerVoiceManager.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    void testSpeakTextWithNullText() throws Exception {
        CompletableFuture<Void> future = voiceManager.speakText(testVillagerId, null, testVillagerData);
        
        // Should complete immediately with null text
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testSpeakTextWithEmptyText() throws Exception {
        CompletableFuture<Void> future = voiceManager.speakText(testVillagerId, "", testVillagerData);
        
        // Should complete immediately with empty text
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testSpeakTextWithWhitespaceOnly() throws Exception {
        CompletableFuture<Void> future = voiceManager.speakText(testVillagerId, "   \n\t   ", testVillagerData);
        
        // Should complete immediately with whitespace-only text
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testGetVoiceProfileForVillager() {
        VoiceProfile profile = voiceManager.getVoiceProfileForVillager(testVillagerId, testVillagerData);
        
        assertNotNull(profile);
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals(PersonalityType.FRIENDLY, profile.getPersonality());
        
        // Should cache the profile
        assertEquals(1, voiceManager.getCachedVoiceCount());
        
        // Getting the same profile again should return the cached one
        VoiceProfile cachedProfile = voiceManager.getVoiceProfileForVillager(testVillagerId, testVillagerData);
        assertSame(profile, cachedProfile);
    }
    
    @Test
    void testUpdateVoiceProfile() {
        // Get initial profile
        VoiceProfile initialProfile = voiceManager.getVoiceProfileForVillager(testVillagerId, testVillagerData);
        
        // Update villager data
        testVillagerData.setGender(Gender.FEMALE);
        testVillagerData.setPersonality(PersonalityType.CHEERFUL);
        
        // Update voice profile
        voiceManager.updateVoiceProfile(testVillagerId, testVillagerData);
        
        // Get updated profile
        VoiceProfile updatedProfile = voiceManager.getVoiceProfileForVillager(testVillagerId, testVillagerData);
        
        assertNotSame(initialProfile, updatedProfile);
        assertEquals(Gender.FEMALE, updatedProfile.getGender());
        assertEquals(PersonalityType.CHEERFUL, updatedProfile.getPersonality());
    }
    
    @Test
    void testRemoveVoiceProfile() {
        // Create a voice profile
        voiceManager.getVoiceProfileForVillager(testVillagerId, testVillagerData);
        assertEquals(1, voiceManager.getCachedVoiceCount());
        
        // Remove the profile
        voiceManager.removeVoiceProfile(testVillagerId);
        assertEquals(0, voiceManager.getCachedVoiceCount());
    }
    
    @Test
    void testClearVoiceCache() {
        // Create multiple voice profiles
        UUID villager1 = UUID.randomUUID();
        UUID villager2 = UUID.randomUUID();
        
        voiceManager.getVoiceProfileForVillager(villager1, testVillagerData);
        voiceManager.getVoiceProfileForVillager(villager2, testVillagerData);
        
        assertEquals(2, voiceManager.getCachedVoiceCount());
        
        // Clear cache
        voiceManager.clearVoiceCache();
        assertEquals(0, voiceManager.getCachedVoiceCount());
    }
    
    @Test
    void testStopSpeaking() {
        // Should not throw exception even if villager is not speaking
        assertDoesNotThrow(() -> voiceManager.stopSpeaking(testVillagerId));
    }
    
    @Test
    void testStopAllSpeech() {
        // Should not throw exception even with no active speech
        assertDoesNotThrow(() -> voiceManager.stopAllSpeech());
    }
    
    @Test
    void testIsVoiceSynthesisAvailable() {
        // This will depend on configuration and system capabilities
        boolean available = voiceManager.isVoiceSynthesisAvailable();
        
        // Just ensure it doesn't throw an exception
        assertNotNull(available);
    }
    
    @Test
    void testGetSystemStatus() {
        VillagerVoiceManager.VoiceSystemStatus status = voiceManager.getSystemStatus();
        
        assertNotNull(status);
        assertNotNull(status.toString());
        
        // Test individual status components
        assertNotNull(status.isVoiceOutputEnabled());
        assertNotNull(status.isAiConfigured());
        assertNotNull(status.isPlaybackAvailable());
        assertTrue(status.getActivePlaybackSessions() >= 0);
        assertTrue(status.getCachedVoiceProfiles() >= 0);
        
        // Test isFullyFunctional
        boolean fullyFunctional = status.isFullyFunctional();
        assertEquals(status.isVoiceOutputEnabled() && status.isAiConfigured() && status.isPlaybackAvailable(), 
                    fullyFunctional);
    }
    
    @Test
    void testVoiceSystemStatusToString() {
        VillagerVoiceManager.VoiceSystemStatus status = voiceManager.getSystemStatus();
        String toString = status.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("VoiceSystemStatus"));
        assertTrue(toString.contains("enabled="));
        assertTrue(toString.contains("aiConfigured="));
        assertTrue(toString.contains("playbackAvailable="));
        assertTrue(toString.contains("activeSessions="));
        assertTrue(toString.contains("cachedProfiles="));
    }
    
    @Test
    void testMultipleVillagerProfiles() {
        UUID villager1 = UUID.randomUUID();
        UUID villager2 = UUID.randomUUID();
        
        VillagerData data1 = new VillagerData();
        data1.setGender(Gender.MALE);
        data1.setPersonality(PersonalityType.GRUMPY);
        
        VillagerData data2 = new VillagerData();
        data2.setGender(Gender.FEMALE);
        data2.setPersonality(PersonalityType.CHEERFUL);
        
        VoiceProfile profile1 = voiceManager.getVoiceProfileForVillager(villager1, data1);
        VoiceProfile profile2 = voiceManager.getVoiceProfileForVillager(villager2, data2);
        
        assertNotEquals(profile1, profile2);
        assertEquals(Gender.MALE, profile1.getGender());
        assertEquals(Gender.FEMALE, profile2.getGender());
        assertEquals(PersonalityType.GRUMPY, profile1.getPersonality());
        assertEquals(PersonalityType.CHEERFUL, profile2.getPersonality());
        
        assertEquals(2, voiceManager.getCachedVoiceCount());
    }
}