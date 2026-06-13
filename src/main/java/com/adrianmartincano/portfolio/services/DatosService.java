package com.adrianmartincano.portfolio.services;

import com.adrianmartincano.portfolio.DTO.Datos;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class DatosService {

    private static final Logger log = LoggerFactory.getLogger(DatosService.class);

    // Inyectamos el ObjectMapper que configura Spring Boot
    // (ignora campos desconocidos del JSON en vez de fallar)
    private final ObjectMapper mapper;

    private final Map<String, Datos> idioma = new HashMap<>();

    public DatosService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void cargar() throws IOException {
        try {
            idioma.put("es", leer("datos/es.json"));
            idioma.put("en", leer("datos/en.json"));
            log.info("Datos del portfolio cargados (es: {} proyectos, en: {} proyectos)",
                    idioma.get("es").proyectos().size(), idioma.get("en").proyectos().size());
        } catch (IOException e) {
            // Falla el arranque a propósito: sin datos la API no sirve para nada,
            // y así el error queda claro en los logs en vez de salir un 500 luego.
            log.error("No se pudieron cargar los JSON de datos al arrancar: {}", e.getMessage());
            throw e;
        }
    }

    private Datos leer(String ruta) throws IOException {
        try (InputStream in = new ClassPathResource(ruta).getInputStream()) {
            return mapper.readValue(in, Datos.class);
        }
    }

    // Este método es por si nos piden un lenguaje que no existe
    public Datos get(String lang) {
        return idioma.getOrDefault(lang, idioma.get("es"));
    }
}
