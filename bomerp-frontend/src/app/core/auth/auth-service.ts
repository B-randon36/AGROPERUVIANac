import { Injectable, computed, signal } from '@angular/core';
import { environment } from '../../../environments/environment';
import { generarCodeChallenge, generarCodeVerifier, generarState } from './pkce';
import { decodeJwtPayload } from './jwt';

interface SesionKeycloak {
  accessToken: string;
  refreshToken: string;
  idToken: string;
  expiraEn: number;
}

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  id_token: string;
  expires_in: number;
}

const CLAVE_CODE_VERIFIER = 'bomerp_pkce_code_verifier';
const CLAVE_STATE = 'bomerp_pkce_state';
const CLAVE_DESTINO = 'bomerp_pkce_destino';
const CLAVE_SESION = 'bomerp_sesion';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sesion = signal<SesionKeycloak | null>(this.restaurarSesion());

  readonly autenticado = computed(() => {
    const actual = this.sesion();
    return actual !== null && actual.expiraEn > Date.now();
  });

  readonly roles = computed<string[]>(() => {
    const token = this.sesion()?.accessToken;
    if (!token) return [];
    const payload = decodeJwtPayload(token);
    if (!payload) return [];

    const realmRoles = payload.realm_access?.roles ?? [];
    const clientRoles = Object.values(payload.resource_access ?? {}).flatMap(
      (recurso) => recurso?.roles ?? [],
    );

    return Array.from(new Set([...realmRoles, ...clientRoles]));
  });

  readonly usuario = computed(() => {
    const token = this.sesion()?.accessToken;
    if (!token) return null;
    const payload = decodeJwtPayload(token);
    if (!payload) return null;
    return {
      nombre: payload.name ?? payload.preferred_username ?? 'Usuario',
      email: payload.email ?? '',
      username: payload.preferred_username ?? '',
      sub: payload.sub ?? '',
    };
  });

  usuarioId(): number {
    const u = this.usuario();
    if (!u) return 1;
    const roles = this.roles().map((r) => r.toUpperCase());
    const texto = `${u.username} ${u.nombre} ${u.email}`.toLowerCase();
    if (roles.includes('VENDEDOR') || roles.includes('ROLE_VENDEDOR') || texto.includes('vendedor')) {
      return 2;
    }
    if (roles.includes('CAJERO') || roles.includes('ROLE_CAJERO') || texto.includes('cajero')) {
      return 3;
    }
    if (roles.includes('OPERADOR') || roles.includes('ROLE_OPERADOR') || texto.includes('operador')) {
      return 4;
    }
    return 1;
  }

  tieneRol(...rolesPermitidos: string[]): boolean {
    const actuales = this.roles().map((r) => r.trim().toUpperCase());
    if (actuales.includes('ADMIN') || actuales.includes('ROLE_ADMIN')) {
      return true;
    }
    return rolesPermitidos.some((rol) => {
      const limpio = rol.trim().toUpperCase();
      return actuales.includes(limpio) || actuales.includes(`ROLE_${limpio}`);
    });
  }

  accessToken(): string | null {
    return this.sesion()?.accessToken ?? null;
  }

  async iniciarLogin(destino = location.pathname): Promise<void> {
    const verifier = generarCodeVerifier();
    const state = generarState();
    const challenge = await generarCodeChallenge(verifier);
    sessionStorage.setItem(CLAVE_CODE_VERIFIER, verifier);
    sessionStorage.setItem(CLAVE_STATE, state);
    sessionStorage.setItem(CLAVE_DESTINO, destino);

    const parametros = new URLSearchParams({
      client_id: environment.keycloak.clientId,
      response_type: 'code',
      redirect_uri: environment.keycloak.redirectUri,
      scope: 'openid profile email',
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    });
    location.href = `${environment.keycloak.issuer}/protocol/openid-connect/auth?${parametros}`;
  }

  async completarLogin(code: string, stateRecibido: string): Promise<void> {
    const stateEsperado = sessionStorage.getItem(CLAVE_STATE);
    const verifier = sessionStorage.getItem(CLAVE_CODE_VERIFIER);
    sessionStorage.removeItem(CLAVE_STATE);
    if (!verifier || stateRecibido !== stateEsperado) {
      throw new Error('state o code_verifier invalido: posible CSRF, inicia el login de nuevo');
    }
    const cuerpo = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: environment.keycloak.clientId,
      redirect_uri: environment.keycloak.redirectUri,
      code,
      code_verifier: verifier,
    });
    const respuesta = await fetch(`${environment.keycloak.issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: cuerpo,
    });
    sessionStorage.removeItem(CLAVE_CODE_VERIFIER);
    if (!respuesta.ok) {
      throw new Error('No se pudo intercambiar el codigo por un token');
    }
    this.guardarSesion(await respuesta.json());
  }

  destinoPendiente(): string {
    const destino = sessionStorage.getItem(CLAVE_DESTINO) ?? '/';
    sessionStorage.removeItem(CLAVE_DESTINO);
    return destino;
  }

  async refrescar(): Promise<boolean> {
    const refreshToken = this.sesion()?.refreshToken;
    if (!refreshToken) return false;

    const cuerpo = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: environment.keycloak.clientId,
      refresh_token: refreshToken,
    });
    const respuesta = await fetch(`${environment.keycloak.issuer}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: cuerpo,
    });
    if (!respuesta.ok) {
      this.sesion.set(null);
      sessionStorage.removeItem(CLAVE_SESION);
      return false;
    }
    this.guardarSesion(await respuesta.json());
    return true;
  }

  cerrarSesion(): void {
    const idToken = this.sesion()?.idToken;
    this.sesion.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
    const parametros = new URLSearchParams({
      post_logout_redirect_uri: environment.keycloak.postLogoutRedirectUri,
      ...(idToken ? { id_token_hint: idToken } : {}),
    });
    location.href = `${environment.keycloak.issuer}/protocol/openid-connect/logout?${parametros}`;
  }

  private guardarSesion(datos: TokenResponse): void {
    const sesion: SesionKeycloak = {
      accessToken: datos.access_token,
      refreshToken: datos.refresh_token,
      idToken: datos.id_token,
      expiraEn: Date.now() + datos.expires_in * 1000,
    };
    this.sesion.set(sesion);
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
  }

  private restaurarSesion(): SesionKeycloak | null {
    const guardada = sessionStorage.getItem(CLAVE_SESION);
    if (!guardada) return null;
    try {
      const sesion = JSON.parse(guardada) as SesionKeycloak;
      if (sesion.expiraEn > Date.now()) {
        return sesion;
      }
    } catch {
      // JSON invalido: se descarta igual que una sesion vencida
    }
    sessionStorage.removeItem(CLAVE_SESION);
    return null;
  }
}
