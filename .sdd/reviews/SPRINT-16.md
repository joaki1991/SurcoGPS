# Revisión — SPRINT-16 (revisor independiente)
- Alcance: `Suavizado.kt`, `UbicacionOverlay.kt`, `EstadoUbicacion`, `MapaCampo`
  (param + capa única), VM (`ubicacion` + umbral), `CampoScreen`, tests.
- Aceptación: punto azul con fix válido — CUMPLE (capa dedicada). Cono de
  precisión con radio métrico — CUMPLE. Flecha de marcha suavizada, sin saltos,
  ausente parado — CUMPLE (tests). Sin confusión marcha/brújula (marcha; brújula
  descartada con motivo) — CUMPLE. Sin permiso/GPS → nada dibujado + HUD
  existente — CUMPLE. Sin duplicación (instancia `remember` + `contains`) — CUMPLE.
  Zoom/pan no la hacen desaparecer (capa del mapa, no marcador efímero) — CUMPLE.
- Seguridad: sin cambios. Calidad: escala px/m por tramo con haversine (válida a
  cualquier zoom/latitud); pinturas como campos (sin allocations por frame salvo
  `Path`/`Point` menores — aceptable).
- Regresiones: 31/31 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
