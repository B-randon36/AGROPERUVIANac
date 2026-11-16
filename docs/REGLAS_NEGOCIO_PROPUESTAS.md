# Nuevas reglas de negocio propuestas — AGROPERUVIAN

Estado: especificación propuesta; este documento no modifica el comportamiento del backend.
Base revisada: rama ventas_avance, commit 41bd74a00342e5b28bb793a9bdc7221e3919fc3a.
Complementa RN-VEN-01 a RN-VEN-10 identificadas en el código. Los identificadores nuevos son locales a esta propuesta y deben conciliarse con el catálogo académico si contiene otras reglas.

## RN-VEN-11 — Reintento de una operación con identidad y contenido coherentes

Un operacionId identifica una única venta de un vendedor. Si se recibe nuevamente, solo se devuelve el resultado anterior cuando coinciden el vendedor y los datos comerciales: cliente, método de pago, sesión de caja, plazo de crédito y productos con sus cantidades. Un cambio de contenido se rechaza como conflicto. Un vendedor distinto no obtiene la venta del primero. Dos solicitudes simultáneas no pueden duplicar inventario ni cobros.

Ejemplo: reenviar el cobro original devuelve el mismo ticket; reutilizar su operacionId con una cantidad diferente se rechaza.

Criterios de aceptación:
- Un reintento idéntico conserva un ticket y un cobro.
- Un reintento con datos distintos responde 409 sin modificar stock.
- Un usuario sin acceso a la venta no obtiene sus datos por este mecanismo.
- Solicitudes concurrentes con la misma operación producen una única venta persistida.

Trazabilidad prevista: VentaServiceImpl.crear, VentaRepository.findByOperacionId, Venta.operacionId, VentaSecurityTest y pruebas de concurrencia.
Situación observada: crear() devuelve la coincidencia por operacionId antes de validar vendedor y contenido; se debe ampliar esa comprobación.

## RN-VEN-12 — Motivo obligatorio en la solicitud de anulación

Toda solicitud de anulación debe incluir un motivo no vacío, conservado junto con el actor, la venta y la fecha. Administradores y supervisores deben poder consultar ese motivo antes de decidir. El motivo no puede sustituirse silenciosamente por otra solicitud mientras la primera esté pendiente.

Ejemplo: «Cliente desistió de la compra» permite enviar la solicitud; un campo vacío o con espacios se rechaza.

Criterios de aceptación:
- Un motivo vacío se rechaza con 400 y no cambia el estado.
- Una solicitud válida queda vinculada a su venta y aparece en la consulta autorizada.
- Aprobar o rechazar conserva la solicitud original y registra al responsable de la decisión.

Trazabilidad prevista: VentaController.solicitarAnulacion, VentaServiceImpl.solicitarAnulacion, AuditoriaService y formularios de anulación del frontend.
Situación observada: el motivo se recibe como parámetro opcional; esta propuesta lo convierte en requisito funcional.

## RN-VEN-13 — Secuencia válida del reparto

Un pedido transita de PENDIENTE a EN_RUTA y después a ENTREGADO. Se puede cancelar desde PENDIENTE o EN_RUTA. ENTREGADO y CANCELADO son estados finales. Para pasar a EN_RUTA debe existir un repartidor asignado. Repetir el estado actual es un reintento sin nuevos efectos financieros ni cambios en la fecha original de entrega.

Transiciones permitidas:

| Origen | Destino | Condición |
|---|---|---|
| PENDIENTE | EN_RUTA | Repartidor asignado |
| PENDIENTE | CANCELADO | Registrar actor y motivo |
| EN_RUTA | ENTREGADO | Registrar fecha de entrega una sola vez |
| EN_RUTA | CANCELADO | Registrar actor y motivo |

Ejemplo: pasar directamente de PENDIENTE a ENTREGADO se rechaza como conflicto.

Criterios de aceptación:
- Toda transición no incluida se rechaza sin cambiar el pedido.
- Un pedido sin repartidor no puede salir a ruta.
- Repetir ENTREGADO conserva fecha y cobro original.
- Dos confirmaciones simultáneas de entrega no generan dos cobros.

Trazabilidad prevista: DeliveryServiceImpl.actualizarEstado, asignarRepartidor, EstadoDelivery y DeliveryServiceTest.
Situación observada: existen comprobaciones para estados finales; falta validar explícitamente toda la secuencia y el repartidor.

## RN-VEN-14 — Reparto asociado a una venta válida

Un delivery vinculado debe referirse a una venta existente y no anulada. No puede haber dos repartos activos para la misma venta. El importe comercial se obtiene de la venta; el costo de envío se registra por separado y no cambia silenciosamente el total vendido. Anular una venta con reparto EN_RUTA requiere resolver primero ese reparto; una venta entregada requiere un procedimiento de devolución trazable.

Ejemplo: asociar un pedido a un identificador de venta inexistente se rechaza. Crear un segundo reparto activo para la misma venta también se rechaza.

Criterios de aceptación:
- Una venta inexistente produce 404; una anulada produce 409.
- La exclusividad del reparto activo se mantiene bajo solicitudes concurrentes.
- El frontend no puede reemplazar el importe de una venta vinculada enviando otro montoTotal.
- Los reportes cuentan una sola vez el importe comercial y distinguen los cargos de envío.

Trazabilidad prevista: DeliveryServiceImpl.crearPedido, PedidoDelivery.ventaId, VentaServiceImpl.anular y reporte omnicanal.
Situación observada: ventaId se recibe como identificador; deben incorporarse validación y coordinación entre módulos.

## RN-VEN-15 — Política de crédito única y validada en el servidor

Propuesta de política comercial: plazo de 15 a 60 días inclusive, siguiendo el rango descrito en los comentarios actuales de RN-VEN-10. Una venta a crédito requiere nombre y documento del cliente. Si no se indica plazo, se aplica 30 días. Un plazo explícitamente fuera del rango se rechaza. Las modalidades al contado no generan cuentas por cobrar ni vencimientos.

Este rango es una propuesta de negocio pendiente de ratificación: el formulario actual permite 1 a 180 días y el backend acepta cualquier entero positivo. La implementación debe elegir un único rango aprobado y aplicarlo en ambas capas.

Ejemplo bajo el rango propuesto: 30 días es válido; 0, 14 o 61 días se rechazan.

Criterios de aceptación:
- Sin identificación de cliente no se registra crédito.
- Se comprueban los límites inferior y superior del rango aprobado.
- Un plazo omitido adopta 30; un plazo inválido no se corrige silenciosamente.
- El vencimiento coincide con la fecha de emisión más el plazo registrado.
- El rechazo no deja ventas, movimientos de inventario ni cuentas por cobrar parciales.

Trazabilidad prevista: VentaRequest, VentaServiceImpl.crear, FinanzasApiImpl.registrarVentaACredito, venta-form.ts y pruebas de validación/transacción.

## Aplicación posterior

Implementar las validaciones en el backend, reflejarlas en los formularios y agregar pruebas de aceptación. Las comprobaciones del navegador no sustituyen las validaciones del servidor. Los cambios de esquema requieren migraciones compatibles con H2 y Oracle. Esta propuesta documental no constituye evidencia de implementación o pruebas funcionales aprobadas.
