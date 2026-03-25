package com.example.todo.flatbuffers;

import com.example.todo.flatbuffers.codec.TodoFlatBuffersCodec;
import com.example.todo.flatbuffers.codec.TodoInputFlatBuffersCodec;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlatBuffersIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private final FlatBuffersSerialization serialization = new FlatBuffersSerialization();
    private static final MediaType FLATBUFFERS = FlatBuffersMediaType.APPLICATION_FLATBUFFERS;

    @Test
    void createTodo_withFlatBuffersContentType() {
        var input = new TodoInput("FlatBuffers task", false);
        byte[] body = new TodoInputFlatBuffersCodec().serialize(input);

        webTestClient.post().uri("/api/todos")
                .contentType(FLATBUFFERS)
                .accept(FLATBUFFERS)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(FLATBUFFERS)
                .expectBody(byte[].class)
                .value(bytes -> {
                    Todo result = new TodoFlatBuffersCodec().deserialize(bytes);
                    assertEquals("FlatBuffers task", result.title());
                    assertFalse(result.completed());
                    assertNotNull(result.id());
                });
    }

    @Test
    void getTodos_withFlatBuffersAcceptHeader() {
        // First create a todo via JSON so there's data
        webTestClient.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"title": "For FlatBuffers list test", "completed": false}
                        """)
                .exchange()
                .expectStatus().isOk();

        // Then retrieve via FlatBuffers
        webTestClient.get().uri("/api/todos")
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(FLATBUFFERS)
                .expectBody(byte[].class)
                .value(bytes -> {
                    var type = Wirespec.getType(Todo.class, List.class);
                    List<Todo> todos = serialization.deserializeBody(bytes, type);
                    assertFalse(todos.isEmpty());
                });
    }

    @Test
    void getTodos_withJsonAcceptHeader_stillWorks() {
        webTestClient.get().uri("/api/todos")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void getTodoById_notFound_returnsFlatBuffersError() {
        webTestClient.get().uri("/api/todos/{id}", 99999)
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(FLATBUFFERS)
                .expectBody(byte[].class)
                .value(bytes -> {
                    var error = new com.example.todo.flatbuffers.codec.ErrorFlatBuffersCodec().deserialize(bytes);
                    assertEquals("Todo not found", error.message());
                });
    }
}
