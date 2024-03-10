package dev.emortal.velocity.party.commands.party;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.party.commands.party.subs.PartyBroadcastSub;
import dev.emortal.velocity.party.commands.party.subs.PartyCloseSub;
import dev.emortal.velocity.party.commands.party.subs.PartyDisbandSub;
import dev.emortal.velocity.party.commands.party.subs.PartyInviteSub;
import dev.emortal.velocity.party.commands.party.subs.PartyJoinSub;
import dev.emortal.velocity.party.commands.party.subs.PartyKickSub;
import dev.emortal.velocity.party.commands.party.subs.PartyLeaderSub;
import dev.emortal.velocity.party.commands.party.subs.PartyLeaveSub;
import dev.emortal.velocity.party.commands.party.subs.PartyListSub;
import dev.emortal.velocity.party.commands.party.subs.PartyOpenSub;
import dev.emortal.velocity.party.commands.party.subs.PartySettingsSub;
import dev.emortal.velocity.player.resolver.PlayerResolver;
import dev.emortal.velocity.player.suggestions.UsernameSuggesterProvider;
import dev.emortal.velocity.utils.CommandUtils;
import org.jetbrains.annotations.NotNull;

public final class PartyCommand extends EmortalCommand {

    public PartyCommand(@NotNull PartyService partyService, @NotNull PlayerResolver playerResolver,
                        @NotNull UsernameSuggesterProvider usernameSuggesterProvider, @NotNull FriendlyKafkaProducer kafkaProducer) {
        super("party", "p");

        super.setPlayerOnly();
        super.setDefaultExecutor(context ->
                ChatMessages.PARTY_HELP.send(context.getSource(), CommandUtils.getCommandName(context.getInput())));

        var playerArgument = argument("player", StringArgumentType.word(), usernameSuggesterProvider.online());
        super.addSyntax(new PartyInviteSub(partyService, playerResolver), literal("invite"), playerArgument);
        super.addSyntax(new PartyJoinSub(partyService, playerResolver), literal("join"), playerArgument);
        super.addSyntax(new PartyLeaveSub(partyService), literal("leave"));
        super.addSyntax(new PartyKickSub(partyService, playerResolver), literal("kick"), playerArgument);
        super.addSyntax(new PartyLeaderSub(partyService, playerResolver), literal("leader"), playerArgument);
        super.addSyntax(new PartyDisbandSub(partyService), literal("disband"));
        super.addSyntax(new PartyOpenSub(partyService), literal("open"));
        super.addSyntax(new PartyCloseSub(partyService), literal("close"));
        super.addSyntax(new PartyListSub(partyService), literal("list"));
        super.addSyntax(new PartySettingsSub(), literal("settings"));

        super.addConditionalSyntax(source -> source.hasPermission("command.party.broadcast"),
                new PartyBroadcastSub(kafkaProducer, partyService), literal("broadcast"));
    }
}
