# SPRINT-05 — Persistencia: SessionStore
- Objetivo: nada de progreso se pierde; reanudación continúa el área.
- Alcance: `core/.../SessionStore.kt` (interfaz + impl. JVM/JSON sin dependencias),
  modelo `WorkSession` (id, parcela, ancla, rumbo, ancho, puntos, celdas compactadas por rangos,
  timestamps, estado), tests round-trip.
- Tareas: 1) modelo + interfaz 2) impl. fichero JSON (serialización manual, sin libs)
  3) compactación de celdas por filas 4) tests (round-trip, reanudar, interrumpida, vacía).
- Aceptación: guardar→cargar conserva área y puntos al 100%; reanudar suma sin duplicar.
- Pruebas: `SessionStoreTest` (≥7 casos, con directorio temporal JUnit5).
