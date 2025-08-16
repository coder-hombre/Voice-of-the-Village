package com.foogly.voiceofthevillage.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles saving and loading villager data to/from JSON files.
 * Provides automatic backup functionality and thread-safe operations.
 */
public class DataPersistence {
    private static final Logger LOGGER = Logger.getLogger(DataPersistence.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private static final String DATA_DIRECTORY = "villager_data";
    private static final String BACKUP_DIRECTORY = "villager_data/backups";
    private static final String FILE_EXTENSION = ".json";
    private static final String BACKUP_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    private static final int MAX_BACKUP_FILES = 10;

    private final Path dataDir;
    private final Path backupDir;
    private final Map<UUID, VillagerData> cache = new ConcurrentHashMap<>();
    private final Object fileLock = new Object();

    /**
     * Creates a new DataPersistence instance with the specified base directory.
     *
     * @param baseDirectory The base directory for data storage
     */
    public DataPersistence(Path baseDirectory) {
        this.dataDir = baseDirectory.resolve(DATA_DIRECTORY);
        this.backupDir = baseDirectory.resolve(BACKUP_DIRECTORY);
        initializeDirectories();
    }

    /**
     * Creates a new DataPersistence instance with the default directory.
     */
    public DataPersistence() {
        this(Paths.get("."));
    }

    /**
     * Initializes the data and backup directories.
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(backupDir);
            LOGGER.info("Initialized villager data directories: " + dataDir.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create villager data directories", e);
            throw new RuntimeException("Failed to initialize data persistence", e);
        }
    }

    /**
     * Saves villager data to a JSON file.
     *
     * @param villagerData The villager data to save
     * @throws IOException If the save operation fails
     */
    public void saveVillagerData(VillagerData villagerData) throws IOException {
        if (villagerData == null || villagerData.getVillagerId() == null) {
            throw new IllegalArgumentException("Villager data and ID cannot be null");
        }

        UUID villagerId = villagerData.getVillagerId();
        Path filePath = getVillagerDataPath(villagerId);

        synchronized (fileLock) {
            try {
                String json = GSON.toJson(villagerData);
                Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
                // Update cache
                cache.put(villagerId, villagerData);
                
                LOGGER.fine("Saved villager data for ID: " + villagerId);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save villager data for ID: " + villagerId, e);
                throw e;
            }
        }
    }

    /**
     * Loads villager data from a JSON file.
     *
     * @param villagerId The UUID of the villager
     * @return The loaded villager data, or null if not found
     * @throws IOException If the load operation fails
     */
    public VillagerData loadVillagerData(UUID villagerId) throws IOException {
        if (villagerId == null) {
            throw new IllegalArgumentException("Villager ID cannot be null");
        }

        // Check cache first
        VillagerData cached = cache.get(villagerId);
        if (cached != null) {
            return cached;
        }

        Path filePath = getVillagerDataPath(villagerId);
        
        if (!Files.exists(filePath)) {
            return null;
        }

        synchronized (fileLock) {
            try {
                String json = Files.readString(filePath);
                VillagerData villagerData = GSON.fromJson(json, VillagerData.class);
                
                // Update cache
                if (villagerData != null) {
                    cache.put(villagerId, villagerData);
                }
                
                LOGGER.fine("Loaded villager data for ID: " + villagerId);
                return villagerData;
            } catch (JsonSyntaxException e) {
                LOGGER.log(Level.WARNING, "Corrupted villager data file for ID: " + villagerId, e);
                // Try to restore from backup
                return restoreFromBackup(villagerId);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load villager data for ID: " + villagerId, e);
                throw e;
            }
        }
    }

    /**
     * Deletes villager data file and removes from cache.
     *
     * @param villagerId The UUID of the villager
     * @throws IOException If the delete operation fails
     */
    public void deleteVillagerData(UUID villagerId) throws IOException {
        if (villagerId == null) {
            throw new IllegalArgumentException("Villager ID cannot be null");
        }

        Path filePath = getVillagerDataPath(villagerId);
        
        synchronized (fileLock) {
            try {
                Files.deleteIfExists(filePath);
                cache.remove(villagerId);
                LOGGER.fine("Deleted villager data for ID: " + villagerId);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete villager data for ID: " + villagerId, e);
                throw e;
            }
        }
    }

    /**
     * Checks if villager data exists for the given ID.
     *
     * @param villagerId The UUID of the villager
     * @return true if data exists, false otherwise
     */
    public boolean hasVillagerData(UUID villagerId) {
        if (villagerId == null) {
            return false;
        }

        // Check cache first
        if (cache.containsKey(villagerId)) {
            return true;
        }

        Path filePath = getVillagerDataPath(villagerId);
        return Files.exists(filePath);
    }

    /**
     * Gets all villager IDs that have saved data.
     *
     * @return A set of villager UUIDs
     * @throws IOException If the directory scan fails
     */
    public Set<UUID> getAllVillagerIds() throws IOException {
        Set<UUID> villagerIds = new HashSet<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*" + FILE_EXTENSION)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String uuidString = fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
                
                try {
                    UUID villagerId = UUID.fromString(uuidString);
                    villagerIds.add(villagerId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid UUID in filename: " + fileName);
                }
            }
        }
        
        return villagerIds;
    }

    /**
     * Loads all villager data from disk.
     *
     * @return A map of villager UUIDs to their data
     * @throws IOException If the load operation fails
     */
    public Map<UUID, VillagerData> loadAllVillagerData() throws IOException {
        Map<UUID, VillagerData> allData = new HashMap<>();
        Set<UUID> villagerIds = getAllVillagerIds();
        
        for (UUID villagerId : villagerIds) {
            try {
                VillagerData data = loadVillagerData(villagerId);
                if (data != null) {
                    allData.put(villagerId, data);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load villager data for ID: " + villagerId, e);
                // Continue loading other villagers even if one fails
            }
        }
        
        LOGGER.info("Loaded " + allData.size() + " villager data entries");
        return allData;
    }

    /**
     * Creates a backup of all villager data.
     *
     * @throws IOException If the backup operation fails
     */
    public void createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(BACKUP_DATE_FORMAT));
        Path backupPath = backupDir.resolve("backup_" + timestamp);
        
        synchronized (fileLock) {
            try {
                Files.createDirectories(backupPath);
                
                // Copy all data files to backup directory
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*" + FILE_EXTENSION)) {
                    for (Path dataFile : stream) {
                        Path backupFile = backupPath.resolve(dataFile.getFileName());
                        Files.copy(dataFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                
                LOGGER.info("Created backup at: " + backupPath.toAbsolutePath());
                
                // Clean up old backups
                cleanupOldBackups();
                
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create backup", e);
                throw e;
            }
        }
    }

    /**
     * Restores villager data from the most recent backup.
     *
     * @param villagerId The UUID of the villager to restore
     * @return The restored villager data, or null if not found
     */
    private VillagerData restoreFromBackup(UUID villagerId) {
        try {
            List<Path> backupDirs = getBackupDirectories();
            
            for (Path backupDir : backupDirs) {
                Path backupFile = backupDir.resolve(villagerId.toString() + FILE_EXTENSION);
                
                if (Files.exists(backupFile)) {
                    String json = Files.readString(backupFile);
                    VillagerData villagerData = GSON.fromJson(json, VillagerData.class);
                    
                    if (villagerData != null) {
                        LOGGER.info("Restored villager data from backup for ID: " + villagerId);
                        // Save the restored data to the main directory
                        saveVillagerData(villagerData);
                        return villagerData;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to restore from backup for ID: " + villagerId, e);
        }
        
        return null;
    }

    /**
     * Gets backup directories sorted by creation time (newest first).
     */
    private List<Path> getBackupDirectories() throws IOException {
        List<Path> backupDirs = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, "backup_*")) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    backupDirs.add(path);
                }
            }
        }
        
        // Sort by creation time (newest first)
        backupDirs.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (IOException e) {
                return 0;
            }
        });
        
        return backupDirs;
    }

    /**
     * Removes old backup directories, keeping only the most recent ones.
     */
    private void cleanupOldBackups() {
        try {
            List<Path> backupDirs = getBackupDirectories();
            
            if (backupDirs.size() > MAX_BACKUP_FILES) {
                for (int i = MAX_BACKUP_FILES; i < backupDirs.size(); i++) {
                    Path oldBackup = backupDirs.get(i);
                    deleteDirectory(oldBackup);
                    LOGGER.fine("Deleted old backup: " + oldBackup.getFileName());
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to cleanup old backups", e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to delete: " + path, e);
                        }
                    });
        }
    }

    /**
     * Gets the file path for a villager's data file.
     */
    private Path getVillagerDataPath(UUID villagerId) {
        return dataDir.resolve(villagerId.toString() + FILE_EXTENSION);
    }

    /**
     * Clears the in-memory cache.
     */
    public void clearCache() {
        cache.clear();
        LOGGER.fine("Cleared villager data cache");
    }

    /**
     * Gets the number of cached villager data entries.
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Gets the data directory path.
     */
    public Path getDataDirectory() {
        return dataDir;
    }

    /**
     * Gets the backup directory path.
     */
    public Path getBackupDirectory() {
        return backupDir;
    }

    /**
     * Saves backup data to a file.
     *
     * @param backupFile Path to the backup file
     * @param backupData Data to backup
     * @throws IOException If the save operation fails
     */
    public void saveBackupData(Path backupFile, Map<String, Object> backupData) throws IOException {
        synchronized (fileLock) {
            try {
                String json = GSON.toJson(backupData);
                Files.writeString(backupFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                LOGGER.fine("Saved backup data to: " + backupFile);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save backup data to: " + backupFile, e);
                throw e;
            }
        }
    }

    /**
     * Loads backup data from a file.
     *
     * @param backupFile Path to the backup file
     * @return Loaded backup data
     * @throws IOException If the load operation fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadBackupData(Path backupFile) throws IOException {
        synchronized (fileLock) {
            try {
                String json = Files.readString(backupFile);
                return GSON.fromJson(json, Map.class);
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.log(Level.WARNING, "Failed to load backup data from: " + backupFile, e);
                throw new IOException("Failed to load backup data", e);
            }
        }
    }

    /**
     * Checks if villager data exists for the given ID.
     *
     * @param villagerId The UUID of the villager
     * @return true if data exists, false otherwise
     */
    public boolean villagerDataExists(UUID villagerId) {
        return hasVillagerData(villagerId);
    }

    /**
     * Deserializes villager data from an object.
     *
     * @param data The data object to deserialize
     * @return VillagerData instance
     */
    public VillagerData deserializeVillagerData(Object data) {
        try {
            String json = GSON.toJson(data);
            return GSON.fromJson(json, VillagerData.class);
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "Failed to deserialize villager data", e);
            throw new RuntimeException("Failed to deserialize villager data", e);
        }
    }
}