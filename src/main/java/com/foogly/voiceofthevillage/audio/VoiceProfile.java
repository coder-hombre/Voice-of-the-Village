package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;

/**
 * Represents a voice profile for text-to-speech synthesis.
 * Defines voice characteristics based on villager gender and personality.
 */
public class VoiceProfile {
    
    // Default voice profiles for different genders
    public static final VoiceProfile DEFAULT_MALE = new VoiceProfile(
        Gender.MALE, "en-US-Neural2-D", 1.0f, 0.0f, PersonalityType.FRIENDLY
    );
    
    public static final VoiceProfile DEFAULT_FEMALE = new VoiceProfile(
        Gender.FEMALE, "en-US-Neural2-F", 1.0f, 0.0f, PersonalityType.FRIENDLY
    );
    
    private final Gender gender;
    private final String voiceId;
    private final float speed;
    private final float pitch;
    private final PersonalityType personality;
    
    public VoiceProfile(Gender gender, String voiceId, float speed, float pitch, PersonalityType personality) {
        this.gender = gender;
        this.voiceId = voiceId;
        this.speed = Math.max(0.25f, Math.min(4.0f, speed)); // Clamp between 0.25x and 4.0x
        this.pitch = Math.max(-20.0f, Math.min(20.0f, pitch)); // Clamp between -20 and +20 semitones
        this.personality = personality;
    }
    
    /**
     * Creates a voice profile based on gender and personality.
     */
    public static VoiceProfile createForVillager(Gender gender, PersonalityType personality) {
        String voiceId;
        float speed = 1.0f;
        float pitch = 0.0f;
        
        // Select voice based on gender
        switch (gender) {
            case MALE:
                voiceId = selectMaleVoice(personality);
                break;
            case FEMALE:
                voiceId = selectFemaleVoice(personality);
                break;
            default:
                voiceId = "en-US-Neural2-C"; // Neutral voice
        }
        
        // Adjust speech characteristics based on personality
        switch (personality) {
            case CHEERFUL:
                speed = 1.1f;
                pitch = 2.0f;
                break;
            case GRUMPY:
                speed = 0.9f;
                pitch = -2.0f;
                break;
            case WISE:
                speed = 0.8f;
                pitch = -1.0f;
                break;
            case CURIOUS:
                speed = 1.2f;
                pitch = 3.0f;
                break;
            case CAUTIOUS:
                speed = 0.85f;
                pitch = -0.5f;
                break;
            case MERCHANT:
                speed = 1.05f;
                pitch = 1.0f;
                break;
            case GOSSIP:
                speed = 1.15f;
                pitch = 1.5f;
                break;
            case FRIENDLY:
            default:
                speed = 1.0f;
                pitch = 0.0f;
                break;
        }
        
        return new VoiceProfile(gender, voiceId, speed, pitch, personality);
    }
    
    /**
     * Selects an appropriate male voice based on personality.
     */
    private static String selectMaleVoice(PersonalityType personality) {
        switch (personality) {
            case WISE:
                return "en-US-Neural2-A"; // Deeper, more authoritative
            case GRUMPY:
                return "en-US-Neural2-D"; // Rougher tone
            case CURIOUS:
                return "en-US-Neural2-I"; // More dynamic
            case MERCHANT:
                return "en-US-Neural2-A"; // Authoritative for business
            case GOSSIP:
                return "en-US-Neural2-I"; // More expressive
            case CHEERFUL:
            case FRIENDLY:
            case CAUTIOUS:
            default:
                return "en-US-Neural2-D"; // Standard male voice
        }
    }
    
    /**
     * Selects an appropriate female voice based on personality.
     */
    private static String selectFemaleVoice(PersonalityType personality) {
        switch (personality) {
            case WISE:
                return "en-US-Neural2-G"; // More mature tone
            case CHEERFUL:
                return "en-US-Neural2-H"; // Brighter tone
            case CURIOUS:
                return "en-US-Neural2-F"; // More dynamic
            case GRUMPY:
                return "en-US-Neural2-E"; // Slightly deeper
            case MERCHANT:
                return "en-US-Neural2-G"; // More mature for business
            case GOSSIP:
                return "en-US-Neural2-H"; // Brighter, more expressive
            case CAUTIOUS:
            case FRIENDLY:
            default:
                return "en-US-Neural2-F"; // Standard female voice
        }
    }
    
    public Gender getGender() {
        return gender;
    }
    
    public String getVoiceId() {
        return voiceId;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public PersonalityType getPersonality() {
        return personality;
    }
    
    /**
     * Creates a modified version of this voice profile with different speed.
     */
    public VoiceProfile withSpeed(float newSpeed) {
        return new VoiceProfile(gender, voiceId, newSpeed, pitch, personality);
    }
    
    /**
     * Creates a modified version of this voice profile with different pitch.
     */
    public VoiceProfile withPitch(float newPitch) {
        return new VoiceProfile(gender, voiceId, speed, newPitch, personality);
    }
    
    @Override
    public String toString() {
        return String.format("VoiceProfile{gender=%s, voiceId='%s', speed=%.2f, pitch=%.2f, personality=%s}",
                           gender, voiceId, speed, pitch, personality);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        VoiceProfile that = (VoiceProfile) obj;
        return Float.compare(that.speed, speed) == 0 &&
               Float.compare(that.pitch, pitch) == 0 &&
               gender == that.gender &&
               voiceId.equals(that.voiceId) &&
               personality == that.personality;
    }
    
    @Override
    public int hashCode() {
        int result = gender.hashCode();
        result = 31 * result + voiceId.hashCode();
        result = 31 * result + Float.hashCode(speed);
        result = 31 * result + Float.hashCode(pitch);
        result = 31 * result + personality.hashCode();
        return result;
    }
}