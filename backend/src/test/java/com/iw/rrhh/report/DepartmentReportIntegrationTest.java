package com.iw.rrhh.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.CollectiveAgreementRepository;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.report.dto.DepartmentReportResponse;
import com.iw.rrhh.report.dto.DepartmentReportRow;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class DepartmentReportIntegrationTest {

    private static final String REPORT_URL = "/reports";
    private static final String HR_USERNAME = "report.hr";
    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate END = LocalDate.of(2026, 11, 3);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CollectiveAgreementRepository agreementRepository;

    @Autowired
    private ContractRecordRepository contractRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("HU-07: informe y CSV contienen los mismos totales y el CSV escapa caracteres RFC 4180")
    void vacationReportAndCsvShareDepartmentAndEmployeeTotals() throws Exception {
        String departmentName = "I+D, \"España\"";
        Employee employee = employee(departmentName);
        employee(departmentName);
        approvedRequest(employee, LeaveType.VACATION, START, END, 2);

        String json = mockMvc.perform(get(REPORT_URL)
                        .param("type", "VACATION_USAGE")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString())
                        .with(asHr()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        DepartmentReportResponse report = objectMapper.readValue(json, DepartmentReportResponse.class);
        DepartmentReportRow department = report.departments().stream()
                .filter(row -> row.department().equals(departmentName))
                .findFirst()
                .orElseThrow();

        assertTrue(report.hasData());
        assertEquals(2, department.headcount());
        assertEquals(new BigDecimal("44"), department.totalEntitledDays());
        assertEquals(new BigDecimal("2"), department.totalTakenDays());
        assertEquals(new BigDecimal("42"), department.totalRemainingDays());
        assertEquals(new BigDecimal("4.55"), department.metricPercentage());
        assertEquals(2, department.employees().size());
        assertTrue(department.employees().stream().anyMatch(row -> row.employeeId().equals(employee.getId())));

        byte[] csv = mockMvc.perform(get(REPORT_URL + "/export")
                        .param("type", "VACATION_USAGE")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString())
                        .with(asHr()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertEquals(0xEF, csv[0] & 0xFF);
        assertEquals(0xBB, csv[1] & 0xFF);
        assertEquals(0xBF, csv[2] & 0xFF);
        String csvText = new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("\"Días correspondientes\""));
        assertTrue(csvText.contains(",\"I+D, \"\"España\"\"\",\"Departamento\","));
        assertTrue(csvText.contains("\"44\",\"2\",\"42\",\"4.55\""));
        assertTrue(csvText.contains("\"" + employee.getFirstName() + " " + employee.getLastName() + "\""));
    }

    @Test
    @DisplayName("HU-07: prorratea solicitudes que cruzan el límite del periodo")
    void absenteeismClipsApprovedRequestToSelectedPeriod() throws Exception {
        Employee employee = employee("Auditoría " + UUID.randomUUID());
        approvedRequest(
                employee,
                LeaveType.PERMIT,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3);

        String json = mockMvc.perform(get(REPORT_URL)
                        .param("type", "ABSENTEEISM")
                        .param("startDate", "2026-11-03")
                        .param("endDate", "2026-11-03")
                        .with(asHr()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        DepartmentReportResponse report = objectMapper.readValue(json, DepartmentReportResponse.class);
        DepartmentReportRow department = report.departments().stream()
                .filter(row -> row.department().startsWith("Auditoría "))
                .findFirst()
                .orElseThrow();

        assertEquals(new BigDecimal("1"), department.totalEntitledDays());
        assertEquals(new BigDecimal("1"), department.totalTakenDays());
        assertEquals(new BigDecimal("100.00"), department.metricPercentage());
    }

    @Test
    @DisplayName("HU-07: informa explícitamente cuando el periodo no tiene registros aprobados")
    void returnsEmptyReportWhenPeriodHasNoApprovedRecords() throws Exception {
        String json = mockMvc.perform(get(REPORT_URL)
                        .param("type", "ABSENTEEISM")
                        .param("startDate", "2040-01-01")
                        .param("endDate", "2040-12-31")
                        .with(asHr()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        DepartmentReportResponse report = objectMapper.readValue(json, DepartmentReportResponse.class);

        assertFalse(report.hasData());
        assertTrue(report.departments().isEmpty());

        mockMvc.perform(get(REPORT_URL + "/export")
                        .param("type", "ABSENTEEISM")
                        .param("startDate", "2040-01-01")
                        .param("endDate", "2040-12-31")
                        .with(asHr()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("HU-07: solo RRHH puede consultar y exportar informes")
    void blocksReportAndExportForOtherRoles() throws Exception {
        for (String role : new String[] {"EMPLEADO", "RESPONSABLE_EQUIPO"}) {
            RequestPostProcessor nonHr = user("report." + role.toLowerCase()).roles(role);
            mockMvc.perform(get(REPORT_URL)
                            .param("type", "ABSENTEEISM")
                            .param("startDate", START.toString())
                            .param("endDate", END.toString())
                            .with(nonHr))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(REPORT_URL + "/export")
                            .param("type", "ABSENTEEISM")
                            .param("startDate", START.toString())
                            .param("endDate", END.toString())
                            .with(nonHr))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("HU-07: rechaza un rango de fechas invertido")
    void rejectsEndDateBeforeStartDate() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                        .param("type", "ABSENTEEISM")
                        .param("startDate", END.toString())
                        .param("endDate", START.toString())
                        .with(asHr()))
                .andExpect(status().isBadRequest());
    }

    private Employee employee(String department) {
        String suffix = UUID.randomUUID().toString();
        User user = userRepository.save(new User(
                "report." + suffix + "@empresa.test",
                "not-used",
                UserRole.EMPLEADO));
        Employee employee = new Employee();
        employee.setUser(user);
        employee.setFirstName("Ana");
        employee.setLastName("Informe " + suffix.substring(0, 8));
        employee.setNationalId("R" + suffix.substring(0, 20));
        employee.setEmail(user.getUsername());
        employee.setHireDate(LocalDate.of(2020, 1, 1));
        employee = employeeRepository.save(employee);

        CollectiveAgreement agreement = agreementRepository.findAll().stream()
                .filter(candidate -> candidate.getCode().equals("GENERAL"))
                .findFirst()
                .orElseThrow();
        contractRecordRepository.save(new ContractRecord(
                employee,
                LocalDate.of(2020, 1, 1),
                "Analista",
                department,
                new BigDecimal("30000.00"),
                agreement,
                null,
                "HU-07 test"));
        return employee;
    }

    private void approvedRequest(
            Employee employee,
            LeaveType type,
            LocalDate startDate,
            LocalDate endDate,
            int days) {
        LeaveRequest request = new LeaveRequest(employee, type, startDate, endDate, days);
        request.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(request);
    }

    private static RequestPostProcessor asHr() {
        return user(HR_USERNAME).roles(UserRole.RRHH.name());
    }
}
