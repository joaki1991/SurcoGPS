package es.surcogps.app.gps

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import es.surcogps.app.mapa.SuavizadorRumbo
import es.surcogps.core.GeoMath
import es.surcogps.core.LatLon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Orientación en la que mira la persona, leída de la brújula del teléfono.
 *
 * - Usa `TYPE_ROTATION_VECTOR` (incluye giroscopio + magnetómetro) y cae a
 *   `TYPE_ORIENTATION` si el fabricante no expone el vector de rotación.
 * - Corrige la declinación magnética con [GeomagneticField] para trabajar con
 *   norte geográfico, que es el mismo de las líneas guía (0° = N).
 * - Compensa la rotación de la pantalla: la flecha sigue apuntando a donde mira
 *   la persona aunque el móvil esté en horizontal.
 * - Emite como máximo ~10 veces/s y suaviza el vector para que la flecha no
 *   tiemble. Sin sensor no inventa nada: [rumbo] queda en `null` y la UI usa el
 *   rumbo de desplazamiento del GPS como respaldo.
 */
class RumboSensor(context: Context) : SensorEventListener {

    private val sensores =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorRotacion = sensores.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val sensorOrientacion = sensores.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _rumbo = MutableStateFlow<Double?>(null)
    /** Rumbo geográfico suavizado en [0, 360), o null si no hay brújula. */
    val rumbo: StateFlow<Double?> = _rumbo.asStateFlow()

    /** ¿El dispositivo tiene algún sensor de orientación utilizable? */
    val disponible: Boolean get() = sensorRotacion != null || sensorOrientacion != null

    private val suavizador = SuavizadorRumbo(0.25)
    private val matrizRotacion = FloatArray(9)
    private val matrizRemapeada = FloatArray(9)
    private val orientacion = FloatArray(3)

    private var activo = false
    private var rotacionPantalla = 0
    private var declinacionDeg = 0.0
    private var ultimaLat: Double? = null
    private var ultimaLon: Double? = null
    private var ultimaDeclinacionMs = 0L
    private var ultimoEmitidoMs = 0L
    private var ultimoValorDeg = Double.NaN

    /**
     * Rotación actual de la pantalla (0, 90, 180 o 270). La UI la actualiza al
     * girar el móvil; no se consulta aquí para no depender de una Activity.
     */
    fun actualizarRotacionPantalla(rotacion: Int) {
        rotacionPantalla = ((rotacion % 360) + 360) % 360
    }

    /**
     * Recalcula la declinación magnética cuando el usuario se ha movido lo
     * suficiente. La precisión de la brújula no necesita recalcularla cada fix.
     */
    fun actualizarPosicion(p: LatLon) {
        val ahora = System.currentTimeMillis()
        val lat = ultimaLat
        val lon = ultimaLon
        val lejos = lat == null || lon == null ||
            GeoMath.haversine(LatLon(lat, lon), p) > 2_000.0
        val viejo = ahora - ultimaDeclinacionMs > 10 * 60_000L
        if (!lejos && !viejo) return
        try {
            declinacionDeg = GeomagneticField(
                p.lat.toFloat(), p.lon.toFloat(), 0f, ahora
            ).declination.toDouble()
            ultimaLat = p.lat
            ultimaLon = p.lon
            ultimaDeclinacionMs = ahora
        } catch (_: Exception) {
            // Sin declinación disponible se usa el norte magnético.
        }
    }

    fun iniciar() {
        if (activo) return
        val sensor = sensorRotacion ?: sensorOrientacion ?: return
        activo = true
        suavizador.reiniciar()
        // 100 ms de latencia máxima: suficiente para una flecha fluida sin
        // despertar la UI decenas de veces por segundo.
        sensores.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 100_000)
    }

    fun detener() {
        if (!activo) return
        activo = false
        sensores.unregisterListener(this)
        suavizador.reiniciar()
        ultimoValorDeg = Double.NaN
        _rumbo.value = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!activo) return
        val azimut = when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> azimutDesdeVector(event.values)
            Sensor.TYPE_ORIENTATION -> event.values[0].toDouble()
            else -> return
        }
        if (!azimut.isFinite()) return
        val geografico = normalizar(azimut + declinacionDeg)
        val suave = suavizador.actualizar(geografico, enMovimiento = true) ?: return

        val ahora = SystemClock.elapsedRealtime()
        val cambio = if (ultimoValorDeg.isNaN()) 360.0 else deltaAngular(suave, ultimoValorDeg)
        if (ahora - ultimoEmitidoMs < 80L && cambio < 0.5) return
        ultimoEmitidoMs = ahora
        ultimoValorDeg = suave
        _rumbo.value = suave
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun azimutDesdeVector(valores: FloatArray): Double {
        SensorManager.getRotationMatrixFromVector(matrizRotacion, valores)
        val remapear = when (rotacionPantalla) {
            90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> null
        }
        val matriz = if (remapear == null) {
            matrizRotacion
        } else {
            SensorManager.remapCoordinateSystem(
                matrizRotacion, remapear.first, remapear.second, matrizRemapeada
            )
            matrizRemapeada
        }
        SensorManager.getOrientation(matriz, orientacion)
        return Math.toDegrees(orientacion[0].toDouble())
    }

    private fun normalizar(grados: Double): Double =
        ((grados % 360.0) + 360.0) % 360.0

    /** Diferencia angular mínima (0..180) entre dos rumbos. */
    private fun deltaAngular(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180.0) 360.0 - d else d
    }
}
