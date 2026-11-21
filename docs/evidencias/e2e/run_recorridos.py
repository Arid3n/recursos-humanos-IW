#!/usr/bin/env python3
"""Ejecuta los dos recorridos E2E contra la API real y registra lo observado.

Uso: python3 -I run_recorridos.py http://localhost:8091/api/v1 salida.md
Requiere el backend arrancado sobre una base de datos limpia (usuarios semilla
rrhh@, direccion@, responsable@ y empleado@ con contrasena admin123).
No modifica el codigo de la aplicacion: solo hace peticiones HTTP.
"""
import base64, json, sys, urllib.request, urllib.error, subprocess, datetime

BASE = sys.argv[1].rstrip("/")
OUT = sys.argv[2]
PWD = "admin123"
results = []   # (recorrido, n, actor, hu, accion, esperado, observado, ok)
ctx = {}

def call(user, method, path, body=None, pwd=PWD, raw=False):
    tok = base64.b64encode(f"{user}:{pwd}".encode()).decode()
    req = urllib.request.Request(BASE + path, method=method,
        data=None if body is None else json.dumps(body).encode())
    req.add_header("Authorization", "Basic " + tok)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            data = r.read()
            code = r.status
    except urllib.error.HTTPError as e:
        data = e.read(); code = e.code
    if raw:
        return code, data
    try:
        return code, json.loads(data) if data else None
    except Exception:
        return code, data.decode("utf-8", "replace")

R = {"n": 0, "rec": 1}
def step(actor, hu, accion, esperado, res, check, resumen=None):
    code, js = res
    R["n"] += 1
    try:
        ok = bool(check(code, js))
    except Exception as ex:
        ok = False; js = f"{js} (error de comprobacion: {ex})"
    obs = f"HTTP {code}"
    if ok and resumen:
        try: obs += f"; {resumen(js)}"
        except Exception: pass
    if not ok:
        obs += "; respuesta: " + (json.dumps(js, ensure_ascii=False) if not isinstance(js, bytes) else js[:150].decode("utf-8", "replace"))[:160]
    results.append((R["rec"], R["n"], actor, hu, accion, esperado, obs, ok))
    return js

def lst(js):
    if isinstance(js, dict):
        for k in ("content", "items", "requests", "entries", "notifications"):
            if k in js: return js[k]
    return js if isinstance(js, list) else []

U = {"rrhh": "rrhh@empresa.com", "dg": "direccion@empresa.com",
     "resp": "responsable@empresa.com", "emp": "empleado@empresa.com"}

# ---------- Datos previos ----------
_, me_emp = call(U["emp"], "GET", "/employees/me")
_, me_resp = call(U["resp"], "GET", "/employees/me")
EMP_ID, RESP_ID = me_emp["id"], me_resp["id"]

# =================== RECORRIDO 1: exito ===================
S, E = "2027-07-05", "2027-07-16"
step("Laura (EMPLEADO)", "HU-02", "Consulta saldo 2027", "22 correspondientes, 0 pendientes, 22 disponibles",
     call(U["emp"], "GET", "/leave/balances/me?year=2027"),
     lambda c, j: c == 200 and j["entitledDays"] == 22 and j["pendingDays"] == 0 and j["availableDays"] == 22,
     lambda j: f"entitled={j['entitledDays']} spent={j['spentDays']} pending={j['pendingDays']} available={j['availableDays']}")
step("Laura", "HU-03", "Consulta elegibilidad", "canRequest=true",
     call(U["emp"], "GET", "/leave/requests/eligibility"), lambda c, j: c == 200 and j["canRequest"] is True,
     lambda j: f"canRequest={j['canRequest']}")
step("Laura", "HU-03", f"Calcula dias {S}..{E}", "10 dias laborables",
     call(U["emp"], "GET", f"/leave/requests/calculate-days?startDate={S}&endDate={E}"),
     lambda c, j: c == 200 and j["workingDays"] == 10, lambda j: f"workingDays={j['workingDays']}")
j = step("Laura", "HU-03", "Crea solicitud VACATION", "201, PENDING, 10 dias",
     call(U["emp"], "POST", "/leave/requests", {"leaveType": "VACATION", "startDate": S, "endDate": E, "justification": "Vacaciones de verano"}),
     lambda c, j: c == 201 and j["status"] == "PENDING" and j["calculatedDays"] == 10,
     lambda j: f"id={j['id']} status={j['status']} calculatedDays={j['calculatedDays']}")
REQ1 = j.get("id") if isinstance(j, dict) else None
step("Laura", "HU-04", "Lista Mis Solicitudes", "contiene la solicitud en PENDING",
     call(U["emp"], "GET", "/leave/my-requests"),
     lambda c, j: any(x["id"] == REQ1 and x["status"] == "PENDING" for x in lst(j)), lambda j: f"{len(lst(j))} solicitud(es)")
step("Laura", "HU-02", "Consulta saldo", "pendientes 10 y disponibles 22 (el saldo disponible no descuenta los pendientes; la reserva se aplica al validar nuevas solicitudes)",
     call(U["emp"], "GET", "/leave/balances/me?year=2027"),
     lambda c, j: j["pendingDays"] == 10 and j["availableDays"] == 22,
     lambda j: f"spent={j['spentDays']} pending={j['pendingDays']} available={j['availableDays']}")
step("Carlos (RESPONSABLE)", "HU-05", "Bandeja del equipo", "contiene la solicitud de Laura",
     call(U["resp"], "GET", "/leave/team-requests"),
     lambda c, j: any(x["id"] == REQ1 for x in lst(j)), lambda j: f"{len(lst(j))} solicitud(es) pendientes")
step("Carlos", "HU-06", "Calendario 2027-07 (pendiente)", "aparece la ausencia de Laura con estado PENDING",
     call(U["resp"], "GET", "/leave/team-calendar?month=2027-07", raw=True),
     lambda c, j: c == 200 and b"PENDING" in j and "Laura".encode() in j, lambda j: "entrada PENDING de Laura presente")
step("Carlos", "HU-05", "Aprueba con comentario", "200, APPROVED",
     call(U["resp"], "POST", f"/leave/requests/{REQ1}/approve", {"comment": "Disfruta las vacaciones"}),
     lambda c, j: c == 200 and j["status"] == "APPROVED", lambda j: f"status={j['status']}")
step("Laura", "HU-05", "Notificaciones", "hay aviso de aprobacion",
     call(U["emp"], "GET", "/notifications"),
     lambda c, j: len(lst(j)) >= 1 and "aprobad" in json.dumps(j).lower(), lambda j: f"{len(lst(j))} notificacion(es), una de aprobacion")
step("Laura", "HU-02", "Consulta saldo", "disfrutados 10, pendientes 0, disponibles 12",
     call(U["emp"], "GET", "/leave/balances/me?year=2027"),
     lambda c, j: j["spentDays"] == 10 and j["pendingDays"] == 0 and j["availableDays"] == 12,
     lambda j: f"spent={j['spentDays']} pending={j['pendingDays']} available={j['availableDays']}")
step("Laura", "HU-04", "Lista Mis Solicitudes", "estado APPROVED",
     call(U["emp"], "GET", "/leave/my-requests"),
     lambda c, j: any(x["id"] == REQ1 and x["status"] == "APPROVED" for x in lst(j)), lambda j: "estado APPROVED")
step("Carlos", "HU-06", "Calendario 2027-07 (aprobada)", "ausencia con estado APPROVED",
     call(U["resp"], "GET", "/leave/team-calendar?month=2027-07", raw=True),
     lambda c, j: c == 200 and b"APPROVED" in j, lambda j: "entrada APPROVED presente")
q = "type=VACATION_USAGE&startDate=2027-01-01&endDate=2027-12-31"
c1, rep = call(U["rrhh"], "GET", f"/reports?{q}", raw=True)
step("Marta (RRHH)", "HU-07", "Informe VACATION_USAGE 2027", "incluye Tecnologia con los 10 dias",
     (c1, rep), lambda c, j: c == 200 and b"Tecnolog" in j and b"10" in j, lambda j: "departamento Tecnologia con 10 dias")
c2, csv = call(U["rrhh"], "GET", f"/reports/export?{q}", raw=True)
step("Marta", "HU-07", "Exporta CSV", "200, UTF-8 con BOM y mismo departamento",
     (c2, csv), lambda c, j: c == 200 and j[:3] == b"\xef\xbb\xbf" and b"Tecnolog" in j, lambda j: f"{len(j)} bytes, BOM presente")
j = step("Carlos", "HU-08", "Crea evaluacion de Laura", "201, PENDING_ACKNOWLEDGMENT",
     call(U["resp"], "POST", f"/performance-reviews/team/{EMP_ID}", {"period": "2027-ANUAL", "objectives": "Objetivos cumplidos", "rating": 4, "comments": "Supera expectativas"}),
     lambda c, j: c == 201 and j["status"] == "PENDING_ACKNOWLEDGMENT", lambda j: f"id={j['id']} status={j['status']}")
REV1 = j.get("id") if isinstance(j, dict) else None
step("Laura", "HU-08", "Da conformidad", "200, FINALIZED con finalizedAt",
     call(U["emp"], "POST", f"/performance-reviews/{REV1}/acknowledge", {"employeeComments": "De acuerdo"}),
     lambda c, j: c == 200 and j["status"] == "FINALIZED" and j.get("finalizedAt"), lambda j: f"status={j['status']} finalizedAt={j.get('finalizedAt')}")

# =================== RECORRIDO 2: error, permisos y estado ===================
R["rec"] = 2; R["n"] = 0
step("Marta (RRHH)", "HU-01", "Alta sin responsable ni indicador withoutManager", "400",
     call(U["rrhh"], "POST", "/employees", {"firstName": "Pedro", "lastName": "Martin E2E", "nationalId": "33333333P", "email": "pedro.e2e@empresa.com", "hireDate": "2026-01-15", "position": "Administrativo", "department": "Administracion", "salary": 24000, "collectiveAgreementId": 1}),
     lambda c, j: c == 400, lambda j: "rechazada")
j = step("Marta", "HU-01", "Alta de Pedro con responsable Carlos", "201 y credencial provisional",
     call(U["rrhh"], "POST", "/employees", {"firstName": "Pedro", "lastName": "Martin E2E", "nationalId": "33333333P", "email": "pedro.e2e@empresa.com", "managerId": RESP_ID, "hireDate": "2026-01-15", "position": "Administrativo", "department": "Administracion", "salary": 24000, "collectiveAgreementId": 1}),
     lambda c, j: c == 201, lambda j: "empleado creado")
ctx["pedro_json"] = j
PED_PWD = None
if isinstance(j, dict):
    for k in ("provisionalPassword", "temporaryPassword", "password", "initialPassword", "generatedPassword"):
        if k in j: PED_PWD = j[k]
    ctx["pedro_keys"] = sorted(j.keys())
PED = "pedro.e2e@empresa.com"
if PED_PWD is None:
    results.append((2, 99, "—", "HU-01", "Obtener credencial provisional", "campo con la contrasena", f"claves: {ctx.get('pedro_keys')}", False))
    PED_PWD = "?"
step("Pedro", "HU-01", "Inicia sesion con la clave provisional", "200 y rol EMPLEADO",
     call(PED, "GET", "/auth/me", pwd=PED_PWD),
     lambda c, j: c == 200, lambda j: f"respuesta de /auth/me: {json.dumps(j, ensure_ascii=False)[:80]}")
_, me_ped = call(PED, "GET", "/employees/me", pwd=PED_PWD)
PED_ID = me_ped["id"] if isinstance(me_ped, dict) else None
step("Pedro", "HU-02", "Consulta saldo 2027", "22 disponibles",
     call(PED, "GET", "/leave/balances/me?year=2027", pwd=PED_PWD),
     lambda c, j: c == 200 and j["availableDays"] == 22, lambda j: f"entitled={j['entitledDays']} available={j['availableDays']}")
step("Pedro", "HU-03", "PERMIT sin justificacion", "400 (justificacion obligatoria)",
     call(PED, "POST", "/leave/requests", {"leaveType": "PERMIT", "startDate": "2027-03-10", "endDate": "2027-03-12"}, pwd=PED_PWD),
     lambda c, j: c == 400, lambda j: "rechazada")
step("Pedro", "HU-03", "VACATION con inicio en el pasado", "400 (fecha pasada)",
     call(PED, "POST", "/leave/requests", {"leaveType": "VACATION", "startDate": "2026-01-05", "endDate": "2026-01-09"}, pwd=PED_PWD),
     lambda c, j: c in (400, 422), lambda j: "rechazada")
step("Pedro", "HU-03", "VACATION que supera el saldo (2027-01-04..2027-12-31)", "409 saldo insuficiente (contrato HU-03: 409 = saldo insuficiente o solapamiento)",
     call(PED, "POST", "/leave/requests", {"leaveType": "VACATION", "startDate": "2027-01-04", "endDate": "2027-12-31"}, pwd=PED_PWD),
     lambda c, j: c == 409, lambda j: "rechazada")
j = step("Pedro", "HU-03", "PERMIT 2027-03-10..12 con justificacion", "201, PENDING",
     call(PED, "POST", "/leave/requests", {"leaveType": "PERMIT", "startDate": "2027-03-10", "endDate": "2027-03-12", "justification": "Mudanza de domicilio"}, pwd=PED_PWD),
     lambda c, j: c == 201 and j["status"] == "PENDING", lambda j: f"id={j['id']} status={j['status']} days={j['calculatedDays']}")
REQ2 = j.get("id") if isinstance(j, dict) else None
step("Pedro", "HU-03", "PERMIT solapado", "409 solapamiento",
     call(PED, "POST", "/leave/requests", {"leaveType": "PERMIT", "startDate": "2027-03-11", "endDate": "2027-03-11", "justification": "Otro"}, pwd=PED_PWD),
     lambda c, j: c == 409, lambda j: "rechazada")
step("Pedro", "HU-02", "Saldo de vacaciones tras el permiso", "sigue en 22 (el permiso no descuenta)",
     call(PED, "GET", "/leave/balances/me?year=2027", pwd=PED_PWD),
     lambda c, j: j["availableDays"] == 22, lambda j: f"available={j['availableDays']}")
step("Laura (EMPLEADO)", "HU-02", "Intenta ver el saldo de Pedro", "403",
     call(U["emp"], "GET", f"/leave/balances/employee/{PED_ID}?year=2027"), lambda c, j: c == 403, lambda j: "denegado")
step("Laura", "HU-04", "Mis Solicitudes no contiene las de Pedro", "no aparece la solicitud de Pedro",
     call(U["emp"], "GET", "/leave/my-requests"), lambda c, j: all(x["id"] != REQ2 for x in lst(j)), lambda j: "aislamiento correcto")
step("Laura", "HU-05", "Intenta aprobar la solicitud de Pedro", "403",
     call(U["emp"], "POST", f"/leave/requests/{REQ2}/approve", {}), lambda c, j: c == 403, lambda j: "denegado")
step("Carlos", "HU-05", "Bandeja del equipo", "contiene el permiso de Pedro",
     call(U["resp"], "GET", "/leave/team-requests"), lambda c, j: any(x["id"] == REQ2 for x in lst(j)), lambda j: "presente")
step("Carlos", "HU-05", "Rechaza con motivo de 3 caracteres", "400 (minimo 5)",
     call(U["resp"], "POST", f"/leave/requests/{REQ2}/reject", {"reason": "abc"}), lambda c, j: c == 400, lambda j: "rechazada")
step("Carlos", "HU-05", "Rechaza con motivo valido", "200, REJECTED",
     call(U["resp"], "POST", f"/leave/requests/{REQ2}/reject", {"reason": "Coincide con cierre contable trimestral"}),
     lambda c, j: c == 200 and j["status"] == "REJECTED", lambda j: f"status={j['status']}")
step("Carlos", "HU-05", "Rechaza de nuevo la misma solicitud", "409 ya resuelta",
     call(U["resp"], "POST", f"/leave/requests/{REQ2}/reject", {"reason": "Coincide con cierre contable trimestral"}),
     lambda c, j: c == 409, lambda j: "rechazada")
step("Pedro", "HU-05", "Notificaciones", "aviso de rechazo con el motivo",
     call(PED, "GET", "/notifications", pwd=PED_PWD),
     lambda c, j: "cierre contable" in json.dumps(j, ensure_ascii=False).lower(), lambda j: "motivo presente")
step("Pedro", "HU-04", "Mis Solicitudes", "REJECTED con rejectionReason",
     call(PED, "GET", "/leave/my-requests", pwd=PED_PWD),
     lambda c, j: any(x["id"] == REQ2 and x["status"] == "REJECTED" and x.get("rejectionReason") for x in lst(j)), lambda j: "estado y motivo visibles")
step("Carlos", "HU-06", "Calendario 2027-03", "ningun dia de marzo con ausencias (la rechazada no aparece)",
     call(U["resp"], "GET", "/leave/team-calendar?month=2027-03", raw=True),
     lambda c, j: c == 200 and all(d["absentCount"] == 0 and not d["events"] for d in json.loads(j)["days"]), lambda j: "absentCount=0 en todos los dias")
j = step("Pedro", "HU-03", "VACATION 2027-12-22..24", "201, PENDING",
     call(PED, "POST", "/leave/requests", {"leaveType": "VACATION", "startDate": "2027-12-22", "endDate": "2027-12-24"}, pwd=PED_PWD),
     lambda c, j: c == 201 and j["status"] == "PENDING", lambda j: f"id={j['id']} days={j['calculatedDays']}")
REQ3 = j.get("id") if isinstance(j, dict) else None
step("Pedro", "HU-04", "Cancela la solicitud", "204 sin contenido (contrato HU-04)",
     call(PED, "POST", f"/leave/requests/{REQ3}/cancel", pwd=PED_PWD),
     lambda c, j: c == 204, lambda j: "cancelada")
step("Pedro", "HU-04", "Mis Solicitudes tras cancelar", "la solicitud figura como CANCELLED",
     call(PED, "GET", "/leave/my-requests", pwd=PED_PWD),
     lambda c, j: any(x["id"] == REQ3 and x["status"] == "CANCELLED" for x in lst(j)), lambda j: "estado CANCELLED")
step("Pedro", "HU-02", "Saldo tras cancelar", "22 disponibles, 0 pendientes",
     call(PED, "GET", "/leave/balances/me?year=2027", pwd=PED_PWD),
     lambda c, j: j["availableDays"] == 22 and j["pendingDays"] == 0, lambda j: f"pending={j['pendingDays']} available={j['availableDays']}")
step("Pedro", "HU-04", "Cancela una solicitud ya rechazada", "409 o 422 (no cancelable)",
     call(PED, "POST", f"/leave/requests/{REQ2}/cancel", pwd=PED_PWD),
     lambda c, j: c in (409, 422), lambda j: "rechazada")
step("Dirección General", "HU-03", "Intenta crear solicitud", "422 (no solicita ausencias)",
     call(U["dg"], "POST", "/leave/requests", {"leaveType": "VACATION", "startDate": "2027-08-02", "endDate": "2027-08-06"}),
     lambda c, j: c == 422, lambda j: "rechazada")
step("Pedro (EMPLEADO)", "HU-07", "Intenta pedir informe", "403",
     call(PED, "GET", f"/reports?type=ABSENTEEISM&startDate=2027-01-01&endDate=2027-12-31", pwd=PED_PWD), lambda c, j: c == 403, lambda j: "denegado")
step("Marta", "HU-07", "Informe ABSENTEEISM de marzo 2027", "200 sin dias de Pedro",
     call(U["rrhh"], "GET", "/reports?type=ABSENTEEISM&startDate=2027-03-01&endDate=2027-03-31", raw=True),
     lambda c, j: c == 200 and b"Pedro" not in j, lambda j: "sin absentismo de Pedro")
j = step("Carlos", "HU-08", "Crea evaluacion de Pedro", "201, PENDING_ACKNOWLEDGMENT",
     call(U["resp"], "POST", f"/performance-reviews/team/{PED_ID}", {"period": "2027-ANUAL", "objectives": "Objetivos", "rating": 3, "comments": "Cumple expectativas"}),
     lambda c, j: c == 201 and j["status"] == "PENDING_ACKNOWLEDGMENT", lambda j: f"id={j['id']}")
REV2 = j.get("id") if isinstance(j, dict) else None
step("Carlos", "HU-08", "Valoracion fuera de rango (6)", "400",
     call(U["resp"], "POST", f"/performance-reviews/team/{PED_ID}", {"period": "2027-X", "objectives": "O", "rating": 6, "comments": "C"}),
     lambda c, j: c == 400, lambda j: "rechazada")
step("Pedro", "HU-08", "Da conformidad", "200, FINALIZED",
     call(PED, "POST", f"/performance-reviews/{REV2}/acknowledge", {"employeeComments": "Conforme"}, pwd=PED_PWD),
     lambda c, j: c == 200 and j["status"] == "FINALIZED", lambda j: f"status={j['status']}")
step("Pedro", "HU-08", "Repite la conformidad sobre evaluacion FINALIZED", "409 (inmutable)",
     call(PED, "POST", f"/performance-reviews/{REV2}/acknowledge", {"employeeComments": "Otra vez"}, pwd=PED_PWD),
     lambda c, j: c == 409, lambda j: "rechazada")

# ---------- Informe ----------
head = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
dirty = subprocess.run(["git", "status", "--porcelain", "backend", "frontend"], capture_output=True, text=True).stdout.strip()
now = datetime.datetime.now().strftime("%Y-%m-%d %H:%M")
lines = [f"# Resultado observado de los recorridos E2E", "",
 f"- **Fecha de ejecucion**: {now}", f"- **Version del codigo**: `{head}` (rama `memoria-y-entregables`)" + ("; hay cambios sin confirmar en `backend/` o `frontend/`" if dirty else "; sin cambios en `backend/` ni `frontend/` respecto al commit"),
 f"- **Entorno**: backend Spring Boot en {BASE} sobre PostgreSQL 16 limpio (base `rrhh_e2e`, migraciones Flyway V1 a V7 y datos semilla); peticiones HTTP directas con `docs/evidencias/e2e/run_recorridos.py`. No se uso la interfaz grafica.",
 "- **Datos de prueba**: usuarios semilla `rrhh@`, `direccion@`, `responsable@` (Carlos Ruiz Navarro) y `empleado@` (Laura Martinez Gomez), contrasena `admin123`; Pedro (`pedro.e2e@empresa.com`) se crea en el recorrido 2 por RRHH. Fechas en 2027 para evitar el bloqueo por fechas pasadas.", ""]
for rec, title in ((1, "Recorrido 1: ciclo satisfactorio (HU-01 a HU-08)"), (2, "Recorrido 2: errores, permisos y estados")):
    rs = [r for r in results if r[0] == rec]
    ok = sum(1 for r in rs if r[7])
    lines += [f"## {title}", "", f"**Resultado: {ok}/{len(rs)} pasos conformes.**", "",
              "| Paso | Actor | HU | Accion | Resultado esperado | Resultado observado | Estado |", "| :-: | :-- | :-: | :-- | :-- | :-- | :-: |"]
    for _, n, actor, hu, acc, esp, obs, k in rs:
        lines.append(f"| {n} | {actor} | {hu} | {acc} | {esp} | {obs} | {'OK' if k else 'FALLA'} |")
    lines.append("")
open(OUT, "w").write("\n".join(lines) + "\n")
tot = len(results); good = sum(1 for r in results if r[7])
print(f"{good}/{tot} pasos conformes")
for r in results:
    if not r[7]: print("FALLA", r[:2], r[4], "|", r[6])
