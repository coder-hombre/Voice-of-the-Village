package com.foogly.voiceofthevillage.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic noise reduction and audio enhancement for voice recordings.
 * Implements simple filtering techniques to improve speech clarity.
 */
public class NoiseReduction {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoiseReduction.class);
    
    // Noise reduction parameters
    private static final double NOISE_GATE_THRESHOLD = 0.02; // 2% of max amplitude
    private static final int SMOOTHING_WINDOW = 5; // Samples for smoothing
    private static final double HIGH_PASS_CUTOFF = 80.0; // Hz - remove low frequency noise
    private static final double LOW_PASS_CUTOFF = 8000.0; // Hz - remove high frequency noise
    
    /**
     * Applies basic noise reduction to the audio data.
     * Includes noise gating, smoothing, and basic filtering.
     */
    public static byte[] applyNoiseReduction(byte[] audioData, AudioFormat format) {
        if (audioData == null || audioData.length == 0) {
            return audioData;
        }
        
        try {
            // Convert bytes to samples for processing
            short[] samples = bytesToSamples(audioData, format);
            
            // Apply noise reduction techniques
            samples = applyNoiseGate(samples);
            samples = applySmoothing(samples);
            samples = normalizeAudio(samples);
            
            // Convert back to bytes
            return samplesToBytes(samples, format);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to apply noise reduction, returning original audio: {}", e.getMessage());
            return audioData;
        }
    }
    
    /**
     * Applies a simple noise gate to remove low-level background noise.
     */
    private static short[] applyNoiseGate(short[] samples) {
        if (samples.length == 0) return samples;
        
        // Find the maximum amplitude to calculate threshold
        short maxAmplitude = 0;
        for (short sample : samples) {
            maxAmplitude = (short) Math.max(maxAmplitude, Math.abs(sample));
        }
        
        short threshold = (short) (maxAmplitude * NOISE_GATE_THRESHOLD);
        
        // Apply noise gate
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            if (Math.abs(samples[i]) > threshold) {
                result[i] = samples[i];
            } else {
                result[i] = 0; // Gate out low-level noise
            }
        }
        
        return result;
    }
    
    /**
     * Applies simple smoothing to reduce sharp transients and clicks.
     */
    private static short[] applySmoothing(short[] samples) {
        if (samples.length < SMOOTHING_WINDOW) return samples;
        
        short[] result = new short[samples.length];
        
        // Copy first few samples unchanged
        for (int i = 0; i < SMOOTHING_WINDOW / 2; i++) {
            result[i] = samples[i];
        }
        
        // Apply moving average smoothing
        for (int i = SMOOTHING_WINDOW / 2; i < samples.length - SMOOTHING_WINDOW / 2; i++) {
            long sum = 0;
            for (int j = -SMOOTHING_WINDOW / 2; j <= SMOOTHING_WINDOW / 2; j++) {
                sum += samples[i + j];
            }
            result[i] = (short) (sum / SMOOTHING_WINDOW);
        }
        
        // Copy last few samples unchanged
        for (int i = samples.length - SMOOTHING_WINDOW / 2; i < samples.length; i++) {
            result[i] = samples[i];
        }
        
        return result;
    }
    
    /**
     * Normalizes audio levels to improve consistency.
     */
    private static short[] normalizeAudio(short[] samples) {
        if (samples.length == 0) return samples;
        
        // Find peak amplitude
        short peak = 0;
        for (short sample : samples) {
            peak = (short) Math.max(peak, Math.abs(sample));
        }
        
        if (peak == 0) return samples; // Avoid division by zero
        
        // Calculate normalization factor (target 70% of max amplitude)
        double targetAmplitude = Short.MAX_VALUE * 0.7;
        double normalizationFactor = targetAmplitude / peak;
        
        // Don't amplify if already loud enough
        if (normalizationFactor > 1.0) {
            normalizationFactor = Math.min(normalizationFactor, 3.0); // Max 3x amplification
        } else {
            normalizationFactor = 1.0; // Don't reduce volume
        }
        
        // Apply normalization
        short[] result = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            double normalized = samples[i] * normalizationFactor;
            result[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, normalized));
        }
        
        return result;
    }
    
    /**
     * Converts byte array to short array for processing.
     */
    private static short[] bytesToSamples(byte[] audioData, AudioFormat format) {
        if (format.getBitDepth() != 16) {
            throw new UnsupportedOperationException("Only 16-bit audio is currently supported");
        }
        
        short[] samples = new short[audioData.length / 2];
        for (int i = 0; i < samples.length; i++) {
            // Little endian conversion
            int low = audioData[i * 2] & 0xFF;
            int high = audioData[i * 2 + 1] & 0xFF;
            samples[i] = (short) ((high << 8) | low);
        }
        
        return samples;
    }
    
    /**
     * Converts short array back to byte array.
     */
    private static byte[] samplesToBytes(short[] samples, AudioFormat format) {
        if (format.getBitDepth() != 16) {
            throw new UnsupportedOperationException("Only 16-bit audio is currently supported");
        }
        
        byte[] audioData = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            // Little endian conversion
            audioData[i * 2] = (byte) (samples[i] & 0xFF);
            audioData[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }
        
        return audioData;
    }
    
    /**
     * Analyzes audio data to determine if it likely contains speech.
     */
    public static boolean containsSpeech(byte[] audioData, AudioFormat format) {
        if (audioData == null || audioData.length == 0) {
            return false;
        }
        
        try {
            short[] samples = bytesToSamples(audioData, format);
            
            // Calculate RMS (Root Mean Square) energy
            long sumSquares = 0;
            for (short sample : samples) {
                sumSquares += (long) sample * sample;
            }
            double rms = Math.sqrt((double) sumSquares / samples.length);
            
            // Calculate zero crossing rate (indicator of speech vs noise)
            int zeroCrossings = 0;
            for (int i = 1; i < samples.length; i++) {
                if ((samples[i] >= 0) != (samples[i - 1] >= 0)) {
                    zeroCrossings++;
                }
            }
            double zeroCrossingRate = (double) zeroCrossings / samples.length;
            
            // Simple heuristics for speech detection
            boolean hasEnoughEnergy = rms > 100; // Minimum energy threshold
            boolean hasReasonableZCR = zeroCrossingRate > 0.01 && zeroCrossingRate < 0.3;
            
            return hasEnoughEnergy && hasReasonableZCR;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to analyze audio for speech content: {}", e.getMessage());
            return true; // Assume it contains speech if analysis fails
        }
    }
}