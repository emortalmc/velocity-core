package dev.emortal.velocity.party.commands.subs;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.command.CommandExecutor;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.party.commands.PartyCommand;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class PartyListSub implements CommandExecutor<CommandSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyListSub.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final TextColor LEADER_COLOR = TextColor.color(255, 225, 115);
    private static final String MESSAGE_CONTENT = """
            <color:#2383d1><strikethrough>          </strikethrough> <bold><color:#2ba0ff>ʏᴏᴜʀ ᴘᴀʀᴛʏ </bold>(<party_size>) <strikethrough>          </strikethrough></color>
                        
            <member_content>
                        
            <color:#2383d1><strikethrough>                                          </strikethrough>""";

    private final @NotNull PartyService partyService;

    public PartyListSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandContext<CommandSource> context) {
        Player executor = (Player) context.getSource();

        Party party;
        try {
            party = this.partyService.getPartyByPlayer(executor.getUniqueId());
        } catch (StatusRuntimeException exception) {
            LOGGER.error("Failed to get party for player {}", executor.getUsername(), exception);
            executor.sendMessage(PartyCommand.ERROR_MESSAGE);
            return;
        }

        if (party == null) {
            executor.sendMessage(PartyCommand.NOT_IN_PARTY_MESSAGE);
            return;
        }

        var partySize = Placeholder.unparsed("party_size", String.valueOf(party.getMembersCount()));
        var memberContent = Placeholder.component("member_content", this.createMessageContent(party));
        executor.sendMessage(MINI_MESSAGE.deserialize(MESSAGE_CONTENT, partySize, memberContent));
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
