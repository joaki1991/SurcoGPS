package es.surcogps.app

import android.app.Application
import es.surcogps.app.motor.MotorCampo
import es.surcogps.app.soporte.CrashReporter

/** Aplicación: alberga el [MotorCampo] para que el tracking sobreviva a la UI. */
class SurcoApp : Application() {
    val motor: MotorCampo by lazy { MotorCampo(this) }

    override fun onCreate() {
        super.onCreate()
        // Informa cierres inesperados en fichero local (para compartir),
        // encadenando al gestor previo del sistema (cierra igual, pero con rastro).
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        val dir = filesDir
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            try {
                CrashReporter.guardarCierre(dir, hilo, error)
            } catch (_: Exception) {
                // No hay nada más que hacer: se delega al sistema.
            }
            anterior?.uncaughtException(hilo, error)
        }
    }
}
