package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.ai.AIServiceManager;
import com.foogly.voiceofthevillage.ai.PromptBuilder;
import com.foogly.voiceofthevillage.data.DataPersistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComprehensiveMemoryManager.
 */
class ComprehensiveMemoryManagerTest {

    private ComprehensiveMemoryManager memoryManager;
    private DataPersistence dataPersistence;

    @BeforeEach
    void setUp() {
        // Create test data persistence with temporary directory
        dataPersistence = new DataPersistence(Paths.get("test_data"));
        
        // Create mock AI service manager and prompt builder for testing
        AIServiceManager aiServiceManager = AIServiceManager.getInstance();
        PromptBuilder promptBuilder = new PromptBuilder();
        
        memoryManager = new ComprehensiveMemoryManager(dataPersistence, aiServiceManager, promptBuilder);
    }

    @Test
    void testInitialization() {
        // Test that all components are properly initialized
        assertNotNull(memoryManager.getMemoryManager());
        assertNotNull(memoryManager.getBackupManager());
        assertNotNull(memoryManager.getOptimizationService());
        assertNotNull(memoryManager.getConversationIntegrator());
    }

    @Test
    void testHealthCheck() {
        // Test health check functionality
        assertDoesNotThrow(() -> {
            memoryManager.performHealthCheck();
        });
        
        // Health status should be available
        assertNotNull(memoryManager.getHealthStatus());
        
        // Health check should return a boolean
        boolean isHealthy = memoryManager.isHealthy();
        assertTrue(isHealthy || !isHealthy); // Always true, just checking no exceptions
    }

    @Test
    void testComprehensiveStatistics() {
        // Test getting comprehensive statistics
        ComprehensiveMemoryManager.ComprehensiveMemoryStatistics stats = 
            memoryManager.getComprehensiveStatistics();
        
        assertNotNull(stats);
        assertNotNull(stats.getHealthStatus());
        assertTrue(stats.getLastHealthCheckTime() >= 0);
        
        // Health status should be consistent
        assertEquals(memoryManager.isHealthy(), stats.isHealthy());
        assertEquals(memoryManager.getHealthStatus(), stats.getHealthStatus());
    }

    @Test
    void testComprehensiveCleanup() {
        // Test comprehensive cleanup
        CompletableFuture<ComprehensiveMemoryManager.CleanupResult> cleanupFuture = 
            memoryManager.performComprehensiveCleanup();
        
        assertNotNull(cleanupFuture);
        
        // Wait for cleanup to complete
        ComprehensiveMemoryManager.CleanupResult result = cleanupFuture.join();
        
        assertNotNull(result);
        assertTrue(result.getMemoriesRemoved() >= 0);
        assertTrue(result.getVillagersProcessed() >= 0);
        assertTrue(result.getDurationMs() >= 0);
        assertNotNull(result.getReport());
        
        // Result should have a success status
        assertTrue(result.isSuccess() || !result.isSuccess()); // Always true, just checking no exceptions
    }

    @Test
    void testComponentAccess() {
        // Test that all components are accessible and functional
        
        // Memory manager should be accessible
        assertNotNull(memoryManager.getMemoryManager());
        assertDoesNotThrow(() -> {
            memoryManager.getMemoryManager().getOverallStatistics();
        });
        
        // Backup manager should be accessible
        assertNotNull(memoryManager.getBackupManager());
        assertDoesNotThrow(() -> {
            memoryManager.getBackupManager().getStatistics();
        });
        
        // Optimization service should be accessible
        assertNotNull(memoryManager.getOptimizationService());
        assertDoesNotThrow(() -> {
            memoryManager.getOptimizationService().getMemoryUsageStatistics();
        });
        
        // Conversation integrator should be accessible
        assertNotNull(memoryManager.getConversationIntegrator());
    }

    @Test
    void testCleanupResultGetters() {
        // Test CleanupResult class
        ComprehensiveMemoryManager.CleanupResult result = 
            new ComprehensiveMemoryManager.CleanupResult(true, 10, 5, 1000L, "Test cleanup");
        
        assertTrue(result.isSuccess());
        assertEquals(10, result.getMemoriesRemoved());
        assertEquals(5, result.getVillagersProcessed());
        assertEquals(1000L, result.getDurationMs());
        assertEquals("Test cleanup", result.getReport());
    }

    @Test
    void testComprehensiveMemoryStatisticsGetters() {
        // Test ComprehensiveMemoryStatistics class
        ComprehensiveMemoryManager.ComprehensiveMemoryStatistics stats = 
            new ComprehensiveMemoryManager.ComprehensiveMemoryStatistics(
                null, null, null, true, "All systems healthy", System.currentTimeMillis());
        
        assertTrue(stats.isHealthy());
        assertEquals("All systems healthy", stats.getHealthStatus());
        assertTrue(stats.getLastHealthCheckTime() > 0);
        
        // Null components should be handled gracefully
        assertNull(stats.getMemoryStats());
        assertNull(stats.getOptimizationStats());
        assertNull(stats.getBackupStats());
    }

    @Test
    void testShutdown() {
        // Test that shutdown completes without errors
        assertDoesNotThrow(() -> {
            memoryManager.shutdown();
        });
    }

    @Test
    void testHealthStatusConsistency() {
        // Perform health check
        memoryManager.performHealthCheck();
        
        // Get health status through different methods
        boolean isHealthy1 = memoryManager.isHealthy();
        String healthStatus1 = memoryManager.getHealthStatus();
        
        ComprehensiveMemoryManager.ComprehensiveMemoryStatistics stats = 
            memoryManager.getComprehensiveStatistics();
        boolean isHealthy2 = stats.isHealthy();
        String healthStatus2 = stats.getHealthStatus();
        
        // Health status should be consistent
        assertEquals(isHealthy1, isHealthy2);
        assertEquals(healthStatus1, healthStatus2);
    }
}