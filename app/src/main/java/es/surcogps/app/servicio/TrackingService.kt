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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Seguimiento GPS con pantalla apagada (foreground service tipo `location`).
 *
 * La app v2 no reanuda sesiones ni arranca sola tras un reinicio: si el usuario
 * cierra la app, el seguimiento termina. Mientras está vivo, la notificación
 * muestra precisión, velocidad, hectáreas y estado de la pintura.
 */
class TrackingService : Service() {

    companion object {
        const val CANAL_ID = "seguimiento"
        const val NOTIF_ID = 41
        const val ACCION_DETENER = "es.surcogps.app.DETENER"
    }

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ultimaNotifMs = 0L
    private var observando = false

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
        observarEstado()
        return START_NOT_STICKY
    }

    private fun motor() = (application as SurcoApp).motor

    private fun arrancarPrimerPlano() {
        val notif = notificacion(0.0, null, null, true)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun observarEstado() {
        if (observando) return
        observando = true
        alcance.launch {
            combine(
                motor().areaHa,
                motor().guiado,
                motor().registrando
            ) { ha, g, pintando -> Triple(ha, g, pintando) }
                .collect { (ha, g, pintando) ->
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimaNotifMs < 2_000) return@collect
                    ultimaNotifMs = ahora
                    val fix = g.fix
                    val notif = notificacion(
                        ha,
                        fix?.accuracyM?.takeIf { it.isFinite() }?.roundToInt(),
                        fix?.speedMps?.let { (it * 3.6f).roundToInt() },
                        pintando
                    )
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, notif)
                }
        }
    }

    private fun notificacion(
        ha: Double,
        precisionM: Int?,
        velocidadKmh: Int?,
        pintando: Boolean
    ): Notification {
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
        val estado = buildString {
            append(if (pintando) "Pintando" else "Pintura en pausa")
            append(" · ").append("%.2f".format(ha)).append(" ha")
            if (precisionM != null) append(" · ±").append(precisionM).append(" m")
            if (velocidadKmh != null) append(" · ").append(velocidadKmh).append(" km/h")
        }
        return NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("SurcoGPS")
            .setContentText(estado)
            .setContentIntent(abrir)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Detener", detener
            )
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
