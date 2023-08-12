package dev.emortal.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class LunarKicker {
    private static final Component NO_LUNAR = Component.text("""
            EmortalMC has a strict no Lunar Client policy

            Please consider using a more capable client, such as Fabric""");

    @Subscribe
    public void onPlayerJoin(@NotNull PlayerClientBrandEvent event) {
        Player player = event.getPlayer();
        String brand = event.getBrand().toLowerCase();

        if (brand.contains("lunar")) {
            player.disconnect(NO_LUNAR);
        }
    }
}
