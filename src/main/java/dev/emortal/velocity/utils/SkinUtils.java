package dev.emortal.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import dev.emortal.api.model.common.PlayerSkin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SkinUtils {

    public static @Nullable PlayerSkin getProtoSkin(@NotNull Player player) {
        List<GameProfile.Property> properties = player.getGameProfileProperties();

        for (GameProfile.Property property : properties) {
            if (!property.getName().equals("textures")) continue;

            return PlayerSkin.newBuilder()
                    .setTexture(property.getValue())
                    .setSignature(property.getSignature())
                    .build();
        }

        return null;
    }
}
