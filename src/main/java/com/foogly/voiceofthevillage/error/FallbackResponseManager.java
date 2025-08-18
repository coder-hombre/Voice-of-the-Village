package com.foogly.voiceofthevillage.error;

import com.foogly.voiceofthevillage.data.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Manages fallback responses when AI services are unavailable
 */
public class FallbackResponseManager {
    private static final Random RANDOM = new Random();
    
    // Generic fallback responses
    private static final List<String> GENERIC_RESPONSES = Arrays.asList(
        "I'm sorry, I'm having trouble understanding right now.",
        "Could you try again? I'm not quite myself today.",
        "Hmm, my thoughts seem a bit scattered at the moment.",
        "I apologize, but I'm having difficulty responding properly.",
        "Perhaps we could talk again later when I'm feeling clearer."
    );
    
    // Profession-specific fallback responses
    private static final List<String> FARMER_RESPONSES = Arrays.asList(
        "The crops need tending, perhaps we can talk later.",
        "I'm focused on the harvest right now.",
        "The fields require my attention at the moment."
    );
    
    private static final List<String> LIBRARIAN_RESPONSES = Arrays.asList(
        "I need to organize these books first.",
        "My mind is on ancient texts right now.",
        "The knowledge seems foggy today."
    );
    
    private static final List<String> BLACKSMITH_RESPONSES = Arrays.asList(
        "The forge demands my attention.",
        "I'm focused on my metalwork right now.",
        "The anvil calls to me."
    );
    
    private static final List<String> CLERIC_RESPONSES = Arrays.asList(
        "I'm in deep contemplation at the moment.",
        "My prayers require focus right now.",
        "The spirits are unclear today."
    );
    
    private static final List<String> BUTCHER_RESPONSES = Arrays.asList(
        "I'm busy preparing meat for the village.",
        "The cuts require my full attention.",
        "I'm focused on my trade right now."
    );
    
    /**
     * Gets a fallback response based on villager data and context
     */
    public static String getFallbackResponse(VillagerData villagerData, String playerMessage) {
        // For now, always use generic responses since VillagerData doesn't contain profession info
        // TODO: Add profession support when VillagerData is extended
        return getRandomResponse(GENERIC_RESPONSES);
    }
    
    /**
     * Gets a simple acknowledgment response
     */
    public static String getAcknowledgmentResponse() {
        List<String> acknowledgments = Arrays.asList(
            "I understand.",
            "I see.",
            "Hmm, yes.",
            "Indeed.",
            "I hear you."
        );
        return getRandomResponse(acknowledgments);
    }
    
    /**
     * Gets an error-specific response
     */
    public static String getErrorResponse(String errorType) {
        switch (errorType.toLowerCase()) {
            case "audio":
                return "I'm having trouble hearing you clearly right now.";
            case "network":
                return "There seems to be a communication issue. Could you try again?";
            case "ai":
                return "My thoughts are a bit muddled at the moment. Please be patient.";
            default:
                return getRandomResponse(GENERIC_RESPONSES);
        }
    }
    
    private static String getProfessionResponse(VillagerProfession profession) {
        if (profession == VillagerProfession.FARMER) {
            return getRandomResponse(FARMER_RESPONSES);
        } else if (profession == VillagerProfession.LIBRARIAN) {
            return getRandomResponse(LIBRARIAN_RESPONSES);
        } else if (profession == VillagerProfession.WEAPONSMITH || 
                   profession == VillagerProfession.ARMORER || 
                   profession == VillagerProfession.TOOLSMITH) {
            return getRandomResponse(BLACKSMITH_RESPONSES);
        } else if (profession == VillagerProfession.CLERIC) {
            return getRandomResponse(CLERIC_RESPONSES);
        } else if (profession == VillagerProfession.BUTCHER) {
            return getRandomResponse(BUTCHER_RESPONSES);
        }
        return null;
    }
    
    private static String getRandomResponse(List<String> responses) {
        return responses.get(RANDOM.nextInt(responses.size()));
    }
}