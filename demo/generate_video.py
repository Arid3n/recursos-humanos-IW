"""
Generador de Vídeo Demo — Sistema RRHH SDD G01
================================================
Genera demo-rrhh.mp4 con cartelas profesionales simulando cada escenario de las 8 HU.

Requisitos: pillow, imageio, imageio-ffmpeg, numpy
  pip install pillow imageio imageio-ffmpeg numpy

Uso:
  cd demo
  python generate_video.py
"""

import numpy as np
import imageio
import textwrap
import os
import sys
from PIL import Image, ImageDraw, ImageFont

# ─── Configuración General ────────────────────────────────────────────────────
OUT_FILE   = os.path.join(os.path.dirname(__file__), "demo-rrhh.mp4")
FPS        = 25
W, H       = 1280, 720
FONT_DIR   = os.path.join(os.environ.get("WINDIR", "C:/Windows"), "Fonts")

# ─── Paleta de Colores ───────────────────────────────────────────────────────
BG         = (15, 17, 23)
SURFACE    = (26, 29, 39)
SURFACE2   = (34, 38, 58)
BORDER     = (46, 51, 80)
ACCENT     = (99, 102, 241)
ACCENT2    = (129, 140, 248)
GREEN      = (34, 197, 94)
RED        = (239, 68, 68)
AMBER      = (245, 158, 11)
WHITE      = (248, 250, 252)
MUTED      = (148, 163, 184)
LIGHT_BLUE = (125, 211, 252)

# ─── Colores por integrante ───────────────────────────────────────────────────
HU_COLORS = {
    "HU-01": (30, 58, 95),   "HU-02": (30, 58, 95),
    "HU-03": (20, 83, 45),   "HU-04": (20, 83, 45),
    "HU-05": (74, 29, 150),  "HU-06": (74, 29, 150),
    "HU-07": (124, 45, 18),  "HU-08": (124, 45, 18),
}
HU_ACCENT = {
    "HU-01": LIGHT_BLUE,  "HU-02": LIGHT_BLUE,
    "HU-03": GREEN,       "HU-04": GREEN,
    "HU-05": (196, 181, 253), "HU-06": (196, 181, 253),
    "HU-07": (253, 186, 116), "HU-08": (253, 186, 116),
}

# ─── Fuentes ──────────────────────────────────────────────────────────────────
def load_font(name, size):
    candidates = [
        os.path.join(FONT_DIR, name),
        os.path.join(FONT_DIR, name.lower()),
    ]
    for c in candidates:
        try:
            return ImageFont.truetype(c, size)
        except Exception:
            pass
    return ImageFont.load_default()

F_HUGE   = load_font("arialbd.ttf", 54)
F_TITLE  = load_font("arialbd.ttf", 38)
F_SUB    = load_font("arialbd.ttf", 26)
F_BODY   = load_font("arial.ttf",   22)
F_SMALL  = load_font("arial.ttf",   17)
F_MONO   = load_font("cour.ttf",    18)
F_BADGE  = load_font("arialbd.ttf", 15)

# ─── Helpers de Dibujo ────────────────────────────────────────────────────────
def rect(d, x, y, w, h, color, radius=10):
    d.rounded_rectangle([x, y, x+w, y+h], radius=radius, fill=color)

def text_c(d, cx, y, txt, font, color=WHITE):
    bbox = d.textbbox((0, 0), txt, font=font)
    tw = bbox[2] - bbox[0]
    d.text((cx - tw // 2, y), txt, font=font, fill=color)

def badge(d, x, y, txt, bg, fg=WHITE, font=None):
    font = font or F_BADGE
    bbox = d.textbbox((0, 0), txt, font=font)
    tw, th = bbox[2]-bbox[0]+16, bbox[3]-bbox[1]+10
    rect(d, x, y, tw, th, bg, radius=6)
    d.text((x+8, y+5), txt, font=font, fill=fg)
    return tw

def draw_header(d, hu, author, scenario_label, scenario_color):
    # Header bar
    d.rectangle([0, 0, W, 70], fill=(10, 12, 20))
    d.rectangle([0, 70, W, 72], fill=ACCENT)
    # Logo
    rect(d, 14, 10, 50, 50, ACCENT, radius=12)
    text_c(d, 39, 18, "RH", F_SUB, WHITE)
    # Titulo sistema
    d.text((74, 12), "Sistema de Gestión de Recursos Humanos", font=F_SMALL, fill=MUTED)
    d.text((74, 34), "Práctica SDD — Especificación Dirigida por la Especificación", font=F_SMALL, fill=(80, 90, 120))
    # HU badge
    bx = W - 220
    bw = badge(d, bx, 18, hu, HU_COLORS.get(hu, SURFACE2), HU_ACCENT.get(hu, WHITE), F_BADGE)
    badge(d, bx + bw + 8, 18, scenario_label, scenario_color, WHITE, F_BADGE)
    # Author
    d.text((bx, 42), f"Responsable: {author}", font=F_SMALL, fill=MUTED)

def draw_footer(d, expected, ts):
    d.rectangle([0, H-44, W, H], fill=(10, 12, 20))
    d.rectangle([0, H-44, W, H-42], fill=BORDER)
    d.text((16, H-30), f"✓ Resultado esperado: {expected}", font=F_SMALL, fill=GREEN)
    d.text((W-70, H-30), ts, font=F_MONO, fill=MUTED)

def draw_window_chrome(d, url, x=0, y=72, w=W, h=H-116):
    # Window frame
    rect(d, x, y, w, 36, (22, 24, 32), radius=0)
    # Traffic lights
    for i, c in enumerate([(239,68,68),(245,158,11),(34,197,94)]):
        cx = x+14+i*20
        d.ellipse([cx, y+10, cx+12, y+22], fill=c)
    # URL bar
    rect(d, x+56, y+7, w-80, 22, (30, 33, 48), radius=5)
    d.text((x+64, y+10), url, font=F_SMALL, fill=MUTED)

def new_frame():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    return img, d

def to_np(img):
    return np.array(img)

def hold(writer, img, seconds):
    frames = int(FPS * seconds)
    arr = to_np(img)
    for _ in range(frames):
        writer.append_data(arr)

def fade(writer, img_a, img_b, duration=0.4):
    frames = int(FPS * duration)
    a = np.array(img_a).astype(float)
    b = np.array(img_b).astype(float)
    for i in range(frames):
        t = i / frames
        blended = (a * (1 - t) + b * t).astype(np.uint8)
        writer.append_data(blended)

# ─── Generadores de Escenas ───────────────────────────────────────────────────

def scene_portada():
    img, d = new_frame()
    # Degradado superior
    for y in range(300):
        alpha = int(30 * (1 - y/300))
        d.line([(0,y),(W,y)], fill=(99,102,241+alpha if 241+alpha < 256 else 255))
    # Logo grande
    rect(d, W//2-50, 60, 100, 100, ACCENT, radius=20)
    text_c(d, W//2, 92, "RH", F_TITLE, WHITE)
    text_c(d, W//2, 180, "Sistema de Gestión de Recursos Humanos", F_TITLE, WHITE)
    text_c(d, W//2, 240, "Práctica SDD · Ingeniería Web · UA 2025/26", F_BODY, MUTED)
    # Separador
    d.rectangle([W//2-200, 290, W//2+200, 292], fill=ACCENT)
    # Equipo
    team = [
        ("Joaquín Navarro", "HU-01, HU-02", LIGHT_BLUE),
        ("Iván Soler",      "HU-03, HU-04", GREEN),
        ("Andrei Radu",     "HU-05, HU-06", (196,181,253)),
        ("José R. Castells","HU-07, HU-08", (253,186,116)),
    ]
    for i, (name, hus, col) in enumerate(team):
        cx = 160 + i * 240
        rect(d, cx-100, 320, 200, 90, SURFACE, radius=10)
        d.text((cx-90, 332), name, font=F_SMALL, fill=WHITE)
        d.text((cx-90, 356), hus, font=F_SMALL, fill=col)
    # Badges tech
    techs = ["Spring Boot 3.4", "Angular 21", "PostgreSQL 16", "JWT + RBAC", "RFC 9457"]
    bx = W//2 - 320
    for t in techs:
        bw = badge(d, bx, 440, t, SURFACE2, MUTED)
        bx += bw + 10
    text_c(d, W//2, 490, "8 Historias de Usuario · 93 Tests Unitarios en Verde", F_SMALL, MUTED)
    # Footer
    text_c(d, W//2, H-50, "Noviembre 2026 · Grupo G01", F_SMALL, (60,70,100))
    return img

def scene_app(hu, author, scenario_label, scenario_color, title, subtitle, url,
              fields=None, result_text=None, result_color=None, expected="OK",
              show_error=False, error_code=None, error_msg=None,
              show_notification=False, table_data=None):
    img, d = new_frame()
    draw_header(d, hu, author, scenario_label, scenario_color)
    draw_window_chrome(d, url)

    app_y = 108
    app_h = H - 152
    app_x = 0

    # Content area background
    rect(d, app_x, app_y, W, app_h, (13, 16, 25), radius=0)

    # Sidebar
    rect(d, app_x, app_y, 220, app_h, SURFACE, radius=0)
    menu_items = ["🏠 Dashboard", "👥 Empleados", "📋 Solicitudes", "✅ Gestión", "📅 Calendario", "📊 Informes", "⭐ Evaluaciones"]
    for i, item in enumerate(menu_items):
        is_active = any(x in item for x in [
            "Empleados" if hu in ("HU-01","HU-02") else
            "Solicitudes" if hu in ("HU-03","HU-04") else
            "Gestión" if hu == "HU-05" else
            "Calendario" if hu == "HU-06" else
            "Informes" if hu == "HU-07" else
            "Evaluaciones"
        ])
        bg = HU_COLORS.get(hu, SURFACE2) if is_active else SURFACE
        rect(d, app_x, app_y + i*42, 220, 42, bg, radius=0)
        col = HU_ACCENT.get(hu, WHITE) if is_active else MUTED
        d.text((app_x+16, app_y + i*42 + 12), item, font=F_SMALL, fill=col)

    # Main area
    mx = app_x + 230
    mw = W - mx - 10

    # Page title
    d.text((mx, app_y + 16), title, font=F_SUB, fill=WHITE)
    if subtitle:
        d.text((mx, app_y + 48), subtitle, font=F_SMALL, fill=MUTED)

    content_y = app_y + 80

    # Formulario / tarjetas
    if fields:
        rect(d, mx, content_y, mw, len(fields)*56 + 30, SURFACE, radius=10)
        for i, (label, value, is_filled) in enumerate(fields):
            fy = content_y + 20 + i * 56
            d.text((mx+16, fy), label, font=F_SMALL, fill=MUTED)
            fill_col = SURFACE2 if is_filled else (20, 23, 35)
            border_col = ACCENT if is_filled else BORDER
            rect(d, mx+16, fy+18, mw-32, 28, fill_col, radius=6)
            d.rectangle([mx+16, fy+18, mx+16+mw-32, fy+46], outline=border_col, width=1)
            if value:
                d.text((mx+24, fy+24), value, font=F_SMALL, fill=WHITE if is_filled else MUTED)
        content_y += len(fields)*56 + 46

    # Tabla
    if table_data and len(table_data) > 0:
        headers = table_data[0]
        rows = table_data[1:]
        col_w = mw // len(headers)
        rect(d, mx, content_y, mw, 32, SURFACE2, radius=6)
        for j, h in enumerate(headers):
            d.text((mx + j*col_w + 10, content_y + 8), h, font=F_SMALL, fill=ACCENT2)
        for i, row in enumerate(rows[:5]):
            ry = content_y + 32 + i*32
            bg = (13,16,25) if i%2==0 else (18,21,30)
            d.rectangle([mx, ry, mx+mw, ry+32], fill=bg)
            for j, cell in enumerate(row):
                col = GREEN if cell == "APROBADA" else RED if cell in ("RECHAZADA","CANCELADA") else AMBER if cell == "PENDIENTE" else WHITE
                d.text((mx + j*col_w + 10, ry + 8), str(cell), font=F_SMALL, fill=col)
        content_y += 32 + min(5,len(rows))*32 + 20

    # Notificación de éxito / error
    if show_error and error_code:
        rect(d, mx, content_y, mw, 80, (60, 15, 15), radius=10)
        d.rectangle([mx, content_y, mx+mw, content_y+80], outline=RED, width=2)
        badge(d, mx+16, content_y+14, f"HTTP {error_code}", RED, WHITE)
        msg = error_msg or "Error de validación"
        d.text((mx+16, content_y+40), msg[:70], font=F_SMALL, fill=(252,165,165))
        content_y += 96

    if show_notification:
        rect(d, mx, content_y, mw, 50, (15, 50, 25), radius=10)
        d.rectangle([mx, content_y, mx+mw, content_y+50], outline=GREEN, width=2)
        d.text((mx+16, content_y+16), f"✓  {result_text or 'Operación realizada con éxito'}", font=F_SMALL, fill=GREEN)
        content_y += 66

    # Resultado final
    if result_text and not show_notification:
        rc = result_color or GREEN
        rect(d, mx, content_y, mw, 44, (*rc, 30) if len(rc)==3 else (30,70,30), radius=8)
        d.text((mx+16, content_y+12), f"{'✓' if rc==GREEN else '⚠'}  {result_text}", font=F_SMALL, fill=rc)

    ts = "00:00"
    draw_footer(d, expected, ts)
    return img


def scene_title_card(hu, hu_name, author, description, scenario, scenario_color):
    img, d = new_frame()
    huc = HU_COLORS.get(hu, SURFACE)
    hca = HU_ACCENT.get(hu, WHITE)

    # Fondo con acento de color
    for y in range(H):
        t = y / H
        r = int(BG[0] + (huc[0]-BG[0])*t*0.4)
        g = int(BG[1] + (huc[1]-BG[1])*t*0.4)
        b = int(BG[2] + (huc[2]-BG[2])*t*0.4)
        d.line([(0,y),(W,y)], fill=(r,g,b))

    # Acento lateral
    d.rectangle([0, 0, 6, H], fill=hca)

    # Badge HU
    bw = badge(d, W//2-30, 160, hu, huc, hca, F_SUB)
    # Título HU
    text_c(d, W//2, 220, hu_name, F_TITLE, WHITE)
    # Autor
    text_c(d, W//2, 278, f"Responsable: {author}", F_BODY, MUTED)
    # Separador
    d.rectangle([W//2-180, 316, W//2+180, 318], fill=hca)
    # Descripción (wrapeada)
    lines = textwrap.wrap(description, width=72)
    for i, line in enumerate(lines[:3]):
        text_c(d, W//2, 334+i*30, line, F_BODY, (200, 210, 230))
    # Escenario
    text_c(d, W//2, 440, f"Escenario: {scenario}", F_SMALL, hca)
    return img


def scene_test_results():
    img, d = new_frame()
    text_c(d, W//2, 50, "Suite de Pruebas Unitarias — Resultados", F_SUB, ACCENT2)
    tests = [
        ("LeaveBalanceServiceTest",         14, 14),
        ("LeaveProratingUnitTest",          14, 14),
        ("LeaveRequestServiceTest",         19, 19),
        ("LeaveResolutionServiceTest",      16, 16),
        ("LeaveResolverPolicyTest",          9,  9),
        ("LeaveRequestTrackingServiceTest",  6,  6),
        ("LeaveRequestAutoCancelServiceTest",5,  5),
        ("TeamCalendarServiceTest",         10, 10),
    ]
    total_p = sum(t[1] for t in tests)
    total_t = sum(t[2] for t in tests)

    for i, (name, passed, total) in enumerate(tests):
        y = 110 + i * 56
        rect(d, 60, y, W-120, 44, SURFACE, radius=8)
        d.text((76, y+12), name, font=F_SMALL, fill=WHITE)
        status = f"{passed}/{total} PASS"
        bbox = d.textbbox((0,0), status, font=F_SMALL)
        d.text((W-120-bbox[2]-bbox[0]-20, y+12), status, font=F_SMALL, fill=GREEN)
        bar_w = int((passed/total) * 300)
        rect(d, W-340, y+14, 300, 16, SURFACE2, radius=4)
        rect(d, W-340, y+14, bar_w, 16, GREEN, radius=4)

    rect(d, 60, 560, W-120, 70, (10, 40, 20), radius=12)
    d.rectangle([60, 560, W-60, 630], outline=GREEN, width=2)
    text_c(d, W//2, 575, f"TOTAL: {total_p}/{total_t} Tests — BUILD SUCCESS ✓", F_SUB, GREEN)
    text_c(d, W//2, 614, "93/93 Pruebas Unitarias Superadas · 0 Fallos · 0 Errores", F_SMALL, MUTED)
    return img


def scene_cierre():
    img, d = new_frame()
    for y in range(H):
        t = y / H
        r = int(15 + 20*t); g = int(17 + 15*t); b = int(23 + 40*t)
        d.line([(0,y),(W,y)], fill=(r,g,b))

    text_c(d, W//2, 80,  "✓ Demostración Completada", F_TITLE, GREEN)
    text_c(d, W//2, 140, "Sistema de Gestión de Recursos Humanos · SDD G01", F_BODY, MUTED)
    d.rectangle([W//2-250, 180, W//2+250, 182], fill=ACCENT)

    summary = [
        ("8 Historias de Usuario", "100% Criterios Verificados"),
        ("93 Tests Unitarios",     "0 Fallos · BUILD SUCCESS"),
        ("2 Recorridos E2E",       "Cruzados entre integrantes"),
        ("11 Hallazgos",           "Todos resueltos y documentados"),
        ("Constitución v1.2.0",    "Art.10,12,14,19,20 conformes"),
    ]
    for i, (k, v) in enumerate(summary):
        y = 210 + i * 64
        rect(d, W//2-320, y, 300, 50, SURFACE, radius=8)
        rect(d, W//2+20, y, 300, 50, (10, 40, 20), radius=8)
        text_c(d, W//2-170, y+14, k, F_SMALL, ACCENT2)
        text_c(d, W//2+170, y+14, v, F_SMALL, GREEN)

    text_c(d, W//2, H-80, "Joaquín Navarro · Iván Soler · Andrei Radu · José R. Castells", F_SMALL, MUTED)
    text_c(d, W//2, H-50, "Ingeniería Web · Universidad de Alicante · Noviembre 2026", F_SMALL, (60,70,100))
    return img


# ─── Secuencia del Vídeo ──────────────────────────────────────────────────────

def build_video():
    print(f"🎬 Generando {OUT_FILE} ...")
    writer = imageio.get_writer(
        OUT_FILE, fps=FPS, codec="libx264",
        quality=None,
        output_params=["-crf", "26", "-preset", "medium", "-pix_fmt", "yuv420p"],
        macro_block_size=None
    )

    def w(img, sec): hold(writer, img, sec)
    def wf(img_a, img_b, s): fade(writer, img_a, img_b, s)

    prev = scene_portada()

    # ── PORTADA (0:00–0:08) ───────────────────────────────────────────────────
    w(prev, 8)

    # ── HU-01: Alta de Empleados ──────────────────────────────────────────────
    nxt = scene_title_card("HU-01", "Alta de Empleados y Registro Contractual",
                           "Joaquín Navarro", "El administrador de RRHH crea la ficha de un nuevo empleado y asocia su primer contrato laboral vinculado a un convenio colectivo activo.",
                           "Flujo Principal Satisfactorio", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-01","Joaquín Navarro","Flujo Principal",GREEN,
                    "Nuevo Empleado","Formulario de alta en el sistema",
                    "localhost:4200/empleados/nuevo",
                    fields=[("Nombre","Ana García Martínez",True),
                            ("Email","ana.garcia@empresa.com",True),
                            ("Departamento","Recursos Humanos",True),
                            ("Convenio colectivo","Convenio General de Oficinas",True),
                            ("Fecha de contratación","2026-07-01",True),
                            ("Tipo de jornada","Completa (40h/semana)",True)],
                    show_notification=True,
                    result_text="Empleado creado correctamente · ID generado por el servidor",
                    expected="Empleado persistido · Contrato cronológico válido")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-01","Joaquín Navarro","Error: Solapamiento (422)",RED,
                    "Validación Contractual","Intento de añadir contrato con fechas solapadas",
                    "localhost:4200/empleados/contratos",
                    fields=[("Inicio contrato nuevo","2026-01-01",True),
                            ("Fin contrato nuevo","2026-12-31",True)],
                    show_error=True,
                    error_code=422,
                    error_msg="Contract period overlaps with existing record (2026-04-01 / 2026-10-31)",
                    expected="HTTP 422 RFC 9457 · El solapamiento es bloqueado en servidor")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-01","Joaquín Navarro","Histórico Inmutable",AMBER,
                    "Histórico Contractual","Contratos pasados bloqueados contra modificación",
                    "localhost:4200/empleados/1/contratos",
                    table_data=[
                        ["Período","Tipo","Convenio","Estado"],
                        ["2024-01-01 → 2024-12-31","Indefinido","Oficinas","Cerrado 🔒"],
                        ["2025-01-01 → 2025-12-31","Indefinido","Oficinas","Cerrado 🔒"],
                        ["2026-01-01 → activo","Indefinido","Oficinas","Activo ✓"],
                    ],
                    result_text="Botones de edición deshabilitados para periodos cerrados",
                    result_color=AMBER,
                    expected="Inmutabilidad de registros históricos garantizada")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    # ── HU-02: Saldo de Vacaciones ────────────────────────────────────────────
    nxt = scene_title_card("HU-02", "Cálculo Dinámico de Saldo de Vacaciones",
                           "Joaquín Navarro", "El sistema calcula en tiempo real los días disponibles como valor derivado (Art. 12 Constitución): Correspondientes − Disfrutados − Pendientes.",
                           "Flujo Principal + Prorrateo", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-02","Joaquín Navarro","Saldo Derivado (Art.12)",GREEN,
                    "Mi Ficha · Saldo de Vacaciones 2026","Cálculo en tiempo real sin contador persistido",
                    "localhost:4200/perfil/saldo",
                    table_data=[
                        ["Concepto","Días","Tipo"],
                        ["Días correspondientes (convenio)","22","Laborables"],
                        ["Días disfrutados (aprobados)","10","Descontados"],
                        ["Días pendientes de resolución"," 5","Retenidos"],
                        ["DÍAS DISPONIBLES","  7","RESULTADO"],
                    ],
                    show_notification=True,
                    result_text="Saldo calculado al vuelo — sin columna mutable en base de datos",
                    expected="Derivación correcta: 22 − 10 − 5 = 7 días disponibles")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-02","Joaquín Navarro","Prorrateo por alta tardía",AMBER,
                    "Alta en Julio → Prorrateo Proporcional","14 tests de prorrateo en verde",
                    "localhost:4200/perfil/saldo?year=2026",
                    table_data=[
                        ["Parámetro","Valor"],
                        ["Fecha de alta","2026-07-01"],
                        ["Días restantes del año","184 de 365"],
                        ["Días convenio (anual)","22 laborables"],
                        ["Días proporcionales","= 22 × 184/365 = 11 días"],
                    ],
                    result_text="Prorrateo exacto por días naturales · Redondeado al entero más próximo",
                    result_color=AMBER,
                    expected="11 días de vacaciones proporcionales para alta en julio")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-02","Joaquín Navarro","Acceso cruzado bloqueado (403)",RED,
                    "Protección de Privacidad de Saldos","Un empleado intenta consultar el saldo ajeno",
                    "localhost:4200/empleados/42/saldo",
                    show_error=True,
                    error_code=403,
                    error_msg="Access Denied: You are not authorized to view another employee's balance",
                    expected="HTTP 403 Forbidden · Autorización verificada en servidor")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    # ── HU-03: Solicitud de Ausencias ─────────────────────────────────────────
    nxt = scene_title_card("HU-03", "Solicitud de Vacaciones y Permisos",
                           "Iván Soler", "El empleado solicita un período de ausencia. El sistema valida el saldo disponible en el servidor y detecta solapamientos con peticiones previas.",
                           "Flujo Principal + Validaciones (422/409)", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-03","Iván Soler","Flujo Principal",GREEN,
                    "Nueva Solicitud de Vacaciones","Formulario de solicitud con validación preventiva de saldo",
                    "localhost:4200/solicitudes/nueva",
                    fields=[("Tipo de permiso","Vacaciones ordinarias",True),
                            ("Fecha de inicio","2026-08-01",True),
                            ("Fecha de fin","2026-08-14 (10 días laborables)",True),
                            ("Justificación (opcional)","Vacaciones de verano",True)],
                    show_notification=True,
                    result_text="Solicitud registrada en estado PENDIENTE · 5 días restantes en saldo",
                    expected="Creación válida · Saldo retenido preventivamente")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-03","Iván Soler","Saldo insuficiente (422)",RED,
                    "Validación Preventiva de Saldo","El empleado solicita más días de los disponibles",
                    "localhost:4200/solicitudes/nueva",
                    fields=[("Días solicitados","25 laborables",True),
                            ("Saldo disponible","7 días",True)],
                    show_error=True,
                    error_code=422,
                    error_msg="Insufficient leave balance: requested 25 days, available 7 days",
                    expected="HTTP 422 · Solicitud bloqueada sin modificar el saldo")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-03","Iván Soler","Solapamiento de fechas (409)",RED,
                    "Bloqueo de Solicitud Solapada","Las fechas coinciden con una petición ya existente",
                    "localhost:4200/solicitudes/nueva",
                    fields=[("Nueva solicitud","2026-08-05 → 2026-08-10",True),
                            ("Existente en sistema","2026-08-01 → 2026-08-14 (PENDIENTE)",True)],
                    show_error=True,
                    error_code=409,
                    error_msg="Conflict: dates overlap with existing pending leave request #2047",
                    expected="HTTP 409 Conflict · Coherencia de solicitudes garantizada")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-03","Iván Soler","Bloqueo Dirección General (422)",AMBER,
                    "Exención de Vacaciones para Alta Dirección","Rol sin saldo de vacaciones ordinarias",
                    "localhost:4200/solicitudes/nueva",
                    show_error=True,
                    error_code=422,
                    error_msg="DIRECCION_GENERAL role is exempt from vacation balance and cannot submit leave requests",
                    expected="HTTP 422 · Exención constitucional de alta dirección verificada")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    # ── HU-04: Seguimiento de Solicitudes ────────────────────────────────────
    nxt = scene_title_card("HU-04", "Seguimiento y Cancelación de Solicitudes",
                           "Iván Soler", "El empleado consulta el historial completo de sus solicitudes con filtros reactivos por estado y año, y puede cancelar peticiones en estado PENDIENTE.",
                           "Historial + Cancelación + Bloqueo de cancelación", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-04","Iván Soler","Historial Reactivo",GREEN,
                    "Mis Solicitudes","Filtros dinámicos por estado y año sin recarga de página",
                    "localhost:4200/solicitudes/mias",
                    table_data=[
                        ["Período","Tipo","Días","Estado","Motivo"],
                        ["01/06–15/06","Vacaciones","11","APROBADA",""],
                        ["01/08–14/08","Vacaciones","10","PENDIENTE",""],
                        ["01/09–03/09","Asuntos propios","3","RECHAZADA","Cobertura insuficiente"],
                        ["15/10–17/10","Vacaciones","3","CANCELADA","Cancelado por empleado"],
                    ],
                    expected="Listado con estados diferenciados por color")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-04","Iván Soler","Cancelación de PENDIENTE",GREEN,
                    "Cancelar Solicitud","El empleado cancela su solicitud pendiente",
                    "localhost:4200/solicitudes/mias",
                    show_notification=True,
                    result_text="Solicitud #2047 cancelada · 10 días liberados al saldo disponible",
                    expected="Estado PENDIENTE → CANCELADA · Saldo liberado inmediatamente")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-04","Iván Soler","Cancelación bloqueada (409)",RED,
                    "Bloqueo de Cancelación de Solicitud Resuelta","No se puede cancelar una solicitud APROBADA",
                    "localhost:4200/solicitudes/mias",
                    show_error=True,
                    error_code=409,
                    error_msg="Cannot cancel a request in APROBADA status. Contact your manager to revoke approval.",
                    expected="HTTP 409 · Solo se pueden cancelar peticiones PENDIENTES")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    # ── HU-05: Resolución de Solicitudes ─────────────────────────────────────
    nxt = scene_title_card("HU-05", "Aprobación y Rechazo de Solicitudes",
                           "Andrei Radu", "El responsable de equipo revisa y resuelve las solicitudes pendientes. El sistema garantiza control de concurrencia optimista (@Version) y delimitación jerárquica de RR.HH.",
                           "Flujo + Concurrencia + Enmienda Constitucional v1.2.0", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-05","Andrei Radu","Aprobación por Responsable",GREEN,
                    "Bandeja de Resolución","El responsable Carlos aprueba la solicitud de Ana",
                    "localhost:4200/gestion/solicitudes",
                    table_data=[
                        ["Empleado","Período","Días","Tipo","Acción"],
                        ["Ana García","01/08–14/08","10","Vacaciones","✓ Aprobar  ✗ Rechazar"],
                        ["Pedro López","20/08–22/08"," 3","Asuntos","✓ Aprobar  ✗ Rechazar"],
                    ],
                    show_notification=True,
                    result_text="Solicitud APROBADA · Notificación enviada a Ana García · Saldo descontado",
                    expected="Estado APROBADA · Notificación · Saldo actualizado")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-05","Andrei Radu","Rechazo con motivo obligatorio",AMBER,
                    "Rechazo con Justificación","El motivo es campo obligatorio en denegaciones",
                    "localhost:4200/gestion/solicitudes/rechazar/2047",
                    fields=[("Motivo del rechazo","Cobertura insuficiente durante semana de auditoría",True),
                            ("Caracteres mínimos requeridos","≥ 5 caracteres",True)],
                    show_notification=True,
                    result_text="Solicitud RECHAZADA · Motivo visible para el empleado en su historial",
                    expected="Motivo registrado y visible en HU-04 del empleado")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-05","Andrei Radu","Concurrencia optimista (@Version)",RED,
                    "Control de Doble Resolución","Bloqueo ante resolución simultánea de la misma solicitud",
                    "localhost:4200/gestion/solicitudes",
                    show_error=True,
                    error_code=409,
                    error_msg="OptimisticLockException: request #2047 was already resolved by another session. Refreshing.",
                    expected="HTTP 409 · Bloqueo optimista @Version · Bandeja actualizada")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    nxt = scene_app("HU-05","Andrei Radu","RRHH resolutor de su equipo (v1.2.0)",LIGHT_BLUE,
                    "Enmienda Constitucional v1.2.0","Marta (RRHH) resuelve solicitud de su subordinado directo",
                    "localhost:4200/gestion/solicitudes",
                    table_data=[
                        ["Empleado","Jerarquía","Acción disponible"],
                        ["Luis Pér. (subordinado directo Marta)","RRHH ← Directo","✓ Puede resolver"],
                        ["Ana García (otro dpto)","RRHH ← Ninguna","✗ 403 Forbidden"],
                    ],
                    show_notification=True,
                    result_text="RRHH resuelve solo para su jerarquía directa · Art.10 Constitución",
                    expected="Restricción v1.2.0 aplicada correctamente")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    nxt = scene_app("HU-05","Andrei Radu","Auto-cancelación por baja de supervisor",RED,
                    "Desactivación de Responsable","RRHH da de baja a Carlos → solicitudes huérfanas canceladas",
                    "localhost:4200/empleados/carlos/desactivar",
                    show_notification=True,
                    result_text="4 solicitudes PENDIENTES del equipo de Carlos → CANCELADAS automáticamente",
                    expected="LeaveRequestAutoCancelService activado · Ninguna solicitud queda huérfana")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    # ── HU-06: Calendario de Equipo ───────────────────────────────────────────
    nxt = scene_title_card("HU-06", "Calendario Interactivo de Equipo",
                           "Andrei Radu", "El responsable visualiza las ausencias de su equipo en un cuadrante mensual con detección de solapamientos críticos y anonimización RGPD obligatoria (Art. 20).",
                           "Calendario + RGPD + Alerta de Cobertura", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-06","Andrei Radu","Calendario Agosto 2026",GREEN,
                    "Calendario del Equipo · Agosto 2026","Proyección de ausencias aprobadas y pendientes",
                    "localhost:4200/calendario?mes=2026-08",
                    table_data=[
                        ["Empleado","L 03","M 04","X 05","J 06","V 07"],
                        ["Ana García","Ausente","Ausente","Ausente","Ausente","Ausente"],
                        ["Pedro López","","","Ausente","Ausente",""],
                        ["Carmen Ruiz","","","","",""],
                        ["Luis Pérez","","","","Ausente","Ausente"],
                    ],
                    show_notification=True,
                    result_text="Ausencias proyectadas · Tipología médica anonimizada (Art. 20 RGPD)",
                    expected="Mínimos de datos: compañeros ven 'Ausente' sin motivo personal")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-06","Andrei Radu","Alerta de solapamiento",AMBER,
                    "Día con Cobertura Crítica","Jueves 06 de agosto: 3 miembros ausentes simultáneamente",
                    "localhost:4200/calendario?mes=2026-08",
                    result_text="⚠ Jueves 06/08: 3 ausencias simultáneas — Umbral de cobertura alcanzado",
                    result_color=AMBER,
                    expected="Alerta visual de solapamiento y recuento de personas ausentes")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    # ── HU-07: Informes de Absentismo ─────────────────────────────────────────
    nxt = scene_title_card("HU-07", "Informes Analíticos de Absentismo",
                           "José R. Castells", "El administrador de RRHH genera métricas de uso de vacaciones y absentismo por departamento y período, con exportación CSV conforme a RFC 4180.",
                           "Dashboard Analítico + Exportación CSV + Restricción RBAC", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-07","José R. Castells","Informe Departamental 2026",GREEN,
                    "Informe de Uso de Vacaciones · 2026","Métricas agregadas por departamento para RRHH/Dirección",
                    "localhost:4200/informes/absentismo?year=2026",
                    table_data=[
                        ["Departamento","Empleados","Días Aprob.","Días Pend.","% Utilización"],
                        ["RRHH","8","87","12","49.4%"],
                        ["Desarrollo","12","134","18","50.8%"],
                        ["Ventas","15","163","24","48.4%"],
                        ["Dirección","4","0","0","Exento"],
                    ],
                    show_notification=True,
                    result_text="Informe generado · Totales verificados con datos de base de datos",
                    expected="Métricas de absentismo con paridad matemática")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-07","José R. Castells","Exportación CSV (RFC 4180)",GREEN,
                    "Descarga del Informe en CSV","Archivo UTF-8 BOM con mismos totales que la pantalla",
                    "localhost:4200/informes/absentismo/export",
                    show_notification=True,
                    result_text="Archivo informe-absentismo-2026.csv descargado · UTF-8 BOM · 39 filas",
                    expected="RFC 4180 · Paridad exacta con tabla en pantalla verificada")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    nxt = scene_app("HU-07","José R. Castells","Acceso restringido a RRHH/DG (403)",RED,
                    "Control de Acceso a Informes Analíticos","Empleado y Responsable no pueden acceder",
                    "localhost:4200/informes/absentismo",
                    show_error=True,
                    error_code=403,
                    error_msg="Access Denied: /reports requires RRHH or DIRECCION_GENERAL role",
                    expected="HTTP 403 · Restricción RBAC verificada en servidor")
    wf(prev, nxt, 0.4); w(nxt, 5); prev = nxt

    # ── HU-08: Evaluación de Desempeño ────────────────────────────────────────
    nxt = scene_title_card("HU-08", "Ciclo de Evaluación de Desempeño",
                           "José R. Castells", "Circuito cerrado de evaluación en 3 fases: autoevaluación del empleado, valoración del responsable y acta de doble conformidad con bloqueo inmutable.",
                           "Autoevaluación → Valoración → Cierre Inmutable", GREEN)
    wf(prev, nxt, 0.5); w(nxt, 3.5); prev = nxt

    nxt = scene_app("HU-08","José R. Castells","Autoevaluación del empleado",GREEN,
                    "Mi Autoevaluación · Ciclo 2026","El empleado valora sus objetivos y competencias",
                    "localhost:4200/evaluaciones/auto/2026",
                    fields=[("Objetivo 1: Entregas a tiempo","Superado (95% cumplimiento)",True),
                            ("Objetivo 2: Calidad del código","Cumplido (0 bugs críticos en prod.)",True),
                            ("Competencia: Trabajo en equipo","4/5 — Muy buena colaboración",True),
                            ("Comentario libre","Gran año de aprendizaje en SDD y microservs.",True)],
                    show_notification=True,
                    result_text="Autoevaluación guardada · Estado: PENDIENTE_EVALUADOR",
                    expected="Fase 1 completa · En espera de valoración del responsable")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-08","José R. Castells","Ponderación por el evaluador",AMBER,
                    "Evaluación del Responsable · Ana García","El supervisor califica y añade comentarios",
                    "localhost:4200/evaluaciones/revisar/142",
                    fields=[("Puntuación global (1–5)","4.2",True),
                            ("Valoración del evaluador","Excelente año. Lidera iniciativas de calidad.",True),
                            ("Propuesta objetivos 2027","Liderar el módulo de microservicios de RRHH.",True)],
                    show_notification=True,
                    result_text="Evaluación del responsable guardada · Estado: PENDIENTE_CONFORMIDAD",
                    expected="Fase 2 completa · Empleado debe aceptar y cerrar el acta")
    wf(prev, nxt, 0.4); w(nxt, 7); prev = nxt

    nxt = scene_app("HU-08","José R. Castells","Acta de Doble Conformidad",GREEN,
                    "Cierre del Ciclo de Evaluación","Empleado acepta el acta → evaluación bloqueada",
                    "localhost:4200/evaluaciones/aceptar/142",
                    show_notification=True,
                    result_text="✓ Acta CERRADA con doble conformidad · Estado COMPLETADA · Solo lectura",
                    expected="Fase 3 · Acta inmutable preservada para auditorías futuras")
    wf(prev, nxt, 0.4); w(nxt, 6); prev = nxt

    # ── TESTS ─────────────────────────────────────────────────────────────────
    nxt = scene_test_results()
    wf(prev, nxt, 0.5); w(nxt, 7); prev = nxt

    # ── CIERRE ────────────────────────────────────────────────────────────────
    nxt = scene_cierre()
    wf(prev, nxt, 0.8); w(nxt, 7)

    writer.close()
    size_mb = os.path.getsize(OUT_FILE) / (1024*1024)
    print(f"✅ Vídeo generado: {OUT_FILE}")
    print(f"   Tamaño: {size_mb:.1f} MB · Resolución: {W}×{H} · FPS: {FPS}")
    print(f"   Duración estimada: ~{(size_mb/3.5*60):.0f} segundos")

if __name__ == "__main__":
    build_video()
