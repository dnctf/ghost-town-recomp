package com.overcookedrecomp.app.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Manajer gamepad terpusat untuk mendeteksi profil Sony DualShock 4 (DS4)
 * dan Xbox Wireless/Elite Controller, serta menormalkan pembacaan axis dan trigger.
 */
class GamepadManager(private val context: Context) : InputManager.InputDeviceListener {

    companion object {
        private const val TAG = "GamepadManager"
        private const val SONY_VENDOR_ID = 0x054c
        private const val MICROSOFT_VENDOR_ID = 0x045e
        private const val DEADZONE = 0.15f
        private const val TRIGGER_THRESHOLD = 0.4f
    }

    interface InputCallback {
        fun onAxisEvent(deviceId: Int, lx: Float, ly: Float, rx: Float, ry: Float, hatX: Float, hatY: Float)
        fun onButtonEvent(deviceId: Int, keyCode: Int, pressed: Boolean)
    }

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    private val deviceProfileMap = ConcurrentHashMap<Int, GamepadType>()
    private val triggerLeftPressed = ConcurrentHashMap<Int, Boolean>()
    private val triggerRightPressed = ConcurrentHashMap<Int, Boolean>()

    var inputCallback: InputCallback? = null

    init {
        inputManager?.registerInputDeviceListener(this, null)
        refreshConnectedControllers()
    }

    /**
     * Memperbarui daftar controller yang sedang terhubung ke perangkat.
     */
    fun refreshConnectedControllers() {
        val deviceIds = InputDevice.getDeviceIds() ?: return
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                identifyAndRegisterDevice(device, showToast = false)
            }
        }
    }

    /**
     * Identifikasi apakah controller adalah Sony DualShock 4 atau Xbox.
     */
    private fun identifyAndRegisterDevice(device: InputDevice, showToast: Boolean): GamepadType {
        val vendorId = device.vendorId
        val name = device.name.lowercase()

        val type = when {
            vendorId == SONY_VENDOR_ID ||
            name.contains("dualshock") ||
            name.contains("dualsense") ||
            name.contains("sony") ||
            name.contains("playstation") -> GamepadType.DUALSHOCK_4

            vendorId == MICROSOFT_VENDOR_ID ||
            name.contains("xbox") ||
            name.contains("microsoft") ||
            name.contains("x-box") -> GamepadType.XBOX

            name.contains("wireless controller") -> {
                // Pada Android standar, nama default Bluetooth DS4 seringkali hanya "Wireless Controller"
                GamepadType.DUALSHOCK_4
            }

            else -> GamepadType.GENERIC
        }

        deviceProfileMap[device.id] = type
        Log.i(TAG, "Controller terdeteksi: ${device.name} [ID: ${device.id}, VID: 0x${Integer.toHexString(vendorId)}] -> Profil: ${type.displayName}")

        if (showToast) {
            Toast.makeText(
                context,
                "${type.iconEmoji} Controller Terhubung:\n${device.name}\n(Profil ${type.displayName} Diaktifkan)",
                Toast.LENGTH_SHORT
            ).show()
        }

        return type
    }

    fun getProfile(deviceId: Int): GamepadType {
        return deviceProfileMap[deviceId] ?: GamepadType.GENERIC
    }

    /**
     * Memproses MotionEvent dari joystick dan trigger analog.
     */
    fun processMotionEvent(event: MotionEvent): Boolean {
        val deviceId = event.deviceId
        val profile = deviceProfileMap[deviceId] ?: identifyAndRegisterDevice(event.device, showToast = false)

        var lx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_X))
        var ly = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Y))
        var rx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_Z))
        var ry = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RZ))

        // Fallback untuk stick kanan pada beberapa driver DS4/Generic
        if (rx == 0f && ry == 0f) {
            rx = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RX))
            ry = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_RY))
        }

        val hatX = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_X))
        val hatY = applyDeadzone(event.getAxisValue(MotionEvent.AXIS_HAT_Y))

        // Pembacaan trigger L2 (LT) dan R2 (RT)
        val lTrigger = when (profile) {
            GamepadType.DUALSHOCK_4 -> {
                val brake = event.getAxisValue(MotionEvent.AXIS_BRAKE)
                if (brake > 0.05f) brake else event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            }
            GamepadType.XBOX -> {
                val lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
                if (lt > 0.05f) lt else event.getAxisValue(MotionEvent.AXIS_BRAKE)
            }
            GamepadType.GENERIC -> {
                maxOf(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE))
            }
        }

        val rTrigger = when (profile) {
            GamepadType.DUALSHOCK_4 -> {
                val gas = event.getAxisValue(MotionEvent.AXIS_GAS)
                if (gas > 0.05f) gas else event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            }
            GamepadType.XBOX -> {
                val rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
                if (rt > 0.05f) rt else event.getAxisValue(MotionEvent.AXIS_GAS)
            }
            GamepadType.GENERIC -> {
                maxOf(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS))
            }
        }

        // Sintesis penekanan tombol L2 & R2 jika trigger analog ditekan melewati batas ambang
        handleTriggerThreshold(deviceId, KeyEvent.KEYCODE_BUTTON_L2, lTrigger, triggerLeftPressed)
        handleTriggerThreshold(deviceId, KeyEvent.KEYCODE_BUTTON_R2, rTrigger, triggerRightPressed)

        // Teruskan data axis ke Native Engine
        inputCallback?.onAxisEvent(deviceId, lx, ly, rx, ry, hatX, hatY)
        return true
    }

    private fun handleTriggerThreshold(deviceId: Int, keyCode: Int, triggerVal: Float, stateMap: ConcurrentHashMap<Int, Boolean>) {
        val wasPressed = stateMap[deviceId] ?: false
        val isPressed = triggerVal >= TRIGGER_THRESHOLD

        if (wasPressed != isPressed) {
            stateMap[deviceId] = isPressed
            inputCallback?.onButtonEvent(deviceId, keyCode, isPressed)
        }
    }

    /**
     * Memproses KeyEvent dari tombol gamepad fisik atau D-pad digital.
     */
    fun processKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN

        // Normalisasi tombol D-Pad digital menjadi axis atau teruskan key event
        inputCallback?.onButtonEvent(event.deviceId, keyCode, isDown)
        return true
    }

    private fun applyDeadzone(value: Float): Float {
        return if (abs(value) < DEADZONE) 0f else value
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        identifyAndRegisterDevice(device, showToast = true)
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        val removed = deviceProfileMap.remove(deviceId)
        triggerLeftPressed.remove(deviceId)
        triggerRightPressed.remove(deviceId)
        if (removed != null) {
            Log.i(TAG, "Controller terputus [ID: $deviceId]")
        }
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        identifyAndRegisterDevice(device, showToast = false)
    }

    fun unregister() {
        inputManager?.unregisterInputDeviceListener(this)
    }
}
