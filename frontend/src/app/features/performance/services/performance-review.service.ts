import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AcknowledgePerformanceReviewRequest,
  CreatePerformanceReviewRequest,
  PerformanceReview,
  PerformanceReviewEmployee
} from '../models/performance-review.models';

@Injectable({
  providedIn: 'root'
})
export class PerformanceReviewService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/performance-reviews';

  getMyReviews(): Observable<PerformanceReview[]> {
    return this.http.get<PerformanceReview[]>(`${this.apiUrl}/me`);
  }

  getTeamReviews(): Observable<PerformanceReview[]> {
    return this.http.get<PerformanceReview[]>(`${this.apiUrl}/team`);
  }

  getTeamEmployees(): Observable<PerformanceReviewEmployee[]> {
    return this.http.get<PerformanceReviewEmployee[]>(`${this.apiUrl}/team/employees`);
  }

  createReview(employeeId: string, request: CreatePerformanceReviewRequest): Observable<PerformanceReview> {
    return this.http.post<PerformanceReview>(`${this.apiUrl}/team/${employeeId}`, request);
  }

  acknowledge(reviewId: string, request: AcknowledgePerformanceReviewRequest): Observable<PerformanceReview> {
    return this.http.post<PerformanceReview>(`${this.apiUrl}/${reviewId}/acknowledge`, request);
  }
}
