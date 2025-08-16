package com.foogly.voiceofthevillage.data;

import com.foogly.voiceofthevillage.config.VoiceConfig;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages villager memory retention and cleanup based on configured days.
 * Handles automatic cleanup of expired memories and provides memory statistics.
 */
public class MemoryManager {
    private static final Logger LOGGER = Logger.getLogger(MemoryManager.class.getName());
    
    private final DataPersistence dataPersistence;
    private final ScheduledExecutorService scheduler;
    private final Object cleanupLock = new Object();
    
    // Statistics
    private long lastCleanupTime = 0;
    private int lastCleanupRemovedCount = 0;
    private int totalMemoriesProcessed = 0;
    private int totalMemoriesRemoved = 0;

    /**
     * Creates a new MemoryManager with the specified data persistence.
     *
     * @param dataPersistence The data persistence instance to use
     */
    public MemoryManager(DataPersistence dataPersistence) {
        this.dataPersistence = dataPersistence;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VillagerMemoryCleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule automatic cleanup every hour
        scheduleAutomaticCleanup();
    }

    /**
     * Schedules automatic memory cleanup to run periodically.
     */
    private void scheduleAutomaticCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performAutomaticCleanup();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during automatic memory cleanup", e);
            }
        }, 1, 1, TimeUnit.HOURS);
        
        LOGGER.info("Scheduled automatic memory cleanup every hour");
    }

    /**
     * Performs automatic cleanup of expired memories for all villagers.
     */
    public void performAutomaticCleanup() {
        performAutomaticCleanup(getConfiguredRetentionDays());
    }

    /**
     * Performs automatic cleanup of expired memories for all villagers with specified retention days.
     *
     * @param retentionDays Number of days to retain memories
     */
    public void performAutomaticCleanup(int retentionDays) {
        synchronized (cleanupLock) {
            try {
                long currentGameDay = getCurrentGameDay();
                
                Set<UUID> villagerIds = dataPersistence.getAllVillagerIds();
                int totalRemoved = 0;
                int villagersProcessed = 0;
                
                LOGGER.info("Starting automatic memory cleanup for " + villagerIds.size() + " villagers");
                
                for (UUID villagerId : villagerIds) {
                    try {
                        VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
                        if (villagerData != null) {
                            int removed = villagerData.cleanupExpiredMemories(currentGameDay, retentionDays);
                            if (removed > 0) {
                                dataPersistence.saveVillagerData(villagerData);
                                totalRemoved += removed;
                                LOGGER.fine("Cleaned up " + removed + " expired memories for villager " + villagerId);
                            }
                            villagersProcessed++;
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to cleanup memories for villager " + villagerId, e);
                    }
                }
                
                // Update statistics
                lastCleanupTime = System.currentTimeMillis();
                lastCleanupRemovedCount = totalRemoved;
                totalMemoriesProcessed += villagersProcessed;
                totalMemoriesRemoved += totalRemoved;
                
                if (totalRemoved > 0) {
                    LOGGER.info("Memory cleanup completed: removed " + totalRemoved + 
                               " expired memories from " + villagersProcessed + " villagers");
                } else {
                    LOGGER.fine("Memory cleanup completed: no expired memories found");
                }
                
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to perform automatic memory cleanup", e);
            }
        }
    }

    /**
     * Cleans up expired memories for a specific villager.
     *
     * @param villagerId The UUID of the villager
     * @return The number of memories removed
     * @throws IOException If the operation fails
     */
    public int cleanupVillagerMemories(UUID villagerId) throws IOException {
        return cleanupVillagerMemories(villagerId, getConfiguredRetentionDays());
    }

    /**
     * Cleans up expired memories for a specific villager with specified retention days.
     *
     * @param villagerId The UUID of the villager
     * @param retentionDays Number of days to retain memories
     * @return The number of memories removed
     * @throws IOException If the operation fails
     */
    public int cleanupVillagerMemories(UUID villagerId, int retentionDays) throws IOException {
        if (villagerId == null) {
            throw new IllegalArgumentException("Villager ID cannot be null");
        }

        synchronized (cleanupLock) {
            VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
            if (villagerData == null) {
                return 0;
            }

            long currentGameDay = getCurrentGameDay();
            
            int removed = villagerData.cleanupExpiredMemories(currentGameDay, retentionDays);
            
            if (removed > 0) {
                dataPersistence.saveVillagerData(villagerData);
                totalMemoriesRemoved += removed;
                LOGGER.fine("Cleaned up " + removed + " expired memories for villager " + villagerId);
            }
            
            return removed;
        }
    }

    /**
     * Forces cleanup of all memories older than the specified number of days.
     *
     * @param maxAgeDays Maximum age of memories to keep
     * @return The total number of memories removed
     * @throws IOException If the operation fails
     */
    public int forceCleanupOlderThan(int maxAgeDays) throws IOException {
        if (maxAgeDays < 0) {
            throw new IllegalArgumentException("Max age days cannot be negative");
        }

        synchronized (cleanupLock) {
            long currentGameDay = getCurrentGameDay();
            Set<UUID> villagerIds = dataPersistence.getAllVillagerIds();
            int totalRemoved = 0;
            
            LOGGER.info("Force cleaning memories older than " + maxAgeDays + " days for " + villagerIds.size() + " villagers");
            
            for (UUID villagerId : villagerIds) {
                try {
                    VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
                    if (villagerData != null) {
                        int removed = villagerData.cleanupExpiredMemories(currentGameDay, maxAgeDays);
                        if (removed > 0) {
                            dataPersistence.saveVillagerData(villagerData);
                            totalRemoved += removed;
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to force cleanup memories for villager " + villagerId, e);
                }
            }
            
            totalMemoriesRemoved += totalRemoved;
            LOGGER.info("Force cleanup completed: removed " + totalRemoved + " memories");
            
            return totalRemoved;
        }
    }

    /**
     * Gets memory statistics for a specific villager.
     *
     * @param villagerId The UUID of the villager
     * @return Memory statistics, or null if villager not found
     * @throws IOException If the operation fails
     */
    public MemoryStatistics getVillagerMemoryStatistics(UUID villagerId) throws IOException {
        return getVillagerMemoryStatistics(villagerId, getConfiguredRetentionDays());
    }

    /**
     * Gets memory statistics for a specific villager with specified retention days.
     *
     * @param villagerId The UUID of the villager
     * @param retentionDays Number of days to retain memories
     * @return Memory statistics, or null if villager not found
     * @throws IOException If the operation fails
     */
    public MemoryStatistics getVillagerMemoryStatistics(UUID villagerId, int retentionDays) throws IOException {
        if (villagerId == null) {
            return null;
        }

        VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
        if (villagerData == null) {
            return null;
        }

        List<InteractionMemory> memories = villagerData.getMemories();
        long currentGameDay = getCurrentGameDay();
        
        int totalMemories = memories.size();
        int expiredMemories = 0;
        long oldestMemoryDay = currentGameDay;
        long newestMemoryDay = 0;
        
        Map<InteractionType, Integer> typeCount = new HashMap<>();
        Map<UUID, Integer> playerCount = new HashMap<>();
        
        for (InteractionMemory memory : memories) {
            if (memory.isExpired(currentGameDay, retentionDays)) {
                expiredMemories++;
            }
            
            oldestMemoryDay = Math.min(oldestMemoryDay, memory.getGameDay());
            newestMemoryDay = Math.max(newestMemoryDay, memory.getGameDay());
            
            typeCount.merge(memory.getInteractionType(), 1, Integer::sum);
            playerCount.merge(memory.getPlayerId(), 1, Integer::sum);
        }
        
        return new MemoryStatistics(
            villagerId,
            totalMemories,
            expiredMemories,
            oldestMemoryDay,
            newestMemoryDay,
            typeCount,
            playerCount.size()
        );
    }

    /**
     * Gets overall memory statistics for all villagers.
     *
     * @return Overall memory statistics
     * @throws IOException If the operation fails
     */
    public OverallMemoryStatistics getOverallStatistics() throws IOException {
        return getOverallStatistics(getConfiguredRetentionDays());
    }

    /**
     * Gets overall memory statistics for all villagers with specified retention days.
     *
     * @param retentionDays Number of days to retain memories
     * @return Overall memory statistics
     * @throws IOException If the operation fails
     */
    public OverallMemoryStatistics getOverallStatistics(int retentionDays) throws IOException {
        Set<UUID> villagerIds = dataPersistence.getAllVillagerIds();
        int totalVillagers = villagerIds.size();
        int totalMemories = 0;
        int totalExpiredMemories = 0;
        long currentGameDay = getCurrentGameDay();
        
        for (UUID villagerId : villagerIds) {
            try {
                VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
                if (villagerData != null) {
                    List<InteractionMemory> memories = villagerData.getMemories();
                    totalMemories += memories.size();
                    
                    for (InteractionMemory memory : memories) {
                        if (memory.isExpired(currentGameDay, retentionDays)) {
                            totalExpiredMemories++;
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to get statistics for villager " + villagerId, e);
            }
        }
        
        return new OverallMemoryStatistics(
            totalVillagers,
            totalMemories,
            totalExpiredMemories,
            lastCleanupTime,
            lastCleanupRemovedCount,
            totalMemoriesProcessed,
            totalMemoriesRemoved
        );
    }

    /**
     * Gets the configured retention days from the config.
     * Uses a default value if config is not available (e.g., during testing).
     *
     * @return The configured retention days
     */
    private int getConfiguredRetentionDays() {
        try {
            return VoiceConfig.MEMORY_RETENTION_DAYS.get();
        } catch (Exception e) {
            // Config not available (e.g., during testing), use default
            return 30;
        }
    }

    /**
     * Gets the current game day. This is a placeholder implementation.
     * In the actual mod, this would get the current day from the Minecraft world.
     *
     * @return The current game day
     */
    protected long getCurrentGameDay() {
        // TODO: Replace with actual Minecraft world day calculation
        // For now, use a simple calculation based on system time
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000); // Days since epoch
    }

    /**
     * Shuts down the memory manager and stops scheduled tasks.
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
        LOGGER.info("Memory manager shutdown completed");
    }

    /**
     * Statistics for a specific villager's memories.
     */
    public static class MemoryStatistics {
        private final UUID villagerId;
        private final int totalMemories;
        private final int expiredMemories;
        private final long oldestMemoryDay;
        private final long newestMemoryDay;
        private final Map<InteractionType, Integer> memoryTypeCount;
        private final int uniquePlayerCount;

        public MemoryStatistics(UUID villagerId, int totalMemories, int expiredMemories,
                               long oldestMemoryDay, long newestMemoryDay,
                               Map<InteractionType, Integer> memoryTypeCount, int uniquePlayerCount) {
            this.villagerId = villagerId;
            this.totalMemories = totalMemories;
            this.expiredMemories = expiredMemories;
            this.oldestMemoryDay = oldestMemoryDay;
            this.newestMemoryDay = newestMemoryDay;
            this.memoryTypeCount = new HashMap<>(memoryTypeCount);
            this.uniquePlayerCount = uniquePlayerCount;
        }

        // Getters
        public UUID getVillagerId() { return villagerId; }
        public int getTotalMemories() { return totalMemories; }
        public int getExpiredMemories() { return expiredMemories; }
        public int getActiveMemories() { return totalMemories - expiredMemories; }
        public long getOldestMemoryDay() { return oldestMemoryDay; }
        public long getNewestMemoryDay() { return newestMemoryDay; }
        public Map<InteractionType, Integer> getMemoryTypeCount() { return new HashMap<>(memoryTypeCount); }
        public int getUniquePlayerCount() { return uniquePlayerCount; }
    }

    /**
     * Overall statistics for all villager memories.
     */
    public static class OverallMemoryStatistics {
        private final int totalVillagers;
        private final int totalMemories;
        private final int totalExpiredMemories;
        private final long lastCleanupTime;
        private final int lastCleanupRemovedCount;
        private final int totalMemoriesProcessed;
        private final int totalMemoriesRemoved;

        public OverallMemoryStatistics(int totalVillagers, int totalMemories, int totalExpiredMemories,
                                     long lastCleanupTime, int lastCleanupRemovedCount,
                                     int totalMemoriesProcessed, int totalMemoriesRemoved) {
            this.totalVillagers = totalVillagers;
            this.totalMemories = totalMemories;
            this.totalExpiredMemories = totalExpiredMemories;
            this.lastCleanupTime = lastCleanupTime;
            this.lastCleanupRemovedCount = lastCleanupRemovedCount;
            this.totalMemoriesProcessed = totalMemoriesProcessed;
            this.totalMemoriesRemoved = totalMemoriesRemoved;
        }

        // Getters
        public int getTotalVillagers() { return totalVillagers; }
        public int getTotalMemories() { return totalMemories; }
        public int getTotalExpiredMemories() { return totalExpiredMemories; }
        public int getTotalActiveMemories() { return totalMemories - totalExpiredMemories; }
        public long getLastCleanupTime() { return lastCleanupTime; }
        public int getLastCleanupRemovedCount() { return lastCleanupRemovedCount; }
        public int getTotalMemoriesProcessed() { return totalMemoriesProcessed; }
        public int getTotalMemoriesRemoved() { return totalMemoriesRemoved; }
    }
}