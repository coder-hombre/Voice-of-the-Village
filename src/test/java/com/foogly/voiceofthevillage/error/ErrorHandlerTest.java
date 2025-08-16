package com.foogly.voiceofthevillage.error;

import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ErrorHandlerTest {

    @Mock
    private ServerPlayer mockPlayer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testHandleWithFallback_Success() {
        // Arrange
        String expectedResult = "success";
        CompletableFuture<String> primaryFuture = CompletableFuture.completedFuture(expectedResult);
        
        // Act
        CompletableFuture<String> result = ErrorHandler.handleWithFallback(
            () -> primaryFuture,
            () -> "fallback",
            "test operation",
            mockPlayer
        );
        
        // Assert
        assertEquals(expectedResult, result.join());
        verify(mockPlayer, never()).sendSystemMessage(any());
    }

    @Test
    void testHandleWithFallback_PrimaryFailsUseFallback() {
        // Arrange
        CompletableFuture<String> primaryFuture = CompletableFuture.failedFuture(
            new RuntimeException("Primary failed")
        );
        String fallbackResult = "fallback response";
        
        // Act
        CompletableFuture<String> result = ErrorHandler.handleWithFallback(
            () -> primaryFuture,
            () -> fallbackResult,
            "test operation",
            mockPlayer
        );
        
        // Assert
        assertEquals(fallbackResult, result.join());
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testHandleWithFallback_BothFail() {
        // Arrange
        CompletableFuture<String> primaryFuture = CompletableFuture.failedFuture(
            new RuntimeException("Primary failed")
        );
        
        // Act & Assert
        CompletableFuture<String> result = ErrorHandler.handleWithFallback(
            () -> primaryFuture,
            () -> { throw new RuntimeException("Fallback failed"); },
            "test operation",
            mockPlayer
        );
        
        assertThrows(CompletionException.class, result::join);
    }

    @Test
    void testLogAndNotify() {
        // Arrange
        Exception testException = new RuntimeException("Test error");
        String context = "test context";
        String userMessage = "Something went wrong";
        
        // Act
        ErrorHandler.logAndNotify(context, testException, mockPlayer, userMessage);
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testLogAndNotify_NullPlayer() {
        // Arrange
        Exception testException = new RuntimeException("Test error");
        String context = "test context";
        String userMessage = "Something went wrong";
        
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> 
            ErrorHandler.logAndNotify(context, testException, null, userMessage)
        );
    }

    @Test
    void testNotifyPlayer() {
        // Arrange
        String message = "Test notification";
        
        // Act
        ErrorHandler.notifyPlayer(mockPlayer, message);
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testNotifyPlayer_NullPlayer() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> ErrorHandler.notifyPlayer(null, "test message"));
    }

    @Test
    void testHandleServiceUnavailable() {
        // Arrange
        String serviceName = "AI Service";
        
        // Act
        ErrorHandler.handleServiceUnavailable(serviceName, mockPlayer);
        
        // Assert
        verify(mockPlayer).sendSystemMessage(any());
    }

    @Test
    void testHandleServiceUnavailable_NullPlayer() {
        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> 
            ErrorHandler.handleServiceUnavailable("AI Service", null)
        );
    }
}