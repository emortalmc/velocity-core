package dev.emortal.velocity.party.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.emortal.api.grpc.mcplayer.McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.CommandConditions;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.party.commands.subs.PartyCloseSub;
import dev.emortal.velocity.player.UsernameSuggestions;
import dev.emortal.velocity.party.PartyCache;
import dev.emortal.velocity.party.commands.subs.PartyDisbandSub;
import dev.emortal.velocity.party.commands.subs.PartyInviteSub;
import dev.emortal.velocity.party.commands.subs.PartyJoinSub;
import dev.emortal.velocity.party.commands.subs.PartyKickSub;
import dev.emortal.velocity.party.commands.subs.PartyLeaderSub;
import dev.emortal.velocity.party.commands.subs.PartyLeaveSub;
import dev.emortal.velocity.party.commands.subs.PartyListSub;
import dev.emortal.velocity.party.commands.subs.PartyOpenSub;
import dev.emortal.velocity.party.commands.subs.PartySettingsSub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

public final class PartyCommand extends EmortalCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static final Component ERROR_MESSAGE = MINI_MESSAGE.deserialize("<red>An error occurred");
    public static final Component NOT_IN_PARTY_MESSAGE = MINI_MESSAGE.deserialize("<red>You are not in a party");

    private static final Component HELP_MESSAGE = MINI_MESSAGE.deserialize("""
            <light_purple>------ Party Help ------
            /party invite <player>
            /party join <player>
            /party leave
            /party list
            /party open
                        
            /party kick <player>
            /party leader <player>
            /party disband
                        
            /party settings
            ----------------------</light_purple>""");

    public PartyCommand(@NotNull PartyService partyService, @NotNull UsernameSuggestions usernameSuggestions, @NotNull PartyCache partyCache) {
        super("party");

        super.setCondition(CommandConditions.playerOnly());
        super.setDefaultExecutor(context -> context.getSource().sendMessage(HELP_MESSAGE));

        var playerArgument = argument("player", StringArgumentType.word(), usernameSuggestions.command(FilterMethod.ONLINE));
        super.addSyntax(new PartyInviteSub(partyService), literal("invite"), playerArgument);
        super.addSyntax(new PartyJoinSub(partyService), literal("join"), playerArgument);
        super.addSyntax(new PartyLeaveSub(partyService), literal("leave"), playerArgument);
        super.addSyntax(new PartyKickSub(partyService), literal("kick"), playerArgument);
        super.addSyntax(new PartyLeaderSub(partyService), literal("leader"), playerArgument);
        super.addSyntax(new PartyDisbandSub(partyService), literal("disband"));
        super.addSyntax(new PartyOpenSub(partyService), literal("open"));
        super.addSyntax(new PartyCloseSub(partyService), literal("close"));
        super.addSyntax(new PartyListSub(partyService), literal("list"));
        super.addSyntax(new PartySettingsSub(), literal("settings"));
    }
}
