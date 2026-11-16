# Patrones aplicados

| Patrón o práctica | Ejemplo en el código | Propósito |
|---|---|---|
| Controller | `VentaController` | Recibir HTTP, validar entrada y construir respuesta |
| Service | `VentaServiceImpl` | Coordinar el caso de uso comercial |
| Repository | `VentaRepository` | Encapsular consultas y persistencia JPA |
| DTO | `VentaRequest`, `VentaResponse` | Separar contratos HTTP de entidades persistentes |
| Mapper | `FinanzasMapper` y su implementación | Transformar estructuras de finanzas |
| Strategy | `CierreCajaExporter` con PDF/Excel | Seleccionar una exportación por formato |
| Inyección de dependencias | Constructores y `@RequiredArgsConstructor` | Suministrar colaboradores y facilitar sustitución en pruebas |
| Fachada de integración interna | `FinanzasApi` | Ofrecer operaciones financieras al resto del backend |

En ventas, la conversión se realiza en `toResponse`; no hay que atribuirle un mapper independiente que no existe. El uso de `.builder()` procede de Lombok y no prueba que el equipo haya diseñado un Builder GoF manual.

## SOLID en la defensa

Explicar una responsabilidad concreta por clase y mostrar qué cambio puede realizarse sin modificar consumidores. Para Strategy, demostrar PDF y Excel usando el contrato común. Para Service/Repository, explicar por qué el controlador no ejecuta directamente las consultas de stock.

## Revisión pendiente

La complejidad de `VentaServiceImpl` y la coordinación de varios módulos justifican revisar responsabilidades. Una interfaz no garantiza automáticamente bajo acoplamiento; revisar imports, acceso a repositorios ajenos y pruebas de módulos.

## Fuentes del proyecto

- [FinanzasMapper.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/mapper/FinanzasMapper.java)
- [CierreCajaExporter.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/exporter/CierreCajaExporter.java)
- [CierreCajaExportService.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/finanzas/exporter/CierreCajaExportService.java)
- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
