# Uso del sistema

## Flujo de venta en mostrador

1. Autenticarse con una cuenta de vendedor o gestor.
2. Comprobar productos y existencias. En efectivo se requiere una caja abierta según la integración financiera.
3. Agregar productos al formulario POS, revisar cantidades y cliente.
4. Seleccionar un método de pago. La pantalla ofrece efectivo, tarjeta, billeteras, transferencia y crédito según su modelo.
5. Enviar una sola operación y esperar la confirmación del backend.
6. Revisar ticket y desglose. Si el precio final es S/ 118, bajo el cálculo actual al 18 % la base es S/ 100 y el impuesto S/ 18.
7. Consultar la venta y comprobar que el vendedor puede acceder a sus propios registros.

La impresión desde el navegador y el diseño térmico del ticket no demuestran una integración certificada con una impresora ESC/POS ni emisión electrónica SUNAT.

## Solicitud y resolución de anulación

El vendedor solicita la anulación de su venta; supervisor o administrador la aprueba o rechaza. Una aprobación solicita restauración de stock, reversión de salidas de Kardex y extorno financiero. El rechazo devuelve la venta a REGISTRADA.

Comprobar siempre los efectos en las tres áreas. Que la pantalla muestre ANULADA no constituye evidencia suficiente de conciliación de inventario y caja.

## Repartos

El módulo permite crear pedidos, asignar repartidor y modificar su estado. No se presupone que toda la secuencia logística esté validada: las restricciones de transición deben contrastarse con `DeliveryServiceImpl`.

## Compras y producción

Registrar proveedores e insumos antes de una compra. Preparar receta y materiales antes de completar una orden productiva. Para demostrar integración, conservar la referencia de compra, la orden, el lote generado y la venta que lo consume.

## Datos de demostración

Los inicializadores y `data.sql` incorporan datos semilla. Los reportes pueden reflejar esos registros; no equivalen a ventas reales de la empresa. No mezclar resultados de demostración con indicadores comerciales de producción.

## Fuentes del proyecto

- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
- [venta-form.ts](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/bomerp-frontend/src/app/features/ventas/venta-form.ts)
- [VentaDataInitializer.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/config/VentaDataInitializer.java)
