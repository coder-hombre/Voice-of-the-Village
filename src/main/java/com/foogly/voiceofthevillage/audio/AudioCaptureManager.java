package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.error.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages audio capture from the system microphone.
 * Handles microphone access, recording, and basic audio processing.
 */
public class AudioCaptureManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioCaptureManager.class);
    
    private static final int BUFFER_SIZE = 4096;
    private static final double MAX_RECORDING_DURATION = 30.0; // 30 seconds max
    
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private TargetDataLine microphone;
    private ByteArrayOutputStream audioBuffer;
    private AudioFormat recordingFormat;
    
    /**
     * Checks if audio capture is available on this system.
     */
    public boolean isAudioCaptureAvailable() {
        try {
            javax.sound.sampled.AudioFormat format = createJavaAudioFormat(AudioFormat.DEFAULT_RECORDING_FORMAT);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            LOGGER.warn("Audio capture not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Starts audio recording with the specified format.
     */
    public CompletableFuture<AudioRecording> startRecording(AudioFormat format) {
        if (isRecording.get()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Already recording audio")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performRecording(format);
            } catch (Exception e) {
                LOGGER.error("Failed to record audio", e);
                throw new RuntimeException("Audio recording failed", e);
            }
        });
    }
    
    /**
     * Stops the current recording and returns the captured audio.
     */
    public void stopRecording() {
        if (isRecording.get()) {
            isRecording.set(false);
            LOGGER.debug("Stopping audio recording");
        }
    }
    
    /**
     * Performs the actual audio recording.
     */
    private AudioRecording performRecording(AudioFormat format) throws LineUnavailableException, IOException {
        this.recordingFormat = format;
        this.audioBuffer = new ByteArrayOutputStream();
        
        // Create and open microphone line
        javax.sound.sampled.AudioFormat javaFormat = createJavaAudioFormat(format);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, javaFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio format not supported: " + format);
        }
        
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(javaFormat, BUFFER_SIZE);
        microphone.start();
        
        isRecording.set(true);
        LOGGER.debug("Started audio recording with format: {}", format);
        
        // Record audio data
        byte[] buffer = new byte[BUFFER_SIZE];
        long startTime = System.currentTimeMillis();
        long maxDurationMs = (long) (MAX_RECORDING_DURATION * 1000);
        
        try {
            while (isRecording.get()) {
                // Check for maximum duration
                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    LOGGER.warn("Recording stopped due to maximum duration limit");
                    break;
                }
                
                // Read audio data
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead);
                }
                
                // Small delay to prevent excessive CPU usage
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            // Clean up resources
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            isRecording.set(false);
        }
        
        byte[] audioData = audioBuffer.toByteArray();
        double duration = format.getDurationForBytes(audioData.length);
        
        LOGGER.debug("Recorded {} bytes of audio data ({:.2f} seconds)", 
                    audioData.length, duration);
        
        return new AudioRecording(audioData, format, duration);
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
     * Gets the current recording status.
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Represents a completed audio recording.
     */
    public static class AudioRecording {
        private final byte[] audioData;
        private final AudioFormat format;
        private final double duration;
        
        public AudioRecording(byte[] audioData, AudioFormat format, double duration) {
            this.audioData = audioData.clone();
            this.format = format;
            this.duration = duration;
        }
        
        public byte[] getAudioData() {
            return audioData.clone();
        }
        
        public AudioFormat getFormat() {
            return format;
        }
        
        public double getDuration() {
            return duration;
        }
        
        public int getDataSize() {
            return audioData.length;
        }
        
        /**
         * Checks if the recording contains meaningful audio data.
         */
        public boolean hasAudioData() {
            return audioData.length > 0 && duration > 0.1; // At least 100ms
        }
    }
}