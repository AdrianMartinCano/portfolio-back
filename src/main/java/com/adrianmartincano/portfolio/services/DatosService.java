package com.adrianmartincano.portfolio.services;

import com.adrianmartincano.portfolio.DTO.Datos;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class DatosService {

    // Inyectamos el ObjectMapper que configura Spring Boot
    // (ignora campos desconocidos del JSON en vez de fallar)
    private final ObjectMapper mapper;

    private final Map<String, Datos> idioma = new HashMap<>();

    public DatosService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void cargar() throws IOException {
        idioma.put("es", leer("datos/es.json"));
        idioma.put("en", leer("datos/en.json"));
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
