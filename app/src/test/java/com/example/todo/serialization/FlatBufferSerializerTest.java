package com.example.todo.serialization;

import com.example.todo.generated.flatbuffers.FlatBufferSerializer;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import com.example.todo.generated.model.Error;
import com.example.todo.generated.record.TodoRecord;
import com.example.todo.generated.record.TodoInputRecord;
import com.example.todo.generated.record.ErrorRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlatBufferSerializerTest {

    @Test
    void serializeAndDeserializeTodo() {
        Todo original = new TodoRecord(1L, "Buy milk", true);
        byte[] bytes = FlatBufferSerializer.serializeTodo(original);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        Todo deserialized = FlatBufferSerializer.deserializeTodo(bytes);
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.title(), deserialized.title());
        assertEquals(original.completed(), deserialized.completed());
        assertInstanceOf(TodoRecord.class, deserialized);
    }

    @Test
    void serializeAndDeserializeTodoInput() {
        TodoInput original = new TodoInputRecord("Exercise", false);
        byte[] bytes = FlatBufferSerializer.serializeTodoInput(original);
        assertNotNull(bytes);

        TodoInput deserialized = FlatBufferSerializer.deserializeTodoInput(bytes);
        assertEquals(original.title(), deserialized.title());
        assertEquals(original.completed(), deserialized.completed());
        assertInstanceOf(TodoInputRecord.class, deserialized);
    }

    @Test
    void serializeAndDeserializeError() {
        Error original = new ErrorRecord("Not found");
        byte[] bytes = FlatBufferSerializer.serializeError(original);
        assertNotNull(bytes);

        Error deserialized = FlatBufferSerializer.deserializeError(bytes);
        assertEquals(original.message(), deserialized.message());
        assertInstanceOf(ErrorRecord.class, deserialized);
    }

    @Test
    void serializeAndDeserializeTodoList() {
        List<Todo> original = List.of(
            new TodoRecord(1L, "First", false),
            new TodoRecord(2L, "Second", true),
            new TodoRecord(3L, "Third", false)
        );

        byte[] bytes = FlatBufferSerializer.serializeTodoList(original);
        assertNotNull(bytes);

        List<Todo> deserialized = FlatBufferSerializer.deserializeTodoList(bytes);
        assertEquals(original.size(), deserialized.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i).id(), deserialized.get(i).id());
            assertEquals(original.get(i).title(), deserialized.get(i).title());
            assertEquals(original.get(i).completed(), deserialized.get(i).completed());
            assertInstanceOf(TodoRecord.class, deserialized.get(i));
        }
    }

    @Test
    void serializeEmptyTodoList() {
        List<Todo> original = List.of();
        byte[] bytes = FlatBufferSerializer.serializeTodoList(original);
        List<Todo> deserialized = FlatBufferSerializer.deserializeTodoList(bytes);
        assertEquals(0, deserialized.size());
    }
}
