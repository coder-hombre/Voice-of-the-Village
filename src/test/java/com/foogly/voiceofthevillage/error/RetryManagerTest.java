package com.foogly.voiceofthevillage.error;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryManagerTest {

    @BeforeEach
    void setUp() {
        // Setup test environment
    }

    @AfterEach
    void tearDown() {
        // Cleanup after tests
    }

    @Test
    void testWithRetry_Success() {
        // Arrange
        String expectedResult = "success";
        CompletableFuture<String> operation = CompletableFuture.completedFuture(expectedResult);
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> operation,
            "test operation"
        );
        
        // Assert
        assertEquals(expectedResult, result.join());
    }

    @Test
    void testWithRetry_SuccessAfterRetries() {
        // Arrange
        AtomicInteger attemptCount = new AtomicInteger(0);
        String expectedResult = "success";
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    return CompletableFuture.failedFuture(new RuntimeException("Temporary failure"));
                }
                return CompletableFuture.completedFuture(expectedResult);
            },
            "test operation"
        );
        
        // Assert
        assertEquals(expectedResult, result.join());
        assertEquals(3, attemptCount.get());
    }

    @Test
    void testWithRetry_MaxRetriesExceeded() {
        // Arrange
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(new RuntimeException("Persistent failure"));
            },
            "test operation",
            2, // max retries
            Duration.ofMillis(10) // short delay for testing
        );
        
        // Assert
        assertThrows(CompletionException.class, result::join);
        assertEquals(3, attemptCount.get()); // Initial attempt + 2 retries
    }

    @Test
    void testWithRetry_NonRetryableError() {
        // Arrange
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(new RuntimeException("Authentication failed"));
            },
            "test operation"
        );
        
        // Assert
        assertThrows(CompletionException.class, result::join);
        assertEquals(1, attemptCount.get()); // Should not retry non-retryable errors
    }

    @Test
    void testWithRetry_RetryableNetworkError() {
        // Arrange
        AtomicInteger attemptCount = new AtomicInteger(0);
        String expectedResult = "success";
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 2) {
                    return CompletableFuture.failedFuture(new RuntimeException("Connection timeout"));
                }
                return CompletableFuture.completedFuture(expectedResult);
            },
            "test operation",
            3,
            Duration.ofMillis(10)
        );
        
        // Assert
        assertEquals(expectedResult, result.join());
        assertEquals(2, attemptCount.get());
    }

    @Test
    void testWithRetry_CustomParameters() {
        // Arrange
        AtomicInteger attemptCount = new AtomicInteger(0);
        int maxRetries = 5;
        Duration initialDelay = Duration.ofMillis(50);
        
        // Act
        CompletableFuture<String> result = RetryManager.withRetry(
            () -> {
                attemptCount.incrementAndGet();
                return CompletableFuture.failedFuture(new RuntimeException("Network error"));
            },
            "test operation",
            maxRetries,
            initialDelay
        );
        
        // Assert
        assertThrows(CompletionException.class, result::join);
        assertEquals(maxRetries + 1, attemptCount.get());
    }

    @Test
    void testShutdown() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(RetryManager::shutdown);
    }
}