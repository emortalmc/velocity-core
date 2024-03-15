package dev.emortal.velocity.party.commands.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.EmortalCommand;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.party.commands.event.subs.CreateSub;
import dev.emortal.velocity.party.commands.event.subs.DeleteSub;
import dev.emortal.velocity.party.commands.event.subs.ListSub;

public class EventCommand extends EmortalCommand {

    public EventCommand(PartyService partyService) {
        super("event");

        super.setCondition(source -> source.hasPermission("command.event"));

        // List
        ListSub listSub = new ListSub(partyService);
        super.setDefaultExecutor(listSub);
        super.addSyntax(listSub, literal("list"));

        // Create
        super.addSyntax(this::createUsage, literal("create"));
        super.addSyntax(this::createUsage, literal("create"), literal("help"));
        super.addSyntax(new CreateSub(partyService), literal("create"), argument("id", StringArgumentType.word()),
                argument("showTime", StringArgumentType.word()), argument("startTime", StringArgumentType.word()));

        // Delete
        DeleteSub deleteSub = new DeleteSub(partyService);
        super.addSyntax(deleteSub, literal("delete"));
        super.addSyntax(deleteSub, literal("delete"), argument("id", StringArgumentType.word()));
    }

    private void createUsage(CommandContext<CommandSource> context) {
        ChatMessages.EVENT_CREATE_USAGE.send(context.getSource());
    }
}
