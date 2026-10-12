package dev.igor.remote.hid

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import dev.igor.remote.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

sealed interface HidState {
    data class Unavailable(val reason: String) : HidState
    data object Idle : HidState
    data object Registered : HidState
    data class Connecting(val device: BluetoothDevice) : HidState
    data class Connected(val device: BluetoothDevice) : HidState
}

/**
 * Mantém o registro do app como periférico HID Bluetooth e envia os reports.
 * Todas as chamadas de envio são serializadas numa única thread para preservar a ordem.
 */
@SuppressLint("MissingPermission")
object HidController {
    private const val TAG = "HidController"
    private const val PREFS = "hid"
    private const val PREF_LAST_DEVICE = "last_device"
    // Um pouco acima do intervalo típico do link HID (11,25 ms): enviar mais rápido que o
    // link aceita enche a fila do Bluetooth e o atraso cresce sem parar.
    private const val FLUSH_INTERVAL_MS = 14L

    private val _state = MutableStateFlow<HidState>(HidState.Idle)
    val state: StateFlow<HidState> = _state.asStateFlow()

    /** Modificadores "travados" pela UI; aplicados à próxima tecla e então liberados. */
    private val _stickyModifiers = MutableStateFlow(Modifier.NONE)
    val stickyModifiers: StateFlow<Int> = _stickyModifiers.asStateFlow()

    private val io = Executors.newSingleThreadScheduledExecutor()
    private var flushTask: ScheduledFuture<*>? = null

    private lateinit var appContext: Context
    private var adapter: BluetoothAdapter? = null
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null

    // Movimento acumulado entre flushes (acesso só na thread `io`).
    private var pendingX = 0f
    private var pendingY = 0f
    private var pendingWheel = 0f
    private var pendingPan = 0f
    private var buttons = 0

    private val sdp = BluetoothHidDeviceAppSdpSettings(
        "Controle Remoto",
        "Teclado, mouse e mídia",
        "dev.igor.remote",
        BluetoothHidDevice.SUBCLASS1_COMBO,
        HidDescriptor.DESCRIPTOR,
    )

    // Pede ao host latência de 11,25 ms em vez do padrão "best effort" sem limite.
    private val qos = BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED,
        800,
        9,
        0,
        11250,
        BluetoothHidDeviceAppQosSettings.MAX,
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged registered=$registered plugged=$pluggedDevice")
            if (!registered) {
                host = null
                _state.value = HidState.Idle
                return
            }
            if (_state.value !is HidState.Connected) _state.value = HidState.Registered
            val target = pluggedDevice ?: lastDevice()
            if (target != null && _state.value !is HidState.Connected) connect(target)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "onConnectionStateChanged $device state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    host = device
                    saveLastDevice(device)
                    _state.value = HidState.Connected(device)
                }
                BluetoothProfile.STATE_CONNECTING -> _state.value = HidState.Connecting(device)
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (host == device || _state.value is HidState.Connecting) {
                        host = null
                        _state.value = if (hid != null) HidState.Registered else HidState.Idle
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            val size = when (id.toInt()) {
                HidDescriptor.ID_KEYBOARD -> 8
                HidDescriptor.ID_MOUSE -> 5
                HidDescriptor.ID_CONSUMER -> 2
                else -> 0
            }
            if (size == 0) {
                hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            } else {
                hid?.replyReport(device, type, id, ByteArray(size))
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            // LEDs do teclado (Caps Lock etc.) — apenas confirma.
            hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val device = proxy as BluetoothHidDevice
            hid = device
            val ok = device.registerApp(sdp, null, qos, io, callback)
            if (!ok) _state.value = HidState.Unavailable("Falha ao registrar o app HID.")
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = null
            host = null
            _state.value = HidState.Idle
        }
    }

    /** Inicia o perfil HID. Exige BLUETOOTH_CONNECT concedida (API 31+). */
    fun start(context: Context) {
        appContext = context.applicationContext
        if (hid != null) return
        val manager = appContext.getSystemService(BluetoothManager::class.java)
        val bt = manager?.adapter
        adapter = bt
        _state.value = HidState.Idle
        when {
            bt == null -> _state.value = HidState.Unavailable("Este aparelho não tem Bluetooth.")
            !bt.isEnabled -> _state.value = HidState.Unavailable("Ative o Bluetooth e tente novamente.")
            !bt.getProfileProxy(appContext, profileListener, BluetoothProfile.HID_DEVICE) ->
                _state.value = HidState.Unavailable(
                    "Este aparelho não suporta o perfil Bluetooth HID Device.",
                )
        }
        if (flushTask == null) {
            flushTask = io.scheduleWithFixedDelay(::flushMouse, 0, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        flushTask?.cancel(false)
        flushTask = null
        hid?.let {
            host?.let { h -> it.disconnect(h) }
            it.unregisterApp()
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        hid = null
        host = null
        _state.value = HidState.Idle
    }

    fun bondedDevices(): List<BluetoothDevice> =
        adapter?.bondedDevices?.sortedBy { it.name ?: it.address }.orEmpty()

    fun connect(device: BluetoothDevice) {
        val proxy = hid ?: return
        _state.value = HidState.Connecting(device)
        if (!proxy.connect(device)) {
            _state.value = HidState.Registered
        }
    }

    fun disconnect() {
        val h = host ?: return
        hid?.disconnect(h)
    }

    // ---------------- Teclado ----------------

    fun toggleSticky(modifier: Int) {
        _stickyModifiers.value = _stickyModifiers.value xor modifier
    }

    fun type(c: Char) {
        KeyMapper.map(c)?.let { pressKey(it.code, it.modifiers) }
    }

    fun pressKey(code: Int, modifiers: Int = Modifier.NONE) {
        val mods = modifiers or _stickyModifiers.value
        _stickyModifiers.value = Modifier.NONE
        io.execute {
            sendKeyboard(mods, code)
            sendKeyboard(Modifier.NONE, 0)
        }
    }

    private fun sendKeyboard(mods: Int, code: Int) {
        send(HidDescriptor.ID_KEYBOARD, byteArrayOf(mods.toByte(), 0, code.toByte(), 0, 0, 0, 0, 0))
    }

    // ---------------- Mouse ----------------

    /** Acumula movimento; enviado em lotes a cada [FLUSH_INTERVAL_MS]. */
    fun move(dx: Float, dy: Float) {
        traceLatency("touch", dx, dy)
        io.execute {
            pendingX += dx
            pendingY += dy
        }
    }

    fun scroll(vertical: Float, horizontal: Float) = io.execute {
        pendingWheel += vertical
        pendingPan += horizontal
    }

    fun setButton(button: Int, pressed: Boolean) = io.execute {
        buttons = if (pressed) buttons or button else buttons and button.inv()
        sendMouse(0, 0, 0, 0)
    }

    fun click(button: Int) = io.execute {
        buttons = buttons or button
        sendMouse(0, 0, 0, 0)
        buttons = buttons and button.inv()
        sendMouse(0, 0, 0, 0)
    }

    /** Envia no máximo um report por ciclo; o que exceder ±127 fica para o próximo. */
    private fun flushMouse() {
        val dx = MouseMath.clamp(pendingX.toInt())
        val dy = MouseMath.clamp(pendingY.toInt())
        val wheel = MouseMath.clamp(pendingWheel.toInt())
        val pan = MouseMath.clamp(pendingPan.toInt())
        if (dx == 0 && dy == 0 && wheel == 0 && pan == 0) return
        pendingX -= dx
        pendingY -= dy
        pendingWheel -= wheel
        pendingPan -= pan
        sendMouse(dx, dy, wheel, pan)
    }

    private fun sendMouse(dx: Int, dy: Int, wheel: Int, pan: Int) {
        val start = System.nanoTime()
        send(
            HidDescriptor.ID_MOUSE,
            byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), wheel.toByte(), pan.toByte()),
        )
        traceLatency("send", dx.toFloat(), dy.toFloat(), (System.nanoTime() - start) / 1000)
    }

    // ---------------- Mídia ----------------

    fun media(usage: Int) = io.execute {
        send(HidDescriptor.ID_CONSUMER, byteArrayOf((usage and 0xFF).toByte(), (usage shr 8).toByte()))
        send(HidDescriptor.ID_CONSUMER, byteArrayOf(0, 0))
    }

    // ---------------- Interno ----------------

    private fun send(id: Int, data: ByteArray) {
        val proxy = hid ?: return
        val h = host ?: return
        if (!proxy.sendReport(h, id, data)) Log.w(TAG, "sendReport falhou (id=$id)")
    }

    private val epochOffsetUs = System.currentTimeMillis() * 1000 - System.nanoTime() / 1000

    /** Log de diagnóstico de latência (só no build de debug): horário de parede em µs. */
    private fun traceLatency(kind: String, dx: Float, dy: Float, callUs: Long = 0) {
        if (!BuildConfig.DEBUG) return
        val nowUs = System.nanoTime() / 1000 + epochOffsetUs
        Log.d("HidLatency", "$kind $nowUs $dx $dy $callUs")
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun saveLastDevice(device: BluetoothDevice) {
        prefs().edit().putString(PREF_LAST_DEVICE, device.address).apply()
    }

    private fun lastDevice(): BluetoothDevice? {
        val address = prefs().getString(PREF_LAST_DEVICE, null) ?: return null
        return adapter?.bondedDevices?.firstOrNull { it.address == address }
    }
}
