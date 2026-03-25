package com.example.todo.flatbuffers;

import com.example.todo.flatbuffers.codec.ErrorFlatBuffersCodec;
import com.example.todo.flatbuffers.codec.TodoFlatBuffersCodec;
import com.example.todo.flatbuffers.codec.TodoInputFlatBuffersCodec;
import com.example.todo.generated.model.Error;
import com.example.todo.generated.model.Todo;
import com.example.todo.generated.model.TodoInput;
import com.google.flatbuffers.FlatBufferBuilder;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.serde.DefaultParamSerialization;
import community.flock.wirespec.java.serde.DefaultPathSerialization;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlatBuffersSerialization implements Wirespec.Serialization,
        DefaultParamSerialization, DefaultPathSerialization {

    private final Map<Class<?>, FlatBuffersCodec<?>> codecs = new HashMap<>();

    public FlatBuffersSerialization() {
        codecs.put(Todo.class, new TodoFlatBuffersCodec());
        codecs.put(TodoInput.class, new TodoInputFlatBuffersCodec());
        codecs.put(Error.class, new ErrorFlatBuffersCodec());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> byte[] serializeBody(T body, Type type) {
        if (body == null) return null;

        Class<?> rawType = getRawType(type);

        if (rawType == List.class) {
            Class<?> elementType = getListElementType(type);
            return serializeList((List<?>) body, elementType);
        }

        FlatBuffersCodec<T> codec = (FlatBuffersCodec<T>) codecs.get(rawType);
        if (codec == null) {
            throw new IllegalArgumentException("No FlatBuffers codec registered for: " + rawType.getName());
        }
        return codec.serialize(body);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeBody(byte[] data, Type type) {
        if (data == null || data.length == 0) return null;

        Class<?> rawType = getRawType(type);

        if (rawType == List.class) {
            Class<?> elementType = getListElementType(type);
            return (T) deserializeList(data, elementType);
        }

        FlatBuffersCodec<T> codec = (FlatBuffersCodec<T>) codecs.get(rawType);
        if (codec == null) {
            throw new IllegalArgumentException("No FlatBuffers codec registered for: " + rawType.getName());
        }
        return codec.deserialize(data);
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeList(List<?> list, Class<?> elementType) {
        FlatBuffersCodec<Object> codec = (FlatBuffersCodec<Object>) codecs.get(elementType);
        if (codec == null) {
            throw new IllegalArgumentException("No FlatBuffers codec registered for: " + elementType.getName());
        }

        FlatBufferBuilder builder = new FlatBufferBuilder(256 * list.size());

        // Serialize each element to get offsets (must be done before startVector)
        int[] offsets = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            offsets[i] = codec.serializeToBuilder(builder, list.get(i));
        }

        // Create vector of table offsets
        builder.startVector(4, offsets.length, 4);
        for (int i = offsets.length - 1; i >= 0; i--) {
            builder.addOffset(offsets[i]);
        }
        int vectorOffset = builder.endVector();

        // Wrap in a root table with the vector as field 0
        builder.startTable(1);
        builder.addOffset(0, vectorOffset, 0);
        int rootOffset = builder.endTable();
        builder.finish(rootOffset);

        return builder.sizedByteArray();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> deserializeList(byte[] data, Class<?> elementType) {
        FlatBufferReader reader = FlatBufferReader.fromBytes(data);
        int length = reader.vectorLength(0);
        List<T> result = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            FlatBufferReader elementReader = reader.vectorElement(0, i);
            if (elementType == Todo.class) {
                result.add((T) TodoFlatBuffersCodec.readFromReader(elementReader));
            } else if (elementType == TodoInput.class) {
                result.add((T) TodoInputFlatBuffersCodec.readFromReader(elementReader));
            } else if (elementType == Error.class) {
                result.add((T) ErrorFlatBuffersCodec.readFromReader(elementReader));
            } else {
                throw new IllegalArgumentException("No FlatBuffers codec for list element: " + elementType.getName());
            }
        }

        return result;
    }

    private Class<?> getRawType(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private Class<?> getListElementType(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> c) return c;
        }
        throw new IllegalArgumentException("Cannot determine list element type from: " + type);
    }
}
