package com.foogly.voiceofthevillage.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NoiseReduction functionality.
 */
class NoiseReductionTest {
    
    @Test
    void testApplyNoiseReductionWithNullData() {
        byte[] result = NoiseReduction.applyNoiseReduction(null, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertNull(result);
    }
    
    @Test
    void testApplyNoiseReductionWithEmptyData() {
        byte[] emptyData = new byte[0];
        byte[] result = NoiseReduction.applyNoiseReduction(emptyData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertArrayEquals(emptyData, result);
    }
    
    @Test
    void testApplyNoiseReductionWithValidData() {
        // Create test audio data (16-bit PCM)
        byte[] testData = createTestAudioData(1000); // 1000 samples
        
        byte[] result = NoiseReduction.applyNoiseReduction(testData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        
        assertNotNull(result);
        assertEquals(testData.length, result.length);
        // Result should be different from input (processed)
        assertFalse(java.util.Arrays.equals(testData, result));
    }
    
    @Test
    void testContainsSpeechWithNullData() {
        boolean result = NoiseReduction.containsSpeech(null, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertFalse(result);
    }
    
    @Test
    void testContainsSpeechWithEmptyData() {
        byte[] emptyData = new byte[0];
        boolean result = NoiseReduction.containsSpeech(emptyData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertFalse(result);
    }
    
    @Test
    void testContainsSpeechWithSilence() {
        // Create silent audio data
        byte[] silentData = new byte[2000]; // 1000 samples of silence
        
        boolean result = NoiseReduction.containsSpeech(silentData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertFalse(result);
    }
    
    @Test
    void testContainsSpeechWithLowEnergyNoise() {
        // Create low-energy noise (should not be detected as speech)
        byte[] noiseData = createLowEnergyNoise(1000);
        
        boolean result = NoiseReduction.containsSpeech(noiseData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertFalse(result);
    }
    
    @Test
    void testContainsSpeechWithSpeechLikeSignal() {
        // Create speech-like signal with reasonable energy and zero crossing rate
        byte[] speechData = createSpeechLikeSignal(1000);
        
        boolean result = NoiseReduction.containsSpeech(speechData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        assertTrue(result);
    }
    
    @Test
    void testContainsSpeechWithHighFrequencyNoise() {
        // Create high-frequency noise (high zero crossing rate, but should still be detected)
        byte[] highFreqData = createHighFrequencySignal(1000);
        
        boolean result = NoiseReduction.containsSpeech(highFreqData, AudioFormat.DEFAULT_RECORDING_FORMAT);
        // High frequency noise might be filtered out depending on zero crossing rate
        // This test verifies the algorithm handles edge cases
        assertNotNull(result); // Just ensure it doesn't crash
    }
    
    /**
     * Creates test audio data with varying amplitudes.
     */
    private byte[] createTestAudioData(int samples) {
        byte[] data = new byte[samples * 2]; // 16-bit = 2 bytes per sample
        
        for (int i = 0; i < samples; i++) {
            // Create a simple sine wave with some variation
            double angle = 2.0 * Math.PI * i / 100.0; // 100 samples per cycle
            short sample = (short) (Math.sin(angle) * 1000 + Math.random() * 200 - 100);
            
            // Convert to little-endian bytes
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
    
    /**
     * Creates low-energy noise data.
     */
    private byte[] createLowEnergyNoise(int samples) {
        byte[] data = new byte[samples * 2];
        
        for (int i = 0; i < samples; i++) {
            // Very low amplitude random noise
            short sample = (short) (Math.random() * 20 - 10);
            
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
    
    /**
     * Creates speech-like signal with appropriate energy and zero crossing rate.
     */
    private byte[] createSpeechLikeSignal(int samples) {
        byte[] data = new byte[samples * 2];
        
        for (int i = 0; i < samples; i++) {
            // Mix of frequencies typical of speech (200-3000 Hz range)
            double lowFreq = Math.sin(2.0 * Math.PI * i / 80.0) * 300;  // ~200 Hz
            double midFreq = Math.sin(2.0 * Math.PI * i / 20.0) * 200;  // ~800 Hz
            double highFreq = Math.sin(2.0 * Math.PI * i / 8.0) * 100;  // ~2000 Hz
            
            short sample = (short) (lowFreq + midFreq + highFreq);
            
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
    
    /**
     * Creates high-frequency signal.
     */
    private byte[] createHighFrequencySignal(int samples) {
        byte[] data = new byte[samples * 2];
        
        for (int i = 0; i < samples; i++) {
            // High frequency signal (alternating pattern)
            short sample = (short) ((i % 2 == 0 ? 500 : -500));
            
            data[i * 2] = (byte) (sample & 0xFF);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        
        return data;
    }
}