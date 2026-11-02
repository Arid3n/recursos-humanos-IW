import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LeaveBalanceResponse,
  LeaveSummaryItem,
  CreateLeaveRequest,
  LeaveRequestResponse,
  WorkingDaysCalculationResponse,
  LeaveRequestSummary,
  PageResponse,
  LeaveStatus
} from '../models/leave.models';

@Injectable({
  providedIn: 'root'
})
export class LeaveService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/leave';

  getMyLeaveBalance(year?: number): Observable<LeaveBalanceResponse> {
    let params = new HttpParams();
    if (year != null) {
      params = params.set('year', year.toString());
    }
    return this.http.get<LeaveBalanceResponse>(`${this.apiUrl}/balances/me`, { params });
  }

  getEmployeeLeaveBalance(employeeId: string, year?: number): Observable<LeaveBalanceResponse> {
    let params = new HttpParams();
    if (year != null) {
      params = params.set('year', year.toString());
    }
    return this.http.get<LeaveBalanceResponse>(`${this.apiUrl}/balances/employee/${employeeId}`, { params });
  }

  getMyLeaveRequests(year?: number): Observable<LeaveSummaryItem[]> {
    let params = new HttpParams();
    if (year != null) {
      params = params.set('year', year.toString());
    }
    return this.http.get<LeaveSummaryItem[]>(`${this.apiUrl}/requests/me`, { params });
  }

  calculateWorkingDays(startDate: string, endDate: string): Observable<WorkingDaysCalculationResponse> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<WorkingDaysCalculationResponse>(`${this.apiUrl}/requests/calculate-days`, { params });
  }

  createLeaveRequest(request: CreateLeaveRequest): Observable<LeaveRequestResponse> {
    return this.http.post<LeaveRequestResponse>(`${this.apiUrl}/requests`, request);
  }

  getMyRequests(page: number = 0, size: number = 10, status?: LeaveStatus): Observable<PageResponse<LeaveRequestSummary>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<PageResponse<LeaveRequestSummary>>(`${this.apiUrl}/my-requests`, { params });
  }

  cancelRequest(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/requests/${id}/cancel`, {});
  }
}

