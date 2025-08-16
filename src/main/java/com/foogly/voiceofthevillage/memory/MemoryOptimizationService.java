package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.InteractionMemory;
import com.foogly.voiceofthevillage.data.MemoryManager;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.VillagerDataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Optimizes memory usage by managing villager data storage and cleanup.
 * Provides intelligent memory management to prevent excessive storage usage.
 */
public class MemoryOptimizationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryOptimizationService.class);
    
    private final MemoryManager memoryManager;
    private final ScheduledExecutorService scheduler;
    
    // Optimization thresholds
    private static final int MAX_MEMORIES_PER_VILLAGER = 100;
    private static final int MAX_TOTAL_MEMORIES = 10000;
    private static final long MAX_MEMORY_AGE_DAYS = 90; // Absolute maximum age
    private static final int OPTIMIZATION_INTERVAL_HOURS = 6;
    
    // Statistics
    private long lastOptimizationTime = 0;
    private int totalMemoriesOptimized = 0;
    private int totalVillagersOptimized = 0;
    private String lastOptimizationResult = "";

    /**
     * Creates a new memory optimization service.
     *
     * @param memoryManager The memory manager instance
     */
    public MemoryOptimizationService(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MemoryOptimization");
            t.setDaemon(true);
            return t;
        });
        
        scheduleOptimization();
    }

    /**
     * Schedules automatic memory optimization.
     */
    private void scheduleOptimization() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performOptimization();
            } catch (Exception e) {
                LOGGER.error("Error during memory optimization", e);
            }
        }, OPTIMIZATION_INTERVAL_HOURS, OPTIMIZATION_INTERVAL_HOURS, TimeUnit.HOURS);
        
        LOGGER.info("Scheduled memory optimization every {} hours", OPTIMIZATION_INTERVAL_HOURS);
    }

    /**
     * Performs comprehensive memory optimization.
     */
    public void performOptimization() {
        long startTime = System.currentTimeMillis();
        int totalOptimized = 0;
        int villagersProcessed = 0;
        
        try {
            LOGGER.info("Starting memory optimization");
            
            // Get all villager data
            Map<UUID, VillagerData> allVillagerData = VillagerDataManager.getAllVillagerData();
            
            // Calculate total memory usage
            int totalMemories = allVillagerData.values().stream()
                .mapToInt(data -> data.getMemories().size())
                .sum();
            
            LOGGER.info("Current memory usage: {} memories across {} villagers", 
                       totalMemories, allVillagerData.size());
            
            // Perform different optimization strategies based on usage
            if (totalMemories > MAX_TOTAL_MEMORIES) {
                totalOptimized += performAggressiveOptimization(allVillagerData);
            } else {
                totalOptimized += performStandardOptimization(allVillagerData);
            }
            
            villagersProcessed = allVillagerData.size();
            
            // Update statistics
            lastOptimizationTime = System.currentTimeMillis();
            totalMemoriesOptimized += totalOptimized;
            totalVillagersOptimized += villagersProcessed;
            
            long duration = System.currentTimeMillis() - startTime;
            lastOptimizationResult = String.format(
                "Optimized %d memories from %d villagers in %dms", 
                totalOptimized, villagersProcessed, duration
            );
            
            LOGGER.info("Memory optimization completed: {}", lastOptimizationResult);
            
        } catch (Exception e) {
            LOGGER.error("Memory optimization failed", e);
            lastOptimizationResult = "Failed: " + e.getMessage();
        }
    }

    /**
     * Performs standard memory optimization.
     *
     * @param allVillagerData Map of all villager data
     * @return Number of memories optimized
     */
    private int performStandardOptimization(Map<UUID, VillagerData> allVillagerData) {
        int totalOptimized = 0;
        int retentionDays = VoiceConfig.MEMORY_RETENTION_DAYS.get();
        long currentGameDay = getCurrentGameDay();
        
        for (VillagerData villagerData : allVillagerData.values()) {
            try {
                // Standard cleanup based on configured retention
                int cleaned = villagerData.cleanupExpiredMemories(currentGameDay, retentionDays);
                
                // Additional optimization for villagers with too many memories
                if (villagerData.getMemories().size() > MAX_MEMORIES_PER_VILLAGER) {
                    cleaned += optimizeVillagerMemories(villagerData, MAX_MEMORIES_PER_VILLAGER);
                }
                
                if (cleaned > 0) {
                    VillagerDataManager.updateVillagerData(villagerData);
                    totalOptimized += cleaned;
                }
                
            } catch (Exception e) {
                LOGGER.warn("Failed to optimize memories for villager {}: {}", 
                           villagerData.getVillagerId(), e.getMessage());
            }
        }
        
        return totalOptimized;
    }

    /**
     * Performs aggressive memory optimization when usage is too high.
     *
     * @param allVillagerData Map of all villager data
     * @return Number of memories optimized
     */
    private int performAggressiveOptimization(Map<UUID, VillagerData> allVillagerData) {
        int totalOptimized = 0;
        long currentGameDay = getCurrentGameDay();
        
        LOGGER.warn("Performing aggressive memory optimization due to high usage");
        
        // Sort villagers by memory usage (highest first)
        List<VillagerData> sortedVillagers = allVillagerData.values().stream()
            .sorted((a, b) -> Integer.compare(b.getMemories().size(), a.getMemories().size()))
            .collect(Collectors.toList());
        
        for (VillagerData villagerData : sortedVillagers) {
            try {
                int memoryCount = villagerData.getMemories().size();
                
                if (memoryCount > 50) {
                    // Aggressive cleanup - keep only 30 most recent memories
                    int cleaned = optimizeVillagerMemories(villagerData, 30);
                    totalOptimized += cleaned;
                } else if (memoryCount > 20) {
                    // Moderate cleanup - use shorter retention period
                    int shorterRetention = Math.max(7, VoiceConfig.MEMORY_RETENTION_DAYS.get() / 2);
                    int cleaned = villagerData.cleanupExpiredMemories(currentGameDay, shorterRetention);
                    totalOptimized += cleaned;
                } else {
                    // Standard cleanup for low-usage villagers
                    int cleaned = villagerData.cleanupExpiredMemories(currentGameDay, 
                                                                     VoiceConfig.MEMORY_RETENTION_DAYS.get());
                    totalOptimized += cleaned;
                }
                
                VillagerDataManager.updateVillagerData(villagerData);
                
            } catch (Exception e) {
                LOGGER.warn("Failed to aggressively optimize memories for villager {}: {}", 
                           villagerData.getVillagerId(), e.getMessage());
            }
        }
        
        return totalOptimized;
    }

    /**
     * Optimizes memories for a specific villager by keeping only the most important ones.
     *
     * @param villagerData The villager data to optimize
     * @param maxMemories Maximum number of memories to keep
     * @return Number of memories removed
     */
    private int optimizeVillagerMemories(VillagerData villagerData, int maxMemories) {
        List<InteractionMemory> memories = villagerData.getMemories();
        
        if (memories.size() <= maxMemories) {
            return 0;
        }
        
        // Sort memories by importance (most recent and diverse interactions first)
        List<InteractionMemory> sortedMemories = memories.stream()
            .sorted(this::compareMemoryImportance)
            .collect(Collectors.toList());
        
        // Keep only the most important memories
        List<InteractionMemory> optimizedMemories = sortedMemories.stream()
            .limit(maxMemories)
            .collect(Collectors.toList());
        
        int removed = memories.size() - optimizedMemories.size();
        
        // Update the villager data with optimized memories
        villagerData.setMemories(optimizedMemories);
        
        LOGGER.debug("Optimized villager {} memories: kept {} out of {}", 
                    villagerData.getVillagerId(), optimizedMemories.size(), memories.size());
        
        return removed;
    }

    /**
     * Compares memory importance for optimization purposes.
     * More recent and diverse interactions are considered more important.
     *
     * @param a First memory
     * @param b Second memory
     * @return Comparison result (negative if a is more important)
     */
    private int compareMemoryImportance(InteractionMemory a, InteractionMemory b) {
        // Recent memories are more important
        long timeDiff = b.getTimestamp() - a.getTimestamp();
        if (Math.abs(timeDiff) > TimeUnit.DAYS.toMillis(1)) {
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        }
        
        // Voice interactions are slightly more important than text
        if (a.getInteractionType() != b.getInteractionType()) {
            if (a.getInteractionType().name().contains("VOICE")) return -1;
            if (b.getInteractionType().name().contains("VOICE")) return 1;
        }
        
        // Longer messages might be more important
        int lengthDiff = b.getPlayerMessage().length() - a.getPlayerMessage().length();
        if (Math.abs(lengthDiff) > 20) {
            return lengthDiff;
        }
        
        // Default to timestamp
        return Long.compare(b.getTimestamp(), a.getTimestamp());
    }

    /**
     * Performs emergency cleanup when memory usage is critically high.
     *
     * @return Number of memories cleaned up
     */
    public int performEmergencyCleanup() {
        LOGGER.warn("Performing emergency memory cleanup");
        
        try {
            // Force cleanup of very old memories
            return memoryManager.forceCleanupOlderThan((int) MAX_MEMORY_AGE_DAYS);
        } catch (IOException e) {
            LOGGER.error("Emergency cleanup failed", e);
            return 0;
        }
    }

    /**
     * Gets memory usage statistics.
     *
     * @return Memory usage statistics
     */
    public MemoryUsageStatistics getMemoryUsageStatistics() {
        try {
            Map<UUID, VillagerData> allVillagerData = VillagerDataManager.getAllVillagerData();
            
            int totalVillagers = allVillagerData.size();
            int totalMemories = 0;
            int villagersWithMemories = 0;
            int maxMemoriesPerVillager = 0;
            long oldestMemoryTimestamp = System.currentTimeMillis();
            long newestMemoryTimestamp = 0;
            
            Map<String, Integer> interactionTypeCounts = new HashMap<>();
            
            for (VillagerData villagerData : allVillagerData.values()) {
                List<InteractionMemory> memories = villagerData.getMemories();
                int memoryCount = memories.size();
                
                if (memoryCount > 0) {
                    villagersWithMemories++;
                    totalMemories += memoryCount;
                    maxMemoriesPerVillager = Math.max(maxMemoriesPerVillager, memoryCount);
                    
                    for (InteractionMemory memory : memories) {
                        oldestMemoryTimestamp = Math.min(oldestMemoryTimestamp, memory.getTimestamp());
                        newestMemoryTimestamp = Math.max(newestMemoryTimestamp, memory.getTimestamp());
                        
                        String type = memory.getInteractionType().toString();
                        interactionTypeCounts.merge(type, 1, Integer::sum);
                    }
                }
            }
            
            double averageMemoriesPerVillager = villagersWithMemories > 0 
                ? (double) totalMemories / villagersWithMemories : 0;
            
            return new MemoryUsageStatistics(
                totalVillagers,
                villagersWithMemories,
                totalMemories,
                averageMemoriesPerVillager,
                maxMemoriesPerVillager,
                oldestMemoryTimestamp,
                newestMemoryTimestamp,
                interactionTypeCounts,
                lastOptimizationTime,
                totalMemoriesOptimized,
                totalVillagersOptimized,
                lastOptimizationResult
            );
            
        } catch (Exception e) {
            LOGGER.error("Failed to get memory usage statistics", e);
            return new MemoryUsageStatistics(0, 0, 0, 0, 0, 0, 0, 
                                           new HashMap<>(), 0, 0, 0, "Error: " + e.getMessage());
        }
    }

    /**
     * Checks if memory usage is approaching critical levels.
     *
     * @return true if memory usage is high
     */
    public boolean isMemoryUsageHigh() {
        MemoryUsageStatistics stats = getMemoryUsageStatistics();
        return stats.getTotalMemories() > MAX_TOTAL_MEMORIES * 0.8 || 
               stats.getMaxMemoriesPerVillager() > MAX_MEMORIES_PER_VILLAGER * 0.8;
    }

    /**
     * Gets the current game day for memory calculations.
     *
     * @return Current game day
     */
    private long getCurrentGameDay() {
        // TODO: Replace with actual Minecraft world day calculation
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000);
    }

    /**
     * Shuts down the optimization service.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Memory optimization service shutdown completed");
    }

    /**
     * Statistics about memory usage and optimization.
     */
    public static class MemoryUsageStatistics {
        private final int totalVillagers;
        private final int villagersWithMemories;
        private final int totalMemories;
        private final double averageMemoriesPerVillager;
        private final int maxMemoriesPerVillager;
        private final long oldestMemoryTimestamp;
        private final long newestMemoryTimestamp;
        private final Map<String, Integer> interactionTypeCounts;
        private final long lastOptimizationTime;
        private final int totalMemoriesOptimized;
        private final int totalVillagersOptimized;
        private final String lastOptimizationResult;

        public MemoryUsageStatistics(int totalVillagers, int villagersWithMemories, int totalMemories,
                                   double averageMemoriesPerVillager, int maxMemoriesPerVillager,
                                   long oldestMemoryTimestamp, long newestMemoryTimestamp,
                                   Map<String, Integer> interactionTypeCounts,
                                   long lastOptimizationTime, int totalMemoriesOptimized,
                                   int totalVillagersOptimized, String lastOptimizationResult) {
            this.totalVillagers = totalVillagers;
            this.villagersWithMemories = villagersWithMemories;
            this.totalMemories = totalMemories;
            this.averageMemoriesPerVillager = averageMemoriesPerVillager;
            this.maxMemoriesPerVillager = maxMemoriesPerVillager;
            this.oldestMemoryTimestamp = oldestMemoryTimestamp;
            this.newestMemoryTimestamp = newestMemoryTimestamp;
            this.interactionTypeCounts = new HashMap<>(interactionTypeCounts);
            this.lastOptimizationTime = lastOptimizationTime;
            this.totalMemoriesOptimized = totalMemoriesOptimized;
            this.totalVillagersOptimized = totalVillagersOptimized;
            this.lastOptimizationResult = lastOptimizationResult;
        }

        // Getters
        public int getTotalVillagers() { return totalVillagers; }
        public int getVillagersWithMemories() { return villagersWithMemories; }
        public int getTotalMemories() { return totalMemories; }
        public double getAverageMemoriesPerVillager() { return averageMemoriesPerVillager; }
        public int getMaxMemoriesPerVillager() { return maxMemoriesPerVillager; }
        public long getOldestMemoryTimestamp() { return oldestMemoryTimestamp; }
        public long getNewestMemoryTimestamp() { return newestMemoryTimestamp; }
        public Map<String, Integer> getInteractionTypeCounts() { return new HashMap<>(interactionTypeCounts); }
        public long getLastOptimizationTime() { return lastOptimizationTime; }
        public int getTotalMemoriesOptimized() { return totalMemoriesOptimized; }
        public int getTotalVillagersOptimized() { return totalVillagersOptimized; }
        public String getLastOptimizationResult() { return lastOptimizationResult; }
        
        public boolean isMemoryUsageHigh() {
            return totalMemories > MAX_TOTAL_MEMORIES * 0.8;
        }
        
        public boolean isMemoryUsageCritical() {
            return totalMemories > MAX_TOTAL_MEMORIES;
        }
        
        public double getMemoryUsagePercentage() {
            return (double) totalMemories / MAX_TOTAL_MEMORIES * 100;
        }
    }
}