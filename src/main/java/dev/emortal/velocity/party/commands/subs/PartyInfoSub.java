package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.velocity.party.PartyCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public class PartyInfoSub {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @NotNull PartyCache partyCache;

    public PartyInfoSub(@NotNull PartyCache partyCache) {
        this.partyCache = partyCache;
    }

    public int execute(CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();

        PartyCache.CachedParty party = this.partyCache.getPlayerParty(executor.getUniqueId());
        if (party == null) {
            executor.sendMessage(MINI_MESSAGE.deserialize("<red>You are not in a party."));
            return 0;
        }

        executor.sendMessage(MINI_MESSAGE.deserialize("<green>Party info: " + party));
        return 1;
    }
}
