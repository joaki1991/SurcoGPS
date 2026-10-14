# Informe — SPRINT-37 + protocolo de captura para el propietario
- Fecha: 2026-10-14 · Sin cambios de código (diagnóstico). Decisión: PASS.

## A. Cómo capturar Logcat filtrado (PC con el móvil por USB + depuración USB)
```powershell
adb devices                          # debe listar el móvil
$pidApp = adb shell pidof es.surcogps.app
adb logcat -d -v time --pid=$pidApp > surco-cierre.log   # tras reproducir el fallo
adb logcat -d -v time | Select-String -Pattern 'AndroidRuntime|FATAL|ANR|OutOfMemory|osmdroid|TileDownloader|SQLite|tileDownloadErrors' | Select-Object -Last 80
```
Enviar `surco-cierre.log` (o pegar las últimas 80 líneas).

## B. Protocolo de reproducción (10 ciclos)
1. Instalar v1.5.0, abrir Campo, esperar 60 s (anotar píldora `Cola` y si hay mapa).
2. Cerrar (atrás + quitar de recientes). Reabrir sin borrar nada. Anotar si
   bloquea/falla y a los cuántos segundos.
3. Repetir 10 veces. Si bloquea: capturar log INMEDIATAMENTE (paso A) sin
   reabrir antes.
4. Contadores: con el mapa abierto 2 min, anotar `Cola` inicial/final y HTTP.
5. Conectar/desconectar WiFi/datos 30 s y anotar recuperación.
6. Tamaño de caché (Ajustes de Android → Apps → SurcoGPS → Almacenamiento).

## C. Qué buscaré en los logs
- `FATAL EXCEPTION` + traza (causa del bloqueo), `OutOfMemoryError`,
  `SQLite*` (caché), `UnknownHostException`/TLS (red), `tileDownloadErrors`,
  `Please configure a relevant user agent` (política UA), ANR (`main` bloqueado).
