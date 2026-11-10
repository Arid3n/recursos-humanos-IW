import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../../core/services/auth.service';
import {
  CreatePerformanceReviewRequest,
  PerformanceReview,
  PerformanceReviewEmployee
} from '../../models/performance-review.models';
import { PerformanceReviewService } from '../../services/performance-review.service';

@Component({
  selector: 'app-performance-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './performance-reviews.component.html',
  styleUrl: './performance-reviews.component.scss'
})
export class PerformanceReviewsComponent implements OnInit {
  private readonly reviewService = inject(PerformanceReviewService);
  private readonly authService = inject(AuthService);

  readonly isManager = this.authService.isResponsable();
  readonly reviews = signal<PerformanceReview[]>([]);
  readonly employees = signal<PerformanceReviewEmployee[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly employeeComments: Record<string, string> = {};

  selectedEmployeeId = '';
  period = '';
  objectives = '';
  rating = 3;
  comments = '';

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    if (this.isManager) {
      this.reviewService.getTeamEmployees().subscribe({
        next: (employees) => this.employees.set(employees),
        error: (error: { error?: { detail?: string } }) =>
          this.errorMessage.set(error.error?.detail || 'No se pudo cargar el equipo.')
      });
      this.reviewService.getTeamReviews().subscribe({
        next: (reviews) => {
          this.reviews.set(reviews);
          this.loading.set(false);
        },
        error: (error: { error?: { detail?: string } }) => {
          this.errorMessage.set(error.error?.detail || 'No se pudieron cargar las evaluaciones del equipo.');
          this.loading.set(false);
        }
      });
    } else {
      this.reviewService.getMyReviews().subscribe({
        next: (reviews) => {
          this.reviews.set(reviews);
          this.loading.set(false);
        },
        error: (error: { error?: { detail?: string } }) => {
          this.errorMessage.set(error.error?.detail || 'No se pudieron cargar tus evaluaciones.');
          this.loading.set(false);
        }
      });
    }
  }

  createReview(): void {
    const request: CreatePerformanceReviewRequest = {
      period: this.period.trim(),
      objectives: this.objectives.trim(),
      rating: Number(this.rating),
      comments: this.comments.trim()
    };
    if (!this.selectedEmployeeId || !request.period || !request.objectives || !request.comments
        || !Number.isInteger(request.rating) || request.rating < 1 || request.rating > 5) {
      this.errorMessage.set('Completa todos los campos y selecciona una valoración entre 1 y 5.');
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.reviewService.createReview(this.selectedEmployeeId, request).subscribe({
      next: (review) => {
        this.reviews.update((reviews) => [review, ...reviews]);
        this.period = '';
        this.objectives = '';
        this.rating = 3;
        this.comments = '';
        this.saving.set(false);
        this.successMessage.set('Evaluación creada y enviada al empleado para su conformidad.');
      },
      error: (error: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.errorMessage.set(error.error?.detail || 'No se pudo crear la evaluación.');
      }
    });
  }

  acknowledge(review: PerformanceReview): void {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.reviewService.acknowledge(review.id, {
      employeeComments: this.employeeComments[review.id] ?? ''
    }).subscribe({
      next: (updated) => {
        this.reviews.update((reviews) => reviews.map((item) => item.id === updated.id ? updated : item));
        this.saving.set(false);
        this.successMessage.set('Conformidad registrada. La evaluación ha quedado formalizada.');
      },
      error: (error: { error?: { detail?: string } }) => {
        this.saving.set(false);
        this.errorMessage.set(error.error?.detail || 'No se pudo formalizar la evaluación.');
      }
    });
  }

  ratingLabel(rating: number): string {
    return ['', 'Insatisfactorio', 'En desarrollo', 'Cumple expectativas', 'Supera expectativas', 'Sobresaliente'][rating] ?? '';
  }
}
