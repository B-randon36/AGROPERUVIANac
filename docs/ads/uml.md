# Catálogo UML

## Clases Principales de Ventas

![Relaciones de clases de ventas](../assets/clases.svg)

La composición entre `Venta` y `DetalleVenta` garantiza el ciclo de vida conjunto de la cabecera y sus líneas. El producto, el lote FEFO y la sesión de caja se vinculan por identificador, conservando el precio unitario y descripción vigentes al momento de la operación.

---

## Diagrama de Secuencia · Registro de Venta POS

```mermaid
sequenceDiagram
    autonumber
    actor V as Vendedor (Angular SPA)
    participant S as Spring Security (JWT)
    participant C as VentaController
    participant VS as VentaServiceImpl
    participant T as NumeroTicketService
    participant I as InventarioService (FEFO)
    participant F as FinanzasApiImpl
    participant R as VentaRepository

    V->>S: POST /api/v1/ventas (VentaRequest + Bearer JWT)
    S->>S: Valida firma OIDC y rol VENDEDOR/ADMIN
    S->>C: Petición autenticada
    C->>VS: registrarVenta(request, jwt.sub)
    VS->>R: findByOperacionId(request.operacionId)
    alt Venta ya registrada (Idempotencia RN-VEN-06)
        R-->>VS: Venta existente
        VS-->>C: VentaResponse existente
    else Nueva operación
        VS->>T: generarSiguienteNumero()
        T-->>VS: Ticket único (ej. T001-000015)
        VS->>I: descontarStockFEFO(producto, cantidad, ticket)
        I-->>VS: Lotes asignados por vencimiento próximo
        VS->>VS: Desglosa Base e IGV 18% (BigDecimal)
        VS->>R: save(Venta + DetalleVenta)
        VS->>F: registrarIngresoVenta(venta, sesionCaja)
        F-->>VS: MovimientoCaja o CuentaPorCobrar
        VS-->>C: VentaResponse (201 Created)
    end
    C-->>V: JSON con ticket listo para impresión 80mm
```

---

## Máquina de Estados de la Venta (`EstadoVenta`)

```mermaid
stateDiagram-v2
    [*] --> REGISTRADA: Venta confirmada (POS / Delivery / Web)
    REGISTRADA --> SOLICITADA_ANULACION: Vendedor solicita anulación con motivo
    REGISTRADA --> ANULADA: Admin/Supervisor anula directamente
    SOLICITADA_ANULACION --> ANULADA: Admin/Supervisor aprueba anulación<br/>(Reversión FEFO + Extornos en Caja/CxC)
    SOLICITADA_ANULACION --> REGISTRADA: Admin/Supervisor rechaza solicitud
    ANULADA --> [*]: Estado terminal inmutable (RN-VEN-07)
```

### Reglas de Transición
- **Idempotencia (`RN-VEN-06`):** Si el cliente reenvía un `operacionId` ya procesado (por reconexión o doble clic), el servicio retorna la venta original sin duplicar ticket, stock ni movimientos de caja.
- **Segregación de Roles (`RN-VEN-07` / `RN-VEN-08`):** El vendedor puede solicitar anulación únicamente sobre sus propias ventas; solo un `SUPERVISOR` o `ADMIN` puede aprobar o rechazar la anulación.
- **Reversión Multilote FEFO (`RN-VEN-09`):** Al aprobarse la anulación, el sistema devuelve las cantidades exactamente a los lotes originales de donde salieron en orden de vencimiento, y genera el extorno financiero correspondiente.

---

## Fuentes del Proyecto

- [VentaController.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/main/java/pe/edu/upeu/bomerp/ventas/controller/VentaController.java)
- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
- [EstadoVenta.java](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/src/main/java/pe/edu/upeu/bomerp/ventas/entity/EstadoVenta.java)
