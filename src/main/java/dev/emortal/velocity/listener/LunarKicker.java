package dev.emortal.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import net.kyori.adventure.text.Component;

public class LunarKicker {

    @Subscribe
    public void onPlayerJoin(PlayerClientBrandEvent event) {
        if (event.getBrand().toLowerCase().contains("lunar")) {
            event.getPlayer().disconnect(Component.text("EmortalMC has a strict no Lunar Client policy\n\nPlease consider using a more capable client, such as Fabric"));
        }
    }

}
