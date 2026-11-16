# Fuentes, versión y mantenimiento

La referencia técnica de esta edición es el commit [`41bd74a`](https://github.com/B-randon36/AGROPERUVIANac/commit/41bd74a00342e5b28bb793a9bdc7221e3919fc3a) de `ventas_avance`. Las páginas enlazan archivos de esa versión; cambios posteriores requieren actualizar la documentación.

## Referencia académica

- [Portal BOM ERP](https://262ciclo4.github.io/bomerp/).
- [Producto integrado U3](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/).
- [Checklist final](https://262ciclo4.github.io/bomerp/proyecto-integrador/u3/checklist-final/).

El contenido propio adapta la estructura al dominio Agro Peruvian Foods. Los requisitos académicos se resumen; no se presenta una copia íntegra del material docente.

## Fuentes de implementación

El POM, package.json, controladores, entidades, servicios, configuración y pruebas tienen prioridad para describir lo que el programa contiene. El README y el brief aportan contexto, responsables y alcance declarado. Cuando difieren, se registra la diferencia.

## Actualización de una página

1. Identificar commit y módulo afectado.
2. Revisar su código, contrato y permisos.
3. Actualizar diagrama, regla y fila de trazabilidad.
4. Adjuntar ejecución de pruebas si se afirma funcionamiento.
5. Ejecutar `python -m mkdocs build --strict`.

No publicar tokens, contraseñas, datos de clientes o capturas de sesiones reales como evidencias académicas.
