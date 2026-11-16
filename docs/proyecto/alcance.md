# Problema, solución y alcance

## Problema declarado

El brief describe una operación que dependía de hojas de cálculo y notas físicas para controlar ventas, turnos de caja, abastecimiento y productos con vencimiento. Separar esos registros dificulta seguir el origen de una salida, conciliar un cobro y conocer qué lote debe despacharse primero.

No se dispone aquí de mediciones que cuantifiquen pérdidas, tiempos ahorrados o reducción de mermas. Esos resultados requieren una línea base y evidencias de uso.

## Solución implementada en el repositorio

Un ERP con una SPA Angular y un backend Spring Boot organiza los procesos comerciales y productivos. Las ventas conservan cabecera y detalles; solicitan descuento de stock, despacho por lotes y registro financiero. Las anulaciones conservan el historial y solicitan reversiones.

| Proceso | Función | Fuente principal |
|---|---|---|
| Comercialización | Registrar ventas, tickets y solicitudes de anulación | `ventas` |
| Caja y finanzas | Aperturas, movimientos, cierres, cuentas por cobrar | `finanzas` |
| Inventario | Lotes, vencimientos, FEFO y Kardex | `inventario` |
| Abastecimiento | Proveedores, compras y pagos | `compras` |
| Fabricación | Recetas, órdenes y consumo de insumos | `produccion` |
| Maestros e identidad | Productos, categorías, terceros y usuarios | `catalogo`, `terceros`, `seguridad` |

## Límites

El brief deja fuera la contabilidad general formal, una tienda B2C y la emisión electrónica activa con firma SUNAT/PSE. Un ticket POS no acredita una factura electrónica aceptada por SUNAT. La consulta de DNI/RUC es una integración de consulta de datos, no una homologación tributaria.

El brief excluye reportería ejecutiva avanzada, aunque el código actual contiene reportes financieros y omnicanal. En esta documentación se reconocen como funciones presentes cuya alineación con el alcance académico debe ratificar el equipo. Tampoco se atribuye un canal feria implementado solo porque figure en el brief.

## Actores

Vendedor, cajero, supervisor y administrador operan procesos según sus roles. El cliente es un actor del negocio; el alcance revisado no demuestra un portal público de compra para clientes.

## Fuentes del proyecto

- [brief_de_proyecto_integrador_erp_agro_peruvian_foods.md](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/brief_de_proyecto_integrador_erp_agro_peruvian_foods.md)
- [INFORME_FUNCIONALIDADES_ERP_AGROPERUVIAN.md](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/INFORME_FUNCIONALIDADES_ERP_AGROPERUVIAN.md)
