package io.dongvelop.springbootsse;

public final class SSEUtils {

    private SSEUtils() {
    }

    public static String generateSSEKey(final Long userId) {
        return userId + "_" + System.currentTimeMillis();
    }
}
