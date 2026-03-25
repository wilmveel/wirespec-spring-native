package com.example.todo.flatbuffers;

import org.springframework.http.MediaType;

public final class FlatBuffersMediaType {

    public static final String APPLICATION_FLATBUFFERS_VALUE = "application/x-flatbuffers";
    public static final MediaType APPLICATION_FLATBUFFERS = MediaType.valueOf(APPLICATION_FLATBUFFERS_VALUE);

    private FlatBuffersMediaType() {}
}
