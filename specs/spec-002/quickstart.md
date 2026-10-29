# Quickstart & Guía de Validación: spec-002 (Consulta de Ficha y Saldo de Vacaciones)

**Feature**: `spec-002` · HU-02  
**Date**: 2026-10-29  
**Branch**: `spec-002`  

---

## 1. Requisitos Previos

- **Java**: OpenJDK 21 LTS (`java -version`).
- **Maven**: `mvnw` (Spring Boot Maven Wrapper).
- **Docker**: Docker Desktop o Docker Engine activo (para PostgreSQL con Testcontainers en pruebas de integración, Art. 22).
- **Node.js & NPM**: Node 20+ para ejecutar el frontend Angular (`npm start` o `npm test`).

---

## 2. Escenarios de Validación Automatizada

### 2.1. Backend (Pruebas Unitarias y de Integración)
Ejecutar la suite de pruebas del módulo de saldo y perfil:
```powershell
cd backend
./mvnw test -Dtest=*Leave*Test,*Profile*Test
```

Escenarios clave probados en el backend:
1. **`testCalculateLeaveBalance_FullYear`**:
   - Empleado contratado antes del año en curso con convenio de 22 días, 5 días de solicitudes aprobadas y 2 pendientes.
   - Resultado esperado: `entitledDays = 22`, `spentDays = 5`, `pendingDays = 2`, `availableDays = 17` (`22 - 5`).
2. **`testCalculateLeaveBalance_MidYearProrate`**:
   - Empleado contratado el 1 de julio en un año con convenio de 22 días.
   - Fórmula: `round((22 / 12) * 6) = 11`.
   - Resultado esperado: `entitledDays = 11`.
3. **`testCalculateLeaveBalance_ZeroAvailableWhenExhausted`**:
   - Empleado con 22 días correspondientes y 22 días disfrutados.
   - Resultado esperado: `availableDays = 0` (no permite números negativos).
4. **`testGetMyLeaveBalance_UsesSecurityContext`**:
   - Petición a `GET /api/v1/leave/balances/me` con usuario autenticado.
   - Resultado esperado: Devuelve el saldo del empleado vinculado a la sesión sin recibir IDs en la URL ni en el body (Art. 10, FR-001).
5. **`testForbiddenAccessToOtherEmployeeBalance_NegativeTest`**:
   - Empleado `A` intenta llamar a `GET /api/v1/leave/balances/employee/{id_empleado_B}`.
   - Resultado esperado: Rechazo inmediato con código HTTP `403 Forbidden` en formato RFC 9457 `ProblemDetail` (Art. 10, FR-005).
6. **`testHistoricalYearBalance_ReadOnly`**:
   - Consulta a `GET /api/v1/leave/balances/me?year=2025`.
   - Resultado esperado: `isClosedYear = true`, días calculados en base al convenio y solicitudes de dicho ejercicio histórico.

### 2.2. Frontend (Pruebas Unitarias y de Componentes)
Ejecutar las pruebas unitarias del frontend:
```powershell
cd frontend
npm test
```

Escenarios probados en frontend:
1. **`MyProfileComponent`**: Renderiza la ficha personal con datos del usuario autenticado (nombre, email, puesto, departamento, responsable).
2. **`LeaveBalanceWidgetComponent`**: Renderiza las 4 tarjetas de métricas (Correspondientes, Disfrutados, Disponibles y En trámite). Verifica que las etiquetas cumplen con accesibilidad WCAG 2.1 AA (Art. 21).
3. **Selector de año reactivo**: Al cambiar el año en el `mat-select`, se dispara la recarga del saldo y muestra el distintivo de "Año cerrado" si aplica.

---

## 3. Verificación Manual End-to-End

1. **Iniciar el entorno local**:
   ```powershell
   # 1. Base de datos PostgreSQL
   docker compose up -d postgres
   
   # 2. Backend Spring Boot (en una terminal)
   cd backend
   ./mvnw spring-boot:run
   
   # 3. Frontend Angular (en otra terminal)
   cd frontend
   npm start
   ```

2. **Acceso como Empleado**:
   - Abrir el navegador en `http://localhost:4200/`.
   - Iniciar sesión con un usuario con rol `EMPLEADO`.
   - Hacer clic en el menú **"Mi Perfil"** o **"Mis Vacaciones"** en la barra de navegación superior.
   - Comprobar que:
     - Se muestran correctamente los datos personales y contractuales.
     - En el bloque de saldo se visualizan las 4 tarjetas: Correspondientes (según convenio), Disfrutados, Disponibles y En trámite.
     - La suma `Disfrutados + Disponibles == Correspondientes` es exacta al 100%.

3. **Prueba de Inmutabilidad e Inmediatez**:
   - En una sesión como `RRHH` o `RESPONSABLE_EQUIPO`, registrar o aprobar una solicitud de vacaciones del empleado.
   - Regresar a la pantalla del empleado y refrescar: el saldo disponible disminuye automáticamente y el contador de disfrutados se incrementa en el número exacto de días aprobados, sin desfases.

4. **Prueba Negativa de Seguridad**:
   - Como empleado autenticado, intentar consultar la URL directa del saldo de otro compañero.
   - Comprobar que la aplicación muestra un mensaje de acceso denegado (403) y no revela información ajena.

