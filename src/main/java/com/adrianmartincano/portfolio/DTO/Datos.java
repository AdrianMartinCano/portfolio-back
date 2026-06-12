package com.adrianmartincano.portfolio.DTO;

import java.util.List;

public record Datos(
        List<ProyectoDTO> proyectos,
        List<ExperienciaDTO> experiencia,
        List<FormacionDTO> formacion
) {
}
