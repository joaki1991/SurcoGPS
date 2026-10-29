package es.surcogps.app.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.core.Fix
import es.surcogps.core.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuente de ubicación real con dos caminos intercambiables:
 *
 * - **Fused** (Google Play Services), si está operativo: mejor fusión de
 *   sensores y bajo consumo.
 * - **LocationManager** del dispositivo (GPS + red): funciona sin Play Services
 *   y sin Internet, que es el caso normal en el campo.
 *
 * Internet no es requisito: el GPS interno entrega los fixes igualmente y el
 * respaldo por red solo aporta precisión extra cuando hay cobertura.
 *
 * Cada fix lleva su precisión estimada; no se promete precisión centimétrica.
 * Las sentencias NMEA crudas se cuentan en [nmeaSentencias] (diagnóstico).
 */
class LocationTracker(private val context: Context) {

    /**
     * Play Services es opcional: en móviles sin él (o con los servicios
     * desactivados) ni se intenta. La comprobación no debe tumbar la app.
     */
    private val fused: FusedLocationProviderClient? = crearFused()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val handler = Handler(Looper.getMainLooper())

    private val _fix = MutableStateFlow<Fix?>(null)
    /** Último fix crudo (sin filtrar). */
    val fix: StateFlow<Fix?> = _fix.asStateFlow()

    private val _fuente = MutableStateFlow<FuenteGps?>(null)
    /** Origen del último fix (Fused o GPS del sistema), para diagnóstico. */
    val fuente: StateFlow<FuenteGps?> = _fuente.asStateFlow()

    private val _nmeaSentencias = MutableStateFlow(0)
    /** Nº de sentencias NMEA vistas (diagnóstico GNSS). */
    val nmeaSentencias: StateFlow<Int> = _nmeaSentencias.asStateFlow()

    private var activo = false
    private var inicioMs = 0L
    private var plataformaActiva = false
    /** Solo se registra el primer fix de cada arranque (diagnóstico, sin spam). */
    private var primerFixHecho = false

    private val callbackFused = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val l = result.lastLocation ?: return
            publicar(l, FuenteGps.FUSED)
        }
    }

    // LocationListener conserva los cuatro métodos para funcionar igual en
    // API 26 (minSdk) que en versiones donde algunos son opcionales.
    @Suppress("DEPRECATION")
    private val listenerPlataforma = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            publicar(location, FuenteGps.PLATAFORMA)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    private val nmeaListener = OnNmeaMessageListener { _, _ ->
        _nmeaSentencias.value = _nmeaSentencias.value + 1
    }

    /** Vigila que Fused entregue algo; si no, añade el GPS del sistema. */
    private val tareaRespaldo = object : Runnable {
        override fun run() {
            if (!activo || plataformaActiva) return
            val ultimo = _fix.value?.timeMs?.takeIf { it >= inicioMs } ?: inicioMs
            if (debeActivarRespaldo(ultimo, System.currentTimeMillis(), plataformaActiva)) {
                iniciarPlataforma("sin fix de Fused en ${UMBRAL_RESPALDO_MS / 1000} s")
            } else {
                handler.postDelayed(this, 5_000L)
            }
        }
    }

    /**
     * Inicia las actualizaciones (requiere permiso ya concedido; el llamador lo
     * garantiza). Intervalo 1 s para guiado en campo.
     */
    @SuppressLint("MissingPermission")
    fun iniciar() {
        if (activo) return
        activo = true
        inicioMs = System.currentTimeMillis()
        plataformaActiva = false
        primerFixHecho = false

        if (elegirFuenteGps(fused != null) == FuenteGps.FUSED) {
            _fuente.value = FuenteGps.FUSED
            if (pedirFused()) {
                handler.postDelayed(tareaRespaldo, 5_000L)
            } else {
                iniciarPlataforma("Fused no disponible al arrancar")
            }
        } else {
            iniciarPlataforma("Google Play Services no disponible")
        }

        try {
            locationManager.addNmeaListener(nmeaListener)
        } catch (_: SecurityException) {
            // Sin permiso no hay NMEA; el estado lo refleja la UI.
        }
        RegistroDiag.registrar(
            context.filesDir, "gps", "fuente inicial: ${etiquetaFuente(_fuente.value)}"
        )
    }

    fun detener() {
        if (!activo) return
        activo = false
        handler.removeCallbacks(tareaRespaldo)
        try {
            fused?.removeLocationUpdates(callbackFused)
        } catch (_: Exception) {
            // Sin Play Services o ya retirado: nada que hacer.
        }
        try {
            locationManager.removeUpdates(listenerPlataforma)
        } catch (_: Exception) {
            // Ya eliminado o sin permiso.
        }
        plataformaActiva = false
        try {
            locationManager.removeNmeaListener(nmeaListener)
        } catch (_: Exception) {
            // Ya eliminado o sin permiso.
        }
        _fuente.value = null
    }

    @SuppressLint("MissingPermission")
    private fun pedirFused(): Boolean {
        val cliente = fused ?: return false
        return try {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .setMaxUpdateDelayMillis(2_000L)
                .build()
            cliente.requestLocationUpdates(req, callbackFused, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    RegistroDiag.registrar(
                        context.filesDir, "gps",
                        "Fused rechazó las actualizaciones: ${e.javaClass.simpleName}"
                    )
                }
            true
        } catch (e: Exception) {
            RegistroDiag.registrar(
                context.filesDir, "gps", "Fused falló al arrancar: ${e.javaClass.simpleName}"
            )
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarPlataforma(motivo: String) {
        if (plataformaActiva) return
        plataformaActiva = true
        var pedidos = 0
        for (proveedor in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                if (!locationManager.isProviderEnabled(proveedor)) continue
                locationManager.requestLocationUpdates(
                    proveedor, 1_000L, 0f, listenerPlataforma, Looper.getMainLooper()
                )
                pedidos++
            } catch (e: Exception) {
                RegistroDiag.registrar(
                    context.filesDir, "gps",
                    "$proveedor no disponible: ${e.javaClass.simpleName}"
                )
            }
        }
        if (_fuente.value == null) _fuente.value = FuenteGps.PLATAFORMA
        RegistroDiag.registrar(
            context.filesDir, "gps",
            "respaldo del sistema ($motivo): $pedidos proveedor(es)"
        )
    }

    /**
     * Publica el fix más reciente. Si Fused y el sistema entregan a la vez, no
     * se retrocede en el tiempo y gana el de mejor precisión.
     */
    private fun publicar(l: Location, origen: FuenteGps) {
        val actual = _fix.value
        if (actual != null) {
            if (l.time < actual.timeMs - 500L) return
            if (l.time <= actual.timeMs && l.hasAccuracy() && l.accuracy > actual.accuracyM) return
        }
        _fix.value = Fix(
            position = LatLon(l.latitude, l.longitude),
            accuracyM = if (l.hasAccuracy()) l.accuracy else Float.NaN,
            speedMps = if (l.hasSpeed()) l.speed else null,
            timeMs = l.time
        )
        _fuente.value = origen
        if (!primerFixHecho) {
            primerFixHecho = true
            val precision = if (l.hasAccuracy()) "±${"%.0f".format(l.accuracy)} m" else "sin precisión"
            RegistroDiag.registrar(
                context.filesDir, "gps",
                "primer fix de ${etiquetaFuente(origen)} ($precision)"
            )
        }
    }

    private fun crearFused(): FusedLocationProviderClient? = try {
        val estado = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        if (estado == ConnectionResult.SUCCESS) {
            LocationServices.getFusedLocationProviderClient(context)
        } else {
            RegistroDiag.registrar(
                context.filesDir, "gps", "Play Services no operativo (código $estado)"
            )
            null
        }
    } catch (t: Throwable) {
        // Fabricantes sin Play Services pueden no traer ni las clases base.
        RegistroDiag.registrar(
            context.filesDir, "gps", "Play Services inaccesible: ${t.javaClass.simpleName}"
        )
        null
    }
}
