package com.adrianmartincano.portfolio.controller;

import com.adrianmartincano.portfolio.DTO.ContactoForm;
import com.adrianmartincano.portfolio.services.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class ContactoController {

    private static final Logger log = LoggerFactory.getLogger(ContactoController.class);

    private static final int MAX_ENVIOS = 3;
    private static final long VENTANA_MINUTOS = 10;

    private final EmailService emailService;
    private final Map<String, List<Instant>> envios = new ConcurrentHashMap<>();

    public ContactoController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/contacto")
    public ResponseEntity<Void> contacto(@Valid @RequestBody ContactoForm form, HttpServletRequest request) {
        // Honeypot: un humano nunca rellena este campo. Si viene relleno, respondemos
        // 200 sin enviar nada para no darle pistas al bot.
        if (form.website() != null && !form.website().isBlank()) {
            log.info("Honeypot activado (posible bot), descartando envío sin notificar");
            return ResponseEntity.ok().build();
        }

        if (!permitido(request.getRemoteAddr())) {
            log.warn("Rate limit superado para IP {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            emailService.enviarContacto(form);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Fallo al enviar el correo de contacto", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean permitido(String ip) {
        Instant limite = Instant.now().minus(VENTANA_MINUTOS, ChronoUnit.MINUTES);
        List<Instant> historial = envios.computeIfAbsent(ip, k -> new ArrayList<>());

        synchronized (historial) {
            historial.removeIf(t -> t.isBefore(limite));
            if (historial.size() >= MAX_ENVIOS) {
                return false;
            }
            historial.add(Instant.now());
            return true;
        }
    }
}
