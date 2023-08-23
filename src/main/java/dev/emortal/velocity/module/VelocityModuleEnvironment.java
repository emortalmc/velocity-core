package dev.emortal.velocity.module;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.modules.ModuleProvider;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.velocity.CorePlugin;
import dev.emortal.velocity.player.provider.PlayerProvider;
import org.jetbrains.annotations.NotNull;

public record VelocityModuleEnvironment(@NotNull ModuleData data, @NotNull ModuleProvider moduleProvider,
                                        @NotNull CorePlugin plugin, @NotNull ProxyServer proxy,
                                        @NotNull PlayerProvider playerProvider) implements ModuleEnvironment {
}
