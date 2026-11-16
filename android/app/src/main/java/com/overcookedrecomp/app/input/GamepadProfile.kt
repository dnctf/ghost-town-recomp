package com.overcookedrecomp.app.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * Tipe controller gamepad yang didukung secara khusus oleh Ghost Town Recomp.
 */
enum class GamepadType(
    val displayName: String,
    val iconEmoji: String,
    val labelA: String, // Tombol Bawah (Pick Up / Grab)
    val labelB: String, // Tombol Kanan (Dash / Cancel)
    val labelX: String, // Tombol Kiri (Chop / Slap)
    val labelY: String  // Tombol Atas (Throw / Emote)
) {
    DUALSHOCK_4(
        displayName = "Sony DualShock 4 / DualSense (DS4)",
        iconEmoji = "🎮 [PS/DS4]",
        labelA = "✕ (Cross)",
        labelB = "◯ (Circle)",
        labelX = "▢ (Square)",
        labelY = "△ (Triangle)"
    ),
    XBOX(
        displayName = "Xbox Wireless / Elite Controller",
        iconEmoji = "🎮 [Xbox]",
        labelA = "A (Hijau)",
        labelB = "B (Merah)",
        labelX = "X (Biru)",
        labelY = "Y (Kuning)"
    ),
    GENERIC(
        displayName = "Gamepad Standar Android",
        iconEmoji = "🎮 [Gamepad]",
        labelA = "A / ✕",
        labelB = "B / ◯",
        labelX = "X / ▢",
        labelY = "Y / △"
    )
}

/**
 * Data hasil pemetaan input analog dan D-pad dari Gamepad fisik.
 */
data class GamepadAxisState(
    var lx: Float = 0f,
    var ly: Float = 0f,
    var rx: Float = 0f,
    var ry: Float = 0f,
    var hatX: Float = 0f,
    var hatY: Float = 0f,
    var triggerLeft: Float = 0f,
    var triggerRight: Float = 0f
)
