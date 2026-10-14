# Revisión — SPRINT-27 (revisor independiente)
- El `javap` de Sprint 21 mostraba firmas Java sin nulabilidad; la presunción de
  no-nulo era el defecto. La captura lo demuestra: osmdroid pasa null.
- Ambos overrides corregidos (completed + expired). `super` acepta null (tipos
  plataforma). Sin cambios de comportamiento salvo no morir.
- Píldora HTTP: distingue red-directa de proveedor; sin auto-conmutaciones.
- Regresiones: 49/49 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
