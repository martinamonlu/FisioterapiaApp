package com.example.fisioterapiaapp.paciente.util

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// ── View extensions ──────────────────────────────────────────
fun View.visible() { visibility = View.VISIBLE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.gone() { visibility = View.GONE }

fun View.snack(mensaje: String, duracion: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, mensaje, duracion).show()
}

fun Context.toast(mensaje: String) {
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
}

// ── Timestamp / Date ─────────────────────────────────────────
fun Timestamp.toFormattedDate(pattern: String = "dd/MM/yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale("es", "ES"))
    return sdf.format(toDate())
}

fun Date.toTimestamp(): Timestamp = Timestamp(this)

fun today(): Timestamp = Timestamp(Date())

// ── Días de la semana ────────────────────────────────────────
val DIAS_SEMANA = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

fun diaDeHoy(): String {
    val cal = Calendar.getInstance(Locale("es", "ES"))
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    return when (dayOfWeek) {
        Calendar.MONDAY    -> "Lunes"
        Calendar.TUESDAY   -> "Martes"
        Calendar.WEDNESDAY -> "Miércoles"
        Calendar.THURSDAY  -> "Jueves"
        Calendar.FRIDAY    -> "Viernes"
        Calendar.SATURDAY  -> "Sábado"
        else               -> "Domingo"
    }
}

// ── Validaciones ─────────────────────────────────────────────
fun String.isValidPassword(): Boolean =
    length >= 8 && any { it.isUpperCase() } && any { it.isDigit() }

fun calcularAdherencia(sesionesCompletadas: Int, sesionesTotales: Int): Float {
    if (sesionesTotales == 0) return 0f
    return (sesionesCompletadas.toFloat() / sesionesTotales.toFloat()) * 100f
}
