import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';
import { ROLE_LABELS } from '../../models/auth.models';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="landing-page">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>Plataforma Corporativa de Gestión Laboral</span>
        </div>
        
        <h1 class="hero-title">
          Gestión de <span class="accent-text">Recursos Humanos</span> y vacaciones
        </h1>
        
        <p class="hero-description">
          Administra el ciclo de vida completo de los empleados: altas con credenciales automáticas,
          trayectoria contractual, saldo de vacaciones, solicitudes de ausencia y calendario del equipo.
        </p>

        <!-- CTA Buttons -->
        <div class="hero-actions">
          @if (authService.isAuthenticated()) {
            @if (authService.isRRHH()) {
              <a mat-flat-button routerLink="/employees/new" class="cta-primary">
                <mat-icon>person_add</mat-icon>
                Alta de Empleado
              </a>
            }
            @if (authService.currentUser(); as user) {
              <div class="logged-banner">
                <mat-icon aria-hidden="true">verified_user</mat-icon>
                <span>Sesión iniciada como <strong>{{ user.username }}</strong> · {{ roleLabels[user.role] }}</span>
              </div>
            }
          } @else {
            <a mat-flat-button routerLink="/login" class="cta-primary">
              <mat-icon>login</mat-icon>
              Iniciar Sesión
            </a>
          }
        </div>
      </section>

      <!-- Modules Grid -->
      <section class="features-section">
        <div class="section-header">
          <h2 class="section-title">Módulos del Sistema</h2>
          <p class="section-subtitle">Cada módulo se habilita según el rol de tu cuenta</p>
        </div>

        <div class="features-grid">
          <!-- Card 1: Empleados y Contratos -->
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <mat-icon aria-hidden="true">badge</mat-icon>
            </div>
            <h3>Altas y Trayectoria Contractual</h3>
            <p>Registro de empleados con generación segura de credenciales temporales y registro histórico inmutable de variaciones laborales.</p>
            <div class="card-footer-info">
              @if (authService.isAuthenticated() && authService.isRRHH()) {
                <a routerLink="/employees/new" class="card-link">Alta de Empleado<mat-icon aria-hidden="true">arrow_forward</mat-icon></a>
              } @else {
                <span class="card-tag">Requiere rol RRHH</span>
              }
            </div>
          </div>

          <!-- Card 2: Vacaciones y Ausencias -->
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <mat-icon aria-hidden="true">beach_access</mat-icon>
            </div>
            <h3>Vacaciones y Permisos</h3>
            <p>Solicitud de vacaciones y permisos, saldo disponible según convenio y resolución por el responsable de equipo.</p>
            <div class="card-footer-info">
              @if (authService.isAuthenticated()) {
                <a routerLink="/profile" class="card-link">Mi Ficha y Saldo<mat-icon aria-hidden="true">arrow_forward</mat-icon></a>
              } @else {
                <span class="card-tag">Requiere iniciar sesión</span>
              }
            </div>
          </div>

          <!-- Card 3: Calendario de Equipo -->
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <mat-icon aria-hidden="true">calendar_month</mat-icon>
            </div>
            <h3>Calendario del Equipo</h3>
            <p>Vista mensual de las ausencias del equipo para anticipar coincidencias y garantizar la cobertura.</p>
            <div class="card-footer-info">
              @if (authService.isAuthenticated() && authService.isResponsable()) {
                <a routerLink="/team/calendar" class="card-link">Calendario del Equipo<mat-icon aria-hidden="true">arrow_forward</mat-icon></a>
              } @else {
                <span class="card-tag">Requiere rol Responsable de equipo</span>
              }
            </div>
          </div>

          <!-- Card 4: Evaluaciones y Auditoría -->
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <mat-icon aria-hidden="true">insights</mat-icon>
            </div>
            <h3>Evaluaciones del Desempeño</h3>
            <p>Revisiones periódicas de objetivos con conformidad digital del empleado y trazabilidad completa para cumplimiento normativo.</p>
            <div class="card-footer-info">
              <span class="card-tag">Próximamente</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Quick Info / Help Section -->
      @if (!authService.isAuthenticated()) {
        <section class="auth-helper-section">
          <div class="helper-card">
            <div class="helper-content">
              <h3>¿Listo para comenzar?</h3>
              <p>Inicia sesión con tu cuenta corporativa para acceder a tus funcionalidades.</p>
            </div>
            <a mat-stroked-button routerLink="/login" class="helper-btn">
              Iniciar Sesión
            </a>
          </div>
        </section>
      }
    </div>
  `,
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  protected readonly authService = inject(AuthService);
  protected readonly roleLabels = ROLE_LABELS;
}
