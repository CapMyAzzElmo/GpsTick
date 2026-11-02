package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.SpeedPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("ViewConstructor")
class FloatingJoyStickView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onCloseClicked: () -> Unit
) : FrameLayout(context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var updatesJob: Job? = null

    private var isExpanded = true

    // UI elements
    private val expandedContainer: LinearLayout
    private val collapsedContainer: FrameLayout
    private val joystickTouchView: JoystickTouchPadView
    private val coordinatesTextView: TextView
    private val speedTextView: TextView
    private val lockWalkButton: TextView
    private val pausePlayButton: TextView

    // Drag tracking for moving the overlay window
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    init {
        // Root container styling
        setBackgroundColor(Color.TRANSPARENT)

        // 1. Expanded view
        expandedContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EE0B0F19"))
            setPadding(dp(12), dp(8), dp(12), dp(10))
            elevation = dp(8).toFloat()

            // Header bar
            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val titleView = TextView(context).apply {
                text = "GPS JOYSTICK"
                setTextColor(Color.parseColor("#06B6D4"))
                textSize = 13f
                paint.isFakeBoldText = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val minimizeBtn = TextView(context).apply {
                text = "—"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 18f
                paint.isFakeBoldText = true
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { toggleExpanded(false) }
            }

            val appBtn = TextView(context).apply {
                text = "APP"
                setTextColor(Color.parseColor("#6366F1"))
                textSize = 11f
                paint.isFakeBoldText = true
                setPadding(dp(6), dp(4), dp(6), dp(4))
                setOnClickListener {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(intent)
                }
            }

            val closeBtn = TextView(context).apply {
                text = "✕"
                setTextColor(Color.parseColor("#F43F5E"))
                textSize = 16f
                paint.isFakeBoldText = true
                setPadding(dp(8), dp(4), dp(4), dp(4))
                setOnClickListener { onCloseClicked() }
            }

            header.addView(titleView)
            header.addView(appBtn)
            header.addView(minimizeBtn)
            header.addView(closeBtn)

            // Make entire header draggable
            setupDragListener(header)

            // Coordinates HUD
            coordinatesTextView = TextView(context).apply {
                text = "37.8087, -122.4098"
                setTextColor(Color.WHITE)
                textSize = 11f
                setPadding(0, dp(4), 0, 0)
            }

            speedTextView = TextView(context).apply {
                text = "Speed: 4.5 km/h"
                setTextColor(Color.parseColor("#22D3EE"))
                textSize = 11f
                paint.isFakeBoldText = true
                setPadding(0, 0, 0, dp(6))
            }

            // Joystick Touch Pad
            joystickTouchView = JoystickTouchPadView(context) { dx, dy ->
                JoyStickController.updateJoystick(dx, dy)
            }
            val joyParams = LinearLayout.LayoutParams(dp(180), dp(180)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            joystickTouchView.layoutParams = joyParams

            // Speed selector pills row
            val speedRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(6), 0, dp(4))
            }

            val walkBtn = createSpeedButton("WALK", SpeedPreset.WALK)
            val jogBtn = createSpeedButton("JOG", SpeedPreset.JOG)
            val driveBtn = createSpeedButton("DRIVE", SpeedPreset.DRIVE)

            speedRow.addView(walkBtn)
            speedRow.addView(jogBtn)
            speedRow.addView(driveBtn)

            // Quick actions row: Lock Walk & Pause/Play
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            }

            lockWalkButton = TextView(context).apply {
                text = "LOCK WALK"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 10f
                paint.isFakeBoldText = true
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(dp(10), dp(6), dp(10), dp(6))
                setOnClickListener {
                    JoyStickController.toggleLockWalk()
                }
            }

            pausePlayButton = TextView(context).apply {
                text = "PAUSE"
                setTextColor(Color.parseColor("#F59E0B"))
                textSize = 10f
                paint.isFakeBoldText = true
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(dp(10), dp(6), dp(10), dp(6))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(8), 0, 0, 0)
                }
                layoutParams = lp
                setOnClickListener {
                    JoyStickController.togglePause()
                }
            }

            actionRow.addView(lockWalkButton)
            actionRow.addView(pausePlayButton)

            addView(header)
            addView(coordinatesTextView)
            addView(speedTextView)
            addView(joystickTouchView)
            addView(speedRow)
            addView(actionRow)
        }

        // 2. Collapsed mini-bubble view
        collapsedContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#EE0B0F19"))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            elevation = dp(8).toFloat()
            visibility = View.GONE

            val miniText = TextView(context).apply {
                text = "🕹️ GPS"
                setTextColor(Color.parseColor("#06B6D4"))
                textSize = 12f
                paint.isFakeBoldText = true
            }
            addView(miniText)

            setOnClickListener {
                toggleExpanded(true)
            }
            setupDragListener(this)
        }

        addView(expandedContainer)
        addView(collapsedContainer)

        listenToStateUpdates()
    }

    private fun createSpeedButton(label: String, preset: SpeedPreset): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 10f
            paint.isFakeBoldText = true
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            layoutParams = lp
            setOnClickListener {
                JoyStickController.setSpeedPreset(preset)
            }
        }
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        if (expand) {
            expandedContainer.visibility = View.VISIBLE
            collapsedContainer.visibility = View.GONE
        } else {
            expandedContainer.visibility = View.GONE
            collapsedContainer.visibility = View.VISIBLE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(targetView: View) {
        targetView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(this@FloatingJoyStickView, layoutParams)
                    } catch (e: Exception) {
                        // View may be unattached
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun listenToStateUpdates() {
        updatesJob?.cancel()
        updatesJob = scope.launch {
            launch {
                JoyStickController.locationState.collectLatest { state ->
                    coordinatesTextView.text = "%.4f, %.4f".format(state.latitude, state.longitude)
                    pausePlayButton.text = if (state.isPaused) "RESUME" else "PAUSE"
                    pausePlayButton.setTextColor(
                        if (state.isPaused) Color.parseColor("#10B981") else Color.parseColor("#F59E0B")
                    )
                }
            }
            launch {
                JoyStickController.speedConfig.collectLatest { config ->
                    speedTextView.text = "Speed: ${config.displaySpeed()}"
                    lockWalkButton.text = if (config.lockWalk) "AUTO-WALK: ON" else "AUTO-WALK: OFF"
                    lockWalkButton.setTextColor(
                        if (config.lockWalk) Color.parseColor("#10B981") else Color.parseColor("#94A3B8")
                    )
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        updatesJob?.cancel()
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}

/**
 * Custom canvas view rendering an arcade style 2D analog thumbstick
 */
@SuppressLint("ClickableViewAccessibility")
class JoystickTouchPadView(
    context: Context,
    private val onDeflectionChanged: (dx: Float, dy: Float) -> Unit
) : View(context) {

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#334155")
    }

    private val outerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44111827")
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#22334155")
    }

    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#06B6D4")
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#22D3EE")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f

    private var knobX = 0f
    private var knobY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = (w.coerceAtMost(h) / 2f) - 16f
        knobRadius = baseRadius * 0.35f
        knobX = centerX
        knobY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw outer ring and background
        canvas.drawCircle(centerX, centerY, baseRadius, outerFillPaint)
        canvas.drawCircle(centerX, centerY, baseRadius, outerPaint)

        // Draw crosshairs
        canvas.drawLine(centerX - baseRadius, centerY, centerX + baseRadius, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - baseRadius, centerX, centerY + baseRadius, crosshairPaint)

        // Compass labels
        canvas.drawText("N", centerX, centerY - baseRadius + 32f, textPaint)
        canvas.drawText("S", centerX, centerY + baseRadius - 12f, textPaint)
        canvas.drawText("W", centerX - baseRadius + 24f, centerY + 8f, textPaint)
        canvas.drawText("E", centerX + baseRadius - 24f, centerY + 8f, textPaint)

        // Draw thumbstick knob
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                val maxDist = baseRadius - knobRadius
                if (dist <= maxDist) {
                    knobX = event.x
                    knobY = event.y
                } else {
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    knobX = centerX + (maxDist * cos(angle)).toFloat()
                    knobY = centerY + (maxDist * sin(angle)).toFloat()
                }

                val normalizedDx = ((knobX - centerX) / maxDist).coerceIn(-1f, 1f)
                val normalizedDy = ((knobY - centerY) / maxDist).coerceIn(-1f, 1f)
                onDeflectionChanged(normalizedDx, normalizedDy)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                knobX = centerX
                knobY = centerY
                onDeflectionChanged(0f, 0f)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
