import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from './auth-service';

@Component({
  selector: 'app-auth-callback',
  template: `
    <div style="display:flex;justify-content:center;align-items:center;min-height:100vh;background-color:#F4F7FB;font-family:'Inter',sans-serif;">
      <div style="text-align:center;background:#ffffff;padding:2.5rem;border-radius:16px;border:1px solid #E2E8F0;box-shadow:0 10px 25px rgba(26,36,56,0.06);max-width:400px;width:90%;">
        <div style="width:72px;height:72px;margin:0 auto 1.25rem;border-radius:50%;background:#ffffff;padding:2px;box-shadow:0 0 0 2px rgba(125,211,252,0.5);">
          <img src="assets/logo.png" alt="Agro Peruvian Foods" style="width:100%;height:100%;border-radius:50%;object-fit:cover;" />
        </div>
        <h2 style="color:#1A2438;font-size:1.25rem;font-weight:700;margin-bottom:0.35rem;">Agro Peruvian Foods</h2>
        <p style="color:#64748B;font-size:0.875rem;margin-bottom:1.5rem;">Validando credenciales seguras...</p>
        <div class="spinner" style="margin:0 auto;"></div>
      </div>
    </div>
  `,
})
export class AuthCallback {
  constructor() {
    const route = inject(ActivatedRoute);
    const router = inject(Router);
    const auth = inject(AuthService);

    const codigo = route.snapshot.queryParamMap.get('code');
    const state = route.snapshot.queryParamMap.get('state');
    const error = route.snapshot.queryParamMap.get('error');

    if (error || !codigo || !state) {
      router.navigateByUrl('/');
      return;
    }

    auth
      .completarLogin(codigo, state)
      .then(() => router.navigateByUrl(auth.destinoPendiente()))
      .catch(() => router.navigateByUrl('/'));
  }
}
