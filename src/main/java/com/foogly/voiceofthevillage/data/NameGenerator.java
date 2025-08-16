package com.foogly.voiceofthevillage.data;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generates random names for villagers and determines gender based on name assignment.
 * Uses predefined lists of male and female names to ensure appropriate voice selection.
 */
public class NameGenerator {
    private static final Random RANDOM = new Random();

    private static final List<String> MALE_NAMES = Arrays.asList(
        "Alexander", "Benjamin", "Christopher", "Daniel", "Edward",
        "Frederick", "George", "Henry", "Isaac", "James", "Kenneth",
        "Lawrence", "Michael", "Nicholas", "Oliver", "Patrick",
        "Robert", "Samuel", "Thomas", "William", "Arthur", "Charles",
        "David", "Edmund", "Francis", "Gregory", "Harold", "Ivan",
        "Jonathan", "Kevin", "Louis", "Matthew", "Nathan", "Oscar",
        "Peter", "Quincy", "Richard", "Stephen", "Theodore", "Victor",
        "Walter", "Xavier", "Zachary", "Adrian", "Bernard", "Cedric",
        "Dominic", "Eugene", "Felix", "Gabriel", "Hugo", "Ignatius"
    );

    private static final List<String> FEMALE_NAMES = Arrays.asList(
        "Alice", "Beatrice", "Catherine", "Diana", "Eleanor",
        "Florence", "Grace", "Helen", "Isabella", "Julia", "Katherine",
        "Louise", "Margaret", "Natalie", "Olivia", "Patricia",
        "Rebecca", "Sarah", "Teresa", "Victoria", "Wendy", "Yvonne",
        "Zoe", "Abigail", "Bridget", "Charlotte", "Delphine", "Emma",
        "Fiona", "Gabrielle", "Hannah", "Irene", "Josephine", "Kimberly",
        "Lillian", "Miranda", "Nicole", "Penelope", "Quinn", "Rose",
        "Sophia", "Tiffany", "Ursula", "Violet", "Winifred", "Xandra",
        "Yasmin", "Zelda", "Anastasia", "Bianca", "Cordelia", "Daphne"
    );

    /**
     * Generates a random name and determines the associated gender.
     *
     * @return A VillagerNameData containing the name and gender
     */
    public static VillagerNameData generateRandomName() {
        // Randomly choose between male and female names
        boolean isMale = RANDOM.nextBoolean();
        
        if (isMale) {
            String name = MALE_NAMES.get(RANDOM.nextInt(MALE_NAMES.size()));
            return new VillagerNameData(name, Gender.MALE);
        } else {
            String name = FEMALE_NAMES.get(RANDOM.nextInt(FEMALE_NAMES.size()));
            return new VillagerNameData(name, Gender.FEMALE);
        }
    }

    /**
     * Generates a random male name.
     *
     * @return A VillagerNameData with a male name and MALE gender
     */
    public static VillagerNameData generateMaleName() {
        String name = MALE_NAMES.get(RANDOM.nextInt(MALE_NAMES.size()));
        return new VillagerNameData(name, Gender.MALE);
    }

    /**
     * Generates a random female name.
     *
     * @return A VillagerNameData with a female name and FEMALE gender
     */
    public static VillagerNameData generateFemaleName() {
        String name = FEMALE_NAMES.get(RANDOM.nextInt(FEMALE_NAMES.size()));
        return new VillagerNameData(name, Gender.FEMALE);
    }

    /**
     * Determines the gender of a given name by checking if it exists in the predefined lists.
     * If the name is not found in either list, returns UNKNOWN.
     *
     * @param name The name to check
     * @return The gender associated with the name, or UNKNOWN if not found
     */
    public static Gender detectGender(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Gender.UNKNOWN;
        }

        String normalizedName = name.trim();
        
        // Check if it's a male name
        if (MALE_NAMES.contains(normalizedName)) {
            return Gender.MALE;
        }
        
        // Check if it's a female name
        if (FEMALE_NAMES.contains(normalizedName)) {
            return Gender.FEMALE;
        }
        
        // Name not found in either list
        return Gender.UNKNOWN;
    }

    /**
     * Checks if a name exists in the male names list.
     *
     * @param name The name to check
     * @return true if the name is in the male names list
     */
    public static boolean isMaleName(String name) {
        return name != null && MALE_NAMES.contains(name.trim());
    }

    /**
     * Checks if a name exists in the female names list.
     *
     * @param name The name to check
     * @return true if the name is in the female names list
     */
    public static boolean isFemaleName(String name) {
        return name != null && FEMALE_NAMES.contains(name.trim());
    }

    /**
     * Gets the total number of available male names.
     *
     * @return The count of male names
     */
    public static int getMaleNameCount() {
        return MALE_NAMES.size();
    }

    /**
     * Gets the total number of available female names.
     *
     * @return The count of female names
     */
    public static int getFemaleNameCount() {
        return FEMALE_NAMES.size();
    }

    /**
     * Gets the total number of available names (male + female).
     *
     * @return The total count of names
     */
    public static int getTotalNameCount() {
        return MALE_NAMES.size() + FEMALE_NAMES.size();
    }

    /**
     * Gets a copy of the male names list.
     *
     * @return A new list containing all male names
     */
    public static List<String> getMaleNames() {
        return List.copyOf(MALE_NAMES);
    }

    /**
     * Gets a copy of the female names list.
     *
     * @return A new list containing all female names
     */
    public static List<String> getFemaleNames() {
        return List.copyOf(FEMALE_NAMES);
    }

    /**
     * Data class to hold a generated name and its associated gender.
     */
    public static class VillagerNameData {
        private final String name;
        private final Gender gender;

        public VillagerNameData(String name, Gender gender) {
            this.name = name;
            this.gender = gender;
        }

        public String getName() {
            return name;
        }

        public Gender getGender() {
            return gender;
        }

        @Override
        public String toString() {
            return "VillagerNameData{" +
                   "name='" + name + '\'' +
                   ", gender=" + gender +
                   '}';
        }
    }
}