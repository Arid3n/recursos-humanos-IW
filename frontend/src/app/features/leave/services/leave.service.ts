import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaveBalanceResponse, LeaveSummaryItem } from '../models/leave.models';

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
}

