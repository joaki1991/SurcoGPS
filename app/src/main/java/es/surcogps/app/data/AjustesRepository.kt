package es.surcogps.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** Ajustes persistentes de la app (DataStore). Valores por defecto según AGENTS.md. */
data class Ajustes(
    val rumboDeg: Double = 0.0,
    val anchoAperoM: Double = 6.0,
    val desplazamientoLateralM: Double = 0.0,
    val offsetLateralM: Double = 0.0,
    val offsetLongitudinalM: Double = 0.0,
    val puertaPrecisionM: Float = 10f,
    /** Ancla de la guía (null = sin fijar). */
    val anclaLat: Double? = null,
    val anclaLon: Double? = null
)

/** Repositorio de ajustes con validación de rangos del dominio. */
class AjustesRepository(private val context: Context) {

    private object Keys {
        val RUMBO = doublePreferencesKey("rumbo_deg")
        val ANCHO = doublePreferencesKey("ancho_apero_m")
        val SHIFT = doublePreferencesKey("shift_lateral_m")
        val OFF_LAT = doublePreferencesKey("offset_lateral_m")
        val OFF_LON = doublePreferencesKey("offset_longitudinal_m")
        val PUERTA = floatPreferencesKey("puerta_precision_m")
        val ANCLA_LAT = doublePreferencesKey("ancla_lat")
        val ANCLA_LON = doublePreferencesKey("ancla_lon")
    }

    val ajustes: Flow<Ajustes> = context.dataStore.data.map { p ->
        Ajustes(
            rumboDeg = p[Keys.RUMBO] ?: 0.0,
            anchoAperoM = p[Keys.ANCHO] ?: 6.0,
            desplazamientoLateralM = p[Keys.SHIFT] ?: 0.0,
            offsetLateralM = p[Keys.OFF_LAT] ?: 0.0,
            offsetLongitudinalM = p[Keys.OFF_LON] ?: 0.0,
            puertaPrecisionM = p[Keys.PUERTA] ?: 10f,
            anclaLat = if (p.contains(Keys.ANCLA_LAT)) p[Keys.ANCLA_LAT] else null,
            anclaLon = if (p.contains(Keys.ANCLA_LON)) p[Keys.ANCLA_LON] else null
        )
    }

    suspend fun guardarRumbo(rumbo: Double) {
        val r = ((rumbo % 360) + 360) % 360
        context.dataStore.edit { it[Keys.RUMBO] = r }
    }

    suspend fun guardarAncho(anchoM: Double) {
        require(anchoM in 0.5..30.0) { "ancho fuera de rango" }
        context.dataStore.edit { it[Keys.ANCHO] = anchoM }
    }

    suspend fun guardarShift(shiftM: Double) {
        require(shiftM.isFinite()) { "shift no finito" }
        context.dataStore.edit { it[Keys.SHIFT] = shiftM }
    }

    suspend fun guardarOffset(lat: Double, lon: Double) {
        require(kotlin.math.hypot(lat, lon) <= 30.0) { "brazo excesivo" }
        context.dataStore.edit {
            it[Keys.OFF_LAT] = lat
            it[Keys.OFF_LON] = lon
        }
    }

    suspend fun guardarPuerta(puertaM: Float) {
        require(puertaM in 1f..50f) { "puerta fuera de rango [1, 50] m" }
        context.dataStore.edit { it[Keys.PUERTA] = puertaM }
    }

    suspend fun guardarAncla(lat: Double, lon: Double) {
        require(lat in -90.0..90.0 && lon in -180.0..180.0) { "ancla inválida" }
        context.dataStore.edit {
            it[Keys.ANCLA_LAT] = lat
            it[Keys.ANCLA_LON] = lon
        }
    }

    suspend fun borrarAncla() {
        context.dataStore.edit {
            it.remove(Keys.ANCLA_LAT)
            it.remove(Keys.ANCLA_LON)
        }
    }
}
