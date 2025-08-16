package com.foogly.voiceofthevillage.ai;

import java.util.Objects;

/**
 * Represents a response from an AI service.
 * Contains the generated text and metadata about the response.
 */
public class AIResponse {
    private final String responseText;
    private final boolean success;
    private final String errorMessage;
    private final long responseTimeMs;
    private final int tokensUsed;

    /**
     * Creates a successful AI response.
     *
     * @param responseText   The generated response text
     * @param responseTimeMs Time taken to generate the response
     * @param tokensUsed     Number of tokens used (if available)
     */
    public AIResponse(String responseText, long responseTimeMs, int tokensUsed) {
        this.responseText = responseText;
        this.success = true;
        this.errorMessage = null;
        this.responseTimeMs = responseTimeMs;
        this.tokensUsed = tokensUsed;
    }

    /**
     * Creates a failed AI response.
     *
     * @param errorMessage   Error message describing the failure
     * @param responseTimeMs Time taken before failure
     */
    public AIResponse(String errorMessage, long responseTimeMs) {
        this.responseText = null;
        this.success = false;
        this.errorMessage = errorMessage;
        this.responseTimeMs = responseTimeMs;
        this.tokensUsed = 0;
    }

    /**
     * Creates a successful AI response with default values.
     *
     * @param responseText The generated response text
     */
    public static AIResponse success(String responseText) {
        return new AIResponse(responseText, 0L, 0);
    }

    /**
     * Creates a failed AI response.
     *
     * @param errorMessage Error message describing the failure
     */
    public static AIResponse failure(String errorMessage) {
        return new AIResponse(errorMessage, 0L);
    }

    // Getters
    public String getResponseText() {
        return responseText;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIResponse that = (AIResponse) o;
        return success == that.success &&
               responseTimeMs == that.responseTimeMs &&
               tokensUsed == that.tokensUsed &&
               Objects.equals(responseText, that.responseText) &&
               Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseText, success, errorMessage, responseTimeMs, tokensUsed);
    }

    @Override
    public String toString() {
        if (success) {
            return "AIResponse{success=true, responseText='" + responseText + "', responseTimeMs=" + responseTimeMs + ", tokensUsed=" + tokensUsed + "}";
        } else {
            return "AIResponse{success=false, errorMessage='" + errorMessage + "', responseTimeMs=" + responseTimeMs + "}";
        }
    }
}