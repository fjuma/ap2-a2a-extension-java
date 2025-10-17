package io.ap2.a2a.extension.roles.merchant.subagents;

import java.util.List;

/**
 * Wrapper class for a list of generated items.
 *
 * This is needed because LangChain4j doesn't support returning List<T> directly.
 */
public class GeneratedItems {

    private List<GeneratedItem> items;

    public GeneratedItems() {
    }

    public GeneratedItems(List<GeneratedItem> items) {
        this.items = items;
    }

    public List<GeneratedItem> getItems() {
        return items;
    }

    public void setItems(List<GeneratedItem> items) {
        this.items = items;
    }
}
