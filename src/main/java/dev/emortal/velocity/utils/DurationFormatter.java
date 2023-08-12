package dev.emortal.velocity.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.StringJoiner;

public final class DurationFormatter {

    public static @NotNull String formatBigToSmall(@NotNull Duration duration) {
        StringJoiner joiner = new StringJoiner(", ");
        int unitCount = 0;

        int years = (int) duration.toDays() / 365;
        int months = (int) duration.toDays() / 30;
        int days = (int) duration.toDays() % 30;
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();

        if (years > 0) {
            joiner.add(years + " year" + (years == 1 ? "" : "s"));
            unitCount++;
        }

        if (months > 0) {
            joiner.add(months + " month" + (months == 1 ? "" : "s"));
            unitCount++;
        }

        if (days > 0) {
            joiner.add(days + " day" + (days == 1 ? "" : "s"));
            if (++unitCount == 3) return joiner.toString();
        }

        if (hours > 0) {
            joiner.add(hours + " hour" + (hours == 1 ? "" : "s"));
            if (++unitCount == 3) return joiner.toString();
        }

        if (minutes > 0) {
            joiner.add(minutes + " minute" + (minutes == 1 ? "" : "s"));
            if (++unitCount == 3) return joiner.toString();
        }

        if (seconds > 0) {
            joiner.add(seconds + " second" + (seconds == 1 ? "" : "s"));
        }

        return joiner.toString();
    }

    public static @NotNull String formatShort(@NotNull Duration duration) {
        int days = (int) duration.toDays();

        if (days > 365) return "%sy ago".formatted(days / 365);
        if (days > 30) return "%smo ago".formatted(days / 30);
        if (days > 0) return "%sd ago".formatted(days);
        if (duration.toHours() > 0) return "%sh ago".formatted(duration.toHours());
        if (duration.toMinutes() > 0) return "%smin ago".formatted(duration.toMinutes());
        if (duration.getSeconds() > 0) return "%ss ago".formatted(duration.getSeconds());

        return "now";
    }

    public static @NotNull String formatShortFromInstant(@NotNull Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        return formatShort(duration);
    }
}
