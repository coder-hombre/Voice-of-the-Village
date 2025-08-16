package com.foogly.voiceofthevillage.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryManager class.
 */
class MemoryManagerTest {
    @TempDir
    Path tempDir;

    private DataPersistence dataPersistence;
    private TestableMemoryManager memoryManager;
    private VillagerData testVillagerData;
    private UUID testVillagerId;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        dataPersistence = new DataPersistence(tempDir);
        memoryManager = new TestableMemoryManager(dataPersistence);
        testVillagerId = UUID.randomUUID();
        testPlayerId = UUID.randomUUID();
        testVillagerData = new VillagerData(testVillagerId, "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
    }

    /**
     * Testable version of MemoryManager that allows controlling the current game day.
     */
    private static class TestableMemoryManager extends MemoryManager {
        private long currentGameDay = 100L; // Default test day

        public TestableMemoryManager(DataPersistence dataPersistence) {
            super(dataPersistence);
        }

        public void setCurrentGameDay(long gameDay) {
            this.currentGameDay = gameDay;
        }

        @Override
        protected long getCurrentGameDay() {
            return currentGameDay;
        }
    }

    @Test
    void testCleanupVillagerMemories() throws IOException {
        // Set current game day
        long currentDay = 100L;
        memoryManager.setCurrentGameDay(currentDay);
        
        // Add some memories with different ages
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player", "Old message", "Old response", InteractionType.TEXT, currentDay - 50)); // Very old
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player", "Recent message", "Recent response", InteractionType.TEXT, currentDay - 5)); // Recent
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player", "Another old", "Another old response", InteractionType.TEXT, currentDay - 40)); // Old
        
        // Save the villager data
        dataPersistence.saveVillagerData(testVillagerData);
        
        // Cleanup memories (using 30 day retention)
        int removed = memoryManager.cleanupVillagerMemories(testVillagerId, 30);
        
        // Should remove 2 old memories (older than 30 days from current day 100)
        assertEquals(2, removed);
        
        // Verify the villager data was updated
        VillagerData updated = dataPersistence.loadVillagerData(testVillagerId);
        assertNotNull(updated);
        assertEquals(1, updated.getMemories().size());
        assertEquals("Recent message", updated.getMemories().get(0).getPlayerMessage());
    }

    @Test
    void testCleanupNonExistentVillager() throws IOException {
        UUID nonExistentId = UUID.randomUUID();
        
        int removed = memoryManager.cleanupVillagerMemories(nonExistentId, 30);
        
        assertEquals(0, removed);
    }

    @Test
    void testCleanupWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.cleanupVillagerMemories(null, 30);
        });
    }

    @Test
    void testForceCleanupOlderThan() throws IOException {
        // Set current game day
        long currentDay = 100L;
        memoryManager.setCurrentGameDay(currentDay);
        
        // Create multiple villagers with memories
        UUID villager1 = UUID.randomUUID();
        UUID villager2 = UUID.randomUUID();
        
        VillagerData data1 = new VillagerData(villager1, "Villager1", Gender.MALE, PersonalityType.FRIENDLY);
        VillagerData data2 = new VillagerData(villager2, "Villager2", Gender.FEMALE, PersonalityType.GRUMPY);
        
        // Add memories to villager 1
        data1.addMemory(new InteractionMemory(testPlayerId, "Player", "Very old", "Response", InteractionType.TEXT, currentDay - 60));
        data1.addMemory(new InteractionMemory(testPlayerId, "Player", "Old", "Response", InteractionType.TEXT, currentDay - 25));
        data1.addMemory(new InteractionMemory(testPlayerId, "Player", "Recent", "Response", InteractionType.TEXT, currentDay - 5));
        
        // Add memories to villager 2
        data2.addMemory(new InteractionMemory(testPlayerId, "Player", "Very old", "Response", InteractionType.TEXT, currentDay - 70));
        data2.addMemory(new InteractionMemory(testPlayerId, "Player", "Recent", "Response", InteractionType.TEXT, currentDay - 10));
        
        dataPersistence.saveVillagerData(data1);
        dataPersistence.saveVillagerData(data2);
        
        // Force cleanup memories older than 20 days
        int totalRemoved = memoryManager.forceCleanupOlderThan(20);
        
        // Should remove 3 memories total (2 from villager1, 1 from villager2)
        assertEquals(3, totalRemoved);
        
        // Verify the data was updated
        VillagerData updated1 = dataPersistence.loadVillagerData(villager1);
        VillagerData updated2 = dataPersistence.loadVillagerData(villager2);
        
        assertEquals(1, updated1.getMemories().size()); // Only recent memory remains
        assertEquals(1, updated2.getMemories().size()); // Only recent memory remains
    }

    @Test
    void testForceCleanupWithNegativeAge() {
        assertThrows(IllegalArgumentException.class, () -> {
            memoryManager.forceCleanupOlderThan(-1);
        });
    }

    @Test
    void testGetVillagerMemoryStatistics() throws IOException {
        // Set current game day
        memoryManager.setCurrentGameDay(100L);
        
        // Add various types of memories
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player1", "Message1", "Response1", InteractionType.VOICE, 90L));
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player1", "Message2", "Response2", InteractionType.TEXT, 95L));
        
        UUID player2 = UUID.randomUUID();
        testVillagerData.addMemory(new InteractionMemory(player2, "Player2", "Message3", "Response3", InteractionType.TRADE, 98L));
        
        dataPersistence.saveVillagerData(testVillagerData);
        
        MemoryManager.MemoryStatistics stats = memoryManager.getVillagerMemoryStatistics(testVillagerId, 30);
        
        assertNotNull(stats);
        assertEquals(testVillagerId, stats.getVillagerId());
        assertEquals(3, stats.getTotalMemories());
        assertEquals(2, stats.getUniquePlayerCount());
        assertEquals(90L, stats.getOldestMemoryDay());
        assertEquals(98L, stats.getNewestMemoryDay());
        
        // Check memory type counts
        assertEquals(1, stats.getMemoryTypeCount().get(InteractionType.VOICE).intValue());
        assertEquals(1, stats.getMemoryTypeCount().get(InteractionType.TEXT).intValue());
        assertEquals(1, stats.getMemoryTypeCount().get(InteractionType.TRADE).intValue());
    }

    @Test
    void testGetVillagerMemoryStatisticsNonExistent() throws IOException {
        UUID nonExistentId = UUID.randomUUID();
        
        MemoryManager.MemoryStatistics stats = memoryManager.getVillagerMemoryStatistics(nonExistentId, 30);
        
        assertNull(stats);
    }

    @Test
    void testGetVillagerMemoryStatisticsWithNull() throws IOException {
        MemoryManager.MemoryStatistics stats = memoryManager.getVillagerMemoryStatistics(null, 30);
        
        assertNull(stats);
    }

    @Test
    void testGetOverallStatistics() throws IOException {
        // Set current game day
        memoryManager.setCurrentGameDay(100L);
        
        // Create multiple villagers with memories
        UUID villager1 = UUID.randomUUID();
        UUID villager2 = UUID.randomUUID();
        
        VillagerData data1 = new VillagerData(villager1, "Villager1", Gender.MALE, PersonalityType.FRIENDLY);
        VillagerData data2 = new VillagerData(villager2, "Villager2", Gender.FEMALE, PersonalityType.GRUMPY);
        
        // Add memories
        data1.addMemory(new InteractionMemory(testPlayerId, "Player", "Message1", "Response1", InteractionType.VOICE, 95L));
        data1.addMemory(new InteractionMemory(testPlayerId, "Player", "Message2", "Response2", InteractionType.TEXT, 98L));
        
        data2.addMemory(new InteractionMemory(testPlayerId, "Player", "Message3", "Response3", InteractionType.TRADE, 97L));
        
        dataPersistence.saveVillagerData(data1);
        dataPersistence.saveVillagerData(data2);
        
        MemoryManager.OverallMemoryStatistics stats = memoryManager.getOverallStatistics(30);
        
        assertNotNull(stats);
        assertEquals(2, stats.getTotalVillagers());
        assertEquals(3, stats.getTotalMemories());
        assertTrue(stats.getTotalActiveMemories() >= 0);
        assertTrue(stats.getTotalExpiredMemories() >= 0);
        assertEquals(stats.getTotalMemories(), stats.getTotalActiveMemories() + stats.getTotalExpiredMemories());
    }

    @Test
    void testPerformAutomaticCleanup() throws IOException {
        // Set current game day
        long currentDay = 100L;
        memoryManager.setCurrentGameDay(currentDay);
        
        // Create villager with old memories
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player", "Very old", "Response", InteractionType.TEXT, currentDay - 50));
        testVillagerData.addMemory(new InteractionMemory(testPlayerId, "Player", "Recent", "Response", InteractionType.TEXT, currentDay - 5));
        
        dataPersistence.saveVillagerData(testVillagerData);
        
        // Perform automatic cleanup with 30 day retention
        memoryManager.performAutomaticCleanup(30);
        
        // Verify cleanup occurred - should remove the very old memory (50 days old > 30 day retention)
        VillagerData updated = dataPersistence.loadVillagerData(testVillagerId);
        assertNotNull(updated);
        assertEquals(1, updated.getMemories().size());
        assertEquals("Recent", updated.getMemories().get(0).getPlayerMessage());
    }

    @Test
    void testShutdown() {
        // Test that shutdown doesn't throw exceptions
        assertDoesNotThrow(() -> {
            memoryManager.shutdown();
        });
    }

    @Test
    void testMemoryStatisticsGetters() {
        UUID villagerId = UUID.randomUUID();
        MemoryManager.MemoryStatistics stats = new MemoryManager.MemoryStatistics(
            villagerId, 10, 3, 50L, 100L, 
            java.util.Map.of(InteractionType.VOICE, 5, InteractionType.TEXT, 5), 2
        );
        
        assertEquals(villagerId, stats.getVillagerId());
        assertEquals(10, stats.getTotalMemories());
        assertEquals(3, stats.getExpiredMemories());
        assertEquals(7, stats.getActiveMemories());
        assertEquals(50L, stats.getOldestMemoryDay());
        assertEquals(100L, stats.getNewestMemoryDay());
        assertEquals(2, stats.getUniquePlayerCount());
        
        assertEquals(5, stats.getMemoryTypeCount().get(InteractionType.VOICE).intValue());
        assertEquals(5, stats.getMemoryTypeCount().get(InteractionType.TEXT).intValue());
    }

    @Test
    void testOverallMemoryStatisticsGetters() {
        MemoryManager.OverallMemoryStatistics stats = new MemoryManager.OverallMemoryStatistics(
            5, 100, 20, 12345L, 15, 50, 200
        );
        
        assertEquals(5, stats.getTotalVillagers());
        assertEquals(100, stats.getTotalMemories());
        assertEquals(20, stats.getTotalExpiredMemories());
        assertEquals(80, stats.getTotalActiveMemories());
        assertEquals(12345L, stats.getLastCleanupTime());
        assertEquals(15, stats.getLastCleanupRemovedCount());
        assertEquals(50, stats.getTotalMemoriesProcessed());
        assertEquals(200, stats.getTotalMemoriesRemoved());
    }
}