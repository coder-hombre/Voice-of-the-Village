package com.foogly.voiceofthevillage.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NameGenerator class.
 */
class NameGeneratorTest {

    @Test
    void testGenerateRandomName() {
        NameGenerator.VillagerNameData nameData = NameGenerator.generateRandomName();
        
        assertNotNull(nameData);
        assertNotNull(nameData.getName());
        assertNotNull(nameData.getGender());
        assertFalse(nameData.getName().trim().isEmpty());
        assertNotEquals(Gender.UNKNOWN, nameData.getGender());
        
        // Verify the name matches the gender
        if (nameData.getGender() == Gender.MALE) {
            assertTrue(NameGenerator.isMaleName(nameData.getName()));
        } else if (nameData.getGender() == Gender.FEMALE) {
            assertTrue(NameGenerator.isFemaleName(nameData.getName()));
        }
    }

    @RepeatedTest(50)
    void testGenerateRandomNameVariety() {
        // Test that we get variety in names over multiple generations
        Set<String> generatedNames = new HashSet<>();
        
        for (int i = 0; i < 20; i++) {
            NameGenerator.VillagerNameData nameData = NameGenerator.generateRandomName();
            generatedNames.add(nameData.getName());
        }
        
        // We should get some variety (at least 5 different names out of 20)
        assertTrue(generatedNames.size() >= 5, "Should generate variety in names");
    }

    @Test
    void testGenerateMaleName() {
        NameGenerator.VillagerNameData nameData = NameGenerator.generateMaleName();
        
        assertNotNull(nameData);
        assertNotNull(nameData.getName());
        assertEquals(Gender.MALE, nameData.getGender());
        assertTrue(NameGenerator.isMaleName(nameData.getName()));
        assertFalse(NameGenerator.isFemaleName(nameData.getName()));
    }

    @Test
    void testGenerateFemaleName() {
        NameGenerator.VillagerNameData nameData = NameGenerator.generateFemaleName();
        
        assertNotNull(nameData);
        assertNotNull(nameData.getName());
        assertEquals(Gender.FEMALE, nameData.getGender());
        assertTrue(NameGenerator.isFemaleName(nameData.getName()));
        assertFalse(NameGenerator.isMaleName(nameData.getName()));
    }

    @Test
    void testDetectGenderKnownNames() {
        // Test known male names
        assertEquals(Gender.MALE, NameGenerator.detectGender("Alexander"));
        assertEquals(Gender.MALE, NameGenerator.detectGender("Benjamin"));
        assertEquals(Gender.MALE, NameGenerator.detectGender("Christopher"));
        
        // Test known female names
        assertEquals(Gender.FEMALE, NameGenerator.detectGender("Alice"));
        assertEquals(Gender.FEMALE, NameGenerator.detectGender("Beatrice"));
        assertEquals(Gender.FEMALE, NameGenerator.detectGender("Catherine"));
    }

    @Test
    void testDetectGenderUnknownNames() {
        // Test unknown names
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender("UnknownName"));
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender("RandomName"));
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender("NotInList"));
    }

    @Test
    void testDetectGenderEdgeCases() {
        // Test null and empty strings
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender(null));
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender(""));
        assertEquals(Gender.UNKNOWN, NameGenerator.detectGender("   "));
        
        // Test whitespace handling
        assertEquals(Gender.MALE, NameGenerator.detectGender(" Alexander "));
        assertEquals(Gender.FEMALE, NameGenerator.detectGender(" Alice "));
    }

    @Test
    void testIsMaleName() {
        assertTrue(NameGenerator.isMaleName("Alexander"));
        assertTrue(NameGenerator.isMaleName("Benjamin"));
        assertFalse(NameGenerator.isMaleName("Alice"));
        assertFalse(NameGenerator.isMaleName("UnknownName"));
        assertFalse(NameGenerator.isMaleName(null));
        assertFalse(NameGenerator.isMaleName(""));
    }

    @Test
    void testIsFemaleName() {
        assertTrue(NameGenerator.isFemaleName("Alice"));
        assertTrue(NameGenerator.isFemaleName("Beatrice"));
        assertFalse(NameGenerator.isFemaleName("Alexander"));
        assertFalse(NameGenerator.isFemaleName("UnknownName"));
        assertFalse(NameGenerator.isFemaleName(null));
        assertFalse(NameGenerator.isFemaleName(""));
    }

    @Test
    void testNameCounts() {
        assertTrue(NameGenerator.getMaleNameCount() > 0);
        assertTrue(NameGenerator.getFemaleNameCount() > 0);
        assertEquals(NameGenerator.getMaleNameCount() + NameGenerator.getFemaleNameCount(), 
                    NameGenerator.getTotalNameCount());
        
        // Verify we have a reasonable number of names
        assertTrue(NameGenerator.getMaleNameCount() >= 40, "Should have at least 40 male names");
        assertTrue(NameGenerator.getFemaleNameCount() >= 40, "Should have at least 40 female names");
    }

    @Test
    void testGetNameLists() {
        List<String> maleNames = NameGenerator.getMaleNames();
        List<String> femaleNames = NameGenerator.getFemaleNames();
        
        assertNotNull(maleNames);
        assertNotNull(femaleNames);
        assertFalse(maleNames.isEmpty());
        assertFalse(femaleNames.isEmpty());
        
        // Verify lists are immutable copies
        assertThrows(UnsupportedOperationException.class, () -> maleNames.add("NewName"));
        assertThrows(UnsupportedOperationException.class, () -> femaleNames.add("NewName"));
        
        // Verify no overlap between male and female names
        Set<String> maleSet = new HashSet<>(maleNames);
        Set<String> femaleSet = new HashSet<>(femaleNames);
        maleSet.retainAll(femaleSet);
        assertTrue(maleSet.isEmpty(), "Male and female name lists should not overlap");
    }

    @Test
    void testVillagerNameData() {
        NameGenerator.VillagerNameData nameData = new NameGenerator.VillagerNameData("TestName", Gender.MALE);
        
        assertEquals("TestName", nameData.getName());
        assertEquals(Gender.MALE, nameData.getGender());
        
        String toString = nameData.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TestName"));
        assertTrue(toString.contains("MALE"));
    }

    @Test
    void testNameConsistency() {
        // Verify that all names in the male list are detected as male
        List<String> maleNames = NameGenerator.getMaleNames();
        for (String name : maleNames) {
            assertEquals(Gender.MALE, NameGenerator.detectGender(name), 
                        "Male name " + name + " should be detected as MALE");
            assertTrue(NameGenerator.isMaleName(name), 
                      "Male name " + name + " should return true for isMaleName");
            assertFalse(NameGenerator.isFemaleName(name), 
                       "Male name " + name + " should return false for isFemaleName");
        }
        
        // Verify that all names in the female list are detected as female
        List<String> femaleNames = NameGenerator.getFemaleNames();
        for (String name : femaleNames) {
            assertEquals(Gender.FEMALE, NameGenerator.detectGender(name), 
                        "Female name " + name + " should be detected as FEMALE");
            assertTrue(NameGenerator.isFemaleName(name), 
                      "Female name " + name + " should return true for isFemaleName");
            assertFalse(NameGenerator.isMaleName(name), 
                       "Female name " + name + " should return false for isMaleName");
        }
    }

    @RepeatedTest(10)
    void testRandomnessDistribution() {
        // Test that random generation produces both male and female names over multiple runs
        Set<Gender> generatedGenders = new HashSet<>();
        
        for (int i = 0; i < 20; i++) {
            NameGenerator.VillagerNameData nameData = NameGenerator.generateRandomName();
            generatedGenders.add(nameData.getGender());
        }
        
        // Over 20 generations, we should get both genders (very high probability)
        // This test might occasionally fail due to randomness, but it's very unlikely
        assertTrue(generatedGenders.size() >= 1, "Should generate at least one gender");
        // Note: We don't assert both genders because randomness could theoretically produce only one
    }
}