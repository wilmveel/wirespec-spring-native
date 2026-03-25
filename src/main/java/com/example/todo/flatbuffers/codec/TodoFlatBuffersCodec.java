package com.example.todo.flatbuffers.codec;

import com.example.todo.flatbuffers.FlatBufferReader;
import com.example.todo.flatbuffers.FlatBuffersCodec;
import com.example.todo.generated.model.Todo;
import com.google.flatbuffers.FlatBufferBuilder;

/**
 * FlatBuffers codec for Todo.
 * Field layout (from todo.ws): 0=id (Long), 1=title (String), 2=completed (Boolean)
 */
public final class TodoFlatBuffersCodec implements FlatBuffersCodec<Todo> {

    private static final int FIELD_ID = 0;
    private static final int FIELD_TITLE = 1;
    private static final int FIELD_COMPLETED = 2;
    private static final int NUM_FIELDS = 3;

    @Override
    public byte[] serialize(Todo value) {
        FlatBufferBuilder builder = new FlatBufferBuilder(256);
        int offset = serializeToBuilder(builder, value);
        builder.finish(offset);
        return builder.sizedByteArray();
    }

    @Override
    public Todo deserialize(byte[] data) {
        FlatBufferReader reader = FlatBufferReader.fromBytes(data);
        return readFromReader(reader);
    }

    @Override
    public int serializeToBuilder(FlatBufferBuilder builder, Todo value) {
        int titleOffset = value.title() != null ? builder.createString(value.title()) : 0;

        builder.startTable(NUM_FIELDS);
        builder.addLong(FIELD_ID, value.id() != null ? value.id() : 0L, 0L);
        if (titleOffset != 0) builder.addOffset(FIELD_TITLE, titleOffset, 0);
        builder.addBoolean(FIELD_COMPLETED, value.completed() != null && value.completed(), false);
        return builder.endTable();
    }

    public static Todo readFromReader(FlatBufferReader reader) {
        Long id = reader.readLong(FIELD_ID, 0L);
        String title = reader.readString(FIELD_TITLE);
        boolean completed = reader.readBoolean(FIELD_COMPLETED, false);
        return new Todo(id, title, completed);
    }
}
