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

import java.time.Duration;
import java.util.Map;

@ModuleData(name = "pyroscope")
public final class PyroscopeModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(PyroscopeModule.class);

    private static final String FLEET_NAME = "velocity";
    private static final String PYROSCOPE_ADDRESS = System.getenv("PYROSCOPE_ADDRESS");

    public PyroscopeModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        if (PYROSCOPE_ADDRESS == null) {
            LOGGER.warn("Pyroscope address not set. Pyroscope profiling events will not be sent.");
            return false;
        }

        this.setupPyroscope();

        return true;
    }

    private void setupPyroscope() {
        Pyroscope.setStaticLabels(Map.of(
                "fleet", FLEET_NAME,
                "pod", Environment.getHostname()
        ));

        Config config = new Config.Builder()
                .setApplicationName(FLEET_NAME)
                .setProfilingEvent(EventType.ITIMER)
                .setFormat(Format.JFR)
                .setProfilingLock("10ms")
                .setProfilingAlloc("512k")
                .setUploadInterval(Duration.ofSeconds(10))
                .setServerAddress(PYROSCOPE_ADDRESS)
                .build();

        String labels = Pyroscope.getStaticLabels().toString();
        LOGGER.info("Starting Pyroscope with: [{}, applicationName={}]", labels, config.applicationName);

        PyroscopeAgent.start(new PyroscopeAgent.Options.Builder(config).build());
    }

    @Override
    public void onUnload() {
        // do nothing
    }
}
