# Portfolio API

API REST que alimenta el [portfolio](https://github.com/AdrianMartinCano/portfolio) (frontend Angular): sirve los datos de proyectos, experiencia y formación en dos idiomas y gestiona el formulario de contacto por email.

🔗 **Frontend en vivo:** [www.codeadrianmc.dev](https://www.codeadrianmc.dev)

**Stack:** Java 21 · Spring Boot 3.5 · Spring Web · Bean Validation · Resend (email)

---

## 🔌 Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/proyectos?lang=es\|en` | Lista de proyectos |
| `GET` | `/api/experiencia?lang=es\|en` | Experiencia profesional |
| `GET` | `/api/formacion?lang=es\|en` | Formación académica |
| `POST` | `/api/contacto` | Envío del formulario de contacto |

El parámetro `lang` por defecto es `es`; cualquier valor no soportado cae también a `es`.

### `POST /api/contacto`

```json
{
  "nombre": "...",
  "email": "...",
  "mensaje": "...",
  "website": "",        // honeypot: debe ir vacío (lo rellenan los bots)
  "lang": "es"          // idioma del correo de confirmación
}
```

- **Validación:** `@NotBlank`, `@Email`, límites de tamaño (Bean Validation).
- **Honeypot:** si `website` viene relleno, se responde `200` sin enviar nada.
- **Rate limiting:** máximo 3 envíos por IP cada 10 minutos (`429` si se supera).
- **Emails (Resend):** notificación al titular + **auto-reply de confirmación** al visitante, con plantilla HTML estilo terminal e idioma según `lang`. El auto-reply es *best-effort* (si falla, no rompe la petición).

---

## 🏗️ Cómo funciona

- Los datos viven en `src/main/resources/datos/{es,en}.json` y se cargan **en memoria** al arrancar (`@PostConstruct` en `DatosService`), deserializados con Jackson. No hay base de datos.
- `DatosController` expone los datos; `ContactoController` gestiona el formulario apoyándose en `EmailService`.
- `CorsConfig` permite el origen de producción (`https://www.codeadrianmc.dev`) y `http://localhost:4200` para desarrollo.

```
src/main/java/com/adrianmartincano/portfolio/
├── controller/   DatosController, ContactoController
├── services/     DatosService, EmailService
├── DTO/          Datos, ProyectoDTO, ExperienciaDTO, FormacionDTO, RepositorioDTO, ContactoForm
└── config/       CorsConfig
src/main/resources/
├── datos/        es.json, en.json   (fuente de los datos)
└── application.properties
```

---

## ⚙️ Configuración (variables de entorno)

| Variable | Descripción |
|---|---|
| `RESEND_API_KEY` | API key de [Resend](https://resend.com) para el envío de correos |
| `CONTACTO_DESTINO` | Email donde llegan los mensajes del formulario |
| `PORT` | Puerto del servidor (por defecto `8080`) |

Definidas en `application.properties` como placeholders (`${RESEND_API_KEY:}`…). **No hay secretos en el repo**: hay que proveerlas por entorno (en local o en el panel de Render).

---

## 🚀 Cómo ejecutar

```bash
# Variables de entorno necesarias para el contacto (en local)
export RESEND_API_KEY=...           # Windows PowerShell: $env:RESEND_API_KEY="..."
export CONTACTO_DESTINO=...

./mvnw spring-boot:run              # → http://localhost:8080
```

```bash
./mvnw clean package               # genera el .jar en target/
```

Sin las variables de Resend el resto de la API (datos) funciona igual; solo fallaría el envío de correo.

---

## ☁️ Deploy

Desplegado en **Render**. Las variables de entorno se configuran en el dashboard (Environment). Al guardarlas o hacer push, Render redespliega automáticamente.
