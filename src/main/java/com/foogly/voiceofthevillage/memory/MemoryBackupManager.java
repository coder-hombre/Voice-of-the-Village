package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import com.foogly.voiceofthevillage.data.DataPersistence;
import com.foogly.voiceofthevillage.data.VillagerData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Manages backup and recovery of villager memory data.
 * Provides automatic backup scheduling and recovery capabilities.
 */
public class MemoryBackupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryBackupManager.class);
    
    private final DataPersistence dataPersistence;
    private final ScheduledExecutorService scheduler;
    private final Path backupDirectory;
    
    // Backup configuration
    private static final String BACKUP_DIR_NAME = "memory_backups";
    private static final String BACKUP_FILE_PREFIX = "villager_data_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUP_FILES = 10; // Keep last 10 backups
    
    // Statistics
    private long lastBackupTime = 0;
    private int totalBackupsCreated = 0;
    private int totalBackupsRestored = 0;
    private String lastBackupError = null;

    /**
     * Creates a new memory backup manager.
     *
     * @param dataPersistence The data persistence instance
     */
    public MemoryBackupManager(DataPersistence dataPersistence) {
        this.dataPersistence = dataPersistence;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MemoryBackup");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize backup directory
        this.backupDirectory = initializeBackupDirectory();
        
        // Schedule automatic backups
        scheduleAutomaticBackups();
    }

    /**
     * Initializes the backup directory.
     *
     * @return Path to the backup directory
     */
    private Path initializeBackupDirectory() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR_NAME);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
                LOGGER.info("Created backup directory: {}", backupDir.toAbsolutePath());
            }
            return backupDir;
        } catch (IOException e) {
            LOGGER.error("Failed to create backup directory", e);
            // Fallback to current directory
            return Paths.get(".");
        }
    }

    /**
     * Schedules automatic backups based on configuration.
     */
    private void scheduleAutomaticBackups() {
        // Schedule daily backups at 3 AM
        long initialDelay = calculateInitialDelayForDailyBackup();
        long period = TimeUnit.DAYS.toMillis(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performAutomaticBackup();
            } catch (Exception e) {
                LOGGER.error("Error during automatic backup", e);
                lastBackupError = e.getMessage();
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);
        
        LOGGER.info("Scheduled automatic daily backups");
    }

    /**
     * Calculates the initial delay for daily backup scheduling.
     *
     * @return Initial delay in milliseconds
     */
    private long calculateInitialDelayForDailyBackup() {
        // Schedule for 3 AM next day
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBackup = now.withHour(3).withMinute(0).withSecond(0).withNano(0);
        
        if (now.isAfter(nextBackup)) {
            nextBackup = nextBackup.plusDays(1);
        }
        
        return java.time.Duration.between(now, nextBackup).toMillis();
    }

    /**
     * Performs an automatic backup of all villager data.
     */
    public void performAutomaticBackup() {
        try {
            String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
            String backupFileName = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
            Path backupFile = backupDirectory.resolve(backupFileName);
            
            createBackup(backupFile);
            cleanupOldBackups();
            
            lastBackupTime = System.currentTimeMillis();
            totalBackupsCreated++;
            lastBackupError = null;
            
            LOGGER.info("Automatic backup completed: {}", backupFileName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to perform automatic backup", e);
            lastBackupError = e.getMessage();
        }
    }

    /**
     * Creates a manual backup of all villager data.
     *
     * @param backupName Optional custom name for the backup
     * @return Path to the created backup file
     * @throws IOException If backup creation fails
     */
    public Path createManualBackup(String backupName) throws IOException {
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        String fileName = backupName != null && !backupName.trim().isEmpty() 
            ? backupName + "_" + timestamp + BACKUP_FILE_EXTENSION
            : BACKUP_FILE_PREFIX + "manual_" + timestamp + BACKUP_FILE_EXTENSION;
        
        Path backupFile = backupDirectory.resolve(fileName);
        createBackup(backupFile);
        
        totalBackupsCreated++;
        LOGGER.info("Manual backup created: {}", fileName);
        
        return backupFile;
    }

    /**
     * Creates a backup file with all villager data.
     *
     * @param backupFile Path where to create the backup
     * @throws IOException If backup creation fails
     */
    private void createBackup(Path backupFile) throws IOException {
        try {
            // Get all villager IDs
            Set<UUID> villagerIds = dataPersistence.getAllVillagerIds();
            
            // Create a consolidated backup structure
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("backup_timestamp", System.currentTimeMillis());
            backupData.put("backup_version", "1.0");
            backupData.put("villager_count", villagerIds.size());
            
            Map<String, VillagerData> villagerDataMap = new HashMap<>();
            
            for (UUID villagerId : villagerIds) {
                try {
                    VillagerData villagerData = dataPersistence.loadVillagerData(villagerId);
                    if (villagerData != null) {
                        villagerDataMap.put(villagerId.toString(), villagerData);
                    }
                } catch (IOException e) {
                    LOGGER.warn("Failed to load villager data for backup: {}", villagerId, e);
                }
            }
            
            backupData.put("villager_data", villagerDataMap);
            
            // Save backup data to file
            dataPersistence.saveBackupData(backupFile, backupData);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create backup at {}", backupFile, e);
            throw e;
        }
    }

    /**
     * Restores villager data from a backup file.
     *
     * @param backupFile Path to the backup file
     * @param overwriteExisting Whether to overwrite existing villager data
     * @return Number of villagers restored
     * @throws IOException If restoration fails
     */
    public int restoreFromBackup(Path backupFile, boolean overwriteExisting) throws IOException {
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file does not exist: " + backupFile);
        }
        
        try {
            LOGGER.info("Starting restoration from backup: {}", backupFile.getFileName());
            
            // Load backup data
            Map<String, Object> backupData = dataPersistence.loadBackupData(backupFile);
            
            // Validate backup structure
            if (!backupData.containsKey("villager_data")) {
                throw new IOException("Invalid backup file format: missing villager_data");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> villagerDataMap = (Map<String, Object>) backupData.get("villager_data");
            
            int restoredCount = 0;
            int skippedCount = 0;
            
            for (Map.Entry<String, Object> entry : villagerDataMap.entrySet()) {
                try {
                    UUID villagerId = UUID.fromString(entry.getKey());
                    
                    // Check if villager data already exists
                    if (!overwriteExisting && dataPersistence.villagerDataExists(villagerId)) {
                        skippedCount++;
                        continue;
                    }
                    
                    // Convert the data back to VillagerData object
                    VillagerData villagerData = dataPersistence.deserializeVillagerData(entry.getValue());
                    
                    // Save the restored data
                    dataPersistence.saveVillagerData(villagerData);
                    restoredCount++;
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to restore villager data for ID {}: {}", entry.getKey(), e.getMessage());
                }
            }
            
            totalBackupsRestored++;
            
            LOGGER.info("Backup restoration completed: {} villagers restored, {} skipped", 
                       restoredCount, skippedCount);
            
            return restoredCount;
            
        } catch (IOException e) {
            LOGGER.error("Failed to restore from backup: {}", backupFile, e);
            throw e;
        }
    }

    /**
     * Lists available backup files.
     *
     * @return List of backup file information
     */
    public List<BackupInfo> listAvailableBackups() {
        List<BackupInfo> backups = new ArrayList<>();
        
        try (Stream<Path> files = Files.list(backupDirectory)) {
            files.filter(path -> path.getFileName().toString().startsWith(BACKUP_FILE_PREFIX))
                 .filter(path -> path.getFileName().toString().endsWith(BACKUP_FILE_EXTENSION))
                 .forEach(path -> {
                     try {
                         BackupInfo info = createBackupInfo(path);
                         backups.add(info);
                     } catch (IOException e) {
                         LOGGER.warn("Failed to read backup info for {}: {}", path, e.getMessage());
                     }
                 });
        } catch (IOException e) {
            LOGGER.error("Failed to list backup files", e);
        }
        
        // Sort by creation time (newest first)
        backups.sort((a, b) -> Long.compare(b.getCreationTime(), a.getCreationTime()));
        
        return backups;
    }

    /**
     * Creates backup information from a backup file.
     *
     * @param backupFile Path to the backup file
     * @return Backup information
     * @throws IOException If reading backup info fails
     */
    private BackupInfo createBackupInfo(Path backupFile) throws IOException {
        long fileSize = Files.size(backupFile);
        long creationTime = Files.getLastModifiedTime(backupFile).toMillis();
        
        // Try to read backup metadata
        int villagerCount = 0;
        long backupTimestamp = creationTime;
        
        try {
            Map<String, Object> backupData = dataPersistence.loadBackupData(backupFile);
            if (backupData.containsKey("villager_count")) {
                villagerCount = (Integer) backupData.get("villager_count");
            }
            if (backupData.containsKey("backup_timestamp")) {
                backupTimestamp = (Long) backupData.get("backup_timestamp");
            }
        } catch (Exception e) {
            LOGGER.debug("Could not read backup metadata for {}: {}", backupFile, e.getMessage());
        }
        
        return new BackupInfo(
            backupFile.getFileName().toString(),
            backupFile,
            fileSize,
            creationTime,
            backupTimestamp,
            villagerCount
        );
    }

    /**
     * Cleans up old backup files, keeping only the most recent ones.
     */
    private void cleanupOldBackups() {
        try {
            List<BackupInfo> backups = listAvailableBackups();
            
            if (backups.size() > MAX_BACKUP_FILES) {
                // Remove oldest backups
                for (int i = MAX_BACKUP_FILES; i < backups.size(); i++) {
                    BackupInfo oldBackup = backups.get(i);
                    try {
                        Files.delete(oldBackup.getFilePath());
                        LOGGER.debug("Deleted old backup: {}", oldBackup.getFileName());
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete old backup {}: {}", oldBackup.getFileName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error during backup cleanup", e);
        }
    }

    /**
     * Gets backup statistics.
     *
     * @return Backup statistics
     */
    public BackupStatistics getStatistics() {
        List<BackupInfo> availableBackups = listAvailableBackups();
        
        return new BackupStatistics(
            lastBackupTime,
            totalBackupsCreated,
            totalBackupsRestored,
            availableBackups.size(),
            lastBackupError,
            backupDirectory.toAbsolutePath().toString()
        );
    }

    /**
     * Shuts down the backup manager and stops scheduled tasks.
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
        LOGGER.info("Memory backup manager shutdown completed");
    }

    /**
     * Information about a backup file.
     */
    public static class BackupInfo {
        private final String fileName;
        private final Path filePath;
        private final long fileSize;
        private final long creationTime;
        private final long backupTimestamp;
        private final int villagerCount;

        public BackupInfo(String fileName, Path filePath, long fileSize, 
                         long creationTime, long backupTimestamp, int villagerCount) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.creationTime = creationTime;
            this.backupTimestamp = backupTimestamp;
            this.villagerCount = villagerCount;
        }

        // Getters
        public String getFileName() { return fileName; }
        public Path getFilePath() { return filePath; }
        public long getFileSize() { return fileSize; }
        public long getCreationTime() { return creationTime; }
        public long getBackupTimestamp() { return backupTimestamp; }
        public int getVillagerCount() { return villagerCount; }
        
        public String getFormattedSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
        
        public String getFormattedCreationTime() {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(creationTime), 
                java.time.ZoneId.systemDefault()
            ).format(BACKUP_TIMESTAMP_FORMAT);
        }
    }

    /**
     * Statistics about backup operations.
     */
    public static class BackupStatistics {
        private final long lastBackupTime;
        private final int totalBackupsCreated;
        private final int totalBackupsRestored;
        private final int availableBackups;
        private final String lastError;
        private final String backupDirectory;

        public BackupStatistics(long lastBackupTime, int totalBackupsCreated, 
                              int totalBackupsRestored, int availableBackups, 
                              String lastError, String backupDirectory) {
            this.lastBackupTime = lastBackupTime;
            this.totalBackupsCreated = totalBackupsCreated;
            this.totalBackupsRestored = totalBackupsRestored;
            this.availableBackups = availableBackups;
            this.lastError = lastError;
            this.backupDirectory = backupDirectory;
        }

        // Getters
        public long getLastBackupTime() { return lastBackupTime; }
        public int getTotalBackupsCreated() { return totalBackupsCreated; }
        public int getTotalBackupsRestored() { return totalBackupsRestored; }
        public int getAvailableBackups() { return availableBackups; }
        public String getLastError() { return lastError; }
        public String getBackupDirectory() { return backupDirectory; }
        
        public boolean hasRecentBackup() {
            return lastBackupTime > 0 && 
                   (System.currentTimeMillis() - lastBackupTime) < TimeUnit.DAYS.toMillis(2);
        }
    }
}