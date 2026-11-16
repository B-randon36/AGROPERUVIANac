# Reglas de negocio

Esta tabla conserva RN-VEN-01 a RN-VEN-10 tal como se identifican en comentarios del código. **Presente** significa que se encontró mecanismo de aplicación; no certifica todos los escenarios ni su ejecución en Oracle.

| Regla | Comportamiento observado | Evidencia | Pendiente |
|---|---|---|---|
| RN-VEN-01 | Verificar y descontar stock; reponer al anular | Servicio de producto y ventas | Concurrencia e integración bajo fallos |
| RN-VEN-02 | Desglosar precio final en base e impuesto al 18 % | BigDecimal, división por 1.18 | Tratamiento de productos con otra afectación tributaria |
| RN-VEN-03 | Seleccionar lotes vigentes por vencimiento | InventarioServiceImpl | Prueba de bloqueo sanitario y saldo sin lote |
| RN-VEN-04 | Ticket único por secuencia | NumeroTicketService y UNIQUE | Ejecución de secuencia y permisos Oracle |
| RN-VEN-05 | Vendedor obtenido del sujeto autenticado | VentaController | Comprobar toda ruta de entrada |
| RN-VEN-06 | Reutilizar venta por operacionId | findByOperacionId | Comparar propietario y contenido; carrera concurrente |
| RN-VEN-07 | Gestor aprueba/rechaza y se evita doble anulación | SecurityConfig, bloqueo de venta | Prueba simultánea de anulación |
| RN-VEN-08 | Restringir vendedor a sus operaciones | Filtros y comprobación de propiedad | Revisar acceso por reintento |
| RN-VEN-09 | Revertir salidas por ticket y lote original | revertirSalidaVentaPorTicket | Prueba con varios lotes y productos repetidos |
| RN-VEN-10 | Integrar venta al contado o crédito con finanzas | FinanzasApi | Propiedad de caja y política coherente de crédito |

## Ejemplo numérico

Con dos unidades a S/ 59, el total es S/ 118. Bajo la hipótesis de precio gravado incluido, la base es S/ 100 y el impuesto S/ 18. Esta fórmula describe el código; no reemplaza una validación tributaria por producto.

## Diferencias por resolver

El comentario de crédito menciona 15 a 60 días, el formulario admite 1 a 180 y el backend adopta 30 por defecto ante ciertos valores. El equipo debe aprobar un rango y aplicarlo de manera coherente. El motivo de solicitud se describe como justificado pero sigue admitiendo ausencia en la entrada.

## Propuestas adicionales

Comparar identidad y contenido en reintentos, exigir motivo de anulación, validar secuencia logística y comprobar venta vinculada en delivery son mejoras propuestas. No se incorporan como reglas ya implementadas ni se atribuye aprobación del rango de crédito al negocio.

## Fuentes del proyecto

- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
- [VentaRepository.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/repository/VentaRepository.java)
- [FinanzasApiImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/api/FinanzasApiImpl.java)
- [venta-form.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/app/features/ventas/venta-form.ts)
