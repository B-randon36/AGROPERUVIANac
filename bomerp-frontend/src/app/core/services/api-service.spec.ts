import { describe, beforeEach, it, expect } from 'vitest';
import { ApiService } from './api-service';

describe('ApiService', () => {
  let service: ApiService;

  beforeEach(() => {
    service = new ApiService();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe normalizar rutas correctamente con prefijo /', () => {
    const url = service.buildUrl('api/v1/ventas');
    expect(url).toContain('/api/v1/ventas');
  });

  it('debe respetar rutas que ya inician con /', () => {
    const url = service.buildUrl('/api/v1/finanzas');
    expect(url).toContain('/api/v1/finanzas');
  });
});
