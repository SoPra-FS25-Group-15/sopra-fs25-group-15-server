package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("https://sopra-fs25-group-15-client.vercel.app",
                          "http://localhost:3000")  // Use allowedOriginPatterns instead of allowedOrigins
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")
                        .allowCredentials(true);
                
                // Explicitly add WebSocket endpoints
                registry.addMapping("/ws/**")
                        .allowedOriginPatterns("https://sopra-fs25-group-15-client.vercel.app",
                          "http://localhost:3000")  // Use allowedOriginPatterns instead of allowedOrigins
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")
                        .allowCredentials(true);
            }
        };
    }
}
