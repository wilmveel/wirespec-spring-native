package com.example.todo.filter;

import com.example.todo.generated.model.ContentTypeContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String contentType = request.getHeader("Content-Type");
            if (contentType != null) {
                ContentTypeContext.setRequestContentType(contentType);
            }

            String accept = request.getHeader("Accept");
            if (accept != null) {
                ContentTypeContext.setAcceptContentType(accept);
                if ("application/flatbuffers".equals(accept)) {
                    response.setContentType("application/flatbuffers");
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            ContentTypeContext.clear();
        }
    }
}
