package com.foogly.voiceofthevillage.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests for AudioCaptureManager functionality.
 * Note: These tests may not work in headless environments without audio hardware.
 */
class AudioCaptureManagerTest {
    
    private AudioCaptureManager audioCaptureManager;
    
    @BeforeEach
    void setUp() {
        audioCaptureManager = new AudioCaptureManager();
    }
    
    @Test
    void testIsAudioCaptureAvailable() {
        // This test checks if the system can detect audio capture capability
        // Result may vary depending on the test environment
        boolean available = audioCaptureManager.isAudioCaptureAvailable();
        
        // Just ensure the method doesn't throw an exception
        assertNotNull(available);
    }
    
    @Test
    void testStartRecordingWhenAlreadyRecording() {
        // Skip this test if audio capture is not available
        if (!audioCaptureManager.isAudioCaptureAvailable()) {
            return;
        }
        
        // Start first recording
        CompletableFuture<AudioCaptureManager.AudioRecording> future1 = 
            audioCaptureManager.startRecording(AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Try to start second recording while first is active
        CompletableFuture<AudioCaptureManager.AudioRecording> future2 = 
            audioCaptureManager.startRecording(AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Second recording should fail
        assertTrue(future2.isCompletedExceptionally());
        
        // Clean up first recording
        audioCaptureManager.stopRecording();
    }
    
    @Test
    void testRecordingStatus() {
        // Initially not recording
        assertFalse(audioCaptureManager.isRecording());
        
        // Skip if audio capture not available
        if (!audioCaptureManager.isAudioCaptureAvailable()) {
            return;
        }
        
        // Start recording
        CompletableFuture<AudioCaptureManager.AudioRecording> future = 
            audioCaptureManager.startRecording(AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        // Should be recording now
        assertTrue(audioCaptureManager.isRecording());
        
        // Stop recording
        audioCaptureManager.stopRecording();
        
        // Wait a bit for the recording to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Should not be recording anymore
        assertFalse(audioCaptureManager.isRecording());
    }
    
    @Test
    void testAudioRecordingProperties() {
        // Create a mock audio recording
        byte[] testData = new byte[1000];
        AudioFormat format = AudioFormat.DEFAULT_RECORDING_FORMAT;
        double duration = 0.5;
        
        AudioCaptureManager.AudioRecording recording = 
            new AudioCaptureManager.AudioRecording(testData, format, duration);
        
        assertEquals(1000, recording.getDataSize());
        assertEquals(format, recording.getFormat());
        assertEquals(0.5, recording.getDuration(), 0.001);
        assertTrue(recording.hasAudioData()); // Duration > 0.1 seconds
        
        // Test audio data is cloned
        byte[] retrievedData = recording.getAudioData();
        assertArrayEquals(testData, retrievedData);
        assertNotSame(testData, retrievedData); // Should be a copy
    }
    
    @Test
    void testAudioRecordingWithNoData() {
        byte[] emptyData = new byte[0];
        AudioFormat format = AudioFormat.DEFAULT_RECORDING_FORMAT;
        double duration = 0.0;
        
        AudioCaptureManager.AudioRecording recording = 
            new AudioCaptureManager.AudioRecording(emptyData, format, duration);
        
        assertEquals(0, recording.getDataSize());
        assertFalse(recording.hasAudioData()); // No meaningful data
    }
    
    @Test
    void testAudioRecordingWithShortDuration() {
        byte[] testData = new byte[100];
        AudioFormat format = AudioFormat.DEFAULT_RECORDING_FORMAT;
        double duration = 0.05; // 50ms - too short
        
        AudioCaptureManager.AudioRecording recording = 
            new AudioCaptureManager.AudioRecording(testData, format, duration);
        
        assertEquals(100, recording.getDataSize());
        assertFalse(recording.hasAudioData()); // Duration < 0.1 seconds
    }
    
    @Test
    void testStopRecordingWhenNotRecording() {
        // Should not throw exception when stopping non-existent recording
        assertDoesNotThrow(() -> audioCaptureManager.stopRecording());
    }
}