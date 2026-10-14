# SPRINT-37 — Diagnóstico reproducible (bloqueos + mapa gris)

## 0. Rama y estado Git verificados
- Rama: `main` (la petición mencionaba `master`, pero no existe en este repo;
  `main` es la principal desde el inicio + remoto `origin`). Limpio, todo
  publicado hasta `a99d2cd` (v1.5.0). Sin cambios pendientes ajenos.

## 1. Fuentes primarias leídas (osmdroid `osmdroid-parent-6.1.20`, GitHub)
- `MapTileDownloader.TileLoader.tileLoaded`: `mapTileRequestCompleted(state, null)`
  **por diseño** (evita parpadeo; el bitmap se recicla y lo sirve la caché) →
  el NPE de v1.3.0/1.3.1 era nuestro (`Drawable` no-nulo), ya corregido (S27).
- `MapTileModuleProviderBase.run()`: captura `Throwable` en `loadTile`, pero NO
  en los callbacks → una excepción en un callback mata el hilo (pool lo repone).
- `TileDownloader.downloadTile`: puerta de UA (con política por defecto la
  nuestra pasa), no-200 → null + `Counters.tileDownloadErrors++`, excepciones
  (DNS/TLS/IO) → null. Sin reintento propio.
- `MapTileModuleProviderBase`: pool fijo, **sin reintento automático**; el exceso
  de cola falla (`…ExceedsMaxQueueSize`). Los re-disparos vienen de nuevos
  `invalidate`/pans: nuestro `invalidate` ~1 Hz re-encola las visibles no
  cacheadas → crecimiento perpetuo de fallos + CPU si el servidor limita.
- `MapTileProviderArray`: cadena por proveedores, `clearTileCache()` en
  `setTileSource` (solo cambia de modo en nuestro código: correcto).
- `MapTileSqlCacheProvider`: fallos de lectura capturados → null (degrada en
  blando; un caché corrupto por sí solo NO explica el bloqueo en reapertura).
- `Counters` (público): `tileDownloadErrors`, `fileCacheHit/Miss/SaveErrors`,
  `countOOM`, `fileCacheOOM` + `printToLogcat()` → instrumento sin subclases
  para Sprint 38 (distingue red / almacenamiento / memoria).

## 2. Conclusiones por síntoma (con grado de certeza)
- **Mapa gris + contadores creciendo**: descargas fallando en el dispositivo
  (HTTP-directo OK ⇒ red básica bien; compatible con throttling ante ráfagas +
  re-disparo 1 Hz nuestro). NO oculto (ningún overlay tapa teselas). **Alta.**
- **Cola 30→36 sin drenar**: re-encolado por nuestros invalidates + fallos
  sostenidos. **Alta.**
- **Bloqueo al reabrir (caché lo cura temporalmente)**: SIN CONFIRMAR. Las
  lecturas de caché fallan en blando según el fuente; falta logcat (ANR/fatal)
  para señalar el punto exacto. Hipótesis (no actuar): DB de teselas en mal
  estado tras muertes no limpias de la era NPE + escrituras concurrentes de dos
  proveedores. **Requiere logs.**

## 3. Lo que falta (bloquea el Sprint 38 factual)
Ver `.sdd/test-reports/SPRINT-37.md` (protocolo logcat exacto): traza del
bloqueo, líneas `osmdroid`/`AndroidRuntime`, `Counters.printToLogcat`,
tamaño de la caché y 10 ciclos abrir/cerrar. Sin esto no hay Sprint 38 honesto.

## Decisión: PASS (diagnóstico con evidencia; 38 pendiente de logs).
