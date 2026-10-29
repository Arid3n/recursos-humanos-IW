import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeService } from '../../../../employees/services/employee.service';
import { EmployeeProfile } from '../../../leave/models/leave.models';
import { LeaveBalanceWidgetComponent } from '../../../leave/components/leave-balance-widget/leave-balance-widget.component';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    LeaveBalanceWidgetComponent
  ],
  template: `
    <div class="profile-page-container">
      <div *ngIf="loading()" class="loading-container">
        <mat-progress-spinner mode="indeterminate" diameter="50"></mat-progress-spinner>
        <span>Cargando ficha personal del empleado...</span>
      </div>

      <div *ngIf="!loading() && profile()" class="profile-content">
        <!-- Banner de Cabecera -->
        <div class="profile-header-card">
          <div class="header-content">
            <div class="avatar-circle">
              {{ profile()?.firstName?.charAt(0) }}{{ profile()?.lastName?.charAt(0) }}
            </div>
            <div class="header-details">
              <h1>{{ profile()?.firstName }} {{ profile()?.lastName }}</h1>
              <span class="role-badge">
                {{ profile()?.currentPosition }} • {{ profile()?.currentDepartment }}
              </span>
            </div>
          </div>
        </div>

        <!-- Secciones de Datos Personales y Contractuales -->
        <div class="details-grid" style="margin-top: 1.5rem;">
          <!-- Tarjeta 1: Información Personal -->
          <mat-card class="section-card">
            <mat-card-header>
              <mat-card-title>📋 Datos Identificativos</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">Nombre Completo</span>
                  <span class="info-value">{{ profile()?.firstName }} {{ profile()?.lastName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">NIF / NIE</span>
                  <span class="info-value">{{ profile()?.nationalId }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Correo Electrónico</span>
                  <span class="info-value">{{ profile()?.email }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Fecha de Incorporación</span>
                  <span class="info-value">{{ profile()?.hireDate }}</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Tarjeta 2: Régimen Contractual Vigente -->
          <mat-card class="section-card">
            <mat-card-header>
              <mat-card-title>🏛️ Situación Contractual</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">Puesto Actual</span>
                  <span class="info-value">{{ profile()?.currentPosition }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Departamento</span>
                  <span class="info-value">{{ profile()?.currentDepartment }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Convenio Colectivo</span>
                  <span class="info-value">{{ profile()?.currentAgreementName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Responsable de Equipo</span>
                  <span class="info-value">
                    {{ profile()?.managerFullName || 'Dirección General / Sin asignar' }}
                  </span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Bloque de Saldo de Vacaciones Embebido -->
        <mat-card class="balance-card" style="margin-top: 1.5rem;">
          <mat-card-content>
            <app-leave-balance-widget></app-leave-balance-widget>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styleUrl: './my-profile.component.scss'
})
export class MyProfileComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);

  readonly profile = signal<EmployeeProfile | null>(null);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.employeeService.getMyProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error al cargar perfil del empleado:', err);
        this.loading.set(false);
      }
    });
  }
}

