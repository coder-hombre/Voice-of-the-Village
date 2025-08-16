package com.foogly.voiceofthevillage.integration;

/**
 * Represents a response from the villager communication system
 */
public class CommunicationResponse {
    private final boolean success;
    private final String textResponse;
    private final byte[] audioResponse;
    private final VillagerCommunicationSystem.CommunicationType type;
    private final String errorMessage;
    private final long timestamp;
    
    private CommunicationResponse(boolean success, String textResponse, byte[] audioResponse, 
                                 VillagerCommunicationSystem.CommunicationType type, String errorMessage) {
        this.success = success;
        this.textResponse = textResponse;
        this.audioResponse = audioResponse;
        this.type = type;
        this.errorMessage = errorMessage;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Creates a successful response
     */
    public static CommunicationResponse success(String textResponse, byte[] audioResponse, 
                                              VillagerCommunicationSystem.CommunicationType type) {
        return new CommunicationResponse(true, textResponse, audioResponse, type, null);
    }
    
    /**
     * Creates an error response
     */
    public static CommunicationResponse error(String errorMessage) {
        return new CommunicationResponse(false, null, null, null, errorMessage);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getTextResponse() { return textResponse; }
    public byte[] getAudioResponse() { return audioResponse; }
    public VillagerCommunicationSystem.CommunicationType getType() { return type; }
    public String getErrorMessage() { return errorMessage; }
    public long getTimestamp() { return timestamp; }
    
    public boolean hasAudioResponse() {
        return audioResponse != null && audioResponse.length > 0;
    }
}