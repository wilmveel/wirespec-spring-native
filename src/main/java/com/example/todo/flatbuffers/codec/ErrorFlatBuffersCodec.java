package com.example.todo.flatbuffers.codec;

import com.example.todo.flatbuffers.FlatBufferReader;
import com.example.todo.flatbuffers.FlatBuffersCodec;
import com.example.todo.generated.model.Error;
import com.google.flatbuffers.FlatBufferBuilder;

/**
 * FlatBuffers codec for Error.
 * Field layout (from todo.ws): 0=message (String)
 */
public final class ErrorFlatBuffersCodec implements FlatBuffersCodec<Error> {

    private static final int FIELD_MESSAGE = 0;
    private static final int NUM_FIELDS = 1;

    @Override
    public byte[] serialize(Error value) {
        FlatBufferBuilder builder = new FlatBufferBuilder(128);
        int offset = serializeToBuilder(builder, value);
        builder.finish(offset);
        return builder.sizedByteArray();
    }

    @Override
    public Error deserialize(byte[] data) {
        FlatBufferReader reader = FlatBufferReader.fromBytes(data);
        return readFromReader(reader);
    }

    @Override
    public int serializeToBuilder(FlatBufferBuilder builder, Error value) {
        int messageOffset = value.message() != null ? builder.createString(value.message()) : 0;

        builder.startTable(NUM_FIELDS);
        if (messageOffset != 0) builder.addOffset(FIELD_MESSAGE, messageOffset, 0);
        return builder.endTable();
    }

    public static Error readFromReader(FlatBufferReader reader) {
        String message = reader.readString(FIELD_MESSAGE);
        return new Error(message);
    }
}
