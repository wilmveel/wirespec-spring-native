package com.example.todo.flatbuffers;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Converter that handles application/x-flatbuffers media type.
 * Claims to support writing any type (so Spring doesn't return 406 before
 * ResponseBodyAdvice transforms the body), but only actually writes RawFlatBuffersBody.
 */
public class FlatBuffersHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    public FlatBuffersHttpMessageConverter() {
        super(FlatBuffersMediaType.APPLICATION_FLATBUFFERS);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // Accept any class to prevent 406 during content negotiation.
        // FlatBuffersResponseBodyAdvice transforms the body to RawFlatBuffersBody
        // before this converter actually writes.
        return true;
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        // Return true for null (producible type probing) and for flatbuffers media type.
        // The converter is registered AFTER Jackson converters so JSON remains the default
        // for wildcard Accept headers.
        return mediaType == null || FlatBuffersMediaType.APPLICATION_FLATBUFFERS.isCompatibleWith(mediaType);
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return new RawFlatBuffersBody(inputMessage.getBody().readAllBytes());
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return new RawFlatBuffersBody(inputMessage.getBody().readAllBytes());
    }

    @Override
    protected void writeInternal(Object body, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if (body instanceof RawFlatBuffersBody fb) {
            outputMessage.getBody().write(fb.data());
        } else if (body instanceof byte[] bytes) {
            outputMessage.getBody().write(bytes);
        }
    }
}
