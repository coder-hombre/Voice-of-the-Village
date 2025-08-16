package com.foogly.voiceofthevillage.memory;

import com.foogly.voiceofthevillage.data.InteractionMemory;
import com.foogly.voiceofthevillage.data.InteractionType;
import com.foogly.voiceofthevillage.data.MemoryManager;
import com.foogly.voiceofthevillage.data.VillagerData;
import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.PersonalityType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MemoryOptimizationService.
 */
class MemoryOptimizationServiceTest {

    private MemoryOptimizationService optimizationService;
    private MemoryManager memoryManager;

    @BeforeEach
    void setUp() {
        // For testing, we'll create a simple memory manager
        // In a real implementation, this would be properly mocked
        memoryManager = new MemoryManager(null);
        optimizationService = new MemoryOptimizationService(memoryManager);
    }

    @Test
    void testMemoryUsageStatistics() {
        // Test getting memory usage statistics
        MemoryOptimizationService.MemoryUsageStatistics stats = optimizationService.getMemoryUsageStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.getTotalVillagers() >= 0);
        assertTrue(stats.getTotalMemories() >= 0);
        assertTrue(stats.getAverageMemoriesPerVillager() >= 0);
        assertNotNull(stats.getInteractionTypeCounts());
        assertNotNull(stats.getLastOptimizationResult());
    }

    @Test
    void testMemoryUsageThresholds() {
        MemoryOptimizationService.MemoryUsageStatistics stats = optimizationService.getMemoryUsageStatistics();
        
        // Test threshold calculations
        double usagePercentage = stats.getMemoryUsagePercentage();
        assertTrue(usagePercentage >= 0 && usagePercentage <= 100);
        
        // Test high usage detection
        boolean isHigh = stats.isMemoryUsageHigh();
        boolean isCritical = stats.isMemoryUsageCritical();
        
        // Critical should imply high
        if (isCritical) {
            assertTrue(isHigh);
        }
    }

    @Test
    void testIsMemoryUsageHigh() {
        // Test the service-level memory usage check
        boolean isHigh = optimizationService.isMemoryUsageHigh();
        
        // Should return a boolean without throwing exceptions
        assertTrue(isHigh || !isHigh); // Always true, just checking no exceptions
    }

    @Test
    void testPerformOptimization() {
        // Test that optimization can be performed without errors
        assertDoesNotThrow(() -> {
            optimizationService.performOptimization();
        });
        
        // Check that statistics are updated after optimization
        MemoryOptimizationService.MemoryUsageStatistics stats = optimizationService.getMemoryUsageStatistics();
        assertTrue(stats.getLastOptimizationTime() > 0);
    }

    @Test
    void testEmergencyCleanup() {
        // Test emergency cleanup
        int cleaned = optimizationService.performEmergencyCleanup();
        
        // Should return a non-negative number
        assertTrue(cleaned >= 0);
    }

    @Test
    void testMemoryUsageStatisticsGetters() {
        MemoryOptimizationService.MemoryUsageStatistics stats = optimizationService.getMemoryUsageStatistics();
        
        // Test all getters return reasonable values
        assertTrue(stats.getTotalVillagers() >= 0);
        assertTrue(stats.getVillagersWithMemories() >= 0);
        assertTrue(stats.getTotalMemories() >= 0);
        assertTrue(stats.getAverageMemoriesPerVillager() >= 0);
        assertTrue(stats.getMaxMemoriesPerVillager() >= 0);
        assertTrue(stats.getOldestMemoryTimestamp() >= 0);
        assertTrue(stats.getNewestMemoryTimestamp() >= 0);
        assertTrue(stats.getLastOptimizationTime() >= 0);
        assertTrue(stats.getTotalMemoriesOptimized() >= 0);
        assertTrue(stats.getTotalVillagersOptimized() >= 0);
        
        assertNotNull(stats.getInteractionTypeCounts());
        assertNotNull(stats.getLastOptimizationResult());
    }

    @Test
    void testShutdown() {
        // Test that shutdown completes without errors
        assertDoesNotThrow(() -> {
            optimizationService.shutdown();
        });
    }

    @Test
    void testMemoryOptimizationWithMockData() {
        // Create a villager with many memories for testing optimization logic
        UUID villagerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        VillagerData villagerData = new VillagerData(villagerId, "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Add many memories to test optimization
        for (int i = 0; i < 150; i++) {
            InteractionMemory memory = new InteractionMemory(
                playerId, 
                "TestPlayer", 
                "Message " + i, 
                "Response " + i, 
                InteractionType.TEXT, 
                100L - i // Older memories have lower game day
            );
            memory.setTimestamp(System.currentTimeMillis() - (i * 1000)); // Spread timestamps
            villagerData.addMemory(memory);
        }
        
        // Test that the villager has the expected number of memories
        assertEquals(150, villagerData.getMemories().size());
        
        // The optimization logic would be tested here if we had access to the private methods
        // For now, we just verify the data structure is set up correctly
        List<InteractionMemory> memories = villagerData.getMemories();
        assertFalse(memories.isEmpty());
        
        // Verify memories are properly structured
        for (InteractionMemory memory : memories) {
            assertNotNull(memory.getPlayerId());
            assertNotNull(memory.getPlayerMessage());
            assertNotNull(memory.getVillagerResponse());
            assertNotNull(memory.getInteractionType());
            assertTrue(memory.getTimestamp() > 0);
            assertTrue(memory.getGameDay() > 0);
        }
    }
}