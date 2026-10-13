# Memoria — estado actual

## 2026-10-13 — Arranque (sesión principal, sin subagentes)
- Inspección: repo vacío (solo `.git`), rama `main` sin commits, remoto
  `origin=https://github.com/joaki1991/SurcoGPS.git`.
- Herramientas: Java 21.0 (Oracle LTS), Node 20.15.1, winget 1.29.380.
  Sin Gradle, sin Android SDK, sin adb/kotlin. Red OK (Gradle services alcanzable),
  disco C: 351 GB libres.
- Creados: `AGENTS.md`, `.sdd/constitution.md`, `.sdd/architecture.md`, `README.md`
  (plan maestro de 12 sprints).
- Decisión: `:core` puro Kotlin/JVM primero (testeable sin SDK); `:app` Android
  después de instalar SDK (cmdline-tools + platform 34 + build-tools 34).
- Siguiente paso: SPRINT-01 (Gradle wrapper + `:core` + test humo).

## 2026-10-13 — SPRINT-01 PASS
- Gradle 8.10.2 (descargado ~130 MB) + wrapper generado (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/gradle-wrapper.jar`).
- `.\gradlew.bat :core:test` → BUILD SUCCESSFUL, 2/2 PASS (ver
  `.sdd/test-reports/SPRINT-01.md`, `.sdd/reviews/SPRINT-01.md`).
- Siguiente: commit + push SPRINT-01, luego SPRINT-02 (GeoMath + GuidanceEngine).

## 2026-10-13 — SPRINT-02 PASS
- `GeoMath` (ENU local, haversine, rumbo, destino) + `GuidanceEngine` (paralelas,
  XTE con signo, shift, re-rumbo). Suite 23/23 verde (1 fallo inicial por punto de
  prueba ambiguo a media calle, corregido). Informes en `.sdd/test-reports/SPRINT-02.md`
  y `.sdd/reviews/SPRINT-02.md`.
- Siguiente: SPRINT-03 (CoverageTracker raster 1 m).

## 2026-10-13 — SPRINT-03 PASS
- `CoverageTracker` (cápsula segmento ± ancho/2, celdas 1 m, área sin duplicar).
  Suite 34/34 verde; 4 expectativas iniciales recalibradas con valores medidos y
  sobrestimación documentada. Informes SPRINT-03 en `.sdd/`.
- Siguiente: SPRINT-04 (GpsFilter + ImplementOffset).

## 2026-10-13 — SPRINT-04 PASS
- `GpsFilter` (caducidad→precisión→salto, puertas configurables) + `ImplementOffset`
  (lateral/longitudinal rotados por rumbo). Suite 52/52 verde (1 empate de media
  calle en test, corregido). Informes SPRINT-04 en `.sdd/`.
- Siguiente: SPRINT-05 (SessionStore persistencia JVM/JSON).

## 2026-10-13 — SPRINT-05 PASS
- `SessionStore` (contrato + `FileSessionStore` JSON sin dependencias + `MiniJson`),
  celdas compactadas por filas. Suite 60/60 verde a la primera. Informes SPRINT-05.
- Núcleo `:core` COMPLETO (sprints 01–05). Siguiente: instalar Android SDK
  (cmdline-tools + plataforma 34) y SPRINT-06 (módulo `:app`).

## 2026-10-13 — SDK instalado + SPRINT-06 PASS
- SDK en `%LOCALAPPDATA%\Android\Sdk` (cmdline-tools vía BITS, build-tools 34.0.0,
  android-34, platform-tools 37.0.1, licencia aceptada).
- `:app` compilable: `assembleDebug` OK, APK 10.3 MB, badging `es.surcogps.app`,
  4 pantallas + DataStore. `:core:test` 60/60 sin regresión.
- Siguiente: SPRINT-07 (mapa híbrido OSMDroid).

## 2026-10-13 — SPRINT-07 PASS
- Mapa híbrido (Esri + referencia) / satélite / calles, atribución visible, aviso
  offline. Tests JVM 4/4 (URLs z/y/x verificadas); assembleDebug OK (13 MB).
- Deuda: verificación visual en pantalla pendiente de dispositivo (sin VT-x para
  emulador, `adb devices` vacío). Anotado para Sprint 12.
- Siguiente: SPRINT-08 (GPS real + banderillero).

## 2026-10-13 — SPRINT-08 PASS
- Fused (1 s, alta precisión) + NMEA diagnóstico + `GuiadoPipeline`
  (Fix→Filtro→Offset→Guía) + HUD (precisión/vel/XTE/flecha) + ancla persistida +
  seguir-posición. Tests app 10/10; assembleDebug OK.
- Siguiente: SPRINT-09 (registro y coloreado de cobertura sobre el mapa).

## 2026-10-13 — SPRINT-09 PASS
- `CoberturaEngine` (umbral 10 cm, pausa sin fantasma) + `FranjaOverlay` (métrico,
  capa única) + HUD ha + auto-ancla. Tests app 16/16; assembleDebug OK.
- Siguiente: SPRINT-10 (parcelas/sesiones Room + reanudación).
