package io.ap2.a2a.extension.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;

/**
 * Helper functions for working with A2A Artifact objects.
 */
public class ArtifactUtils {

    /**
     * Finds all canonical objects of the given type in the artifacts.
     *
     * @param artifacts The list of artifacts to be searched
     * @param dataKey The key of the DataPart to search for
     * @param <T> The type of the canonical object
     * @return A list of canonical objects of the given type in the artifacts
     */
    public static <T> List<T> findCanonicalObjects(
            List<Artifact> artifacts,
            String dataKey,
            Class<T> clazz) {
        List<T> canonicalObjects = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            for (Part<?> part : artifact.parts()) {
                if (part instanceof DataPart) {
                    DataPart dataPart = (DataPart) part;
                    if (dataPart.getData().containsKey(dataKey)) {
                        Object value = dataPart.getData().get(dataKey);
                        if (clazz.isInstance(value)) {
                            T typedValue = clazz.cast(value);
                            canonicalObjects.add(typedValue);
                        }
                    }
                }
            }
        }
        return canonicalObjects;
    }

    /**
     * Returns the first DataPart encountered in all the given artifacts.
     *
     * @param artifacts The artifacts to be searched for a DataPart
     * @return The data contents within the first found DataPart, or empty map if none found
     */
    public static Map<String, Object> getFirstDataPart(List<Artifact> artifacts) {
        for (Artifact artifact : artifacts) {
            for (Part<?> part : artifact.parts()) {
                if (part instanceof DataPart) {
                    return ((DataPart) part).getData();
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the only element in a list.
     *
     * @param list The list expected to contain exactly one element
     * @param <T> The type of elements in the list
     * @return The only element in the list
     * @throws IllegalArgumentException if the list is empty or has more than one element
     */
    public static <T> T only(List<T> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List is empty.");
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("List has more than one element.");
        }
        return list.get(0);
    }

    private ArtifactUtils() {
        // Utility class should not be instantiated
    }
}
