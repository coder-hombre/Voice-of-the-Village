package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.ai.AIServiceManager;
import com.foogly.voiceofthevillage.ai.PromptBuilder;
import com.foogly.voiceofthevillage.data.DataPersistence;
import com.foogly.voiceofthevillage.data.MemoryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive memory management system that coordinates all memory-related services.
 * Provides a unified interface for memory operations, cleanup, backup, and optimization.
 */
public class ComprehensiveMemoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveMemoryManager.class);
    
    private final MemoryManager memoryManager;
    private final MemoryBackupManager backupManager;
    private final MemoryOptimizationService optimizationService;
    private final ConversationMemoryIntegrator conversationIntegrator;
    private final ScheduledExecutorService scheduler;
    
    // Health monitoring
    private boolean isHealthy = true;
    private String lastHealthCheckResult = "OK";
    private long lastHealthCheckTime = 0;

    /**
     * Creates a comprehensive memory manager with all sub-services.
     *
     * @param dataPersistence   The data persistence service
     * @param aiServiceManager  The AI service manager
     * @param promptBuilder     The prompt builder
     */
    public ComprehensiveMemoryManager(DataPersistence dataPersistence,
                                    AIServiceManager aiServiceManager,
                                    PromptBuilder promptBuilder) {
        // Initialize core services
        this.memoryManager = new MemoryManager(dataPersistence);
        this.backupManager = new MemoryBackupManager(dataPersistence);
        this.optimizationService = new MemoryOptimizationService(memoryManager);
        this.conversationIntegrator = new ConversationMemoryIntegrator(
            memoryManager, aiServiceManager, promptBuilder);
        
        // Initialize health monitoring
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MemoryHealthMonitor");
            t.setDaemon(true);
            return t;
        });
        
        scheduleHealthChecks();
        
        LOGGER.info("Comprehensive memory manager initialized");
    }

    /**
     * Schedules periodic health checks.
     */
    private void scheduleHealthChecks() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                LOGGER.error("Error during memory health check", e);
                isHealthy = false;
                lastHealthCheckResult = "Error: " + e.getMessage();
            }
        }, 1, 1, TimeUnit.HOURS);
        
        LOGGER.info("Scheduled memory health checks every hour");
    }

    /**
     * Performs a comprehensive health check of all memory services.
     */
    public void performHealthCheck() {
        StringBuilder healthReport = new StringBuilder();
        boolean allHealthy = true;
        
        try {
            // Check memory manager health
            MemoryManager.OverallMemoryStatistics memStats = memoryManager.getOverallStatistics();
            if (memStats.getTotalMemories() > 0) {
                healthReport.append("Memory: ").append(memStats.getTotalMemories()).append(" memories, ");
            } else {
                healthReport.append("Memory: No data, ");
            }
            
            // Check optimization service health
            MemoryOptimizationService.MemoryUsageStatistics optStats = optimizationService.getMemoryUsageStatistics();
            if (optStats.isMemoryUsageCritical()) {
                healthReport.append("CRITICAL memory usage (").append(String.format("%.1f", optStats.getMemoryUsagePercentage())).append("%), ");
                allHealthy = false;
                
                // Trigger emergency cleanup
                LOGGER.warn("Critical memory usage detected, triggering emergency cleanup");
                optimizationService.performEmergencyCleanup();
            } else if (optStats.isMemoryUsageHigh()) {
                healthReport.append("HIGH memory usage (").append(String.format("%.1f", optStats.getMemoryUsagePercentage())).append("%), ");
            } else {
                healthReport.append("Memory usage OK (").append(String.format("%.1f", optStats.getMemoryUsagePercentage())).append("%), ");
            }
            
            // Check backup manager health
            MemoryBackupManager.BackupStatistics backupStats = backupManager.getStatistics();
            if (backupStats.hasRecentBackup()) {
                healthReport.append("Backup: Recent backup available, ");
            } else {
                healthReport.append("Backup: No recent backup, ");
                allHealthy = false;
            }
            
            if (backupStats.getLastError() != null) {
                healthReport.append("Backup error: ").append(backupStats.getLastError()).append(", ");
                allHealthy = false;
            }
            
            // Overall health assessment
            isHealthy = allHealthy;
            lastHealthCheckResult = healthReport.toString();
            lastHealthCheckTime = System.currentTimeMillis();
            
            if (allHealthy) {
                LOGGER.debug("Memory health check passed: {}", lastHealthCheckResult);
            } else {
                LOGGER.warn("Memory health check issues detected: {}", lastHealthCheckResult);
            }
            
        } catch (Exception e) {
            isHealthy = false;
            lastHealthCheckResult = "Health check failed: " + e.getMessage();
            lastHealthCheckTime = System.currentTimeMillis();
            LOGGER.error("Memory health check failed", e);
        }
    }

    /**
     * Gets the conversation memory integrator.
     *
     * @return The conversation memory integrator
     */
    public ConversationMemoryIntegrator getConversationIntegrator() {
        return conversationIntegrator;
    }

    /**
     * Gets the memory manager.
     *
     * @return The memory manager
     */
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    /**
     * Gets the backup manager.
     *
     * @return The backup manager
     */
    public MemoryBackupManager getBackupManager() {
        return backupManager;
    }

    /**
     * Gets the optimization service.
     *
     * @return The optimization service
     */
    public MemoryOptimizationService getOptimizationService() {
        return optimizationService;
    }

    /**
     * Performs a comprehensive cleanup of all memory systems.
     *
     * @return Cleanup result summary
     */
    public CompletableFuture<CleanupResult> performComprehensiveCleanup() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int totalMemoriesRemoved = 0;
            int villagersProcessed = 0;
            StringBuilder report = new StringBuilder();
            
            try {
                LOGGER.info("Starting comprehensive memory cleanup");
                
                // Perform memory cleanup
                memoryManager.performAutomaticCleanup();
                MemoryManager.OverallMemoryStatistics memStats = memoryManager.getOverallStatistics();
                totalMemoriesRemoved += memStats.getLastCleanupRemovedCount();
                villagersProcessed = memStats.getTotalVillagers();
                report.append("Memory cleanup: ").append(memStats.getLastCleanupRemovedCount()).append(" memories removed. ");
                
                // Perform optimization
                optimizationService.performOptimization();
                MemoryOptimizationService.MemoryUsageStatistics optStats = optimizationService.getMemoryUsageStatistics();
                report.append("Optimization: ").append(optStats.getLastOptimizationResult()).append(". ");
                
                // Create backup
                try {
                    Path backupPath = backupManager.createManualBackup("comprehensive_cleanup");
                    report.append("Backup created: ").append(backupPath.getFileName()).append(". ");
                } catch (IOException e) {
                    report.append("Backup failed: ").append(e.getMessage()).append(". ");
                    LOGGER.warn("Failed to create backup during comprehensive cleanup", e);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                
                LOGGER.info("Comprehensive cleanup completed in {}ms: {}", duration, report.toString());
                
                return new CleanupResult(true, totalMemoriesRemoved, villagersProcessed, 
                                       duration, report.toString());
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                String errorMessage = "Comprehensive cleanup failed: " + e.getMessage();
                
                LOGGER.error("Comprehensive cleanup failed", e);
                
                return new CleanupResult(false, totalMemoriesRemoved, villagersProcessed, 
                                       duration, errorMessage);
            }
        });
    }

    /**
     * Gets comprehensive statistics about all memory systems.
     *
     * @return Comprehensive memory statistics
     */
    public ComprehensiveMemoryStatistics getComprehensiveStatistics() {
        try {
            MemoryManager.OverallMemoryStatistics memStats = memoryManager.getOverallStatistics();
            MemoryOptimizationService.MemoryUsageStatistics optStats = optimizationService.getMemoryUsageStatistics();
            MemoryBackupManager.BackupStatistics backupStats = backupManager.getStatistics();
            
            return new ComprehensiveMemoryStatistics(
                memStats,
                optStats,
                backupStats,
                isHealthy,
                lastHealthCheckResult,
                lastHealthCheckTime
            );
            
        } catch (Exception e) {
            LOGGER.error("Failed to get comprehensive statistics", e);
            return new ComprehensiveMemoryStatistics(
                null, null, null, false, 
                "Statistics error: " + e.getMessage(), 
                System.currentTimeMillis()
            );
        }
    }

    /**
     * Checks if the memory system is healthy.
     *
     * @return true if all systems are healthy
     */
    public boolean isHealthy() {
        return isHealthy;
    }

    /**
     * Gets the last health check result.
     *
     * @return Health check result message
     */
    public String getHealthStatus() {
        return lastHealthCheckResult;
    }

    /**
     * Shuts down all memory management services.
     */
    public void shutdown() {
        LOGGER.info("Shutting down comprehensive memory manager");
        
        try {
            // Shutdown all services
            scheduler.shutdown();
            memoryManager.shutdown();
            backupManager.shutdown();
            optimizationService.shutdown();
            
            // Wait for shutdown completion
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            LOGGER.info("Comprehensive memory manager shutdown completed");
            
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.warn("Memory manager shutdown interrupted", e);
        }
    }

    /**
     * Result of a comprehensive cleanup operation.
     */
    public static class CleanupResult {
        private final boolean success;
        private final int memoriesRemoved;
        private final int villagersProcessed;
        private final long durationMs;
        private final String report;

        public CleanupResult(boolean success, int memoriesRemoved, int villagersProcessed, 
                           long durationMs, String report) {
            this.success = success;
            this.memoriesRemoved = memoriesRemoved;
            this.villagersProcessed = villagersProcessed;
            this.durationMs = durationMs;
            this.report = report;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getMemoriesRemoved() { return memoriesRemoved; }
        public int getVillagersProcessed() { return villagersProcessed; }
        public long getDurationMs() { return durationMs; }
        public String getReport() { return report; }
    }

    /**
     * Comprehensive statistics from all memory systems.
     */
    public static class ComprehensiveMemoryStatistics {
        private final MemoryManager.OverallMemoryStatistics memoryStats;
        private final MemoryOptimizationService.MemoryUsageStatistics optimizationStats;
        private final MemoryBackupManager.BackupStatistics backupStats;
        private final boolean isHealthy;
        private final String healthStatus;
        private final long lastHealthCheckTime;

        public ComprehensiveMemoryStatistics(MemoryManager.OverallMemoryStatistics memoryStats,
                                           MemoryOptimizationService.MemoryUsageStatistics optimizationStats,
                                           MemoryBackupManager.BackupStatistics backupStats,
                                           boolean isHealthy, String healthStatus, long lastHealthCheckTime) {
            this.memoryStats = memoryStats;
            this.optimizationStats = optimizationStats;
            this.backupStats = backupStats;
            this.isHealthy = isHealthy;
            this.healthStatus = healthStatus;
            this.lastHealthCheckTime = lastHealthCheckTime;
        }

        // Getters
        public MemoryManager.OverallMemoryStatistics getMemoryStats() { return memoryStats; }
        public MemoryOptimizationService.MemoryUsageStatistics getOptimizationStats() { return optimizationStats; }
        public MemoryBackupManager.BackupStatistics getBackupStats() { return backupStats; }
        public boolean isHealthy() { return isHealthy; }
        public String getHealthStatus() { return healthStatus; }
        public long getLastHealthCheckTime() { return lastHealthCheckTime; }
    }
}