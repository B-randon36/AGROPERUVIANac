import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import { App } from './app';

describe('App', () => {
  it('debe instanciarse correctamente como componente raíz', () => {
    const app = new App();
    expect(app).toBeTruthy();
  });
});
