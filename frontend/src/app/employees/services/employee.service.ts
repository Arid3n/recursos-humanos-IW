import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateEmployeeRequest,
  CreateEmployeeResponse,
  CreateContractVariationRequest,
  ContractHistoryItemResponse,
  EmployeeDetailResponse,
  UpdatePersonalDataRequest
} from '../models/employee.models';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/v1/employees';

  createEmployee(request: CreateEmployeeRequest): Observable<CreateEmployeeResponse> {
    return this.http.post<CreateEmployeeResponse>(this.apiUrl, request);
  }

  getEmployeeDetail(id: string): Observable<EmployeeDetailResponse> {
    return this.http.get<EmployeeDetailResponse>(`${this.apiUrl}/${id}`);
  }

  listEmployees(activeOnly: boolean = true, page: number = 0, size: number = 20): Observable<PageResponse<EmployeeDetailResponse>> {
    const params = new HttpParams()
      .set('activeOnly', activeOnly)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<EmployeeDetailResponse>>(this.apiUrl, { params });
  }

  addContractVariation(employeeId: string, request: CreateContractVariationRequest): Observable<ContractHistoryItemResponse> {
    return this.http.post<ContractHistoryItemResponse>(`${this.apiUrl}/${employeeId}/contracts`, request);
  }

  getContractHistory(employeeId: string): Observable<ContractHistoryItemResponse[]> {
    return this.http.get<ContractHistoryItemResponse[]>(`${this.apiUrl}/${employeeId}/contracts`);
  }

  updatePersonalData(employeeId: string, request: UpdatePersonalDataRequest): Observable<EmployeeDetailResponse> {
    return this.http.put<EmployeeDetailResponse>(`${this.apiUrl}/${employeeId}/personal-data`, request);
  }

  deactivateEmployee(employeeId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${employeeId}`);
  }

  getMyProfile(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/me`);
  }
}
