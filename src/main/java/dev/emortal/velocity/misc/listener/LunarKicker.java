package dev.emortal.velocity.misc.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;

final class LunarKicker {

    @Subscribe
    void onPlayerJoin(@NotNull PlayerClientBrandEvent event) {
        Player player = event.getPlayer();
        String brand = event.getBrand().toLowerCase();

        if (brand.contains("lunar")) {
            ChatMessages.NO_LUNAR.send(player);
        }
    }
}
