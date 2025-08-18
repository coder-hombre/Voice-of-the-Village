package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
 * Processes text responses by converting them to speech using external text-to-speech services.
 * Supports gender-based voice selection and personality-influenced speech characteristics.
 */
public class TextToSpeechProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextToSpeechProcessor.class);
    
    private static final String OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech";
    private static final int MAX_TEXT_LENGTH = 4096; // OpenAI TTS limit
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public TextToSpeechProcessor() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Converts text to speech using the specified voice profile.
     */
    public CompletableFuture<TextToSpeechResult> processText(String text, VoiceProfile voiceProfile) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                TextToSpeechResult.failure("No text provided for speech synthesis")
            );
        }
        
        if (!VoiceConfig.isAIConfigured()) {
            return CompletableFuture.completedFuture(
                TextToSpeechResult.failure("AI service not configured")
            );
        }
        
        if (!VoiceConfig.ENABLE_VOICE_OUTPUT.get()) {
            return CompletableFuture.completedFuture(
                TextToSpeechResult.failure("Voice output is disabled")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return performTextToSpeech(text.trim(), voiceProfile);
            } catch (Exception e) {
                LOGGER.error("Text-to-speech processing failed", e);
                return TextToSpeechResult.failure("Speech synthesis failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Performs the actual text-to-speech conversion.
     */
    private TextToSpeechResult performTextToSpeech(String text, VoiceProfile voiceProfile) 
            throws IOException, InterruptedException {
        
        // Check text length limits
        if (text.length() > MAX_TEXT_LENGTH) {
            LOGGER.warn("Text too long for TTS: {} characters (max: {})", text.length(), MAX_TEXT_LENGTH);
            // Truncate text to fit within limits
            text = text.substring(0, MAX_TEXT_LENGTH - 3) + "...";
        }
        
        // Clean up text for better speech synthesis
        String cleanedText = cleanTextForSpeech(text);
        
        // Get provider and process accordingly
        String provider = VoiceConfig.AI_PROVIDER.get().toLowerCase();
        
        switch (provider) {
            case "openai":
                return processWithOpenAITTS(cleanedText, voiceProfile);
            default:
                LOGGER.warn("Unsupported text-to-speech provider: {}", provider);
                return TextToSpeechResult.failure("Unsupported TTS provider: " + provider);
        }
    }
    
    /**
     * Processes text using OpenAI TTS API.
     */
    private TextToSpeechResult processWithOpenAITTS(String text, VoiceProfile voiceProfile) 
            throws IOException, InterruptedException {
        
        String apiKey = VoiceConfig.AI_API_KEY.get();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return TextToSpeechResult.failure("OpenAI API key not configured");
        }
        
        // Map our voice profile to OpenAI voice names
        String openAIVoice = mapToOpenAIVoice(voiceProfile);
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "tts-1");
        requestBody.addProperty("input", text);
        requestBody.addProperty("voice", openAIVoice);
        requestBody.addProperty("response_format", "mp3");
        requestBody.addProperty("speed", Math.max(0.25, Math.min(4.0, voiceProfile.getSpeed())));
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENAI_TTS_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        // Send request and process response
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() == 200) {
            byte[] audioData = response.body();
            
            if (audioData.length == 0) {
                return TextToSpeechResult.failure("No audio data received from TTS service");
            }
            
            // Convert MP3 to PCM for playback (simplified - in real implementation would use audio conversion library)
            AudioFormat outputFormat = AudioFormat.DEFAULT_RECORDING_FORMAT;
            byte[] pcmData = convertMp3ToPcm(audioData, outputFormat);
            
            LOGGER.debug("Text-to-speech successful: {} characters -> {} bytes audio", 
                        text.length(), pcmData.length);
            
            return TextToSpeechResult.success(pcmData, outputFormat, text);
            
        } else {
            String errorMessage = "TTS failed with status: " + response.statusCode();
            try {
                String responseBody = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
                JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                if (errorJson.has("error")) {
                    JsonObject error = errorJson.getAsJsonObject("error");
                    errorMessage = error.get("message").getAsString();
                }
            } catch (Exception e) {
                // Use default error message if parsing fails
            }
            
            LOGGER.error("OpenAI TTS API error: {}", errorMessage);
            return TextToSpeechResult.failure(errorMessage);
        }
    }
    
    /**
     * Maps our voice profile to OpenAI voice names.
     */
    private String mapToOpenAIVoice(VoiceProfile voiceProfile) {
        // OpenAI TTS voices: alloy, echo, fable, onyx, nova, shimmer
        switch (voiceProfile.getGender()) {
            case MALE:
                switch (voiceProfile.getPersonality()) {
                    case WISE:
                        return "onyx"; // Deep, authoritative
                    case GRUMPY:
                        return "echo"; // Rougher tone
                    case CURIOUS:
                        return "alloy"; // Dynamic
                    case MERCHANT:
                        return "onyx"; // Authoritative for business
                    case GOSSIP:
                        return "fable"; // More expressive
                    case CHEERFUL:
                    case FRIENDLY:
                    case CAUTIOUS:
                    default:
                        return "alloy"; // Standard male-ish voice
                }
            case FEMALE:
                switch (voiceProfile.getPersonality()) {
                    case WISE:
                        return "nova"; // Mature tone
                    case CHEERFUL:
                        return "shimmer"; // Bright tone
                    case CURIOUS:
                        return "fable"; // Dynamic
                    case GRUMPY:
                        return "nova"; // Slightly deeper
                    case MERCHANT:
                        return "nova"; // Professional tone
                    case GOSSIP:
                        return "shimmer"; // Bright, expressive
                    case CAUTIOUS:
                    case FRIENDLY:
                    default:
                        return "nova"; // Standard female voice
                }
            default:
                return "alloy"; // Neutral voice
        }
    }
    
    /**
     * Cleans text to improve speech synthesis quality.
     */
    private String cleanTextForSpeech(String text) {
        return text
            // Remove excessive punctuation
            .replaceAll("[!]{2,}", "!")
            .replaceAll("[?]{2,}", "?")
            .replaceAll("[.]{3,}", "...")
            // Replace common abbreviations with full words
            .replaceAll("\\bw/\\b", "with")
            .replaceAll("\\bu\\b", "you")
            .replaceAll("\\br\\b", "are")
            // Add pauses for better pacing
            .replaceAll("([.!?])\\s*([A-Z])", "$1 $2")
            // Clean up whitespace
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Converts MP3 audio data to PCM format.
     * This is a simplified placeholder - real implementation would use a proper audio conversion library.
     */
    private byte[] convertMp3ToPcm(byte[] mp3Data, AudioFormat targetFormat) {
        // TODO: Implement proper MP3 to PCM conversion
        // For now, return a placeholder PCM audio (silence)
        // In a real implementation, you would use libraries like JavaZOOM BasicPlayer or similar
        
        LOGGER.warn("MP3 to PCM conversion not fully implemented - returning placeholder audio");
        
        // Generate 1 second of silence as placeholder
        int sampleCount = targetFormat.getSampleRate(); // 1 second
        byte[] pcmData = new byte[sampleCount * targetFormat.getFrameSize()];
        
        // Fill with very quiet sine wave instead of complete silence
        for (int i = 0; i < sampleCount; i++) {
            double angle = 2.0 * Math.PI * i / 100.0; // Low frequency
            short sample = (short) (Math.sin(angle) * 100); // Very quiet
            
            // Convert to little-endian bytes
            int byteIndex = i * 2;
            if (byteIndex + 1 < pcmData.length) {
                pcmData[byteIndex] = (byte) (sample & 0xFF);
                pcmData[byteIndex + 1] = (byte) ((sample >> 8) & 0xFF);
            }
        }
        
        return pcmData;
    }
    
    /**
     * Result of text-to-speech processing.
     */
    public static class TextToSpeechResult {
        private final boolean success;
        private final byte[] audioData;
        private final AudioFormat audioFormat;
        private final String originalText;
        private final String errorMessage;
        
        private TextToSpeechResult(boolean success, byte[] audioData, AudioFormat audioFormat, 
                                 String originalText, String errorMessage) {
            this.success = success;
            this.audioData = audioData;
            this.audioFormat = audioFormat;
            this.originalText = originalText;
            this.errorMessage = errorMessage;
        }
        
        public static TextToSpeechResult success(byte[] audioData, AudioFormat audioFormat, String originalText) {
            return new TextToSpeechResult(true, audioData, audioFormat, originalText, null);
        }
        
        public static TextToSpeechResult failure(String errorMessage) {
            return new TextToSpeechResult(false, null, null, null, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public byte[] getAudioData() {
            return audioData != null ? audioData.clone() : null;
        }
        
        public AudioFormat getAudioFormat() {
            return audioFormat;
        }
        
        public String getOriginalText() {
            return originalText;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public int getAudioDataSize() {
            return audioData != null ? audioData.length : 0;
        }
        
        @Override
        public String toString() {
            if (success) {
                return String.format("TextToSpeechResult{success=true, text='%s', audioSize=%d}", 
                                   originalText, getAudioDataSize());
            } else {
                return String.format("TextToSpeechResult{success=false, error='%s'}", errorMessage);
            }
        }
    }
}