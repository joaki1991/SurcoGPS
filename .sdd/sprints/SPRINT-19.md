# SPRINT-19 — Nuevo componente MapaSurco (estilo Traccar)
- Objetivo: cuadro de mapa determinista con controles dentro y diagnóstico de teselas.
- Alcance: `MapaCampo.kt` reescrito (Box único + `fillMaxSize`, sin pesos anidados;
  chips/estado/zoom/ubicación flotando sobre el mapa; `CopyrightOverlay` dentro;
  sin zoom integrado; overlays persistentes, referencia insertada bajo franja),
  `FuentesMapa.kt` (`urlMuestraEsri`, `resolverFuente`, `EstadoRed/Fuente`),
  `DiagnosticoTeselas.kt` (HEAD/GET con timeout), `ResolverFuenteTest`.
- Tareas: layout, overlays, watchdog por cambio de modo, píldora de estado, tests.
- Aceptación: chips y botones imposibles de desplazar fuera (dentro del Box);
  fuentes/capas/overlays creados una vez; diagnóstico distingue red de render;
  sin conmutación mágica de modo.
- Pruebas: `ResolverFuenteTest` 5/5 + suite 40/40 + `assembleDebug`.
