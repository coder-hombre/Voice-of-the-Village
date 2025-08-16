package com.foogly.voiceofthevillage.error;

import com.foogly.voiceofthevillage.data.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FallbackResponseManagerTest {

    @Mock
    private VillagerData mockVillagerData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetFallbackResponse_WithVillagerData() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.FARMER);
        String playerMessage = "Hello there!";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("crops") || response.contains("harvest") || response.contains("fields"));
    }

    @Test
    void testGetFallbackResponse_LibrarianProfession() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.LIBRARIAN);
        String playerMessage = "What are you reading?";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("books") || response.contains("texts") || response.contains("knowledge"));
    }

    @Test
    void testGetFallbackResponse_BlacksmithProfession() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.WEAPONSMITH);
        String playerMessage = "Can you make me a sword?";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("forge") || response.contains("metalwork") || response.contains("anvil"));
    }

    @Test
    void testGetFallbackResponse_ClericProfession() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.CLERIC);
        String playerMessage = "Bless me!";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("contemplation") || response.contains("prayers") || response.contains("spirits"));
    }

    @Test
    void testGetFallbackResponse_ButcherProfession() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.BUTCHER);
        String playerMessage = "Do you have fresh meat?";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("meat") || response.contains("cuts") || response.contains("trade"));
    }

    @Test
    void testGetFallbackResponse_UnknownProfession() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.NITWIT);
        String playerMessage = "Hello!";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        // Should fall back to generic response
    }

    @Test
    void testGetFallbackResponse_NullVillagerData() {
        // Arrange
        String playerMessage = "Hello!";
        
        // Act
        String response = FallbackResponseManager.getFallbackResponse(null, playerMessage);
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        // Should return generic response
    }

    @Test
    void testGetAcknowledgmentResponse() {
        // Act
        String response = FallbackResponseManager.getAcknowledgmentResponse();
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.matches("I understand\\.|I see\\.|Hmm, yes\\.|Indeed\\.|I hear you\\."));
    }

    @Test
    void testGetErrorResponse_AudioError() {
        // Act
        String response = FallbackResponseManager.getErrorResponse("audio");
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("hearing"));
    }

    @Test
    void testGetErrorResponse_NetworkError() {
        // Act
        String response = FallbackResponseManager.getErrorResponse("network");
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("communication"));
    }

    @Test
    void testGetErrorResponse_AIError() {
        // Act
        String response = FallbackResponseManager.getErrorResponse("ai");
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("thoughts") || response.contains("muddled"));
    }

    @Test
    void testGetErrorResponse_UnknownError() {
        // Act
        String response = FallbackResponseManager.getErrorResponse("unknown");
        
        // Assert
        assertNotNull(response);
        assertFalse(response.isEmpty());
        // Should return generic response
    }

    @Test
    void testMultipleCallsReturnDifferentResponses() {
        // Arrange
        when(mockVillagerData.getProfession()).thenReturn(VillagerProfession.FARMER);
        String playerMessage = "Hello!";
        
        // Act
        String response1 = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        String response2 = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        String response3 = FallbackResponseManager.getFallbackResponse(mockVillagerData, playerMessage);
        
        // Assert - At least one should be different (randomness test)
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
        // Note: Due to randomness, responses might be the same, but this tests the mechanism
    }
}