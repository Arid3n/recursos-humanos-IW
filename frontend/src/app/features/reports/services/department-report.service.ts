import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DepartmentReportResponse, ReportType } from '../models/department-report.models';

@Injectable({
  providedIn: 'root'
})
export class DepartmentReportService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/reports';

  getReport(type: ReportType, startDate: string, endDate: string): Observable<DepartmentReportResponse> {
    const params = new HttpParams()
      .set('type', type)
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<DepartmentReportResponse>(this.apiUrl, { params });
  }

  exportCsv(type: ReportType, startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('type', type)
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get(this.apiUrl + '/export', { params, responseType: 'blob' });
  }
}
