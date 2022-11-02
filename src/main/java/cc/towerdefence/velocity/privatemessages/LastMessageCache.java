package cc.towerdefence.velocity.privatemessages;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastMessageCache {
    // recipient, last person who messaged them
    private final Map<UUID, String> lastMessageMap = new ConcurrentHashMap<>();

    public void setLastMessage(UUID recipient, String sender) {
        this.lastMessageMap.put(recipient, sender);
    }

    public String getLastMessageSender(UUID recipient) {
        return this.lastMessageMap.get(recipient);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        this.lastMessageMap.remove(event.getPlayer().getUniqueId());
    }
}
