package com.example.todo.config;

/**
 * ThreadLocal holder for raw FlatBuffer bytes.
 * Used to preserve binary data that would be corrupted by RawJsonBody's
 * byte[] -> String -> byte[] round-trip (invalid UTF-8 sequences get mangled).
 */
public final class FlatBufferBytesHolder {

    private static final ThreadLocal<byte[]> RAW_BYTES = new ThreadLocal<>();

    private FlatBufferBytesHolder() {}

    public static void set(byte[] bytes) {
        RAW_BYTES.set(bytes);
    }

    public static byte[] get() {
        return RAW_BYTES.get();
    }

    public static byte[] getAndClear() {
        byte[] bytes = RAW_BYTES.get();
        RAW_BYTES.remove();
        return bytes;
    }

    public static void clear() {
        RAW_BYTES.remove();
    }
}
