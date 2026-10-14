# Revisión — SPRINT-25 (revisor independiente)
- Alcance: crash reporter, handler, visor con compartir/descartar, provider
  FileProvider, fallback de MapView, tests.
- Aceptación: traza con hilo/excepción/causa/stack + rotación — CUMPLE. Encadena
  al handler del sistema (no altera el ciclo de muerte) — CUMPLE. Compartir solo
  por acción del usuario (nada sale solo) — CUMPLE. Fallback acotado al delta
  v1.3.0 (proveedor) con nota — CUMPLE (no oculta: el informe lo muestra).
- Seguridad: ficheros en almacenamiento interno; FileProvider `exported=false`
  con grant temporal; sin red — OK.
- Regresiones: 49/49 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
