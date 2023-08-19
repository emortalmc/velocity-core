package dev.emortal.testing.party;

import com.google.common.collect.Collections2;
import dev.emortal.api.model.party.Party;
import dev.emortal.api.model.party.PartyMember;
import dev.emortal.velocity.party.cache.LocalParty;
import dev.emortal.velocity.party.cache.LocalPartyMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

public record ForwardingLocalParty(@NotNull Party party) implements LocalParty {

    @Override
    public @NotNull String id() {
        return this.party.getId();
    }

    @Override
    public @NotNull UUID leaderId() {
        return UUID.fromString(this.party.getLeaderId());
    }

    @Override
    public boolean open() {
        return this.party.getOpen();
    }

    @Override
    public int size() {
        return this.party.getMembersCount();
    }

    @Override
    public @NotNull Collection<UUID> memberIds() {
        return Collections2.transform(this.party.getMembersList(), member -> UUID.fromString(member.getId()));
    }

    @Override
    public @NotNull Collection<? extends LocalPartyMember> members() {
        return Collections2.transform(this.party.getMembersList(), ForwardingLocalPartyMember::new);
    }

    @Override
    public @Nullable LocalPartyMember getMember(@NotNull UUID memberId) {
        for (PartyMember member : this.party.getMembersList()) {
            if (member.getId().equals(memberId.toString())) return new ForwardingLocalPartyMember(member);
        }
        return null;
    }
}
