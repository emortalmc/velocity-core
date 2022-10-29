package cc.towerdefence.velocity.utils;

import com.google.common.util.concurrent.FutureCallback;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FunctionalFutureCallback {

    public static <T> FutureCallback<T> create(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                onSuccess.accept(result);
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                onFailure.accept(throwable);
            }
        };
    }
}
