package com.example.todo.flatbuffers.codec;

import com.example.todo.flatbuffers.FlatBufferReader;
import com.example.todo.flatbuffers.FlatBuffersCodec;
import com.example.todo.generated.model.TodoInput;
import com.google.flatbuffers.FlatBufferBuilder;

/**
 * FlatBuffers codec for TodoInput.
 * Field layout (from todo.ws): 0=title (String), 1=completed (Boolean)
 */
public final class TodoInputFlatBuffersCodec implements FlatBuffersCodec<TodoInput> {

    private static final int FIELD_TITLE = 0;
    private static final int FIELD_COMPLETED = 1;
    private static final int NUM_FIELDS = 2;

    @Override
    public byte[] serialize(TodoInput value) {
        FlatBufferBuilder builder = new FlatBufferBuilder(128);
        int offset = serializeToBuilder(builder, value);
        builder.finish(offset);
        return builder.sizedByteArray();
    }

    @Override
    public TodoInput deserialize(byte[] data) {
        FlatBufferReader reader = FlatBufferReader.fromBytes(data);
        return readFromReader(reader);
    }

    @Override
    public int serializeToBuilder(FlatBufferBuilder builder, TodoInput value) {
        int titleOffset = value.title() != null ? builder.createString(value.title()) : 0;

        builder.startTable(NUM_FIELDS);
        if (titleOffset != 0) builder.addOffset(FIELD_TITLE, titleOffset, 0);
        builder.addBoolean(FIELD_COMPLETED, value.completed() != null && value.completed(), false);
        return builder.endTable();
    }

    public static TodoInput readFromReader(FlatBufferReader reader) {
        String title = reader.readString(FIELD_TITLE);
        boolean completed = reader.readBoolean(FIELD_COMPLETED, false);
        return new TodoInput(title, completed);
    }
}
