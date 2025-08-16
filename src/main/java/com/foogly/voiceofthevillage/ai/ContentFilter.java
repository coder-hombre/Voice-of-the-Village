package com.foogly.voiceofthevillage.ai;

import java.util.regex.Pattern;

/**
 * Filters AI-generated content to ensure appropriate responses for Minecraft villagers.
 * Removes inappropriate content and ensures responses stay within the Minecraft context.
 */
public class ContentFilter {
    // Patterns for inappropriate content
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
        "\\b(damn|hell|crap|stupid|idiot|moron|dumb)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MODERN_REFERENCES_PATTERN = Pattern.compile(
        "\\b(internet|phone|computer|car|airplane|television|tv|movie|email|website|social media|facebook|twitter|instagram|youtube|google|amazon|netflix)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INAPPROPRIATE_TOPICS_PATTERN = Pattern.compile(
        "\\b(politics|religion|war|violence|death|kill|murder|suicide|drugs|alcohol|sex|sexual|romantic|love|dating|marriage)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Maximum response length
    private static final int MAX_RESPONSE_LENGTH = 200;

    /**
     * Filters an AI response to ensure it's appropriate for Minecraft villagers.
     *
     * @param response The raw AI response
     * @return Filtered and cleaned response
     */
    public static String filterResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return getDefaultResponse();
        }

        String filtered = response.trim();

        // Remove quotes if the entire response is quoted
        if (filtered.startsWith("\"") && filtered.endsWith("\"")) {
            filtered = filtered.substring(1, filtered.length() - 1).trim();
        }

        // Remove any "As a villager" or similar meta-commentary
        filtered = removeMetaCommentary(filtered);

        // Apply content filters
        filtered = filterProfanity(filtered);
        filtered = filterModernReferences(filtered);
        filtered = filterInappropriateTopics(filtered);

        // Ensure response length is reasonable
        if (filtered.length() > MAX_RESPONSE_LENGTH) {
            filtered = truncateResponse(filtered);
        }

        // Ensure response ends with proper punctuation
        filtered = ensureProperEnding(filtered);

        // Final validation - if response is too short or empty after filtering, use default
        if (filtered.length() < 3 || isResponseTooGeneric(filtered)) {
            return getDefaultResponse();
        }

        return filtered;
    }

    /**
     * Removes meta-commentary that breaks immersion.
     *
     * @param response The response to clean
     * @return Cleaned response
     */
    private static String removeMetaCommentary(String response) {
        // Remove common AI meta-phrases
        String[] metaPhrases = {
            "As a villager,?\\s*",
            "As an? \\w+ villager,?\\s*",
            "I am a villager,?\\s*",
            "Being a villager,?\\s*",
            "In my role as a villager,?\\s*"
        };

        for (String phrase : metaPhrases) {
            response = response.replaceAll("(?i)^" + phrase, "");
        }

        return response.trim();
    }

    /**
     * Filters out mild profanity and replaces with villager-appropriate alternatives.
     *
     * @param response The response to filter
     * @return Filtered response
     */
    private static String filterProfanity(String response) {
        return PROFANITY_PATTERN.matcher(response).replaceAll(match -> {
            String word = match.group().toLowerCase();
            return switch (word) {
                case "damn", "hell" -> "darn";
                case "crap" -> "nonsense";
                case "stupid", "idiot", "moron", "dumb" -> "silly";
                default -> "silly";
            };
        });
    }

    /**
     * Filters out modern references that don't fit in Minecraft.
     *
     * @param response The response to filter
     * @return Filtered response
     */
    private static String filterModernReferences(String response) {
        // First handle article corrections for specific replacements
        response = response.replaceAll("\\ban\\s+email\\b", "a letter");
        response = response.replaceAll("\\bthe\\s+internet\\b", "the village network");
        
        // Then handle the general pattern replacements
        return MODERN_REFERENCES_PATTERN.matcher(response).replaceAll(match -> {
            String word = match.group().toLowerCase();
            return switch (word) {
                case "internet", "website" -> "village network";
                case "phone" -> "message";
                case "computer" -> "redstone contraption";
                case "car", "airplane" -> "minecart";
                case "television", "tv", "movie" -> "story";
                case "email" -> "letter";
                default -> "thing";
            };
        });
    }

    /**
     * Filters out inappropriate topics for a family-friendly game.
     *
     * @param response The response to filter
     * @return Filtered response
     */
    private static String filterInappropriateTopics(String response) {
        if (INAPPROPRIATE_TOPICS_PATTERN.matcher(response).find()) {
            // If the response contains inappropriate topics, replace with a generic friendly response
            return "That's quite interesting! Is there anything else I can help you with?";
        }
        return response;
    }

    /**
     * Truncates a response that's too long while preserving sentence structure.
     *
     * @param response The response to truncate
     * @return Truncated response
     */
    private static String truncateResponse(String response) {
        if (response.length() <= MAX_RESPONSE_LENGTH) {
            return response;
        }

        // Try to truncate at sentence boundaries
        String[] sentences = response.split("[.!?]+");
        StringBuilder result = new StringBuilder();
        
        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (result.length() + trimmedSentence.length() + 1 <= MAX_RESPONSE_LENGTH) {
                if (result.length() > 0) {
                    result.append(". ");
                }
                result.append(trimmedSentence);
            } else {
                break;
            }
        }

        String truncated = result.toString().trim();
        if (truncated.isEmpty()) {
            // If no complete sentences fit, just truncate at word boundary
            int lastSpace = response.lastIndexOf(' ', MAX_RESPONSE_LENGTH - 3);
            if (lastSpace > 0) {
                truncated = response.substring(0, lastSpace) + "...";
            } else {
                truncated = response.substring(0, MAX_RESPONSE_LENGTH - 3) + "...";
            }
        }

        return truncated;
    }

    /**
     * Ensures the response ends with proper punctuation.
     *
     * @param response The response to check
     * @return Response with proper ending
     */
    private static String ensureProperEnding(String response) {
        if (response.isEmpty()) {
            return response;
        }

        char lastChar = response.charAt(response.length() - 1);
        if (lastChar != '.' && lastChar != '!' && lastChar != '?') {
            response += ".";
        }

        return response;
    }

    /**
     * Checks if a response is too generic or unhelpful.
     *
     * @param response The response to check
     * @return true if the response is too generic
     */
    private static boolean isResponseTooGeneric(String response) {
        String lower = response.toLowerCase();
        
        // Check for overly generic responses
        String[] genericPhrases = {
            "i don't know",
            "i'm not sure",
            "that's interesting",
            "hmm",
            "ok",
            "okay",
            "yes",
            "no",
            "maybe"
        };

        for (String phrase : genericPhrases) {
            if (lower.equals(phrase) || lower.equals(phrase + ".")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets a default response when filtering results in an empty or inappropriate response.
     *
     * @return Default villager response
     */
    private static String getDefaultResponse() {
        String[] defaultResponses = {
            "Hello there! How can I help you today?",
            "Greetings, friend! What brings you to our village?",
            "Good day! Is there something you need?",
            "Welcome! I hope you're enjoying your time in our village.",
            "Hello! Feel free to browse my wares if you're interested in trading."
        };

        // Return a random default response
        int index = (int) (Math.random() * defaultResponses.length);
        return defaultResponses[index];
    }
}