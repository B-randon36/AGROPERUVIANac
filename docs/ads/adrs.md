# Decisiones arquitectónicas

Estos registros se reconstruyen a partir de la implementación. **Estado: observadas, pendientes de ratificación por el equipo.** No se inventan fechas de aprobación ni decisiones de reuniones.

## ADR-DOC-01 · Una aplicación backend modular

**Contexto:** ventas necesita catálogo, inventario y finanzas. **Decisión observada:** un ejecutable Spring Boot organiza paquetes de negocio y usa llamadas Java. **Consecuencia:** facilita transacciones locales y despliegue conjunto; exige verificar límites de módulo para evitar dependencias circulares. **Evidencia:** clase principal, paquetes y `ModularityTests`.

## ADR-DOC-02 · Identidad con Keycloak y JWT

**Contexto:** roles y propiedad deben verificarse en el servidor. **Decisión observada:** Resource Server con issuer Keycloak; la SPA usa autorización con PKCE. **Consecuencia:** se delega identidad, pero hay que mantener coherencia de issuer, cliente, callbacks y roles. **Evidencia:** SecurityConfig y archivos de autenticación de la SPA. Existen además rutas de autenticación local de demostración que requieren delimitar su uso.

## ADR-DOC-03 · Persistencia de laboratorio y destino Oracle

**Contexto:** la demostración debe poder arrancar sin administrar Oracle, pero BD2 exige operación empresarial. **Decisión observada:** perfil H2 y scripts/perfil Oracle. **Consecuencia:** las pruebas H2 no acreditan tipos, permisos, secuencias ni recuperación Oracle. **Pendiente:** migración reproducible y evidencias en Oracle.

## ADR-DOC-04 · Ticket por secuencia

**Contexto:** evitar colisiones al registrar ventas. **Decisión observada:** secuencia `BOM_VENTAS.SEQ_TICKET_VENTA` y restricción única. **Consecuencia:** los valores pueden dejar huecos tras rollback; no se debe reconstruir una secuencia para ocultarlos. El ticket interno no se presenta como correlativo tributario autorizado.

## ADR-DOC-05 · Documentación con MkDocs Material

**Contexto:** se necesita una web navegable similar a la guía académica. **Decisión de esta entrega:** Markdown, navegación por áreas, búsqueda y diagramas SVG locales. **Consecuencia:** edición sencilla en VS Code y salida estática; requiere reconstruir la web cuando cambie el contenido. Publicar es un paso posterior que depende de autorización.

## Caché y Redis

La presencia de Redis no se acredita en esta revisión. Antes de redactar un ADR aprobado, medir consultas repetidas y frescura requerida. Cualquier decisión de usarlo o descartarlo debe justificar invalidación, costo operativo y beneficio medido; no asignar ganancias de rendimiento sin prueba.

## Fuentes del proyecto

- [SecurityConfig.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/config/SecurityConfig.java)
- [NumeroTicketService.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/NumeroTicketService.java)
- [pom.xml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/pom.xml)
