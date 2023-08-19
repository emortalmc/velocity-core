package dev.emortal.testing;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class RandomIdGenerator {

    public static @NotNull String randomId() {
        return randomUUID().toString();
    }

    public static @NotNull UUID randomUUID() {
        return UUID.randomUUID();
    }

    private RandomIdGenerator() {
    }
}
