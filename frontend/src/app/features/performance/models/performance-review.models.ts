export type PerformanceReviewStatus = 'PENDING_ACKNOWLEDGMENT' | 'FINALIZED';

export interface PerformanceReview {
  id: string;
  employeeId: string;
  employeeName: string;
  evaluatorName: string;
  period: string;
  objectives: string;
  rating: number;
  comments: string;
  employeeComments: string | null;
  status: PerformanceReviewStatus;
  createdAt: string;
  employeeAcknowledgedAt: string | null;
  finalizedAt: string | null;
}

export interface PerformanceReviewEmployee {
  id: string;
  fullName: string;
}

export interface CreatePerformanceReviewRequest {
  period: string;
  objectives: string;
  rating: number;
  comments: string;
}

export interface AcknowledgePerformanceReviewRequest {
  employeeComments: string;
}
