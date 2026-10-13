# Revisión — SPRINT-09 (revisor independiente)
- Alcance: `cobertura/CoberturaEngine.kt`, `mapa/FranjaOverlay.kt`, `MapaCampo`
  (capa + param), `CampoViewModel` (registro, auto-ancla, pausa), `CampoScreen`
  (ha + botón), `CoberturaEngineTest`.
- Aceptación: solo fixes válidos colorean (registro consume `EstadoGuiado.aceptado`
  con `posicionApero`; quieto no pinta) — CUMPLE. Franja = ancho del apero en
  metros (overlay con escala por tramo) — CUMPLE. ha UI == ha core (mismo
  `CoverageTracker`) — CUMPLE. Pausa/registro funcionales — CUMPLE.
- Reglas técnicas: acercarse a la guía sin moverse NO marca trabajo (test
  `quietoNoColorea`); umbral 10 cm bajo el ruido GPS; capa única eficiente;
  dibujo capado a 20 000 tramos sin perder área — CUMPLE.
- Seguridad/privacidad: sin cambios (datos en memoria; persistencia en Sprint 10) — OK.
- Calidad: `FranjaOverlay` no es `TilesOverlay` (sobrevive a la limpieza de capas
  por cambio de modo — verificado en código); auto-ancla una sola vez; motor
  recreado al cambiar ancla/ancho; `reanudar` evita tramo fantasma.
- Regresiones: 16/16 verde + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
