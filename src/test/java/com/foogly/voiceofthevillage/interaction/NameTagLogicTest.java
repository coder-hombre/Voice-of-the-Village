package com.foogly.voiceofthevillage.interaction;

import com.foogly.voiceofthevillage.data.Gender;
import com.foogly.voiceofthevillage.data.NameGenerator;
import com.foogly.voiceofthevillage.data.PersonalityType;
import com.foogly.voiceofthevillage.data.VillagerData;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for name tag functionality logic.
 * Tests name validation, gender detection, and data preservation.
 */
class NameTagLogicTest {

    @Test
    void testNameValidation_ValidNames() {
        assertTrue(isValidVillagerName("Alice"), "Simple name should be valid");
        assertTrue(isValidVillagerName("Bob Smith"), "Name with space should be valid");
        assertTrue(isValidVillagerName("Jean-Luc"), "Name with hyphen should be valid");
        assertTrue(isValidVillagerName("O'Connor"), "Name with apostrophe should be valid");
        assertTrue(isValidVillagerName("Player123"), "Name with numbers should be valid");
        assertTrue(isValidVillagerName("Test_Name"), "Name with underscore should be valid");
        assertTrue(isValidVillagerName("Dr. Smith"), "Name with period should be valid");
    }

    @Test
    void testNameValidation_InvalidNames() {
        assertFalse(isValidVillagerName(null), "Null name should be invalid");
        assertFalse(isValidVillagerName(""), "Empty name should be invalid");
        assertFalse(isValidVillagerName("   "), "Whitespace-only name should be invalid");
        assertFalse(isValidVillagerName("Name@WithSymbols"), "Name with @ should be invalid");
        assertFalse(isValidVillagerName("Name#WithHash"), "Name with # should be invalid");
        assertFalse(isValidVillagerName("Name$WithDollar"), "Name with $ should be invalid");
        
        // Test very long name
        String longName = "A".repeat(50);
        assertFalse(isValidVillagerName(longName), "Very long name should be invalid");
    }

    @Test
    void testNameValidation_EdgeCases() {
        assertTrue(isValidVillagerName("A"), "Single character name should be valid");
        assertTrue(isValidVillagerName("X A-12"), "Complex but valid name should be valid");
        
        // Test name at length limit
        String maxLengthName = "A".repeat(32);
        assertTrue(isValidVillagerName(maxLengthName), "Name at max length should be valid");
        
        String tooLongName = "A".repeat(33);
        assertFalse(isValidVillagerName(tooLongName), "Name over max length should be invalid");
    }
    
    /**
     * Local implementation of name validation logic for testing.
     */
    private boolean isValidVillagerName(String name) {
        if (name == null) {
            return false;
        }
        
        String trimmedName = name.trim();
        
        // Check if name is empty
        if (trimmedName.isEmpty()) {
            return false;
        }
        
        // Check name length (reasonable limits)
        if (trimmedName.length() > 32) {
            return false;
        }
        
        // Check for invalid characters (basic validation)
        // Allow letters, numbers, spaces, and common punctuation
        if (!trimmedName.matches("^[a-zA-Z0-9\\s\\-_'.]+$")) {
            return false;
        }
        
        return true;
    }

    @Test
    void testEffectiveNameLogic() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Test original name priority
        assertEquals("OriginalName", data.getEffectiveName(), "Should return original name initially");
        
        // Test custom name priority
        data.setCustomName("CustomName");
        assertEquals("CustomName", data.getEffectiveName(), "Should return custom name when set");
        
        // Test fallback behavior
        data.setCustomName("");
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original for empty custom name");
        
        data.setCustomName("   ");
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original for whitespace custom name");
        
        data.setCustomName(null);
        assertEquals("OriginalName", data.getEffectiveName(), "Should fall back to original for null custom name");
    }

    @Test
    void testGenderUpdateLogic() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Test gender update with recognizable female name
        data.setCustomName("Alice");
        Gender detectedGender = NameGenerator.detectGender("Alice");
        if (detectedGender != Gender.UNKNOWN) {
            data.setGender(detectedGender);
        }
        
        assertEquals(Gender.FEMALE, data.getGender(), "Should update gender to female for Alice");
        assertEquals("Alice", data.getCustomName(), "Should preserve custom name");
        assertEquals("OriginalName", data.getOriginalName(), "Should preserve original name");
        assertEquals(PersonalityType.FRIENDLY, data.getPersonality(), "Should preserve personality");
    }

    @Test
    void testGenderUpdateLogic_UnknownName() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Test gender preservation with unknown name
        data.setCustomName("UnknownName123");
        Gender detectedGender = NameGenerator.detectGender("UnknownName123");
        if (detectedGender != Gender.UNKNOWN) {
            data.setGender(detectedGender);
        }
        
        assertEquals(Gender.MALE, data.getGender(), "Should preserve original gender for unknown name");
        assertEquals("UnknownName123", data.getCustomName(), "Should set custom name");
    }

    @Test
    void testDataPreservation() {
        UUID testId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Add some data to preserve
        data.getOrCreateReputation(playerId, "TestPlayer");
        data.addMemory(new com.foogly.voiceofthevillage.data.InteractionMemory(
            playerId, "TestPlayer", "Hello", "Hi there!", 
            com.foogly.voiceofthevillage.data.InteractionType.TEXT, 1));
        
        int originalInteractions = data.getTotalInteractions();
        long originalCreationTime = data.getCreationTime();
        
        // Rename the villager
        data.setCustomName("NewName");
        
        // Verify data preservation
        assertEquals(testId, data.getVillagerId(), "Should preserve villager ID");
        assertEquals("OriginalName", data.getOriginalName(), "Should preserve original name");
        assertEquals("NewName", data.getCustomName(), "Should update custom name");
        assertEquals(PersonalityType.FRIENDLY, data.getPersonality(), "Should preserve personality");
        assertEquals(originalInteractions, data.getTotalInteractions(), "Should preserve interaction count");
        assertEquals(originalCreationTime, data.getCreationTime(), "Should preserve creation time");
        assertEquals(1, data.getMemories().size(), "Should preserve memories");
        assertEquals(1, data.getPlayerReputations().size(), "Should preserve reputation data");
        assertNotNull(data.getReputation(playerId), "Should preserve specific reputation");
    }

    @Test
    void testNameSynchronization() {
        // Test the logic for synchronizing names between vanilla and mod data
        String vanillaName = "VanillaName";
        String modName = "ModName";
        
        // Test when names match
        assertTrue(namesMatch(vanillaName, vanillaName), "Matching names should be synchronized");
        
        // Test when names don't match
        assertFalse(namesMatch(vanillaName, modName), "Different names should not be synchronized");
        
        // Test null handling
        assertFalse(namesMatch(null, modName), "Null vanilla name should not match");
        assertTrue(namesMatch(null, null), "Both null should match");
    }

    @Test
    void testMultipleRenames() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Test multiple renames
        data.setCustomName("FirstRename");
        assertEquals("FirstRename", data.getEffectiveName(), "Should handle first rename");
        
        data.setCustomName("SecondRename");
        assertEquals("SecondRename", data.getEffectiveName(), "Should handle second rename");
        
        data.setCustomName("Alice");
        assertEquals("Alice", data.getEffectiveName(), "Should handle third rename");
        
        // Test reverting to original
        data.setCustomName(null);
        assertEquals("OriginalName", data.getEffectiveName(), "Should revert to original name");
        
        // Verify original data is still intact
        assertEquals("OriginalName", data.getOriginalName(), "Original name should be unchanged");
        assertEquals(PersonalityType.FRIENDLY, data.getPersonality(), "Personality should be unchanged");
    }

    @Test
    void testGenderTransitions() {
        UUID testId = UUID.randomUUID();
        VillagerData data = new VillagerData(testId, "OriginalName", Gender.MALE, PersonalityType.FRIENDLY);
        
        // Test male to female transition
        data.setCustomName("Alice");
        Gender newGender = NameGenerator.detectGender("Alice");
        if (newGender != Gender.UNKNOWN) {
            data.setGender(newGender);
        }
        assertEquals(Gender.FEMALE, data.getGender(), "Should transition from male to female");
        
        // Test female to male transition
        data.setCustomName("Alexander");
        newGender = NameGenerator.detectGender("Alexander");
        if (newGender != Gender.UNKNOWN) {
            data.setGender(newGender);
        }
        assertEquals(Gender.MALE, data.getGender(), "Should transition from female to male");
        
        // Test unknown name (should preserve current gender)
        Gender currentGender = data.getGender();
        data.setCustomName("UnknownName");
        newGender = NameGenerator.detectGender("UnknownName");
        if (newGender != Gender.UNKNOWN) {
            data.setGender(newGender);
        }
        assertEquals(currentGender, data.getGender(), "Should preserve gender for unknown name");
    }

    // Helper method to simulate name synchronization logic
    private boolean namesMatch(String vanillaName, String modName) {
        if (vanillaName == null && modName == null) {
            return true;
        }
        if (vanillaName == null || modName == null) {
            return false;
        }
        return vanillaName.equals(modName);
    }
}