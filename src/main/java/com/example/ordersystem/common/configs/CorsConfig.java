package com.example.ordersystem.common.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // 콤마로 구분된 허용 origin 목록. 로컬/도커 개발용 origin을 기본값에 포함시켜 두고,
    // 배포 환경에서는 CORS_ALLOWED_ORIGINS 환경변수로 덮어쓴다.
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry){
        corsRegistry.addMapping("/**")
                .allowedOrigins(allowedOrigins) // 허용 url 명시 (우리 서버에 들어올 수 있는 url)
                .allowedMethods("*") // CRUD
                .allowedHeaders("*")
                .allowCredentials(true);
    }

}
