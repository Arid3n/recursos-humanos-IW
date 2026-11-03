import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TeamLeaveService } from '../../services/team-leave.service';
import { TeamLeaveRequest } from '../../models/team-leave.models';
import {
  ResolveMode,
  ResolveRequestDialogComponent,
  ResolveRequestDialogData,
  ResolveRequestDialogResult
} from '../resolve-request-dialog/resolve-request-dialog.component';

@Component({
  selector: 'app-team-pending-requests',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './team-pending-requests.component.html',
  styleUrls: ['./team-pending-requests.component.scss']
})
export class TeamPendingRequestsComponent implements OnInit {
  private readonly teamLeaveService = inject(TeamLeaveService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns: string[] = ['employee', 'leaveType', 'dates', 'calculatedDays', 'createdAt', 'actions'];

  readonly requests = signal<TeamLeaveRequest[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly resolvingId = signal<string | null>(null);
  readonly totalElements = signal<number>(0);

  pageIndex = 0;
  pageSize = 10;

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.teamLeaveService.getTeamPendingRequests(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.requests.set(response.content ?? []);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.detail || 'No se pudieron cargar las solicitudes del equipo. Por favor, reintenta más tarde.');
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadRequests();
  }

  openResolveDialog(request: TeamLeaveRequest, mode: ResolveMode): void {
    const data: ResolveRequestDialogData = { mode, request };
    const dialogRef = this.dialog.open<ResolveRequestDialogComponent, ResolveRequestDialogData, ResolveRequestDialogResult>(
      ResolveRequestDialogComponent,
      { width: '480px', data }
    );

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.resolve(request, mode, result.text);
      }
    });
  }

  private resolve(request: TeamLeaveRequest, mode: ResolveMode, text: string | null): void {
    this.resolvingId.set(request.id);
    const action$ = mode === 'approve'
      ? this.teamLeaveService.approve(request.id, text)
      : this.teamLeaveService.reject(request.id, text ?? '');

    action$.subscribe({
      next: () => {
        this.resolvingId.set(null);
        this.showMessage(mode === 'approve' ? 'Solicitud aprobada correctamente.' : 'Solicitud rechazada correctamente.', 'snackbar-success');
        this.loadRequests();
      },
      error: (err) => {
        this.resolvingId.set(null);
        this.showMessage(err.error?.detail || 'No se pudo resolver la solicitud. Es posible que haya cambiado de estado.', 'snackbar-error');
        this.loadRequests();
      }
    });
  }

  private showMessage(message: string, panelClass: string): void {
    this.snackBar.open(message, 'Cerrar', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: [panelClass]
    });
  }
}
