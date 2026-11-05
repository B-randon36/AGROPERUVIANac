import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-sin-permiso',
  imports: [RouterLink],
  template: `
    <div style="max-width: 540px; margin: 4rem auto; text-align: center; padding: 2.5rem; background: #ffffff; border-radius: 16px; box-shadow: 0 10px 25px rgba(26,36,56,0.08); border: 1px solid #E2E8F0; border-top: 5px solid #EF4444;">
      <div style="width: 64px; height: 64px; background: #FEF2F2; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem;">
        <span class="material-symbols-outlined" style="font-size: 2.5rem; color: #EF4444;">shield_lock</span>
      </div>
      <h2 style="color: #1A2438; margin-bottom: 0.5rem; font-size: 1.5rem; font-weight: 700;">Acceso Restringido (403)</h2>
      <p style="color: #64748B; font-size: 0.95rem; line-height: 1.6; margin: 1rem 0 2rem;">No cuentas con los permisos autorizados en el sistema para ingresar a este módulo o funcionalidad. Contacta con el Administrador del sistema si requieres habilitar este perfil.</p>
      <a routerLink="/" style="display: inline-flex; align-items: center; gap: 0.5rem; background: #7DD3FC; color: #0F172A; padding: 0.65rem 1.4rem; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 0.875rem; box-shadow: 0 2px 6px rgba(125,211,252,0.3); transition: all 0.2s;">
        <span class="material-symbols-outlined" style="font-size: 1.1rem;">arrow_back</span>
        Volver al Panel Principal
      </a>
    </div>
  `,
})
export class SinPermiso {}
