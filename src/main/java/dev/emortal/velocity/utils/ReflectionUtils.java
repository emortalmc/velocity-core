package dev.emortal.velocity.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public final class ReflectionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);

    public static <T> @Nullable T get(@NotNull Object instance, @NotNull Class<?> clazz, @NotNull String fieldName, @NotNull Class<T> resultType) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return resultType.cast(field.get(instance));
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            LOGGER.error("Failed to get field " + fieldName + " from " + clazz.getName(), exception);
            return null;
        }
    }

    public static @Nullable Object invoke(@NotNull Object instance, @NotNull Class<?> clazz, @NotNull String methodName, @Nullable Object... args) {
        try {
            return clazz.getDeclaredMethod(methodName).invoke(instance, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            LOGGER.error("Failed to call method " + methodName + " from " + clazz.getName(), exception);
        }
        return null;
    }
}
