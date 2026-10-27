export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  nationalId: string;
  email: string;
  managerId?: string | null;
  hireDate: string; // YYYY-MM-DD
  position: string;
  department: string;
  salary: number;
  collectiveAgreementId: number;
}

export interface CreateEmployeeResponse {
  id: string;
  email: string;
  fullName: string;
  nationalId: string;
  temporaryPassword?: string;
  contractRecordId: number;
  createdAt: string;
}

export interface CreateContractVariationRequest {
  effectiveFrom: string; // YYYY-MM-DD
  position: string;
  department: string;
  salary: number;
  collectiveAgreementId: number;
  managerId?: string | null;
}

export interface ContractHistoryItemResponse {
  id: number;
  effectiveFrom: string;
  position: string;
  department: string;
  salary: number;
  collectiveAgreementId: number;
  collectiveAgreementName: string;
  managerId?: string | null;
  managerName?: string | null;
  createdAt: string;
  createdBy: string;
}

export interface EmployeeDetailResponse {
  id: string;
  firstName: string;
  lastName: string;
  nationalId: string;
  email: string;
  active: boolean;
  hireDate: string;
  currentContract?: ContractHistoryItemResponse;
}

export interface UpdatePersonalDataRequest {
  firstName: string;
  lastName: string;
}
