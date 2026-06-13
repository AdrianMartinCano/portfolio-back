package com.adrianmartincano.portfolio.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactoForm(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 2000) String mensaje,
        String website, // honeypot: si viene relleno, es un bot
        String lang) {} // idioma de la web ("es"/"en") para la confirmación
