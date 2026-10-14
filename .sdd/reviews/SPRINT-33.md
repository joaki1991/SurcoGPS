# Revisión — SPRINT-33 (revisor independiente)
- Constructor `RotationGestureOverlay(MapView)` verificado por `javap`; overlay
  de gestos el primero (recibe toques) sin dibujar; long-press intacto.
- Flecha compensada en el overlay (no en el estado): el estado sigue en rumbos
  geográficos. Firma incluye orientación (redibuja al girar, y solo entonces).
- Regresiones: 59/59 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
