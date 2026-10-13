package es.surcogps.app.servicio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import es.surcogps.app.SurcoApp

/**
 * Tras un reinicio del dispositivo, el servicio decide por sí solo si reanuda
 * (tracking activo + permiso) mediante `MotorCampo.restaurarTrasMuerte()`.
 */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendiente = goAsync()
        // Se usa el ámbito de la app a través de un hilo simple: la decisión real
        // vive en el servicio; aquí solo se lo despierta.
        Thread {
            try {
                val app = context.applicationContext as SurcoApp
                // Si no hay permiso, el motor no reanudará (ver Politicas).
                ContextCompat.startForegroundService(
                    app, Intent(app, TrackingService::class.java)
                )
            } catch (_: Exception) {
                // Arranque en segundo plano restringido: no se puede hacer más.
            } finally {
                pendiente.finish()
            }
        }.start()
    }
}
