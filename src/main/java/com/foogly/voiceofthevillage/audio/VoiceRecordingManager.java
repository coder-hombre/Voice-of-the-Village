package com.foogly.voiceofthevillage.audio;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.network.VoiceInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages voice recording functionality for simple mode communication.
 * Handles audio capture, processing, and sending to server.
 */
public class VoiceRecordingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceRecordingManager.class);
    
    private static final VoiceRecordingManager INSTANCE = new VoiceRecordingManager();
    
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // Audio processing components
    private final AudioCaptureManager audioCaptureManager;
    private final SpeechToTextProcessor speechProcessor;
    
    // Recording state
    private UUID currentVillagerUUID;
    private VoiceRecordingCallback callback;
    private CompletableFuture<Void> recordingTask;
    private AudioCaptureManager.AudioRecording currentRecording;
    
    private VoiceRecordingManager() {
        this.audioCaptureManager = new AudioCaptureManager();
        this.speechProcessor = new SpeechToTextProcessor();
    }
    
    public static VoiceRecordingManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Starts voice recording for the specified villager.
     */
    public boolean startRecording(UUID villagerUUID, VoiceRecordingCallback callback) {
        if (!VoiceConfig.ENABLE_VOICE_INPUT.get()) {
            LOGGER.warn("Voice input is disabled in configuration");
            return false;
        }
        
        if (isRecording.get() || isProcessing.get()) {
            LOGGER.warn("Already recording or processing voice input");
            return false;
        }
        
        if (!audioCaptureManager.isAudioCaptureAvailable()) {
            LOGGER.warn("Audio capture not available on this system");
            if (callback != null) {
                callback.onRecordingError(Component.translatable("error.voiceofthevillage.no_microphone"));
            }
            return false;
        }
        
        this.currentVillagerUUID = villagerUUID;
        this.callback = callback;
        
        isRecording.set(true);
        
        // Start recording task with actual audio capture
        recordingTask = audioCaptureManager.startRecording(AudioFormat.DEFAULT_RECORDING_FORMAT)
            .thenAccept(recording -> {
                this.currentRecording = recording;
                if (callback != null) {
                    callback.onRecordingStopped();
                }
                // Automatically start processing when recording completes
                processRecordedAudio();
            })
            .exceptionally(throwable -> {
                LOGGER.error("Recording failed", throwable);
                isRecording.set(false);
                if (callback != null) {
                    callback.onRecordingError(Component.translatable("error.voiceofthevillage.recording_failed"));
                }
                cleanup();
                return null;
            });
        
        LOGGER.debug("Started voice recording for villager {}", villagerUUID);
        
        if (callback != null) {
            callback.onRecordingStarted();
        }
        
        return true;
    }
    
    /**
     * Stops voice recording and processes the audio.
     */
    public void stopRecording() {
        if (!isRecording.get()) {
            LOGGER.warn("No active recording to stop");
            return;
        }
        
        isRecording.set(false);
        
        // Stop the audio capture
        audioCaptureManager.stopRecording();
        
        LOGGER.debug("Stopped voice recording for villager {}", currentVillagerUUID);
    }
    

    
    /**
     * Processes the recorded audio and sends it to the server.
     */
    private void processRecordedAudio() {
        if (currentRecording == null) {
            LOGGER.warn("No recording available for processing");
            cleanup();
            return;
        }
        
        isProcessing.set(true);
        
        CompletableFuture.runAsync(() -> {
            try {
                // Check if recording has meaningful audio data
                if (!currentRecording.hasAudioData()) {
                    LOGGER.debug("Recording does not contain meaningful audio data");
                    if (callback != null) {
                        callback.onRecordingError(Component.translatable("error.voiceofthevillage.no_speech_detected"));
                    }
                    return;
                }
                
                LOGGER.debug("Processing {} bytes of audio data ({:.2f} seconds)", 
                           currentRecording.getDataSize(), currentRecording.getDuration());
                
                // Process audio with speech-to-text
                speechProcessor.processAudio(currentRecording.getAudioData(), currentRecording.getFormat())
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            String transcribedText = result.getText();
                            LOGGER.debug("Speech-to-text successful: '{}'", transcribedText);
                            
                            // Send voice packet with transcribed text
                            sendVoicePacket(transcribedText, currentRecording.getAudioData(), currentRecording.getFormat());
                            
                            if (callback != null) {
                                callback.onProcessingComplete(Component.literal("\"" + transcribedText + "\""));
                            }
                        } else {
                            LOGGER.warn("Speech-to-text failed: {}", result.getErrorMessage());
                            if (callback != null) {
                                callback.onRecordingError(Component.literal(result.getErrorMessage()));
                            }
                        }
                    })
                    .exceptionally(throwable -> {
                        LOGGER.error("Speech processing failed", throwable);
                        if (callback != null) {
                            callback.onRecordingError(Component.translatable("error.voiceofthevillage.processing_failed"));
                        }
                        return null;
                    })
                    .whenComplete((result, throwable) -> {
                        isProcessing.set(false);
                        cleanup();
                    });
                    
            } catch (Exception e) {
                LOGGER.error("Error processing recorded audio", e);
                if (callback != null) {
                    callback.onRecordingError(Component.translatable("error.voiceofthevillage.processing_failed"));
                }
                isProcessing.set(false);
                cleanup();
            }
        });
    }
    
    /**
     * Sends a voice input packet to the server.
     */
    private void sendVoicePacket(String transcribedText, byte[] audioData, AudioFormat format) {
        if (currentVillagerUUID == null) {
            LOGGER.error("No villager UUID set for voice packet");
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            LOGGER.error("No player available for voice packet");
            return;
        }
        
        // Create voice input packet with actual audio data
        VoiceInputPacket packet = new VoiceInputPacket(
            currentVillagerUUID,
            minecraft.player.getUUID(),
            minecraft.player.getName().getString(),
            audioData,
            format.getSampleRate(),
            System.currentTimeMillis()
        );
        
        PacketDistributor.sendToServer(packet);
        
        LOGGER.debug("Sent voice input packet for villager {}: '{}' ({} bytes audio)", 
                    currentVillagerUUID, transcribedText, audioData.length);
    }
    
    /**
     * Cleans up recording state.
     */
    private void cleanup() {
        currentVillagerUUID = null;
        callback = null;
        recordingTask = null;
        currentRecording = null;
    }
    
    /**
     * Checks if currently recording.
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Checks if currently processing audio.
     */
    public boolean isProcessing() {
        return isProcessing.get();
    }
    
    /**
     * Checks if voice recording is available.
     */
    public boolean isVoiceRecordingAvailable() {
        return VoiceConfig.ENABLE_VOICE_INPUT.get() && 
               !isRecording.get() && 
               !isProcessing.get();
    }
    
    /**
     * Callback interface for voice recording events.
     */
    public interface VoiceRecordingCallback {
        void onRecordingStarted();
        void onRecordingStopped();
        void onRecordingProgress();
        void onProcessingComplete(Component result);
        void onRecordingError(Component error);
    }
}