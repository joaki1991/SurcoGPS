# SurcoGPS — Plan maestro SDD

> **Estado global:** ✅ FASE 13 TERMINADA (sprint 49 PASS) — entrega **v2.0.0**.
> App de campo simplificada a **una sola pantalla**: punto azul + flecha de
> rumbo, recorrido pintado según el ancho del apero (pasos de 0,1 m), líneas
> guía acotadas y mapa a pantalla completa en horizontal. Sin parcelas ni
> sesiones que gestionar; los datos ya guardados en el móvil no se tocan.
> Rama principal: `main` · Remoto: `origin` (https://github.com/joaki1991/SurcoGPS.git)
> Última actualización: 2026-10-30 (sesión principal OpenCode, sin subagentes).

## 1. Objetivo y alcance
SurcoGPS es una app Android nativa (Kotlin + Jetpack Compose) de **guiado GPS agrícola**:
genera **líneas rectas paralelas** según rumbo (0°=N, 90°=E, 180°=S, 270°=O) y ancho de
apero, registra las **pasadas reales del tractor** y **colorea la superficie trabajada**
sobre **satélite o cartografía** (una fuente cada vez). Actúa como **banderillero visual**.
**No es conducción automática.**

## 2. Funcionalidades de la v2.0.0 (pantalla única)
1. Mapa a pantalla completa (OSMDroid): satélite Esri World Imagery o calles
   OpenStreetMap, con atribución visible y conmutación con un botón.
2. Punto azul con flecha de rumbo (brújula del móvil; respaldo GPS solo en
   movimiento) y círculo de precisión estimada. Sin promesas centimétricas.
3. Recorrido pintado con el ancho real del apero, en pasos de 0,1 m (0,5–30 m).
   Solo se pinta con fixes aceptados; cambiar el ancho no borra lo ya pintado.
4. Líneas guía paralelas según ancho y orientación (0–359°), con presets N/E/S/O,
   brújula y ancla. En pantalla se dibujan solo las líneas visibles, dentro de
   10 km y con un tope de 200; si el zoom no permite distinguirlas, solo la activa.
5. Seguimiento de posición («Seguir») y botón «Centrar»; el panel de control se
   oculta para dejar todo el mapa y no tapa el punto azul.
6. Pantalla encendida durante el trabajo y servicio en primer plano con
   notificación, para seguir registrando con la app en segundo plano.
7. «Nuevo recorrido» limpia solo la pintura en memoria: las sesiones y parcelas
   guardadas en el dispositivo no se leen ni se borran.
8. Persistencia local de ajustes (ancho, orientación y puerta de precisión) en
   DataStore; sin telemetría ni claves. Internet solo mejora las teselas.

## 3. Arquitectura y tecnologías
Ver `.sdd/architecture.md`. Resumen: `:core` Kotlin/JVM puro (GeoMath, GuidanceEngine,
CoverageTracker, GpsFilter, ImplementOffset, SessionStore, tests JUnit5 sin SDK) +
`:app` Android (Compose BOM 2024.09.00, OSMDroid 6.1.20, Play-services-location,
DataStore, foreground service). En la v2 el `:app` es una sola pantalla: mapa,
panel flotante y sensores; las clases de sesiones/parcelas del `:core` se conservan
para no romper los datos ya guardados, pero la v2 no las usa. Kotlin 2.0.20 ·
AGP 8.5.2 · Gradle 8.10.2. Sin API keys. Coste de mapas: 0 € en uso normal
(política de tiles OSM/Esri).

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
| 20 | QA + v1.2.0 en releases/ | QA | 19 | ✅ PASS |
| 21 | Diagnóstico captura 2 (teselas, sangrado, líneas) + plan 22–24 | Mapa | — | ✅ PASS |
| 22 | VigilanteTeselas (ok/fallo) + píldora + UA + clipToBounds | Mapa | 21 | ✅ PASS |
| 23 | Líneas guía en mapa + ancla por toque + marcador | Guía | 21 | ✅ PASS |
| 24 | QA + v1.3.0 en releases/ | QA | 22, 23 | ✅ PASS |
| 25 | Informe de cierre en app + arranque a prueba de fallos | Estabilidad | — | ✅ PASS |
| 26 | QA + v1.3.1 en releases/ | QA | 25 | ✅ PASS |
| 27 | Fix NPE drawable nulo + HTTP en píldora | Estabilidad | — | ✅ PASS |
| 28 | QA + v1.3.2 en releases/ | QA | 27 | ✅ PASS |
| 29 | Reversión a proveedor estándar + tests locales | Mapa | — | ✅ PASS |
| 30 | QA + v1.4.0 en releases/ | QA | 29 | ✅ PASS |
| 31 | Diagnóstico ralentización + plan simplificación | Perf | — | ✅ PASS |
| 32 | Rumbo desde home (A+B) + sliders al soltar + firma de dibujado | Perf/UX | 31 | ✅ PASS |
| 33 | Rotación del mapa + flecha corregida | Mapa | 31 | ✅ PASS |
| 34 | QA + v1.5.0 en releases/ | QA | 32, 33 | ✅ PASS |
| 35 | Quitar topes de descarga (vuelven valores de serie) | Mapa | — | ✅ PASS |
| 36 | QA + v1.5.1 en releases/ | QA | 35 | ✅ PASS |
| 37 | Diagnóstico bloqueos + mapa (fuentes osmdroid + protocolo) | Diag | — | ✅ PASS |
| 38 | Diagnóstico autónomo en app (registro, pantalla, exportar) | Diag | 37 | ✅ PASS |
| 39 | QA + v1.6.0 en releases/ | QA | 38 | ✅ PASS |
| 40 | Sonda de almacenamiento + informe | Diag | 37 | ✅ PASS |
| 41 | QA + v1.6.1 en releases/ | QA | 40 | ✅ PASS |
| 42 | Cache de teselas en dir garantizado | Mapa | 37 | ✅ PASS |
| 43 | QA + v1.6.2 en releases/ | QA | 42 | ✅ PASS |
| 44 | Mapa retenido + sonda SQL corregida | Mapa | 37 | ✅ PASS |
| 45 | QA + v1.7.0 en releases/ | QA | 44 | ✅ PASS |
| 46 | Reapertura con datos (índice de sesiones) | Rendimiento | 45 | ✅ PASS |
| 47 | QA + v1.8.0 en releases/ | QA | 46 | ✅ PASS |
| 48 | Ancho 0,1 m, pintado opaco y GPS sin Internet | GPS+UI | 47 | PASS |
| 49 | Simplificación v2.0.0: pantalla única, punto/flecha, pintado por ancho, líneas acotadas | App | 48 | ✅ PASS |

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
### SPRINT-21 — Diagnóstico fase 4
- Aceptación: causas de teselas vacías/sangrado/falta de líneas analizadas con
  evidencia (curl + javap) + plan 22–24 publicado.
### SPRINT-22 — Teselas con diagnóstico
- Aceptación: contadores ok/fallo por proveedor en la píldora; UA con contacto;
  `clipToBounds` (sin sangrado); suite verde.
### SPRINT-23 — Líneas guía
- Aceptación: paralelas del ancho dibujadas + activa resaltada + ancla visible;
  long-press fija ancla; suite verde.
### SPRINT-24 — QA + v1.3.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.3.0-release.apk`
  verificado, informe.
### SPRINT-25 — Informe de cierre + arranque seguro
- Aceptación: cierre con traza compartible desde la app; fallback a mapa
  estándar anotado; suite verde.
### SPRINT-26 — QA + v1.3.1
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.3.1-release.apk`
  verificado, informe.
### SPRINT-27 — Fix NPE + HTTP en píldora
- Aceptación: overrides con `Drawable?` (sin NPE); píldora con HTTP; suite verde.
### SPRINT-28 — QA + v1.3.2
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.3.2-release.apk`
  verificado, informe.
### SPRINT-29 — Reversión + tests locales
- Aceptación: proveedor estándar, hilos/cola acotados, píldora de cola,
  servidor HTTP local en tests, suite verde.
### SPRINT-30 — QA + v1.4.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.4.0-release.apk`
  verificado, informe.
### SPRINT-31 — Diagnóstico ralentización + simplificación
- Aceptación: causas (sliders por tick, invalidate por tick) identificadas +
  plan 32–34 publicado.
### SPRINT-32 — Rumbo home + rendimiento
- Aceptación: long-press fija B y rumbo; sliders persisten al soltar;
  invalidate solo con cambios; suite verde.
### SPRINT-33 — Rotación
- Aceptación: gesto de rotación + flecha corregida por orientación; suite verde.
### SPRINT-34 — QA + v1.5.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.5.0-release.apk`
  verificado, informe.
### SPRINT-35 — Quitar topes de descarga
- Aceptación: vuelta a valores de serie; suite verde.
### SPRINT-36 — QA + v1.5.1
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.5.1-release.apk`
  verificado, informe.
### SPRINT-37 — Diagnóstico bloqueos + mapa
- Aceptación: fuentes osmdroid citadas, causas graduadas, protocolo logcat,
  info faltante documentada. Sin cambios de código.
### SPRINT-38 — Diagnóstico autónomo en app
- Aceptación: pantalla en Ajustes, registro persistente, botón de chequeo
  limitado, exportar TXT, suite verde.
### SPRINT-39 — QA + v1.6.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.6.0-release.apk`
  verificado, informe.
### SPRINT-40 — Sonda de almacenamiento
- Aceptación: sonda directa + SQL real en diagnóstico e informe; suite verde.
### SPRINT-41 — QA + v1.6.1
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.6.1-release.apk`
  verificado, informe.
### SPRINT-42 — Caché garantizada
- Aceptación: fallback a caché interna con sonda real; suite verde.
### SPRINT-43 — QA + v1.6.2
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.6.2-release.apk`
  verificado, informe.
### SPRINT-44 — Mapa retenido + sonda SQL
- Aceptación: una creación de MapView por proceso; veredicto SQL honesto;
  suite verde.
### SPRINT-45 — QA + v1.7.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.7.0-release.apk`
  verificado, informe.
### SPRINT-46 — Reapertura con datos acumulados
- Aceptación: `list()` deja de escalar con las celdas (índice por sesión),
  autoguardado sin reparsear el resto, sin recarga al reabrir en el mismo
  proceso, guardado atómico; medición antes/después real.
### SPRINT-47 — QA + v1.8.0
- Aceptación: suites verdes, builds OK, `releases/SurcoGPS-v1.8.0-release.apk`
  verificado, informe.

### SPRINT-48 - Ancho de 0,1 m, pintado opaco y GPS sin Internet
- Aceptación: slider 0,5-30 m en pasos de 0,1 (294 pasos intermedios) con
  redondeo a 1 decimal; franja con alfa de relleno 210 y borde 245; GPS con
  Fused si existe y `LocationManager` del dispositivo si no, con respaldo a los
  20 s sin fix; sin red no se espera la sonda HTTP. Suites core 79/79 y app
  88/88; APK v1.9.0 (versionCode 15) verificada. Verificación visual en móvil
  físico pendiente (el emulador entra en ANR de System UI).

### SPRINT-49 - Simplificación v2.0.0 (pantalla única de campo)
- Aceptación: una sola pantalla edge-to-edge; mapa a pantalla completa en
  horizontal con panel flotante ocultable; punto azul + flecha de brújula +
  círculo de precisión; recorrido pintado con el ancho del apero (0,1 m) sin
  borrarse al cambiarlo; líneas guía según ancho/orientación acotadas a lo
  visible, a 10 km y a 200 líneas; «Nuevo recorrido» limpia solo la memoria;
  las capas sobreviven a la rotación de pantalla; sin crash al arrancar.
  Suites core 79/79 y app 103/103 (15 casos nuevos); APK v2.0.0 (versionCode
  16) verificada y probada en emulador Android 14. Brújula, GPS de campo y
  consumo pendientes de móvil físico.

## 6. Dependencias entre sprints
01 → 02 → 03 ↘
01 → 06 → 07 → 08 → 09 → 10 → 11 → 12 · 04 alimenta 08 · 05 alimenta 10.
Fase 2: 13 → 14 · 13 → 15 → 17 · 13 → 16.
Fase 3 (mapa): 18 → 19 → 20.
Fase 4 (teselas + líneas): 21 → 22 · 21 → 23 · 22 + 23 → 24.
Fase 5 (cierre v1.3.0): 25 → 26.
Fase 6 (fix NPE): 27 → 28.
Fase 7 (reversión mapa): 29 → 30.
Fase 8 (fluidez + rumbo home + rotación): 31 → 32 · 31 → 33 · 32 + 33 → 34.
Fase 9 (topes de descarga): 35 → 36.
Fase 10 (bloqueos + mapa gris): 37 → 38 → 39 · 40 → 41 · 42 → 43 · 44 → 45.
Fase 11 (reapertura con datos): 46 → 47.
Fase 12 (ancho 0,1 m + GPS sin Internet): 48.
Fase 13 (simplificación de campo): 49 (sustituye la UI de las fases 2–12;
conserva `:core` y los datos guardados).

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
| 17 | `6f84a2b` | OK |
| 18 | `906b445` | OK |
| 19 | `c5c5d68` | OK |
| 20 | `4251705` | OK |
| 21 | `dde1854` | OK |
| 22 | `437741d` | OK |
| 23 | `3add192` | OK |
| 24 | `0dc82d0` | OK |
| 25 | `c4e3a44` | OK |
| 26 | `b434cbb` | OK |
| 27 | `a13b8e6` | OK |
| 28 | `a121cf0` | OK |
| 29 | `ff64341` | OK |
| 30 | `a99d2cd` | OK |
| 31 | `091e438` | OK |
| 32 | `04902f0` | OK |
| 33 | `2743fcf` | OK |
| 34 | `a236943` | OK |
| 35 | `b2fd183` | OK |
| 36 | `c9abe2f` | OK |
| 37 | `0375373` | OK |
| 38 | `4f4cc87` | OK |
| 39 | `52782a8` | OK |
| 40 | `6cc1bc1` | OK |
| 41 | `e41e728` | OK |
| 42 | `e0fc871` | OK |
| 43 | `f27809b` | OK |
| 44 | `9ec74ab` | OK |
| 45 | `fc99257` | OK |
| 46 | `c1911b7` | OK |
| 47 | `7e0998e` | OK |
| 48 | `be25ce4` | OK |
| 49 | (este commit) | (con este push) |

## 12. Instalación y uso (v2.0.0)

### Descarga directa desde GitHub
- **v2.0.0 (actual)**: [`releases/SurcoGPS-v2.0.0-release.apk`](releases/SurcoGPS-v2.0.0-release.apk)
  (8,5 MB, firmado, `es.surcogps.app` versión 2.0.0). Pantalla única de campo:
  punto azul con flecha, pintado por ancho de apero y líneas guía acotadas.
- URL directa: `https://github.com/joaki1991/SurcoGPS/raw/main/releases/SurcoGPS-v2.0.0-release.apk`
- Anteriores: `releases/SurcoGPS-v1.9.0-release.apk`, `releases/SurcoGPS-v1.8.0-release.apk`,
  `releases/SurcoGPS-v1.7.0-release.apk`, `releases/SurcoGPS-v1.6.2-release.apk`,
  `releases/SurcoGPS-v1.6.1-release.apk`, `releases/SurcoGPS-v1.6.0-release.apk`,
  `releases/SurcoGPS-v1.5.1-release.apk`, `releases/SurcoGPS-v1.5.0-release.apk`,
  `releases/SurcoGPS-v1.4.0-release.apk`, `releases/SurcoGPS-v1.3.2-release.apk`,
  `releases/SurcoGPS-v1.3.1-release.apk`, `releases/SurcoGPS-v1.3.0-release.apk`,
  `releases/SurcoGPS-v1.2.0-release.apk`, `releases/SurcoGPS-v1.1.0-release.apk`,
  `releases/SurcoGPS-v1.0.0-release.apk`.

### Artefactos
- Depuración: `releases/SurcoGPS-v2.0.0-debug.apk` (12,0 MB, clave debug).
- Lanzamiento: `releases/SurcoGPS-v2.0.0-release.apk` (8,5 MB, firmada con la clave
  local de desarrollo `CN=SurcoGPS-Dev`; SHA-256 del APK
  `073B725EBBFE406365F9D266A45C9CB5195160036A379CF4C6E1642E750051F5`).
- Requisito: Android 8.0 (API 26) o superior. Package: `es.surcogps.app`.

### Instalar en otro dispositivo
1. Copia el APK al móvil (USB, correo o descarga) o usa `adb install -r`.
2. Permite «instalar aplicaciones desconocidas» para la app usada, o confirma
   con `adb install app-release.apk`. Instalar **encima** conserva los datos.
3. Abre **SurcoGPS** y concede la ubicación precisa («Mientras se usa la app»).
4. Sin conexión funciona lo ya visitado (caché de teselas); con red se descargan
   Esri World Imagery o Mapnik (© OpenStreetMap contributors).

### Flujo de trabajo en campo (una sola pantalla)
1. Al abrir, el GPS arranca solo y el ancla de las líneas se coloca en la primera
   posición válida (o pulsa **Ancla aquí** cuando quieras recolocarla).
2. Ajusta el **ancho del apero** con «−»/«+» (pasos de 0,1 m) o el deslizador.
3. Elige la **orientación**: deslizador, presets N/E/S/O, **Usar brújula** o deja
   que el rumbo actual marque la dirección. La línea activa se resalta en amarillo.
4. Trabaja siguiendo las líneas: la franja del apero se pinta en verde con cada
   fix aceptado. La píldora muestra precisión (±m), velocidad y hectáreas.
5. **Pausar pintura** en cabeceras; **Centrar** vuelve a tu posición; **Seguir**
   mantiene el punto a la vista; **Ocultar** deja solo el mapa.
6. **Nuevo recorrido** borra únicamente lo pintado en pantalla: las sesiones y
   parcelas guardadas en el móvil no se tocan.

### Reconstruir y firmar
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :core:test                 # 79 tests
.\gradlew.bat :app:testDebugUnitTest     # 103 tests
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease       # requiere clave en local.properties (ver abajo)
```
Firma release: genera tu keystore (`keytool -genkeypair ...`) y añade a
`local.properties` (NO commitear): `surco.storeFile`, `surco.storePassword`,
`surco.keyAlias`, `surco.keyPassword`. Sin estas propiedades el release sale sin
firmar. Para Google Play genera una clave dedicada y custódiala fuera del repo.

### Verificación pendiente en móvil físico
Con un dispositivo a mano: instala encima de la versión anterior y comprueba que
los datos guardados siguen ahí; concede ubicación; verifica que la flecha apunta
al rumbo real al girar el móvil; camina 100 m recto con ancho 6 m y comprueba el
pintado y las hectáreas; cambia el ancho a mitad de recorrido (no debe borrarse
lo pintado); deja la pantalla apagada 5 min (debe seguir registrando); revisa
consumo de batería. Registrar resultados en `.sdd/test-reports/`.
