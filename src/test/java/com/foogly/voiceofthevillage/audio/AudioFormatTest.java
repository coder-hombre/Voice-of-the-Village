package com.foogly.voiceofthevillage.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AudioFormat class functionality.
 */
class AudioFormatTest {
    
    @Test
    void testDefaultRecordingFormat() {
        AudioFormat format = AudioFormat.DEFAULT_RECORDING_FORMAT;
        
        assertEquals(16000, format.getSampleRate());
        assertEquals(16, format.getBitDepth());
        assertEquals(1, format.getChannels());
        assertEquals(AudioFormat.AudioEncoding.PCM_SIGNED, format.getEncoding());
    }
    
    @Test
    void testHighQualityFormat() {
        AudioFormat format = AudioFormat.HIGH_QUALITY_FORMAT;
        
        assertEquals(44100, format.getSampleRate());
        assertEquals(16, format.getBitDepth());
        assertEquals(1, format.getChannels());
        assertEquals(AudioFormat.AudioEncoding.PCM_SIGNED, format.getEncoding());
    }
    
    @Test
    void testFrameSize() {
        AudioFormat monoFormat = new AudioFormat(44100, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        assertEquals(2, monoFormat.getFrameSize()); // 16 bits = 2 bytes, 1 channel
        
        AudioFormat stereoFormat = new AudioFormat(44100, 16, 2, AudioFormat.AudioEncoding.PCM_SIGNED);
        assertEquals(4, stereoFormat.getFrameSize()); // 16 bits = 2 bytes, 2 channels
        
        AudioFormat format24Bit = new AudioFormat(44100, 24, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        assertEquals(3, format24Bit.getFrameSize()); // 24 bits = 3 bytes, 1 channel
    }
    
    @Test
    void testBytesForDuration() {
        AudioFormat format = new AudioFormat(44100, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        
        // 1 second of audio
        int bytesFor1Second = format.getBytesForDuration(1.0);
        assertEquals(88200, bytesFor1Second); // 44100 samples * 2 bytes
        
        // 0.5 seconds of audio
        int bytesForHalfSecond = format.getBytesForDuration(0.5);
        assertEquals(44100, bytesForHalfSecond);
    }
    
    @Test
    void testDurationForBytes() {
        AudioFormat format = new AudioFormat(44100, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        
        // 88200 bytes should be 1 second
        double duration = format.getDurationForBytes(88200);
        assertEquals(1.0, duration, 0.001);
        
        // 44100 bytes should be 0.5 seconds
        double halfSecondDuration = format.getDurationForBytes(44100);
        assertEquals(0.5, halfSecondDuration, 0.001);
    }
    
    @Test
    void testEqualsAndHashCode() {
        AudioFormat format1 = new AudioFormat(44100, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        AudioFormat format2 = new AudioFormat(44100, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        AudioFormat format3 = new AudioFormat(48000, 16, 1, AudioFormat.AudioEncoding.PCM_SIGNED);
        
        assertEquals(format1, format2);
        assertNotEquals(format1, format3);
        
        assertEquals(format1.hashCode(), format2.hashCode());
        assertNotEquals(format1.hashCode(), format3.hashCode());
    }
    
    @Test
    void testToString() {
        AudioFormat format = new AudioFormat(44100, 16, 2, AudioFormat.AudioEncoding.PCM_SIGNED);
        String toString = format.toString();
        
        assertTrue(toString.contains("44100"));
        assertTrue(toString.contains("16"));
        assertTrue(toString.contains("2"));
        assertTrue(toString.contains("PCM_SIGNED"));
    }
}