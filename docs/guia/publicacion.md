# Publicación en GitHub Pages

## Estado

GitHub Pages está activo y publicado desde la rama `gh-pages` mediante el flujo automático `.github/workflows/deploy-docs.yml`.

## Despliegue automático desde `main`

1. Editar los archivos Markdown en `docs/` o la configuración en `mkdocs.yml`.
2. Hacer commit y push hacia la rama `main`.
3. El flujo `.github/workflows/deploy-docs.yml` instala `requirements-docs.txt`, ejecuta `mkdocs build --strict` y publica la salida estática en `gh-pages`.

## Compilación y publicación manual (opcional)

1. Desde la raíz del proyecto, preparar el entorno de documentación siguiendo `LEEME_DOCUMENTACION.md`.
2. Comprobar la compilación:

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
```

3. Con una cuenta que tenga permiso de escritura y una sesión Git configurada, publicar la salida estática en `gh-pages`:

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs gh-deploy
```

El sitio expone documentación del proyecto. No ejecuta la aplicación Spring Boot, la SPA del ERP, Oracle ni Keycloak. No se necesita publicar credenciales del entorno para servir estas páginas.

## Mantener el sitio

Actualizar Markdown, reconstruir en modo estricto y publicar de nuevo cuando corresponda. Conservar en las páginas el commit del código descrito y las evidencias pendientes.

## Referencia técnica

[Guía oficial de despliegue de MkDocs](https://www.mkdocs.org/user-guide/deploying-your-docs/).
