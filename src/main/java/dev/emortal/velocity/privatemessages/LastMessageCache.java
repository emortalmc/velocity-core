package dev.emortal.velocity.privatemessages;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LastMessageCache {

    // recipient, last person who messaged them
    private final Map<UUID, String> lastMessageMap = new ConcurrentHashMap<>();

    void setLastMessage(@NotNull UUID recipient, @NotNull String sender) {
        this.lastMessageMap.put(recipient, sender);
    }

    public @Nullable String getLastMessageSender(@NotNull UUID recipient) {
        return this.lastMessageMap.get(recipient);
    }

    @Subscribe
    public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        this.lastMessageMap.remove(event.getPlayer().getUniqueId());
    }
}
