package com.example.todo.flatbuffers.codec;

import com.example.todo.generated.model.Error;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TodoFlatBuffersCodecTest {

    @Test
    void roundTripTodo() {
        var codec = new TodoFlatBuffersCodec();
        var original = new Todo(42L, "Buy groceries", false);

        byte[] bytes = codec.serialize(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        Todo deserialized = codec.deserialize(bytes);
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.title(), deserialized.title());
        assertEquals(original.completed(), deserialized.completed());
    }

    @Test
    void roundTripTodoWithCompleted() {
        var codec = new TodoFlatBuffersCodec();
        var original = new Todo(1L, "Exercise", true);

        Todo deserialized = codec.deserialize(codec.serialize(original));
        assertEquals(1L, deserialized.id());
        assertEquals("Exercise", deserialized.title());
        assertTrue(deserialized.completed());
    }

    @Test
    void roundTripTodoInput() {
        var codec = new TodoInputFlatBuffersCodec();
        var original = new TodoInput("Learn FlatBuffers", false);

        TodoInput deserialized = codec.deserialize(codec.serialize(original));
        assertEquals(original.title(), deserialized.title());
        assertEquals(original.completed(), deserialized.completed());
    }

    @Test
    void roundTripError() {
        var codec = new ErrorFlatBuffersCodec();
        var original = new Error("Todo not found");

        Error deserialized = codec.deserialize(codec.serialize(original));
        assertEquals(original.message(), deserialized.message());
    }

    @Test
    void todoWithUnicodeTitle() {
        var codec = new TodoFlatBuffersCodec();
        var original = new Todo(1L, "Achetez des légumes 🥬", true);

        Todo deserialized = codec.deserialize(codec.serialize(original));
        assertEquals(original.title(), deserialized.title());
    }
}
