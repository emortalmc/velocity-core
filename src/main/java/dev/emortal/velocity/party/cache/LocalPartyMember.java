package dev.emortal.velocity.party.cache;

import dev.emortal.api.model.party.PartyMember;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface LocalPartyMember {

    @NotNull UUID id();

    @NotNull String username();

    default @NotNull PartyMember toProto() {
        return PartyMember.newBuilder()
                .setId(this.id().toString())
                .setUsername(this.username())
                .build();
    }
}
