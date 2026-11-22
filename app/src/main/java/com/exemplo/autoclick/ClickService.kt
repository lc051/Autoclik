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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlin.math.abs
import kotlin.random.Random

class ClickService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var botao: TextView
    private lateinit var lp: WindowManager.LayoutParams
    private var rodando = false
    private var pendente = false
    private var pausaMin = 20L
    private var pausaMax = 40L
    private var dens = 1f
    private var ok = 0
    private var falhas = 0
    private var kickX = 79
    private var kickY = 31
    private var powX = 76
    private var powY = 68

    private val watchdog = Runnable {
        if (pendente && rodando) {
            pendente = false
            falhas++
            mostrar()
            tocar()
        }
    }

    private fun borda(cor: Int) {
        (botao.background as GradientDrawable).setStroke((3 * dens).toInt(), cor)
    }

    private fun mostrar() {
        if (ok + falhas == 0) {
            botao.text = "DB"
            return
        }
        val s = SpannableString("DB\n✓$ok ✗$falhas")
        s.setSpan(RelativeSizeSpan(0.45f), 2, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        botao.text = s
    }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        dens = resources.displayMetrics.density
        val tam = (72 * dens).toInt()

        botao = TextView(this).apply {
            text = "DB"
            textSize = 24f
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
            x = (resources.displayMetrics.widthPixels * 0.4f).toInt()
            y = (resources.displayMetrics.heightPixels * 0.5f).toInt()
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
            kickX = p.getInt("kx", 79)
            kickY = p.getInt("ky", 31)
            powX = p.getInt("px", 76)
            powY = p.getInt("py", 68)
            ok = 0
            falhas = 0
            pendente = false
            rodando = true
            borda(Color.RED)
            tocar()
        }
    }

    private fun parar() {
        rodando = false
        pendente = false
        handler.removeCallbacksAndMessages(null)
        if (::botao.isInitialized) borda(Color.BLACK)
    }

    // Kick e, poucos ms depois, Power, num único gesto
    @Suppress("DEPRECATION")
    private fun tocar() {
        if (!rodando) return
        val m = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(m)
        val w = m.widthPixels.toFloat()
        val h = m.heightPixels.toFloat()
        val atraso = Random.nextLong(20, 71)

        val b = GestureDescription.Builder()
        val pontos = listOf(Triple(kickX, kickY, 0L), Triple(powX, powY, atraso))
        for ((fx, fy, ini) in pontos) {
            val x = (w * fx / 100f + Random.nextDouble(-12.0, 12.0).toFloat() * dens).coerceIn(1f, w - 1f)
            val y = (h * fy / 100f + Random.nextDouble(-12.0, 12.0).toFloat() * dens).coerceIn(1f, h - 1f)
            b.addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply { moveTo(x, y) }, ini, Random.nextLong(40, 71)
                )
            )
        }
        val cb = object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) {
                if (!pendente) return
                handler.removeCallbacks(watchdog)
                pendente = false
                ok++
                mostrar()
                proximo()
            }
            override fun onCancelled(g: GestureDescription?) {
                if (!pendente) return
                handler.removeCallbacks(watchdog)
                pendente = false
                falhas++
                mostrar()
                proximo()
            }
        }
        pendente = true
        handler.postDelayed(watchdog, 600)
        if (!dispatchGesture(b.build(), cb, null)) {
            handler.removeCallbacks(watchdog)
            pendente = false
            falhas++
            mostrar()
            proximo()
        }
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
