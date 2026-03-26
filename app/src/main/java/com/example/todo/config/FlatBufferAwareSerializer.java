package com.example.todo.config;

import com.example.todo.generated.model.ContentTypeContext;
import com.example.todo.generated.model.ContentTypeSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Wraps ContentTypeSerializer to capture raw FlatBuffer bytes in a ThreadLocal
 * before they get corrupted by RawJsonBody's byte[] -> String UTF-8 conversion.
 */
public class FlatBufferAwareSerializer implements Wirespec.Serialization {

    private final ContentTypeSerializer delegate;

    public FlatBufferAwareSerializer(ObjectMapper objectMapper) {
        this.delegate = new ContentTypeSerializer(objectMapper);
    }

    @Override
    public <T> byte[] serializeBody(T body, Type type) {
        byte[] bytes = delegate.serializeBody(body, type);
        String accept = ContentTypeContext.getAcceptContentType();
        if ("application/flatbuffers".equals(accept) && bytes != null && bytes.length > 0) {
            FlatBufferBytesHolder.set(bytes);
        }
        return bytes;
    }

    @Override
    public <T> T deserializeBody(byte[] bytes, Type type) {
        return delegate.deserializeBody(bytes, type);
    }

    @Override
    public <T> String serializePath(T value, Type type) {
        return delegate.serializePath(value, type);
    }

    @Override
    public <T> T deserializePath(String value, Type type) {
        return delegate.deserializePath(value, type);
    }

    @Override
    public <T> List<String> serializeParam(T value, Type type) {
        return delegate.serializeParam(value, type);
    }

    @Override
    public <T> T deserializeParam(List<String> values, Type type) {
        return delegate.deserializeParam(values, type);
    }
}
