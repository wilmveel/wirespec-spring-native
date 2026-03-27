package com.example.todo.serialization;

import com.example.todo.generated.flatbuffers.FlatBufferSerializer;
import com.example.todo.generated.model.ContentTypeContext;
import com.example.todo.generated.model.ContentTypeSerializer;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.WirespecJacksonModule;
import com.example.todo.generated.record.TodoRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeSerializerTest {

    private ContentTypeSerializer serializer;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new WirespecJacksonModule());
        serializer = new ContentTypeSerializer(mapper);
    }

    @AfterEach
    void cleanup() {
        ContentTypeContext.clear();
    }

    @Test
    void serializeAsJsonByDefault() {
        Todo todo = new TodoRecord(1L, "Test", false);
        byte[] bytes = serializer.serializeBody(todo, Todo.class);
        String json = new String(bytes);
        assertTrue(json.contains("\"title\""));
        assertTrue(json.contains("\"Test\""));
    }

    @Test
    void serializeAsFlatBufferWhenAcceptHeader() {
        ContentTypeContext.setAcceptContentType("application/flatbuffers");
        Todo todo = new TodoRecord(1L, "Test", false);
        byte[] bytes = serializer.serializeBody(todo, Todo.class);

        Todo deserialized = FlatBufferSerializer.deserializeTodo(bytes);
        assertEquals(todo.id(), deserialized.id());
        assertEquals(todo.title(), deserialized.title());
    }

    @Test
    void deserializeFromJsonByDefault() {
        String json = "{\"id\":1,\"title\":\"Test\",\"completed\":false}";
        Todo todo = serializer.deserializeBody(json.getBytes(), Todo.class);
        assertEquals(1L, todo.id());
        assertEquals("Test", todo.title());
    }

    @Test
    void deserializeFromFlatBufferWhenContentType() {
        ContentTypeContext.setRequestContentType("application/flatbuffers");
        Todo original = new TodoRecord(1L, "Test", false);
        byte[] fbBytes = FlatBufferSerializer.serializeTodo(original);

        Todo deserialized = serializer.deserializeBody(fbBytes, Todo.class);
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.title(), deserialized.title());
    }

    @Test
    void serializeNullBodyReturnsEmpty() {
        byte[] bytes = serializer.serializeBody(null, Todo.class);
        assertEquals(0, bytes.length);
    }

    @Test
    void deserializeEmptyBodyReturnsNull() {
        Todo result = serializer.deserializeBody(new byte[0], Todo.class);
        assertNull(result);
    }

    @Test
    void serializeListAsFlatBuffer() {
        ContentTypeContext.setAcceptContentType("application/flatbuffers");
        List<Todo> todos = List.of(
            new TodoRecord(1L, "A", false),
            new TodoRecord(2L, "B", true)
        );
        byte[] bytes = serializer.serializeBody(todos, Wirespec.getType(Todo.class, List.class));

        List<Todo> deserialized = FlatBufferSerializer.deserializeTodoList(bytes);
        assertEquals(2, deserialized.size());
        assertEquals("A", deserialized.get(0).title());
    }
}
