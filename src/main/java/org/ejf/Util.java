package org.ejf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Util {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, String>>> TYPE_REF =
            new TypeReference<Map<String, Map<String, String>>>() {};

    static String getFormattedCurrentProgressTime(Instant start, Instant finish) {
        Duration dur = Duration.between(start, finish);
        return dur.toHoursPart() + "h " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s";
    }

    public static String cleanText(String input) {
        if (input == null) {
            return null;
        }
        // Remove digits, spaces, and special symbols
        // [^a-zA-Z] means "not a letter" (^ negates the character class)
        return input.replaceAll("[^а-яА-Я]", "").toLowerCase();
    }

    /**
     * Save a Map<String, Map<String, String>> to a JSON file
     * @param nestedMap The nested map to save
     * @param filePath Path to the output file
     */
    public static void saveNestedMapToFile(Map<String, Map<String, String>> nestedMap, String filePath) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), nestedMap);
            System.out.println("Nested map saved to: " + filePath);
        } catch (Exception e) {
            System.err.println("Error saving nested map to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load a Map<String, Map<String, String>> from a JSON file
     * @param filePath Path to the input file
     * @return The loaded nested map, or empty map if file doesn't exist
     */
    public static Map<String, Map<String, String>> loadNestedMapFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File doesn't exist, returning empty map");
                return new HashMap<>();
            }

            return objectMapper.readValue(file, TYPE_REF);
        } catch (Exception e) {
            System.err.println("Error loading nested map from file: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
