# Revisión — SPRINT-44 (revisor independiente)
- Sin cambios en providers/fuentes/cámara/overlays (misma ruta que pinta).
  Solo cambia la PROPIEDAD del MapView (composición → ViewModel) y el
  `onDetach` difiere al final. Riesgo de contexto acotado (app context, sin
  diálogos en el mapa) y documentado.
- Sonda SQL: borra antes de probar (sin falsos positivos) y limpia después.
- Regresiones: 75/75 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
