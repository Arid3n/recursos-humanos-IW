import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse } from '../../leave/models/leave.models';
import { ApproveLeaveRequest, LeaveResolution, RejectLeaveRequest, TeamLeaveRequest } from '../models/team-leave.models';

@Injectable({
  providedIn: 'root'
})
export class TeamLeaveService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/leave';

  getTeamPendingRequests(page: number = 0, size: number = 10): Observable<PageResponse<TeamLeaveRequest>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<TeamLeaveRequest>>(`${this.apiUrl}/team-requests`, { params });
  }

  approve(id: string, comment: string | null = null): Observable<LeaveResolution> {
    const body: ApproveLeaveRequest = { comment };
    return this.http.post<LeaveResolution>(`${this.apiUrl}/requests/${id}/approve`, body);
  }

  reject(id: string, reason: string): Observable<LeaveResolution> {
    const body: RejectLeaveRequest = { reason };
    return this.http.post<LeaveResolution>(`${this.apiUrl}/requests/${id}/reject`, body);
  }
}
