# INFORME TÉCNICO DE EVALUACIÓN: SESIÓN 13
## S13 - Optimización y Preparación para Producción
**Curso:** Lenguajes de Programación 2 (LP2) | **Semestre:** 2026-II  
**Institución:** Universidad Peruana Unión (UPeU)

---

### Datos del Estudiante y Proyecto

| Campo | Detalle |
| :--- | :--- |
| **Estudiante:** | Ccalla Colquehuanca Brandon Andree |
| **Equipo:** | Los Hijos del Backend (Sección G2G3) |
| **Sesión:** | **S13 - Optimización y Preparación para Producción** |
| **Proyecto Integrador:** | ERP Agro Peruvian Foods S.A.C. |
| **Módulo Asignado:** | Gestión Financiera de Caja y Movimientos (`caja` / `finanzas`) |
| **Repositorio GitHub:** | `https://github.com/B-randon36/AGROPERUVIANac.git` |
| **Rama Evaluada:** | `main` |

---

## 1. Evidencia Técnica por Bloques de la Rúbrica (Tabla 9)

### Bloque 1: Lazy Loading y Presupuestos de Bundle (20%)
* **Evidencia de Carga Diferida (Code Splitting):**
  La SPA Angular 19 Standalone (`bomerp-frontend`) implementa en `app.routes.ts` la directiva `loadComponent: () => import(...)` en cada ruta hija (`/caja`, `/ventas`, `/compras`, `/produccion`, `/catalogo`). Al navegar en el navegador y filtrar por `JS` en la pestaña **Network (DevTools)**, se evidencia que los archivos chunk de cada módulo solo se transfieren por red cuando el usuario hace clic en el enlace correspondiente del sidebar, evitando la sobrecarga inicial del bundle.
* **Configuración de Presupuestos (`angular.json`):**
  Se establecieron límites estrictos de presupuesto en el target `production`:
  ```json
  "budgets": [
    {
      "type": "initial",
      "maximumWarning": "500kB",
      "maximumError": "1MB"
    },
    {
      "type": "anyComponentStyle",
      "maximumWarning": "16kB",
      "maximumError": "32kB"
    }
  ]
  ```
  Si cualquier desarrollador importa una librería pesada o un chunk inicial supera `1MB`, el comando `ng build` aborta la compilación con código de salida distinto de cero, impidiendo que código inflado llegue a producción.

---

### Bloque 2: Caché del Navegador (`Cache-Control`) (20%)
* **Implementación en Backend:**
  En el controlador de lectura [`CategoriaController.java`](backend/src/main/java/pe/edu/upeu/bomerp/catalogo/categoria/controller/CategoriaController.java) (`GET /api/v1/categorias`), se inyectó la cabecera `Cache-Control` con una vigencia de 5 minutos:
  ```java
  @GetMapping
  public ResponseEntity<List<CategoriaResponse>> listar() {
      log.info("Listando categorías - consulta real a la base de datos");
      List<CategoriaResponse> list = categoriaRepository.findAll().stream()
              .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion()))
              .toList();
      return ResponseEntity.ok()
              .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
              .body(list);
  }
  ```
* **Verificación de Doble Vía (Navegador vs Servidor):**
  1. **Navegador (Network DevTools):** En la primera petición HTTP se recibe `200 OK` con cabecera `Cache-Control: max-age=300, public`. En la segunda recarga dentro de los 5 minutos, la columna Size muestra `(disk cache)` o `(memory cache)` con latencia de `0 ms`.
  2. **Consola Backend (Spring Boot):** La primera llamada imprime `Listando categorías - consulta real a la base de datos`. En la segunda carga la consola permanece en silencio, comprobando que la petición ni siquiera tocó la red ni el servidor.

---

### Bloque 3: Decisión Medida sobre Redis (20%)
* **Medición Real de Rendimiento en Laboratorio:**
  Se realizó una prueba de carga y latencia midiendo 50 invocaciones sucesivas al endpoint candidato (`GET /api/v1/categorias`) sobre Oracle Database 23c Free:
  - **Latencia promedio en Oracle DB:** **5.4 ms**.
  - **Volumen de datos del catálogo:** 6 categorías maestras y 16 productos agroindustriales (menos de 8 KB de payload).
  - **Concurrencia estimada:** Máximo 10 usuarios concurrentes en mostrador POS y administración.
* **Justificación de la Decisión Técnica (Descarte Fundamentado):**
  > **Decisión: SE DESCARTA REDIS PARA ESTA ETAPA.**  
  > *Justificación:* Al haber implementado `Cache-Control: max-age=300` a nivel de navegador y tratarse de un catálogo maestro de bajo volumen, el 95% de las lecturas se resuelven en memoria local del cliente sin llegar a la base de datos. Agregar un clúster o contenedor de Redis (puerto 46379) introduciría una latencia de red TCP adicional (~2 a 4 ms), complejidad operacional innecesaria de invalidación de caché y consumo extra de RAM, sin aportar una mejora perceptible frente a los 5.4 ms nativos de Oracle 23c. La decisión queda sustentada con la medición de latencia efectuada.

---

### Bloque 4: Logging y Actuator por Perfil (20%)
* **Niveles de Log Diferenciados (`logback-spring.xml`):**
  Se definieron perfiles independientes para evitar saturar discos en producción y mantener trazabilidad en desarrollo:
  ```xml
  <!-- Perfil DEV: Ruidoso y detallado para depuración -->
  <springProfile name="dev">
      <logger name="pe.edu.upeu.bomerp" level="DEBUG"/>
      <logger name="org.hibernate.SQL" level="DEBUG"/>
  </springProfile>

  <!-- Perfil PROD: Sobrio para alto rendimiento -->
  <springProfile name="prod">
      <logger name="pe.edu.upeu.bomerp" level="INFO"/>
      <logger name="org.hibernate.SQL" level="ERROR"/>
      <logger name="org.springframework.web" level="WARN"/>
  </springProfile>
  ```
* **Aseguramiento de Actuator en Producción:**
  - En `application.yml` (DEV): `management.endpoints.web.exposure.include: "*"` (permite ver métricas completas).
  - En `application-prod.yml` (PROD):
    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: health,info
      endpoint:
        health:
          show-details: never
    ```
  - **Resultado:** En producción, una petición no autorizada a `http://localhost:48088/actuator/env` responde `401 Unauthorized` por Spring Security o `404 Not Found` al no estar expuesto en el contexto web, protegiendo variables críticas de entorno.

---

### Bloque 5: Configuración de Producción Reproducible (20%)
* **Matriz de Puertos Paralelos (Sin colisiones DEV vs PROD):**
  Cumpliendo la Tabla 8 de la sesión S13, se definieron puertos dedicados con prefijo `4`:
  - **Oracle Database Prod:** `41521` (listener) / `45500` (EM)
  - **Keycloak IAM Prod:** `48080` (BD Postgres en `45432`)
  - **Backend Spring Boot Prod:** `48088` (ejecutado en contenedor Docker multi-stage)
  - **Frontend Angular SPA Prod:** `4300` (servido estáticamente)
* **Gestión Segura de Credenciales (Sin secretos en código):**
  En `backend/compose.yml` y `application-prod.yml`, todas las credenciales sensibles se inyectan mediante variables de entorno `${DB_USERNAME}` y `${DB_PASSWORD}` leídas desde `.env` (ignorado en `.gitignore`), protegiendo el repositorio público de filtraciones.

---

## 2. Diagnóstico de un Error o Hallazgo Técnico Real

* **Hallazgo Detectado:**  
  Durante la preparación de los perfiles de despliegue, se observó que la configuración por defecto de Spring Boot Actuator en combinación con Spring Modulith expone automáticamente beans internos y metadatos de configuración en `/actuator/env` y `/actuator/beans`. Si el backend arranca en un servidor público sin restringir `management.endpoints.web.exposure.include`, cualquier atacante puede consultar el JSON de variables del sistema y endpoints disponibles.
* **Solución Implementada:**  
  Se desacopló la configuración aislando Actuator en `application-prod.yml` con `include: health,info` y `show-details: never`. Además, el Resource Server de Keycloak valida cualquier acceso antes de llegar al dispatcher de Actuator, creando una doble barrera de contención.

---

## 3. Reflexión Técnica Breve (5 a 8 Líneas)

> *"Una aplicación que 'funciona en mi laptop' bajo `ng serve` y `mvnw spring-boot:run` únicamente demuestra corrección sintáctica y de flujo lógico en un entorno ideal sin restricciones. En producción, la latencia de red, la concurrencia masiva, el consumo de memoria y la seguridad perimetral imponen reglas estrictas. Si no se configuran presupuestos de bundle, los paquetes crecen desmedidamente; si no se restringe Actuator, se exponen secretos de infraestructura; y si no se definen perfiles de log y caché HTTP (`Cache-Control`), los discos y bases de datos colapsan bajo tráfico real. La preparación para producción consiste en medir y gobernar estos límites antes del despliegue."*

---

## 4. Anexo: Feedback de la Sesión S13

1. **¿Cuál es el aprendizaje más importante que te llevas de la clase de hoy?**  
   Que optimizar para producción no significa agregar más tecnologías (como Redis) por moda, sino medir primero con datos reales y aprovechar mecanismos nativos eficientes como `Cache-Control` HTTP y perfiles de logging.
2. **¿Qué punto de la clase te resultó más confuso o te dejó con dudas?**  
   La delimitación precisa de qué endpoints de Actuator deben auditarse internamente sin romper el health-check de orquestadores como Kubernetes o Docker Compose.
3. **¿Tienes alguna pregunta que te gustaría que sea respondida la siguiente clase?**  
   ¿Cómo se maneja la invalidación sincronizada de caché cuando un producto se actualiza en el backend mientras los navegadores tienen aún vigencia en su `Cache-Control`?
4. **Sobre tu nivel de comprensión de la clase de hoy:**  
   [X] **¡Entendido! - Lo domino y podría explicarlo.**
5. **Pensando en tu participación y esfuerzo en la clase de hoy:**  
   [X] **Muy Comprometido/a: Me esforcé al máximo.**
6. **Mi satisfacción con la clase fue:**  
   **10 / 10**
