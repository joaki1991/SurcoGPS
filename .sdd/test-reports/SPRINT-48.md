# SPRINT-48 — Informe de pruebas

Fecha: 2026-10-29. Sesión principal de OpenCode, sin subagentes.

## 1. Suites unitarias (ejecutadas de verdad, `--rerun-tasks`)

| Comando | Resultado | Casos |
|---|---|---|
| `.\gradlew.bat :core:test` | BUILD SUCCESSFUL | **79/79** (0 fallos, 0 errores) |
| `.\gradlew.bat :app:testDebugUnitTest` | BUILD SUCCESSFUL | **88/88** (0 fallos, 0 errores, 21 suites) |

Casos nuevos (13):

- `PasosAjusteTest` (6): 294 pasos intermedios → 295 posiciones de 0,1 m;
  redondeo a 1 decimal; límites 0,5/30,0; valor no finito; paso inválido.
- `FuenteGpsTest` (5): sin Play Services → `LocationManager`; con Play Services
  → Fused; umbral de respaldo 20 000 ms (19 999 no, 20 000 sí); no se repite si
  ya está activo; etiquetas de diagnóstico.
- `EstiloFranjaTest` (2): alfa de relleno ≥ 200; borde más opaco que relleno y
  grosor ≥ 2 px.

## 2. Compilación y APK

| Artefacto | Ruta | Tamaño |
|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | 12 171 677 B |
| Release | `releases/SurcoGPS-v1.9.0-release.apk` | 8 647 012 B |

Verificación real:

```
aapt dump badging releases\SurcoGPS-v1.9.0-release.apk
package: name='es.surcogps.app' versionCode='15' versionName='1.9.0'
sdkVersion:'26'
targetSdkVersion:'34'
application-label:'SurcoGPS'

apksigner verify --print-certs releases\SurcoGPS-v1.9.0-release.apk
Signer #1 certificate DN: CN=SurcoGPS-Dev
SHA-256: 3946599ef0f42bde96ddbc382f83a38bd27b523f35e950708b2e7a63f73cc401
```

SHA-256 del APK: `455446565F947B2CFE853424CCBB72C9CCFE7ECEF833A34ED30A039990B20C93`.

## 3. Verificación en emulador (AVD `surco`, Android 14 x86_64)

Emulador con `-no-snapshot -no-boot-anim -no-audio -no-window -gpu
swiftshader_indirect -memory 3072 -cores 2`, APK debug instalada y permisos de
ubicación concedidos (`pm grant`).

### 3.1 GPS sin Google Play Services (requisito principal)

Se desactivó Play Services en el emulador (`pm disable-user --user 0
com.google.android.gms`) y se relanzó la app. Registro local real
(`files/diag/diag-eventos.jsonl`):

```
gps | Play Services no operativo (código 3)
gps | respaldo del sistema (Google Play Services no disponible): 1 proveedor(es)
gps | fuente inicial: GPS del dispositivo (LocationManager)
```

y `dumpsys location` confirma que el sistema registró al app y le entregó
posiciones del proveedor GPS:

```
gps: 10192/es.surcogps.app: min/max interval = 1s/1s, locations = 16
gps provider +registration 10192/es.surcogps.app/1EDAEDA8 -> Request[@+1s0ms HIGH_ACCURACY, ...]
gps provider delivered location[1] to 10192/es.surcogps.app/1EDAEDA8   (×16)
```

Con Play Services activo, el mismo registro muestra `fuente inicial: Google Play
Services (Fused)`, es decir, no se ha roto la ruta preferente.

### 3.2 Sin red no se espera a la sonda HTTP

Se desactivó wifi y datos (`svc wifi disable; svc data disable`) y se relanzó:

```
mapa | modo HIBRIDO aplicado
gps  | fuente inicial: Google Play Services (Fused)
mapa | sonda HTTP omitida: sin red (se usa la caché)   (+325 ms)
```

La omisión ocurre en milisegundos; antes se lanzaba un HTTP con 8 s de espera.

### 3.3 Slider de 0,1 m

Captura del emulador con la app en Campo: el texto muestra `Ancho del apero:
6.0 m` y la pista del slider presenta las 294 marcas intermedias (295
posiciones), frente a las 58 anteriores. El valor se redondea a 1 decimal antes
de persistir.

### 3.4 Límite de la verificación en emulador

El emulador de este PC (7,9 GB de RAM, sin aceleración gráfica real) entra en
ANR de `System UI` y de la propia app al interactuar con el mapa; el diálogo
tapa la pantalla y no permite una captura fiable de la franja pintada sobre
satélite. Por eso **la comparación visual de opacidad en pantalla no se declara
verificada aquí**: queda pendiente en el móvil del propietario, junto con la
prueba de campo (fix real, rumbo, pintado, gestos). La opacidad sí está fijada
por código y protegida por test (`ALFA_RELLENO = 210`, antes 110).

## 4. Pendiente en dispositivo físico

- Ver que la franja verde se distingue claramente sobre satélite a pleno sol.
- Comprobar que el slider permite 0,5 → 0,6 → … y que persiste tras cerrar.
- Arrancar en un móvil sin Play Services (o con GPS solo) y confirmar fix.
- Prueba de campo: ancla, rumbo, Iniciar, pintado y solapes.

Estas comprobaciones no se han ejecutado: en este PC no hay dispositivo Android
físico conectado.
