package dev.emortal.velocity.party.commands.event.subs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.emortal.api.model.common.PlayerSkin;
import dev.emortal.api.model.party.EventData;
import dev.emortal.api.service.party.PartyService;
import dev.emortal.velocity.command.ArgumentProvider;
import dev.emortal.velocity.command.EmortalCommandExecutor;
import dev.emortal.velocity.lang.ChatMessages;
import dev.emortal.velocity.utils.SkinUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CreateSub implements EmortalCommandExecutor {
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CreateSub.class);

    private final @NotNull PartyService partyService;

    public CreateSub(@NotNull PartyService partyService) {
        this.partyService = partyService;
    }

    @Override
    public void execute(@NotNull CommandSource source, @NotNull ArgumentProvider arguments) {
        if (!(source instanceof Player player)) return;

        String id = arguments.getArgument("id", String.class);
        if (id.length() < 3 || id.length() > 32) {
            ChatMessages.ERROR_EVENT_INVALID_ID.send(source);
            return;
        }

        PlayerSkin skin = SkinUtils.getProtoSkin(player);
        if (skin == null) {
            ChatMessages.ERROR_NO_SKIN.send(player);
            return;
        }

        String showTimeStr = arguments.getArgument("showTime", String.class);
        Instant showTime;
        try {
            showTime = this.parseStrTime(showTimeStr);
        } catch (DateTimeParseException unused) {
            ChatMessages.ERROR_INVALID_TIME_FORMAT_ARG.send(player, "showTime");
            return;
        }

        String startTimeStr = arguments.getArgument("startTime", String.class);
        Instant startTime;
        try {
            startTime = this.parseStrTime(startTimeStr);
        } catch (DateTimeParseException unused) {
            ChatMessages.ERROR_INVALID_TIME_FORMAT_ARG.send(player, "startTime");
            return;
        }

        Instant timeNow = Instant.now().minusSeconds(60);
        if (timeNow.isAfter(startTime)) {
            ChatMessages.ERROR_TIME_IN_PAST_ARG.send(player, "startTime");
            return;
        }

        EventData createdEvent;
        try {
            createdEvent = this.partyService.createEvent(id, player.getUniqueId(), player.getUsername(), skin, showTime, startTime);
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                ChatMessages.ERROR_EVENT_ALREADY_EXISTS.send(player, id);
                return;
            }

            ChatMessages.GENERIC_ERROR.send(player);
            LOGGER.error("Failed to create event", ex);
            return;
        }

        ChatMessages.EVENT_CREATED.send(player, createdEvent);
    }

    private Instant parseStrTime(@Nullable String input) {
        if (input == null) return null;
        if (input.equals("now")) return Instant.now();

        return LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.of("Europe/London"))
                .toInstant();
    }
}
