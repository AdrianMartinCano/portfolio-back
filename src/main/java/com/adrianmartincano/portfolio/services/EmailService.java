package com.adrianmartincano.portfolio.services;

import com.adrianmartincano.portfolio.DTO.ContactoForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Arte ASCII "ADRIAN" (figlet de barras). La web usa un wordmark en bloque
    // (ANSI Shadow), pero en email los caracteres de bloque/box-drawing rompen
    // en algunos clientes (Outlook), así que aquí se mantiene el figlet de barras
    // —texto puro, se renderiza siempre— recoloreado al magenta de la firma.
    // El '<' va escapado.
    private static final String ART = String.join("\n",
            "    _     ____   ____   ___     _     _   _",
            "   / \\   |  _ \\ |  _ \\ |_ _|   / \\   | \\ | |",
            "  / _ \\  | | | || |_) | | |   / _ \\  |  \\| |",
            " / ___ \\ | |_| ||  _ &lt;  | |  / ___ \\ | |\\  |",
            "/_/   \\_\\|____/ |_| \\_\\|___|/_/   \\_\\|_| \\_|");

    // Prompt reutilizable: adrian@portfolio:~$
    private static final String PROMPT =
            "<span style=\"color:#9ece6a;\">adrian@portfolio</span>"
            + "<span style=\"color:#828bb8;\">:</span>"
            + "<span style=\"color:#7aa2f7;\">~</span>"
            + "<span style=\"color:#828bb8;\">$</span>";

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${contacto.destino}")
    private String destino;

    // Remitente de los correos. El email debe coincidir con el buzón autenticado
    // en el SMTP de Plesk (spring.mail.username) para que SPF/DKIM queden alineados.
    @Value("${contacto.remitente.email}")
    private String remitenteEmail;

    @Value("${contacto.remitente.nombre}")
    private String remitenteNombre;

    public void enviarContacto(ContactoForm form) {
        log.info("Procesando contacto de '{}' <{}>", form.nombre(), form.email());

        // Notificación para mí: es la importante, si falla que propague el error.
        enviar(notificacion(form));
        log.info("Notificación enviada a {}", destino);

        // Confirmación para el visitante: best-effort. Si falla, lo registramos
        // pero no rompemos la petición (la notificación ya se envió).
        try {
            enviar(confirmacion(form));
            log.info("Confirmación enviada a {}", form.email());
        } catch (Exception e) {
            log.warn("No se pudo enviar la confirmación a {}: {}", form.email(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void enviar(Map<String, Object> cuerpo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            // multipart=true para enviar texto plano + HTML como alternativas.
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom((String) cuerpo.get("from"));
            helper.setTo(((List<String>) cuerpo.get("to")).toArray(new String[0]));
            if (cuerpo.get("reply_to") != null) {
                helper.setReplyTo((String) cuerpo.get("reply_to"));
            }
            helper.setSubject((String) cuerpo.get("subject"));
            // setText(plano, html): el cliente muestra el HTML y cae al texto si no puede.
            helper.setText((String) cuerpo.get("text"), (String) cuerpo.get("html"));
            mailSender.send(mensaje);
        } catch (MailException | MessagingException e) {
            // El SMTP de Plesk explica el motivo (auth incorrecta, 'from' no permitido,
            // conexión rechazada, timeout...).
            log.error("El SMTP rechazó el envío: {}", e.getMessage());
            throw new RuntimeException("Fallo al enviar el correo de contacto", e);
        }
    }

    // Correo que recibo yo cuando alguien usa el formulario.
    private Map<String, Object> notificacion(ContactoForm form) {
        String fecha = ahora();

        String filas =
                  filaNeo("Nombre", esc(form.nombre()))
                + filaNeo("Email", esc(form.email()))
                + filaNeo("Fecha", fecha);

        String cuerpo = ficha("cat contacto.log", filas)
                + "<div style=\"margin-bottom:16px;\">" + PROMPT
                + " <span style=\"color:#c0caf5;\">cat mensaje.txt</span></div>"
                + "<div style=\"color:#c0caf5; white-space:pre-wrap; word-break:break-word;\">"
                + esc(form.mensaje()) + "</div>";

        String html = envolver("es", cuerpo, "// responde a este correo para contestar al remitente");

        // Texto plano de respaldo.
        String text = "Nombre: " + form.nombre() + "\n"
                + "Email: " + form.email() + "\n"
                + "Fecha: " + fecha + "\n\n"
                + form.mensaje();

        return Map.of(
                "from", "Portfolio Contacto <" + remitenteEmail + ">",
                "to", List.of(destino),
                "reply_to", form.email(),
                "subject", "Nuevo contacto de " + form.nombre(),
                "text", text,
                "html", html
        );
    }

    // Confirmación que recibe el visitante.
    private Map<String, Object> confirmacion(ContactoForm form) {
        boolean en = "en".equalsIgnoreCase(form.lang());

        String subject = en ? "Message received" : "He recibido tu mensaje";
        String estado = en ? "message received successfully" : "mensaje recibido correctamente";
        String saludo = en ? "Hi " : "Hola ";
        String cuerpoTexto = en
                ? "I've received your message and will reply as soon as possible. "
                        + "Thanks for reaching out."
                : "He recibido tu mensaje y te responderé lo antes posible. "
                        + "Gracias por ponerte en contacto.";
        String pie = en
                ? "// this is an automated message, no need to reply"
                : "// este es un mensaje automático, no hace falta responder";

        String fecha = ahora();
        String html = htmlConfirmacion(en, subject, esc(form.email()), fecha,
                estado, saludo, esc(form.nombre()), cuerpoTexto, pie, esc(remitenteNombre));

        // Texto plano de respaldo para clientes que no renderizan HTML.
        String text = saludo + form.nombre() + ",\n\n" + cuerpoTexto + "\n\n— " + remitenteNombre;

        return Map.of(
                "from", remitenteNombre + " <" + remitenteEmail + ">",
                "to", List.of(form.email()),
                "subject", subject,
                "text", text,
                "html", html
        );
    }

    // Cuerpo HTML de la confirmación (sesión de terminal: neofetch + mail --status).
    private String htmlConfirmacion(boolean en, String asunto, String remitente, String fecha,
                                    String estado, String saludo, String nombre,
                                    String cuerpoTexto, String pie, String firma) {
        String filas =
                  filaNeo(en ? "Subject" : "Asunto", asunto)
                + filaNeo(en ? "From" : "De", remitente)
                + filaNeo(en ? "Date" : "Fecha", fecha);

        String cuerpo = ficha("neofetch", filas)
                + "<div style=\"margin-bottom:16px;\">" + PROMPT
                + " <span style=\"color:#c0caf5;\">mail --status</span></div>"
                + "<div style=\"color:#9ece6a; margin-bottom:18px;\">&#10003; " + estado + "</div>"
                + "<div style=\"color:#c0caf5; margin-bottom:14px;\"><span style=\"color:#9ece6a;\">&gt;</span> "
                + saludo + nombre + ",</div>"
                + "<div style=\"color:#c0caf5; margin-bottom:18px;\">" + cuerpoTexto + "</div>"
                + "<div style=\"color:#828bb8; margin-bottom:18px;\">&#8212; " + firma + "</div>"
                + "<div>" + PROMPT + " <span style=\"color:#c0caf5;\">_</span></div>";

        return envolver(en ? "en" : "es", cuerpo, pie);
    }

    // Ventana de terminal (paleta Tokyo Night): barra de título, cuerpo y pie.
    private String envolver(String lang, String cuerpo, String pie) {
        return ("""
                <!DOCTYPE html>
                <html lang="%LANG%">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    /* El bloque neofetch (arte ASCII + datos) va en dos columnas en
                       escritorio. En móvil esas dos columnas no caben y el cliente
                       hace zoom-out dejando todo ilegible, así que aquí se apilan:
                       el arte arriba (centrado y un punto más pequeño) y los datos
                       debajo. Las reglas necesitan !important para ganar al estilo
                       inline. Outlook-desktop ignora la media query y mantiene la
                       vista de escritorio, que en pantalla grande es lo deseado. */
                    @media only screen and (max-width:480px) {
                      .nf-col { display:block !important; width:100% !important; }
                      .nf-art { padding:0 0 16px 0 !important; text-align:center !important; }
                      .nf-art pre { font-size:9px !important; display:inline-block !important; }
                      .nf-data { padding:0 !important; }
                    }
                  </style>
                </head>
                <body style="margin:0; padding:0; background-color:#16161e;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#16161e; padding:24px 0;">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px; width:100%; border:1px solid #2f334d; border-radius:8px; overflow:hidden; font-family:'SF Mono','Consolas','Liberation Mono',Menlo,monospace;">
                        <tr>
                          <td style="background-color:#24283b; padding:10px 14px; border-bottom:1px solid #2f334d;">
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#f7768e; margin-right:6px;"></span>
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#e0af68; margin-right:6px;"></span>
                            <span style="display:inline-block; width:11px; height:11px; border-radius:50%; background-color:#9ece6a;"></span>
                            <span style="color:#828bb8; font-size:12px; vertical-align:middle; margin-left:10px;">adrian@portfolio: ~</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="background-color:#1a1b26; padding:22px; font-size:14px; line-height:1.7;">%CUERPO%</td>
                        </tr>
                        <tr>
                          <td style="background-color:#24283b; padding:10px 14px; border-top:1px solid #2f334d; color:#828bb8; font-size:11px;">%PIE% &nbsp;&#183;&nbsp; <a href="https://www.codeadrianmc.dev" style="color:#7aa2f7; text-decoration:none;">www.codeadrianmc.dev</a></td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """)
                .replace("%LANG%", lang)
                .replace("%PIE%", pie)
                .replace("%CUERPO%", cuerpo);
    }

    // Ficha estilo neofetch: línea de comando + arte ASCII (izq.) y tabla de datos (der.).
    private String ficha(String comando, String filas) {
        return "<div style=\"margin-bottom:14px;\">" + PROMPT
                + " <span style=\"color:#c0caf5;\">" + comando + "</span></div>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:22px;\">"
                + "<tr>"
                + "<td valign=\"top\" class=\"nf-col nf-art\" style=\"padding-right:22px;\">"
                + "<pre style=\"margin:0; color:#bb9af7; font-size:10px; line-height:1.15; white-space:pre; font-family:'SF Mono','Consolas','Liberation Mono',Menlo,monospace;\">"
                + ART + "</pre>"
                + "</td>"
                + "<td valign=\"top\" class=\"nf-col nf-data\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:13px;\">"
                + filas + "</table>"
                + "</td>"
                + "</tr>"
                + "</table>";
    }

    private static String filaNeo(String label, String value) {
        return "<tr>"
                + "<td style=\"color:#9ece6a; padding:0 14px 3px 0; white-space:nowrap; vertical-align:top;\">" + label + "</td>"
                + "<td style=\"color:#c0caf5; padding-bottom:3px; word-break:break-word;\">" + value + "</td>"
                + "</tr>";
    }

    private static String ahora() {
        return ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // Escapa los caracteres con significado en HTML para que el contenido del
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
