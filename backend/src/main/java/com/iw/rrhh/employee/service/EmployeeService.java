package com.iw.rrhh.employee.service;

import com.iw.rrhh.employee.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {
    CreateEmployeeResponse createEmployee(CreateEmployeeRequest request);
    ContractHistoryItemResponse addContractVariation(UUID employeeId, CreateContractVariationRequest request);
    EmployeeDetailResponse getEmployeeDetail(UUID employeeId);
    List<ContractHistoryItemResponse> getContractHistory(UUID employeeId);
    EmployeeDetailResponse updatePersonalData(UUID employeeId, UpdatePersonalDataRequest request);
    void deactivateEmployee(UUID employeeId);
    Page<EmployeeDetailResponse> listEmployees(boolean activeOnly, Pageable pageable);
}
