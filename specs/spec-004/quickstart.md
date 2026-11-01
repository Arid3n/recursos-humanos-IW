# Quickstart: spec-004 — Seguimiento del Estado de Mis Solicitudes

**Feature**: `spec-004` (HU-04)  
**Date**: 2026-11-01  
**Author**: Integrante 2  

---

## 1. Requisitos Previos

- Base de datos PostgreSQL activa (`docker compose up -d`).
- Backend Spring Boot iniciado (`cd backend && ./mvnw spring-boot:run`).
- Frontend Angular en desarrollo (`cd frontend && npm start`).
- Usuario de prueba: `empleado@empresa.com` / `admin123`.

---

## 2. Escenarios de Validación Rápida

### Escenario 1: Consulta del listado paginado (HU-04.CA-1)
1. Iniciar sesión como `empleado@empresa.com`.
2. Acceder a la ruta `/leave/my-requests` (o mediante el botón "Mis Solicitudes" en la barra superior).
3. **Resultado esperado**:
   - Se muestra la tabla con las columnas: Tipo de Ausencia, Fecha Inicio, Fecha Fin, Días Laborables, Estado, Fecha Solicitud y Acciones.
   - Paginación visible si hay más de 10 registros.

### Escenario 2: Filtrado por Estado (HU-04.CA-4)
1. En la vista "Mis Solicitudes", cambiar el filtro de estado a `Pendientes`.
2. **Resultado esperado**:
   - Solo se muestran solicitudes con badge amarillo `PENDIENTE`.
3. Cambiar a `Aprobadas` o `Rechazadas`.
4. **Resultado esperado**:
   - Se refresca reactivamente mostrando únicamente las solicitudes correspondientes.

### Escenario 3: Detalle de Motivo de Rechazo (HU-04.CA-2)
1. En una solicitud con estado `RECHAZADA`, hacer clic en el botón de información o "Ver Motivo".
2. **Resultado esperado**:
   - Se abre un diálogo modal accesible mostrando el texto justificativo del rechazo.

### Escenario 4: Cancelación de Solicitud en Trámite (HU-04.CA-3)
1. Localizar una solicitud con estado `PENDIENTE`.
2. Hacer clic en el botón "Cancelar".
3. Confirmar la acción en el diálogo de confirmación.
4. **Resultado esperado**:
   - La solicitud desaparece de la tabla y se muestra notificación de éxito ("Solicitud cancelada correctamente").
   - El periodo queda liberado y no aparece como pendiente para el responsable.

### Escenario 5: Protección de Alcance y Privacidad (HU-04.CA-5 / Art. 10)
1. Intentar cancelar una solicitud perteneciente a otro empleado mediante llamada REST con credenciales de `empleado@empresa.com`.
2. **Resultado esperado**:
   - Respuesta HTTP `403 Forbidden` con ProblemDetail (RFC 9457).

### Escenario 6: Intento de Cancelar Solicitud ya Aprobada (Art. 13)
1. Intentar cancelar una solicitud con estado `APPROVED`.
2. **Resultado esperado**:
   - El botón no está disponible en la interfaz.
   - Si se fuerza la llamada a la API, devuelve HTTP `409 Conflict`.
