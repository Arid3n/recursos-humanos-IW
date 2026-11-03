import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { LeaveService } from '../../services/leave.service';
import { LeaveRequestSummary, LeaveStatus } from '../../models/leave.models';
import { RejectionReasonDialogComponent } from '../rejection-reason-dialog/rejection-reason-dialog.component';
import { CancelConfirmDialogComponent } from './cancel-confirm-dialog.component';

@Component({
  selector: 'app-leave-request-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './leave-request-list.component.html',
  styleUrls: ['./leave-request-list.component.scss']
})
export class LeaveRequestListComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns: string[] = [
    'leaveType',
    'dates',
    'calculatedDays',
    'status',
    'createdAt',
    'actions'
  ];

  readonly requests = signal<LeaveRequestSummary[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly cancellingId = signal<string | null>(null);

  readonly selectedStatus = signal<LeaveStatus | null>(null);
  pageIndex: number = 0;
  pageSize: number = 10;
  readonly totalElements = signal<number>(0);

  readonly statusFilters: { label: string; value: LeaveStatus | null; icon: string }[] = [
    { label: 'Todas', value: null, icon: 'list_alt' },
    { label: 'Pendientes', value: 'PENDING', icon: 'schedule' },
    { label: 'Aprobadas', value: 'APPROVED', icon: 'check_circle' },
    { label: 'Rechazadas', value: 'REJECTED', icon: 'cancel' }
  ];

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const filterStatus = this.selectedStatus() ?? undefined;

    this.leaveService.getMyRequests(this.pageIndex, this.pageSize, filterStatus).subscribe({
      next: (response) => {
        this.requests.set(response.content || []);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err.error?.detail || 'No se pudieron cargar las solicitudes de ausencia. Por favor, reintenta más tarde.'
        );
      }
    });
  }

  setFilter(status: LeaveStatus | null): void {
    if (this.selectedStatus() === status) return;
    this.selectedStatus.set(status);
    this.pageIndex = 0;
    this.loadRequests();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRequests();
  }

  openRejectionReason(request: LeaveRequestSummary): void {
    this.dialog.open(RejectionReasonDialogComponent, {
      width: '480px',
      data: {
        rejectionReason: request.rejectionReason,
        leaveType: request.leaveType,
        startDate: request.startDate,
        endDate: request.endDate,
        calculatedDays: request.calculatedDays
      }
    });
  }

  confirmCancel(request: LeaveRequestSummary): void {
    const dialogRef = this.dialog.open(CancelConfirmDialogComponent, {
      width: '480px',
      data: { request }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.executeCancel(request);
      }
    });
  }

  private executeCancel(request: LeaveRequestSummary): void {
    this.cancellingId.set(request.id);

    this.leaveService.cancelRequest(request.id).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.snackBar.open('Solicitud cancelada correctamente.', 'Cerrar', {
          duration: 4000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['snackbar-success']
        });
        this.loadRequests();
      },
      error: (err) => {
        this.cancellingId.set(null);
        const detail = err.error?.detail || 'No se pudo cancelar la solicitud. Es posible que haya cambiado de estado.';
        this.snackBar.open(detail, 'Cerrar', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['snackbar-error']
        });
        this.loadRequests();
      }
    });
  }
}
