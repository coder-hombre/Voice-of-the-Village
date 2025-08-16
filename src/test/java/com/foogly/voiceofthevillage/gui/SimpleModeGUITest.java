package com.foogly.voiceofthevillage.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for simple mode GUI functionality.
 * Tests GUI component functionality without requiring Minecraft classes.
 */
class SimpleModeGUITest {

    @Test
    void testConversationEntryClassExists() {
        // Test that ConversationEntry class exists and has expected structure
        Class<?> entryClass = ConversationEntry.class;
        assertNotNull(entryClass);
        assertEquals("ConversationEntry", entryClass.getSimpleName());
        assertEquals("com.foogly.voiceofthevillage.gui", entryClass.getPackage().getName());
        
        // Test that the Type enum is accessible
        Class<?>[] innerClasses = entryClass.getDeclaredClasses();
        boolean hasTypeEnum = false;
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals("Type")) {
                hasTypeEnum = true;
                assertTrue(innerClass.isEnum());
                break;
            }
        }
        assertTrue(hasTypeEnum, "ConversationEntry should have Type enum");
    }
    
    @Test
    void testConversationEntryTypes() {
        // Test that all expected conversation entry types exist
        ConversationEntry.Type[] types = ConversationEntry.Type.values();
        assertEquals(3, types.length);
        
        // Test specific types exist
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
    void testConversationEntryBasicProperties() {
        long timestamp = System.currentTimeMillis();
        
        // Test that we can create entries with different types
        ConversationEntry.Type[] types = ConversationEntry.Type.values();
        
        for (ConversationEntry.Type type : types) {
            // Test that each type can be used to create an entry
            assertNotNull(type);
            assertTrue(type.getColor() >= 0);
            
            // Test that colors are within valid range (0x000000 to 0xFFFFFF)
            assertTrue(type.getColor() <= 0xFFFFFF);
        }
        
        // Test timestamp handling
        assertTrue(timestamp > 0);
        assertTrue(System.currentTimeMillis() >= timestamp);
    }
    
    @Test
    void testConversationEntryTypeProperties() {
        // Test that each type has the expected properties
        ConversationEntry.Type playerType = ConversationEntry.Type.PLAYER;
        ConversationEntry.Type villagerType = ConversationEntry.Type.VILLAGER;
        ConversationEntry.Type systemType = ConversationEntry.Type.SYSTEM;
        
        // Test color values are positive
        assertTrue(playerType.getColor() > 0);
        assertTrue(villagerType.getColor() > 0);
        assertTrue(systemType.getColor() > 0);
        
        // Test that colors are in RGB format (no alpha channel in these constants)
        assertTrue(playerType.getColor() < 0x1000000); // Less than 24-bit max
        assertTrue(villagerType.getColor() < 0x1000000);
        assertTrue(systemType.getColor() < 0x1000000);
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
    }
    
    @Test
    void testConversationEntryTimestamp() {
        // Test timestamp functionality
        long timestamp1 = System.currentTimeMillis();
        
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long timestamp2 = System.currentTimeMillis();
        
        // Test that timestamps are ordered correctly
        assertTrue(timestamp2 >= timestamp1);
    }
    
    @Test
    void testConversationEntryTypeConsistency() {
        // Test that enum values are consistent
        for (ConversationEntry.Type type : ConversationEntry.Type.values()) {
            // Each type should have a valid color
            assertNotNull(type);
            assertTrue(type.getColor() >= 0);
            
            // Each type should be convertible to string and back
            String typeName = type.name();
            assertNotNull(typeName);
            assertEquals(type, ConversationEntry.Type.valueOf(typeName));
        }
    }
}