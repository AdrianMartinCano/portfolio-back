package com.adrianmartincano.portfolio.controller;


import com.adrianmartincano.portfolio.DTO.ExperienciaDTO;
import com.adrianmartincano.portfolio.DTO.FormacionDTO;
import com.adrianmartincano.portfolio.DTO.ProyectoDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.adrianmartincano.portfolio.services.DatosService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DatosController {
    private final DatosService datos;
    public DatosController(DatosService datos) {
        this.datos = datos;
    }
    @GetMapping("/proyectos")
    public List<ProyectoDTO> proyectos(@RequestParam(defaultValue = "es") String
                                            lang) {
        return datos.get(lang).proyectos();
    }
    @GetMapping("/experiencia")
    public List<ExperienciaDTO> experiencia(@RequestParam(defaultValue = "es") String
                                                 lang) {
        return datos.get(lang).experiencia();
    }
    @GetMapping("/formacion")
    public List<FormacionDTO> formacion(@RequestParam(defaultValue = "es") String
                                             lang) {
        return datos.get(lang).formacion();
    }

}
