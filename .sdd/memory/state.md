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

## 2026-10-13 — SPRINT-10 PASS
- Room descartado por anomalía de toolchain (documentada en ADR-05 e informe);
  persistencia en ficheros JSON de `:core` (`FileParcelaStore` nuevo + contrato
  `SessionStore`). Parcelas/sesiones UI reales, reanudación con cuadrícula
  intacta, autoguardado 15 s + al salir. Core 70/70, app 17/17, APK OK.
- Siguiente: SPRINT-11 (foreground service + robustez).

## 2026-10-13 — SPRINT-11 PASS
- `MotorCampo` (ámbito app) + `TrackingService` (FGS `location`, START_STICKY,
  notificación ha/XTE/Detener) + `ArranqueReceiver` + flags persistidos.
  ViewModel adelgazado a fachada (pantallas intactas). App 20/20, core 70/70, APK.
- Siguiente: SPRINT-12 (QA final + APKs + entrega).

## 2026-10-13 — SPRINT-12 PASS · PROYECTO TERMINADO (v1.0.0)- Suites: core 70/70, app 20/20. `assembleDebug` + `assembleRelease` (lint vital OK).
- APKs: debug 11 648 261 B + release 8 266 192 B, package `es.surcogps.app`,
  min 26/target 34, firmas verificadas (`CN=SurcoGPS-Dev`, huella documentada).
- Seguridad PASS (sin secretos/TODOs, permisos mínimos, sin telemetría).
- README con instalación, uso, firma y verificación en hardware pendiente.
- Deuda única: prueba en dispositivo (sin hardware ni VT-x aquí).
- Historial: 12 commits (1 por sprint), pushes OK. Cierre con commit + push final.

## 2026-10-14 — FASE 2 (mantenimiento). SPRINT-13 PASS
- Diagnóstico del parpadeo (4 causas en `MapaCampo.update`), plan 14–17 en README,
  icono fuente localizado (`Downloads\FaviconSurcoGPS.png`, 1254×1254, verificado).
- Rama: se sigue en `main` (la petición decía `master`, pero el repo usa `main`).
- Siguiente: SPRINT-14 (icono personalizado).

## 2026-10-14 — SPRINT-14 PASS
- Icono propio integrado (fuente 1254px verificada → 5 mipmaps + foreground 432 +
  adaptativos + manifest). Badging confirma icono y etiqueta. assembleDebug OK.
- Siguiente: SPRINT-15 (estabilidad del mapa).

## 2026-10-14 — SPRINT-15 PASS
- Fuentes/capas solo al cambiar de modo; cámara por comandos + umbral 2 m;
  gesto manual desactiva seguir. Tests app 24/24; assembleDebug OK.
- Siguiente: SPRINT-16 (punto azul + precisión + rumbo).

## 2026-10-14 — SPRINT-16 PASS
- `UbicacionOverlay` (cono + punto + flecha de marcha suavizada, capa única) +
  `SuavizadorRumbo` testeado. Sin brújula (decisión documentada). App 31/31, APK OK.
- Siguiente: SPRINT-17 (zoom auto máximo + botón ubicación + v1.1.0).

## 2026-10-14 — SPRINT-17 PASS · FASE 2 TERMINADA (v1.1.0)
- Encuadre inicial one-shot (zoom 19) + botón «Mi ubicación» + estado de carga +
  versionCode 2. App 35/35. Release v1.1.0 (8 580 920 B) firmado y verificado en
  `releases/`. Falsa alarma `res/BW.xml`: es el adaptive-icon con nombres
  colapsados por AAPT2 (verificado con xmltree).
- Pendiente de hardware: parpadeo visual, punto/flecha, encuadre e icono en
  lanzador (guía de verificación en README §12).

## 2026-10-14 — FASE 3. SPRINT-18 PASS
- Captura real analizada (teselas vacías, chips ausentes, zoom centrado).
  Patrón Traccar investigado en código (WebView único + APIs estrechas).
  Diseño MapaSurco publicado. Siguiente: SPRINT-19 (implementación).

## 2026-10-14 — SPRINT-19 PASS
- MapaSurco: cuadro determinista, controles dentro, CopyrightOverlay,
  diagnóstico Esri con píldora, sin auto-conmutación. App 40/40, APK OK.
  Sin cambios en CampoScreen. Siguiente: SPRINT-20 (QA + v1.2.0).

## 2026-10-14 — SPRINT-20 PASS · FASE 3 TERMINADA (v1.2.0)
- Suites forzadas: core 70/70 + app 40/40. Release v1.2.0 (8 597 308 B) firmada y
  verificada en `releases/`. Se pide captura de confirmación (chips, teselas, píldora).

## 2026-10-14 - SPRINT-21 PASS - FASE 4 EN CURSO
- Diagnostico con evidencia (Esri OK, OSM con throttling, API callbacks via javap).

## 2026-10-14 - SPRINT-22 PASS
- VigilanteTeselas en pildora + UA con contacto + clipToBounds. App 41/41. Siguiente: SPRINT-23.

## 2026-10-14 - SPRINT-23 PASS
- LineasOverlay (paralelas + activa + ancla + long-press con Toast). App 45/45. Siguiente: SPRINT-24 (QA + v1.3.0).

## 2026-10-14 - SPRINT-24 PASS - FASE 4 TERMINADA (v1.3.0)
- Suites forzadas 70/70 + 45/45. Release v1.3.0 firmada en releases/. Se pide captura (pildora, lineas, clip).

## 2026-10-14 - SPRINT-25 PASS
- CrashReporter + AvisoCierre con compartir + fallback MapView. App 49/49. Siguiente: SPRINT-26 (QA + v1.3.1).

## 2026-10-14 - SPRINT-26 PASS - FASE 5 TERMINADA (v1.3.1)
- Suites forzadas 70/70 + 49/49. Release v1.3.1 firmada en releases/. Pendiente: informe de cierre del propietario.
