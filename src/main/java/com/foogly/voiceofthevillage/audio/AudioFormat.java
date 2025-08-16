package com.foogly.voiceofthevillage.audio;

/**
 * Represents audio format specifications for voice processing.
 * Defines sample rate, bit depth, channels, and encoding format.
 */
public class AudioFormat {
    public static final AudioFormat DEFAULT_RECORDING_FORMAT = new AudioFormat(
        16000, // 16kHz sample rate - good balance of quality and file size
        16,    // 16-bit depth
        1,     // Mono channel
        AudioEncoding.PCM_SIGNED
    );
    
    public static final AudioFormat HIGH_QUALITY_FORMAT = new AudioFormat(
        44100, // CD quality sample rate
        16,    // 16-bit depth
        1,     // Mono channel
        AudioEncoding.PCM_SIGNED
    );
    
    private final int sampleRate;
    private final int bitDepth;
    private final int channels;
    private final AudioEncoding encoding;
    
    public AudioFormat(int sampleRate, int bitDepth, int channels, AudioEncoding encoding) {
        this.sampleRate = sampleRate;
        this.bitDepth = bitDepth;
        this.channels = channels;
        this.encoding = encoding;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public int getBitDepth() {
        return bitDepth;
    }
    
    public int getChannels() {
        return channels;
    }
    
    public AudioEncoding getEncoding() {
        return encoding;
    }
    
    /**
     * Gets the frame size in bytes (bytes per sample * channels).
     */
    public int getFrameSize() {
        return (bitDepth / 8) * channels;
    }
    
    /**
     * Calculates the number of bytes needed for the given duration.
     */
    public int getBytesForDuration(double durationSeconds) {
        return (int) (sampleRate * getFrameSize() * durationSeconds);
    }
    
    /**
     * Calculates the duration in seconds for the given number of bytes.
     */
    public double getDurationForBytes(int bytes) {
        return (double) bytes / (sampleRate * getFrameSize());
    }
    
    @Override
    public String toString() {
        return String.format("AudioFormat{rate=%dHz, depth=%dbit, channels=%d, encoding=%s}",
                           sampleRate, bitDepth, channels, encoding);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AudioFormat that = (AudioFormat) obj;
        return sampleRate == that.sampleRate &&
               bitDepth == that.bitDepth &&
               channels == that.channels &&
               encoding == that.encoding;
    }
    
    @Override
    public int hashCode() {
        int result = sampleRate;
        result = 31 * result + bitDepth;
        result = 31 * result + channels;
        result = 31 * result + encoding.hashCode();
        return result;
    }
    
    /**
     * Audio encoding formats supported by the system.
     */
    public enum AudioEncoding {
        PCM_SIGNED,
        PCM_UNSIGNED,
        PCM_FLOAT
    }
}