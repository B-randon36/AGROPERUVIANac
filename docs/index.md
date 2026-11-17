# AGROPERUVIAN · GDMERP

<div class="hero">
<span class="eyebrow">PROYECTO INTEGRADOR · ADS / BD2 / LP2 · SECCIÓN G2 · GRUPO 3</span>
<h2>Del lote a la venta.<br>Una operación agroindustrial conectada.</h2>
<p>Documentación técnica y funcional de <strong>GDMERP (Agro Peruvian Foods)</strong>, desarrollado por <strong>Los Hijos del Backend</strong>: ventas omnicanal y POS, control de caja, inventario con trazabilidad FEFO, compras y producción de derivados de cacao y coco.</p>
<a class="md-button md-button--primary" href="ads/arquitectura.html">Ver Arquitectura C4</a>
<a class="md-button" href="guia/instalacion.html">Guía de Instalación</a>
</div>

## Resumen Ejecutivo

**GDMERP** centraliza la operación comercial y logística de Agro Peruvian Foods bajo una arquitectura **Monolito Modular** sobre Spring Boot y Angular, asegurando integridad transaccional desde la orden de producción hasta el comprobante de venta en ventanilla.

<div class="grid cards" markdown>

- **Proyecto y Reglas de Negocio**

    Alcance del sistema, actores, equipo y las 18 reglas de negocio implementadas (`RN-VEN-01` a `RN-VEN-10` y `RN-FIN-01` a `RN-FIN-08`).

    [Explorar proyecto](proyecto/alcance.md)

- **ADS · Arquitectura y Diseño C4**

    Diagramas vectoriales SVG de Contexto (C1), Contenedores (C2), Componentes (C3), secuencia UML, estados y ADRs.

    [Explorar arquitectura](ads/arquitectura.md)

- **BD2 · Datos y Persistencia**

    Modelo entidad-relación JPA, secuencias únicas de tickets, optimización en Oracle Database y operación.

    [Revisar datos](bd2/datos.md)

- **LP2 · Backend, SPA y Seguridad**

    APIs REST en Spring Boot 4, aplicación Angular 22, control de acceso OIDC con Keycloak 26 y evidencias de pruebas.

    [Revisar implementación](lp2/backend.md)

</div>

## Estado Actualizado del Proyecto

| Indicador | Detalle |
|---|---|
| **Equipo y Sección** | **Grupo 3 · Los Hijos del Backend** — Sección `G2` |
| **Nombre del Proyecto** | **GDMERP** (Agro Peruvian Foods) |
| **Rama Oficial** | `main` (unificada y sincronizada en GitHub) |
| **Stack Backend** | Java 21, Spring Boot 4.0.7, Spring Modulith, Spring Data JPA, Spring Security (OAuth2/OIDC) |
| **Stack Frontend** | Angular 22 (Standalone Components, Signals), HTML5/CSS3 |
| **Identidad y Datos** | Keycloak 26.8.0 (PostgreSQL 17), H2 (dev/test), Oracle Database (producción) |
| **Suite de Pruebas** | **105 pruebas automatizadas pasando** (`0` fallos, `0` errores) |
| **Despliegue Validado** | Backend (`:8080`), Frontend (`:4200`) y Keycloak (`:38080`) ejecutados end-to-end |

[Guía de lectura](guia/lectura.md) · [Reglas de negocio](proyecto/reglas.md) · [Plan de pruebas](evidencias/pruebas.md) · [Checklist final](evidencias/checklist.md)

## Fuentes del Repositorio

- [Repositorio en GitHub (rama `main`)](https://github.com/B-randon36/AGROPERUVIANac/tree/main)
- [README.md](https://github.com/B-randon36/AGROPERUVIANac/blob/main/README.md)
- [backend/pom.xml](https://github.com/B-randon36/AGROPERUVIANac/blob/main/backend/pom.xml)
- [bomerp-frontend/package.json](https://github.com/B-randon36/AGROPERUVIANac/blob/main/bomerp-frontend/package.json)
