package dev.emortal.velocity;

import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import io.pyroscope.labels.Pyroscope;

import java.util.Map;

public class PyroscopeHandler {
    private static final String FLEET_NAME = "velocity";
    private static final String PYROSCOPE_ADDRESS = System.getenv("PYROSCOPE_SERVER_ADDRESS");

    public static void register() {
        Pyroscope.setStaticLabels(Map.of(
                        "fleet", FLEET_NAME,
                        "pod", Environment.getHostname()
                )
        );

        PyroscopeAgent.start(
                new PyroscopeAgent.Options.Builder(
                        new Config.Builder()
                                .setApplicationName(FLEET_NAME)
                                .setProfilingEvent(EventType.ITIMER)
                                .setFormat(Format.JFR)
                                .setServerAddress(PYROSCOPE_ADDRESS)
                                .build()
                ).build()
        );
    }
}
