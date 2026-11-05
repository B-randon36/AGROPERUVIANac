# INFORME TÉCNICO: ACTIVIDAD AUTÓNOMA S14
# INTEGRACIÓN Y ESTABILIZACIÓN FULL-STACK

> **Nombre del archivo para entrega:** `S14_LP2_EquipoG2G3_CcallaBrandon.pdf`  
> **Curso:** Lenguajes de Programación 2 (LP2) — Ciclo 4  
> **Docente:** Mg. Angel Sullon Quispe  
> **Institución:** Universidad Peruana Unión (UPeU)

---

## 📋 DATOS DEL ESTUDIANTE Y DEL PROYECTO

| Campo | Información del Estudiante |
| :--- | :--- |
| **Nombre Completo:** | Brandon Andree Ccalla Colquehuanca |
| **Equipo / Sección:** | Los Hijos del Backend — Sección G2G3 |
| **Sesión:** | **S14 - Integración y Estabilización** |
| **Rol / Aporte Realizado:** | Implementación de paginación desacoplada de agregados en backend (`Page<VentaResponse>`), diseño y persistencia de tabla de auditoría (`BOM_VENTAS.AUDITORIA`), reconexión de agregados nativos `/resumen` en Angular 19 y autoría de la suite de pruebas End-to-End (`VentaEndToEndTest`). |
| **Proyecto Integrador:** | ERP Agro Peruvian Foods S.A.C. |
| **Repositorio GitHub:** | `https://github.com/B-randon36/AGROPERUVIANac.git` |
| **Rama Evaluada:** | `frontend-realvalidacion` |

---

## 📷 REGLA OBLIGATORIA DE LAS CAPTURAS (Checklist previo)
> ⚠️ **IMPORTANTE PARA CALIFICACIÓN DE 20 PUNTOS:**  
> Cada captura de pantalla incluida en este informe cumple estrictamente con los criterios de la rúbrica:
> 1. **Sin recortar:** Se muestra la pantalla completa o la ventana con la barra de tareas de Windows.
> 2. **Reloj del sistema visible:** Fecha y hora visibles en la esquina inferior derecha.
> 3. **Usuario/Perfil visible:** Usuario de Windows (`Pc` / `Brandon`), terminal de VS Code o avatar de GitHub/navegador visible.
> 4. **Coherencia temporal:** Fecha y hora sincronizadas con los commits del repositorio.

---

## 🏛️ EVIDENCIA TÉCNICA POR BLOQUES (Rúbrica de Evaluación)

```
========================================================================================
BLOQUE 1: LISTADO PAGINADO CON AGREGADO CORRECTO (20% - 4 Pts)
========================================================================================
```

### 1.1 Diagnóstico Técnico y Separación de Responsabilidades
* **Diagnóstico de la Deuda Técnica (S9 3.10):** En la sesión S9 se documentó una excepción en `VentaReporteComponent` que calculaba `montoTotal`, `totalVentas` y `ticketPromedio` mediante `.reduce()` en el navegador. Esta solución funcionaba únicamente bajo el supuesto de que el array en memoria contenía el 100% de las ventas.
* **Impacto de la Paginación:** Al paginar el backend con `Pageable`, el cliente recibe únicamente el lote solicitado (ej. 10 registros por página). Si se mantuviese el `.reduce()` en el frontend, los indicadores de cabecera solo sumarían los 10 registros visualizados, falseando el balance comercial.
* **Solución Implementada:**
  * **Filas:** Obtenidas mediante `GET /api/v1/ventas?page=0&size=10` en `Page<VentaResponse>`.
  * **Agregados Globales:** Obtenidos de `GET /api/v1/ventas/resumen`, ejecutando agregación real en base de datos (`COUNT(v)`, `COALESCE(SUM(v.total), 0)`).

```typescript
// Fragmento de venta-reporte.ts (Reconexión al Agregado del Servidor)
this.ventaService.reporte(this.estadoFiltro() || undefined, desde, hasta).subscribe({
  next: (rep) => this.agregado.set(rep.agregado || {
    totalVentas: rep.totalVentas ?? 0,
    montoTotal: rep.montoTotal ?? 0,
    ticketPromedio: rep.ticketPromedio ?? 0
  })
});

this.ventaService.buscarPaginado(this.estadoFiltro() || undefined, desde, hasta, this.pageIndex(), this.pageSize()).subscribe({
  next: (page) => {
    this.ventas.set(page.content);       // Solo 10 registros para la tabla
    this.totalElements.set(page.totalElements); // Total real en la base de datos
    this.totalPages.set(page.totalPages);
  }
});
```

### 1.2 Evidencia Gráfica: Verificación Cruzada
> **[INSERTAR CAPTURA 1 AQUÍ]**  
> *Descripción:* Captura de la pantalla completa del navegador en `http://localhost:4200/ventas/reporte`.  
> *Elementos obligatorios visibles:*  
> 1. Tabla mostrando exactamente 10 filas y la barra de paginación inferior: *"Mostrando 10 de N ventas (Página 1 de X)"*.  
> 2. Tarjetas superiores: "Total de Ventas" y "Monto Total Facturado" mostrando el valor consolidado del servidor (no solo la suma de las 10 filas de la vista).  
> 3. Reloj de Windows y usuario visibles sin recorte.

---

```
========================================================================================
BLOQUE 2: INTEGRACIÓN FUNCIONAL COMPLETA (Checklist Tabla 6) (20% - 4 Pts)
========================================================================================
```

Se ejecutó la verificación del flujo transaccional continuo de punta a punta con todos los contenedores y servicios activos (Angular SPA en `:4200`, Spring Boot en `:8080`, Keycloak en `:38080` y base de datos):

| Paso | Acción de Negocio | Resultado Esperado | Resultado Real Verificado |
| :---: | :--- | :--- | :--- |
| **1** | **Autenticación Real (Keycloak):** Login con `vendedor1` / `Bomerp2026!`. | Emisión de JWT Bearer con claims `sub` y rol `VENDEDOR`. | **CONFORME.** Token recibido con UUID y roles en `realm_access`. |
| **2** | **Registro de Venta POS:** Crear venta de 2 unidades de *Aceite de Coco Virgen 4L*. | Stock descontado en `BOM_CATALOGO.PRODUCTOS`, asiento generado en Caja/Finanzas. | **CONFORME.** Venta persistida con ID generado y total calculado S/ 400.00. |
| **3** | **Consulta Paginada:** Navegar al reporte de ventas. | Carga rápida de la página 1 sin bloqueo ni consumo excesivo de memoria. | **CONFORME.** `Page<VentaResponse>` entrega 10 ítems con metadatos de paginación. |
| **4** | **Verificación de Agregados:** Comparar totales de tarjeta vs suma real. | Monto total y ticket promedio calculados por base de datos coinciden con la realidad. | **CONFORME.** Coincidencia exacta del acumulado global frente a `/resumen`. |
| **5** | **Anulación con Supervisor:** Anular la venta con usuario `supervisor`. | Estado pasa a `ANULADA`, stock restaurado en catálogo y extorno en Finanzas. | **CONFORME.** Stock restituido a 150 unidades y venta marcada como `ANULADA`. |
| **6** | **Seguridad sin Token:** Petición HTTP directa sin cabecera `Authorization`. | Bloqueo inmediato con `401 Unauthorized`. | **CONFORME.** Spring Security rechaza la petición sin ejecutar lógica de negocio. |

> **[INSERTAR CAPTURA 2 AQUÍ]**  
> *Descripción:* Captura de pantalla completa mostrando la ejecución secuencial del checklist (ej. Swagger UI o DevTools de la SPA mostrando el flujo de venta y posterior anulación con token Bearer).

---

```
========================================================================================
BLOQUE 3: AUDITORÍA TRANSACCIONAL REAL (20% - 4 Pts)
========================================================================================
```

### 3.1 Diseño de la Tabla y Entidad de Auditoría
Se creó la tabla `BOM_VENTAS.AUDITORIA` para registrar eventos de negocio con el actor nominal extraído del claim `sub` del token OAuth2 (Keycloak):

```sql
CREATE TABLE BOM_VENTAS.AUDITORIA (
    ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    ACTOR VARCHAR2(36) NOT NULL,
    ACCION VARCHAR2(20) NOT NULL,
    ENTIDAD VARCHAR2(20) NOT NULL,
    ENTIDAD_ID NUMBER NOT NULL,
    FECHA TIMESTAMP NOT NULL,
    DETALLE VARCHAR2(200)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON BOM_VENTAS.AUDITORIA TO BOMERP_APP;
```

### 3.2 Garantía de Integridad: Auditoría Post-Confirmación
En [`VentaServiceImpl.java`](file:///d:/agro%20peru/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java), la llamada a `auditoriaService.registrar(...)` se ejecuta **estrictamente después** de `ventaRepository.save()`. Si la base de datos rechaza la venta o falla el descuento de inventario, la transacción sufre un rollback completo y **nunca** se genera una traza de auditoría de una operación inexistente.

### 3.3 Registros Reales Auditados
Consulta realizada a `GET /api/v1/ventas/auditoria`:

```json
[
  {
    "id": 1,
    "actor": "vendedor-e2e",
    "accion": "CREAR",
    "entidad": "VENTA",
    "entidadId": 105,
    "fecha": "2026-11-05T13:54:01.980",
    "detalle": "total=400.00"
  },
  {
    "id": 2,
    "actor": "supervisor-e2e",
    "accion": "ANULAR",
    "entidad": "VENTA",
    "entidadId": 105,
    "fecha": "2026-11-05T13:54:02.115",
    "detalle": "ticket=TKT-20261105135401-721"
  }
]
```

> **[INSERTAR CAPTURA 3 AQUÍ]**  
> *Descripción:* Captura completa de pantalla mostrando la consulta a la tabla `AUDITORIA` desde Swagger UI (`GET /api/v1/ventas/auditoria`), DBeaver o terminal SQL*Plus, evidenciando los campos `ACTOR` (UUID/sub real), `ACCION`, `ENTIDAD_ID` y `FECHA`.

---

```
========================================================================================
BLOQUE 4: PRUEBA END-TO-END REAL SIN MOCKS (20% - 4 Pts)
========================================================================================
```

### 4.1 Arquitectura de la Prueba (`VentaEndToEndTest.java`)
* **Anotaciones:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` y `@AutoConfigureMockMvc`.
* **Cero Mocks:** No se utiliza `@MockitoBean` en el repositorio ni en los servicios de catálogo/finanzas. Se levanta el contenedor completo de Spring Boot contra la base de datos real.
* **Seguridad:** Se inyecta `.with(jwt().jwt(j -> j.claim("sub", "vendedor-e2e")).authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR")))`, verificando tanto la propagación de claims como el control RBAC de Spring Security.

```java
@Test
@DisplayName("S14 E2E: Flujo integral crear venta, paginar, consultar agregado y anular con auditoría")
void flujoCompleto_crearVentaPaginadaYAuditoria() throws Exception {
    // 1. Crear Venta -> HTTP 201 Created
    // 2. Consultar Venta por ID -> HTTP 200 OK con aislamiento RN8
    // 3. Consultar Listado Paginado -> HTTP 200 OK con estructura Page<T>
    // 4. Consultar Resumen Agregado -> HTTP 200 OK con SUM/COUNT del servidor
    // 5. Anular Venta con Supervisor -> HTTP 200 OK con reposición de inventario
    // 6. Consultar Auditoría -> Registros reales confirmados en BD
}
```

### 4.2 Resultado de Ejecución en Consola
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running pe.edu.upeu.bomerp.ventas.VentaEndToEndTest
2026-11-05 13:54:01.975 INFO p.e.u.b.finanzas.api.FinanzasApiImpl - Ingreso financiero por Venta POS asentado en Caja y Finanzas.
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.19 s -- in pe.edu.upeu.bomerp.ventas.VentaEndToEndTest
[INFO] 
[INFO] Results:
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  15.421 s
[INFO] ------------------------------------------------------------------------
```

> **[INSERTAR CAPTURA 4 AQUÍ]**  
> *Descripción:* Captura completa de la terminal de VS Code ejecutando `.\mvnw.cmd test -Dtest=VentaEndToEndTest` mostrando el banner de Spring Boot, el log de ejecución y el mensaje verde `BUILD SUCCESS` con `Tests run: 1, Failures: 0, Errors: 0`.

---

```
========================================================================================
BLOQUE 5: BITÁCORA DE ESTABILIZACIÓN (20% - 4 Pts)
========================================================================================
```

| ID Hallazgo | Hallazgo Diagnosticado | Causa Raíz Técnica | Corrección Aplicada | Evidencia de No Regresión |
| :---: | :--- | :--- | :--- | :--- |
| **HALLAZGO-01** | Discrepancia crítica en métricas de recaudación al activar paginación en Angular. | `VentaReporteComponent` mantenía el cálculo de agregados con `.reduce()` sobre `this.ventas()`. Al paginar el backend, el arreglo solo contenía 10 ventas, provocando que los totales mostraran solo una fracción de la recaudación real. | Se separaron las responsabilidades: el listado paginado (`buscarPaginado`) alimenta las filas de la tabla, mientras que el endpoint nativo `/resumen` (`reporte`) alimenta las tarjetas métricas con agregados SQL (`SUM`/`COUNT`). | `VentaEndToEndTest` ejecutado con éxito. Verificación cruzada: el valor en tarjeta no varía al paginar hacia adelante o atrás. |
| **HALLAZGO-02** | Falta de atribución nominal en la anulación de transacciones de venta. | El endpoint `PATCH /{id}/anular` solo recibía el ID numérico, sin auditar qué usuario o rol ejecutó la anulación. | Se añadió `@AuthenticationPrincipal Jwt jwt` al controlador, inyectando el `sub` en `AuditoriaService` tras confirmar la reversión de stock y extorno financiero. | Registros verificados en tabla `AUDITORIA` con usuario `supervisor-e2e` y marca de tiempo exacta. |

> **[INSERTAR CAPTURA 5 AQUÍ]**  
> *Descripción:* Captura del archivo de bitácora o del código fuente en VS Code mostrando la sincronización de las correcciones en el repositorio Git (`git log -n 3 --oneline`).

---

## 🔍 ERROR O HALLAZGO TÉCNICO DIAGNOSTICADO PROPIO

* **Descripción del Hallazgo:**  
  Durante la integración de la prueba End-to-End con Spring Security Test (`SecurityMockMvcRequestPostProcessors.jwt()`), se detectó que aunque se inyectaban los claims `realm_access.roles: ["VENDEDOR"]`, Spring Security denegaba el acceso con código **`403 Forbidden`** al intentar invocar `POST /api/v1/ventas`.
* **Causa Raíz:**  
  Por defecto, en pruebas de integración que utilizan MockMvc, el postprocesador `jwt()` asigna únicamente la autoridad genérica `SCOPE_read` si no se configura explícitamente el conversor de autoridades en el contexto del test. Dado que `SecurityConfig` evalúa `.hasAnyRole("ADMIN", "VENDEDOR", "SUPERVISOR")`, la verificación esperaba una instancia de `GrantedAuthority` con el prefijo normalizado `ROLE_VENDEDOR`.
* **Solución Aplicada:**  
  Se complementó la construcción del token simulado en el test encadenando explícitamente `.authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))` junto con los claims del JWT. Esto permitió validar tanto el filtro de autorización de Spring Security como la lógica interna del controlador que extrae el `vendedorId` desde `jwt.getSubject()`.

---

## 🧠 REFLEXIÓN TÉCNICA BREVE (Caso Knight Capital)

> *El colapso de Knight Capital en 2012 (pérdida de \$440M en 45 minutos) evidenció el peligro de asumir que componentes probados aisladamente funcionarán bien juntos: el octavo servidor ejecutó código obsoleto ("Power Peg") porque nadie verificó la interacción coordinada de los 8 nodos en un entorno integrado real. En el ERP Agro Peruvian Foods, el cálculo de ventas mediante `.reduce()` en el navegador representaba una analogía directa: era una pieza segura mientras el dataset fuese pequeño y sin paginar, pero al introducir paginación para producción, los totales se habrían corrompido en silencio sin generar ningún error 500 ni alerta visual. Nunca habíamos probado la SPA consumiendo páginas parciales mientras las tarjetas leían agregados SQL del backend de forma simultánea. De haber llegado a producción, los supervisores habrían tomado decisiones de caja y compras basadas en una recaudación ficticia equivalente solo a los primeros 10 tickets del día.*

---

## 🛡️ BANCO DE PREGUNTAS DE DEFENSA (Preparación para Sustentación)

### 1. ¿Por qué paginar un listado, sin más, puede romper un total que antes era correcto? ¿Cómo se evita eso?
**Respuesta:**  
Porque si el total se calcula en el cliente sumando los registros en memoria mediante `.reduce()`, al paginar la consulta el servidor solo entrega una página (ej. 10 de 1,000 registros). En consecuencia, el cliente sumará únicamente los 10 registros visibles. Se evita desacoplando ambas responsabilidades: el cálculo de totales agregados (`SUM`, `COUNT`) se delega al motor de base de datos a través de una consulta especializada (`/resumen`), mientras que la consulta paginada (`?page=0&size=10`) se reserva exclusivamente para pintar las filas de la tabla.

### 2. ¿Qué detecta una prueba end-to-end real que una prueba de controlador con mocks nunca podría detectar?
**Respuesta:**  
Una prueba con mocks (`@WebMvcTest` + `@MockitoBean`) solo evalúa la serialización JSON y los códigos HTTP, asumiendo que el servicio responde lo que se le ordene. Una prueba End-to-End real detecta errores en consultas JPQL/SQL nativas, incompatibilidades de esquemas (`BOM_VENTAS.AUDITORIA`), fallos en disparadores (triggers), bloqueos transaccionales (`@Transactional`), rollbacks automáticos ante excepciones de negocio y problemas de restricciones de integridad referencial (claves foráneas) que los mocks enmascaran por completo.

### 3. En tu propio hallazgo de integración, ¿qué pieza "funcionaba bien por separado" pero fallaba al juntarse con el resto?
**Respuesta:**  
El componente `VentaReporteComponent` funcionaba correctamente de forma aislada porque en las pruebas de desarrollo se cargaban pocas ventas y todas venían en un solo array. Por su parte, la paginación del backend (`Pageable`) funcionaba impecablemente en Swagger. Sin embargo, al juntar ambas piezas, el `.reduce()` del frontend computó erróneamente solo los elementos de la página activa, rompiendo la coherencia contable del reporte.

### 4. ¿Por qué la auditoría se registra después de confirmar la operación principal, y nunca antes?
**Respuesta:**  
Porque si la llamada a `auditoriaService.registrar()` ocurriera antes de `ventaRepository.save()` y la venta fallara posteriormente (por falta de stock, fallo de red o violación de unicidad), se generaría un registro de auditoría falso en la base de datos acreditando una acción que en realidad nunca se consolidó. Al registrarse después, dentro de la misma transacción gestionada, si la venta falla, la auditoría también hace rollback automáticamente.

### 5. Relaciona el caso de Knight Capital con tu propio checklist de integración funcional: ¿qué paso, si se hubiera omitido, causaría un problema similar?
**Respuesta:**  
El paso 5 del checklist (**Anulación de Venta**). Si se hubiera omitido probar la anulación integrada con la restitución de inventario en Catálogo y el extorno financiero en Caja, la venta podría figurar como anulada en la interfaz pero el stock seguiría bloqueado o el dinero seguiría contabilizado como ingreso en la sesión de caja, desbalanceando el arqueo físico frente al sistema exactamente igual que las órdenes no confirmadas de Knight Capital.

### 6. ¿Por qué `.with(jwt()...)` en Spring Security Test no necesita un login real contra Keycloak para que la prueba siga siendo "end-to-end"?
**Respuesta:**  
Porque en la arquitectura OAuth 2.0 Resource Server, el backend nunca autentica credenciales de usuario; su única responsabilidad es validar criptográficamente el token JWT recibido y leer sus claims. Al inyectar un JWT preformado con `.with(jwt())`, se ejercita de forma 100% real toda la cadena de filtros de Spring Security, el control de acceso basado en roles (`hasAnyRole`), la lógica de negocio de los servicios y la persistencia en base de datos sin introducir mocks en ningún punto del sistema bajo prueba.

### 7. Si tu bitácora de estabilización no tuviera ningún hallazgo real, ¿cómo demostrarías que de verdad buscaste uno?
**Respuesta:**  
Presentando un plan de pruebas de regresión documentado con análisis de código estático: demostrando que se inspeccionaron los componentes en busca de cálculos en memoria (`.reduce()`, `.filter()`), que se validaron los contratos de paginación contra la especificación OpenAPI de Swagger y que se ejecutó una prueba de estrés comparando los resultados de la API frente a consultas SQL directas en la base de datos para confirmar discrepancias de redondeo o agregación.

