package dev.emortal.velocity.adapter.resourcepack;

import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface ResourcePackProvider {

    @NotNull ResourcePackInfo createResourcePack(@NotNull String url, byte@NotNull[] hash, @NotNull Component prompt, boolean force);
}
