# AGENTS.md — SurcoGPS (instrucciones permanentes)

## 0. Regla fundamental de autonomía
- Trabaja exclusivamente la sesión principal de OpenCode. **PROHIBIDO** crear agentes,
  subagentes, delegaciones, archivos en `.opencode/agents/` u orquestación multiagente.
- Planifica, implementa, prueba, revisa, documenta y publica tu propio trabajo.
- No simules informes de agentes inexistentes. Toda evidencia (comandos, tests, APK)
  debe ser real y verificable.

## 1. Alcance del proyecto
SurcoGPS = app Android nativa (Kotlin + Jetpack Compose) de guiado GPS agrícola:
líneas rectas paralelas según rumbo (0°=N, 90°=E, 180°=S, 270°=O) y ancho de apero,
registro de pasadas reales del tractor y coloreado de superficie trabajada sobre mapa híbrido.
Banderillero visual. **NO** es conducción automática.

## 2. Reglas técnicas obligatorias
1. Distancias/anchuras en **coordenadas métricas locales** (marco ENU tangente al punto
   de referencia, proyección equirectangular con `cos(latRef)`), nunca restando
   lat/lon en grados. Haversine solo para verificación/distancias largas.
2. El ancho del apero define la anchura de la franja trabajada.
3. Solo se colorea recorrido respaldado por **ubicaciones válidas filtradas**.
   Acercarse a una línea guía NO marca trabajo.
4. Mostrar siempre **precisión estimada** del fix; sin promesas de precisión centimétrica
   con GPS de móvil convencional. Puerta de precisión configurable (defecto 10 m).
5. Solapamientos: contabilizar superficie **sin duplicar** (celdas raster de 1 m en marco local).
6. Mapa con **fuentes autorizadas**: OSMDroid (© OpenStreetMap contributors, Mapnik) +
   Esri World Imagery como capa satélite, con atribución visible. Respetar Tile Usage Policy.
7. Sin claves privadas ni API keys en el repo. Sin secretos en commits.
8. Identificador de paquete estable: `es.surcogps.app`. No cambiarlo sin documentar migración.
9. minSdk 26, targetSdk/compileSdk 34. Kotlin 2.0.x, AGP 8.5.x, Gradle 8.10.x, Compose BOM 2024.09.00.
10. Prohibido dejar en producción: TODOs funcionales, datos simulados, botones muertos,
    implementaciones ficticias de funciones esenciales.

## 3. Arquitectura
- `:core` módulo Kotlin/JVM puro (sin dependencias Android): `GeoMath`, `GuidanceEngine`,
  `CoverageTracker`, `GpsFilter`, `ImplementOffset`, `SessionStore` + interfaces de persistencia.
  Todo testeable con JUnit5 sin SDK.
- `:app` módulo Android: UI Compose (MVVM), mapa OSMDroid, ubicación FusedLocationProvider +
  NMEA/GNSS, Room + DataStore, foreground service `location`, permisos correctos.
- Ver `.sdd/architecture.md` para decisiones y diagrama de módulos.

## 4. Flujo SDD por sprint (obligatorio, sin excepción)
Seleccionar → Especificar → Implementar → Probar → Revisar (diff completo como revisor
independiente) → Decidir (PASS/BLOCKED/FAIL) → Documentar → Git+push.
- Solo avanzar con `PASS`. `BLOCKED` exige documentar causa externa y no inventar resultados.
- Cada sprint: spec en `.sdd/sprints/SPRINT-XX.md`, informe en `.sdd/test-reports/`,
  revisión en `.sdd/reviews/`, memoria en `.sdd/memory/state.md`, estado en `README.md`.
- Commits descriptivos por sprint aprobado. No commitear sprints FAIL. Integración en `main`
  (rama principal de este repo) + `git push` a `origin`. Nunca `--force`, `reset --hard`
  ni reescritura destructiva. No incluir cambios ajenos al sprint ni secretos.

## 5. Calidad y pruebas
- Cada funcionalidad core lleva pruebas unitarias JUnit5 con casos borde
  (rumbo 0/359, anchos extremos, solapamientos, saltos GPS, reanudación).
- Comandos canónicos (Windows PowerShell):
  - Tests core: `.\gradlew.bat :core:test`
  - Lint/compile app: `.\gradlew.bat :app:assembleDebug`
  - APK release: `.\gradlew.bat :app:assembleRelease`
  - SDK vía `ANDROID_HOME`/`ANDROID_SDK_ROOT`; licencias aceptadas con `sdkmanager --licenses`.
- No declarar superada una prueba no ejecutada. Registrar comandos + resultados reales.

## 6. Seguridad y privacidad
- Ubicación solo con permiso explícito; rationale visible; sin tracking encubierto.
- Datos de parcelas/sesiones solo en almacenamiento local del dispositivo.
- Sin telemetría de terceros salvo tiles del mapa (documentado). Revisión de seguridad
  obligatoria antes del APK final.

## 7. APK final
- Generar `app-release.apk` (firmado) y `app-debug.apk`; verificar existencia,
  package id (`aapt dump badging`), firma (`apksigner verify`) y ruta exacta documentada.
- No commitear keystores ni claves privadas. Instrucciones de instalación en README.
