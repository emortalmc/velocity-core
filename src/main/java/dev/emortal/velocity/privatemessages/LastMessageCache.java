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
    private final Map<UUID, UUID> lastRecipientMap = new ConcurrentHashMap<>();

    void setLastRecipient(@NotNull UUID recipientId, @NotNull UUID senderId) {
        this.lastRecipientMap.put(recipientId, senderId);
    }

    public @Nullable UUID getLastMessageSender(@NotNull UUID recipientId) {
        return this.lastRecipientMap.get(recipientId);
    }

    @Subscribe
    void onPlayerDisconnect(@NotNull DisconnectEvent event) {
        this.lastRecipientMap.remove(event.getPlayer().getUniqueId());
    }
}
