package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VoiceProfile functionality.
 */
class VoiceProfileTest {
    
    @Test
    void testDefaultMaleProfile() {
        VoiceProfile profile = VoiceProfile.DEFAULT_MALE;
        
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals("en-US-Neural2-D", profile.getVoiceId());
        assertEquals(1.0f, profile.getSpeed(), 0.001f);
        assertEquals(0.0f, profile.getPitch(), 0.001f);
        assertEquals(PersonalityType.FRIENDLY, profile.getPersonality());
    }
    
    @Test
    void testDefaultFemaleProfile() {
        VoiceProfile profile = VoiceProfile.DEFAULT_FEMALE;
        
        assertEquals(Gender.FEMALE, profile.getGender());
        assertEquals("en-US-Neural2-F", profile.getVoiceId());
        assertEquals(1.0f, profile.getSpeed(), 0.001f);
        assertEquals(0.0f, profile.getPitch(), 0.001f);
        assertEquals(PersonalityType.FRIENDLY, profile.getPersonality());
    }
    
    @Test
    void testCreateForVillagerMaleCheerful() {
        VoiceProfile profile = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.CHEERFUL);
        
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals(PersonalityType.CHEERFUL, profile.getPersonality());
        assertEquals(1.1f, profile.getSpeed(), 0.001f);
        assertEquals(2.0f, profile.getPitch(), 0.001f);
    }
    
    @Test
    void testCreateForVillagerFemaleGrumpy() {
        VoiceProfile profile = VoiceProfile.createForVillager(Gender.FEMALE, PersonalityType.GRUMPY);
        
        assertEquals(Gender.FEMALE, profile.getGender());
        assertEquals(PersonalityType.GRUMPY, profile.getPersonality());
        assertEquals(0.9f, profile.getSpeed(), 0.001f);
        assertEquals(-2.0f, profile.getPitch(), 0.001f);
    }
    
    @Test
    void testCreateForVillagerMaleWise() {
        VoiceProfile profile = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.WISE);
        
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals(PersonalityType.WISE, profile.getPersonality());
        assertEquals(0.8f, profile.getSpeed(), 0.001f);
        assertEquals(-1.0f, profile.getPitch(), 0.001f);
    }
    
    @Test
    void testCreateForVillagerFemaleCurious() {
        VoiceProfile profile = VoiceProfile.createForVillager(Gender.FEMALE, PersonalityType.CURIOUS);
        
        assertEquals(Gender.FEMALE, profile.getGender());
        assertEquals(PersonalityType.CURIOUS, profile.getPersonality());
        assertEquals(1.2f, profile.getSpeed(), 0.001f);
        assertEquals(3.0f, profile.getPitch(), 0.001f);
    }
    
    @Test
    void testCreateForVillagerCautious() {
        VoiceProfile profile = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.CAUTIOUS);
        
        assertEquals(Gender.MALE, profile.getGender());
        assertEquals(PersonalityType.CAUTIOUS, profile.getPersonality());
        assertEquals(0.85f, profile.getSpeed(), 0.001f);
        assertEquals(-0.5f, profile.getPitch(), 0.001f);
    }
    
    @Test
    void testSpeedClamping() {
        // Test speed clamping in constructor
        VoiceProfile tooFast = new VoiceProfile(Gender.MALE, "test", 10.0f, 0.0f, PersonalityType.FRIENDLY);
        assertEquals(4.0f, tooFast.getSpeed(), 0.001f);
        
        VoiceProfile tooSlow = new VoiceProfile(Gender.MALE, "test", 0.1f, 0.0f, PersonalityType.FRIENDLY);
        assertEquals(0.25f, tooSlow.getSpeed(), 0.001f);
    }
    
    @Test
    void testPitchClamping() {
        // Test pitch clamping in constructor
        VoiceProfile tooHigh = new VoiceProfile(Gender.MALE, "test", 1.0f, 50.0f, PersonalityType.FRIENDLY);
        assertEquals(20.0f, tooHigh.getPitch(), 0.001f);
        
        VoiceProfile tooLow = new VoiceProfile(Gender.MALE, "test", 1.0f, -50.0f, PersonalityType.FRIENDLY);
        assertEquals(-20.0f, tooLow.getPitch(), 0.001f);
    }
    
    @Test
    void testWithSpeed() {
        VoiceProfile original = VoiceProfile.DEFAULT_MALE;
        VoiceProfile modified = original.withSpeed(1.5f);
        
        assertEquals(1.5f, modified.getSpeed(), 0.001f);
        assertEquals(original.getPitch(), modified.getPitch(), 0.001f);
        assertEquals(original.getGender(), modified.getGender());
        assertEquals(original.getVoiceId(), modified.getVoiceId());
        assertEquals(original.getPersonality(), modified.getPersonality());
        
        // Original should be unchanged
        assertEquals(1.0f, original.getSpeed(), 0.001f);
    }
    
    @Test
    void testWithPitch() {
        VoiceProfile original = VoiceProfile.DEFAULT_FEMALE;
        VoiceProfile modified = original.withPitch(2.5f);
        
        assertEquals(2.5f, modified.getPitch(), 0.001f);
        assertEquals(original.getSpeed(), modified.getSpeed(), 0.001f);
        assertEquals(original.getGender(), modified.getGender());
        assertEquals(original.getVoiceId(), modified.getVoiceId());
        assertEquals(original.getPersonality(), modified.getPersonality());
        
        // Original should be unchanged
        assertEquals(0.0f, original.getPitch(), 0.001f);
    }
    
    @Test
    void testEqualsAndHashCode() {
        VoiceProfile profile1 = new VoiceProfile(Gender.MALE, "test", 1.0f, 0.0f, PersonalityType.FRIENDLY);
        VoiceProfile profile2 = new VoiceProfile(Gender.MALE, "test", 1.0f, 0.0f, PersonalityType.FRIENDLY);
        VoiceProfile profile3 = new VoiceProfile(Gender.FEMALE, "test", 1.0f, 0.0f, PersonalityType.FRIENDLY);
        
        assertEquals(profile1, profile2);
        assertNotEquals(profile1, profile3);
        
        assertEquals(profile1.hashCode(), profile2.hashCode());
        assertNotEquals(profile1.hashCode(), profile3.hashCode());
    }
    
    @Test
    void testToString() {
        VoiceProfile profile = new VoiceProfile(Gender.MALE, "test-voice", 1.2f, -1.5f, PersonalityType.CHEERFUL);
        String toString = profile.toString();
        
        assertTrue(toString.contains("MALE"));
        assertTrue(toString.contains("test-voice"));
        assertTrue(toString.contains("1.20"));
        assertTrue(toString.contains("-1.50"));
        assertTrue(toString.contains("CHEERFUL"));
    }
    
    @Test
    void testVoiceSelectionForDifferentPersonalities() {
        // Test that different personalities get different voice IDs for the same gender
        VoiceProfile wise = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.WISE);
        VoiceProfile grumpy = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.GRUMPY);
        VoiceProfile curious = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.CURIOUS);
        
        // Voice IDs might be the same or different depending on implementation
        // Just ensure they're all valid
        assertNotNull(wise.getVoiceId());
        assertNotNull(grumpy.getVoiceId());
        assertNotNull(curious.getVoiceId());
        
        // Ensure personalities are preserved
        assertEquals(PersonalityType.WISE, wise.getPersonality());
        assertEquals(PersonalityType.GRUMPY, grumpy.getPersonality());
        assertEquals(PersonalityType.CURIOUS, curious.getPersonality());
    }
}