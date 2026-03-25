package com.example.todo.flatbuffers;

import com.google.flatbuffers.FlatBufferBuilder;

public interface FlatBuffersCodec<T> {
    byte[] serialize(T value);
    T deserialize(byte[] data);
    int serializeToBuilder(FlatBufferBuilder builder, T value);
}
