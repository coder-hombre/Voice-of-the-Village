package com.foogly.voiceofthevillage.integration;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks performance metrics for the villager communication system
 */
public class PerformanceMetrics {
    
    // Counters
    private final LongAdder totalTextInteractions = new LongAdder();
    private final LongAdder totalVoiceInteractions = new LongAdder();
    private final LongAdder successfulTextInteractions = new LongAdder();
    private final LongAdder successfulVoiceInteractions = new LongAdder();
    
    // Timing metrics
    private final AtomicLong totalTextProcessingTime = new AtomicLong(0);
    private final AtomicLong totalVoiceProcessingTime = new AtomicLong(0);
    private final AtomicLong maxTextProcessingTime = new AtomicLong(0);
    private final AtomicLong maxVoiceProcessingTime = new AtomicLong(0);
    
    // System start time
    private final long systemStartTime = System.currentTimeMillis();
    
    /**
     * Records a text interaction
     */
    public void recordTextInteraction(long processingTimeMs, boolean successful) {
        totalTextInteractions.increment();
        if (successful) {
            successfulTextInteractions.increment();
        }
        
        totalTextProcessingTime.addAndGet(processingTimeMs);
        updateMaxTime(maxTextProcessingTime, processingTimeMs);
    }
    
    /**
     * Records a voice interaction
     */
    public void recordVoiceInteraction(long processingTimeMs, boolean successful) {
        totalVoiceInteractions.increment();
        if (successful) {
            successfulVoiceInteractions.increment();
        }
        
        totalVoiceProcessingTime.addAndGet(processingTimeMs);
        updateMaxTime(maxVoiceProcessingTime, processingTimeMs);
    }
    
    /**
     * Updates maximum processing time atomically
     */
    private void updateMaxTime(AtomicLong maxTime, long newTime) {
        maxTime.updateAndGet(current -> Math.max(current, newTime));
    }
    
    /**
     * Gets a snapshot of current metrics
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            totalTextInteractions.sum(),
            totalVoiceInteractions.sum(),
            successfulTextInteractions.sum(),
            successfulVoiceInteractions.sum(),
            calculateAverageTime(totalTextProcessingTime.get(), totalTextInteractions.sum()),
            calculateAverageTime(totalVoiceProcessingTime.get(), totalVoiceInteractions.sum()),
            maxTextProcessingTime.get(),
            maxVoiceProcessingTime.get(),
            System.currentTimeMillis() - systemStartTime
        );
    }
    
    /**
     * Calculates average processing time
     */
    private double calculateAverageTime(long totalTime, long totalInteractions) {
        return totalInteractions > 0 ? (double) totalTime / totalInteractions : 0.0;
    }
    
    /**
     * Resets all metrics
     */
    public void reset() {
        totalTextInteractions.reset();
        totalVoiceInteractions.reset();
        successfulTextInteractions.reset();
        successfulVoiceInteractions.reset();
        totalTextProcessingTime.set(0);
        totalVoiceProcessingTime.set(0);
        maxTextProcessingTime.set(0);
        maxVoiceProcessingTime.set(0);
    }
    
    /**
     * Immutable snapshot of metrics at a point in time
     */
    public static class MetricsSnapshot {
        private final long totalTextInteractions;
        private final long totalVoiceInteractions;
        private final long successfulTextInteractions;
        private final long successfulVoiceInteractions;
        private final double averageTextProcessingTime;
        private final double averageVoiceProcessingTime;
        private final long maxTextProcessingTime;
        private final long maxVoiceProcessingTime;
        private final long systemUptimeMs;
        
        public MetricsSnapshot(long totalTextInteractions, long totalVoiceInteractions,
                             long successfulTextInteractions, long successfulVoiceInteractions,
                             double averageTextProcessingTime, double averageVoiceProcessingTime,
                             long maxTextProcessingTime, long maxVoiceProcessingTime,
                             long systemUptimeMs) {
            this.totalTextInteractions = totalTextInteractions;
            this.totalVoiceInteractions = totalVoiceInteractions;
            this.successfulTextInteractions = successfulTextInteractions;
            this.successfulVoiceInteractions = successfulVoiceInteractions;
            this.averageTextProcessingTime = averageTextProcessingTime;
            this.averageVoiceProcessingTime = averageVoiceProcessingTime;
            this.maxTextProcessingTime = maxTextProcessingTime;
            this.maxVoiceProcessingTime = maxVoiceProcessingTime;
            this.systemUptimeMs = systemUptimeMs;
        }
        
        // Getters
        public long getTotalTextInteractions() { return totalTextInteractions; }
        public long getTotalVoiceInteractions() { return totalVoiceInteractions; }
        public long getSuccessfulTextInteractions() { return successfulTextInteractions; }
        public long getSuccessfulVoiceInteractions() { return successfulVoiceInteractions; }
        public double getAverageTextProcessingTime() { return averageTextProcessingTime; }
        public double getAverageVoiceProcessingTime() { return averageVoiceProcessingTime; }
        public long getMaxTextProcessingTime() { return maxTextProcessingTime; }
        public long getMaxVoiceProcessingTime() { return maxVoiceProcessingTime; }
        public long getSystemUptimeMs() { return systemUptimeMs; }
        
        public double getTextSuccessRate() {
            return totalTextInteractions > 0 ? 
                (double) successfulTextInteractions / totalTextInteractions * 100.0 : 0.0;
        }
        
        public double getVoiceSuccessRate() {
            return totalVoiceInteractions > 0 ? 
                (double) successfulVoiceInteractions / totalVoiceInteractions * 100.0 : 0.0;
        }
        
        public long getTotalInteractions() {
            return totalTextInteractions + totalVoiceInteractions;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Metrics[Text: %d/%d (%.1f%%), Voice: %d/%d (%.1f%%), AvgTime: %.1fms/%.1fms, MaxTime: %dms/%dms, Uptime: %ds]",
                successfulTextInteractions, totalTextInteractions, getTextSuccessRate(),
                successfulVoiceInteractions, totalVoiceInteractions, getVoiceSuccessRate(),
                averageTextProcessingTime, averageVoiceProcessingTime,
                maxTextProcessingTime, maxVoiceProcessingTime,
                systemUptimeMs / 1000
            );
        }
    }
}