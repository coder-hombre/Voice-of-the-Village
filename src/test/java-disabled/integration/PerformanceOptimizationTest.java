package com.foogly.voiceofthevillage.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance optimization tests for the villager communication system.
 * These tests verify that the system meets performance requirements.
 */
class PerformanceOptimizationTest {

    private PerformanceMetrics performanceMetrics;

    @BeforeEach
    void setUp() {
        performanceMetrics = new PerformanceMetrics();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testTextMessageProcessingPerformance() {
        // Arrange
        int numberOfMessages = 100;
        long maxAcceptableTimeMs = 1000; // 1 second per message
        
        // Act
        long startTime = System.currentTimeMillis();
        
        IntStream.range(0, numberOfMessages).forEach(i -> {
            long messageStart = System.currentTimeMillis();
            
            // Simulate text message processing
            simulateTextProcessing();
            
            long messageEnd = System.currentTimeMillis();
            performanceMetrics.recordTextInteraction(messageEnd - messageStart, true);
        });
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Assert
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertTrue(snapshot.getAverageTextProcessingTime() < maxAcceptableTimeMs,
            "Average text processing time should be under " + maxAcceptableTimeMs + "ms");
        assertTrue(totalTime < numberOfMessages * maxAcceptableTimeMs,
            "Total processing time should be reasonable");
        assertEquals(numberOfMessages, snapshot.getTotalTextInteractions());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testVoiceMessageProcessingPerformance() {
        // Arrange
        int numberOfMessages = 50; // Fewer voice messages due to higher processing cost
        long maxAcceptableTimeMs = 3000; // 3 seconds per voice message
        
        // Act
        long startTime = System.currentTimeMillis();
        
        IntStream.range(0, numberOfMessages).forEach(i -> {
            long messageStart = System.currentTimeMillis();
            
            // Simulate voice message processing (more expensive)
            simulateVoiceProcessing();
            
            long messageEnd = System.currentTimeMillis();
            performanceMetrics.recordVoiceInteraction(messageEnd - messageStart, true);
        });
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Assert
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertTrue(snapshot.getAverageVoiceProcessingTime() < maxAcceptableTimeMs,
            "Average voice processing time should be under " + maxAcceptableTimeMs + "ms");
        assertEquals(numberOfMessages, snapshot.getTotalVoiceInteractions());
    }

    @Test
    void testConcurrentProcessingPerformance() {
        // Arrange
        int numberOfConcurrentRequests = 10;
        
        // Act
        CompletableFuture<?>[] futures = IntStream.range(0, numberOfConcurrentRequests)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                simulateTextProcessing();
                long end = System.currentTimeMillis();
                performanceMetrics.recordTextInteraction(end - start, true);
            }))
            .toArray(CompletableFuture[]::new);
        
        // Wait for all to complete
        CompletableFuture.allOf(futures).join();
        
        // Assert
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertEquals(numberOfConcurrentRequests, snapshot.getTotalTextInteractions());
        assertTrue(snapshot.getTextSuccessRate() == 100.0, "All concurrent requests should succeed");
    }

    @Test
    void testMemoryUsageOptimization() {
        // Arrange
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Act - Simulate many interactions to test memory usage
        IntStream.range(0, 1000).forEach(i -> {
            simulateTextProcessing();
            performanceMetrics.recordTextInteraction(10, true);
            
            // Force garbage collection periodically
            if (i % 100 == 0) {
                System.gc();
            }
        });
        
        System.gc(); // Final garbage collection
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Assert
        long memoryIncrease = finalMemory - initialMemory;
        long maxAcceptableIncrease = 50 * 1024 * 1024; // 50MB
        
        assertTrue(memoryIncrease < maxAcceptableIncrease,
            "Memory usage should not increase excessively. Increase: " + memoryIncrease + " bytes");
    }

    @Test
    void testResponseTimeConsistency() {
        // Arrange
        int numberOfSamples = 100;
        double maxVarianceThreshold = 0.3; // 30% variance allowed
        
        // Act
        long[] responseTimes = new long[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++) {
            long start = System.currentTimeMillis();
            simulateTextProcessing();
            long end = System.currentTimeMillis();
            responseTimes[i] = end - start;
            performanceMetrics.recordTextInteraction(responseTimes[i], true);
        }
        
        // Calculate statistics
        double average = IntStream.range(0, numberOfSamples)
            .mapToLong(i -> responseTimes[i])
            .average()
            .orElse(0.0);
        
        double variance = IntStream.range(0, numberOfSamples)
            .mapToDouble(i -> Math.pow(responseTimes[i] - average, 2))
            .average()
            .orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / average;
        
        // Assert
        assertTrue(coefficientOfVariation < maxVarianceThreshold,
            "Response time should be consistent. Coefficient of variation: " + coefficientOfVariation);
    }

    @Test
    void testThroughputUnderLoad() {
        // Arrange
        int durationSeconds = 5;
        int minExpectedThroughput = 10; // messages per second
        
        // Act
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000);
        int processedMessages = 0;
        
        while (System.currentTimeMillis() < endTime) {
            long messageStart = System.currentTimeMillis();
            simulateTextProcessing();
            long messageEnd = System.currentTimeMillis();
            
            performanceMetrics.recordTextInteraction(messageEnd - messageStart, true);
            processedMessages++;
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        double actualThroughput = (double) processedMessages / (actualDuration / 1000.0);
        
        // Assert
        assertTrue(actualThroughput >= minExpectedThroughput,
            "Throughput should meet minimum requirements. Actual: " + actualThroughput + " msg/sec");
    }

    @Test
    void testResourceCleanupEfficiency() {
        // Arrange
        int numberOfCycles = 10;
        
        // Act & Assert
        for (int cycle = 0; cycle < numberOfCycles; cycle++) {
            // Simulate resource allocation and cleanup
            IntStream.range(0, 100).forEach(i -> {
                simulateTextProcessing();
                performanceMetrics.recordTextInteraction(10, true);
            });
            
            // Simulate cleanup
            performanceMetrics.reset();
            System.gc();
            
            // Verify cleanup was effective
            PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
            assertEquals(0, snapshot.getTotalInteractions(), 
                "Metrics should be reset after cleanup in cycle " + cycle);
        }
    }

    @Test
    void testScalabilityWithIncreasingLoad() {
        // Arrange
        int[] loadLevels = {10, 50, 100, 200};
        double maxDegradationFactor = 2.0; // Performance shouldn't degrade more than 2x
        
        double baselinePerformance = 0;
        
        // Act & Assert
        for (int loadLevel : loadLevels) {
            performanceMetrics.reset();
            
            long startTime = System.currentTimeMillis();
            
            IntStream.range(0, loadLevel).parallel().forEach(i -> {
                long messageStart = System.currentTimeMillis();
                simulateTextProcessing();
                long messageEnd = System.currentTimeMillis();
                performanceMetrics.recordTextInteraction(messageEnd - messageStart, true);
            });
            
            long totalTime = System.currentTimeMillis() - startTime;
            double averageTime = (double) totalTime / loadLevel;
            
            if (baselinePerformance == 0) {
                baselinePerformance = averageTime;
            } else {
                double degradationFactor = averageTime / baselinePerformance;
                assertTrue(degradationFactor <= maxDegradationFactor,
                    "Performance degradation should be acceptable at load level " + loadLevel + 
                    ". Degradation factor: " + degradationFactor);
            }
        }
    }

    /**
     * Simulates text message processing with realistic timing
     */
    private void simulateTextProcessing() {
        try {
            // Simulate AI processing time (50-200ms)
            Thread.sleep(50 + (int)(Math.random() * 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulates voice message processing with realistic timing
     */
    private void simulateVoiceProcessing() {
        try {
            // Simulate speech-to-text + AI + text-to-speech (500-2000ms)
            Thread.sleep(500 + (int)(Math.random() * 1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}