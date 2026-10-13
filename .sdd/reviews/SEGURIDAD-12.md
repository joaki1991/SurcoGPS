# Revisión de seguridad — SPRINT-12 (pre-APK final, obligatoria)

## 1. Permisos (mínimo necesario, todos justificados)
| Permiso | Uso |
|---|---|
| `ACCESS_FINE/COARSE_LOCATION` | Posición del tractor (solicitud en ejecución con botón explícito; sin tracking previo) |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` | Tracking con pantalla apagada (Android 14 exige tipo `location`) |
| `POST_NOTIFICATIONS` | Notificación persistente del servicio (solicitado junto a ubicación) |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Teselas OSM/Esri + aviso offline |
| `RECEIVE_BOOT_COMPLETED` | Reanudar tracking tras reinicio (solo si estaba activo + hay permiso) |
- NO se solicita `BACKGROUND_LOCATION` (el FGS cubre pantalla apagada). Verificado en `AndroidManifest.xml`.

## 2. Secretos y claves
- Búsqueda en `app/` y `core/` de `api[_-]?key|secret|storePassword|PRIVATE KEY`: **0 resultados** en código.
- Sin `google-services.json`, sin keystores en el repo (`.gitignore`: `*.jks`, `*.keystore`, `*.p12`, `*.pem`).
- Firma release con clave LOCAL en `%USERPROFILE%\.android\surcogps-release.jks` (contraseña solo en `local.properties`, ignorado por git). Clave de desarrollo autofirmada (CN=SurcoGPS-Dev, RSA-2048, 30 años); para Play Store generar clave dedicada y custodiarla fuera del repo.

## 3. Datos y privacidad
- Ubicación: solo con permiso; rationale en el botón «Permitir ubicación»; sin transmisión a terceros.
- Parcelas/sesiones/ajustes: solo almacenamiento interno (`filesDir`, DataStore). `allowBackup=true` (copia del sistema, sin nube propia).
- Telemetría: ninguna. Único tráfico tercero: teselas OSM (`tile.openstreetmap.org`, user-agent `SurcoGPS/1.0`) y Esri (`server.arcgisonline.com`), documentado + atribución visible.

## 4. Superficie de ataque
- Sin WebView, sin deep links, sin exportación de componentes (`MainActivity` exported por ser lanzador; servicio y receptor `exported=false`).
- JSON manual con parseo acotado al esquema propio; ficheros corruptos → null/vacío sin crash (testeado).
- `minSdk 26`: sin ramas legacy de permisos en tiempo de ejecución por debajo de 23.

## 5. Dependencias
Gradle/Maven Central/Google: Compose BOM 2024.09.00, activity, navigation, lifecycle, datastore, osmdroid 6.1.20, play-services-location 21.3.0, JUnit. Sin repositorios exóticos (`FAIL_ON_PROJECT_REPOS`).

## Decisión: **PASS** (con nota: clave release = desarrollo local, no distribución en Play).
