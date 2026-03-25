package com.example.todo.flatbuffers;

import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.serde.DefaultParamSerialization;
import community.flock.wirespec.java.serde.DefaultPathSerialization;

import java.lang.reflect.Type;
import java.util.List;

public class ContentNegotiatingWirespecSerialization implements Wirespec.Serialization,
        DefaultParamSerialization, DefaultPathSerialization {

    private final Wirespec.Serialization jsonSerialization;
    private final Wirespec.Serialization flatBuffersSerialization;

    public ContentNegotiatingWirespecSerialization(Wirespec.Serialization jsonSerialization,
                                                   Wirespec.Serialization flatBuffersSerialization) {
        this.jsonSerialization = jsonSerialization;
        this.flatBuffersSerialization = flatBuffersSerialization;
    }

    @Override
    public <T> byte[] serializeBody(T body, Type type) {
        return getDelegate().serializeBody(body, type);
    }

    @Override
    public <T> T deserializeBody(byte[] data, Type type) {
        return getDelegate().deserializeBody(data, type);
    }

    private Wirespec.Serialization getDelegate() {
        return FlatBuffersContentNegotiationFilter.isFlatBuffersRequest()
                ? flatBuffersSerialization
                : jsonSerialization;
    }
}
