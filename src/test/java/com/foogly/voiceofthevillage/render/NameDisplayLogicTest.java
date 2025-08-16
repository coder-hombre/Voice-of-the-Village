package com.foogly.voiceofthevillage.render;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for name display logic and distance calculations.
 * These tests focus on the mathematical and logical aspects without requiring Minecraft classes.
 */
class NameDisplayLogicTest {

    @Test
    void testDistanceCalculation() {
        // Test 3D distance calculation (Pythagorean theorem)
        double distance = calculateDistance(0, 0, 0, 3, 4, 0);
        assertEquals(5.0, distance, 0.001, "Distance should be 5 (3-4-5 triangle)");
    }

    @Test
    void testDistanceCalculation_SamePosition() {
        double distance = calculateDistance(5, 10, 15, 5, 10, 15);
        assertEquals(0.0, distance, 0.001, "Distance should be 0 when at same position");
    }

    @Test
    void testDistanceCalculation_3D() {
        // Test 3D distance: sqrt(1^2 + 2^2 + 2^2) = sqrt(9) = 3
        double distance = calculateDistance(0, 0, 0, 1, 2, 2);
        assertEquals(3.0, distance, 0.001, "3D distance should be 3");
    }

    @Test
    void testSquaredDistanceCalculation() {
        // Test squared distance for performance optimization
        double squaredDistance = calculateSquaredDistance(0, 0, 0, 3, 4, 0);
        assertEquals(25.0, squaredDistance, 0.001, "Squared distance should be 25 (5^2)");
    }

    @Test
    void testWithinDistanceCheck() {
        // Test distance checking logic
        assertTrue(isWithinDistance(5.0, 10.0), "Should be within distance when distance < limit");
        assertTrue(isWithinDistance(10.0, 10.0), "Should be within distance when distance = limit");
        assertFalse(isWithinDistance(15.0, 10.0), "Should not be within distance when distance > limit");
    }

    @Test
    void testWithinDistanceCheck_DisabledDistance() {
        // Test disabled distance (0 or negative)
        assertFalse(isWithinDistance(5.0, 0.0), "Should not be within distance when limit is 0");
        assertFalse(isWithinDistance(5.0, -1.0), "Should not be within distance when limit is negative");
    }

    @Test
    void testNamePriority() {
        // Test name priority logic: custom name takes precedence over original name
        String result = getEffectiveName("CustomName", "OriginalName");
        assertEquals("CustomName", result, "Custom name should take precedence");
        
        result = getEffectiveName(null, "OriginalName");
        assertEquals("OriginalName", result, "Should fall back to original name when custom is null");
        
        result = getEffectiveName("", "OriginalName");
        assertEquals("OriginalName", result, "Should fall back to original name when custom is empty");
        
        result = getEffectiveName("   ", "OriginalName");
        assertEquals("OriginalName", result, "Should fall back to original name when custom is whitespace");
    }

    @Test
    void testNamePriority_BothNull() {
        String result = getEffectiveName(null, null);
        assertNull(result, "Should return null when both names are null");
    }

    // Helper methods that simulate the logic from VillagerNameRenderer

    /**
     * Calculates 3D distance between two points.
     */
    private double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates squared 3D distance between two points (for performance).
     */
    private double calculateSquaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Checks if a distance is within the specified limit.
     */
    private boolean isWithinDistance(double distance, double limit) {
        if (limit <= 0.0) {
            return false;
        }
        return distance <= limit;
    }

    /**
     * Gets the effective name, prioritizing custom name over original name.
     */
    private String getEffectiveName(String customName, String originalName) {
        if (customName != null && !customName.trim().isEmpty()) {
            return customName;
        }
        return originalName;
    }
}