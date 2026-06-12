package com.adrianmartincano.portfolio.DTO;

import java.util.List;

public record ProyectoDTO(
        String slug,
        String nombre,
        String tipo,
        String permisos,
        String descripcion,
        String imagen,
        List<RepositorioDTO> repositorios,
        String demo,
        List<String> tecnologias,
        List<String> caracteristicas,
        String problema,
        String aprendizajes,
        String fecha,
        String estado) {}
