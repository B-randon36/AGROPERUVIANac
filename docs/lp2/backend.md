# Backend REST

## Organización

El paquete raíz es `pe.edu.upeu.bomerp`. La clase `BomerpBackendApplication` arranca la aplicación. Los paquetes de negocio agrupan controladores, servicios, DTO, entidades y repositorios.

| Capa | Trabajo | Ejemplo |
|---|---|---|
| Controller | Entrada HTTP y contexto del usuario | `ventas/controller/VentaController.java` |
| DTO | Validación y contrato de intercambio | `ventas/dto/VentaRequest.java` |
| Service | Operación de negocio y transacción | `ventas/service/VentaServiceImpl.java` |
| Repository | Consultas y persistencia | `ventas/repository/VentaRepository.java` |
| Entity | Estado persistente | `ventas/entity/Venta.java` |
| Integración interna | Contrato entre módulos | `finanzas/api/FinanzasApi.java` |

## Dónde se crea una venta

En VS Code usa **Ctrl+P**, escribe `VentaServiceImpl.java` y busca `public VentaResponse crear`. Ese método toma datos de catálogo, descuenta existencias, solicita despacho FEFO, calcula importes y ejecuta `ventaRepository.save(venta)`. El controlador lo invoca desde `POST /api/v1/ventas`.

`@Transactional` define la unidad transaccional del servicio. La prueba de rollback debe demostrar ausencia de efectos parciales en venta, stock y finanzas ante fallos; no basta con mostrar un error HTTP.

## Contratos y errores

`@Valid` activa validación de los DTO que tienen restricciones. Los manejadores de excepciones traducen fallos a respuestas HTTP. Probar 400 para entradas inválidas, 401 por ausencia de autenticación, 403 por falta de autorización y conflictos de negocio conforme al manejador real.

## Dependencias

El POM declara Java 21, Spring Boot 4.0.7, Spring Modulith, Spring Security, JPA, drivers H2/Oracle y bibliotecas de exportación. La versión efectiva debe comprobarse con Maven al preparar la ejecución.

## Fuentes del proyecto

- [BomerpBackendApplication.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/BomerpBackendApplication.java)
- [VentaServiceImpl.java](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/src/main/java/pe/edu/upeu/bomerp/ventas/service/VentaServiceImpl.java)
- [pom.xml](https://github.com/B-randon36/AGROPERUVIANac/blob/41bd74a00342e5b28bb793a9bdc7221e3919fc3a/backend/pom.xml)
