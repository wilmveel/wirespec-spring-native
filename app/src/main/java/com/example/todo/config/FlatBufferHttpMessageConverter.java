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

@Component
public class FlatBufferHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    public static final MediaType APPLICATION_FLATBUFFERS = MediaType.parseMediaType("application/flatbuffers");

    public FlatBufferHttpMessageConverter() {
        super(APPLICATION_FLATBUFFERS);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // Support any class when the Accept content type is application/flatbuffers.
        // The WirespecResponseBodyAdvice will convert Wirespec Response objects
        // to RawJsonBody before writeInternal is called.
        return "application/flatbuffers".equals(
                com.example.todo.generated.model.ContentTypeContext.getAcceptContentType());
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        byte[] bytes = inputMessage.getBody().readAllBytes();
        return new RawJsonBody(bytes);
    }

    @Override
    protected void writeInternal(Object body, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        byte[] rawBytes = FlatBufferBytesHolder.getAndClear();
        if (rawBytes != null) {
            // Use the raw FlatBuffer bytes stored before RawJsonBody corruption
            outputMessage.getBody().write(rawBytes);
            outputMessage.getBody().flush();
            return;
        }
        // Fallback: if body is RawJsonBody, use its JSON string bytes
        if (body instanceof RawJsonBody rawJsonBody) {
            byte[] bytes = rawJsonBody.getJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            outputMessage.getBody().write(bytes);
            outputMessage.getBody().flush();
        }
    }
}
