package dev.emortal.velocity.resourcepack;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourcePackForcer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackForcer.class);

    private static final String PACK_URL = "https://github.com/EmortalMC/Resourcepack/releases/download/latest/pack.zip";

    private static final Component DL_PROMPT = Component.text("We love you");
    private static final Component DECLINED_PROMPT = Component.text(
            "Using the resource pack is required. It isn't big and only has to be downloaded once.\nIf the dialog is annoying, you can enable 'Server Resource Packs' when adding the server and the prompt will disappear.",
            NamedTextColor.GRAY);
    private static final Component FAILED_PROMPT = Component.text(
            "The resource pack download failed.\nIf the issue persists, contact a staff member",
            NamedTextColor.RED);

    private ResourcePackInfo resourcePackInfo;

    public ResourcePackForcer(@NotNull ProxyServer proxy) {
        this.updateResourcePackInfo(proxy);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> this.updateResourcePackInfo(proxy), 0, 15, TimeUnit.MINUTES);
    }

    private void updateResourcePackInfo(@NotNull ProxyServer proxy) {
        byte[] sha1 = this.fetchSha1();
        this.resourcePackInfo = proxy.createResourcePackBuilder(PACK_URL)
                .setHash(sha1)
                .setPrompt(DL_PROMPT)
                .setShouldForce(true)
                .build();

        LOGGER.info("Update resource pack info with hash {}", this.byteArrayToHexString(sha1));
    }

    public @NotNull String byteArrayToHexString(byte @NotNull [] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    @SneakyThrows
    private byte[] fetchSha1() {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fileInputStream = new URL(PACK_URL).openStream();
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fileInputStream.read(buffer);
            if (n > 0)
                digest.update(buffer, 0, n);
        }
        fileInputStream.close();
        return digest.digest();
    }

    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        if (event.getPreviousServer() != null) return; // Don't send the resource pack if the player is switching servers

        Player player = event.getPlayer();

        player.sendResourcePackOffer(this.resourcePackInfo);
    }

    @Subscribe
    public void onPlayerResourceStatus(@NotNull PlayerResourcePackStatusEvent event) {
        LOGGER.info("Player {} resource pack status {}", event.getPlayer().getUsername(), event.getStatus());
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case DECLINED -> player.disconnect(DECLINED_PROMPT);
            case FAILED_DOWNLOAD -> player.disconnect(FAILED_PROMPT);
        }
    }
}
