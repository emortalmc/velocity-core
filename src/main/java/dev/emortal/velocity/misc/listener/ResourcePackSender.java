package dev.emortal.velocity.misc.listener;

import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import dev.emortal.velocity.adapter.resourcepack.ResourcePackProvider;
import dev.emortal.velocity.adapter.scheduler.EmortalScheduler;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class ResourcePackSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackSender.class);

    private static final String PACK_URL = "https://github.com/emortalmc/Resourcepack/releases/download/latest/pack.zip";

    private ResourcePackInfo resourcePackInfo;

    private final Set<UUID> rpAcceptedPlayers = new HashSet<>();
    private final ConcurrentHashMap<UUID, Continuation> pendingPack = new ConcurrentHashMap<>();

    ResourcePackSender(@NotNull ResourcePackProvider resourcePackProvider, @NotNull EmortalScheduler scheduler) {
        this.updateResourcePackInfo(resourcePackProvider);
        scheduler.repeat(() -> this.updateResourcePackInfo(resourcePackProvider), 30, TimeUnit.SECONDS);
    }

    private void updateResourcePackInfo(@NotNull ResourcePackProvider resourcePackProvider) {
        byte[] sha1;
        try {
            sha1 = this.fetchSha1();
        } catch (NoSuchAlgorithmException | IOException exception) {
            LOGGER.error("Failed to update resource pack info!", exception);
            return;
        }

        this.resourcePackInfo = resourcePackProvider.createResourcePack(PACK_URL, sha1, ChatMessages.RESOURCE_PACK_DOWNLOAD.get(), true);
        LOGGER.info("Update resource pack info with hash {}", byteArrayToHexString(sha1));
    }

    private static @NotNull String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    private byte[] fetchSha1() throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        URI uri = URI.create(PACK_URL);
        try (InputStream input = uri.toURL().openStream()) {
            int n = 0;
            byte[] buffer = new byte[8192];

            while (n != -1) {
                n = input.read(buffer);
                if (n > 0) digest.update(buffer, 0, n);
            }
        }

        return digest.digest();
    }

    @Subscribe
    void onPlayerConfiguration(PlayerConfigurationEvent event, Continuation continuation) {
        if (this.rpAcceptedPlayers.contains(event.player().getUniqueId())) {
            continuation.resume();
            return; // Don't send the resource pack if the player has already got it
        }

        event.player().sendResourcePackOffer(this.resourcePackInfo);
        pendingPack.put(event.player().getUniqueId(), continuation);
    }

    @Subscribe
    void onPlayerResourceStatus(@NotNull PlayerResourcePackStatusEvent event) {
        LOGGER.info("Player {} resource pack status {}", event.getPlayer().getUsername(), event.getStatus());
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case ACCEPTED -> {
                this.rpAcceptedPlayers.add(event.getPlayer().getUniqueId());
                this.pendingPack.get(player.getUniqueId()).resume();
            }
            case DECLINED -> {
                player.disconnect(ChatMessages.RESOURCE_PACK_DECLINED.get());
                this.pendingPack.get(player.getUniqueId()).resume();
            }
            case FAILED_DOWNLOAD, INVALID_URL, DISCARDED -> {
                ChatMessages.RESOURCE_PACK_FAILED.send(player);
                this.pendingPack.get(player.getUniqueId()).resume();
            }
        }
        this.pendingPack.remove(player.getUniqueId());
    }

    @Subscribe
    void onPlayerDisconnect(DisconnectEvent event) {
        this.rpAcceptedPlayers.remove(event.getPlayer().getUniqueId());
    }

}
