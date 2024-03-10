package dev.emortal.velocity.party.commands.party.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class PartyListSub implements EmortalCommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyListSub.class);

    private static final TextColor LEADER_COLOR = TextColor.color(255, 225, 115);

    private final @NotNull PartyService partyService;

    public PartyListSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player executor = (Player) source;

        Party party;
        try {
            party = this.partyService.getPartyByPlayer(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get party of '{}'", executor.getUsername(), exception);
            ChatMessages.GENERIC_ERROR.send(executor);
            return;
        }

        if (party == null) {
            ChatMessages.ERROR_YOU_NOT_IN_PARTY.send(executor);
            return;
        }

        ChatMessages.PARTY_LIST.send(executor, party.getMembersCount(), this.createMessageContent(party));
    }

    private @NotNull Component createMessageContent(@NotNull Party party) {
        TextComponent.Builder result = Component.text();

        List<PartyMember> members = new ArrayList<>();
        PartyMember leader = null;

        for (PartyMember member : party.getMembersList()) {
            if (member.getId().equals(party.getLeaderId())) {
                leader = member;
                continue;
            }
            members.add(member);
        }
        if (leader == null) throw new IllegalStateException("Leader was not in members list!");

        result.append(Component.text("⭐ ", LEADER_COLOR));
        result.append(Component.text(leader.getUsername(), LEADER_COLOR));

        for (PartyMember member : members) {
            result.append(Component.text(", ", NamedTextColor.GRAY));
            result.append(Component.text(member.getUsername(), NamedTextColor.GRAY));
        }

        return result.build();
    }
}
