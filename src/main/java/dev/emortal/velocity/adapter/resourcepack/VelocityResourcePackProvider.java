package dev.emortal.velocity.adapter.resourcepack;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class VelocityResourcePackProvider implements ResourcePackProvider {

    private final ProxyServer proxy;

    public VelocityResourcePackProvider(@NotNull ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public @NotNull ResourcePackInfo createResourcePack(@NotNull String url, byte@NotNull[] hash, @NotNull Component prompt, boolean force) {
        return this.proxy.createResourcePackBuilder(url)
                .setHash(hash)
                .setPrompt(prompt)
                .setShouldForce(force)
                .build();
    }
}
