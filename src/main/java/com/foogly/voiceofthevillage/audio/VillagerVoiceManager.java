package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.VillagerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages villager voice synthesis and playback.
 * Coordinates text-to-speech processing and audio playback for villager responses.
 */
public class VillagerVoiceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerVoiceManager.class);
    
    private static final VillagerVoiceManager INSTANCE = new VillagerVoiceManager();
    
    private final TextToSpeechProcessor ttsProcessor;
    private final AudioPlaybackManager playbackManager;
    private final ConcurrentHashMap<UUID, VoiceProfile> villagerVoices;
    
    private VillagerVoiceManager() {
        this.ttsProcessor = new TextToSpeechProcessor();
        this.playbackManager = AudioPlaybackManager.getInstance();
        this.villagerVoices = new ConcurrentHashMap<>();
    }
    
    public static VillagerVoiceManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Makes a villager speak the given text.
     * Handles TTS processing and audio playback.
     */
    public CompletableFuture<Void> speakText(UUID villagerId, String text, VillagerData villagerData) {
        if (!VoiceConfig.ENABLE_VOICE_OUTPUT.get()) {
            LOGGER.debug("Voice output disabled, skipping TTS for villager {}", villagerId);
            return CompletableFuture.completedFuture(null);
        }
        
        if (text == null || text.trim().isEmpty()) {
            LOGGER.debug("No text to speak for villager {}", villagerId);
            return CompletableFuture.completedFuture(null);
        }
        
        if (!playbackManager.isPlaybackAvailable()) {
            LOGGER.warn("Audio playback not available, cannot speak text for villager {}", villagerId);
            return CompletableFuture.completedFuture(null);
        }
        
        // Get or create voice profile for this villager
        VoiceProfile voiceProfile = getVoiceProfileForVillager(villagerId, villagerData);
        
        // Generate session ID for this speech
        String sessionId = "villager_" + villagerId.toString();
        
        LOGGER.debug("Processing TTS for villager {}: '{}'", villagerId, text);
        
        return ttsProcessor.processText(text, voiceProfile)
            .thenCompose(ttsResult -> {
                if (ttsResult.isSuccess()) {
                    LOGGER.debug("TTS successful for villager {}, playing audio ({} bytes)", 
                               villagerId, ttsResult.getAudioDataSize());
                    
                    return playbackManager.playAudio(sessionId, ttsResult.getAudioData(), ttsResult.getAudioFormat());
                } else {
                    LOGGER.warn("TTS failed for villager {}: {}", villagerId, ttsResult.getErrorMessage());
                    return CompletableFuture.completedFuture(null);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.error("Error during villager speech for {}", villagerId, throwable);
                return null;
            });
    }
    
    /**
     * Stops any currently playing speech for the specified villager.
     */
    public void stopSpeaking(UUID villagerId) {
        String sessionId = "villager_" + villagerId.toString();
        playbackManager.stopPlayback(sessionId);
        LOGGER.debug("Stopped speech for villager {}", villagerId);
    }
    
    /**
     * Stops all villager speech.
     */
    public void stopAllSpeech() {
        playbackManager.stopAllPlayback();
        LOGGER.debug("Stopped all villager speech");
    }
    
    /**
     * Gets or creates a voice profile for the specified villager.
     */
    public VoiceProfile getVoiceProfileForVillager(UUID villagerId, VillagerData villagerData) {
        return villagerVoices.computeIfAbsent(villagerId, id -> {
            VoiceProfile profile = VoiceProfile.createForVillager(
                villagerData.getGender(), 
                villagerData.getPersonality()
            );
            
            LOGGER.debug("Created voice profile for villager {}: {}", villagerId, profile);
            return profile;
        });
    }
    
    /**
     * Updates the voice profile for a villager (e.g., when renamed or personality changes).
     */
    public void updateVoiceProfile(UUID villagerId, VillagerData villagerData) {
        VoiceProfile newProfile = VoiceProfile.createForVillager(
            villagerData.getGender(), 
            villagerData.getPersonality()
        );
        
        villagerVoices.put(villagerId, newProfile);
        LOGGER.debug("Updated voice profile for villager {}: {}", villagerId, newProfile);
    }
    
    /**
     * Removes the voice profile for a villager (e.g., when villager is removed).
     */
    public void removeVoiceProfile(UUID villagerId) {
        villagerVoices.remove(villagerId);
        stopSpeaking(villagerId);
        LOGGER.debug("Removed voice profile for villager {}", villagerId);
    }
    
    /**
     * Gets the number of cached voice profiles.
     */
    public int getCachedVoiceCount() {
        return villagerVoices.size();
    }
    
    /**
     * Clears all cached voice profiles.
     */
    public void clearVoiceCache() {
        villagerVoices.clear();
        stopAllSpeech();
        LOGGER.debug("Cleared all voice profiles");
    }
    
    /**
     * Checks if voice synthesis is available and configured.
     */
    public boolean isVoiceSynthesisAvailable() {
        return VoiceConfig.ENABLE_VOICE_OUTPUT.get() && 
               VoiceConfig.isAIConfigured() && 
               playbackManager.isPlaybackAvailable();
    }
    
    /**
     * Gets the current status of the voice system.
     */
    public VoiceSystemStatus getSystemStatus() {
        return new VoiceSystemStatus(
            VoiceConfig.ENABLE_VOICE_OUTPUT.get(),
            VoiceConfig.isAIConfigured(),
            playbackManager.isPlaybackAvailable(),
            playbackManager.getActiveSessionCount(),
            villagerVoices.size()
        );
    }
    
    /**
     * Represents the current status of the voice system.
     */
    public static class VoiceSystemStatus {
        private final boolean voiceOutputEnabled;
        private final boolean aiConfigured;
        private final boolean playbackAvailable;
        private final int activePlaybackSessions;
        private final int cachedVoiceProfiles;
        
        public VoiceSystemStatus(boolean voiceOutputEnabled, boolean aiConfigured, 
                               boolean playbackAvailable, int activePlaybackSessions, 
                               int cachedVoiceProfiles) {
            this.voiceOutputEnabled = voiceOutputEnabled;
            this.aiConfigured = aiConfigured;
            this.playbackAvailable = playbackAvailable;
            this.activePlaybackSessions = activePlaybackSessions;
            this.cachedVoiceProfiles = cachedVoiceProfiles;
        }
        
        public boolean isVoiceOutputEnabled() {
            return voiceOutputEnabled;
        }
        
        public boolean isAiConfigured() {
            return aiConfigured;
        }
        
        public boolean isPlaybackAvailable() {
            return playbackAvailable;
        }
        
        public int getActivePlaybackSessions() {
            return activePlaybackSessions;
        }
        
        public int getCachedVoiceProfiles() {
            return cachedVoiceProfiles;
        }
        
        public boolean isFullyFunctional() {
            return voiceOutputEnabled && aiConfigured && playbackAvailable;
        }
        
        @Override
        public String toString() {
            return String.format("VoiceSystemStatus{enabled=%s, aiConfigured=%s, playbackAvailable=%s, " +
                               "activeSessions=%d, cachedProfiles=%d}", 
                               voiceOutputEnabled, aiConfigured, playbackAvailable, 
                               activePlaybackSessions, cachedVoiceProfiles);
        }
    }
}