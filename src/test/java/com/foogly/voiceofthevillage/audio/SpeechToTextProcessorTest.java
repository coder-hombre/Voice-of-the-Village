package com.foogly.voiceofthevillage.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests for SpeechToTextProcessor functionality.
 * Note: These tests use mock data and don't make actual API calls.
 */
class SpeechToTextProcessorTest {
    
    private SpeechToTextProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new SpeechToTextProcessor();
    }
    
    @Test
    void testProcessAudioWithNullData() throws Exception {
        CompletableFuture<SpeechToTextProcessor.SpeechToTextResult> future = 
            processor.processAudio(null, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        SpeechToTextProcessor.SpeechToTextResult result = future.get(1, TimeUnit.SECONDS);
        
        assertFalse(result.isSuccess());
        assertEquals("No audio data provided", result.getErrorMessage());
    }
    
    @Test
    void testProcessAudioWithEmptyData() throws Exception {
        byte[] emptyData = new byte[0];
        
        CompletableFuture<SpeechToTextProcessor.SpeechToTextResult> future = 
            processor.processAudio(emptyData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        SpeechToTextProcessor.SpeechToTextResult result = future.get(1, TimeUnit.SECONDS);
        
        assertFalse(result.isSuccess());
        assertEquals("No audio data provided", result.getErrorMessage());
    }
    
    @Test
    void testProcessAudioWithoutAIConfiguration() throws Exception {
        // Create test audio data
        byte[] testData = createTestAudioData(1000);
        
        // Note: This test assumes AI is not configured in the test environment
        CompletableFuture<SpeechToTextProcessor.SpeechToTextResult> future = 
            processor.processAudio(testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        SpeechToTextProcessor.SpeechToTextResult result = future.get(1, TimeUnit.SECONDS);
        
        // Should fail due to missing AI configuration
        assertFalse(result.isSuccess());
        assertEquals("AI service not configured", result.getErrorMessage());
    }
    
    @Test
    void testSpeechToTextResultSuccess() {
        String testText = "Hello, this is a test transcription";
        SpeechToTextProcessor.SpeechToTextResult result = 
            SpeechToTextProcessor.SpeechToTextResult.success(testText);
        
        assertTrue(result.isSuccess());
        assertEquals(testText, result.getText());
        assertNull(result.getErrorMessage());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains(testText));
    }
    
    @Test
    void testSpeechToTextResultFailure() {
        String errorMessage = "Speech recognition failed";
        SpeechToTextProcessor.SpeechToTextResult result = 
            SpeechToTextProcessor.SpeechToTextResult.failure(errorMessage);
        
        assertFalse(result.isSuccess());
        assertNull(result.getText());
        assertEquals(errorMessage, result.getErrorMessage());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains(errorMessage));
    }
    
    @Test
    void testProcessAudioWithLargeFile() throws Exception {
        // Create audio data larger than the limit (25MB)
        byte[] largeData = new byte[26 * 1024 * 1024]; // 26MB
        
        CompletableFuture<SpeechToTextProcessor.SpeechToTextResult> future = 
            processor.processAudio(largeData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        SpeechToTextProcessor.SpeechToTextResult result = future.get(1, TimeUnit.SECONDS);
        
        // Should fail due to size limit
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("too large") || 
                  result.getErrorMessage().contains("AI service not configured"));
    }
    
    @Test
    void testProcessAudioWithSilentData() throws Exception {
        // Create silent audio data (should be filtered out by noise reduction)
        byte[] silentData = new byte[2000]; // 1000 samples of silence
        
        CompletableFuture<SpeechToTextProcessor.SpeechToTextResult> future = 
            processor.processAudio(silentData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        SpeechToTextProcessor.SpeechToTextResult result = future.get(1, TimeUnit.SECONDS);
        
        // Should fail due to no speech detected or AI not configured
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("No speech detected") || 
                  result.getErrorMessage().contains("AI service not configured"));
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