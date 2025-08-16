package com.foogly.voiceofthevillage.gui;

import net.minecraft.network.chat.Component;

/**
 * Represents a single entry in the conversation history.
 * Contains the message type, content, and timestamp.
 */
public class ConversationEntry {
    private final Type type;
    private final Component message;
    private final long timestamp;
    
    public ConversationEntry(Type type, Component message, long timestamp) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public Type getType() {
        return type;
    }
    
    public Component getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the display text for this conversation entry.
     * Includes type prefix for better visual distinction.
     */
    public Component getDisplayText() {
        return switch (type) {
            case PLAYER -> Component.literal("[You] ").append(message);
            case VILLAGER -> Component.literal("[Villager] ").append(message);
            case SYSTEM -> Component.literal("[System] ").append(message);
        };
    }
    
    /**
     * Types of conversation entries with associated display colors.
     */
    public enum Type {
        PLAYER(0xFFFFFF),    // White
        VILLAGER(0x55FF55),  // Green
        SYSTEM(0xFFAA00);    // Orange
        
        private final int color;
        
        Type(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
}