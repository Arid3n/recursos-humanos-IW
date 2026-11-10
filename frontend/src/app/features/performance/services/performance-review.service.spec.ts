import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PerformanceReviewService } from './performance-review.service';

describe('PerformanceReviewService', () => {
  let service: PerformanceReviewService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PerformanceReviewService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses role-scoped endpoints for team and employee reviews', () => {
    service.getTeamEmployees().subscribe();
    http.expectOne('/api/v1/performance-reviews/team/employees').flush([]);

    service.createReview('employee-id', {
      period: '2026-ANUAL',
      objectives: 'Objetivos',
      rating: 4,
      comments: 'Comentarios'
    }).subscribe();
    const create = http.expectOne('/api/v1/performance-reviews/team/employee-id');
    expect(create.request.method).toBe('POST');
    expect(create.request.body.rating).toBe(4);
    create.flush({});

    service.getMyReviews().subscribe();
    http.expectOne('/api/v1/performance-reviews/me').flush([]);

    service.acknowledge('review-id', { employeeComments: 'Conforme' }).subscribe();
    const acknowledge = http.expectOne('/api/v1/performance-reviews/review-id/acknowledge');
    expect(acknowledge.request.method).toBe('POST');
    expect(acknowledge.request.body.employeeComments).toBe('Conforme');
    acknowledge.flush({});
  });
});
