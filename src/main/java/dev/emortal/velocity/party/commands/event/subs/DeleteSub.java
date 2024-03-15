package dev.emortal.velocity.party.commands.event.subs;

import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.service.party.DeleteEventResult;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;

public class DeleteSub implements EmortalCommandExecutor {

    private final @NotNull PartyService partyService;

    public DeleteSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        String id;
        if (arguments.hasArgument("id")) id = arguments.getArgument("id", String.class);
        else id = null;

        DeleteEventResult result = this.partyService.deleteEvent(id);
        switch (result) {
            case SUCCESS -> {
                if (id == null) ChatMessages.CURRENT_EVENT_DELETED.send(source);
                else ChatMessages.EVENT_DELETED.send(source, id);
            }
            case NO_CURRENT_EVENT -> ChatMessages.ERROR_NO_CURRENT_EVENT_DELETE.send(source);
            case NOT_FOUND -> ChatMessages.ERROR_EVENT_NOT_FOUND.send(source, id);
        }
    }
}
