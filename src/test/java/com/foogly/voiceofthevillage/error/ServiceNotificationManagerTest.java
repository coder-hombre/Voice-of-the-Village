package com.foogly.voiceofthevillage.error;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceNotificationManagerTest {

    @Mock
    private ServerPlayer mockPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlayer.getUUID()).thenReturn(UUID.randomUUID());
        when(mockPlayer.getName()).thenReturn(net.minecraft.network.chat.Component.literal("TestPlayer"));
    }

    @AfterEach
    void tearDown() {
        ServiceNotificationManager.shutdown();
    }

    @Test
    void testNotifyServiceUnavailable() {
        // Act
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, "AI Service");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyServiceUnavailable_Cooldown() {
        // Arrange
        String serviceName = "AI Service";
        
        // Act - First notification
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, serviceName);
        
        // Act - Second notification immediately (should be blocked by cooldown)
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, serviceName);
        
        // Assert - Only one notification should be sent
        verify(mockPlayer, times(1)).sendSystemMessage(any());
    }

    @Test
    void testNotifyServiceRecovered() {
        // Act
        ServiceNotificationManager.notifyServiceRecovered(mockPlayer, "AI Service");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyAudioError_Microphone() {
        // Act
        ServiceNotificationManager.notifyAudioError(mockPlayer, "microphone");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyAudioError_Recording() {
        // Act
        ServiceNotificationManager.notifyAudioError(mockPlayer, "recording");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyAudioError_Processing() {
        // Act
        ServiceNotificationManager.notifyAudioError(mockPlayer, "processing");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyAudioError_Unknown() {
        // Act
        ServiceNotificationManager.notifyAudioError(mockPlayer, "unknown");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyNetworkError() {
        // Act
        ServiceNotificationManager.notifyNetworkError(mockPlayer, "speech recognition");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyConfigurationError() {
        // Act
        ServiceNotificationManager.notifyConfigurationError(mockPlayer, "Missing API key");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyGeneralError() {
        // Act
        ServiceNotificationManager.notifyGeneralError(mockPlayer, "villager communication");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testCleanupCooldowns() {
        // Arrange
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, "Test Service");
        
        // Act
        ServiceNotificationManager.cleanupCooldowns();
        
        // Assert - Should not throw exception
        assertDoesNotThrow(() -> ServiceNotificationManager.cleanupCooldowns());
    }

    @Test
    void testStartCleanupScheduler() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(ServiceNotificationManager::startCleanupScheduler);
    }

    @Test
    void testShutdown() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(ServiceNotificationManager::shutdown);
    }

    @Test
    void testMultiplePlayersNotifications() {
        // Arrange
        ServerPlayer mockPlayer2 = mock(ServerPlayer.class);
        when(mockPlayer2.getUUID()).thenReturn(UUID.randomUUID());
        when(mockPlayer2.getName()).thenReturn(net.minecraft.network.chat.Component.literal("TestPlayer2"));
        
        // Act
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, "AI Service");
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer2, "AI Service");
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
        verify(mockPlayer2).sendSystemMessage(any());
    }

    @Test
    void testDifferentServicesNoCooldownInterference() {
        // Act
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, "AI Service");
        ServiceNotificationManager.notifyServiceUnavailable(mockPlayer, "Audio Service");
        
        // Assert - Both notifications should be sent (different services)
        verify(mockPlayer, times(2)).sendSystemMessage(any());
    }
}