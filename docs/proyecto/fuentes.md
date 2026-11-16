# Fuentes, Versión y Mantenimiento

La documentación corresponde al estado consolidado del repositorio en la rama oficial **`main`** de [AGROPERUVIANac](https://github.com/B-randon36/AGROPERUVIANac). Todas las rutas y enlaces apuntan directamente al código fuente vigente en `main`.

## Referencia Académica

- [Portal BOM ERP](https://262ciclo4.github.io/bomerp/)
- [Producto integrado U3](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/)
- [Checklist final](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/checklist-final/)

## Fuentes de Implementación

La documentación está sincronizada directamente con los archivos del código fuente:
- Configuración y dependencias: `backend/pom.xml`, `bomerp-frontend/package.json`, `backend/compose-dev.yml`
- Reglas de negocio en código (`RN-VEN-01..10` y `RN-FIN-01..08`): `VentaServiceImpl.java`, `ProductoServiceImpl.java`, `InventarioServiceImpl.java`, `SesionCajaServiceImpl.java`, `CuentaPorCobrarServiceImpl.java`, `FinanzasApiImpl.java`, `VentaController.java`
- Suite de pruebas: `VentaServiceTest.java`, `VentaSecurityTest.java`, `VentaPersistenciaTest.java`, `VentaEndToEndTest.java`, `SesionCajaServiceTest.java`, `ModularityTests.java`

## Flujo de Actualización

1. Editar los archivos Markdown en la carpeta `docs/`.
2. Hacer commit y push hacia la rama `main`.
3. El flujo de GitHub Actions compila el sitio con `mkdocs build --strict` y publica automáticamente los cambios en la rama `gh-pages`.
