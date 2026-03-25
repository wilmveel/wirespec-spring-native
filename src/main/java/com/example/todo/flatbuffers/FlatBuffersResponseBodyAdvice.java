package com.example.todo.flatbuffers;

import community.flock.wirespec.java.Wirespec;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts Wirespec responses for FlatBuffers content type.
 * Runs before the default WirespecResponseBodyAdvice (higher precedence).
 * When FlatBuffers is requested, serializes the response and wraps it in RawFlatBuffersBody
 * instead of RawJsonBody, preventing binary data corruption.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlatBuffersResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final Wirespec.Serialization wirespecSerialization;
    private final Map<Class<?>, Method> toResponseCache = new ConcurrentHashMap<>();

    public FlatBuffersResponseBodyAdvice(Wirespec.Serialization wirespecSerialization) {
        this.wirespecSerialization = wirespecSerialization;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return FlatBuffersContentNegotiationFilter.isFlatBuffersRequest()
                && Wirespec.Response.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class selectedConverterType, ServerHttpRequest request,
                                   ServerHttpResponse response) {
        if (!(body instanceof Wirespec.Response<?> wirespecResponse)) {
            return body;
        }

        try {
            Class<?> endpointClass = returnType.getParameterType().getDeclaringClass();
            Method toResponse = toResponseCache.computeIfAbsent(endpointClass, this::findToResponseMethod);

            Wirespec.RawResponse rawResponse = (Wirespec.RawResponse) toResponse.invoke(
                    null, wirespecSerialization, wirespecResponse);

            response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode()));
            rawResponse.headers().forEach((name, values) ->
                    response.getHeaders().addAll(name, values));
            response.getHeaders().setContentType(FlatBuffersMediaType.APPLICATION_FLATBUFFERS);

            return rawResponse.body()
                    .map(RawFlatBuffersBody::new)
                    .orElse(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method findToResponseMethod(Class<?> endpointClass) {
        Class<?> handlerClass = Arrays.stream(endpointClass.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Handler"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Handler class in " + endpointClass));

        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getName().equals("toResponse") && java.lang.reflect.Modifier.isStatic(m.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No toResponse method in " + handlerClass));
    }
}
