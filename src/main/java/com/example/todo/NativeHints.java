package com.example.todo;

import com.example.todo.flatbuffers.*;
import com.example.todo.flatbuffers.codec.*;
import com.example.todo.generated.endpoint.*;
import com.example.todo.generated.model.Error;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import community.flock.wirespec.integration.spring.shared.RawJsonBody;
import community.flock.wirespec.java.Wirespec;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeHints.WirespecHints.class)
public class NativeHints {

	static class WirespecHints implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			var allMembers = MemberCategory.values();

			// Wirespec library classes (from dependency JARs, can't be auto-discovered)
			hints.reflection().registerType(RawJsonBody.class, allMembers);
			hints.reflection().registerType(Wirespec.RawRequest.class, allMembers);
			hints.reflection().registerType(Wirespec.RawResponse.class, allMembers);

			// Kotlin module metadata (needed for Kotlin reflection in native image)
			hints.resources().registerPattern("META-INF/*.kotlin_module");

			// Generated wirespec model classes
			registerWithInnerClasses(hints, Todo.class, allMembers);
			registerWithInnerClasses(hints, TodoInput.class, allMembers);
			registerWithInnerClasses(hints, Error.class, allMembers);

			// FlatBuffers integration classes
			hints.reflection().registerType(RawFlatBuffersBody.class, allMembers);
			hints.reflection().registerType(FlatBuffersSerialization.class, allMembers);
			hints.reflection().registerType(ContentNegotiatingWirespecSerialization.class, allMembers);
			hints.reflection().registerType(FlatBuffersResponseBodyAdvice.class, allMembers);
			hints.reflection().registerType(FlatBuffersHttpMessageConverter.class, allMembers);
			hints.reflection().registerType(TodoFlatBuffersCodec.class, allMembers);
			hints.reflection().registerType(TodoInputFlatBuffersCodec.class, allMembers);
			hints.reflection().registerType(ErrorFlatBuffersCodec.class, allMembers);

			// Generated wirespec endpoint classes
			registerWithInnerClasses(hints, GetTodos.class, allMembers);
			registerWithInnerClasses(hints, GetTodoById.class, allMembers);
			registerWithInnerClasses(hints, CreateTodo.class, allMembers);
			registerWithInnerClasses(hints, UpdateTodo.class, allMembers);
			registerWithInnerClasses(hints, DeleteTodo.class, allMembers);
		}

		private static void registerWithInnerClasses(RuntimeHints hints, Class<?> clazz, MemberCategory[] categories) {
			hints.reflection().registerType(clazz, categories);
			for (Class<?> inner : clazz.getDeclaredClasses()) {
				registerWithInnerClasses(hints, inner, categories);
			}
		}
	}
}
