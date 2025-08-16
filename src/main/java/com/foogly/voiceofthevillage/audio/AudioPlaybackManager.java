package com.foogly.voiceofthevillage.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages audio playback for text-to-speech generated audio.
 * Handles multiple simultaneous audio streams and volume control.
 */
public class AudioPlaybackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioPlaybackManager.class);
    
    private static final AudioPlaybackManager INSTANCE = new AudioPlaybackManager();
    
    // Track active playback sessions
    private final ConcurrentHashMap<String, PlaybackSession> activeSessions = new ConcurrentHashMap<>();
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    
    private AudioPlaybackManager() {
        // Private constructor for singleton
    }
    
    public static AudioPlaybackManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Plays audio data with the specified format.
     * Returns a CompletableFuture that completes when playback finishes.
     */
    public CompletableFuture<Void> playAudio(String sessionId, byte[] audioData, AudioFormat format) {
        if (!isEnabled.get()) {
            LOGGER.debug("Audio playback is disabled");
            return CompletableFuture.completedFuture(null);
        }
        
        if (audioData == null || audioData.length == 0) {
            LOGGER.warn("No audio data provided for playback");
            return CompletableFuture.completedFuture(null);
        }
        
        // Stop any existing session with the same ID
        stopPlayback(sessionId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performPlayback(sessionId, audioData, format);
            } catch (Exception e) {
                LOGGER.error("Audio playback failed for session {}", sessionId, e);
                throw new RuntimeException("Audio playback failed", e);
            }
        });
    }
    
    /**
     * Stops playback for the specified session.
     */
    public void stopPlayback(String sessionId) {
        PlaybackSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.stop();
            LOGGER.debug("Stopped playback session: {}", sessionId);
        }
    }
    
    /**
     * Stops all active playback sessions.
     */
    public void stopAllPlayback() {
        LOGGER.debug("Stopping all active playback sessions");
        activeSessions.values().forEach(PlaybackSession::stop);
        activeSessions.clear();
    }
    
    /**
     * Checks if audio playback is available on this system.
     */
    public boolean isPlaybackAvailable() {
        try {
            javax.sound.sampled.AudioFormat testFormat = new javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                16000, 16, 1, 2, 16000, false
            );
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, testFormat);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            LOGGER.warn("Audio playback not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Enables or disables audio playback.
     */
    public void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
        if (!enabled) {
            stopAllPlayback();
        }
        LOGGER.debug("Audio playback {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Checks if audio playback is enabled.
     */
    public boolean isEnabled() {
        return isEnabled.get();
    }
    
    /**
     * Gets the number of active playback sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Performs the actual audio playback.
     */
    private Void performPlayback(String sessionId, byte[] audioData, AudioFormat format) 
            throws LineUnavailableException, IOException {
        
        // Convert our AudioFormat to Java's AudioFormat
        javax.sound.sampled.AudioFormat javaFormat = createJavaAudioFormat(format);
        
        // Create and open audio line
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, javaFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio format not supported: " + format);
        }
        
        SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
        PlaybackSession session = new PlaybackSession(audioLine);
        activeSessions.put(sessionId, session);
        
        try {
            audioLine.open(javaFormat);
            audioLine.start();
            
            LOGGER.debug("Started playback session {} with {} bytes of audio", sessionId, audioData.length);
            
            // Play audio data in chunks
            int bufferSize = 4096;
            int offset = 0;
            
            while (offset < audioData.length && !session.isStopped()) {
                int chunkSize = Math.min(bufferSize, audioData.length - offset);
                int bytesWritten = audioLine.write(audioData, offset, chunkSize);
                offset += bytesWritten;
                
                // Small delay to prevent excessive CPU usage
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // Wait for playback to complete
            if (!session.isStopped()) {
                audioLine.drain();
            }
            
            LOGGER.debug("Completed playback session: {}", sessionId);
            
        } finally {
            // Clean up resources
            audioLine.stop();
            audioLine.close();
            activeSessions.remove(sessionId);
        }
        
        return null;
    }
    
    /**
     * Converts our AudioFormat to Java's AudioFormat.
     */
    private javax.sound.sampled.AudioFormat createJavaAudioFormat(AudioFormat format) {
        javax.sound.sampled.AudioFormat.Encoding encoding;
        
        switch (format.getEncoding()) {
            case PCM_SIGNED:
                encoding = javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
                break;
            case PCM_UNSIGNED:
                encoding = javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;
                break;
            case PCM_FLOAT:
                encoding = javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
                break;
            default:
                encoding = javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
        }
        
        return new javax.sound.sampled.AudioFormat(
            encoding,
            format.getSampleRate(),
            format.getBitDepth(),
            format.getChannels(),
            format.getFrameSize(),
            format.getSampleRate(),
            false // little endian
        );
    }
    
    /**
     * Represents an active playback session.
     */
    private static class PlaybackSession {
        private final SourceDataLine audioLine;
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        
        public PlaybackSession(SourceDataLine audioLine) {
            this.audioLine = audioLine;
        }
        
        public void stop() {
            stopped.set(true);
            if (audioLine != null && audioLine.isOpen()) {
                audioLine.stop();
            }
        }
        
        public boolean isStopped() {
            return stopped.get();
        }
    }
}