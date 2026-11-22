package com.exemplo.autoclick

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlin.math.abs
import kotlin.random.Random

class ClickService : AccessibilityService() {

    private class Alvo(
        val cor: Int,
        val mira: View,
        val miraLp: WindowManager.LayoutParams,
        val botao: TextView,
        val botaoLp: WindowManager.LayoutParams
    ) { var ativo = false }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private val alvos = mutableListOf<Alvo>()
    private var rodando = false
    private var minMs = 20L
    private var maxMs = 26L

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        // K = Kick (vermelho), P = Power (azul)
        criar("K", Color.RED, 300, 700, 40, 300)
        criar("P", Color.rgb(30, 90, 220), 600, 700, 40, 430)
    }

    private fun criar(letra: String, cor: Int, mx: Int, my: Int, bx: Int, by: Int) {
        val d = resources.displayMetrics.density
        val tm = (48 * d).toInt()
        val tb = (52 * d).toInt()

        val mira = TextView(this).apply {
            text = letra; textSize = 16f; setTextColor(cor); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(50, Color.red(cor), Color.green(cor), Color.blue(cor)))
                setStroke((3 * d).toInt(), cor)
            }
        }
        val miraLp = params(tm, tm, mx, my)
        val botao = TextView(this).apply {
            text = "▶ $letra"; textSize = 16f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(220, 60, 60, 60))
            }
        }
        val botaoLp = params(tb, tb, bx, by)

        val a = Alvo(cor, mira, miraLp, botao, botaoLp)
        arrastavel(mira, miraLp, null)
        arrastavel(botao, botaoLp) { alternar(a, letra) }
        wm.addView(mira, miraLp)
        wm.addView(botao, botaoLp)
        alvos.add(a)
    }

    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    private fun arrastavel(v: View, lp: WindowManager.LayoutParams, aoClicar: (() -> Unit)?) {
        var dx = 0f; var dy = 0f; var moveu = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { dx = e.rawX - lp.x; dy = e.rawY - lp.y; moveu = false }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(e.rawX - dx - lp.x) > 8 || abs(e.rawY - dy - lp.y) > 8) moveu = true
                    if (moveu) {
                        lp.x = (e.rawX - dx).toInt(); lp.y = (e.rawY - dy).toInt()
                        wm.updateViewLayout(v, lp)
                    }
                }
                MotionEvent.ACTION_UP -> if (!moveu) aoClicar?.invoke()
            }
            true
        }
    }

    private fun alternar(a: Alvo, letra: String) {
        a.ativo = !a.ativo
        val bg = a.botao.background as GradientDrawable
        if (a.ativo) {
            a.botao.text = "■ $letra"
            bg.setColor(Color.argb(220, Color.red(a.cor), Color.green(a.cor), Color.blue(a.cor)))
            a.miraLp.flags = a.miraLp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            a.botao.text = "▶ $letra"
            bg.setColor(Color.argb(220, 60, 60, 60))
            a.miraLp.flags = a.miraLp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        }
        wm.updateViewLayout(a.mira, a.miraLp)

        if (alvos.any { it.ativo } && !rodando) {
            val p = getSharedPreferences("cfg", MODE_PRIVATE)
            minMs = p.getLong("min", 20).coerceAtLeast(20)
            maxMs = p.getLong("max", 26).coerceAtLeast(minMs)
            rodando = true
            tocar()
        } else if (alvos.none { it.ativo }) {
            rodando = false
            handler.removeCallbacksAndMessages(null)
        }
    }

    private var duracao = 6L

    // Um único gesto com um toque por alvo ativo: Kick e Power podem tocar ao mesmo tempo
    private fun tocar() {
        val ativos = alvos.filter { it.ativo }
        if (ativos.isEmpty()) { rodando = false; return }
        duracao = Random.nextLong(5, 9)
        val b = GestureDescription.Builder()
        for (a in ativos) {
            val loc = IntArray(2)
            a.mira.getLocationOnScreen(loc)
            val x = loc[0] + a.mira.width / 2f + Random.nextInt(-3, 4)
            val y = loc[1] + a.mira.height / 2f + Random.nextInt(-3, 4)
            b.addStroke(GestureDescription.StrokeDescription(Path().apply { moveTo(x, y) }, 0, duracao))
        }
        val cb = object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = proximo()
            override fun onCancelled(g: GestureDescription?) = proximo()
        }
        if (!dispatchGesture(b.build(), cb, null)) proximo()
    }

    private fun proximo() {
        if (!rodando) return
        // Intervalo total entre toques ≈ valor configurado (descontando a duração do toque)
        val espera = (Random.nextLong(minMs, maxMs + 1) - duracao).coerceAtLeast(5)
        handler.postDelayed({ tocar() }, espera)
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}

    override fun onInterrupt() {
        rodando = false
        handler.removeCallbacksAndMessages(null)
        alvos.filter { it.ativo }.forEach { alternar(it, if (it === alvos[0]) "K" else "P") }
    }

    override fun onDestroy() {
        rodando = false
        handler.removeCallbacksAndMessages(null)
        runCatching { alvos.forEach { wm.removeView(it.mira); wm.removeView(it.botao) } }
        super.onDestroy()
    }
}
