import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DepartmentReportService } from '../../services/department-report.service';
import {
  DepartmentReportResponse,
  DepartmentReportRow,
  ReportPeriodPreset,
  ReportType
} from '../../models/department-report.models';

@Component({
  selector: 'app-department-reports',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './department-reports.component.html',
  styleUrl: './department-reports.component.scss'
})
export class DepartmentReportsComponent {
  private readonly reportService = inject(DepartmentReportService);

  readonly reportType = signal<ReportType>('ABSENTEEISM');
  readonly periodPreset = signal<ReportPeriodPreset>('CURRENT_YEAR');
  readonly startDate = signal('');
  readonly endDate = signal('');
  readonly report = signal<DepartmentReportResponse | null>(null);
  readonly loading = signal(false);
  readonly exporting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly expandedDepartments = signal<Set<string>>(new Set());

  constructor() {
    this.applyPeriodPreset('CURRENT_YEAR');
  }

  onTypeChange(event: Event): void {
    const target = event.target;
    if (target instanceof HTMLSelectElement) {
      this.reportType.set(target.value as ReportType);
    }
  }

  onPeriodPresetChange(event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLSelectElement)) {
      return;
    }
    const preset = target.value as ReportPeriodPreset;
    this.periodPreset.set(preset);
    if (preset !== 'CUSTOM') {
      this.applyPeriodPreset(preset);
    }
  }

  onDateChange(field: 'startDate' | 'endDate', event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    this.periodPreset.set('CUSTOM');
    if (field === 'startDate') {
      this.startDate.set(target.value);
    } else {
      this.endDate.set(target.value);
    }
  }

  generateReport(): void {
    if (!this.startDate() || !this.endDate() || this.endDate() < this.startDate()) {
      this.errorMessage.set('Selecciona un rango de fechas válido.');
      this.report.set(null);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.report.set(null);
    this.reportService.getReport(this.reportType(), this.startDate(), this.endDate()).subscribe({
      next: (report) => {
        this.report.set(report);
        this.expandedDepartments.set(new Set());
        this.loading.set(false);
      },
      error: (error: { error?: { detail?: string } }) => {
        this.loading.set(false);
        this.errorMessage.set(error.error?.detail || 'No se pudo generar el informe. Inténtalo de nuevo.');
      }
    });
  }

  exportCsv(): void {
    const report = this.report();
    if (!report?.hasData) {
      return;
    }

    this.exporting.set(true);
    this.reportService.exportCsv(report.type, report.startDate, report.endDate).subscribe({
      next: (file) => {
        const url = URL.createObjectURL(file);
        const link = document.createElement('a');
        link.href = url;
        link.download = `informe-${report.type.toLowerCase()}-${report.startDate}-${report.endDate}.csv`;
        link.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: (error: { error?: { detail?: string } }) => {
        this.exporting.set(false);
        this.errorMessage.set(error.error?.detail || 'No se pudo exportar el informe. Inténtalo de nuevo.');
      }
    });
  }

  toggleDepartment(department: DepartmentReportRow): void {
    const expanded = new Set(this.expandedDepartments());
    if (expanded.has(department.department)) {
      expanded.delete(department.department);
    } else {
      expanded.add(department.department);
    }
    this.expandedDepartments.set(expanded);
  }

  isExpanded(department: DepartmentReportRow): boolean {
    return this.expandedDepartments().has(department.department);
  }

  private applyPeriodPreset(preset: Exclude<ReportPeriodPreset, 'CUSTOM'>): void {
    const today = new Date();
    const year = today.getFullYear();
    let start: Date;
    let end: Date;

    if (preset === 'PREVIOUS_YEAR') {
      start = new Date(year - 1, 0, 1);
      end = new Date(year - 1, 11, 31);
    } else if (preset === 'CURRENT_QUARTER') {
      const quarterStartMonth = Math.floor(today.getMonth() / 3) * 3;
      start = new Date(year, quarterStartMonth, 1);
      end = new Date(year, quarterStartMonth + 3, 0);
    } else {
      start = new Date(year, 0, 1);
      end = new Date(year, 11, 31);
    }

    this.startDate.set(this.toLocalDate(start));
    this.endDate.set(this.toLocalDate(end));
  }

  private toLocalDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
