# Checklist Final del Proyecto

Estado de cumplimiento técnico y documental de **GDMERP (Agro Peruvian Foods)** — **Grupo 3 · Los Hijos del Backend (Sección G2)** sobre la rama **`main`**.

| Requisito | Estado | Evidencia Verificada |
|---|---|---|
| **Problema, solución y alcance** | ✅ Completado | Documentado en `proyecto/alcance.md` y `proyecto/modulos.md` |
| **Arquitectura C4 (C1, C2, C3) y UML** | ✅ Completado | Diagramas interactivos Mermaid en `ads/arquitectura.md` y `ads/uml.md` |
| **Patrones y decisiones (ADR)** | ✅ Completado | Monolito Modular, DTO, Repository, State, Adapter (`ads/patrones.md`, `ads/adrs.md`) |
| **Modelo de dominio y reglas** | ✅ Completado | 10 reglas `RN-VEN` y 8 reglas `RN-FIN` documentadas en código y portal |
| **Backend Spring Boot 4 + SPA Angular 22** | ✅ Completado | Operativos end-to-end en puertos `:8080` y `:4200` |
| **Despliegue único y Spring Modulith** | ✅ Completado | Validado por `ModularityTests` (`ApplicationModules.verify()`) |
| **Seguridad OAuth2 / OIDC (Keycloak)** | ✅ Completado | Keycloak 26.8 (`:38080`), roles `ADMIN`, `SUPERVISOR`, `VENDEDOR` y `VentaSecurityTest` |
| **Idempotencia, concurrencia y FEFO** | ✅ Completado | `operacionId`, secuencia de tickets única y reversión multilote FEFO probadas |
| **Suite de pruebas automatizadas** | ✅ Completado | **105/105 pruebas backend pasando** (`0` fallos, `0` errores) |
| **Scripts Oracle y optimización SQL** | ✅ Preparado | DDL, índices y secuencia Oracle en `db/oracle_ventas_ticket_sequence.sql` |
| **Auditoría de operaciones** | ✅ Completado | `AuditoriaService` con registro de actor JWT, fecha y acción |
| **Repositorio académico y Topics** | ✅ Completado | Rama `main` unificada, topics G2/Grupo 3 configurados |
| **Documentación Web en GitHub Pages** | ✅ Publicado | Portal MkDocs Material activo en `https://b-randon36.github.io/AGROPERUVIANac/` |
| **Sustentación individual** | ✅ Preparado | Matriz de módulos por integrante en `proyecto/equipo.md` y guía en `evidencias/sustentacion.md` |
