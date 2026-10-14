# SPRINT-21 — Diagnóstico captura 2 + plan 22-24

## 1. Síntomas (captura v1.2.0, con chips ya visibles)
- **A. Sin teselas en NINGÚN modo** (Híbrido/Satélite/Calles): cuadrícula gris.
  Overlays propios SÍ dibujan (punto azul visible) → vista, proyección e
  `invalidate` funcionan; falla la RUTA de teselas en conjunto.
- **B. El mapa sangra sobre el menú inferior al desplazar**: el `AndroidView` pinta
  fuera de sus límites → falta `Modifier.clipToBounds()` (causa directa).
- **C. Sin líneas guía**: el motor las calcula (XTE vivo en HUD) pero nunca se
  dibujan. El botón «Mover ancla» usa el GPS; se pide además fijar ancla por toque.

## 2. Verificaciones con evidencia (2026-10-14, este PC con red)
- Esri `.../tile/10/200/100` → **200, JPEG 256×256 válido** (URL nuestra correcta).
- `tile.openstreetmap.org/0/0/0` → **200** (llega); `6/31/24` y `10/500/395` →
  **400 «invalid tile»** incluso con UA con contacto. `www.openstreetmap.org` → 200.
  Conclusión: OSM responde pero con throttling/bloqueo intermitente por IP/UA.
  Acción: UA con contacto según su política + contadores reales en el dispositivo.
- `javap` sobre osmdroid 6.1.20: `MapView(Context, MapTileProviderBase)` y
  `setTileProvider()` existen; `MapTileProviderBasic` implementa
  `IMapTileProviderCallback` con métodos públicos sobrescribibles
  (`mapTileRequestCompleted/Failed/ExpiredTile`) → base del `VigilanteTeselas`.

## 3. Plan
| Sprint | Contenido | Deps |
|---|---|---|
| 21 | Diagnóstico + plan (este documento) | — |
| 22 | `VigilanteTeselas` (contadores ok/fallo por proveedor) + píldora con cifras + UA con contacto + `clipToBounds` | 21 |
| 23 | `LineasOverlay` (familia de paralelas + activa + ancla) + ancla por long-press + motor/VM | 21 |
| 24 | QA + v1.3.0 en releases/ | 22, 23 |

La próxima captura del propietario debe mostrar la píldora con cifras
(«O:24 F:0» u «O:0 F:9»), que dirá si es red o render.

## Decisión: PASS (documentación).
