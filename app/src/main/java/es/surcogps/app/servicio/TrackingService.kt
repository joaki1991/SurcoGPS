package es.surcogps.app.servicio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import es.surcogps.app.MainActivity
import es.surcogps.app.SurcoApp
import es.surcogps.app.motor.Politicas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Seguimiento GPS con pantalla apagada (foreground service tipo `location`).
 * - `START_STICKY`: el sistema lo recrea tras muerte del proceso; al recrear,
 *   el motor decide si reanuda (tracking activo + permiso) o no hace nada.
 * - Acción `DETENER`: para el tracking desde la notificación.
 * - Sin Internet no se pierde nada: todo es local (ficheros + DataStore).
 */
class TrackingService : Service() {

    companion object {
        const val CANAL_ID = "seguimiento"
        const val NOTIF_ID = 41
        const val ACCION_DETENER = "es.surcogps.app.DETENER"
    }

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ultimaNotifMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            motor().detenerGps()
            stopSelf()
            return START_NOT_STICKY
        }
        arrancarPrimerPlano()
        motor().restaurarTrasMuerte()
        observarEstado()
        return START_STICKY
    }

    private fun motor() = (application as SurcoApp).motor

    private fun arrancarPrimerPlano() {
        val notif = notificacion(motor().areaHa.value, null, null)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private var observando = false

    private fun observarEstado() {
        if (observando) return
        observando = true
        alcance.launch {
            combine(motor().areaHa, motor().guiado) { ha, g -> Triple(ha, g.xteM, g.linea) }
                .collect { (ha, xte, linea) ->
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimaNotifMs < 2_000) return@collect
                    ultimaNotifMs = ahora
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, notificacion(ha, xte, linea))
                }
        }
    }

    private fun notificacion(ha: Double, xte: Double?, linea: Int?): Notification {
        val detener = PendingIntent.getService(
            this, 0,
            Intent(this, TrackingService::class.java).setAction(ACCION_DETENER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val abrir = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(Politicas.textoNotificacion(ha, xte, linea))
            .setContentText("Pulsa para abrir SurcoGPS")
            .setContentIntent(abrir)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", detener)
            .setOngoing(true)
            .build()
    }

    private fun crearCanal() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ID, "Seguimiento GPS", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onDestroy() {
        motor().detenerTrackerSolo()
        alcance.cancel()
        super.onDestroy()
    }
}
