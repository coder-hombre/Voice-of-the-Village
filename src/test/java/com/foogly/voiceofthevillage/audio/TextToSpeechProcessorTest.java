package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests for TextToSpeechProcessor functionality.
 * Note: These tests use mock data and don't make actual API calls.
 */
class TextToSpeechProcessorTest {
    
    private TextToSpeechProcessor processor;
    private VoiceProfile testVoiceProfile;
    
    @BeforeEach
    void setUp() {
        processor = new TextToSpeechProcessor();
        testVoiceProfile = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.FRIENDLY);
    }
    
    @Test
    void testProcessTextWithNullText() throws Exception {
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> future = 
            processor.processText(null, testVoiceProfile);
        
        TextToSpeechProcessor.TextToSpeechResult result = future.get(1, TimeUnit.SECONDS);
        
        assertFalse(result.isSuccess());
        assertEquals("No text provided for speech synthesis", result.getErrorMessage());
    }
    
    @Test
    void testProcessTextWithEmptyText() throws Exception {
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> future = 
            processor.processText("", testVoiceProfile);
        
        TextToSpeechProcessor.TextToSpeechResult result = future.get(1, TimeUnit.SECONDS);
        
        assertFalse(result.isSuccess());
        assertEquals("No text provided for speech synthesis", result.getErrorMessage());
    }
    
    @Test
    void testProcessTextWithWhitespaceOnly() throws Exception {
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> future = 
            processor.processText("   \n\t   ", testVoiceProfile);
        
        TextToSpeechProcessor.TextToSpeechResult result = future.get(1, TimeUnit.SECONDS);
        
        assertFalse(result.isSuccess());
        assertEquals("No text provided for speech synthesis", result.getErrorMessage());
    }
    
    @Test
    void testProcessTextWithoutAIConfiguration() throws Exception {
        String testText = "Hello, this is a test message.";
        
        // Note: This test assumes AI is not configured in the test environment
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> future = 
            processor.processText(testText, testVoiceProfile);
        
        TextToSpeechProcessor.TextToSpeechResult result = future.get(1, TimeUnit.SECONDS);
        
        // Should fail due to missing AI configuration
        assertFalse(result.isSuccess());
        assertEquals("AI service not configured", result.getErrorMessage());
    }
    
    @Test
    void testTextToSpeechResultSuccess() {
        String testText = "Hello, this is a test transcription";
        byte[] testAudioData = new byte[]{1, 2, 3, 4, 5};
        AudioFormat testFormat = AudioFormat.DEFAULT_RECORDING_FORMAT;
        
        TextToSpeechProcessor.TextToSpeechResult result = 
            TextToSpeechProcessor.TextToSpeechResult.success(testAudioData, testFormat, testText);
        
        assertTrue(result.isSuccess());
        assertEquals(testText, result.getOriginalText());
        assertEquals(testFormat, result.getAudioFormat());
        assertEquals(5, result.getAudioDataSize());
        assertArrayEquals(testAudioData, result.getAudioData());
        assertNull(result.getErrorMessage());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=true"));
        assertTrue(toString.contains(testText));
        assertTrue(toString.contains("audioSize=5"));
    }
    
    @Test
    void testTextToSpeechResultFailure() {
        String errorMessage = "TTS processing failed";
        TextToSpeechProcessor.TextToSpeechResult result = 
            TextToSpeechProcessor.TextToSpeechResult.failure(errorMessage);
        
        assertFalse(result.isSuccess());
        assertNull(result.getOriginalText());
        assertNull(result.getAudioFormat());
        assertNull(result.getAudioData());
        assertEquals(0, result.getAudioDataSize());
        assertEquals(errorMessage, result.getErrorMessage());
        
        String toString = result.toString();
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains(errorMessage));
    }
    
    @Test
    void testAudioDataCloning() {
        byte[] originalData = new byte[]{1, 2, 3, 4, 5};
        TextToSpeechProcessor.TextToSpeechResult result = 
            TextToSpeechProcessor.TextToSpeechResult.success(
                originalData, AudioFormat.DEFAULT_RECORDING_FORMAT, "test"
            );
        
        byte[] retrievedData = result.getAudioData();
        
        // Should be equal but not the same object
        assertArrayEquals(originalData, retrievedData);
        assertNotSame(originalData, retrievedData);
        
        // Modifying retrieved data should not affect original
        retrievedData[0] = 99;
        assertEquals(1, originalData[0]); // Original unchanged
    }
    
    @Test
    void testProcessTextWithLongText() throws Exception {
        // Create text longer than the limit (4096 characters)
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            longText.append("a");
        }
        
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> future = 
            processor.processText(longText.toString(), testVoiceProfile);
        
        TextToSpeechProcessor.TextToSpeechResult result = future.get(1, TimeUnit.SECONDS);
        
        // Should fail due to AI not configured, but text length handling should work
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("AI service not configured"));
    }
    
    @Test
    void testProcessTextWithDifferentVoiceProfiles() throws Exception {
        String testText = "Hello, world!";
        
        VoiceProfile maleProfile = VoiceProfile.createForVillager(Gender.MALE, PersonalityType.GRUMPY);
        VoiceProfile femaleProfile = VoiceProfile.createForVillager(Gender.FEMALE, PersonalityType.CHEERFUL);
        
        // Both should fail due to AI not configured, but should handle different profiles
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> maleFuture = 
            processor.processText(testText, maleProfile);
        CompletableFuture<TextToSpeechProcessor.TextToSpeechResult> femaleFuture = 
            processor.processText(testText, femaleProfile);
        
        TextToSpeechProcessor.TextToSpeechResult maleResult = maleFuture.get(1, TimeUnit.SECONDS);
        TextToSpeechProcessor.TextToSpeechResult femaleResult = femaleFuture.get(1, TimeUnit.SECONDS);
        
        // Both should fail with same error (AI not configured)
        assertFalse(maleResult.isSuccess());
        assertFalse(femaleResult.isSuccess());
        assertEquals(maleResult.getErrorMessage(), femaleResult.getErrorMessage());
    }
    
    @Test
    void testResultToStringWithNullValues() {
        // Test toString with null audio data
        TextToSpeechProcessor.TextToSpeechResult result = 
            TextToSpeechProcessor.TextToSpeechResult.failure("Test error");
        
        String toString = result.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains("Test error"));
    }
}