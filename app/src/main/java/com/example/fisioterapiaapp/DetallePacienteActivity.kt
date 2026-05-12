package com.example.fisioterapiaapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.fisioterapiaapp.fisio.ChatFisioActivity
import com.example.fisioterapiaapp.fisio.GenerarInformeActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DetallePacienteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var pacienteId     = ""
    private var nombreCompleto = ""
    private var planId         = ""
    private var duracionSemanas = 1
    private var semanaActual   = 1
    private lateinit var ejerciciosPlan: List<Map<String, Any>>

    // Launcher para volver de GenerarInformeActivity y recargar la lista
    private val informeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) cargarInformes()
    }

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

        // Botón generar informe
        findViewById<MaterialButton>(R.id.btnGenerarInforme).setOnClickListener {
            informeLauncher.launch(
                Intent(this, GenerarInformeActivity::class.java).apply {
                    putExtra("pacienteId", pacienteId)
                    putExtra("nombrePaciente", nombreCompleto)
                }
            )
        }

        cargarPlan()
        cargarInformes()

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

    // ── Navegación ─────────────────────────────────────────────────────────────

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

    // ── Cargar plan ────────────────────────────────────────────────────────────

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

                findViewById<MaterialButton>(R.id.btnEditarPlan).visibility = View.VISIBLE
                actualizarCalendario()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar el plan", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Cargar informes ────────────────────────────────────────────────────────

    private fun cargarInformes() {
        val container  = findViewById<LinearLayout>(R.id.containerInformesFisio)
        val tvSin      = findViewById<TextView>(R.id.tvSinInformesFisio)

        db.collection("informes")
            .whereEqualTo("pacienteId", pacienteId)
            .orderBy("fechaGeneracion", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                container.removeAllViews()

                if (docs.isEmpty) {
                    tvSin.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                tvSin.visibility = View.GONE
                val inflater = LayoutInflater.from(this)

                for (doc in docs) {
                    val titulo      = doc.getString("titulo") ?: "Informe"
                    val urlPdf      = doc.getString("urlPdf") ?: ""
                    val comentario  = doc.getString("comentarioFisio") ?: ""
                    val fecha       = doc.getTimestamp("fechaGeneracion")?.toDate()

                    val itemView = inflater.inflate(R.layout.item_informe, container, false)
                    itemView.findViewById<TextView>(R.id.tvTituloInforme).text = titulo
                    itemView.findViewById<TextView>(R.id.tvFechaInforme).text =
                        fecha?.let { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(it) } ?: ""

                    val tvComentario = itemView.findViewById<TextView>(R.id.tvComentarioFisio)
                    if (comentario.isNotBlank()) {
                        tvComentario.visibility = View.VISIBLE
                        tvComentario.text = comentario
                    } else {
                        tvComentario.visibility = View.GONE
                    }

                    val btnVer = itemView.findViewById<MaterialButton>(R.id.btnVerPdf)
                    btnVer.isEnabled = urlPdf.isNotEmpty()
                    btnVer.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlPdf)))
                    }

                    container.addView(itemView)
                }
            }
            .addOnFailureListener {
                tvSin.visibility = View.VISIBLE
                tvSin.text = "Error al cargar informes"
            }
    }

    // ── Calendario ─────────────────────────────────────────────────────────────

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
