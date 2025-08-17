package org.example.customapisvc.config;

import lombok.RequiredArgsConstructor;
import org.example.customapisvc.interceptor.UserActivityLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 클래스
 * 인터셉터 및 기타 웹 관련 설정
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserActivityLoggingInterceptor userActivityLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userActivityLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/v1/health/**",
                    "/api/v1/actuator/**",
                    "/api/v1/swagger-ui/**",
                    "/api/v1/v3/api-docs/**"
                );
    }
}