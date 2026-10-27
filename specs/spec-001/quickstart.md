# Quickstart & Guía de Validación: spec-001 (Alta de Empleados y Trayectoria Contractual)

**Feature**: `spec-001` · HU-01  
**Date**: 2026-10-27  

---

## 1. Requisitos Previos

- **Java**: OpenJDK 21 LTS instalado (`java -version`).
- **Maven**: `mvnw` (Spring Boot Maven Wrapper).
- **Docker**: Docker Desktop o Docker Engine activo (requerido para **Testcontainers** con PostgreSQL real en pruebas de integración, Art. 22).
- **Node.js**: v20+ y NPM para ejecutar los tests del frontend (`npm test`).

---

## 2. Escenarios de Validación Automatizada

### 2.1. Backend (Pruebas con Testcontainers)
Ejecutar la suite de pruebas del módulo de empleados:
```powershell
cd backend
./mvnw test -Dtest=*Employee*Test
```

Escenarios probados en backend:
1. **`testCreateEmployee_Success`**: Alta válida con generación de `User`, `ContractRecord` inicial y credencial provisional.
2. **`testCreateEmployee_ValidationErrors`**: Error `400 Bad Request` con RFC 9457 (`invalidFields`) al omitir NIF, puesto o salario.
3. **`testAddContractVariation_AppendOnly_Retroactive`**: Añadir variación contractual con fecha de efectos pasada; se comprueba que el registro previo sigue intacto y que el nuevo se inserta con fecha actual de auditoría y efectos pasados.
4. **`testFindFullHistoryChronological`**: Comprobación de que la lista histórica se devuelve ordenada cronológicamente por `effectiveFrom ASC`.
5. **`testCreateEmployee_ForbiddenForNonHR`**: Test de autorización negativa (Art. 10): el endpoint devuelve `403 Forbidden` si se invoca con token de rol `EMPLEADO` o `RESPONSABLE_EQUIPO`.

### 2.2. Frontend (Pruebas Unitarias y de Componentes)
Ejecutar los tests con Vitest:
```powershell
cd frontend
npm test
```

Escenarios probados en frontend:
1. **`EmployeeCreateComponent`**: Renderiza campos obligatorios con validación reactiva en cliente; al introducir un salario negativo o NIF inválido muestra `mat-error`.
2. **`PasswordDialogComponent`**: Muestra la clave autogenerada recibida tras el alta exitosa con botón para copiar al portapapeles.
3. **`ContractHistoryComponent`**: Renderiza la tabla cronológica de variaciones con columnas de concepto, fecha de efectos y valores.

---

## 3. Verificación Manual End-to-End

1. Iniciar los servicios con Docker Compose o en local:
   ```powershell
   # Desde el directorio raíz
   docker compose up -d postgres
   cd backend; ./mvnw spring-boot:run
   # En otra terminal
   cd frontend; npm start
   ```
2. Acceder a `http://localhost:4200` e iniciar sesión con un usuario con rol `RRHH`.
3. Navegar a **"Gestión de Empleados" → "Nuevo Empleado"**.
4. Rellenar los campos obligatorios y pulsar **"Guardar"**. Verificar que aparece el modal con la contraseña provisional.
5. Acceder a la ficha del empleado recién creado y pulsar **"Añadir variación contractual"** introduciendo una fecha de efectos y un nuevo salario.
6. Consultar la pestaña **"Historial Contractual"** y comprobar que ambos registros aparecen en orden cronológico sin pérdida de información.
