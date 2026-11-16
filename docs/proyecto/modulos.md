# Módulos y funcionalidades

| Módulo | Entidades y procesos | Interacción principal | Estado documental |
|---|---|---|---|
| Catálogo | Producto y categoría | Ofrece precio y existencias a ventas | Presente en código |
| Ventas | Venta, detalle, ticket y auditoría | Coordina catálogo, inventario y finanzas | Presente; validar escenarios pendientes |
| Delivery | Pedido y estados de reparto | Puede vincularse por ventaId o cobrar al entregar | Presente; revisar reglas de transición |
| Inventario | Lote, almacén y Kardex | Despacho FEFO y reversión por ticket | Presente; probar distribución multi-lote |
| Finanzas | Sesión, movimiento, crédito y rendición | Cobros, extornos, cierres y exportación | Presente; revisar propiedad de sesión |
| Compras | Proveedor, compra, detalle y pago | Abastecimiento y compromisos de pago | Presente; adjuntar recorrido completo |
| Producción | Orden, receta y consumos | Consumo de insumos y generación de lotes | Presente; adjuntar integración con inventario |
| Terceros | Datos de personas y organizaciones | Soporte a procesos comerciales | Presente en código |
| Seguridad | Usuarios, roles y autenticación | Control de identidad y permisos | Keycloak y componentes locales observados |

## Funcionalidad central y ampliaciones

El núcleo de la demostración es un flujo verificable entre venta, inventario y caja. Los reportes, consulta externa de documentos, escáner y mapas deben presentarse según su implementación y dependencias. No se declaran obligatorios por el solo hecho de aparecer en la interfaz.

## Fronteras y responsabilidad

Una pantalla no define por sí sola el límite de un módulo. Usar propietarios de datos, contratos de servicio y pruebas de dependencia para justificar la división. La revisión de Spring Modulith debe mostrar la salida real de `ModularityTests`.

## Fuentes del proyecto

- [brief_de_proyecto_integrador_erp_agro_peruvian_foods.md](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/brief_de_proyecto_integrador_erp_agro_peruvian_foods.md)
- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
