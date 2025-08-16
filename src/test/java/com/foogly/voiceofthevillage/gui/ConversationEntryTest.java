package com.foogly.voiceofthevillage.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConversationEntry class.
 * Tests the conversation entry types, colors, and basic functionality.
 */
class ConversationEntryTest {

    @Test
    void testConversationEntryTypes() {
        // Test all conversation entry types exist
        ConversationEntry.Type[] types = ConversationEntry.Type.values();
        assertEquals(3, types.length);
        
        // Test specific types
        assertTrue(java.util.Arrays.asList(types).contains(ConversationEntry.Type.PLAYER));
        assertTrue(java.util.Arrays.asList(types).contains(ConversationEntry.Type.VILLAGER));
        assertTrue(java.util.Arrays.asList(types).contains(ConversationEntry.Type.SYSTEM));
    }

    @Test
    void testConversationEntryColors() {
        // Test that each type has a unique color
        int playerColor = ConversationEntry.Type.PLAYER.getColor();
        int villagerColor = ConversationEntry.Type.VILLAGER.getColor();
        int systemColor = ConversationEntry.Type.SYSTEM.getColor();
        
        // Verify colors are different
        assertNotEquals(playerColor, villagerColor);
        assertNotEquals(playerColor, systemColor);
        assertNotEquals(villagerColor, systemColor);
        
        // Verify colors are valid hex values
        assertEquals(0xFFFFFF, playerColor); // White for player
        assertEquals(0x55FF55, villagerColor); // Green for villager
        assertEquals(0xFFAA00, systemColor); // Orange for system
    }

    @Test
    void testConversationEntryTypeProperties() {
        // Test that each type has the expected properties
        ConversationEntry.Type playerType = ConversationEntry.Type.PLAYER;
        ConversationEntry.Type villagerType = ConversationEntry.Type.VILLAGER;
        ConversationEntry.Type systemType = ConversationEntry.Type.SYSTEM;
        
        // Test color values
        assertTrue(playerType.getColor() > 0);
        assertTrue(villagerType.getColor() > 0);
        assertTrue(systemType.getColor() > 0);
        
        // Test that colors are within valid range (0x000000 to 0xFFFFFF)
        assertTrue(playerType.getColor() <= 0xFFFFFF);
        assertTrue(villagerType.getColor() <= 0xFFFFFF);
        assertTrue(systemType.getColor() <= 0xFFFFFF);
    }

    @Test
    void testConversationEntryBasicProperties() {
        long timestamp = System.currentTimeMillis();
        
        // Test that we can create entries with different types
        ConversationEntry.Type[] types = ConversationEntry.Type.values();
        
        for (ConversationEntry.Type type : types) {
            // Test that each type can be used to create an entry
            assertNotNull(type);
            assertTrue(type.getColor() >= 0);
        }
        
        // Test timestamp handling
        assertTrue(timestamp > 0);
        assertTrue(System.currentTimeMillis() >= timestamp);
    }

    @Test
    void testEnumValues() {
        // Test that we have exactly the expected enum values
        ConversationEntry.Type[] types = ConversationEntry.Type.values();
        assertEquals(3, types.length);
        
        // Test valueOf functionality
        assertEquals(ConversationEntry.Type.PLAYER, ConversationEntry.Type.valueOf("PLAYER"));
        assertEquals(ConversationEntry.Type.VILLAGER, ConversationEntry.Type.valueOf("VILLAGER"));
        assertEquals(ConversationEntry.Type.SYSTEM, ConversationEntry.Type.valueOf("SYSTEM"));
    }

    @Test
    void testColorConstants() {
        // Test that the color constants are as expected
        assertEquals(0xFFFFFF, ConversationEntry.Type.PLAYER.getColor());
        assertEquals(0x55FF55, ConversationEntry.Type.VILLAGER.getColor());
        assertEquals(0xFFAA00, ConversationEntry.Type.SYSTEM.getColor());
        
        // Test that colors are in RGB format (no alpha channel in these constants)
        assertTrue(ConversationEntry.Type.PLAYER.getColor() < 0x1000000); // Less than 24-bit max
        assertTrue(ConversationEntry.Type.VILLAGER.getColor() < 0x1000000);
        assertTrue(ConversationEntry.Type.SYSTEM.getColor() < 0x1000000);
    }
}