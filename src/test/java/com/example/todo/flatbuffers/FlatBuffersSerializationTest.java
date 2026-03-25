package com.example.todo.flatbuffers;

import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlatBuffersSerializationTest {

    private final FlatBuffersSerialization serialization = new FlatBuffersSerialization();

    @Test
    void serializeAndDeserializeSingleTodo() {
        var todo = new Todo(1L, "Test", true);
        var type = Wirespec.getType(Todo.class, null);

        byte[] bytes = serialization.serializeBody(todo, type);
        Todo result = serialization.deserializeBody(bytes, type);

        assertEquals(todo.id(), result.id());
        assertEquals(todo.title(), result.title());
        assertEquals(todo.completed(), result.completed());
    }

    @Test
    void serializeAndDeserializeTodoList() {
        var todos = List.of(
                new Todo(1L, "First", false),
                new Todo(2L, "Second", true)
        );
        var type = Wirespec.getType(Todo.class, List.class);

        byte[] bytes = serialization.serializeBody(todos, type);
        List<Todo> result = serialization.deserializeBody(bytes, type);

        assertEquals(2, result.size());
        assertEquals("First", result.get(0).title());
        assertEquals("Second", result.get(1).title());
        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        assertFalse(result.get(0).completed());
        assertTrue(result.get(1).completed());
    }

    @Test
    void serializeAndDeserializeEmptyList() {
        var type = Wirespec.getType(Todo.class, List.class);

        byte[] bytes = serialization.serializeBody(List.of(), type);
        List<Todo> result = serialization.deserializeBody(bytes, type);

        assertEquals(0, result.size());
    }

    @Test
    void serializeAndDeserializeTodoInput() {
        var input = new TodoInput("Buy milk", false);
        var type = Wirespec.getType(TodoInput.class, null);

        byte[] bytes = serialization.serializeBody(input, type);
        TodoInput result = serialization.deserializeBody(bytes, type);

        assertEquals(input.title(), result.title());
        assertEquals(input.completed(), result.completed());
    }

    @Test
    void serializeNullReturnsNull() {
        assertNull(serialization.serializeBody(null, Wirespec.getType(Todo.class, null)));
    }

    @Test
    void deserializeNullReturnsNull() {
        assertNull(serialization.deserializeBody(null, Wirespec.getType(Todo.class, null)));
    }
}
