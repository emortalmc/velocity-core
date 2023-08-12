package dev.emortal.velocity;

import org.jetbrains.annotations.NotNull;

public final class Environment {
    private static final boolean DEVELOPMENT = System.getenv("KUBERNETES_SERVICE_HOST") == null;
    private static final String HOSTNAME = System.getenv("HOSTNAME");

    public static boolean isProduction() {
        return !DEVELOPMENT;
    }

    public static @NotNull String getHostname() {
        return HOSTNAME;
    }
}
