package com.example.todo.config;

import community.flock.wirespec.integration.spring.shared.RawJsonBody;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FlatBufferHttpMessageConverter extends AbstractHttpMessageConverter<RawJsonBody> {

    public static final MediaType APPLICATION_FLATBUFFERS = MediaType.parseMediaType("application/flatbuffers");

    public FlatBufferHttpMessageConverter() {
        super(APPLICATION_FLATBUFFERS);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RawJsonBody.class.isAssignableFrom(clazz);
    }

    @Override
    protected RawJsonBody readInternal(Class<? extends RawJsonBody> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        byte[] bytes = inputMessage.getBody().readAllBytes();
        return new RawJsonBody(bytes);
    }

    @Override
    protected void writeInternal(RawJsonBody rawJsonBody, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        // RawJsonBody stores bytes as a UTF-8 String via getJson().
        // Convert back to bytes using the same charset to recover the original bytes.
        byte[] bytes = rawJsonBody.getJson().getBytes(StandardCharsets.UTF_8);
        outputMessage.getBody().write(bytes);
        outputMessage.getBody().flush();
    }
}
