import imageio

reader = imageio.get_reader('demo/demo-rrhh.mp4')
meta = reader.get_meta_data()
fps = meta['fps']
duration = meta['duration']
print(f'FPS: {fps}  Duration: {duration:.1f}s  ({int(duration//60)}:{int(duration%60):02d})')
reader.close()

FPS = 25
scenes = []
t = 0

def ts(sec):
    return f'{int(sec//60):02d}:{int(sec%60):02d}', int(sec)

def add(label, seconds):
    global t
    mm_ss, s = ts(t)
    scenes.append((label, mm_ss, s))
    t += seconds + 0.4

# Portada
add('Portada - Presentacion del equipo', 8)
# HU-01
add('HU-01: Alta de Empleado - Tarjeta + Flujo Principal', 3.5 + 0.5 + 7)
add('HU-01: Solapamiento contractual (422)', 6)
add('HU-01: Historico inmutable', 6)
# HU-02
add('HU-02: Tarjeta HU - Saldo Dinamico', 3.5)
add('HU-02: Saldo derivado (Art.12) - Flujo Principal', 7)
add('HU-02: Prorrateo por alta tardia', 6)
add('HU-02: Acceso cruzado bloqueado (403)', 5)
# HU-03
add('HU-03: Tarjeta HU - Solicitud de Ausencias', 3.5)
add('HU-03: Solicitud valida - Flujo Principal', 7)
add('HU-03: Saldo insuficiente (422)', 6)
add('HU-03: Solapamiento de fechas (409)', 6)
add('HU-03: Bloqueo Direccion General (422)', 5)
# HU-04
add('HU-04: Tarjeta HU - Seguimiento', 3.5)
add('HU-04: Historial reactivo con filtros', 7)
add('HU-04: Cancelacion de PENDIENTE', 6)
add('HU-04: Cancelacion bloqueada (409)', 5)
# HU-05
add('HU-05: Tarjeta HU - Resolucion', 3.5)
add('HU-05: Aprobacion por responsable', 7)
add('HU-05: Rechazo con motivo obligatorio', 6)
add('HU-05: Concurrencia optimista (@Version)', 5)
add('HU-05: RRHH resolutor v1.2.0', 6)
add('HU-05: Auto-cancelacion por baja de supervisor', 6)
# HU-06
add('HU-06: Tarjeta HU - Calendario', 3.5)
add('HU-06: Calendario agosto 2026 (RGPD Art.20)', 7)
add('HU-06: Alerta de solapamiento de cobertura', 5)
# HU-07
add('HU-07: Tarjeta HU - Informes', 3.5)
add('HU-07: Informe departamental 2026', 7)
add('HU-07: Exportacion CSV (RFC 4180)', 5)
add('HU-07: Acceso restringido RBAC (403)', 5)
# HU-08
add('HU-08: Tarjeta HU - Evaluaciones', 3.5)
add('HU-08: Autoevaluacion del empleado', 7)
add('HU-08: Ponderacion del responsable', 7)
add('HU-08: Acta de doble conformidad', 6)
# Cierre
add('Suite de Tests 93/93 en Verde', 7)
add('Cierre y Resumen del Proyecto', 7)

print(f'Total escenas: {len(scenes)}')
print(f'Duracion calculada: {t:.1f}s ({int(t//60)}:{int(t%60):02d})')
print()
print('--- TIMESTAMPS EXACTOS ---')
for name, mm, ss in scenes:
    print(f'ts={ss:4d}  label="{mm}"  // {name}')
