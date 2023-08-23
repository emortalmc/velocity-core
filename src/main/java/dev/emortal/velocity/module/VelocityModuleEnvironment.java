package dev.emortal.velocity.module;

import dev.emortal.api.modules.ModuleProvider;
import dev.emortal.api.modules.annotation.ModuleData;
import dev.emortal.api.modules.env.ModuleEnvironment;
import dev.emortal.api.service.mcplayer.McPlayerService;
import dev.emortal.velocity.adapter.AdapterContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// We include the player service in the environment rather than loading it as a module as it is such a core service
// that the vast majority of other components require it, and I would rather it not depend on being loaded and provided by a module.
public record VelocityModuleEnvironment(@NotNull ModuleData data, @NotNull ModuleProvider moduleProvider,
                                        @NotNull AdapterContext adapters, @Nullable McPlayerService playerService) implements ModuleEnvironment {
}
