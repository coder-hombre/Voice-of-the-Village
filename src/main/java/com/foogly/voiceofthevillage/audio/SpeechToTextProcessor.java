package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.error.ErrorHandler;
import com.foogly.voiceofthevillage.error.RetryManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Processes voice input by converting speech to text using external speech recognition services.
 * Supports multiple providers and handles audio format conversion and error cases.
 */
public class SpeechToTextProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechToTextProcessor.class);
    
    private static final String OPENAI_WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final int MAX_AUDIO_SIZE = 25 * 1024 * 1024; // 25MB limit for OpenAI Whisper
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public SpeechToTextProcessor() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Converts audio data to text using speech recognition.
     */
    public CompletableFuture<SpeechToTextResult> processAudio(byte[] audioData, AudioFormat format) {
        if (audioData == null || audioData.length == 0) {
            return CompletableFuture.completedFuture(
                SpeechToTextResult.failure("No audio data provided")
            );
        }
        
        if (!VoiceConfig.isAIConfigured()) {
            return CompletableFuture.completedFuture(
                SpeechToTextResult.failure("AI service not configured")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performSpeechToText(audioData, format);
            } catch (Exception e) {
                LOGGER.error("Speech-to-text processing failed", e);
                return SpeechToTextResult.failure("Speech recognition failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Performs the actual speech-to-text conversion.
     */
    private SpeechToTextResult performSpeechToText(byte[] audioData, AudioFormat format) throws IOException, InterruptedException {
        // Apply noise reduction to improve recognition accuracy
        byte[] processedAudio = NoiseReduction.applyNoiseReduction(audioData, format);
        
        // Check if audio likely contains speech
        if (!NoiseReduction.containsSpeech(processedAudio, format)) {
            LOGGER.debug("Audio does not appear to contain speech");
            return SpeechToTextResult.failure("No speech detected in audio");
        }
        
        // Check audio size limits
        if (processedAudio.length > MAX_AUDIO_SIZE) {
            LOGGER.warn("Audio data too large: {} bytes (max: {})", processedAudio.length, MAX_AUDIO_SIZE);
            return SpeechToTextResult.failure("Audio file too large");
        }
        
        // Convert to appropriate format for the service
        String provider = VoiceConfig.AI_PROVIDER.get().toLowerCase();
        
        switch (provider) {
            case "openai":
                return processWithOpenAIWhisper(processedAudio, format);
            default:
                LOGGER.warn("Unsupported speech-to-text provider: {}", provider);
                return SpeechToTextResult.failure("Unsupported speech recognition provider: " + provider);
        }
    }
    
    /**
     * Processes audio using OpenAI Whisper API.
     */
    private SpeechToTextResult processWithOpenAIWhisper(byte[] audioData, AudioFormat format) throws IOException, InterruptedException {
        String apiKey = VoiceConfig.AI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return SpeechToTextResult.failure("OpenAI API key not configured");
        }
        
        // Convert audio to WAV format for Whisper
        byte[] wavData = convertToWav(audioData, format);
        String base64Audio = Base64.getEncoder().encodeToString(wavData);
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "whisper-1");
        requestBody.addProperty("audio", base64Audio);
        requestBody.addProperty("response_format", "json");
        requestBody.addProperty("language", "en"); // English language
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_WHISPER_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        // Send request and process response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            String transcribedText = responseJson.get("text").getAsString().trim();
            
            if (transcribedText.isEmpty()) {
                return SpeechToTextResult.failure("No speech recognized");
            }
            
            LOGGER.debug("Speech-to-text successful: '{}'", transcribedText);
            return SpeechToTextResult.success(transcribedText);
            
        } else {
            String errorMessage = "Speech recognition failed with status: " + response.statusCode();
            try {
                JsonObject errorJson = gson.fromJson(response.body(), JsonObject.class);
                if (errorJson.has("error")) {
                    JsonObject error = errorJson.getAsJsonObject("error");
                    errorMessage = error.get("message").getAsString();
                }
            } catch (Exception e) {
                // Use default error message if parsing fails
            }
            
            LOGGER.error("OpenAI Whisper API error: {}", errorMessage);
            return SpeechToTextResult.failure(errorMessage);
        }
    }
    
    /**
     * Converts audio data to WAV format for compatibility with speech recognition services.
     */
    private byte[] convertToWav(byte[] audioData, AudioFormat format) {
        // Simple WAV header creation for PCM audio
        int dataSize = audioData.length;
        int fileSize = dataSize + 36; // WAV header is 44 bytes, data size is file size - 8
        
        byte[] wavHeader = new byte[44];
        int pos = 0;
        
        // RIFF header
        wavHeader[pos++] = 'R'; wavHeader[pos++] = 'I'; wavHeader[pos++] = 'F'; wavHeader[pos++] = 'F';
        writeInt(wavHeader, pos, fileSize); pos += 4;
        wavHeader[pos++] = 'W'; wavHeader[pos++] = 'A'; wavHeader[pos++] = 'V'; wavHeader[pos++] = 'E';
        
        // Format chunk
        wavHeader[pos++] = 'f'; wavHeader[pos++] = 'm'; wavHeader[pos++] = 't'; wavHeader[pos++] = ' ';
        writeInt(wavHeader, pos, 16); pos += 4; // Format chunk size
        writeShort(wavHeader, pos, (short) 1); pos += 2; // PCM format
        writeShort(wavHeader, pos, (short) format.getChannels()); pos += 2;
        writeInt(wavHeader, pos, format.getSampleRate()); pos += 4;
        writeInt(wavHeader, pos, format.getSampleRate() * format.getFrameSize()); pos += 4; // Byte rate
        writeShort(wavHeader, pos, (short) format.getFrameSize()); pos += 2; // Block align
        writeShort(wavHeader, pos, (short) format.getBitDepth()); pos += 2;
        
        // Data chunk
        wavHeader[pos++] = 'd'; wavHeader[pos++] = 'a'; wavHeader[pos++] = 't'; wavHeader[pos++] = 'a';
        writeInt(wavHeader, pos, dataSize);
        
        // Combine header and audio data
        byte[] wavData = new byte[wavHeader.length + audioData.length];
        System.arraycopy(wavHeader, 0, wavData, 0, wavHeader.length);
        System.arraycopy(audioData, 0, wavData, wavHeader.length, audioData.length);
        
        return wavData;
    }
    
    /**
     * Writes a 32-bit integer to byte array in little-endian format.
     */
    private void writeInt(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
    
    /**
     * Writes a 16-bit short to byte array in little-endian format.
     */
    private void writeShort(byte[] buffer, int offset, short value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
    
    /**
     * Result of speech-to-text processing.
     */
    public static class SpeechToTextResult {
        private final boolean success;
        private final String text;
        private final String errorMessage;
        
        private SpeechToTextResult(boolean success, String text, String errorMessage) {
            this.success = success;
            this.text = text;
            this.errorMessage = errorMessage;
        }
        
        public static SpeechToTextResult success(String text) {
            return new SpeechToTextResult(true, text, null);
        }
        
        public static SpeechToTextResult failure(String errorMessage) {
            return new SpeechToTextResult(false, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getText() {
            return text;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "SpeechToTextResult{success=true, text='" + text + "'}";
            } else {
                return "SpeechToTextResult{success=false, error='" + errorMessage + "'}";
            }
        }
    }
}