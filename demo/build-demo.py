#!/usr/bin/env python3
"""Convierte la grabacion de record-demo.mjs en demo-rrhh.mp4 y genera chapters.js.

Uso: python3 -I build-demo.py <video.webm> <chapters.json> <directorio_salida>

El guion pinta en la esquina inferior izquierda una marca binaria (12 celdas de 6 px) con el
indice del capitulo en curso. Aqui se lee esa marca fotograma a fotograma para obtener el
instante EXACTO de cada capitulo dentro del video (la reloj del guion y el del video no
coinciden), y despues se tapa la marca con una caja del color del rotulo.
Requiere ffmpeg en el PATH.
"""
import json, subprocess, sys, os

webm, chap_json, out = sys.argv[1:4]
W, H, CW, N = 1280, 720, 72, 12
FPS = 10

meta = json.load(open(chap_json, encoding="utf-8"))
chapters = meta["chapters"]

# 1) leer la marca binaria
raw = subprocess.run(
    ["ffmpeg", "-v", "error", "-i", webm, "-vf", f"crop={CW}:6:0:{H-6},fps={FPS},format=gray", "-f", "rawvideo", "-"],
    capture_output=True, check=True).stdout
frame = CW * 6
ids = []
for i in range(len(raw) // frame):
    f = raw[i * frame:(i + 1) * frame]
    v = 0
    for b in range(N):
        px = f[3 * CW + b * 6 + 3]          # centro de la celda b (fila 3)
        v = (v << 1) | (1 if px > 128 else 0)
    ids.append(v)

# 2) primer instante estable (2 fotogramas seguidos) de cada capitulo
start = {}
for i in range(len(ids) - 1):
    if ids[i] == ids[i + 1] and ids[i] not in start and ids[i] < len(chapters):
        start[ids[i]] = i / FPS
last = 0.0
for c in chapters:
    t = start.get(c["id"])
    if t is None or t < last:
        t = last
    c["video_t"] = round(max(0.0, t - 0.3), 2)   # pequeño margen para que se vea el inicio
    last = t

# 3) codificar el MP4 tapando la marca
os.makedirs(out, exist_ok=True)
mp4 = os.path.join(out, "demo-rrhh.mp4")
subprocess.run([
    "ffmpeg", "-v", "error", "-y", "-i", webm,
    "-vf", f"drawbox=x=0:y={H-6}:w={CW}:h=6:color=0x0f1117:t=fill,format=yuv420p",
    "-c:v", "libx264", "-preset", "medium", "-crf", "23", "-r", "25",
    "-movflags", "+faststart", "-an", mp4], check=True)
dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", mp4],
                           capture_output=True, text=True, check=True).stdout.strip())

# 4) capitulos para la pagina (script de datos: funciona tambien abriendo index.html con file://)
data = {"duration": round(dur, 1),
        "chapters": [{"hu": c["hu"], "title": c["title"], "text": c.get("text", c["title"]),
                      "t": c["video_t"], "intro": bool(c.get("intro"))} for c in chapters]}
with open(os.path.join(out, "chapters.js"), "w", encoding="utf-8") as fh:
    fh.write("window.DEMO = " + json.dumps(data, ensure_ascii=False, indent=1) + ";\n")
print(f"OK {mp4} ({dur:.0f} s, {os.path.getsize(mp4)/1e6:.1f} MB), {len(chapters)} capitulos, {len(start)} marcas detectadas")
