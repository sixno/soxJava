package com.sox.api.configuration;

import com.sox.api.interceptor.AlwaysInterceptor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootConfiguration
public class CommonConfiguration implements WebMvcConfigurer {
    @Bean
    public AlwaysInterceptor alwaysInterceptor() {
        return new AlwaysInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alwaysInterceptor()).addPathPatterns("/**");
    }
}
