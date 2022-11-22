package cc.towerdefence.velocity;

public class Environment {
    private static final boolean DEVELOPMENT = System.getenv("KUBERNETES_SERVICE_HOST") == null;
    private static final String HOSTNAME = System.getenv("HOSTNAME");

    public static boolean isProduction() {
        return !DEVELOPMENT;
    }

    public static String getHostname() {
        return HOSTNAME;
    }
}
