package dev.emortal.velocity.player.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import dev.emortal.velocity.lang.ChatMessages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public final class PlayerJoinQuitListener {
    private static final Key JOIN_QUIT_SOUND = Key.key(Key.MINECRAFT_NAMESPACE, "entity.item.pickup");

    private final Audience audience;

    public PlayerJoinQuitListener(@NotNull Audience audience) {
        this.audience = audience;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (event.getPlayer().getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21_11)) {
            event.setResult(ResultedEvent.ComponentResult.denied(
                    Component.text("EmortalMC requires Minecraft version 1.21.11")
            ));
        }

    }

    @Subscribe
    public void onJoin(@NotNull PostLoginEvent event) {
        ChatMessages.JOIN.send(this.audience, event.getPlayer().getUsername());
        this.audience.playSound(Sound.sound(JOIN_QUIT_SOUND, Sound.Source.MASTER, 1F, 1.2F));
    }

    @Subscribe
    public void onQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) return;

        ChatMessages.QUIT.send(this.audience, event.getPlayer().getUsername());
        this.audience.playSound(Sound.sound(JOIN_QUIT_SOUND, Sound.Source.MASTER, 1F, 0.5F));
    }
}
