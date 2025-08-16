package com.foogly.voiceofthevillage.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages retry logic with exponential backoff for API calls and other operations
 */
public class RetryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryManager.class);
    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(2);
    
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(500);
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    
    /**
     * Executes an operation with exponential backoff retry logic
     */
    public static <T> CompletableFuture<T> withRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName) {
        return withRetry(operation, operationName, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY);
    }
    
    /**
     * Executes an operation with custom retry parameters
     */
    public static <T> CompletableFuture<T> withRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int maxRetries,
            Duration initialDelay) {
        
        return executeWithRetry(operation, operationName, 0, maxRetries, initialDelay);
    }
    
    private static <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation,
            String operationName,
            int currentAttempt,
            int maxRetries,
            Duration currentDelay) {
        
        return operation.get()
            .exceptionally(throwable -> {
                if (currentAttempt >= maxRetries) {
                    LOGGER.error("Operation {} failed after {} attempts: {}", 
                        operationName, maxRetries + 1, throwable.getMessage());
                    throw new CompletionException("Max retries exceeded for " + operationName, throwable);
                }
                
                if (isRetryableError(throwable)) {
                    LOGGER.warn("Operation {} failed (attempt {}/{}), retrying in {}ms: {}", 
                        operationName, currentAttempt + 1, maxRetries + 1, 
                        currentDelay.toMillis(), throwable.getMessage());
                    
                    // Schedule retry with exponential backoff
                    Duration nextDelay = calculateNextDelay(currentDelay);
                    
                    CompletableFuture<T> retryFuture = new CompletableFuture<>();
                    SCHEDULER.schedule(() -> {
                        executeWithRetry(operation, operationName, currentAttempt + 1, maxRetries, nextDelay)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    retryFuture.completeExceptionally(error);
                                } else {
                                    retryFuture.complete(result);
                                }
                            });
                    }, currentDelay.toMillis(), TimeUnit.MILLISECONDS);
                    
                    try {
                        return retryFuture.get();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                } else {
                    LOGGER.error("Operation {} failed with non-retryable error: {}", 
                        operationName, throwable.getMessage());
                    throw new CompletionException("Non-retryable error for " + operationName, throwable);
                }
            });
    }
    
    /**
     * Determines if an error is retryable
     */
    private static boolean isRetryableError(Throwable throwable) {
        // Network timeouts, connection issues, and temporary service errors are retryable
        String message = throwable.getMessage().toLowerCase();
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("temporary") ||
               message.contains("rate limit") ||
               message.contains("503") ||
               message.contains("502") ||
               message.contains("504");
    }
    
    /**
     * Calculates next delay with exponential backoff
     */
    private static Duration calculateNextDelay(Duration currentDelay) {
        long nextDelayMs = (long) (currentDelay.toMillis() * DEFAULT_BACKOFF_MULTIPLIER);
        return Duration.ofMillis(Math.min(nextDelayMs, DEFAULT_MAX_DELAY.toMillis()));
    }
    
    /**
     * Shutdown the retry scheduler
     */
    public static void shutdown() {
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}