import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule],
  template: `
    <div class="landing-page">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          <span>Plataforma Corporativa de Gestión Laboral</span>
        </div>
        
        <h1 class="hero-title">
          Gestión inteligente de <span class="gradient-text">Recursos Humanos</span>
        </h1>
        
        <p class="hero-description">
          Administra el ciclo de vida completo de los empleados: altas con credenciales automáticas,
          trayectoria contractual inmutable, saldo de vacaciones en tiempo real, calendario de equipos y evaluaciones.
        </p>

        <!-- CTA Buttons -->
        <div class="hero-actions">
          @if (authService.isAuthenticated()) {
            @if (authService.isRRHH()) {
              <a mat-flat-button color="primary" routerLink="/employees/new" class="cta-primary">
                <span>➕ Dar de Alta Empleado</span>
              </a>
            }
            <div class="logged-banner">
              <span class="logged-icon">✅</span>
              <span>Sesión activa como <strong>{{ authService.currentUser()?.username }}</strong> ({{ authService.currentUser()?.role }})</span>
            </div>
          } @else {
            <a mat-flat-button color="primary" routerLink="/login" class="cta-primary">
              <span>🚀 Acceder al Portal</span>
            </a>
          }
        </div>
      </section>

      <!-- Modules Grid -->
      <section class="features-section">
        <div class="section-header">
          <h2 class="section-title">Módulos del Sistema</h2>
          <p class="section-subtitle">Diseñado con arquitectura modular, trazabilidad y consistencia contractual</p>
        </div>

        <div class="features-grid">
          <!-- Card 1: Empleados y Contratos -->
          <div class="feature-card">
            <div class="feature-icon-wrapper blue">
              <span class="feature-icon">👥</span>
            </div>
            <h3>Altas y Trayectoria Contractual</h3>
            <p>Registro de empleados con generación segura de credenciales temporales y registro histórico inmutable de variaciones laborales.</p>
            <div class="card-footer-info">
              @if (authService.isAuthenticated() && authService.isRRHH()) {
                <a routerLink="/employees/new" class="card-link">Registrar nuevo empleado →</a>
              } @else {
                <span class="card-tag">Requiere rol RRHH</span>
              }
            </div>
          </div>

          <!-- Card 2: Vacaciones y Ausencias -->
          <div class="feature-card">
            <div class="feature-icon-wrapper teal">
              <span class="feature-icon">🏖️</span>
            </div>
            <h3>Vacaciones y Permisos</h3>
            <p>Gestión de solicitudes de ausencias, cálculo derivado de saldo disponible según convenio y resolución atómica por responsables.</p>
            <div class="card-footer-info">
              <span class="card-tag">Art. 12, 13 y 14</span>
            </div>
          </div>

          <!-- Card 3: Calendario de Equipo -->
          <div class="feature-card">
            <div class="feature-icon-wrapper purple">
              <span class="feature-icon">📅</span>
            </div>
            <h3>Calendario de Disponibilidad</h3>
            <p>Visualización del estado del equipo en tiempo real para evitar solapamientos y garantizar cobertura en cada departamento.</p>
            <div class="card-footer-info">
              <span class="card-tag">Vista Mensual / Semanal</span>
            </div>
          </div>

          <!-- Card 4: Evaluaciones y Auditoría -->
          <div class="feature-card">
            <div class="feature-icon-wrapper amber">
              <span class="feature-icon">📈</span>
            </div>
            <h3>Evaluaciones del Desempeño</h3>
            <p>Revisiones periódicas de objetivos con conformidad digital del empleado y trazabilidad completa para cumplimiento normativo.</p>
            <div class="card-footer-info">
              <span class="card-tag">Art. 18 y 19</span>
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
}
