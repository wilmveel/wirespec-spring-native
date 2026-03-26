package com.example.todo.config;

import com.example.todo.generated.model.ContentTypeContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor that captures Content-Type and Accept headers into ThreadLocal context.
 * Uses HandlerInterceptor instead of Filter to properly handle async
 * CompletableFuture responses where the response body is written after
 * the filter chain completes.
 */
@Component
public class ContentTypeInterceptor implements AsyncHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String contentType = request.getHeader("Content-Type");
        if (contentType != null) {
            ContentTypeContext.setRequestContentType(contentType);
        }

        String accept = request.getHeader("Accept");
        if (accept != null) {
            ContentTypeContext.setAcceptContentType(accept);
        }

        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Do NOT clear context here - the async response writing still needs it
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Context is still needed during response body writing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ContentTypeContext.clear();
        FlatBufferBytesHolder.clear();
    }
}
