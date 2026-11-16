# Matriz de trazabilidad ADS–BD2–LP2

Las filas conectan una necesidad con diseño, persistencia, API y pantalla. La columna de prueba identifica artefactos o casos por ejecutar; no es una aprobación automática.

| Necesidad | ADS | Datos / BD2 | LP2 / API | Pantalla | Verificación |
|---|---|---|---|---|---|
| Registrar venta | Agregado Venta–Detalle | VENTAS y DETALLE_VENTAS | POST /api/v1/ventas | venta-form | VentaServiceTest / E2E |
| Consultar operación propia | Propiedad por vendedor | VENDEDOR_ID | GET /api/v1/ventas/{id} | venta-list | VentaSecurityTest |
| Evitar ticket repetido | Secuencia y unicidad | SEQ_TICKET_VENTA / NUMERO_TICKET | NumeroTicketService | Ticket generado | VentaPersistenciaTest y Oracle pendiente |
| Despachar por vencimiento | FEFO e invariantes sanitarias | Lote y Kardex | InventarioService | lote-list / POS | Integración multi-lote pendiente |
| Anular con control | Estados y segregación | Estado, auditoría y movimientos | PATCH /ventas/{id}/anular | venta-reporte | E2E y concurrencia pendiente |
| Cobrar en sesión | Límite Ventas–Finanzas | SESION_CAJA_ID / movimientos | FinanzasApi | POS / sesión de caja | Dos cajeros pendiente |
| Generar compra | Cabecera y detalles | Compra / DetalleCompra | Controlador de compras | compra-form | CompraServiceTest |
| Completar producción | Orden y consumos | Orden / insumos / lote | ProduccionService | orden-form | ProduccionServiceTest |
| Recuperar operación | Disponibilidad y continuidad | Backup y recuperación Oracle | Retomar servicio y consultar venta | Consulta de operación | Ejercicio BD2 pendiente |

Las rutas abreviadas `/ventas/...` se refieren al prefijo `/api/v1/ventas`. El [catálogo de API](../lp2/api.md) lista las anotaciones HTTP detectadas.

## Cómo cerrar una fila

Vincular el requisito académico, el archivo de diseño y la salida real de SQL o prueba. Para una regla modificada, conservar el caso que fallaba y el resultado posterior. Una entidad JPA no sustituye un objeto Oracle instalado y comprobado.
