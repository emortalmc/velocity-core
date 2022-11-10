package cc.towerdefence.velocity.utils;

import java.time.Duration;

public class GrpcDurationConverter {

    public static Duration reverse(com.google.protobuf.Duration duration) {
        return Duration.ofSeconds(duration.getSeconds(), duration.getNanos());
    }
}
