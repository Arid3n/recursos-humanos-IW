# Quickstart & Guía de Validación: spec-003 (Solicitud de Vacaciones o Permisos)

**Feature**: `spec-003` · HU-03  
**Date**: 2026-11-01  
**Branch**: `spec-003`  

---

## 1. Requisitos Previos

- **Java**: OpenJDK 21 LTS (`java -version`).
- **Maven**: `mvnw` (Spring Boot Maven Wrapper en `backend/`).
- **Docker**: Docker activo para pruebas de integración con PostgreSQL real vía Testcontainers (Art. 8, Art. 22).
- **Node.js & NPM**: Node 20+ para la ejecución y pruebas de Angular en `frontend/`.

---

## 2. Escenarios de Validación Automatizada

### 2.1. Backend (Pruebas Unitarias y de Integración)
Ejecutar la suite de pruebas del módulo de solicitud de ausencias:
```powershell
cd backend
./mvnw test -Dtest=*LeaveRequest*Test,*Holiday*Test
```

Escenarios clave probados en el backend:
1. **`testCalculateWorkingDays_ExcludesWeekendsAndHolidays`**:
   - Selección de un rango de 2 semanas (10 días de lunes a viernes).
   - Coincide 1 festivo oficial en el intervalo.
   - Resultado esperado: `workingDays = 9`, `weekendDays = 4`, `holidayDays = 1`.
2. **`testCreateLeaveRequest_VacationSuccess`**:
   - Empleado con 15 días disponibles solicita 5 días laborables de tipo `VACATION`.
   - Resultado esperado: HTTP `201 Created`, estado `PENDING`, `calculatedDays = 5`, asociado al `manager` del empleado.
3. **`testCreateLeaveRequest_InsufficientBalance_Rejected`**:
   - Empleado con 2 días disponibles intenta solicitar 5 días laborables de `VACATION`.
   - Resultado esperado: HTTP `409 Conflict` (o `400 Bad Request`) con RFC 9457 `ProblemDetail` explicando el saldo disponible vs requerido. La solicitud no se persiste en BD (FR-004).
4. **`testCreateLeaveRequest_PermitDoesNotDeductVacation`**:
   - Empleado solicita un `PERMIT` de 3 días laborables.
   - Resultado esperado: HTTP `201 Created`, estado `PENDING`, sin exigir saldo de vacaciones ordinarias ni alterar `availableDays` (FR-008).
5. **`testCreateLeaveRequest_OverlappingDates_Rejected`**:
   - Empleado tiene una solicitud activa del 10 al 15 de julio. Intenta crear otra del 12 al 18 de julio.
   - Resultado esperado: HTTP `409 Conflict`, mensaje de solapamiento detectado, 0 inserciones en BD (FR-007, SC-002).
6. **`testCreateLeaveRequest_EndDateBeforeStartDate_Rejected`**:
   - `startDate = 2026-07-15`, `endDate = 2026-07-10`.
   - Resultado esperado: HTTP `400 Bad Request` con error en campo `endDate` (FR-005, SC-004).
7. **`testCreateLeaveRequest_ZeroWorkingDays_Rejected`**:
   - Selección de sábado y domingo exclusivamente (`calculatedDays = 0`).
   - Resultado esperado: HTTP `400 Bad Request` indicando que el intervalo carece de días laborables.

### 2.2. Frontend (Pruebas Unitarias y de Componentes)
Ejecutar las pruebas en el frontend:
```powershell
cd frontend
npm test -- --include=**/leave-request-create*.spec.ts
```

Escenarios probados en frontend:
1. **`LeaveRequestCreateComponent`**:
   - Formulario reactivo inicializa con tipo por defecto `VACATION`.
   - Al seleccionar rango de fechas, se emite petición con debounce al endpoint de cálculo de días hábiles y actualiza el indicador numérico.
   - Si la fecha de inicio dista menos de 7 días, renderiza banner informativo de recomendación sin bloquear el botón.
   - Si los días superan el saldo disponible del widget, deshabilita el botón de envío y muestra error accesible.

---

## 3. Verificación Manual Rápida (Endpoints HTTP)

### 3.1. Cómputo previo de días hábiles:
```bash
curl -X GET "http://localhost:8080/api/v1/leave/requests/calculate-days?startDate=2026-07-06&endDate=2026-07-10" \
     -H "Authorization: Bearer <TOKEN>"
```
*Respuesta esperada:*
```json
{
  "startDate": "2026-07-06",
  "endDate": "2026-07-10",
  "workingDays": 5,
  "calendarDays": 5,
  "weekendDays": 0,
  "holidayDays": 0
}
```

### 3.2. Envío de solicitud válida:
```bash
curl -X POST "http://localhost:8080/api/v1/leave/requests" \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d '{
       "leaveType": "VACATION",
       "startDate": "2026-07-06",
       "endDate": "2026-07-10"
     }'
```
*Respuesta esperada:* `201 Created` con objeto `LeaveRequestResponse` en estado `PENDING`.
