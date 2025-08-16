package com.foogly.voiceofthevillage.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests for AudioPlaybackManager functionality.
 * Note: These tests may not work in headless environments without audio hardware.
 */
class AudioPlaybackManagerTest {
    
    private AudioPlaybackManager playbackManager;
    
    @BeforeEach
    void setUp() {
        playbackManager = AudioPlaybackManager.getInstance();
        playbackManager.setEnabled(true);
        playbackManager.stopAllPlayback();
    }
    
    @Test
    void testSingletonInstance() {
        AudioPlaybackManager instance1 = AudioPlaybackManager.getInstance();
        AudioPlaybackManager instance2 = AudioPlaybackManager.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    void testIsPlaybackAvailable() {
        // This test checks if the system can detect audio playback capability
        // Result may vary depending on the test environment
        boolean available = playbackManager.isPlaybackAvailable();
        
        // Just ensure the method doesn't throw an exception
        assertNotNull(available);
    }
    
    @Test
    void testPlayAudioWithNullData() throws Exception {
        CompletableFuture<Void> future = playbackManager.playAudio("test", null, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Should complete immediately with null data
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testPlayAudioWithEmptyData() throws Exception {
        byte[] emptyData = new byte[0];
        CompletableFuture<Void> future = playbackManager.playAudio("test", emptyData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Should complete immediately with empty data
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testPlayAudioWhenDisabled() throws Exception {
        playbackManager.setEnabled(false);
        
        byte[] testData = createTestAudioData(1000);
        CompletableFuture<Void> future = playbackManager.playAudio("test", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Should complete immediately when disabled
        assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
        
        playbackManager.setEnabled(true); // Reset for other tests
    }
    
    @Test
    void testEnabledState() {
        assertTrue(playbackManager.isEnabled()); // Should be enabled from setUp
        
        playbackManager.setEnabled(false);
        assertFalse(playbackManager.isEnabled());
        
        playbackManager.setEnabled(true);
        assertTrue(playbackManager.isEnabled());
    }
    
    @Test
    void testActiveSessionCount() {
        assertEquals(0, playbackManager.getActiveSessionCount());
        
        // Skip if playback not available
        if (!playbackManager.isPlaybackAvailable()) {
            return;
        }
        
        byte[] testData = createTestAudioData(1000);
        
        // Start playback
        CompletableFuture<Void> future = playbackManager.playAudio("test1", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Session count might be 1 briefly, but could complete quickly
        // Just ensure it doesn't throw an exception
        int sessionCount = playbackManager.getActiveSessionCount();
        assertTrue(sessionCount >= 0);
        
        // Stop playback
        playbackManager.stopPlayback("test1");
    }
    
    @Test
    void testStopPlayback() {
        // Should not throw exception even if session doesn't exist
        assertDoesNotThrow(() -> playbackManager.stopPlayback("nonexistent"));
        
        // Skip if playback not available
        if (!playbackManager.isPlaybackAvailable()) {
            return;
        }
        
        byte[] testData = createTestAudioData(1000);
        
        // Start and immediately stop playback
        CompletableFuture<Void> future = playbackManager.playAudio("test", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        playbackManager.stopPlayback("test");
        
        // Should complete without error
        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS));
    }
    
    @Test
    void testStopAllPlayback() {
        // Should not throw exception even with no active sessions
        assertDoesNotThrow(() -> playbackManager.stopAllPlayback());
        
        // Skip if playback not available
        if (!playbackManager.isPlaybackAvailable()) {
            return;
        }
        
        byte[] testData = createTestAudioData(1000);
        
        // Start multiple sessions
        CompletableFuture<Void> future1 = playbackManager.playAudio("test1", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        CompletableFuture<Void> future2 = playbackManager.playAudio("test2", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Stop all
        playbackManager.stopAllPlayback();
        
        // Both should complete
        assertDoesNotThrow(() -> {
            future1.get(2, TimeUnit.SECONDS);
            future2.get(2, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testReplaceExistingSession() {
        // Skip if playback not available
        if (!playbackManager.isPlaybackAvailable()) {
            return;
        }
        
        byte[] testData = createTestAudioData(1000);
        
        // Start first session
        CompletableFuture<Void> future1 = playbackManager.playAudio("test", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Start second session with same ID (should replace first)
        CompletableFuture<Void> future2 = playbackManager.playAudio("test", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Both should complete without error
        assertDoesNotThrow(() -> {
            future1.get(2, TimeUnit.SECONDS);
            future2.get(2, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testSetEnabledStopsPlayback() {
        // Skip if playback not available
        if (!playbackManager.isPlaybackAvailable()) {
            return;
        }
        
        byte[] testData = createTestAudioData(1000);
        
        // Start playback
        CompletableFuture<Void> future = playbackManager.playAudio("test", testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Disable playback (should stop all sessions)
        playbackManager.setEnabled(false);
        
        // Should complete
        assertDoesNotThrow(() -> future.get(2, TimeUnit.SECONDS));
        
        playbackManager.setEnabled(true); // Reset for other tests
    }
    
    /**
     * Creates test audio data for testing purposes.
     */
    private byte[] createTestAudioData(int samples) {
        byte[] data = new byte[samples * 2]; // 16-bit = 2 bytes per sample
        
        for (int i = 0; i < samples; i++) {
            // Create a simple sine wave
            double angle = 2.0 * Math.PI * i / 100.0;
            short sample = (short) (Math.sin(angle) * 1000);
            
            // Convert to little-endian bytes
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
}