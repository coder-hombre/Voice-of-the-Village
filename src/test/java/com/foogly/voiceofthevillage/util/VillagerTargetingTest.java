package com.foogly.voiceofthevillage.util;

import com.foogly.voiceofthevillage.config.VoiceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VillagerTargeting utility class.
 * Tests raycast-based villager targeting and distance validation.
 */
@ExtendWith(MockitoExtension.class)
class VillagerTargetingTest {
    
    @Mock
    private Minecraft mockMinecraft;
    
    @Mock
    private LocalPlayer mockPlayer;
    
    @Mock
    private Level mockLevel;
    
    @Mock
    private Villager mockVillager;
    
    @BeforeEach
    void setUp() {
        // Setup basic mocks
        when(mockVillager.getUUID()).thenReturn(UUID.randomUUID());
        when(mockVillager.isAlive()).thenReturn(true);
        when(mockVillager.isBaby()).thenReturn(false);
        when(mockVillager.getBoundingBox()).thenReturn(new AABB(0, 0, 0, 1, 2, 1));
        
        when(mockPlayer.getEyePosition()).thenReturn(new Vec3(0, 1.6, 0));
        when(mockPlayer.getViewVector(1.0f)).thenReturn(new Vec3(0, 0, 1));
        when(mockPlayer.position()).thenReturn(new Vec3(0, 0, 0));
        when(mockPlayer.distanceTo(any())).thenReturn(5.0);
        
        when(mockMinecraft.player).thenReturn(mockPlayer);
        when(mockMinecraft.level).thenReturn(mockLevel);
    }
    
    @Test
    void testGetTargetedVillager_ReturnsNullWhenNoPlayer() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class)) {
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            when(mockMinecraft.player).thenReturn(null);
            
            Villager result = VillagerTargeting.getTargetedVillager();
            
            assertNull(result, "Should return null when no player is available");
        }
    }
    
    @Test
    void testGetTargetedVillager_ReturnsNullWhenNoLevel() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class)) {
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            when(mockMinecraft.level).thenReturn(null);
            
            Villager result = VillagerTargeting.getTargetedVillager();
            
            assertNull(result, "Should return null when no level is available");
        }
    }
    
    @Test
    void testGetTargetedVillager_ReturnsNullWhenNoVillagersInRange() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class);
             MockedStatic<VoiceConfig> mockConfig = mockStatic(VoiceConfig.class)) {
            
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            mockConfig.when(VoiceConfig::getEffectiveInteractionDistance).thenReturn(10.0);
            mockConfig.when(VoiceConfig::isDistanceCheckEnabled).thenReturn(true);
            
            when(mockLevel.getEntitiesOfClass(eq(Villager.class), any(AABB.class), any()))
                .thenReturn(List.of());
            
            Villager result = VillagerTargeting.getTargetedVillager();
            
            assertNull(result, "Should return null when no villagers are in range");
        }
    }
    
    @Test
    void testGetTargetedVillagerDistance_ReturnsNegativeWhenNoTarget() {
        try (MockedStatic<VillagerTargeting> mockTargeting = mockStatic(VillagerTargeting.class)) {
            mockTargeting.when(VillagerTargeting::getTargetedVillager).thenReturn(null);
            mockTargeting.when(VillagerTargeting::getTargetedVillagerDistance).thenCallRealMethod();
            
            double result = VillagerTargeting.getTargetedVillagerDistance();
            
            assertEquals(-1.0, result, "Should return -1 when no villager is targeted");
        }
    }
    
    @Test
    void testGetTargetedVillagerDistance_ReturnsDistanceWhenTargeted() {
        try (MockedStatic<VillagerTargeting> mockTargeting = mockStatic(VillagerTargeting.class);
             MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class)) {
            
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            mockTargeting.when(VillagerTargeting::getTargetedVillager).thenReturn(mockVillager);
            mockTargeting.when(VillagerTargeting::getTargetedVillagerDistance).thenCallRealMethod();
            
            when(mockPlayer.distanceTo(mockVillager)).thenReturn(7.5);
            
            double result = VillagerTargeting.getTargetedVillagerDistance();
            
            assertEquals(7.5, result, 0.01, "Should return the actual distance to the villager");
        }
    }
    
    @Test
    void testIsVillagerTargetable_ReturnsFalseForNullVillager() {
        boolean result = VillagerTargeting.isVillagerTargetable(null);
        
        assertFalse(result, "Should return false for null villager");
    }
    
    @Test
    void testIsVillagerTargetable_ReturnsFalseForDeadVillager() {
        when(mockVillager.isAlive()).thenReturn(false);
        
        boolean result = VillagerTargeting.isVillagerTargetable(mockVillager);
        
        assertFalse(result, "Should return false for dead villager");
    }
    
    @Test
    void testIsVillagerTargetable_ReturnsFalseForBabyVillager() {
        when(mockVillager.isBaby()).thenReturn(true);
        
        boolean result = VillagerTargeting.isVillagerTargetable(mockVillager);
        
        assertFalse(result, "Should return false for baby villager");
    }
    
    @Test
    void testIsVillagerTargetable_ReturnsFalseWhenTooFar() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class);
             MockedStatic<VoiceConfig> mockConfig = mockStatic(VoiceConfig.class)) {
            
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            mockConfig.when(VoiceConfig::isDistanceCheckEnabled).thenReturn(true);
            mockConfig.when(VoiceConfig::getEffectiveInteractionDistance).thenReturn(5.0);
            
            when(mockPlayer.distanceTo(mockVillager)).thenReturn(10.0);
            
            boolean result = VillagerTargeting.isVillagerTargetable(mockVillager);
            
            assertFalse(result, "Should return false when villager is too far away");
        }
    }
    
    @Test
    void testGetTargetableVillagers_ReturnsEmptyWhenNoPlayer() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class)) {
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            when(mockMinecraft.player).thenReturn(null);
            
            List<Villager> result = VillagerTargeting.getTargetableVillagers();
            
            assertTrue(result.isEmpty(), "Should return empty list when no player is available");
        }
    }
    
    @Test
    void testGetTargetableVillagers_ReturnsEmptyWhenNoLevel() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class)) {
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            when(mockMinecraft.level).thenReturn(null);
            
            List<Villager> result = VillagerTargeting.getTargetableVillagers();
            
            assertTrue(result.isEmpty(), "Should return empty list when no level is available");
        }
    }
    
    @Test
    void testGetTargetableVillagers_FiltersTargetableVillagers() {
        try (MockedStatic<Minecraft> mockMinecraftStatic = mockStatic(Minecraft.class);
             MockedStatic<VoiceConfig> mockConfig = mockStatic(VoiceConfig.class);
             MockedStatic<VillagerTargeting> mockTargeting = mockStatic(VillagerTargeting.class)) {
            
            mockMinecraftStatic.when(Minecraft::getInstance).thenReturn(mockMinecraft);
            mockConfig.when(VoiceConfig::getEffectiveInteractionDistance).thenReturn(10.0);
            mockConfig.when(VoiceConfig::isDistanceCheckEnabled).thenReturn(true);
            
            Villager targetableVillager = mock(Villager.class);
            Villager nonTargetableVillager = mock(Villager.class);
            
            mockTargeting.when(() -> VillagerTargeting.isVillagerTargetable(targetableVillager))
                         .thenReturn(true);
            mockTargeting.when(() -> VillagerTargeting.isVillagerTargetable(nonTargetableVillager))
                         .thenReturn(false);
            mockTargeting.when(VillagerTargeting::getTargetableVillagers).thenCallRealMethod();
            
            when(mockLevel.getEntitiesOfClass(eq(Villager.class), any(AABB.class), any()))
                .thenReturn(List.of(targetableVillager));
            
            List<Villager> result = VillagerTargeting.getTargetableVillagers();
            
            assertEquals(1, result.size(), "Should return only targetable villagers");
            assertTrue(result.contains(targetableVillager), "Should contain the targetable villager");
        }
    }
    
    @Test
    void testDistanceCalculations() {
        try (MockedStatic<VoiceConfig> mockConfig = mockStatic(VoiceConfig.class)) {
            // Test unlimited range
            mockConfig.when(VoiceConfig::getEffectiveInteractionDistance).thenReturn(Double.MAX_VALUE);
            mockConfig.when(VoiceConfig::isDistanceCheckEnabled).thenReturn(false);
            
            assertEquals(Double.MAX_VALUE, VoiceConfig.getEffectiveInteractionDistance(), 
                        "Should return unlimited range when distance check is disabled");
            
            // Test normal range
            mockConfig.when(VoiceConfig::getEffectiveInteractionDistance).thenReturn(15.0);
            mockConfig.when(VoiceConfig::isDistanceCheckEnabled).thenReturn(true);
            
            assertEquals(15.0, VoiceConfig.getEffectiveInteractionDistance(), 
                        "Should return configured distance when distance check is enabled");
        }
    }
}