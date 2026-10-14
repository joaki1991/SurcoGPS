# SurcoGPS — Plan maestro SDD

> **Estado global:** 🟡 FASE 3 EN CURSO (mapa con captura real; v1.1.0 entregada).
> Deuda explícita: prueba visual en hardware pendiente (sin dispositivo/VT-x).
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
| 08 | GPS real + banderillero (velocidad, precisión, XTE) | D | 04, 07 | ✅ PASS |
| 09 | Registro y coloreado de cobertura sobre el mapa | E | 03, 08 | ✅ PASS |
| 10 | Parcelas/sesiones UI + reanudación | F | 05, 09 | ✅ PASS |
| 11 | Foreground service + robustez (reinicios, batería, sin red) | G | 08, 10 | ✅ PASS |
| 12 | QA final + APKs + revisión seguridad + docs instalación | H | 06–11 | ✅ PASS |
| 13 | Diagnóstico fase 2 + planificación (parpadeo, punto azul, icono, zoom auto) | — | — | 🟡 en curso |
| 14 | Icono personalizado (fuente del propietario → mipmaps + adaptativo) | Icono | 13 | ✅ PASS |
| 15 | Estabilidad del mapa (anti-parpadeo: fuentes/capas/cámara) | Mapa | 13 | ✅ PASS |
| 16 | Punto azul + cono de precisión + flecha de rumbo | GPS | 13 | ✅ PASS |
| 17 | Zoom detalle automático en primer fix + botón «Mi ubicación» + v1.1.0 | GPS | 13, 15 | ✅ PASS |
| 18 | Diagnóstico con captura + diseño mapa estilo Traccar | Mapa | — | 🟡 en curso |
| 19 | Nuevo componente MapaSurco (cuadro fijo, overlays persistentes, diagnóstico) | Mapa | 18 | ✅ PASS |
| 20 | QA + v1.2.0 en releases/ | QA | 19 | ⏳ pendiente |

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
### SPRINT-13 — Diagnóstico fase 2
- Aceptación: causas del parpadeo identificadas en código + plan de sprints 14–17 publicado.
### SPRINT-14 — Icono
- Aceptación: `android:icon` propio desde la fuente del propietario (mipmaps por
  densidad + adaptativo), `aapt` resuelve el icono, nombre «SurcoGPS».
### SPRINT-15 — Estabilidad del mapa
- Aceptación: sin `setTileSource` ni recreación de capas por fix GPS; cámara solo
  por comando o seguimiento con gesto respetado; sin parpadeo inducido por recomposición.
### SPRINT-16 — Posición y orientación
- Aceptación: punto azul + cono de precisión + flecha de rumbo de marcha suavizado;
  parado = sin flecha; sin brújula (documentado); centrado disponible.
### SPRINT-17 — Zoom automático + v1.1.0
- Aceptación: primer fix válido centra + zoom 19 (una vez); gestos manuales
  respetados; botón «Mi ubicación» reencuadra; `releases/SurcoGPS-v1.1.0-release.apk`
  verificado.
### SPRINT-18 — Diagnóstico fase 3
- Aceptación: síntomas de la captura analizados + patrón Traccar investigado en
  código + diseño del nuevo componente publicado.
### SPRINT-19 — MapaSurco
- Aceptación: cuadro de mapa determinista con controles dentro; overlays
  persistentes; diagnóstico de teselas con aviso; sin recreaciones por fix.
### SPRINT-20 — QA + v1.2.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.2.0-release.apk`
  verificado (aapt/apksigner), informe.

## 6. Dependencias entre sprints
01 → 02 → 03 ↘
01 → 06 → 07 → 08 → 09 → 10 → 11 → 12 · 04 alimenta 08 · 05 alimenta 10.
Fase 2: 13 → 14 · 13 → 15 → 17 · 13 → 16.
Fase 3 (mapa): 18 → 19 → 20.

## 7. Criterios de aceptación globales
- Geometría en marco ENU; tests borde (rumbo 0/359, anchos 0.5/30 m, solapes, saltos, reanudación) en verde.
- Ninguna línea marcada como trabajada sin fixes válidos.
- Precisión estimada siempre visible; puerta configurable (defecto 10 m).
- Área sin duplicar; mapa con atribución; cero secretos en repo.
- APKs generados y verificados; instalación documentada.

## 8. Pruebas necesarias
- Unitarias JUnit5 en `:core` (obligatorias desde sprint 02).
- Integración (store round-trip, guidance+coverage+filter encadenados).
- App: `assembleDebug`, ejecución en emulador/dispositivo con mock locations.
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
| Sprint | Commit | Push a `origin/main` |
|---|---|---|
| 01 | `0d3363a` | OK |
| 02 | `6f5893d` | OK |
| 03 | `25b917e` | OK |
| 04 | `547dfde` | OK |
| 05 | `bda4ef8` | OK |
| 06 | `afe67ae` | OK |
| 07 | `e10f583` | OK |
| 08 | `597899d` | OK |
| 09 | `7ae4bc8` | OK |
| 10 | `45abf98` | OK |
| 11 | `c02f944` | OK |
| 12 | `e8285e2` | OK |
| Entrega APK | `88dd177` | OK |
| 13 | `69e58d2` | OK |
| 14 | `1d52ecc` | OK |
| 15 | `6569728` | OK |
| 16 | `9247579` | OK |
| 17 | (este commit) | (con este push) |
| 18 | (este commit) | (con este push) |

## 12. Instalación y uso (v1.0.0)

### Descarga directa desde GitHub
- **v1.1.0 (actual)**: [`releases/SurcoGPS-v1.1.0-release.apk`](releases/SurcoGPS-v1.1.0-release.apk)
  (8,6 MB, firmado, `es.surcogps.app` v1.1.0, con icono propio + mapa estable +
  punto azul + zoom automático). En la web de GitHub: carpeta `releases/` → clic → **Download**.
- URL directa: `https://github.com/joaki1991/SurcoGPS/raw/main/releases/SurcoGPS-v1.1.0-release.apk`
- v1.0.0 anterior: `releases/SurcoGPS-v1.0.0-release.apk` (conservada).

### Artefactos
- Depuración: `app/build/outputs/apk/debug/app-debug.apk` (11,6 MB, firmada con clave debug).
- Lanzamiento: `app/build/outputs/apk/release/app-release.apk` (8,3 MB, firmada con clave
  local de desarrollo `CN=SurcoGPS-Dev`, SHA-256 `39:46:59:9E:F0:F4:2B:DE:96:DD:BC:38:2F:83:A3:8B:D2:7B:52:3F:35:E9:50:70:8B:2E:7A:63:F7:3C:C4:01`).
- Requisito: Android 8.0 (API 26) o superior. Package: `es.surcogps.app`.

### Instalar en otro dispositivo
1. Copia el APK al móvil (USB, correo o descarga).
2. En el móvil: permite «instalar aplicaciones desconocidas» para la app usada
   (Ajustes → Seguridad) o confirma con `adb install app-release.apk`.
3. Abre **SurcoGPS**, pulsa **Permitir ubicación** y concede «Mientras se usa la app».
4. Sin conexión funciona lo ya visitado (caché de teselas); con red se descargan
   Mapnik (© OpenStreetMap contributors) y Esri World Imagery.

### Flujo de trabajo en campo
1. **Campo**: elige modo de mapa (Híbrido/Satélite/Calles), ajusta rumbo (0°=N,
   90°=E, 180°=S, 270°=O) y ancho del apero.
2. Pulsa **Fijar ancla aquí** (o se fija sola en la primera posición válida).
3. **Sesión → Nueva sesión**; trabaja: la franja del apero se colorea en verde y
   el HUD muestra precisión (±m, siempre visible), velocidad, línea, XTE (→/←/✓)
   y hectáreas sin duplicar.
4. **Pausar/Registrar** para cabeceras; **Terminar** al acabar.
5. **Parcela → Guardar parcela actual** para reutilizar ancla+rumbo+ancho.
6. Con pantalla apagada el tracking sigue (notificación persistente con Detener).

### Reconstruir y firmar
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :core:test                 # 70 tests
.\gradlew.bat :app:testDebugUnitTest     # 20 tests
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease       # requiere clave en local.properties (ver abajo)
```
Firma release: genera tu keystore (`keytool -genkeypair ...`) y añade a
`local.properties` (NO commitear): `surco.storeFile`, `surco.storePassword`,
`surco.keyAlias`, `surco.keyPassword`. Sin estas propiedades el release sale sin
firmar. Para Google Play genera una clave dedicada y custódiala fuera del repo.

### Verificación tras un cambio de hardware (pendiente)
Con un dispositivo a mano: instala, concede permiso, fija ancla, camina 100 m en
línea recta con ancho 6 m (≈600–780 m² por sesgo documentado del raster),
comprueba que quieto no colorea, mata la app y reanuda (misma área), y deja la
pantalla apagada 5 min (sin huecos). Registrar resultados en
`.sdd/test-reports/`.
