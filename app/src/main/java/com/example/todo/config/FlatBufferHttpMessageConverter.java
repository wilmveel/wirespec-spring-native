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
        return "application/flatbuffers".equals(
                com.example.todo.generated.model.ContentTypeContext.getAcceptContentType());
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return new RawJsonBody(inputMessage.getBody().readAllBytes());
    }

    @Override
    protected void writeInternal(Object body, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            byte[] rawBytes = FlatBufferBytesHolder.getAndClear();
            if (rawBytes != null) {
                outputMessage.getBody().write(rawBytes);
                outputMessage.getBody().flush();
                return;
            }
            if (body instanceof RawJsonBody rawJsonBody) {
                byte[] bytes = rawJsonBody.getJson().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                outputMessage.getBody().write(bytes);
                outputMessage.getBody().flush();
            }
        } finally {
            FlatBufferBytesHolder.clear();
        }
    }
}
