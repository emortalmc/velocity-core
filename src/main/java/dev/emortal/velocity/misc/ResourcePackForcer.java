package dev.emortal.velocity.misc;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import dev.emortal.velocity.adapter.resourcepack.ResourcePackProvider;
import dev.emortal.velocity.adapter.scheduler.EmortalScheduler;
import dev.emortal.velocity.lang.ChatMessages;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

final class ResourcePackForcer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackForcer.class);

    private static final String PACK_URL = "https://github.com/EmortalMC/Resourcepack/releases/download/latest/pack.zip";

    private ResourcePackInfo resourcePackInfo;

    ResourcePackForcer(@NotNull ResourcePackProvider resourcePackProvider, @NotNull EmortalScheduler scheduler) {
        this.updateResourcePackInfo(resourcePackProvider);
        scheduler.repeat(() -> this.updateResourcePackInfo(resourcePackProvider), 15, TimeUnit.MINUTES);
    }

    private void updateResourcePackInfo(@NotNull ResourcePackProvider resourcePackProvider) {
        byte[] sha1 = this.fetchSha1();
        this.resourcePackInfo = resourcePackProvider.createResourcePack(PACK_URL, sha1, ChatMessages.RESOURCE_PACK_DOWNLOAD.parse(), true);

        LOGGER.info("Update resource pack info with hash {}", byteArrayToHexString(sha1));
    }

    private static @NotNull String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    @SneakyThrows
    private byte[] fetchSha1() {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fileInputStream = new URI(PACK_URL).toURL().openStream();

        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fileInputStream.read(buffer);
            if (n > 0) digest.update(buffer, 0, n);
        }

        fileInputStream.close();
        return digest.digest();
    }

    @Subscribe
    void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        if (event.getPreviousServer() != null) return; // Don't send the resource pack if the player is switching servers

        event.getPlayer().sendResourcePackOffer(this.resourcePackInfo);
    }

    @Subscribe
    void onPlayerResourceStatus(@NotNull PlayerResourcePackStatusEvent event) {
        LOGGER.info("Player {} resource pack status {}", event.getPlayer().getUsername(), event.getStatus());
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case DECLINED -> ChatMessages.RESOURCE_PACK_DECLINED.send(player);
            case FAILED_DOWNLOAD -> ChatMessages.RESOURCE_PACK_FAILED.send(player);
        }
    }
}
