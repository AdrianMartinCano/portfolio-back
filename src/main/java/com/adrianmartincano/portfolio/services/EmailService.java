package com.adrianmartincano.portfolio.services;

import com.adrianmartincano.portfolio.DTO.ContactoForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Arte ASCII "ADRIAN" (figlet), el mismo que el neofetch de la web. Texto
    // puro: se renderiza siempre, sin imágenes ni SVG. El '<' va escapado.
    private static final String ART = String.join("\n",
            "    _     ____   ____   ___     _     _   _",
            "   / \\   |  _ \\ |  _ \\ |_ _|   / \\   | \\ | |",
            "  / _ \\  | | | || |_) | | |   / _ \\  |  \\| |",
            " / ___ \\ | |_| ||  _ &lt;  | |  / ___ \\ | |\\  |",
            "/_/   \\_\\|____/ |_| \\_\\|___|/_/   \\_\\|_| \\_|");

    // Prompt reutilizable: adrian@portfolio:~$
    private static final String PROMPT =
            "<span style=\"color:#3fb950;\">adrian@portfolio</span>"
            + "<span style=\"color:#8b949e;\">:</span>"
            + "<span style=\"color:#58a6ff;\">~</span>"
            + "<span style=\"color:#8b949e;\">$</span>";

    private final RestClient http = RestClient.create();

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${contacto.destino}")
    private String destino;

    // Remitente de los correos. El email debe pertenecer a un dominio
    // verificado en Resend.
    @Value("${contacto.remitente.email}")
    private String remitenteEmail;

    @Value("${contacto.remitente.nombre}")
    private String remitenteNombre;

    public void enviarContacto(ContactoForm form) {
        // Notificación para mí: es la importante, si falla que propague el error.
        enviar(Map.of(
                "from", "Portfolio <" + remitenteEmail + ">",
                "to", List.of(destino),
                "reply_to", form.email(),
                "subject", "Nuevo contacto de " + form.nombre(),
                "text", form.mensaje()
        ));

        // Confirmación para el visitante: best-effort. Si falla, lo registramos
        // pero no rompemos la petición (la notificación ya se envió).
        try {
            enviar(confirmacion(form));
        } catch (Exception e) {
            log.warn("No se pudo enviar la confirmación a {}: {}", form.email(), e.getMessage());
        }
    }

    private void enviar(Map<String, Object> cuerpo) {
        http.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> confirmacion(ContactoForm form) {
        boolean en = "en".equalsIgnoreCase(form.lang());
        String nombre = form.nombre();

        String subject = en ? "Message received" : "He recibido tu mensaje";
        String estado = en ? "message received successfully" : "mensaje recibido correctamente";
        String saludo = en ? "Hi " : "Hola ";
        String cuerpo = en
                ? "I've received your message and will reply as soon as possible. "
                        + "Thanks for reaching out."
                : "He recibido tu mensaje y te responderé lo antes posible. "
                        + "Gracias por ponerte en contacto.";
        String pie = en
                ? "// this is an automated message, no need to reply"
                : "// este es un mensaje automático, no hace falta responder";

        // Datos contextuales para el panel "neofetch" (resumen del correo).
        String remitente = esc(form.email());
        String fecha = ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Texto plano de respaldo para clientes que no renderizan HTML.
        String text = saludo + nombre + ",\n\n" + cuerpo + "\n\n— " + remitenteNombre;

        return Map.of(
                "from", remitenteNombre + " <" + remitenteEmail + ">",
                "to", List.of(form.email()),
                "subject", subject,
                "text", text,
                "html", htmlTerminal(estado, saludo, esc(nombre), cuerpo, pie, en, subject, remitente, fecha, esc(remitenteNombre))
        );
    }

    // Correo con aspecto de ventana de terminal, a juego con la web (paleta GitHub dark).
    private String htmlTerminal(String estado, String saludo, String nombre,
                                String cuerpo, String pie, boolean en,
                                String asunto, String remitente, String fecha, String firma) {
        return ("""
                <!DOCTYPE html>
                <html lang="%LANG%">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#010409;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#010409; padding:24px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%; border:1px solid #30363d; border-radius:8px; overflow:hidden; font-family:'SF Mono','Consolas','Liberation Mono',Menlo,monospace;">
                        <tr>
                          <td style="background-color:#161b22; padding:10px 14px; border-bottom:1px solid #30363d;">
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#f85149; margin-right:6px;"></span>
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#d29922; margin-right:6px;"></span>
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#3fb950;"></span>
                            <span style="color:#8b949e; font-size:12px; vertical-align:middle; margin-left:10px;">adrian@portfolio: ~</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="background-color:#0d1117; padding:22px; font-size:14px; line-height:1.7;">
                            %NEOFETCH%
                            <div style="margin-bottom:16px;">%PROMPT% <span style="color:#e6edf3;">mail --status</span></div>
                            <div style="color:#3fb950; margin-bottom:18px;">&#10003; %ESTADO%</div>
                            <div style="color:#e6edf3; margin-bottom:14px;"><span style="color:#3fb950;">&gt;</span> %SALUDO%%NOMBRE%,</div>
                            <div style="color:#e6edf3; margin-bottom:18px;">%CUERPO%</div>
                            <div style="color:#8b949e; margin-bottom:18px;">&#8212; %FIRMA%</div>
                            <div>%PROMPT% <span style="color:#e6edf3;">_</span></div>
                          </td>
                        </tr>
                        <tr>
                          <td style="background-color:#161b22; padding:10px 14px; border-top:1px solid #30363d; color:#8b949e; font-size:11px;">%PIE% &nbsp;&#183;&nbsp; <a href="https://www.codeadrianmc.dev" style="color:#58a6ff; text-decoration:none;">www.codeadrianmc.dev</a></td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """)
                .replace("%LANG%", en ? "en" : "es")
                .replace("%NEOFETCH%", neofetch(en, asunto, remitente, fecha))
                .replace("%PROMPT%", PROMPT)
                .replace("%ESTADO%", estado)
                .replace("%SALUDO%", saludo)
                .replace("%NOMBRE%", nombre)
                .replace("%CUERPO%", cuerpo)
                .replace("%FIRMA%", firma)
                .replace("%PIE%", pie);
    }

    // Bloque neofetch: comando + arte ASCII (izquierda) y resumen del correo (derecha).
    private String neofetch(boolean en, String asunto, String remitente, String fecha) {
        String info =
                  filaNeo(en ? "Subject" : "Asunto", asunto)
                + filaNeo(en ? "From" : "De", remitente)
                + filaNeo(en ? "Date" : "Fecha", fecha);

        return "<div style=\"margin-bottom:14px;\">" + PROMPT
                + " <span style=\"color:#e6edf3;\">neofetch</span></div>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:22px;\">"
                + "<tr>"
                + "<td valign=\"top\" style=\"padding-right:22px;\">"
                + "<pre style=\"margin:0; color:#58a6ff; font-size:10px; line-height:1.15; white-space:pre; font-family:'SF Mono','Consolas','Liberation Mono',Menlo,monospace;\">"
                + ART + "</pre>"
                + "</td>"
                + "<td valign=\"top\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:13px;\">"
                + info + "</table>"
                + "</td>"
                + "</tr>"
                + "</table>";
    }

    private static String filaNeo(String label, String value) {
        return "<tr>"
                + "<td style=\"color:#3fb950; padding:0 14px 3px 0; white-space:nowrap; vertical-align:top;\">" + label + "</td>"
                + "<td style=\"color:#e6edf3; padding-bottom:3px;\">" + value + "</td>"
                + "</tr>";
    }

    // Escapa los caracteres con significado en HTML para que el nombre del
    // visitante no pueda romper la plantilla ni inyectar marcado.
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
