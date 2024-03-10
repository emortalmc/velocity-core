package dev.emortal.velocity.party.commands.event.subs;

import com.velocitypowered.api.command.CommandSource;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;

public class ListSub implements EmortalCommandExecutor {
    private final @NotNull PartyService partyService;

    public ListSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        ChatMessages.EVENT_DATA_LIST.send(source, this.partyService.listEvents());
    }
}
