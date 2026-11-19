import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

/** Pantalla a la que se redirige a Dirección General al abrir las rutas de solicitud de ausencia (HU-05 FR-017). */
@Component({
  selector: 'app-direccion-general-no-absences',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatIconModule, MatButtonModule],
  template: `
    <div class="page-container page-narrow">
      <mat-card class="surface-card" role="status">
        <mat-card-content>
          <h1 class="page-title">
            <mat-icon aria-hidden="true">info</mat-icon>
            Dirección General no solicita ausencias
          </h1>
          <p>Puedes aprobar o rechazar las solicitudes de tus subordinados directos desde «Solicitudes del Equipo».</p>
          <a mat-flat-button routerLink="/team/requests">Ir a Solicitudes del Equipo</a>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class DireccionGeneralNoAbsencesComponent {}
