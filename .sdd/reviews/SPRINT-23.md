# Revisión — SPRINT-23 (revisor independiente)
- Alcance: `LineasGuia.kt`, `LineasOverlay.kt`, motor/VM/pantalla, tests.
- Aceptación: paralelas del ancho sobre la vista (rango por bbox + margen) —
  CUMPLE. Activa resaltada con `linea` del pipeline — CUMPLE. Ancla marcada —
  CUMPLE. Long-press fija ancla validada + Toast — CUMPLE. Coloreado por donde
  pasa según ancho (FranjaOverlay intacta, alimentada por fixes válidos) — CUMPLE.
- Seguridad: ancla validada en rango; sin cambios de permisos. OK.
- Calidad: overlay único (dibujo + gesto juntos, sin duplicar capas); long-press
  no interfiere con scroll/zoom (gestos distintos); `MapListener` intacto.
- Regresiones: 45/45 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
