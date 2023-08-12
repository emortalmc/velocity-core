package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.ModuleData;
import dev.emortal.api.modules.ModuleProvider;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.velocity.CorePlugin;
import org.jetbrains.annotations.NotNull;

public record VelocityModuleEnvironment(@NotNull ModuleData data, @NotNull ModuleProvider moduleProvider,
                                        @NotNull CorePlugin plugin, @NotNull ProxyServer proxy) implements ModuleEnvironment {
}
