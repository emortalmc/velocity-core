package dev.emortal.testing.party;

import dev.emortal.api.model.party.PartyMember;
import dev.emortal.velocity.party.cache.LocalPartyMember;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ForwardingLocalPartyMember(@NotNull PartyMember member) implements LocalPartyMember {

    @Override
    public @NotNull UUID id() {
        return UUID.fromString(this.member.getId());
    }

    @Override
    public @NotNull String username() {
        return this.member.getUsername();
    }
}
