package com.foogly.voiceofthevillage.integration;

/**
 * Represents the current status of the villager communication system
 */
public class SystemStatus {
    private final boolean aiServiceConfigured;
    private final String aiProviderName;
    private final int activeConversations;
    private final PerformanceMetrics.MetricsSnapshot performanceMetrics;
    private final long timestamp;
    
    private SystemStatus(boolean aiServiceConfigured, String aiProviderName, 
                        int activeConversations, PerformanceMetrics.MetricsSnapshot performanceMetrics) {
        this.aiServiceConfigured = aiServiceConfigured;
        this.aiProviderName = aiProviderName;
        this.activeConversations = activeConversations;
        this.performanceMetrics = performanceMetrics;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean isAiServiceConfigured() { return aiServiceConfigured; }
    public String getAiProviderName() { return aiProviderName; }
    public int getActiveConversations() { return activeConversations; }
    public PerformanceMetrics.MetricsSnapshot getPerformanceMetrics() { return performanceMetrics; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isHealthy() {
        return aiServiceConfigured && 
               (performanceMetrics.getTotalInteractions() == 0 || 
                (performanceMetrics.getTextSuccessRate() > 80.0 && performanceMetrics.getVoiceSuccessRate() > 70.0));
    }
    
    @Override
    public String toString() {
        return String.format(
            "SystemStatus[AI: %s (%s), Conversations: %d, Healthy: %s, %s]",
            aiServiceConfigured ? "Configured" : "Not Configured",
            aiProviderName,
            activeConversations,
            isHealthy(),
            performanceMetrics.toString()
        );
    }
    
    public static class Builder {
        private boolean aiServiceConfigured;
        private String aiProviderName;
        private int activeConversations;
        private PerformanceMetrics.MetricsSnapshot performanceMetrics;
        
        public Builder aiServiceConfigured(boolean configured) {
            this.aiServiceConfigured = configured;
            return this;
        }
        
        public Builder aiProviderName(String providerName) {
            this.aiProviderName = providerName;
            return this;
        }
        
        public Builder activeConversations(int count) {
            this.activeConversations = count;
            return this;
        }
        
        public Builder performanceMetrics(PerformanceMetrics.MetricsSnapshot metrics) {
            this.performanceMetrics = metrics;
            return this;
        }
        
        public SystemStatus build() {
            return new SystemStatus(aiServiceConfigured, aiProviderName, activeConversations, performanceMetrics);
        }
    }
}