package cc.towerdefence.velocity.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class ReflectionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionUtils.class);

    public static <T> T get(Object instance, Class<?> clazz, String fieldName, Class<T> resultType) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return resultType.cast(field.get(instance));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            LOGGER.error("Failed to get field " + fieldName + " from " + clazz.getName(), ex);
            return null;
        }
    }

    public static Object invoke(Object instance, Class<?> clazz, String methodName, Object... args) {
        try {
            return clazz.getDeclaredMethod(methodName).invoke(instance, args);
        } catch (Exception ex) {
            LOGGER.error("Failed to call method " + methodName + " from " + clazz.getName(), ex);
        }
        return null;
    }
}
