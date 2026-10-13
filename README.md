# SurcoGPS — Plan maestro SDD

> **Estado global:** 🟡 EN CURSO — ver tabla de sprints abajo.
> Rama principal: `main` · Remoto: `origin` (https://github.com/joaki1991/SurcoGPS.git)
> Última actualización: 2026-10-13 (sesión principal OpenCode, sin subagentes).

## 1. Objetivo y alcance
SurcoGPS es una app Android nativa (Kotlin + Jetpack Compose) de **guiado GPS agrícola**:
genera **líneas rectas paralelas** según rumbo (0°=N, 90°=E, 180°=S, 270°=O) y ancho de
apero, registra las **pasadas reales del tractor** y **colorea la superficie trabajada**
sobre un **mapa híbrido** (satélite + cartografía). Actúa como **banderillero visual**.
**No es conducción automática.**

## 2. Funcionalidades finales previstas
1. Proyecto reproducible (Gradle wrapper, `es.surcogps.app`, minSdk 26 / target 34).
2. Mapa híbrido real (OSMDroid Mapnik + Esri World Imagery), zoom/desplazamiento,
   posición actual, atribución, comportamiento offline documentado.
3. Configuración de rumbo 0–359° y ancho de apero 0.5–30 m; líneas paralelas métricas,
   referencia estable, reajuste de rumbo y desplazamiento lateral.
4. GPS real: lat/lon, velocidad, precisión estimada visible; filtrado (puerta de
   precisión 10 m, puerta de salto, caducidad); compatibilidad documentada con GNSS externo.
5. Registro de recorrido + franja del apero, coloreado progresivo, distinción
   trabajado/pendiente, solapamientos sin duplicar, hectáreas reales, offset antena→apero.
6. Persistencia local: parcelas, sesiones, reanudación, supervivencia a cierres.
7. Seguimiento con pantalla apagada (foreground service `location` + notificación),
   permisos correctos, recuperación ante errores/reinicios.
8. APKs debug y release generados, verificados (`aapt dump badging`, `apksigner verify`),
   con instrucciones de instalación.

## 3. Arquitectura y tecnologías
Ver `.sdd/architecture.md`. Resumen: `:core` Kotlin/JVM puro (GeoMath, GuidanceEngine,
CoverageTracker, GpsFilter, ImplementOffset, SessionStore, tests JUnit5 sin SDK) +
`:app` Android (Compose BOM 2024.09.00, OSMDroid 6.1.20, Play-services-location,
Room 2.6.1, DataStore, foreground service). Kotlin 2.0.20 · AGP 8.5.2 · Gradle 8.10.2.
Sin API keys. Coste de mapas: 0 € en uso normal (política de tiles OSM/Esri).

## 4. Sprints (lista ordenada, dependencias y estado)

| # | Sprint | Bloque | Dependencias | Estado |
|---|--------|--------|--------------|--------|
| 01 | Fundación: Gradle wrapper, `:core` Kotlin puro, JUnit5, CI local | A | — | ✅ PASS |
| 02 | Geometría: GeoMath + GuidanceEngine (líneas paralelas, XTE, shift) | C | 01 | ✅ PASS |
| 03 | Cobertura: CoverageTracker raster 1 m, área sin duplicar | E | 02 | ✅ PASS |
| 04 | GPS: GpsFilter + ImplementOffset | D/E | 02 | ✅ PASS |
| 05 | Persistencia: SessionStore JSON/JVM + contrato Room | F | 03, 04 | ✅ PASS |
| 06 | App base: módulo `:app`, Compose, navegación, permisos, ajustes | A | 01 | ✅ PASS |
| 07 | Mapa híbrido OSMDroid + atribución + offline | B | 06 | ✅ PASS |
| 08 | GPS real + banderillero (velocidad, precisión, XTE) | D | 04, 07 | ⏳ pendiente |
| 09 | Registro y coloreado de cobertura sobre el mapa | E | 03, 08 | ⏳ pendiente |
| 10 | Parcelas/sesiones UI + reanudación | F | 05, 09 | ⏳ pendiente |
| 11 | Foreground service + robustez (reinicios, batería, sin red) | G | 08, 10 | ⏳ pendiente |
| 12 | QA final + APKs + revisión seguridad + docs instalación | H | 06–11 | ⏳ pendiente |

Leyenda: ⏳ pendiente · 🟡 en curso · ✅ PASS · ⛔ BLOCKED · ❌ FAIL.

## 5. Objetivos, tareas y aceptación por sprint
### SPRINT-01 — Fundación
- Tareas: `settings.gradle`, `gradlew` wrapper 8.10.2, `:core/build.gradle.kts`
  (Kotlin 2.0.20, JUnit5), `.gitignore`, `gradle.properties`.
- Aceptación: `.\gradlew.bat :core:test` en verde con 1 test humo; repo compila sin SDK.
### SPRINT-02 — Geometría
- Tareas: `GeoMath` (ENU local, haversine, destino, rumbo), `GuidanceEngine`
  (familia de paralelas, XTE con signo, línea más cercana, shift lateral, re-rumbo).
- Aceptación: tests de separación métrica exacta, rumbo 0/90/180/270/359, shift.
### SPRINT-03 — Cobertura
- Tareas: `CoverageTracker` (franja por segmentos, raster 1 m, área única, reset).
- Aceptación: pasada simple = largo×ancho; solape doble contado una vez; ida+vuelta adyacentes suman.
### SPRINT-04 — GPS
- Tareas: `GpsFilter` (puerta precisión, salto, caducidad), `ImplementOffset` (rotación por rumbo).
- Aceptación: rechaza fix impreciso/salto; offset 0/90/180 verificado.
### SPRINT-05 — Persistencia
- Tareas: `SessionStore` (guardar/cargar/reanudar sesión + celdas compactadas, JSON JVM).
- Aceptación: round-trip sin pérdida; reanudación continúa área; sesión interrumpida recuperable.
### SPRINT-06 — App base
- Aceptación: `:app:assembleDebug` OK, pantallas navegan, sin botones muertos.
### SPRINT-07 — Mapa
- Aceptación: híbrido visible, mi-posición, atribución, mensaje offline.
### SPRINT-08 — GPS real
- Aceptación: velocidad/precisión visibles, XTE y flecha guía, mock-location verificado.
### SPRINT-09 — Coloreado
- Aceptación: solo fixes válidos colorean; franja = ancho apero; ha coinciden con core.
### SPRINT-10 — Parcelas/sesiones
- Aceptación: crear/guardar/reanudar; tras matar la app el progreso persiste.
### SPRINT-11 — Segundo plano
- Aceptación: con pantalla apagada sigue registrando; notificación persistente; reintento tras reinicio.
### SPRINT-12 — Entrega
- Aceptación: `app-debug.apk` + `app-release.apk` existen, badging = `es.surcogps.app`,
  firma verificada, README documenta instalación, revisión seguridad PASS.

## 6. Dependencias entre sprints
01 → 02 → 03 ↘
01 → 06 → 07 → 08 → 09 → 10 → 11 → 12 · 04 alimenta 08 · 05 alimenta 10.

## 7. Criterios de aceptación globales
- Geometría en marco ENU; tests borde (rumbo 0/359, anchos 0.5/30 m, solapes, saltos, reanudación) en verde.
- Ninguna línea marcada como trabajada sin fixes válidos.
- Precisión estimada siempre visible; puerta configurable (defecto 10 m).
- Área sin duplicar; mapa con atribución; cero secretos en repo.
- APKs generados y verificados; instalación documentada.

## 8. Pruebas necesarias
- Unitarias JUnit5 en `:core` (obligatorias desde sprint 02).
- Integración (store round-trip, guidance+coverage+filter encadenados).
- App: `assembleDebug`,逐步 ejecución en emulador/dispositivo con mock locations.
- QA final: flujo completo campo simulado + revisión seguridad + verificación artefactos.

## 9. Comandos de compilación y verificación (PowerShell)
```powershell
.\gradlew.bat :core:test                 # tests del núcleo (sin SDK)
.\gradlew.bat :app:assembleDebug         # APK depuración (requiere SDK)
.\gradlew.bat :app:assembleRelease       # APK lanzamiento (requiere SDK)
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
& "$env:ANDROID_HOME\platform-tools\adb.exe" install app\build\outputs\apk\debug\app-debug.apk
& "$env:ANDROID_HOME\build-tools\34.0.0\aapt.exe" dump badging app\build\outputs\apk\release\app-release.apk | Select-String package
& "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

## 10. Criterio objetivo de proyecto terminado
1. Los 12 sprints están en ✅ PASS con evidencias en `.sdd/`.
2. Funcionalidades 1–8 implementadas y verificadas.
3. Tests obligatorios ejecutados y superados (informes reales).
4. `assembleRelease` correcta; ambos APK existen y pasan `aapt`/`apksigner`.
5. Documentación sincronizada; historial Git con 1 commit por sprint; último push confirmado
   o bloqueo externo documentado con evidencia.

## 11. Registro de publicación
(Por sprint: commit, push y verificación — se rellena al cerrar cada sprint.)
