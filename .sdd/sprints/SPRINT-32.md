# SPRINT-32 — Rumbo desde home + rendimiento
- Objetivo: definir el rumbo con dos toques en casa y eliminar la tormenta de
  emisiones/redibujados de los sliders.
- Alcance: punto final B persistido (`Ajustes`+DataStore), `rumboDesdeToque` +
  `firmaDibujo` (puros), `Motor.fijarPuntoMapa` (ANCLA/FIN/IGNORADO),
  `LineasOverlay` con marcador B y sin Toast propio, VM con Toasts,
  `fijarAnclaAqui/En` invalidan B, sliders con estado local + persistir al
  soltar (Campo + Ajustes), botón «Iniciar», firma de dibujado en el update.
- Aceptación: long-press sin ancla → ancla; con ancla → rumbo+B; <1 m ignorado;
  arrastrar sliders no emite ni redibuja; suite verde.
- Pruebas: `PuntoRumboTest` 3/3 + suite 56/56 + `assembleDebug`.
