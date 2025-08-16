package com.foogly.voiceofthevillage.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DataPersistence class.
 */
class DataPersistenceTest {
    @TempDir
    Path tempDir;

    private DataPersistence dataPersistence;
    private VillagerData testVillagerData;
    private UUID testVillagerId;

    @BeforeEach
    void setUp() {
        dataPersistence = new DataPersistence(tempDir);
        testVillagerId = UUID.randomUUID();
        testVillagerData = new VillagerData(testVillagerId, "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
    }

    @Test
    void testSaveAndLoadVillagerData() throws IOException {
        // Save villager data
        dataPersistence.saveVillagerData(testVillagerData);
        
        // Verify it was saved
        assertTrue(dataPersistence.hasVillagerData(testVillagerId));
        
        // Load villager data
        VillagerData loaded = dataPersistence.loadVillagerData(testVillagerId);
        
        assertNotNull(loaded);
        assertEquals(testVillagerId, loaded.getVillagerId());
        assertEquals("TestVillager", loaded.getOriginalName());
        assertEquals(Gender.MALE, loaded.getGender());
        assertEquals(PersonalityType.FRIENDLY, loaded.getPersonality());
    }

    @Test
    void testLoadNonExistentVillagerData() throws IOException {
        UUID nonExistentId = UUID.randomUUID();
        
        VillagerData loaded = dataPersistence.loadVillagerData(nonExistentId);
        
        assertNull(loaded);
        assertFalse(dataPersistence.hasVillagerData(nonExistentId));
    }

    @Test
    void testDeleteVillagerData() throws IOException {
        // Save villager data
        dataPersistence.saveVillagerData(testVillagerData);
        assertTrue(dataPersistence.hasVillagerData(testVillagerId));
        
        // Delete villager data
        dataPersistence.deleteVillagerData(testVillagerId);
        
        // Verify it was deleted
        assertFalse(dataPersistence.hasVillagerData(testVillagerId));
        assertNull(dataPersistence.loadVillagerData(testVillagerId));
    }

    @Test
    void testGetAllVillagerIds() throws IOException {
        UUID villager1 = UUID.randomUUID();
        UUID villager2 = UUID.randomUUID();
        UUID villager3 = UUID.randomUUID();
        
        VillagerData data1 = new VillagerData(villager1, "Villager1", Gender.MALE, PersonalityType.FRIENDLY);
        VillagerData data2 = new VillagerData(villager2, "Villager2", Gender.FEMALE, PersonalityType.GRUMPY);
        VillagerData data3 = new VillagerData(villager3, "Villager3", Gender.MALE, PersonalityType.WISE);
        
        dataPersistence.saveVillagerData(data1);
        dataPersistence.saveVillagerData(data2);
        dataPersistence.saveVillagerData(data3);
        
        Set<UUID> allIds = dataPersistence.getAllVillagerIds();
        
        assertEquals(3, allIds.size());
        assertTrue(allIds.contains(villager1));
        assertTrue(allIds.contains(villager2));
        assertTrue(allIds.contains(villager3));
    }

    @Test
    void testCaching() throws IOException {
        // Save villager data
        dataPersistence.saveVillagerData(testVillagerData);
        
        // Load once (should cache)
        VillagerData loaded1 = dataPersistence.loadVillagerData(testVillagerId);
        assertNotNull(loaded1);
        
        // Load again (should use cache)
        VillagerData loaded2 = dataPersistence.loadVillagerData(testVillagerId);
        assertNotNull(loaded2);
        
        // Verify cache is working
        assertTrue(dataPersistence.getCacheSize() > 0);
        
        // Clear cache
        dataPersistence.clearCache();
        assertEquals(0, dataPersistence.getCacheSize());
    }

    @Test
    void testCreateBackup() throws IOException {
        // Save some villager data
        dataPersistence.saveVillagerData(testVillagerData);
        
        UUID villager2 = UUID.randomUUID();
        VillagerData data2 = new VillagerData(villager2, "Villager2", Gender.FEMALE, PersonalityType.GRUMPY);
        dataPersistence.saveVillagerData(data2);
        
        // Create backup
        dataPersistence.createBackup();
        
        // Verify backup directory exists and contains files
        assertTrue(dataPersistence.getBackupDirectory().toFile().exists());
        
        // The backup should contain subdirectories with timestamp names
        String[] backupContents = dataPersistence.getBackupDirectory().toFile().list();
        assertNotNull(backupContents);
        assertTrue(backupContents.length > 0);
    }

    @Test
    void testSaveNullVillagerData() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataPersistence.saveVillagerData(null);
        });
    }

    @Test
    void testSaveVillagerDataWithNullId() {
        VillagerData dataWithNullId = new VillagerData();
        dataWithNullId.setVillagerId(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            dataPersistence.saveVillagerData(dataWithNullId);
        });
    }

    @Test
    void testLoadWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataPersistence.loadVillagerData(null);
        });
    }

    @Test
    void testDeleteWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataPersistence.deleteVillagerData(null);
        });
    }

    @Test
    void testHasVillagerDataWithNullId() {
        assertFalse(dataPersistence.hasVillagerData(null));
    }

    @Test
    void testComplexVillagerData() throws IOException {
        // Create villager data with memories and reputation
        UUID playerId = UUID.randomUUID();
        testVillagerData.addMemory(new InteractionMemory(playerId, "TestPlayer", "Hello", "Hi there!", InteractionType.VOICE, 1L));
        testVillagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 5, "Nice chat"));
        
        // Save and load
        dataPersistence.saveVillagerData(testVillagerData);
        VillagerData loaded = dataPersistence.loadVillagerData(testVillagerId);
        
        assertNotNull(loaded);
        assertEquals(1, loaded.getMemories().size());
        assertEquals(1, loaded.getPlayerReputations().size());
        
        InteractionMemory memory = loaded.getMemories().get(0);
        assertEquals(playerId, memory.getPlayerId());
        assertEquals("TestPlayer", memory.getPlayerName());
        assertEquals("Hello", memory.getPlayerMessage());
        assertEquals("Hi there!", memory.getVillagerResponse());
        assertEquals(InteractionType.VOICE, memory.getInteractionType());
        
        ReputationData reputation = loaded.getReputation(playerId);
        assertNotNull(reputation);
        assertEquals(5, reputation.getScore());
        assertEquals(1, reputation.getEvents().size());
    }

    @Test
    void testDirectoryCreation() {
        // Verify directories were created
        assertTrue(dataPersistence.getDataDirectory().toFile().exists());
        assertTrue(dataPersistence.getBackupDirectory().toFile().exists());
    }
}