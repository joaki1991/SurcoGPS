# SPRINT-27 — Fix NPE drawable nulo + HTTP en píldora
- Causa (informe del propietario, v1.3.1): `NullPointerException` en hilo
  `downloader`: osmdroid entrega `drawable` NULO en `mapTileRequestCompleted`
  (y puede hacerlo en `ExpiredTile`); el override Kotlin lo declaraba no-nulo →
  chequeo generado → muerte del proceso. Cada tesela completada-sin-imagen
  tumbaba la app.
- Alcance: `aDrawable: Drawable?` en ambos overrides (+nota), `textoTeselas`
  con resultado HTTP (`HTTP ok/fallo/…`), `httpOk` por modo en `MapaCampo`,
  tests actualizados.
- Dato clave del informe: `0 ok · 68 fallo` → las teselas SÍ se piden y fallan
  en red/descarga en ese dispositivo (no es render). La píldora HTTP dirá si el
  HTTP directo funciona (pendiente de captura).
- Aceptación: sin NPE posible en callbacks; suite verde.
- Pruebas: suite 49/49 + `assembleDebug`.
