package io.ap2.a2a.extension.roles.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.a2a.spec.DataPart;

/**
 * Utility methods for working with A2A messages.
 */
public class MessageUtils {

    /**
     * Finds a single data part by key.
     *
     * @param key The key to search for
     * @param dataParts The list of data parts
     * @return The value associated with the key, or null if not found
     */
    public static Object findDataPart(String key, List<DataPart> dataParts) {
        for (DataPart dataPart : dataParts) {
            if (dataPart.getData().containsKey(key)) {
                return dataPart.getData().get(key);
            }
        }
        return null;
    }

    /**
     * Finds all data parts matching a key.
     *
     * @param key The key to search for
     * @param dataParts The list of data parts
     * @return A list of values associated with the key
     */
    public static List<Map<String, Object>> findDataParts(String key, List<DataPart> dataParts) {
        return dataParts.stream()
                .map(DataPart::getData)
                .filter(dataMap -> dataMap.containsKey(key))
                .collect(Collectors.toList());
    }

    /**
     * Parses a canonical object from data parts.
     *
     * @param key The key to search for
     * @param dataParts The list of data parts
     * @param clazz The class type to parse into
     * @param <T> The type parameter
     * @return The parsed object, or null if not found
     */
    public static <T> T parseCanonicalObject(String key, List<DataPart> dataParts, Class<T> clazz) {
        for (DataPart part : dataParts) {
            if (part.getData().containsKey(key)) {
                return (T) part.getData().get(key);
            }
        }
        return null;
    }
}
