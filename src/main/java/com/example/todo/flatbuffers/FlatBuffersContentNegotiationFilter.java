package com.example.todo.flatbuffers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FlatBuffersContentNegotiationFilter extends OncePerRequestFilter {

    private static final ThreadLocal<Boolean> FLATBUFFERS_REQUESTED = new ThreadLocal<>();

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String accept = request.getHeader("Accept");
            String contentType = request.getContentType();
            boolean isFlatBuffers = isFlatBuffersMediaType(accept) || isFlatBuffersMediaType(contentType);
            FLATBUFFERS_REQUESTED.set(isFlatBuffers);
            filterChain.doFilter(request, response);
        } finally {
            FLATBUFFERS_REQUESTED.remove();
        }
    }

    public static boolean isFlatBuffersRequest() {
        Boolean value = FLATBUFFERS_REQUESTED.get();
        return value != null && value;
    }

    private static boolean isFlatBuffersMediaType(String mediaType) {
        return mediaType != null && mediaType.contains(FlatBuffersMediaType.APPLICATION_FLATBUFFERS_VALUE);
    }
}
