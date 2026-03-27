package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ContentTypeInterceptor contentTypeInterceptor;

    public WebMvcConfig(ContentTypeInterceptor contentTypeInterceptor) {
        this.contentTypeInterceptor = contentTypeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(contentTypeInterceptor).addPathPatterns("/api/**");
    }
}
