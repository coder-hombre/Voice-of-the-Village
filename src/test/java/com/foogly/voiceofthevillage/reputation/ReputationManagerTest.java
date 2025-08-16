package com.foogly.voiceofthevillage.reputation;

import com.foogly.voiceofthevillage.data.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReputationManagerTest {

    @Mock
    private Villager villager;

    @Mock
    private Player player;

    private ReputationManager reputationManager;
    private VillagerData villagerData;
    private UUID playerId;
    private UUID villagerId;
    private MockedStatic<VillagerDataManager> mockedVillagerDataManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reputationManager = new ReputationManager();
        
        playerId = UUID.randomUUID();
        villagerId = UUID.randomUUID();
        
        villagerData = new VillagerData(villagerId, "TestVillager", Gender.MALE, PersonalityType.FRIENDLY);
        
        when(player.getUUID()).thenReturn(playerId);
        when(player.getName()).thenReturn(net.minecraft.network.chat.Component.literal("TestPlayer"));
        when(villager.getUUID()).thenReturn(villagerId);
        
        // Mock static methods
        mockedVillagerDataManager = mockStatic(VillagerDataManager.class);
        mockedVillagerDataManager.when(() -> VillagerDataManager.getOrCreateVillagerData(villager)).thenReturn(villagerData);
        mockedVillagerDataManager.when(() -> VillagerDataManager.getVillagerData(villagerId)).thenReturn(villagerData);
    }

    @AfterEach
    void tearDown() {
        if (mockedVillagerDataManager != null) {
            mockedVillagerDataManager.close();
        }
    }

    @Test
    void testAddReputationEvent() {
        // Test adding a reputation event
        reputationManager.addReputationEvent(villager, player, ReputationEventType.POSITIVE_CONVERSATION, "Test conversation");
        
        // Verify the event was added to villager data
        ReputationData reputation = villagerData.getReputation(playerId);
        assertNotNull(reputation);
        assertEquals(ReputationEventType.POSITIVE_CONVERSATION.getDefaultScoreChange(), reputation.getScore());
        assertEquals(1, reputation.getEvents().size());
        
        // Verify save was called
        mockedVillagerDataManager.verify(() -> VillagerDataManager.updateVillagerData(eq(villagerData)));
    }

    @Test
    void testAddReputationEventWithCustomScore() {
        // Test adding a reputation event with custom score
        int customScore = 15;
        reputationManager.addReputationEvent(villager, player, ReputationEventType.POSITIVE_CONVERSATION, 
                                           customScore, "Custom score conversation");
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertNotNull(reputation);
        assertEquals(customScore, reputation.getScore());
    }

    @Test
    void testGetReputationScore() {
        // Add some reputation events
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.POSITIVE_CONVERSATION, 5, "Test"));
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.RUDE_BEHAVIOR, -5, "Test"));
        
        int score = reputationManager.getReputationScore(villager, player);
        assertEquals(0, score); // 5 + (-5) = 0
    }

    @Test
    void testGetReputationScoreForNewVillager() {
        mockedVillagerDataManager.when(() -> VillagerDataManager.getVillagerData(villagerId)).thenReturn(null);
        
        int score = reputationManager.getReputationScore(villager, player);
        assertEquals(0, score); // Should return neutral for new villagers
    }

    @Test
    void testGetReputationThreshold() {
        // Test different reputation thresholds
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.PLAYER_HURT_VILLAGER, -50, "Hurt"));
        
        ReputationThreshold threshold = reputationManager.getReputationThreshold(villager, player);
        assertEquals(ReputationThreshold.UNFRIENDLY, threshold);
    }

    @Test
    void testShouldVillagerAttackPlayer() {
        // Set reputation to unfriendly level
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.PLAYER_HURT_VILLAGER, -50, "Hurt"));
        
        assertTrue(reputationManager.shouldVillagerAttackPlayer(villager, player));
        
        // After marking as attacked, should return false
        reputationManager.markVillagerAttackedPlayer(villager, player);
        assertFalse(reputationManager.shouldVillagerAttackPlayer(villager, player));
    }

    @Test
    void testShouldVillagerSpawnIronGolem() {
        // Set reputation to hostile level
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.THEFT, -85, "Stole items"));
        
        assertTrue(reputationManager.shouldVillagerSpawnIronGolem(villager, player));
        
        // After marking as spawned, should return false
        reputationManager.markVillagerSpawnedIronGolem(villager, player);
        assertFalse(reputationManager.shouldVillagerSpawnIronGolem(villager, player));
    }

    @Test
    void testMarkVillagerAttackedPlayer() {
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.PLAYER_HURT_VILLAGER, -50, "Hurt"));
        
        reputationManager.markVillagerAttackedPlayer(villager, player);
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertTrue(reputation.hasAttackedPlayer());
        mockedVillagerDataManager.verify(() -> VillagerDataManager.updateVillagerData(eq(villagerData)));
    }

    @Test
    void testMarkVillagerSpawnedIronGolem() {
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.THEFT, -85, "Stole items"));
        
        reputationManager.markVillagerSpawnedIronGolem(villager, player);
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertTrue(reputation.hasSpawnedIronGolem());
        mockedVillagerDataManager.verify(() -> VillagerDataManager.updateVillagerData(eq(villagerData)));
    }

    @Test
    void testGetReputationModifier() {
        // Test different reputation modifiers
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.THEFT, -85, "Stole items"));
        
        String modifier = reputationManager.getReputationModifier(villager, player);
        assertEquals("extremely hostile and angry", modifier);
        
        // Reset and test friendly
        villagerData.getReputation(playerId).setScore(50);
        modifier = reputationManager.getReputationModifier(villager, player);
        assertEquals("friendly and helpful", modifier);
    }

    @Test
    void testProcessConversationReputationPositive() {
        String positiveText = "Thank you so much for your help!";
        
        reputationManager.processConversationReputation(villager, player, positiveText);
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertNotNull(reputation);
        assertTrue(reputation.getScore() > 0);
        
        // Check that the event was added
        assertEquals(1, reputation.getEvents().size());
        assertEquals(ReputationEventType.POLITE_BEHAVIOR, reputation.getEvents().get(0).getEventType());
    }

    @Test
    void testProcessConversationReputationNegative() {
        String negativeText = "You're so stupid and useless!";
        
        reputationManager.processConversationReputation(villager, player, negativeText);
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertNotNull(reputation);
        assertTrue(reputation.getScore() < 0);
        
        // Check that the event was added
        assertEquals(1, reputation.getEvents().size());
        assertEquals(ReputationEventType.RUDE_BEHAVIOR, reputation.getEvents().get(0).getEventType());
    }

    @Test
    void testProcessConversationReputationNeutral() {
        String neutralText = "What items do you have for trade?";
        
        reputationManager.processConversationReputation(villager, player, neutralText);
        
        ReputationData reputation = villagerData.getReputation(playerId);
        assertNotNull(reputation);
        assertEquals(1, reputation.getScore()); // Small positive boost for neutral conversation
        
        // Check that the event was added
        assertEquals(1, reputation.getEvents().size());
        assertEquals(ReputationEventType.POSITIVE_CONVERSATION, reputation.getEvents().get(0).getEventType());
    }

    @Test
    void testResetReputationFlags() {
        // Set up reputation with flags
        villagerData.addReputationEvent(playerId, "TestPlayer", 
            new ReputationEvent(ReputationEventType.THEFT, -85, "Stole items"));
        
        ReputationData reputation = villagerData.getReputation(playerId);
        reputation.setHasAttackedPlayer(true);
        reputation.setHasSpawnedIronGolem(true);
        
        // Reset flags
        reputationManager.resetReputationFlags(villager, player);
        
        assertFalse(reputation.hasAttackedPlayer());
        assertFalse(reputation.hasSpawnedIronGolem());
        mockedVillagerDataManager.verify(() -> VillagerDataManager.updateVillagerData(eq(villagerData)));
    }

    @Test
    void testGetReputationDataReturnsNull() {
        mockedVillagerDataManager.when(() -> VillagerDataManager.getVillagerData(villagerId)).thenReturn(null);
        
        ReputationData reputation = reputationManager.getReputationData(villager, player);
        assertNull(reputation);
    }

    @Test
    void testErrorHandlingInAddReputationEvent() {
        // Test error handling when villager data manager throws exception
        mockedVillagerDataManager.when(() -> VillagerDataManager.getOrCreateVillagerData(villager)).thenThrow(new RuntimeException("Test error"));
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            reputationManager.addReputationEvent(villager, player, ReputationEventType.POSITIVE_CONVERSATION, "Test");
        });
    }

    @Test
    void testErrorHandlingInGetReputationScore() {
        // Test error handling when villager data manager throws exception
        mockedVillagerDataManager.when(() -> VillagerDataManager.getVillagerData(villagerId)).thenThrow(new RuntimeException("Test error"));
        
        // Should return 0 (neutral) on error
        int score = reputationManager.getReputationScore(villager, player);
        assertEquals(0, score);
    }
}