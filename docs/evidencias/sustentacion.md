# Guía de sustentación

## Presentación del problema

Explicar el control de lotes, ventas y caja que el brief busca integrar. Presentar un caso de compra y venta concreto sin inventar cifras de ahorro o resultados comerciales.

## Recorrido de demostración

| Paso | Acción | Evidencia que se debe mostrar |
|---|---|---|
| 1 | Iniciar sesión como vendedor | Identidad y menú de rol |
| 2 | Revisar producto y lote | Precio, stock y vencimiento |
| 3 | Abrir o seleccionar caja | Sesión y responsable |
| 4 | Registrar venta | Cabecera, detalles y ticket |
| 5 | Consultar datos | Venta, Kardex y referencia financiera |
| 6 | Intentar acceso con otro vendedor | Rechazo por propiedad |
| 7 | Solicitar y resolver anulación | Actor, estado y reversiones |
| 8 | Cerrar caja | Arqueo y reporte |
| 9 | Mostrar operación Oracle | Base usada y evidencia BD2 |

Ensayar la demo con datos preparados y roles autorizados. No depender de una consulta externa de DNI/RUC para demostrar el flujo central si el proveedor no está disponible.

## Preguntas técnicas

- ¿Dónde comienza y termina la transacción de venta?
- ¿Qué contiene el agregado y qué referencias quedan fuera?
- ¿Cómo se comprueba que el vendedor no accede a otra venta?
- ¿Cómo se restaura una salida que consumió varios lotes?
- ¿Qué diferencia hay entre probar H2 y probar Oracle?
- ¿Qué sucede si dos solicitudes usan el mismo operacionId?
- ¿Cómo se demuestra que un respaldo puede restaurarse?

Responder señalando archivo, método, objeto de base y caso de prueba. Registrar pendientes de forma explícita cuando la evidencia no exista.
