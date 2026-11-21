# Resultado observado de los recorridos E2E

- **Fecha de ejecucion**: 2026-11-21 19:14
- **Version del codigo**: `662da3900231139dfc63acecfb2838105644da61` (rama `memoria-y-entregables`); sin cambios en `backend/` ni `frontend/` respecto al commit
- **Entorno**: backend Spring Boot en http://localhost:8091/api/v1 sobre PostgreSQL 16 limpio (base `rrhh_e2e`, migraciones Flyway V1 a V7 y datos semilla); peticiones HTTP directas con `docs/evidencias/e2e/run_recorridos.py`. No se uso la interfaz grafica.
- **Datos de prueba**: usuarios semilla `rrhh@`, `direccion@`, `responsable@` (Carlos Ruiz Navarro) y `empleado@` (Laura Martinez Gomez), contrasena `admin123`; Pedro (`pedro.e2e@empresa.com`) se crea en el recorrido 2 por RRHH. Fechas en 2027 para evitar el bloqueo por fechas pasadas.

## Recorrido 1: ciclo satisfactorio (HU-01 a HU-08)

**Resultado: 17/17 pasos conformes.**

| Paso | Actor | HU | Accion | Resultado esperado | Resultado observado | Estado |
| :-: | :-- | :-: | :-- | :-- | :-- | :-: |
| 1 | Laura (EMPLEADO) | HU-02 | Consulta saldo 2027 | 22 correspondientes, 0 pendientes, 22 disponibles | HTTP 200; entitled=22 spent=0 pending=0 available=22 | OK |
| 2 | Laura | HU-03 | Consulta elegibilidad | canRequest=true | HTTP 200; canRequest=True | OK |
| 3 | Laura | HU-03 | Calcula dias 2027-07-05..2027-07-16 | 10 dias laborables | HTTP 200; workingDays=10 | OK |
| 4 | Laura | HU-03 | Crea solicitud VACATION | 201, PENDING, 10 dias | HTTP 201; id=ac3753e4-cced-4de2-9c45-4e8c07e936ce status=PENDING calculatedDays=10 | OK |
| 5 | Laura | HU-04 | Lista Mis Solicitudes | contiene la solicitud en PENDING | HTTP 200; 1 solicitud(es) | OK |
| 6 | Laura | HU-02 | Consulta saldo | pendientes 10 y disponibles 22 (el saldo disponible no descuenta los pendientes; la reserva se aplica al validar nuevas solicitudes) | HTTP 200; spent=0 pending=10 available=22 | OK |
| 7 | Carlos (RESPONSABLE) | HU-05 | Bandeja del equipo | contiene la solicitud de Laura | HTTP 200; 1 solicitud(es) pendientes | OK |
| 8 | Carlos | HU-06 | Calendario 2027-07 (pendiente) | aparece la ausencia de Laura con estado PENDING | HTTP 200; entrada PENDING de Laura presente | OK |
| 9 | Carlos | HU-05 | Aprueba con comentario | 200, APPROVED | HTTP 200; status=APPROVED | OK |
| 10 | Laura | HU-05 | Notificaciones | hay aviso de aprobacion | HTTP 200; 1 notificacion(es), una de aprobacion | OK |
| 11 | Laura | HU-02 | Consulta saldo | disfrutados 10, pendientes 0, disponibles 12 | HTTP 200; spent=10 pending=0 available=12 | OK |
| 12 | Laura | HU-04 | Lista Mis Solicitudes | estado APPROVED | HTTP 200; estado APPROVED | OK |
| 13 | Carlos | HU-06 | Calendario 2027-07 (aprobada) | ausencia con estado APPROVED | HTTP 200; entrada APPROVED presente | OK |
| 14 | Marta (RRHH) | HU-07 | Informe VACATION_USAGE 2027 | incluye Tecnologia con los 10 dias | HTTP 200; departamento Tecnologia con 10 dias | OK |
| 15 | Marta | HU-07 | Exporta CSV | 200, UTF-8 con BOM y mismo departamento | HTTP 200; 959 bytes, BOM presente | OK |
| 16 | Carlos | HU-08 | Crea evaluacion de Laura | 201, PENDING_ACKNOWLEDGMENT | HTTP 201; id=af9d5a2f-7609-4fc0-950a-a01a589bf241 status=PENDING_ACKNOWLEDGMENT | OK |
| 17 | Laura | HU-08 | Da conformidad | 200, FINALIZED con finalizedAt | HTTP 200; status=FINALIZED finalizedAt=2026-11-21T18:14:35.854350777Z | OK |

## Recorrido 2: errores, permisos y estados

**Resultado: 32/32 pasos conformes.**

| Paso | Actor | HU | Accion | Resultado esperado | Resultado observado | Estado |
| :-: | :-- | :-: | :-- | :-- | :-- | :-: |
| 1 | Marta (RRHH) | HU-01 | Alta sin responsable ni indicador withoutManager | 400 | HTTP 400; rechazada | OK |
| 2 | Marta | HU-01 | Alta de Pedro con responsable Carlos | 201 y credencial provisional | HTTP 201; empleado creado | OK |
| 3 | Pedro | HU-01 | Inicia sesion con la clave provisional | 200 y rol EMPLEADO | HTTP 200; respuesta de /auth/me: {"id": "9869f9e4-273b-4ed1-932e-2fc1567eae18", "username": "pedro.e2e@empresa.co | OK |
| 4 | Pedro | HU-02 | Consulta saldo 2027 | 22 disponibles | HTTP 200; entitled=22 available=22 | OK |
| 5 | Pedro | HU-03 | PERMIT sin justificacion | 400 (justificacion obligatoria) | HTTP 400; rechazada | OK |
| 6 | Pedro | HU-03 | VACATION con inicio en el pasado | 400 (fecha pasada) | HTTP 400; rechazada | OK |
| 7 | Pedro | HU-03 | VACATION que supera el saldo (2027-01-04..2027-12-31) | 409 saldo insuficiente (contrato HU-03: 409 = saldo insuficiente o solapamiento) | HTTP 409; rechazada | OK |
| 8 | Pedro | HU-03 | PERMIT 2027-03-10..12 con justificacion | 201, PENDING | HTTP 201; id=61bdd98a-64f6-49ff-8d87-1cf9e632a89f status=PENDING days=3 | OK |
| 9 | Pedro | HU-03 | PERMIT solapado | 409 solapamiento | HTTP 409; rechazada | OK |
| 10 | Pedro | HU-02 | Saldo de vacaciones tras el permiso | sigue en 22 (el permiso no descuenta) | HTTP 200; available=22 | OK |
| 11 | Laura (EMPLEADO) | HU-02 | Intenta ver el saldo de Pedro | 403 | HTTP 403; denegado | OK |
| 12 | Laura | HU-04 | Mis Solicitudes no contiene las de Pedro | no aparece la solicitud de Pedro | HTTP 200; aislamiento correcto | OK |
| 13 | Laura | HU-05 | Intenta aprobar la solicitud de Pedro | 403 | HTTP 403; denegado | OK |
| 14 | Carlos | HU-05 | Bandeja del equipo | contiene el permiso de Pedro | HTTP 200; presente | OK |
| 15 | Carlos | HU-05 | Rechaza con motivo de 3 caracteres | 400 (minimo 5) | HTTP 400; rechazada | OK |
| 16 | Carlos | HU-05 | Rechaza con motivo valido | 200, REJECTED | HTTP 200; status=REJECTED | OK |
| 17 | Carlos | HU-05 | Rechaza de nuevo la misma solicitud | 409 ya resuelta | HTTP 409; rechazada | OK |
| 18 | Pedro | HU-05 | Notificaciones | aviso de rechazo con el motivo | HTTP 200; motivo presente | OK |
| 19 | Pedro | HU-04 | Mis Solicitudes | REJECTED con rejectionReason | HTTP 200; estado y motivo visibles | OK |
| 20 | Carlos | HU-06 | Calendario 2027-03 | ningun dia de marzo con ausencias (la rechazada no aparece) | HTTP 200; absentCount=0 en todos los dias | OK |
| 21 | Pedro | HU-03 | VACATION 2027-12-22..24 | 201, PENDING | HTTP 201; id=ff1912b6-84ff-4b37-a4aa-3d80c1a4ba8f days=3 | OK |
| 22 | Pedro | HU-04 | Cancela la solicitud | 204 sin contenido (contrato HU-04) | HTTP 204; cancelada | OK |
| 23 | Pedro | HU-04 | Mis Solicitudes tras cancelar | la solicitud figura como CANCELLED | HTTP 200; estado CANCELLED | OK |
| 24 | Pedro | HU-02 | Saldo tras cancelar | 22 disponibles, 0 pendientes | HTTP 200; pending=0 available=22 | OK |
| 25 | Pedro | HU-04 | Cancela una solicitud ya rechazada | 409 o 422 (no cancelable) | HTTP 409; rechazada | OK |
| 26 | Dirección General | HU-03 | Intenta crear solicitud | 422 (no solicita ausencias) | HTTP 422; rechazada | OK |
| 27 | Pedro (EMPLEADO) | HU-07 | Intenta pedir informe | 403 | HTTP 403; denegado | OK |
| 28 | Marta | HU-07 | Informe ABSENTEEISM de marzo 2027 | 200 sin dias de Pedro | HTTP 200; sin absentismo de Pedro | OK |
| 29 | Carlos | HU-08 | Crea evaluacion de Pedro | 201, PENDING_ACKNOWLEDGMENT | HTTP 201; id=d6b7d3d8-ecdd-4714-8335-dcfff9a89d88 | OK |
| 30 | Carlos | HU-08 | Valoracion fuera de rango (6) | 400 | HTTP 400; rechazada | OK |
| 31 | Pedro | HU-08 | Da conformidad | 200, FINALIZED | HTTP 200; status=FINALIZED | OK |
| 32 | Pedro | HU-08 | Repite la conformidad sobre evaluacion FINALIZED | 409 (inmutable) | HTTP 409; rechazada | OK |

