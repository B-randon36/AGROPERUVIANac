import { Component, signal, OnInit, OnDestroy, inject, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AuthService } from '../auth/auth-service';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, DatePipe],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly currentDateTime = signal(new Date());
  private clockInterval: any;

  protected readonly rolHumano = computed(() => {
    const roles = this.auth.roles().map((r) => r.toUpperCase());
    if (roles.includes('ADMIN')) return 'Administrador General';
    if (roles.includes('SUPERVISOR')) return 'Supervisor Operativo';
    if (roles.includes('ADMIN_FINANZAS') || roles.includes('CAJERO')) return 'Cajero / Tesorería';
    if (roles.includes('OPERADOR')) return 'Jefe de Planta / Operador';
    if (roles.includes('VENDEDOR')) return 'Ejecutivo de Ventas';
    return 'Personal Agro Peruvian';
  });

  ngOnInit(): void {
    this.clockInterval = setInterval(() => {
      this.currentDateTime.set(new Date());
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
    }
  }
}
