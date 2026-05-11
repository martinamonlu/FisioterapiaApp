package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DetallePacienteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var pacienteId = ""
    private var planId = ""
    private var duracionSemanas = 1
    private var semanaActual = 1
    private lateinit var ejerciciosPlan: List<Map<String, Any>>

    private val diasOrden = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_paciente)

        // Recibir datos del paciente
        pacienteId      = intent.getStringExtra("pacienteId") ?: ""
        val nombre      = intent.getStringExtra("nombre") ?: ""
        val apellidos   = intent.getStringExtra("apellidos") ?: ""
        val email       = intent.getStringExtra("email") ?: ""
        val diagnostico = intent.getStringExtra("diagnostico") ?: ""

        // Rellenar cabecera
        findViewById<TextView>(R.id.tvNombreCompleto).text = "$nombre $apellidos"
        findViewById<TextView>(R.id.tvEmail).text = email
        findViewById<TextView>(R.id.tvDiagnostico).text =
            if (diagnostico.isNotEmpty()) "Diagnóstico: $diagnostico" else "Sin diagnóstico registrado"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Navegación semanas
        findViewById<ImageButton>(R.id.btnSemanaAnterior).setOnClickListener {
            if (semanaActual > 1) {
                semanaActual--
                actualizarCalendario()
            }
        }

        findViewById<ImageButton>(R.id.btnSemanaSiguiente).setOnClickListener {
            if (semanaActual < duracionSemanas) {
                semanaActual++
                actualizarCalendario()
            }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVerDetalleSemana)
            .setOnClickListener {
                if (planId.isNotEmpty()) {
                    val intent = Intent(this, DetalleSemanaActivity::class.java).apply {
                        putExtra("planId", planId)
                        putExtra("semana", semanaActual)
                        putExtra("duracionSemanas", duracionSemanas)
                        putExtra("nombre", "$nombre $apellidos")
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No hay plan activo para este paciente", Toast.LENGTH_SHORT).show()
                }
            }

        cargarPlan()
    }

    private fun cargarPlan() {
        db.collection("planes_ejercicio")
            .whereEqualTo("pacienteId", pacienteId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    findViewById<TextView>(R.id.tvTituloSemana).text = "Sin plan activo"
                    return@addOnSuccessListener
                }

                val doc = docs.documents[0]
                planId = doc.id
                duracionSemanas = (doc.getLong("duracionSemanas") ?: 1).toInt()

                @Suppress("UNCHECKED_CAST")
                ejerciciosPlan = (doc.get("ejercicios") as? List<Map<String, Any>>) ?: emptyList()

                // Calcular semana actual basándose en fechaCreacion
                val fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate() ?: Date()
                val hoy = Date()
                val diffMs = hoy.time - fechaCreacion.time
                val diffSemanas = (diffMs / (1000 * 60 * 60 * 24 * 7)).toInt() + 1
                semanaActual = diffSemanas.coerceIn(1, duracionSemanas)

                actualizarCalendario()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar el plan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarCalendario() {
        findViewById<TextView>(R.id.tvTituloSemana).text =
            "Plan · Semana $semanaActual/$duracionSemanas"

        // Rango de fechas de la semana
        val sdf = SimpleDateFormat("dd MMM", Locale("es", "ES"))
        val cal = Calendar.getInstance()
        // Semana 1 = semana de creación del plan
        // calculamos la fecha de inicio de esa semana
        db.collection("planes_ejercicio").document(planId).get()
            .addOnSuccessListener { doc ->
                val fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate() ?: Date()
                cal.time = fechaCreacion
                cal.add(Calendar.WEEK_OF_YEAR, semanaActual - 1)
                // Ir al lunes de esa semana
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val inicioSemana = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val finSemana = sdf.format(cal.time)
                findViewById<TextView>(R.id.tvRangoSemana).text = "$inicioSemana – $finSemana"
            }

        // Recoger todos los días que tienen ejercicio
        val diasConEjercicio = mutableSetOf<String>()
        for (ejercicio in ejerciciosPlan) {
            @Suppress("UNCHECKED_CAST")
            val dias = ejercicio["diasSemana"] as? List<String> ?: continue
            diasConEjercicio.addAll(dias)
        }

        // Actualizar dots
        val dots = mapOf(
            "Lunes"     to R.id.dotLunes,
            "Martes"    to R.id.dotMartes,
            "Miércoles" to R.id.dotMiercoles,
            "Jueves"    to R.id.dotJueves,
            "Viernes"   to R.id.dotViernes,
            "Sábado"    to R.id.dotSabado,
            "Domingo"   to R.id.dotDomingo
        )

        for ((dia, viewId) in dots) {
            val dot = findViewById<View>(viewId)
            dot.setBackgroundResource(
                if (dia in diasConEjercicio) R.drawable.circle_dot_active
                else R.drawable.circle_dot_inactive
            )
        }
    }
}