package dev.igor.remote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.igor.remote.MainActivity
import dev.igor.remote.R
import dev.igor.remote.hid.Consumer
import dev.igor.remote.hid.HidController
import dev.igor.remote.hid.HidState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Mantém o registro HID vivo com o app em segundo plano ou tela apagada. */
class HidService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(HidController.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        HidController.start(this)
        scope.launch {
            HidController.state.collect { state ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> HidController.media(Consumer.PLAY_PAUSE)
            ACTION_DISCONNECT -> HidController.disconnect()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        HidController.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Conexão", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: HidState): Notification {
        val text = when (state) {
            is HidState.Connected -> "Conectado a ${state.device.safeName()}"
            is HidState.Connecting -> "Conectando…"
            HidState.Registered -> "Aguardando conexão"
            HidState.Idle -> "Iniciando…"
            is HidState.Unavailable -> state.reason
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Controle Remoto")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
        if (state is HidState.Connected) {
            builder.addAction(0, "Play/Pause", serviceIntent(ACTION_PLAY_PAUSE))
            builder.addAction(0, "Desconectar", serviceIntent(ACTION_DISCONNECT))
        }
        builder.addAction(0, "Encerrar", serviceIntent(ACTION_STOP))
        return builder.build()
    }

    private fun serviceIntent(action: String) = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, HidService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL_ID = "hid"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PLAY_PAUSE = "play_pause"
        private const val ACTION_DISCONNECT = "disconnect"
        private const val ACTION_STOP = "stop"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, HidService::class.java))
        }
    }
}

@android.annotation.SuppressLint("MissingPermission")
fun android.bluetooth.BluetoothDevice.safeName(): String = name ?: address
