package com.exemplo.autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ClickService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var botao: TextView
    private lateinit var lp: WindowManager.LayoutParams
    private var rodando = false
    private var pausaMin = 20L
    private var pausaMax = 40L
    private var dens = 1f

    private fun borda(cor: Int) {
        (botao.background as GradientDrawable).setStroke((3 * dens).toInt(), cor)
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        dens = resources.displayMetrics.density
        val tam = (56 * dens).toInt()

        botao = TextView(this).apply {
            text = "DB"
            textSize = 20f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke((3 * dens).toInt(), Color.BLACK)
            }
        }
        lp = WindowManager.LayoutParams(
            tam, tam,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels * 0.7f).toInt()
            y = (resources.displayMetrics.heightPixels * 0.3f).toInt()
        }

        var dx = 0f; var dy = 0f; var x0 = 0f; var y0 = 0f
        var arrastando = false
        botao.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dx = e.rawX - lp.x; dy = e.rawY - lp.y
                    x0 = e.rawX; y0 = e.rawY
                    arrastando = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!arrastando && (abs(e.rawX - x0) > 12 * dens || abs(e.rawY - y0) > 12 * dens)) {
                        arrastando = true
                    }
                    if (arrastando) {
                        lp.x = (e.rawX - dx).toInt()
                        lp.y = (e.rawY - dy).toInt()
                        wm.updateViewLayout(botao, lp)
                    }
                }
                MotionEvent.ACTION_UP -> if (!arrastando) alternar()
            }
            true
        }
        wm.addView(botao, lp)
    }

    private fun alternar() {
        if (rodando) {
            parar()
        } else {
            val p = getSharedPreferences("cfg", MODE_PRIVATE)
            pausaMin = p.getLong("pmin", 20).coerceAtLeast(10)
            pausaMax = p.getLong("pmax", 40).coerceAtLeast(pausaMin)
            rodando = true
            borda(Color.RED)
            tocar()
        }
    }

    private fun parar() {
        rodando = false
        handler.removeCallbacksAndMessages(null)
        if (::botao.isInitialized) borda(Color.BLACK)
    }

    // Toca em volta do DB (fora da bolinha), em posição diferente a cada vez
    private fun tocar() {
        if (!rodando) return
        val loc = IntArray(2)
        botao.getLocationOnScreen(loc)
        val cx = loc[0] + botao.width / 2f
        val cy = loc[1] + botao.height / 2f
        val ang = Random.nextDouble(0.0, 2 * PI)
        val raio = Random.nextDouble(34.0, 60.0) * dens
        val x = (cx + cos(ang) * raio).toFloat()
        val y = (cy + sin(ang) * raio).toFloat()
        val dur = Random.nextLong(25, 46)

        val gesto = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(Path().apply { moveTo(x, y) }, 0, dur))
            .build()
        val cb = object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = proximo()
            override fun onCancelled(g: GestureDescription?) = proximo()
        }
        if (!dispatchGesture(gesto, cb, null)) proximo()
    }

    private fun proximo() {
        if (!rodando) return
        handler.postDelayed({ tocar() }, Random.nextLong(pausaMin, pausaMax + 1))
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() = parar()

    override fun onDestroy() {
        parar()
        runCatching { wm.removeView(botao) }
        super.onDestroy()
    }
}
