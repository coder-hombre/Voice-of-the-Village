package com.foogly.voiceofthevillage.reputation;

import com.foogly.voiceofthevillage.data.ReputationThreshold;
import com.foogly.voiceofthevillage.data.VillagerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BehaviorTriggerTest {

    @Mock
    private ReputationManager reputationManager;

    @Mock
    private Villager villager;

    @Mock
    private Player player;

    @Mock
    private ServerLevel serverLevel;

    @Mock
    private DamageSources damageSources;

    @Mock
    private DamageSource damageSource;

    @Mock
    private IronGolem ironGolem;

    @Mock
    private BlockState blockState;

    private BehaviorTrigger behaviorTrigger;
    private UUID playerId;
    private UUID villagerId;
    private MockedStatic<VillagerDataManager> mockedVillagerDataManager;
    private MockedStatic<EntityType> mockedEntityType;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        behaviorTrigger = new BehaviorTrigger(reputationManager);
        
        playerId = UUID.randomUUID();
        villagerId = UUID.randomUUID();
        
        when(player.getUUID()).thenReturn(playerId);
        when(player.getName()).thenReturn(Component.literal("TestPlayer"));
        when(villager.getUUID()).thenReturn(villagerId);
        when(villager.level()).thenReturn(serverLevel);
        when(villager.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        
        when(player.damageSources()).thenReturn(damageSources);
        when(damageSources.mobAttack(villager)).thenReturn(damageSource);
        
        // Mock static methods
        mockedVillagerDataManager = mockStatic(VillagerDataManager.class);
        mockedVillagerDataManager.when(() -> VillagerDataManager.getVillagerName(villagerId)).thenReturn("TestVillager");
        
        mockedEntityType = mockStatic(EntityType.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedVillagerDataManager != null) {
            mockedVillagerDataManager.close();
        }
        if (mockedEntityType != null) {
            mockedEntityType.close();
        }
    }

    @Test
    void testCheckAndTriggerBehaviorsNeutral() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.NEUTRAL);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        // Should not trigger any behaviors for neutral reputation
        verify(reputationManager, never()).shouldVillagerAttackPlayer(any(), any());
        verify(reputationManager, never()).shouldVillagerSpawnIronGolem(any(), any());
    }

    @Test
    void testCheckAndTriggerBehaviorsUnfriendly() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.UNFRIENDLY);
        when(reputationManager.shouldVillagerAttackPlayer(villager, player)).thenReturn(true);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        verify(reputationManager).shouldVillagerAttackPlayer(villager, player);
        verify(player).hurt(eq(damageSource), eq(1.0f));
        verify(reputationManager).markVillagerAttackedPlayer(villager, player);
    }

    @Test
    void testCheckAndTriggerBehaviorsUnfriendlyAlreadyAttacked() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.UNFRIENDLY);
        when(reputationManager.shouldVillagerAttackPlayer(villager, player)).thenReturn(false);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        verify(reputationManager).shouldVillagerAttackPlayer(villager, player);
        verify(player, never()).hurt(any(), anyFloat());
        verify(reputationManager, never()).markVillagerAttackedPlayer(any(), any());
    }

    @Test
    void testCheckAndTriggerBehaviorsHostile() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.HOSTILE);
        when(reputationManager.shouldVillagerSpawnIronGolem(villager, player)).thenReturn(true);
        
        // Mock iron golem creation
        mockedEntityType.when(() -> EntityType.IRON_GOLEM.create(serverLevel)).thenReturn(ironGolem);
        
        // Mock block states for spawn location
        when(serverLevel.getBlockState(any(BlockPos.class))).thenReturn(blockState);
        when(blockState.isAir()).thenReturn(true);
        when(blockState.isSolid()).thenReturn(true);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        verify(reputationManager).shouldVillagerSpawnIronGolem(villager, player);
        verify(ironGolem).setTarget(player);
        verify(ironGolem).setPlayerCreated(true);
        verify(serverLevel).addFreshEntity(ironGolem);
        verify(reputationManager).markVillagerSpawnedIronGolem(villager, player);
    }

    @Test
    void testCheckAndTriggerBehaviorsHostileAlreadySpawned() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.HOSTILE);
        when(reputationManager.shouldVillagerSpawnIronGolem(villager, player)).thenReturn(false);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        verify(reputationManager).shouldVillagerSpawnIronGolem(villager, player);
        verify(serverLevel, never()).addFreshEntity(any());
        verify(reputationManager, never()).markVillagerSpawnedIronGolem(any(), any());
    }

    @Test
    void testOnVillagerInteraction() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.NEUTRAL);
        
        behaviorTrigger.onVillagerInteraction(villager, player);
        
        verify(reputationManager).getReputationThreshold(villager, player);
    }

    @Test
    void testOnPlayerApproachNeutral() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.NEUTRAL);
        
        behaviorTrigger.onPlayerApproach(villager, player);
        
        verify(reputationManager).getReputationThreshold(villager, player);
        verify(reputationManager, never()).shouldVillagerSpawnIronGolem(any(), any());
    }

    @Test
    void testOnPlayerApproachHostile() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.HOSTILE);
        when(reputationManager.shouldVillagerSpawnIronGolem(villager, player)).thenReturn(true);
        
        // Mock iron golem creation
        mockedEntityType.when(() -> EntityType.IRON_GOLEM.create(serverLevel)).thenReturn(ironGolem);
        
        // Mock block states for spawn location
        when(serverLevel.getBlockState(any(BlockPos.class))).thenReturn(blockState);
        when(blockState.isAir()).thenReturn(true);
        when(blockState.isSolid()).thenReturn(true);
        
        behaviorTrigger.onPlayerApproach(villager, player);
        
        verify(reputationManager).shouldVillagerSpawnIronGolem(villager, player);
        verify(serverLevel).addFreshEntity(ironGolem);
    }

    @Test
    void testGetRandomAttackMessage() {
        String message = behaviorTrigger.getRandomAttackMessage();
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test
    void testGetRandomIronGolemMessage() {
        String message = behaviorTrigger.getRandomIronGolemMessage();
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test
    void testErrorHandlingInCheckAndTriggerBehaviors() {
        when(reputationManager.getReputationThreshold(villager, player)).thenThrow(new RuntimeException("Test error"));
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        });
    }

    @Test
    void testIronGolemSpawnFailsWhenNoSuitableLocation() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.HOSTILE);
        when(reputationManager.shouldVillagerSpawnIronGolem(villager, player)).thenReturn(true);
        
        // Mock iron golem creation
        mockedEntityType.when(() -> EntityType.IRON_GOLEM.create(serverLevel)).thenReturn(ironGolem);
        
        // Mock block states to simulate no suitable location (all blocks are solid)
        when(serverLevel.getBlockState(any(BlockPos.class))).thenReturn(blockState);
        when(blockState.isAir()).thenReturn(false);
        when(blockState.isSolid()).thenReturn(true);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        // Should still mark as spawned even if spawn fails
        verify(reputationManager).markVillagerSpawnedIronGolem(villager, player);
        // But should not actually add the entity
        verify(serverLevel, never()).addFreshEntity(any());
    }

    @Test
    void testIronGolemCreationFails() {
        when(reputationManager.getReputationThreshold(villager, player)).thenReturn(ReputationThreshold.HOSTILE);
        when(reputationManager.shouldVillagerSpawnIronGolem(villager, player)).thenReturn(true);
        
        // Mock iron golem creation to fail
        mockedEntityType.when(() -> EntityType.IRON_GOLEM.create(serverLevel)).thenReturn(null);
        
        behaviorTrigger.checkAndTriggerBehaviors(villager, player);
        
        // Should still mark as spawned even if creation fails
        verify(reputationManager).markVillagerSpawnedIronGolem(villager, player);
        verify(serverLevel, never()).addFreshEntity(any());
    }
}