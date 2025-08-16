package com.foogly.voiceofthevillage.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpeakingIndicator functionality.
 * Tests visibility state management and villager name handling.
 */
class SpeakingIndicatorTest {
    
    private SpeakingIndicator speakingIndicator;
    
    @BeforeEach
    void setUp() {
        speakingIndicator = SpeakingIndicator.getInstance();
        // Ensure clean state for each test
        speakingIndicator.hideSpeakingIndicator();
    }
    
    @Test
    void testGetInstance_ReturnsSameInstance() {
        SpeakingIndicator instance1 = SpeakingIndicator.getInstance();
        SpeakingIndicator instance2 = SpeakingIndicator.getInstance();
        
        assertSame(instance1, instance2, "getInstance should return the same singleton instance");
    }
    
    @Test
    void testInitialState_IsNotVisible() {
        assertFalse(speakingIndicator.isVisible(), "Speaking indicator should initially be hidden");
        assertEquals("", speakingIndicator.getVillagerName(), "Villager name should initially be empty");
    }
    
    @Test
    void testShowSpeakingIndicator_MakesVisible() {
        String villagerName = "TestVillager";
        
        speakingIndicator.showSpeakingIndicator(villagerName);
        
        assertTrue(speakingIndicator.isVisible(), "Speaking indicator should be visible after showing");
        assertEquals(villagerName, speakingIndicator.getVillagerName(), 
                    "Villager name should be set correctly");
    }
    
    @Test
    void testShowSpeakingIndicator_HandlesNullName() {
        speakingIndicator.showSpeakingIndicator(null);
        
        assertTrue(speakingIndicator.isVisible(), "Speaking indicator should be visible even with null name");
        assertEquals("Unknown Villager", speakingIndicator.getVillagerName(), 
                    "Should use default name for null input");
    }
    
    @Test
    void testShowSpeakingIndicator_HandlesEmptyName() {
        speakingIndicator.showSpeakingIndicator("");
        
        assertTrue(speakingIndicator.isVisible(), "Speaking indicator should be visible with empty name");
        assertEquals("", speakingIndicator.getVillagerName(), 
                    "Should preserve empty string name");
    }
    
    @Test
    void testHideSpeakingIndicator_MakesInvisible() {
        // First show the indicator
        speakingIndicator.showSpeakingIndicator("TestVillager");
        assertTrue(speakingIndicator.isVisible(), "Precondition: indicator should be visible");
        
        // Then hide it
        speakingIndicator.hideSpeakingIndicator();
        
        assertFalse(speakingIndicator.isVisible(), "Speaking indicator should be hidden after hiding");
        assertEquals("", speakingIndicator.getVillagerName(), 
                    "Villager name should be cleared after hiding");
    }
    
    @Test
    void testShowSpeakingIndicator_UpdatesExistingIndicator() {
        String firstVillager = "FirstVillager";
        String secondVillager = "SecondVillager";
        
        // Show first villager
        speakingIndicator.showSpeakingIndicator(firstVillager);
        assertEquals(firstVillager, speakingIndicator.getVillagerName(), 
                    "Should show first villager name");
        
        // Update to second villager
        speakingIndicator.showSpeakingIndicator(secondVillager);
        assertEquals(secondVillager, speakingIndicator.getVillagerName(), 
                    "Should update to second villager name");
        assertTrue(speakingIndicator.isVisible(), "Should remain visible during update");
    }
    
    @Test
    void testHideSpeakingIndicator_WhenAlreadyHidden() {
        // Ensure it's hidden
        speakingIndicator.hideSpeakingIndicator();
        assertFalse(speakingIndicator.isVisible(), "Precondition: should be hidden");
        
        // Hide again - should not cause issues
        assertDoesNotThrow(() -> speakingIndicator.hideSpeakingIndicator(), 
                          "Hiding already hidden indicator should not throw exception");
        
        assertFalse(speakingIndicator.isVisible(), "Should remain hidden");
    }
    
    @Test
    void testVillagerNamePersistence() {
        String villagerName = "PersistentVillager";
        
        speakingIndicator.showSpeakingIndicator(villagerName);
        
        // Name should persist while visible
        assertEquals(villagerName, speakingIndicator.getVillagerName(), 
                    "Villager name should persist while indicator is visible");
        
        // Multiple calls to getVillagerName should return the same value
        assertEquals(villagerName, speakingIndicator.getVillagerName(), 
                    "Villager name should be consistent across multiple calls");
        assertEquals(villagerName, speakingIndicator.getVillagerName(), 
                    "Villager name should be consistent across multiple calls");
    }
    
    @Test
    void testStateTransitions() {
        String villagerName = "TransitionVillager";
        
        // Initial state: hidden
        assertFalse(speakingIndicator.isVisible());
        assertEquals("", speakingIndicator.getVillagerName());
        
        // Transition to visible
        speakingIndicator.showSpeakingIndicator(villagerName);
        assertTrue(speakingIndicator.isVisible());
        assertEquals(villagerName, speakingIndicator.getVillagerName());
        
        // Transition back to hidden
        speakingIndicator.hideSpeakingIndicator();
        assertFalse(speakingIndicator.isVisible());
        assertEquals("", speakingIndicator.getVillagerName());
    }
    
    @Test
    void testMultipleShowCalls() {
        String[] villagerNames = {"Villager1", "Villager2", "Villager3"};
        
        for (String name : villagerNames) {
            speakingIndicator.showSpeakingIndicator(name);
            
            assertTrue(speakingIndicator.isVisible(), 
                      "Should be visible for villager: " + name);
            assertEquals(name, speakingIndicator.getVillagerName(), 
                        "Should show correct name for villager: " + name);
        }
        
        // Should still be visible with the last villager
        assertTrue(speakingIndicator.isVisible());
        assertEquals(villagerNames[villagerNames.length - 1], speakingIndicator.getVillagerName());
    }
    
    @Test
    void testSpecialCharactersInVillagerName() {
        String specialName = "Villager_123!@#$%^&*()";
        
        speakingIndicator.showSpeakingIndicator(specialName);
        
        assertTrue(speakingIndicator.isVisible(), "Should handle special characters in name");
        assertEquals(specialName, speakingIndicator.getVillagerName(), 
                    "Should preserve special characters in villager name");
    }
    
    @Test
    void testLongVillagerName() {
        String longName = "VeryLongVillagerNameThatExceedsNormalLengthLimits123456789";
        
        speakingIndicator.showSpeakingIndicator(longName);
        
        assertTrue(speakingIndicator.isVisible(), "Should handle long villager names");
        assertEquals(longName, speakingIndicator.getVillagerName(), 
                    "Should preserve long villager names");
    }
}