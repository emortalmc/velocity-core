package dev.emortal.velocity;

import dev.emortal.api.modules.Module;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;
import io.pyroscope.labels.Pyroscope;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ModuleData(name = "pyroscope")
public final class PyroscopeModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(PyroscopeModule.class);

    private static final String FLEET_NAME = "velocity";
    private static final String PYROSCOPE_ADDRESS = System.getenv("PYROSCOPE_SERVER_ADDRESS");

    public PyroscopeModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        if (PYROSCOPE_ADDRESS == null) {
            LOGGER.warn("Pyroscope address not set. Pyroscope profiling events will not be sent.");
            return false;
        }

        Pyroscope.setStaticLabels(Map.of(
                "fleet", FLEET_NAME,
                "pod", Environment.getHostname())
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

        return true;
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
