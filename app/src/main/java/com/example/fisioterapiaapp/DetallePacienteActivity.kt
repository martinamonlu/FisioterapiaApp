package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.fisioterapiaapp.fisio.ChatFisioActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DetallePacienteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var pacienteId = ""
    private var nombreCompleto = ""
    private var planId = ""
    private var duracionSemanas = 1
    private var semanaActual = 1
    private lateinit var ejerciciosPlan: List<Map<String, Any>>

    private val diasOrden = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_detalle_paciente)

        pacienteId      = intent.getStringExtra("pacienteId") ?: ""
        val nombre      = intent.getStringExtra("nombre") ?: ""
        val apellidos   = intent.getStringExtra("apellidos") ?: ""
        val email       = intent.getStringExtra("email") ?: ""
        val diagnostico = intent.getStringExtra("diagnostico") ?: ""
        nombreCompleto  = "$nombre $apellidos".trim()

        findViewById<TextView>(R.id.tvNombreCompleto).text = nombreCompleto
        findViewById<TextView>(R.id.tvEmail).text = email
        findViewById<TextView>(R.id.tvDiagnostico).text =
            if (diagnostico.isNotEmpty()) "Diagnóstico: $diagnostico" else "Sin diagnóstico registrado"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageButton>(R.id.btnSemanaAnterior).setOnClickListener {
            if (semanaActual > 1) { semanaActual--; actualizarCalendario() }
        }
        findViewById<ImageButton>(R.id.btnSemanaSiguiente).setOnClickListener {
            if (semanaActual < duracionSemanas) { semanaActual++; actualizarCalendario() }
        }

        findViewById<MaterialButton>(R.id.btnVerDetalleSemana).setOnClickListener {
            if (planId.isNotEmpty()) abrirDetalleSemana()
            else Toast.makeText(this, "No hay plan activo para este paciente", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnEditarPlan).setOnClickListener {
            if (planId.isNotEmpty()) abrirEditorPlan()
            else Toast.makeText(this, "No hay plan para editar", Toast.LENGTH_SHORT).show()
        }

        cargarPlan()

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavDetalle)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> true
                R.id.nav_plan -> {
                    if (planId.isNotEmpty()) abrirDetalleSemana()
                    else Toast.makeText(this, "No hay plan activo", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_chat -> {
                    val i = Intent(this, ChatFisioActivity::class.java).apply {
                        putExtra("pacienteId", pacienteId)
                        putExtra("nombrePaciente", nombreCompleto)
                    }
                    startActivity(i)
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_inicio
    }

    private fun abrirDetalleSemana() {
        val i = Intent(this, DetalleSemanaActivity::class.java).apply {
            putExtra("planId", planId)
            putExtra("semana", semanaActual)
            putExtra("duracionSemanas", duracionSemanas)
            putExtra("nombre", nombreCompleto)
            putExtra("pacienteId", pacienteId)
        }
        startActivity(i)
    }

    private fun abrirEditorPlan() {
        val i = Intent(this, CrearPlanActivity::class.java).apply {
            putExtra("planId", planId)
            putExtra("pacienteId", pacienteId)
            putExtra("nombrePaciente", nombreCompleto)
            putExtra("modoEdicion", true)
        }
        startActivity(i)
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

                val fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate() ?: Date()
                val diffMs = Date().time - fechaCreacion.time
                semanaActual = ((diffMs / (1000 * 60 * 60 * 24 * 7)).toInt() + 1).coerceIn(1, duracionSemanas)

                // Mostrar botón editar ahora que hay plan
                findViewById<MaterialButton>(R.id.btnEditarPlan).visibility = View.VISIBLE

                actualizarCalendario()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar el plan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarCalendario() {
        findViewById<TextView>(R.id.tvTituloSemana).text =
            "Plan · Semana $semanaActual/$duracionSemanas"

        db.collection("planes_ejercicio").document(planId).get()
            .addOnSuccessListener { doc ->
                val fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate() ?: Date()
                val sdf = SimpleDateFormat("dd MMM", Locale("es", "ES"))
                val cal = Calendar.getInstance()
                cal.time = fechaCreacion
                cal.add(Calendar.WEEK_OF_YEAR, semanaActual - 1)
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val inicioSemana = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val finSemana = sdf.format(cal.time)
                findViewById<TextView>(R.id.tvRangoSemana).text = "$inicioSemana – $finSemana"
            }

        val diasConEjercicio = mutableSetOf<String>()
        for (ejercicio in ejerciciosPlan) {
            @Suppress("UNCHECKED_CAST")
            val dias = ejercicio["diasSemana"] as? List<String> ?: continue
            diasConEjercicio.addAll(dias)
        }

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
