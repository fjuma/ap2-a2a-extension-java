package io.ap2.a2a.extension.roles.utils;

import java.util.Map;
import java.util.UUID;

import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;

/**
 * A builder class for building an A2A Message object.
 */
public class A2aMessageBuilder {

    private final Message.Builder messageBuilder;

    public A2aMessageBuilder() {
        this.messageBuilder = new Message.Builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.AGENT);
    }

    /**
     * Adds a TextPart to the Message.
     *
     * @param text The text to be added to the Message.
     * @return The A2aMessageBuilder instance for method chaining.
     */
    public A2aMessageBuilder addText(String text) {
        messageBuilder.addPart(new TextPart(text));
        return this;
    }

    /**
     * Adds a new DataPart to the Message.
     * <p>
     * If a key is provided, then the DataPart's data dictionary will be set to {key: data}.
     * If no key is provided (null), then the data parameter must be a Map and will be used
     * directly as the DataPart's data dictionary.
     *
     * @param key The key to use for the data part. If null, data must be a Map.
     * @param data The data to accompany the key, if provided. Otherwise, the Map to be
     *             set within the DataPart object.
     * @return The A2aMessageBuilder instance for method chaining.
     */
    public A2aMessageBuilder addData(String key, Object data) {
        if (data == null) {
            return this;
        }

        Map<String, Object> nestedData;
        if (key != null) {
            nestedData = Map.of(key, data);
        } else {
            if (!(data instanceof Map)) {
                throw new IllegalArgumentException(
                        "When key is null, data must be a Map<String, Object>");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            nestedData = dataMap;
        }

        messageBuilder.addPart(new DataPart(nestedData));
        return this;
    }

    /**
     * Adds a DataPart to the Message using only a Map.
     *
     * @param data The Map to be set within the DataPart object.
     * @return The A2aMessageBuilder instance for method chaining.
     */
    public A2aMessageBuilder addData(Map<String, Object> data) {
        return addData(null, data);
    }

    /**
     * Sets the context id on the Message.
     *
     * @param contextId The context id to set.
     * @return The A2aMessageBuilder instance for method chaining.
     */
    public A2aMessageBuilder setContextId(String contextId) {
        messageBuilder.contextId(contextId);
        return this;
    }

    /**
     * Sets the task id on the Message.
     *
     * @param taskId The task id to set.
     * @return The A2aMessageBuilder instance for method chaining.
     */
    public A2aMessageBuilder setTaskId(String taskId) {
        messageBuilder.taskId(taskId);
        return this;
    }

    /**
     * Returns the Message object that has been built.
     *
     * @return The constructed Message object.
     */
    public Message build() {
        return messageBuilder.build();
    }
}
