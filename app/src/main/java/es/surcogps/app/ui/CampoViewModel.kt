package es.surcogps.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import es.surcogps.app.SurcoApp
import es.surcogps.app.cobertura.TramoFranja
import es.surcogps.app.data.Ajustes
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.mapa.ModoMapa
import es.surcogps.core.Parcela
import es.surcogps.core.SessionSummary
import es.surcogps.core.WorkSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    fun alternarRegistro() = motor.alternarRegistro()
    fun fijarAnclaAqui() = motor.fijarAnclaAqui()
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

    override fun onCleared() {
        // El tracking lo posee el motor+servicio: aquí solo se persiste la foto.
        guardarSesionActiva()
        super.onCleared()
    }
}
