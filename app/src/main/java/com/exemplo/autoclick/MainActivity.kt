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
        val min = campo("Pausa mínima entre toques (ms)", prefs.getLong("pmin", 20))
        val max = campo("Pausa máxima entre toques (ms)", prefs.getLong("pmax", 40))

        val salvar = Button(this).apply {
            text = "Salvar"
            setOnClickListener {
                val a = (min.text.toString().toLongOrNull() ?: 20).coerceAtLeast(10)
                val c = (max.text.toString().toLongOrNull() ?: 40).coerceAtLeast(a)
                prefs.edit().putLong("pmin", a).putLong("pmax", c).apply()
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
                "2) Abra o jogo: aparece a bolinha DB.\n" +
                "3) Arraste o DB até o centro do botão Kick.\n" +
                "4) Segure o dedo no DB para disparar; solte para parar.\n" +
                "Pausa maior = menos toques por segundo e jogo mais fluido."
            setPadding(0, pad, 0, 0)
        }
        listOf(min, max, salvar, acess, ajuda).forEach {
            raiz.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        setContentView(raiz)
    }
}
