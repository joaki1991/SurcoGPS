package es.surcogps.app.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import es.surcogps.core.Fix
import es.surcogps.core.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente de ubicación real: FusedLocationProvider (alta precisión) + diagnóstico
 * NMEA/GNSS. Sin promesas centimétricas: cada fix lleva su precisión estimada.
 *
 * Compatibilidad GNSS externo: un receptor RTK/Bluetooth que inyecte posición vía
 * mock provider es consumido igual que el GPS interno (misma API de Fused); las
 * sentencias NMEA crudas se exponen en [nmeaSentencias] a efectos de diagnóstico.
 */
class LocationTracker(private val context: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _fix = MutableStateFlow<Fix?>(null)
    /** Último fix crudo (sin filtrar). */
    val fix: StateFlow<Fix?> = _fix.asStateFlow()

    private val _nmeaSentencias = MutableStateFlow(0)
    /** Nº de sentencias NMEA vistas (diagnóstico GNSS). */
    val nmeaSentencias: StateFlow<Int> = _nmeaSentencias.asStateFlow()

    private var activo = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val l = result.lastLocation ?: return
            _fix.value = Fix(
                position = LatLon(l.latitude, l.longitude),
                accuracyM = if (l.hasAccuracy()) l.accuracy else Float.NaN,
                speedMps = if (l.hasSpeed()) l.speed else null,
                timeMs = l.time
            )
        }
    }

    private val nmeaListener = OnNmeaMessageListener { _, _ ->
        _nmeaSentencias.value = _nmeaSentencias.value + 1
    }

    /**
     * Inicia las actualizaciones (requiere permiso ya concedido; el llamador lo
     * garantiza vía [tienePermiso]). Intervalo 1 s para guiado en campo.
     */
    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (activo) return
        activo = true
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMaxUpdateDelayMillis(2_000L)
            .build()
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        try {
            locationManager.addNmeaListener(nmeaListener)
        } catch (_: SecurityException) {
            // Sin permiso no hay NMEA; el estado lo refleja la UI.
        }
    }

    fun detener() {
        if (!activo) return
        activo = false
        fused.removeLocationUpdates(callback)
        try {
            locationManager.removeNmeaListener(nmeaListener)
        } catch (_: Exception) {
            // Ya eliminado o sin permiso: nada que hacer.
        }
    }
}
