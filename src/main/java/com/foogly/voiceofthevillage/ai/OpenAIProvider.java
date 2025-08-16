package com.foogly.voiceofthevillage.ai;

import com.foogly.voiceofthevillage.data.VillagerData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * OpenAI API provider implementation for generating villager responses.
 * Supports GPT models including GPT-3.5-turbo and GPT-4 variants.
 */
public class OpenAIProvider extends AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIProvider.class);
    
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Pattern API_KEY_PATTERN = Pattern.compile("^sk-[A-Za-z0-9]{48}$");
    
    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Creates a new OpenAI provider instance.
     *
     * @param apiKey         OpenAI API key
     * @param model          Model to use (e.g., gpt-3.5-turbo, gpt-4)
     * @param timeoutSeconds Request timeout in seconds
     */
    public OpenAIProvider(String apiKey, String model, int timeoutSeconds) {
        super(apiKey, model, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<AIResponse> generateResponse(String playerMessage, VillagerData villagerData,
                                                         Object player, GameContext gameContext) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(
                AIResponse.failure("OpenAI provider is not properly configured")
            );
        }

        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the prompt using PromptBuilder
                PromptBuilder promptBuilder = new PromptBuilder();
                String prompt = promptBuilder.buildPrompt(playerMessage, villagerData, player, gameContext);

                // Create the request payload
                JsonObject requestBody = createRequestBody(prompt);
                
                // Make the HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OPENAI_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long responseTime = System.currentTimeMillis() - startTime;

                return parseResponse(response, responseTime);

            } catch (IOException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                LOGGER.error("Network error during OpenAI request: {}", e.getMessage());
                return new AIResponse("Network error: " + e.getMessage(), responseTime);
            } catch (InterruptedException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                LOGGER.error("Request interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                return new AIResponse("Request was interrupted", responseTime);
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                LOGGER.error("Unexpected error during OpenAI request: {}", e.getMessage(), e);
                return new AIResponse("Unexpected error: " + e.getMessage(), responseTime);
            }
        });
    }

    /**
     * Creates the JSON request body for the OpenAI API.
     *
     * @param prompt The constructed prompt
     * @return JSON request body
     */
    private JsonObject createRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", 150);
        requestBody.addProperty("temperature", 0.8);
        requestBody.addProperty("top_p", 0.9);
        requestBody.addProperty("frequency_penalty", 0.1);
        requestBody.addProperty("presence_penalty", 0.1);

        JsonArray messages = new JsonArray();
        
        // System message to set the context
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
            "You are a Minecraft villager. Respond in character as a villager would, " +
            "keeping responses concise (1-2 sentences), friendly, and appropriate for all ages. " +
            "Stay in the Minecraft world context and maintain your personality consistently.");
        messages.add(systemMessage);

        // User message with the full prompt
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);
        return requestBody;
    }

    /**
     * Parses the response from the OpenAI API.
     *
     * @param response     HTTP response
     * @param responseTime Time taken for the request
     * @return Parsed AI response
     */
    private AIResponse parseResponse(HttpResponse<String> response, long responseTime) {
        if (response.statusCode() != 200) {
            String errorMessage = "OpenAI API error (status " + response.statusCode() + "): " + response.body();
            LOGGER.error(errorMessage);
            return new AIResponse(errorMessage, responseTime);
        }

        try {
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            
            if (responseJson.has("error")) {
                JsonObject error = responseJson.getAsJsonObject("error");
                String errorMessage = "OpenAI API error: " + error.get("message").getAsString();
                LOGGER.error(errorMessage);
                return new AIResponse(errorMessage, responseTime);
            }

            JsonArray choices = responseJson.getAsJsonArray("choices");
            if (choices.size() == 0) {
                return new AIResponse("No response generated", responseTime);
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            String content = message.get("content").getAsString().trim();

            // Get token usage if available
            int tokensUsed = 0;
            if (responseJson.has("usage")) {
                JsonObject usage = responseJson.getAsJsonObject("usage");
                tokensUsed = usage.get("total_tokens").getAsInt();
            }

            // Apply content filtering
            String filteredContent = ContentFilter.filterResponse(content);

            return new AIResponse(filteredContent, responseTime, tokensUsed);

        } catch (Exception e) {
            String errorMessage = "Failed to parse OpenAI response: " + e.getMessage();
            LOGGER.error(errorMessage, e);
            return new AIResponse(errorMessage, responseTime);
        }
    }

    @Override
    public boolean isConfigured() {
        return isValidApiKey(apiKey) && isValidModel(model);
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    protected boolean isValidApiKey(String apiKey) {
        return apiKey != null && API_KEY_PATTERN.matcher(apiKey).matches();
    }

    @Override
    protected boolean isValidModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return false;
        }
        
        // Check for supported OpenAI models
        return model.startsWith("gpt-3.5") || 
               model.startsWith("gpt-4") ||
               model.equals("gpt-3.5-turbo") ||
               model.equals("gpt-4-turbo") ||
               model.equals("gpt-4o");
    }
}