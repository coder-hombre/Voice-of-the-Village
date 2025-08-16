package com.foogly.voiceofthevillage.data;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import net.minecraft.world.entity.npc.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages villager data storage and retrieval.
 * Handles creation, persistence, and cleanup of villager data.
 */
public class VillagerDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerDataManager.class);
    
    // In-memory cache of villager data
    private static final Map<UUID, VillagerData> villagerDataCache = new ConcurrentHashMap<>();
    
    // Data persistence handler
    private static DataPersistence dataPersistence;
    
    // Memory manager for cleanup
    private static MemoryManager memoryManager;
    
    /**
     * Initializes the villager data manager with persistence and memory management.
     */
    public static void initialize() {
        dataPersistence = new DataPersistence();
        memoryManager = new MemoryManager(dataPersistence);
        
        // Load existing villager data from disk
        loadAllVillagerData();
        
        LOGGER.info("VillagerDataManager initialized");
    }
    
    /**
     * Gets villager data for a specific villager UUID.
     *
     * @param villagerId The UUID of the villager
     * @return VillagerData if found, null otherwise
     */
    public static VillagerData getVillagerData(UUID villagerId) {
        return villagerDataCache.get(villagerId);
    }
    
    /**
     * Gets or creates villager data for a villager entity.
     * If the villager doesn't have data, generates new data with name and personality.
     *
     * @param villager The villager entity
     * @return VillagerData for the villager
     */
    public static VillagerData getOrCreateVillagerData(Villager villager) {
        UUID villagerId = villager.getUUID();
        
        VillagerData data = villagerDataCache.get(villagerId);
        if (data == null) {
            // Generate new villager data
            data = createNewVillagerData(villagerId);
            villagerDataCache.put(villagerId, data);
            
            // Save to disk
            if (dataPersistence != null) {
                try {
                    dataPersistence.saveVillagerData(data);
                } catch (IOException e) {
                    LOGGER.error("Failed to save villager data for {}", villagerId, e);
                }
            }
            
            if (VoiceConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Created new villager data for {}: {}", villagerId, data.getEffectiveName());
            }
        }
        
        return data;
    }
    
    /**
     * Creates new villager data with generated name and personality.
     *
     * @param villagerId The UUID of the villager
     * @return New VillagerData instance
     */
    private static VillagerData createNewVillagerData(UUID villagerId) {
        // Generate random name and determine gender
        NameGenerator.VillagerNameData nameData = NameGenerator.generateRandomName();
        String name = nameData.getName();
        Gender gender = nameData.getGender();
        
        // Assign random personality
        PersonalityType personality = getRandomPersonality();
        
        return new VillagerData(villagerId, name, gender, personality);
    }
    
    /**
     * Gets a random personality type.
     *
     * @return A random PersonalityType
     */
    private static PersonalityType getRandomPersonality() {
        PersonalityType[] personalities = PersonalityType.values();
        int randomIndex = (int) (Math.random() * personalities.length);
        return personalities[randomIndex];
    }
    
    /**
     * Updates villager data in the cache and persists to disk.
     *
     * @param villagerData The villager data to update
     */
    public static void updateVillagerData(VillagerData villagerData) {
        if (villagerData == null || villagerData.getVillagerId() == null) {
            return;
        }
        
        villagerDataCache.put(villagerData.getVillagerId(), villagerData);
        
        // Save to disk
        if (dataPersistence != null) {
            try {
                dataPersistence.saveVillagerData(villagerData);
            } catch (IOException e) {
                LOGGER.error("Failed to save villager data for {}", villagerData.getVillagerId(), e);
            }
        }
    }
    
    /**
     * Removes villager data from cache and disk.
     *
     * @param villagerId The UUID of the villager to remove
     */
    public static void removeVillagerData(UUID villagerId) {
        villagerDataCache.remove(villagerId);
        
        if (dataPersistence != null) {
            try {
                dataPersistence.deleteVillagerData(villagerId);
            } catch (IOException e) {
                LOGGER.error("Failed to delete villager data for {}", villagerId, e);
            }
        }
        
        if (VoiceConfig.DEBUG_MODE.get()) {
            LOGGER.debug("Removed villager data for {}", villagerId);
        }
    }
    
    /**
     * Updates a villager's custom name from a name tag.
     *
     * @param villagerId The UUID of the villager
     * @param customName The new custom name
     */
    public static void updateVillagerCustomName(UUID villagerId, String customName) {
        VillagerData data = villagerDataCache.get(villagerId);
        if (data != null) {
            data.setCustomName(customName);
            
            // Update gender based on new name if it's a recognizable name
            if (customName != null && !customName.trim().isEmpty()) {
                Gender newGender = NameGenerator.detectGender(customName);
                if (newGender != Gender.UNKNOWN) {
                    data.setGender(newGender);
                }
            }
            
            updateVillagerData(data);
            
            if (VoiceConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Updated villager {} custom name to: {}", villagerId, customName);
            }
        }
    }
    
    /**
     * Gets all cached villager data.
     *
     * @return Map of villager UUIDs to their data
     */
    public static Map<UUID, VillagerData> getAllVillagerData() {
        return new ConcurrentHashMap<>(villagerDataCache);
    }
    
    /**
     * Performs memory cleanup for all villagers based on configured retention period.
     *
     * @param currentGameDay The current game day
     * @return Total number of memories cleaned up
     */
    public static int performMemoryCleanup(long currentGameDay) {
        int totalCleaned = 0;
        int retentionDays = VoiceConfig.MEMORY_RETENTION_DAYS.get();
        
        for (VillagerData data : villagerDataCache.values()) {
            int cleaned = data.cleanupExpiredMemories(currentGameDay, retentionDays);
            totalCleaned += cleaned;
            
            if (cleaned > 0) {
                updateVillagerData(data);
            }
        }
        
        if (totalCleaned > 0 && VoiceConfig.DEBUG_MODE.get()) {
            LOGGER.debug("Cleaned up {} expired memories", totalCleaned);
        }
        
        return totalCleaned;
    }
    
    /**
     * Loads all villager data from disk into cache.
     */
    private static void loadAllVillagerData() {
        if (dataPersistence == null) {
            return;
        }
        
        try {
            Map<UUID, VillagerData> loadedData = dataPersistence.loadAllVillagerData();
            villagerDataCache.putAll(loadedData);
            
            LOGGER.info("Loaded {} villager data entries from disk", loadedData.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load villager data from disk", e);
        }
    }
    
    /**
     * Saves all cached villager data to disk.
     */
    public static void saveAllVillagerData() {
        if (dataPersistence == null) {
            return;
        }
        
        try {
            for (VillagerData data : villagerDataCache.values()) {
                dataPersistence.saveVillagerData(data);
            }
            
            if (VoiceConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Saved {} villager data entries to disk", villagerDataCache.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save villager data to disk", e);
        }
    }
    
    /**
     * Clears all cached villager data. Used for cleanup on mod shutdown.
     */
    public static void clearCache() {
        villagerDataCache.clear();
        LOGGER.info("Cleared villager data cache");
    }
    
    /**
     * Gets the number of villagers currently in cache.
     *
     * @return Number of cached villager data entries
     */
    public static int getCacheSize() {
        return villagerDataCache.size();
    }
    
    /**
     * Checks if a villager has data in the cache.
     *
     * @param villagerId The UUID of the villager
     * @return true if data exists, false otherwise
     */
    public static boolean hasVillagerData(UUID villagerId) {
        return villagerDataCache.containsKey(villagerId);
    }
    
    /**
     * Gets the effective name of a villager (custom name if set, otherwise original name).
     *
     * @param villagerId The UUID of the villager
     * @return The villager's effective name, or "Unknown Villager" if not found
     */
    public static String getVillagerName(UUID villagerId) {
        VillagerData data = villagerDataCache.get(villagerId);
        return data != null ? data.getEffectiveName() : "Unknown Villager";
    }
    
    /**
     * Gets the custom name of a villager if set.
     *
     * @param villagerId The UUID of the villager
     * @return The villager's custom name, or null if not set or villager not found
     */
    public static String getCustomName(UUID villagerId) {
        VillagerData data = villagerDataCache.get(villagerId);
        return data != null ? data.getCustomName() : null;
    }
    
    /**
     * Gets the original generated name of a villager.
     *
     * @param villagerId The UUID of the villager
     * @return The villager's original name, or null if villager not found
     */
    public static String getOriginalName(UUID villagerId) {
        VillagerData data = villagerDataCache.get(villagerId);
        return data != null ? data.getOriginalName() : null;
    }
    
    /**
     * Gets a singleton instance of the VillagerDataManager.
     * This method is provided for compatibility with existing code that expects an instance.
     *
     * @return A singleton instance
     */
    public static VillagerDataManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Singleton holder for lazy initialization.
     */
    private static class SingletonHolder {
        private static final VillagerDataManager INSTANCE = new VillagerDataManager();
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private VillagerDataManager() {
        // Private constructor
    }
}