package ch.uzh.ifi.hase.soprafs24.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> globalCorsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://sopra-fs25-group-15-client.vercel.app"
        ));
        cfg.addAllowedHeader(CorsConfiguration.ALL);
        cfg.addAllowedMethod(CorsConfiguration.ALL);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        // apply to every URL—REST and SockJS alike
        src.registerCorsConfiguration("/", cfg);

        FilterRegistrationBean<CorsFilter> bean =
            new FilterRegistrationBean<>(new CorsFilter(src));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
