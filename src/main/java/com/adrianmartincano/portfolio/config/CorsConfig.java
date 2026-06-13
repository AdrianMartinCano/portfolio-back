package com.adrianmartincano.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns (no allowedOrigins) para poder usar comodines.
        // El patrón de Vercel se acota a MI team (adrianmartincanos-projects):
        // nadie más puede desplegar bajo ese slug, así que no abre la API a
        // cualquier app de *.vercel.app.
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "https://www.codeadrianmc.dev",
                        "https://codeadrianmc.dev",
                        "https://*-adrianmartincanos-projects.vercel.app", // previews de mi cuenta
                        "http://localhost:4200"
                )
                .allowedMethods("GET", "POST");
    }
}
