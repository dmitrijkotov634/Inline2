package com.wavecat.inline;

import androidx.annotation.NonNull;

import org.luaj.vm2.LuaValue;

public class Command {

    private final String category;
    private final LuaValue callable;
    private final String description;

    public Command(String category, LuaValue callable, String description) {
        this.category = category;
        this.callable = callable;
        this.description = description;
    }

    @SuppressWarnings("unused")
    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LuaValue getCallable() {
        return callable;
    }

    @NonNull
    @Override
    public String toString() {
        return "Command{" +
                "category='" + category + '\'' +
                ", callable=" + callable +
                ", description='" + description + '\'' +
                '}';
    }
}
