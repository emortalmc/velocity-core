package dev.emortal.velocity.party.commands.party.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.message.party.PartyBroadcastMessage;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.api.utils.kafka.FriendlyKafkaProducer;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import org.jetbrains.annotations.NotNull;

public class PartyBroadcastSub implements EmortalCommandExecutor {
    private final @NotNull FriendlyKafkaProducer kafkaProducer;
    private final @NotNull PartyService partyService;

    public PartyBroadcastSub(@NotNull FriendlyKafkaProducer kafkaProducer, @NotNull PartyService partyService) {
        this.kafkaProducer = kafkaProducer;
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        Player executor = (Player) source;

        Party party = this.partyService.getPartyByPlayer(executor.getUniqueId());

        if (party == null) return;

        if (!party.getOpen()) {
            ChatMessages.PARTY_BROADCAST_PARTY_CLOSED.send(executor);
            return;
        }

        this.kafkaProducer.produceAndForget( PartyBroadcastMessage.newBuilder().setParty(party).build());
    }
}
