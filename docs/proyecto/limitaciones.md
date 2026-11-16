# Limitaciones y mejoras priorizadas

## Antes de presentar el cierre técnico

1. Ejecutar la batería actual con el commit documentado y conservar resultados.
2. Demostrar instalación y flujo sobre Oracle, incluidos permisos y secuencia.
3. Ratificar que Java y PL/SQL no duplican descuentos de inventario.
4. Verificar idempotencia por usuario y contenido, además de concurrencia.
5. Probar propiedad de sesión de caja y comportamiento de devolución con sesión cerrada.
6. Ensayar anulación multi-lote y productos repetidos en el detalle.

## Antes de operar con datos reales

Sustituir perfiles destructivos, preparar migraciones, restringir servicios de demostración, configurar secretos por ambiente y demostrar recuperación. La documentación no transforma el entorno de laboratorio en una instalación productiva.

## Consistencia documental

- El README menciona una versión Angular distinta a package.json.
- El brief excluye reportería avanzada pero aparecen reportes en el código.
- La política de días de crédito difiere entre comentarios, servidor y formulario.
- La configuración del entorno frontend requiere comprobar qué archivo utiliza el build.
- Los ADR observados requieren aprobación del equipo.

## Qué no se afirma

No se acreditan homologación tributaria SUNAT, mediciones de ahorro, SLA, despliegue público del ERP, recuperación Oracle satisfactoria ni todas las pruebas aprobadas. No se han creado capturas ficticias de la aplicación.
