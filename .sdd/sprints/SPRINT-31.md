# SPRINT-31 — Diagnóstico ralentización + simplificación

## 1. Ralentización al cambiar el rumbo en Ajustes (causas en código)
1. **Persistencia por tick**: los sliders (`AjustesScreen`, `CampoScreen`) llaman a
   `DataStore.edit` (disco) en cada paso del arrastre → decenas de emisiones/s.
2. **Cada emisión re-ejecuta todo**: `guiado` (pipeline), `guiaActual` (nuevo
   `GuidanceEngine`), `ubicacion`, `onGuiado` (nueva lista de tramos) →
   recomposición total + `MapaCampo.update` → como hay guía, `invalidate()` en
   CADA tick → redibujado completo (franja + líneas + teselas) al ritmo del dedo.
3. El slider de rumbo tiene 358 pasos: un arrastre = cientos de eventos.
- Fix (S32): estado local durante el arrastre + persistir al soltar
  (`onValueChangeFinished`) + **firma de dibujado** (invalidar solo si cambian
  tramos/guía/ubicación).

## 2. Rumbo desde casa (petición UX)
- A = ancla (auto en primer fix, botón GPS o primer long-press sin ancla).
- B = long-press con ancla → rumbo = `initialBearing(A,B)` persistido + marcador.
- El slider queda como ajuste fino en Ajustes. Botón «Registrar» → «Iniciar».

## 3. Rotación (verificada por `javap` en osmdroid 6.1.20)
- `RotationGestureOverlay(MapView)` + `MapView.get/setMapOrientation`.
- La flecha debe rotarse por (rumbo − orientación) al dejar de asumir norte-arriba.

## 4. Decisión de alcance
Sin reescritura total (riesgo sin dispositivo): flujo de campo en 4 pasos
(abrir → ancla → B → Iniciar) con el mismo motor probado. Plan:
31 (este doc) → 32 (rumbo home + perf) → 33 (rotación) → 34 (QA + v1.5.0).

## Decisión: PASS (documentación).
