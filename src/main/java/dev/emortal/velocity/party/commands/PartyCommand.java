package dev.emortal.velocity.party.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.emortal.api.grpc.mcplayer.McPlayerProto;
import dev.emortal.velocity.general.UsernameSuggestions;
import dev.emortal.velocity.party.commands.subs.PartyInviteSub;
import dev.emortal.velocity.party.commands.subs.PartyJoinSub;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PartyCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Component HELP_MESSAGE = MINI_MESSAGE.deserialize("""
            <light_purple>------ Party Help ------
            /party invite <player>
            /party join <player>
            /party leave
            /party list
                        
            /party kick <player>
            /party leader <player>
            /party disband
                        
            /party settings
            ----------------------</light_purple>""");

    private static final Component SETTINGS_HELP_MESSAGE = MINI_MESSAGE.deserialize("""
            <light_purple>----- Party Settings Help -----
            /party settings
            /party settings <setting> <value>
            ---------------------------</light_purple>""");

    private static final Component USAGE_PARTY_INVITE = Component.text("Usage: /party invite <player>", NamedTextColor.RED);
    private static final Component USAGE_PARTY_JOIN = Component.text("Usage: /party join <player>", NamedTextColor.RED);
    private static final Component USAGE_PARTY_KICK = Component.text("Usage: /party kick <player>", NamedTextColor.RED);
    private static final Component USAGE_PARTY_LEADER = Component.text("Usage: /party leader <player>", NamedTextColor.RED);

    private final UsernameSuggestions usernameSuggestions;

    private final PartyInviteSub inviteSub = new PartyInviteSub();
    private final PartyJoinSub joinSub = new PartyJoinSub();

    public PartyCommand(ProxyServer proxy, UsernameSuggestions usernameSuggestions) {
        this.usernameSuggestions = usernameSuggestions;

        proxy.getCommandManager().register(this.createCommand());
    }

    public BrigadierCommand createCommand() {
        return new BrigadierCommand(
                LiteralArgumentBuilder.<CommandSource>literal("party")
                        .then(LiteralArgumentBuilder.<CommandSource>literal("create").executes(context -> {
                            // todo
                            return 1;
                        })
                        .then(LiteralArgumentBuilder.<CommandSource>literal("invite").executes(context -> {
                                    context.getSource().sendMessage(USAGE_PARTY_INVITE);
                                    return 1;
                                }).then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                        .suggests(this.usernameSuggestions.command(McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                        .executes(this.inviteSub::execute))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("join").executes(context -> {
                                            context.getSource().sendMessage(USAGE_PARTY_JOIN);
                                            return 1;
                                        })
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                                .suggests(this.usernameSuggestions.command(McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                                .executes(this.joinSub::execute))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("leave").executes(context -> {
                            // todo
                            return 1;
                        }))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("kick").executes(context -> {
                                            context.getSource().sendMessage(USAGE_PARTY_KICK);
                                            return 1;
                                        })
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                                .suggests(this.usernameSuggestions.command(McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                                .executes(context -> {
                                                    // todo
                                                    return 1;
                                                }))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("leader").executes(context -> {
                                            context.getSource().sendMessage(USAGE_PARTY_LEADER);
                                            return 1;
                                        })
                                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                                .suggests(this.usernameSuggestions.command(McPlayerProto.SearchPlayersByUsernameRequest.FilterMethod.ONLINE))
                                                .executes(context -> {
                                                    // todo
                                                    return 1;
                                                }))
                        )
                        .then(LiteralArgumentBuilder.<CommandSource>literal("list").executes(context -> {
                            // todo
                            return 1;
                        }))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("disband").executes(context -> {
                            // todo
                            return 1;
                        }))
                        .then(LiteralArgumentBuilder.<CommandSource>literal("settings").executes(context -> {
                            context.getSource().sendMessage(SETTINGS_HELP_MESSAGE);
                            return 1;
                        }))
        );
    }
}
