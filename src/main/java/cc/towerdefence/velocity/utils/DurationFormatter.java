package cc.towerdefence.velocity.utils;

import java.time.Duration;
import java.time.Instant;

public class DurationFormatter {

    public static String formatShort(Duration duration) {
        int days = (int) duration.toDays();

        if (days > 365)
            return "%sy ago".formatted(days / 365);
        if (days > 30)
            return "%smo ago".formatted(days / 30);
        if (days > 0)
            return "%sd ago".formatted(days);
        if (duration.toHours() > 0)
            return "%sh ago".formatted(duration.toHours());
        if (duration.toMinutes() > 0)
            return "%smin ago".formatted(duration.toMinutes());
        if (duration.getSeconds() > 0)
            return "%ss ago".formatted(duration.getSeconds());

        return "now";
    }

    public static String formatShortFromInstant(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        return formatShort(duration);
    }
}
