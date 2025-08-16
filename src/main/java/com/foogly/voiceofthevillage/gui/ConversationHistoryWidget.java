package com.foogly.voiceofthevillage.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Widget for displaying conversation history in a scrollable area.
 * Shows player messages, villager responses, and system messages.
 */
public class ConversationHistoryWidget extends AbstractWidget {
    private static final int LINE_HEIGHT = 12;
    private static final int SCROLL_SPEED = 3;
    private static final int PADDING = 2;
    
    private final List<ConversationEntry> entries;
    private final Font font;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    
    public ConversationHistoryWidget(int x, int y, int width, int height, List<ConversationEntry> entries) {
        super(x, y, width, height, Component.translatable("gui.voiceofthevillage.conversation_history"));
        this.entries = entries;
        this.font = Minecraft.getInstance().font;
    }
    
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88222222);
        
        // Draw border
        guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF444444);
        
        // Calculate visible area
        int visibleLines = (height - PADDING * 2) / LINE_HEIGHT;
        int totalLines = calculateTotalLines();
        
        // Update max scroll offset
        maxScrollOffset = Math.max(0, totalLines - visibleLines);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        
        // Render conversation entries
        renderConversationEntries(guiGraphics, visibleLines);
        
        // Draw scrollbar if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(guiGraphics);
        }
    }
    
    /**
     * Renders the conversation entries within the visible area.
     */
    private void renderConversationEntries(GuiGraphics guiGraphics, int visibleLines) {
        int currentLine = 0;
        int renderY = getY() + PADDING;
        
        for (ConversationEntry entry : entries) {
            Component displayText = entry.getDisplayText();
            List<FormattedCharSequence> wrappedLines = font.split(displayText, width - PADDING * 2);
            
            for (FormattedCharSequence line : wrappedLines) {
                // Skip lines that are above the visible area
                if (currentLine < scrollOffset) {
                    currentLine++;
                    continue;
                }
                
                // Stop rendering if we've filled the visible area
                if (currentLine >= scrollOffset + visibleLines) {
                    return;
                }
                
                // Render the line
                guiGraphics.drawString(
                    font, 
                    line, 
                    getX() + PADDING, 
                    renderY, 
                    entry.getType().getColor()
                );
                
                renderY += LINE_HEIGHT;
                currentLine++;
            }
        }
    }
    
    /**
     * Renders a scrollbar on the right side of the widget.
     */
    private void renderScrollbar(GuiGraphics guiGraphics) {
        int scrollbarX = getX() + width - 6;
        int scrollbarY = getY();
        int scrollbarHeight = height;
        
        // Draw scrollbar background
        guiGraphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFF333333);
        
        // Calculate thumb position and size
        int totalLines = calculateTotalLines();
        int visibleLines = (height - PADDING * 2) / LINE_HEIGHT;
        
        if (totalLines > visibleLines) {
            int thumbHeight = Math.max(10, (scrollbarHeight * visibleLines) / totalLines);
            int thumbY = scrollbarY + (scrollOffset * (scrollbarHeight - thumbHeight)) / maxScrollOffset;
            
            // Draw scrollbar thumb
            guiGraphics.fill(scrollbarX + 1, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF666666);
        }
    }
    
    /**
     * Calculates the total number of lines needed to display all entries.
     */
    private int calculateTotalLines() {
        int totalLines = 0;
        
        for (ConversationEntry entry : entries) {
            Component displayText = entry.getDisplayText();
            List<FormattedCharSequence> wrappedLines = font.split(displayText, width - PADDING * 2);
            totalLines += wrappedLines.size();
        }
        
        return totalLines;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset - (int)(scrollY * SCROLL_SPEED)));
            return true;
        }
        return false;
    }
    
    /**
     * Scrolls to the bottom of the conversation history.
     */
    public void scrollToBottom() {
        scrollOffset = maxScrollOffset;
    }
    
    /**
     * Scrolls to the top of the conversation history.
     */
    public void scrollToTop() {
        scrollOffset = 0;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Simple narration implementation - just provide basic widget info
        defaultButtonNarrationText(narrationElementOutput);
    }
}