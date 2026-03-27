package com.example.todo.controller;

import com.example.todo.generated.flatbuffers.FlatBufferSerializer;
import com.example.todo.generated.model.Error;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import com.example.todo.generated.record.TodoInputRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoControllerFlatBufferTest {

    private static final MediaType FLATBUFFERS = MediaType.parseMediaType("application/flatbuffers");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void getTodos_asFlatBuffer_returnsList() {
        byte[] responseBytes = webTestClient.get().uri("/api/todos")
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        List<Todo> todos = FlatBufferSerializer.deserializeTodoList(responseBytes);
        assertFalse(todos.isEmpty());
        assertNotNull(todos.get(0).title());
    }

    @Test
    void createTodo_withFlatBufferBody() {
        TodoInput input = new TodoInputRecord("FlatBuffer task", false);
        byte[] requestBytes = FlatBufferSerializer.serializeTodoInput(input);

        byte[] responseBytes = webTestClient.post().uri("/api/todos")
                .contentType(FLATBUFFERS)
                .accept(FLATBUFFERS)
                .bodyValue(requestBytes)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        Todo created = FlatBufferSerializer.deserializeTodo(responseBytes);
        assertEquals("FlatBuffer task", created.title());
        assertEquals(false, created.completed());
        assertNotNull(created.id());
    }

    @Test
    void getTodoById_asFlatBuffer() {
        Todo created = webTestClient.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\": \"FB get test\", \"completed\": false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Todo.class)
                .returnResult()
                .getResponseBody();

        byte[] responseBytes = webTestClient.get().uri("/api/todos/{id}", created.id())
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        Todo todo = FlatBufferSerializer.deserializeTodo(responseBytes);
        assertEquals("FB get test", todo.title());
    }

    @Test
    void updateTodo_withFlatBuffer() {
        Todo created = webTestClient.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\": \"FB update test\", \"completed\": false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Todo.class)
                .returnResult()
                .getResponseBody();

        TodoInput update = new TodoInputRecord("Updated via FB", true);
        byte[] requestBytes = FlatBufferSerializer.serializeTodoInput(update);

        byte[] responseBytes = webTestClient.put().uri("/api/todos/{id}", created.id())
                .contentType(FLATBUFFERS)
                .accept(FLATBUFFERS)
                .bodyValue(requestBytes)
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        Todo updated = FlatBufferSerializer.deserializeTodo(responseBytes);
        assertEquals("Updated via FB", updated.title());
        assertEquals(true, updated.completed());
    }

    @Test
    void getTodoById_notFound_asFlatBuffer() {
        byte[] responseBytes = webTestClient.get().uri("/api/todos/{id}", 99999)
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        Error error = FlatBufferSerializer.deserializeError(responseBytes);
        assertEquals("Todo not found", error.message());
    }

    @Test
    void deleteTodo_asFlatBuffer_returnsNoContent() {
        Todo created = webTestClient.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\": \"FB delete test\", \"completed\": false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Todo.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete().uri("/api/todos/{id}", created.id())
                .accept(FLATBUFFERS)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void mixedContentTypes_jsonRequestFlatBufferResponse() {
        byte[] responseBytes = webTestClient.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(FLATBUFFERS)
                .bodyValue("{\"title\": \"Mixed test\", \"completed\": true}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();

        Todo created = FlatBufferSerializer.deserializeTodo(responseBytes);
        assertEquals("Mixed test", created.title());
        assertEquals(true, created.completed());
    }

    @Test
    void existingJsonBehaviorPreserved() {
        webTestClient.get().uri("/api/todos")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].title").isNotEmpty();
    }
}
