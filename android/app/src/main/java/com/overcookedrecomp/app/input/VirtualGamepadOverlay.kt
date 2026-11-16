package com.overcookedrecomp.app.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Overlay On-Screen Touch Controls (Virtual Buttons & Analog Joystick)
 * untuk memainkan game Overcooked 1, Overcooked 2, Moving Out 1 & 2 secara langsung
 * di layar sentuh Android (misal: tablet MatePad atau HP) tanpa controller fisik.
 */
class VirtualGamepadOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface VirtualInputListener {
        fun onVirtualAxis(lx: Float, ly: Float)
        fun onVirtualButton(keyCode: Int, pressed: Boolean)
        fun onToggleMenuRequested()
    }

    var inputListener: VirtualInputListener? = null
    var overlayAlpha: Float = 0.85f
        set(value) {
            field = value.coerceIn(0.1f, 1.0f)
            invalidate()
        }

    // Identifikasi pointer untuk multi-touch
    private var joystickPointerId = -1
    private val buttonPointerMap = mutableMapOf<Int, Int>() // Pointer ID -> Button KeyCode

    // Dimensi Joystick Kiri
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private var joyKnobX = 0f
    private var joyKnobY = 0f
    private var joyBaseRadius = 0f
    private var joyKnobRadius = 0f

    // Tombol Aksi Kanan (Diamond)
    private var btnRadius = 0f
    private var btnAx = 0f; private var btnAy = 0f // Tombol A / Cross (Bawah)
    private var btnBx = 0f; private var btnBy = 0f // Tombol B / Circle (Kanan)
    private var btnXx = 0f; private var btnXy = 0f // Tombol X / Square (Kiri)
    private var btnYx = 0f; private var btnYy = 0f // Tombol Y / Triangle (Atas)

    // Tombol Shoulder (L1, R1, L2, R2)
    private val rectL1 = RectF()
    private val rectR1 = RectF()
    private val rectL2 = RectF()
    private val rectR2 = RectF()
    private val rectMenu = RectF()

    // Status Tombol (Ditekan atau tidak)
    private val pressedButtons = mutableSetOf<Int>()

    // Paints
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isClickable = true
        isFocusable = false

        basePaint.style = Paint.Style.FILL
        knobPaint.style = Paint.Style.FILL

        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 3f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true

        subTextPaint.textAlign = Paint.Align.CENTER
        subTextPaint.textSize = 22f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupControls(w.toFloat(), h.toFloat())
    }

    private fun setupControls(width: Float, height: Float) {
        val density = resources.displayMetrics.density

        // Ukuran Responsif
        joyBaseRadius = min(width * 0.16f, 110f * density)
        joyKnobRadius = joyBaseRadius * 0.42f
        joyCenterX = joyBaseRadius + 24f * density
        joyCenterY = height - joyBaseRadius - 28f * density
        joyKnobX = joyCenterX
        joyKnobY = joyCenterY

        // Tombol Aksi Diamond (Kanan Bawah)
        btnRadius = min(width * 0.055f, 36f * density)
        val diamondCenterX = width - (joyBaseRadius + 24f * density)
        val diamondCenterY = height - joyBaseRadius - 28f * density
        val offset = btnRadius * 1.55f

        btnAx = diamondCenterX; btnAy = diamondCenterY + offset           // A (Bawah)
        btnBx = diamondCenterX + offset; btnBy = diamondCenterY           // B (Kanan)
        btnXx = diamondCenterX - offset; btnXy = diamondCenterY           // X (Kiri)
        btnYx = diamondCenterX; btnYy = diamondCenterY - offset           // Y (Atas)

        // Tombol Shoulder L1 / L2 (Kiri Atas)
        val shoulderW = 90f * density
        val shoulderH = 40f * density
        val margin = 20f * density

        rectL1.set(margin, margin + 40f * density, margin + shoulderW, margin + 40f * density + shoulderH)
        rectL2.set(margin, margin + 40f * density + shoulderH + 12f * density, margin + shoulderW, margin + 40f * density + shoulderH * 2 + 12f * density)

        // Tombol Shoulder R1 / R2 (Kanan Atas)
        rectR1.set(width - margin - shoulderW, margin + 40f * density, width - margin, margin + 40f * density + shoulderH)
        rectR2.set(width - margin - shoulderW, margin + 40f * density + shoulderH + 12f * density, width - margin, margin + 40f * density + shoulderH * 2 + 12f * density)

        // Tombol Menu / Pause (Tengah Atas)
        val menuW = 100f * density
        val menuH = 34f * density
        rectMenu.set(width / 2f - menuW / 2f, margin, width / 2f + menuW / 2f, margin + menuH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val globalAlpha = (overlayAlpha * 255).toInt()

        // 1. Gambar Base Joystick
        basePaint.color = Color.argb((globalAlpha * 0.35f).toInt(), 20, 25, 35)
        canvas.drawCircle(joyCenterX, joyCenterY, joyBaseRadius, basePaint)

        strokePaint.color = Color.argb((globalAlpha * 0.6f).toInt(), 0, 168, 232)
        canvas.drawCircle(joyCenterX, joyCenterY, joyBaseRadius, strokePaint)

        // Crosshair halus pada base joystick
        canvas.drawLine(joyCenterX - 20, joyCenterY, joyCenterX + 20, joyCenterY, strokePaint)
        canvas.drawLine(joyCenterX, joyCenterY - 20, joyCenterX, joyCenterY + 20, strokePaint)

        // 2. Gambar Knob Joystick
        val knobAlpha = if (joystickPointerId != -1) (globalAlpha * 0.9f).toInt() else (globalAlpha * 0.6f).toInt()
        knobPaint.color = Color.argb(knobAlpha, 0, 200, 255)
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, knobPaint)

        strokePaint.color = Color.argb(globalAlpha, 255, 255, 255)
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, strokePaint)

        // 3. Gambar Tombol Diamond Kanan (A, B, X, Y)
        drawActionButton(canvas, btnAx, btnAy, btnRadius, "A", "✕", Color.parseColor("#2EC4B6"), KeyEvent.KEYCODE_BUTTON_A, globalAlpha)
        drawActionButton(canvas, btnBx, btnBy, btnRadius, "B", "◯", Color.parseColor("#E71D36"), KeyEvent.KEYCODE_BUTTON_B, globalAlpha)
        drawActionButton(canvas, btnXx, btnXy, btnRadius, "X", "▢", Color.parseColor("#00A8E8"), KeyEvent.KEYCODE_BUTTON_X, globalAlpha)
        drawActionButton(canvas, btnYx, btnYy, btnRadius, "Y", "△", Color.parseColor("#FF9F1C"), KeyEvent.KEYCODE_BUTTON_Y, globalAlpha)

        // 4. Gambar Shoulder Buttons (L1, R1, L2, R2)
        drawShoulderButton(canvas, rectL1, "L1 (Switch)", KeyEvent.KEYCODE_BUTTON_L1, globalAlpha)
        drawShoulderButton(canvas, rectL2, "L2 (Grab)", KeyEvent.KEYCODE_BUTTON_L2, globalAlpha)
        drawShoulderButton(canvas, rectR1, "R1 (Emote)", KeyEvent.KEYCODE_BUTTON_R1, globalAlpha)
        drawShoulderButton(canvas, rectR2, "R2 (Throw)", KeyEvent.KEYCODE_BUTTON_R2, globalAlpha)

        // 5. Gambar Tombol Menu / Setup
        drawMenuButton(canvas, rectMenu, "⚙ MENU", globalAlpha)
    }

    private fun drawActionButton(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        primaryText: String,
        subText: String,
        colorHex: Int,
        keyCode: Int,
        globalAlpha: Int
    ) {
        val isPressed = pressedButtons.contains(keyCode)
        val fillAlpha = if (isPressed) min(255, (globalAlpha * 0.95f).toInt()) else (globalAlpha * 0.45f).toInt()

        buttonPaint.color = Color.argb(
            fillAlpha,
            Color.red(colorHex),
            Color.green(colorHex),
            Color.blue(colorHex)
        )
        canvas.drawCircle(cx, cy, r, buttonPaint)

        strokePaint.color = if (isPressed) Color.WHITE else Color.argb((globalAlpha * 0.8f).toInt(), Color.red(colorHex), Color.green(colorHex), Color.blue(colorHex))
        strokePaint.strokeWidth = if (isPressed) 5f else 3f
        canvas.drawCircle(cx, cy, r, strokePaint)

        // Label teks
        textPaint.color = Color.argb(globalAlpha, 255, 255, 255)
        textPaint.textSize = r * 0.65f
        canvas.drawText(primaryText, cx, cy - 2f, textPaint)

        subTextPaint.color = Color.argb((globalAlpha * 0.75f).toInt(), 220, 220, 220)
        subTextPaint.textSize = r * 0.4f
        canvas.drawText(subText, cx, cy + r * 0.55f, subTextPaint)
    }

    private fun drawShoulderButton(canvas: Canvas, rect: RectF, text: String, keyCode: Int, globalAlpha: Int) {
        val isPressed = pressedButtons.contains(keyCode)
        val fillAlpha = if (isPressed) (globalAlpha * 0.85f).toInt() else (globalAlpha * 0.35f).toInt()

        buttonPaint.color = Color.argb(fillAlpha, 40, 50, 70)
        canvas.drawRoundRect(rect, 16f, 16f, buttonPaint)

        strokePaint.color = if (isPressed) Color.parseColor("#00E5FF") else Color.argb((globalAlpha * 0.6f).toInt(), 120, 140, 180)
        strokePaint.strokeWidth = if (isPressed) 4f else 2f
        canvas.drawRoundRect(rect, 16f, 16f, strokePaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 26f
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }

    private fun drawMenuButton(canvas: Canvas, rect: RectF, text: String, globalAlpha: Int) {
        buttonPaint.color = Color.argb((globalAlpha * 0.4f).toInt(), 30, 30, 40)
        canvas.drawRoundRect(rect, 18f, 18f, buttonPaint)

        strokePaint.color = Color.argb((globalAlpha * 0.7f).toInt(), 255, 160, 0)
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 18f, 18f, strokePaint)

        textPaint.color = Color.parseColor("#FFD166")
        textPaint.textSize = 24f
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(actionIndex)
                val y = event.getY(actionIndex)
                handleTouchDown(pointerId, x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (pId == joystickPointerId) {
                        updateJoystickKnob(x, y)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                handleTouchUp(pointerId)
            }

            MotionEvent.ACTION_CANCEL -> {
                resetAllInputs()
            }
        }

        invalidate()
        return true
    }

    private fun handleTouchDown(pointerId: Int, x: Float, y: Float) {
        // 1. Cek Sentuhan Joystick (Area Kiri Layar Bawah)
        if (x < width * 0.45f && y > height * 0.4f && joystickPointerId == -1) {
            joystickPointerId = pointerId
            updateJoystickKnob(x, y)
            return
        }

        // 2. Cek Tombol Menu
        if (rectMenu.contains(x, y)) {
            triggerHaptic()
            inputListener?.onToggleMenuRequested()
            return
        }

        // 3. Cek Tombol Aksi Diamond
        val hitButton = checkActionButtonHit(x, y)
        if (hitButton != -1) {
            buttonPointerMap[pointerId] = hitButton
            pressButton(hitButton)
            return
        }

        // 4. Cek Tombol Shoulder L1/L2/R1/R2
        val hitShoulder = checkShoulderHit(x, y)
        if (hitShoulder != -1) {
            buttonPointerMap[pointerId] = hitShoulder
            pressButton(hitShoulder)
            return
        }
    }

    private fun handleTouchUp(pointerId: Int) {
        if (pointerId == joystickPointerId) {
            joystickPointerId = -1
            joyKnobX = joyCenterX
            joyKnobY = joyCenterY
            inputListener?.onVirtualAxis(0f, 0f)
        }

        val keyCode = buttonPointerMap.remove(pointerId)
        if (keyCode != null) {
            releaseButton(keyCode)
        }
    }

    private fun updateJoystickKnob(touchX: Float, touchY: Float) {
        val dx = touchX - joyCenterX
        val dy = touchY - joyCenterY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist <= joyBaseRadius) {
            joyKnobX = touchX
            joyKnobY = touchY
        } else {
            val angle = atan2(dy, dx)
            joyKnobX = joyCenterX + cos(angle) * joyBaseRadius
            joyKnobY = joyCenterY + sin(angle) * joyBaseRadius
        }

        // Normalisasi Axis (-1.0 s/d 1.0)
        val normX = ((joyKnobX - joyCenterX) / joyBaseRadius).coerceIn(-1f, 1f)
        val normY = ((joyKnobY - joyCenterY) / joyBaseRadius).coerceIn(-1f, 1f)

        inputListener?.onVirtualAxis(normX, normY)
    }

    private fun checkActionButtonHit(x: Float, y: Float): Int {
        val hitDist = btnRadius * 1.35f
        if (distance(x, y, btnAx, btnAy) <= hitDist) return KeyEvent.KEYCODE_BUTTON_A
        if (distance(x, y, btnBx, btnBy) <= hitDist) return KeyEvent.KEYCODE_BUTTON_B
        if (distance(x, y, btnXx, btnXy) <= hitDist) return KeyEvent.KEYCODE_BUTTON_X
        if (distance(x, y, btnYx, btnYy) <= hitDist) return KeyEvent.KEYCODE_BUTTON_Y
        return -1
    }

    private fun checkShoulderHit(x: Float, y: Float): Int {
        if (rectL1.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_L1
        if (rectL2.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_L2
        if (rectR1.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_R1
        if (rectR2.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_R2
        return -1
    }

    private fun pressButton(keyCode: Int) {
        if (pressedButtons.add(keyCode)) {
            triggerHaptic()
            inputListener?.onVirtualButton(keyCode, true)
        }
    }

    private fun releaseButton(keyCode: Int) {
        if (pressedButtons.remove(keyCode)) {
            inputListener?.onVirtualButton(keyCode, false)
        }
    }

    private fun resetAllInputs() {
        joystickPointerId = -1
        joyKnobX = joyCenterX
        joyKnobY = joyCenterY
        inputListener?.onVirtualAxis(0f, 0f)

        for (btn in pressedButtons) {
            inputListener?.onVirtualButton(btn, false)
        }
        pressedButtons.clear()
        buttonPointerMap.clear()
    }

    private fun triggerHaptic() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
