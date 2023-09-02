package dev.emortal.velocity;

import org.jetbrains.annotations.NotNull;

public final class Environment {
    private static final boolean DEVELOPMENT = Boolean.parseBoolean(System.getenv("DEVELOPMENT"));
    private static final boolean KUBERNETES = System.getenv("KUBERNETES_SERVICE_HOST") == null;
    private static final String HOSTNAME = System.getenv("HOSTNAME");

    public static boolean isKubernetes() {
        return KUBERNETES;
    }

    public static boolean isDevelopment() {
        return DEVELOPMENT;
    }

    public static @NotNull String getHostname() {
        return HOSTNAME;
    }

    private Environment() {
    }
}
