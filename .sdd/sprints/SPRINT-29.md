# SPRINT-29 — Reversión a proveedor estándar + tests locales
- Objetivo: volver a la ruta de teselas que renderizaba, sin reintroducir parpadeo.
- Alcance: `MapView(context)` interno; referencia con `MapTileProviderBasic`
  estándar; hilos de descarga 4 + cola máx. 40; sondeo de cola 1×/s en píldora
  (`textoCola`); `clipToBounds` (se mantiene); ELIMINADO `VigilanteTeselas`
  (rompía descarga y tumbaba el hilo) + su test; `DiagnosticoTeselasTest` con
  `ServerSocket` local + `TextoColaTest`.
- Se MANTIENE (anti-parpadeo): guardia por modo, cámara por comandos, umbral,
  gesto manual, watchdog HTTP, overlays persistentes, líneas, punto azul.
- Aceptación: suite verde incl. prueba local extremo a extremo de la pila HTTP;
  build verde.
- Pruebas: 53/53 + `assembleDebug`. Servidores reales verificados por curl.
