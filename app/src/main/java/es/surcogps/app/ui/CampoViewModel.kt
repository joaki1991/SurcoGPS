package es.surcogps.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.surcogps.app.SurcoApp
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.data.Ajustes
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.mapa.DestinoCamara
import es.surcogps.app.mapa.EstadoUbicacion
import es.surcogps.app.mapa.ModoMapa
import es.surcogps.app.mapa.SuavizadorRumbo
import es.surcogps.core.GuidanceEngine
import es.surcogps.core.Parcela
import es.surcogps.core.SessionSummary
import es.surcogps.core.WorkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Fachada de UI sobre el [es.surcogps.app.motor.MotorCampo] (ámbito de app).
 * Solo conserva estado puramente visual (modo de mapa, seguir).
 */
class CampoViewModel(app: Application) : AndroidViewModel(app) {

    private val motor = (app as SurcoApp).motor

    val ajustes: StateFlow<Ajustes> = motor.ajustes
    val nmeaSentencias: StateFlow<Int> = motor.nmeaSentencias
    val guiado: StateFlow<EstadoGuiado> = motor.guiado
    val areaHa: StateFlow<Double> = motor.areaHa
    val tramosDibujo: StateFlow<List<TramoFranja>> = motor.tramosDibujo
    val registrando: StateFlow<Boolean> = motor.registrando
    val parcelas: StateFlow<List<Parcela>> = motor.parcelas
    val sesiones: StateFlow<List<SessionSummary>> = motor.sesiones
    val sesionActivaId: StateFlow<String?> = motor.sesionActivaId
    val sesionActivaNombre: StateFlow<String?> = motor.sesionActivaNombre
    val destinoCamara: StateFlow<DestinoCamara?> = motor.destinoCamara
    val guiaActual: StateFlow<GuidanceEngine?> = motor.guiaActual

    // Suavizado del rumbo de marcha para la flecha (con estado entre fixes).
    private val suavizador = SuavizadorRumbo()

    /** Indicador de posición para el mapa (null = sin fix). */
    val ubicacion: StateFlow<EstadoUbicacion?> = motor.guiado.map { g ->
        val fix = g.fix ?: return@map null
        if (!g.aceptado) return@map null
        val enMarcha = (fix.speedMps ?: 0f) > UMBRAL_MARCHA_MS
        EstadoUbicacion(
            lat = fix.position.lat,
            lon = fix.position.lon,
            precisionM = fix.accuracyM,
            rumboDeg = suavizador.actualizar(g.rumboVehiculoDeg, enMarcha)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Estado puramente visual (no pertenece al motor).
    private val _modoMapa = MutableStateFlow(ModoMapa.HIBRIDO)
    val modoMapa: StateFlow<ModoMapa> = _modoMapa.asStateFlow()

    private val _seguirPosicion = MutableStateFlow(false)
    val seguirPosicion: StateFlow<Boolean> = _seguirPosicion.asStateFlow()

    fun tienePermisoUbicacion(): Boolean = motor.tienePermisoUbicacion()
    fun iniciarGps() = motor.iniciarGps()
    fun detenerGps() = motor.detenerGps()
    fun cambiarModoMapa(modo: ModoMapa) {
        _modoMapa.value = modo
    }
    fun alternarSeguir() {
        _seguirPosicion.value = !_seguirPosicion.value
    }
    /** El usuario movió el mapa a mano: se deja de seguir (sin pelear la cámara). */
    fun alGestoManual() {
        _seguirPosicion.value = false
    }
    /** Botón «Mi ubicación»: centra en la posición actual con zoom de detalle. */
    fun irAUbicacion() {
        val p = motor.guiado.value.posicionApero
            ?: motor.guiado.value.fix?.position ?: return
        motor.solicitarDestino(p, es.surcogps.app.mapa.PoliticaEncuadre.ZOOM_DETALLE)
        _seguirPosicion.value = true
    }
    fun consumirDestino() {
        val d = destinoCamara.value ?: return
        motor.consumirDestino(d.id)
    }
    fun alternarRegistro() = motor.alternarRegistro()
    fun fijarAnclaAqui() = motor.fijarAnclaAqui()
    fun fijarAnclaEn(p: es.surcogps.core.LatLon) = motor.fijarAnclaEn(p)
    fun guardarRumbo(r: Double) = motor.guardarRumbo(r)
    fun guardarAncho(a: Double) = motor.guardarAncho(a)
    fun guardarShift(s: Double) = motor.guardarShift(s)
    fun guardarOffset(lat: Double, lon: Double) = motor.guardarOffset(lat, lon)
    fun guardarPuerta(p: Float) = motor.guardarPuerta(p)
    fun crearParcela(nombre: String): Boolean = motor.crearParcela(nombre)
    fun borrarParcela(id: String) = motor.borrarParcela(id)
    fun usarParcela(p: Parcela) = motor.usarParcela(p)
    fun fotoSesionActiva(): WorkSession? = motor.fotoSesionActiva()
    fun nuevaSesion(nombre: String? = null): Boolean = motor.nuevaSesion(nombre)
    fun reanudarSesion(id: String) = motor.reanudarSesion(id)
    fun guardarSesionActiva(terminada: Boolean = false) = motor.guardarSesionActiva(terminada)
    fun terminarSesion() = motor.terminarSesion()
    fun borrarSesion(id: String) = motor.borrarSesion(id)

    companion object {
        /** Velocidad mínima para considerar marcha fiable (m/s). */
        const val UMBRAL_MARCHA_MS = 0.8f
    }

    override fun onCleared() {
        // El tracking lo posee el motor+servicio: aquí solo se persiste la foto.
        guardarSesionActiva()
        super.onCleared()
    }
}
