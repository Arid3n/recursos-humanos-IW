# Research & Technical Decisions: spec-001 (Alta de Empleados y Trayectoria Contractual)

**Feature**: `spec-001` · HU-01  
**Date**: 2026-10-27  
**Branch**: `spec-001`  

---

## Decision 1: Modelo Histórico Append-Only para Trayectoria Contractual (Art. 11)

### Context & Problem
El sistema debe registrar todas las variaciones de puesto, departamento, salario, convenio y responsable conservando fecha de efectos y sin sobrescribir registros previos. Se debe poder reconstruir la situación contractual a cualquier fecha dada.

### Decision
Se implementa una entidad JPA `ContractRecord` asociada a `Employee`. Cada variación genera una nueva fila *append-only*. La condición vigente a una fecha de corte $T$ se resuelve mediante una consulta JPQL optimizada con índice compuesto `(employee_id, effective_from DESC, created_at DESC)`:
```sql
SELECT c FROM ContractRecord c 
WHERE c.employee.id = :employeeId AND c.effectiveFrom <= :targetDate 
ORDER BY c.effectiveFrom DESC, c.createdAt DESC
```
Para modificaciones retroactivas, el nuevo registro se inserta con su `effective_from` pasado y la fecha de creación del sistema (`createdAt = now()`), garantizando auditoría íntegra sin mutar filas existentes.

### Alternatives Considered
- **Sobrescribir datos en la tabla Employee:** Descartado categóricamente; viola el requisito de negocio de HU-01 y el Art. 11 de la Constitución.
- **Hibernate Envers / Auditoría automática:** Descartado; añade sobrecarga de tablas sombra `_AUD` y complejidad innecesaria cuando el dominio de negocio exige gestionar la vigencia contractual explícita como entidad de primer orden (Art. 23 Simplicidad).

---

## Decision 2: Generación Segura de Credencial Provisional para Nuevo Empleado

### Context & Problem
Al dar de alta a un empleado, debe habilitarse su acceso al sistema (`User`) asignándole el rol `EMPLEADO` (o superior) sin depender de un servidor de correo saliente SMTP (acordado en clarificación Q2).

### Decision
El backend generará una contraseña aleatoria de 12 caracteres (cumpliendo entropía con mayúsculas, minúsculas, números y caracteres especiales) mediante `SecureRandom`. Esta contraseña se almacena con hash BCrypt en la tabla `users` y se retorna en texto claro **únicamente** en el DTO de respuesta `EmployeeCreationResultResponse`. El frontend muestra un diálogo modal de confirmación con la contraseña y un botón de "Copiar al portapapeles" para que el operador de RRHH se la facilite al empleado.

### Alternatives Considered
- **Contraseña elegida a mano por RRHH:** Descartada; propensa a contraseñas débiles o compartidas.
- **Activación por correo electrónico:** Descartada por complejidad y dependencia de infraestructura SMTP externa no contemplada en el entorno local/educativo.

---

## Decision 3: Control de Acceso, Validación en Profundidad y Errores RFC 9457

### Context & Problem
El alta y modificación contractual es exclusiva de usuarios con rol `RRHH`. Los errores de validación deben presentarse junto al campo correspondiente en la interfaz (HU-01 y Art. 19).

### Decision
- **Seguridad:** `@PreAuthorize("hasRole('RRHH')")` a nivel de clase/método en `EmployeeController`.
- **Validación:** DTOs inmutables (Java records) con anotaciones Jakarta Validation (`@NotBlank`, `@Email`, `@Pattern(regexp = "^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$")` para NIF, `@DecimalMin("0.0")`).
- **Mapeo de Errores RFC 9457:** El `GlobalExceptionHandler` intercepta `MethodArgumentNotValidException` y serializa `ProblemDetail` con extensión `invalidFields: Map<String, String>`, permitiendo que Angular asocie cada mensaje al `mat-error` del control reactivo.

### Alternatives Considered
- **Validaciones en el servicio con `IllegalArgumentException`:** Descartado; la validación estructural debe ser declarativa en la capa de entrada (controlador).

---

## Decision 4: Arquitectura Frontend (Angular 21 + Angular Material)

### Context & Problem
El formulario debe ser accesible, responsivo, validar en tiempo real y presentar el historial contractual ordenado cronológicamente.

### Decision
- Componente standalone `EmployeeCreateComponent` organizado en `features/employees/components/employee-create/`.
- Uso de `ReactiveFormsModule` con `FormGroup` y validadores síncronos de formato.
- Componentes de Angular Material: `mat-form-field`, `mat-input`, `mat-select` (convenio y responsable), `mat-datepicker`, `mat-button`.
- Diálogo `MatDialog` para confirmación de alta con presentación de credenciales.
- Vista de historial `ContractHistoryComponent` con `mat-table` cronológica mostrando: concepto, valor anterior, valor nuevo, fecha de efectos y auditoría.
