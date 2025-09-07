package org.example.customapisvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 기본 설정 클래스
 * 모든 요청을 허용하는 개발용 보안 설정
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    public SecurityConfig() {
        System.out.println("🔧 SecurityConfig 생성자 호출됨 - 모든 요청 허용 설정");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("🔧 SecurityFilterChain 빈 생성 중 - 모든 요청 permitAll 설정");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/**").permitAll()
                .anyRequest().permitAll()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.disable());
        System.out.println("✅ SecurityFilterChain 설정 완료 - 모든 인증 비활성화, 모든 요청 허용");
        return http.build();
    }
}