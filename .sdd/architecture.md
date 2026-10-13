# Arquitectura — SurcoGPS

## Resumen
App Android nativa (Kotlin + Jetpack Compose, MVVM) + núcleo de dominio puro `:core`
(Kotlin/JVM sin Android) para que toda la geometría y cobertura sea unit-testeable sin SDK.

```
┌────────────────────────────── :app (Android) ──────────────────────────────┐
│ UI Compose (pantallas: Campo, Parcela, Sesión, Ajustes) · ViewModels (StateFlow) │
│ Mapa OSMDroid (Mapnik base + Esri World Imagery overlay, atribución visible)    │
│ Ubicación: FusedLocationProvider (alta precisión) + NMEA/GnssStatus (compat.    │
│   GNSS externo vía mock provider / NMEA Bluetooth, documentado)                 │
│ Cobertura overlay (polilíneas/franjas) + banderillero (XTE, línea actual)       │
│ Persistencia: Room (parcelas, sesiones, puntos, celdas) + DataStore (ajustes)   │
│ ForegroundService `location` + notificación persistente + WorkManager reintento │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ usa (dependencia Gradle)
┌────────────────────────────── :core (Kotlin puro) ─────────────────────────┐
│ GeoMath          WGS84↔ENU local (equirectangular cos(latRef)), haversine,  │
│                  rumbo inicial, destino geodésico                           │
│ GuidanceEngine   línea base (punto+rumbo), líneas paralelas cada `ancho`,   │
│                  error transversal (XTE), línea más cercana, shift lateral  │
│ CoverageTracker  franja = segmento ± ancho/2; raster celdas 1 m; área única │
│ GpsFilter        puerta precisión (def. 10 m), puerta salto (def. 50 m/s),  │
│                  caducidad fix, racha mínima                                │
│ ImplementOffset  brazo antena→apero (dx lateral, dy longitudinal) rotado    │
│                  por rumbo del vehículo                                     │
│ SessionStore     interfaces + impl. JVM/JSON para tests; Room la implementa │
│                  en :app con el mismo contrato                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Decisiones (ADR resumidos)
- **ADR-01 Marco métrico local.** Equirectangular con `cos(latRef)` alrededor del
  ancla de parcela. Error < 1 % en parcelas de < 5 km (latitudes medias). Simple,
  sin dependencia proj.4/UTM. Haversine solo verificación.
- **ADR-02 Mapa OSMDroid + Esri.** Sin API key ni facturación; genera APK
  reproducible. Mapnik como base cartográfica, Esri World Imagery como overlay
  semitransparente ("híbrido"). Alternativa Google Maps descartada (requiere key
  y billing). Coste: 0 € en uso normal; respetar max ~2 tiles/s y caché.
- **ADR-03 Cobertura raster 1 m.** Cuadrícula `Set<Long>` de celdas (clave = x*2^32+y
  cuantizados a 1 m en marco local) → área = celdas × 1 m², sin duplicar solapes.
  Memoria acotada; sesiones de 100 ha ≈ 1 M celdas máx. (aceptable; se compacta por
  rangos al persistir).
- **ADR-04 Fused + NMEA.** Fused para consumo/batería; `OnNmeaMessageListener`
  expone GGA/RMC para diagnóstico y deja puerta a GNSS externo RTK vía mock
  location (documentado, no implementado el BT propio en v1).
- **ADR-05 Room + DataStore.** Room para series (puntos, celdas); DataStore
  Preferences para ajustes (rumbo, ancho, puertas, offsets). Supervivencia a
  muerte de proceso y reinicio.
- **ADR-06 Foreground service tipo `location`.** Obligatorio desde Android 14
  para tracking con pantalla apagada. Notificación persistente con ha, XTE y botón parar.

## Definiciones normativas
- Rumbo: grados sexagesimales horarios desde el norte, [0, 360). 0=N, 90=E, 180=S, 270=O.
- Línea base: recta infinita por punto ancla con ese rumbo (en marco ENU).
- Líneas paralelas: familia separada exactamente `anchoApero` metros (ancho ∈ [0.5, 30] m).
- XTE (cross-track error): distancia con signo a la línea activa (+ = derecha del rumbo).
- Franja trabajada: cápsula de anchura `anchoApero` centrada en la trayectoria del
  **apero** (posición antena + offset rotado), solo con fixes válidos.

## Permisos Android
`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS` (33+), `INTERNET`,
`ACCESS_NETWORK_STATE`. Sin ubicación en segundo plano (`BACKGROUND_LOCATION` no se
solicita en v1: el foreground service cubre pantalla apagada).
