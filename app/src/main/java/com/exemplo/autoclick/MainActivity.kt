package com.exemplo.autoclick

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun campo(dica: String, valor: Long) = EditText(this).apply {
            hint = dica
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(valor.toString())
        }
        val min = campo("Intervalo mínimo (ms)", prefs.getLong("min", 20))
        val max = campo("Intervalo máximo (ms)", prefs.getLong("max", 26))

        val salvar = Button(this).apply {
            text = "Salvar intervalo"
            setOnClickListener {
                val a = (min.text.toString().toLongOrNull() ?: 20).coerceAtLeast(20)
                val c = (max.text.toString().toLongOrNull() ?: 26).coerceAtLeast(a)
                prefs.edit().putLong("min", a).putLong("max", c).apply()
                min.setText(a.toString()); max.setText(c.toString())
                Toast.makeText(this@MainActivity, "Salvo: $a–$c ms", Toast.LENGTH_SHORT).show()
            }
        }
        val acess = Button(this).apply {
            text = "Ativar serviço de acessibilidade"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val ajuda = TextView(this).apply {
            text = "1) Ative \"AutoClick\" em Acessibilidade.\n" +
                "2) Abra o jogo: aparecem 2 miras (K = Kick, P = Power) e 2 botões ▶.\n" +
                "3) Arraste cada mira para cima do botão correspondente no jogo.\n" +
                "4) Toque em ▶ K ou ▶ P para ligar cada um (ou os dois juntos); ■ para parar.\n" +
                "Intervalo padrão: 20–26 ms (mínimo 20)."
            setPadding(0, pad, 0, 0)
        }
        listOf(min, max, salvar, acess, ajuda).forEach {
            raiz.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        setContentView(raiz)
    }
}
