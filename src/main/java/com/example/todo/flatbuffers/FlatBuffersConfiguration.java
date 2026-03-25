package com.example.todo.flatbuffers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.java.Wirespec;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class FlatBuffersConfiguration implements WebMvcConfigurer {

    @Bean
    @Primary
    public Wirespec.Serialization contentNegotiatingWirespecSerialization() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        Wirespec.Serialization jsonSerialization = new WirespecSerialization(objectMapper);
        Wirespec.Serialization flatBuffersSerialization = new FlatBuffersSerialization();
        return new ContentNegotiatingWirespecSerialization(jsonSerialization, flatBuffersSerialization);
    }

    @Bean
    public FilterRegistrationBean<FlatBuffersContentNegotiationFilter> flatBuffersFilter() {
        FilterRegistrationBean<FlatBuffersContentNegotiationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FlatBuffersContentNegotiationFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("flatbuffers", FlatBuffersMediaType.APPLICATION_FLATBUFFERS);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addLast(new FlatBuffersHttpMessageConverter());
    }
}
