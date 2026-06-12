package com.adrianmartincano.portfolio.DTO;

import java.util.List;

public record ExperienciaDTO(
        String puesto,
        String empresa,
        String fechaInicio,
        String fechaFin,
        List<String> tareas
) {
}
