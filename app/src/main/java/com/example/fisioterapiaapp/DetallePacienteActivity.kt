package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.fisioterapiaapp.fisio.ChatFisioActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DetallePacienteActivity : AppCompatActivity() {

    private val TAG = "DetallePaciente"
    private val db = FirebaseFirestore.getInstance()

    private var pacienteId     = ""
    private var nombreCompleto = ""
    private var planId         = ""
    private var duracionSemanas = 1
    private var semanaActual   = 1
    private lateinit var ejerciciosPlan: List<Map<String, Any>>

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

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📋 DetallePacienteActivity iniciado")
        Log.d(TAG, "👤 Paciente ID: $pacienteId")
        Log.d(TAG, "👤 Nombre: $nombreCompleto")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        findViewById<TextView>(R.id.tvNombreCompleto).text = nombreCompleto
        findViewById<TextView>(R.id.tvEmail).text = email
        findViewById<TextView>(R.id.tvDiagnostico).text =
            if (diagnostico.isNotEmpty()) "Diagnóstico: $diagnostico"
            else "Sin diagnóstico registrado"

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
        cargarMonitorizacion()

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
                    startActivity(Intent(this, ChatFisioActivity::class.java).apply {
                        putExtra("pacienteId", pacienteId)
                        putExtra("nombrePaciente", nombreCompleto)
                    })
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_inicio
    }

    private fun abrirDetalleSemana() {
        startActivity(Intent(this, DetalleSemanaActivity::class.java).apply {
            putExtra("planId", planId)
            putExtra("semana", semanaActual)
            putExtra("duracionSemanas", duracionSemanas)
            putExtra("nombre", nombreCompleto)
            putExtra("pacienteId", pacienteId)
        })
    }

    private fun abrirEditorPlan() {
        startActivity(Intent(this, CrearPlanActivity::class.java).apply {
            putExtra("planId", planId)
            putExtra("pacienteId", pacienteId)
            putExtra("nombrePaciente", nombreCompleto)
            putExtra("modoEdicion", true)
        })
    }

    private fun cargarPlan() {
        Log.d(TAG, "🔍 Cargando plan para paciente: $pacienteId")

        db.collection("planes_ejercicio")
            .whereEqualTo("pacienteId", pacienteId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { docs ->
                Log.d(TAG, "✅ Planes encontrados: ${docs.size()}")

                if (docs.isEmpty) {
                    Log.w(TAG, "⚠️ No hay plan activo para este paciente")
                    findViewById<TextView>(R.id.tvTituloSemana).text = "Sin plan activo"
                    return@addOnSuccessListener
                }
                val doc = docs.documents[0]
                planId = doc.id
                duracionSemanas = (doc.getLong("duracionSemanas") ?: 1).toInt()

                Log.d(TAG, "📋 Plan ID: $planId")
                Log.d(TAG, "📋 Duración: $duracionSemanas semanas")

                @Suppress("UNCHECKED_CAST")
                ejerciciosPlan = (doc.get("ejercicios") as? List<Map<String, Any>>) ?: emptyList()

                val fechaCreacion = doc.getTimestamp("fechaCreacion")?.toDate() ?: Date()
                val diffMs = Date().time - fechaCreacion.time
                semanaActual = ((diffMs / (1000 * 60 * 60 * 24 * 7)).toInt() + 1).coerceIn(1, duracionSemanas)

                findViewById<MaterialButton>(R.id.btnEditarPlan).visibility = View.VISIBLE
                actualizarCalendario()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ ERROR al cargar plan: ${e.message}", e)
                Toast.makeText(this, "Error al cargar el plan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarMonitorizacion() {
        Log.d(TAG, "")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "🔍 INICIANDO CARGA DE MONITORIZACIÓN")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "👤 Paciente ID para query: $pacienteId")

        db.collection("registros_sesion")
            .whereEqualTo("pacienteId", pacienteId)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                val registros = snap.documents
                Log.d(TAG, "✅ Query exitosa - Registros encontrados: ${registros.size}")

                if (registros.isEmpty()) {
                    Log.w(TAG, "⚠️ No hay registros de sesión para este paciente")
                    Log.w(TAG, "⚠️ Los cards de monitorización permanecerán ocultos")
                    return@addOnSuccessListener
                }

                registros.forEachIndexed { i, doc ->
                    val eva = doc.getLong("eva")
                    val notas = doc.getString("notas")
                    Log.d(TAG, "  📊 Registro ${i+1}: EVA=$eva, Notas=${notas?.take(20) ?: "sin notas"}")
                }

                Log.d(TAG, "")
                Log.d(TAG, "🚦 Llamando a mostrarSemaforo()...")
                mostrarSemaforo(registros)

                Log.d(TAG, "📈 Llamando a mostrarGrafica()...")
                mostrarGrafica(registros)

                Log.d(TAG, "📝 Llamando a mostrarNotas()...")
                mostrarNotas(registros)

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ MONITORIZACIÓN COMPLETADA")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ ERROR AL CARGAR REGISTROS")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "❌ Tipo de error: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Mensaje: ${e.message}")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.e(TAG, "Stack trace:", e)

                Toast.makeText(
                    this,
                    "Error al cargar monitorización: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarSemaforo(registros: List<com.google.firebase.firestore.DocumentSnapshot>) {
        Log.d(TAG, "  🚦 Dentro de mostrarSemaforo(), registros: ${registros.size}")

        val ultimaEva = registros.firstNotNullOfOrNull { it.getLong("eva")?.toInt() }

        if (ultimaEva == null) {
            Log.w(TAG, "  ⚠️ No se encontró EVA en ningún registro")
            return
        }

        Log.d(TAG, "  📊 EVA encontrada: $ultimaEva")

        val (colorResId, textoResId) = when {
            ultimaEva >= 7 -> Pair(R.color.eva_max, "Dolor elevado — revisión recomendada")
            ultimaEva >= 4 -> Pair(R.color.color_pendiente, "Dolor moderado — seguimiento activo")
            else           -> Pair(R.color.eva_none, "Estado estable")
        }

        val color = ContextCompat.getColor(this, colorResId)
        Log.d(TAG, "  🎨 Color seleccionado, Texto: $textoResId")

        val cardEstado   = findViewById<CardView>(R.id.cardEstado)
        val viewSemaforo = findViewById<View>(R.id.viewSemaforo)
        val tvEstado     = findViewById<TextView>(R.id.tvEstadoTexto)
        val tvEva        = findViewById<TextView>(R.id.tvEvaValor)

        cardEstado.visibility = View.VISIBLE
        viewSemaforo.setBackgroundColor(color)
        viewSemaforo.background = createCircleDrawable(color)
        tvEstado.text = textoResId
        tvEva.text    = "$ultimaEva/10"
        tvEva.setTextColor(color)

        Log.d(TAG, "  ✅ Card semáforo ahora VISIBLE")
    }

    private fun createCircleDrawable(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun mostrarGrafica(registros: List<com.google.firebase.firestore.DocumentSnapshot>) {
        Log.d(TAG, "  📈 Dentro de mostrarGrafica(), registros: ${registros.size}")

        val ultimas = registros.take(10).reversed()
        val evas = ultimas.mapIndexedNotNull { i, doc ->
            val eva = doc.getLong("eva")?.toFloat() ?: return@mapIndexedNotNull null
            Entry(i.toFloat(), eva)
        }

        Log.d(TAG, "  📊 Puntos de datos (EVAs): ${evas.size}")

        if (evas.size < 2) {
            Log.w(TAG, "  ⚠️ No hay suficientes datos para gráfica (mínimo 2)")
            return
        }

        val chartColor = ContextCompat.getColor(this, R.color.chart_eva)

        val dsEva = LineDataSet(evas, "Dolor (EVA)").apply {
            color = chartColor
            setCircleColor(chartColor)
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val chart = findViewById<LineChart>(R.id.chartEvolucionFisio)
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            legend.isEnabled = true
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                setDrawLabels(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 10f
                granularity = 1f
            }
            axisRight.isEnabled = false
            data = LineData(dsEva)
            animateX(400)
            invalidate()
        }

        findViewById<CardView>(R.id.cardGrafica).visibility = View.VISIBLE
        Log.d(TAG, "  ✅ Card gráfica ahora VISIBLE")
    }

    private fun mostrarNotas(registros: List<com.google.firebase.firestore.DocumentSnapshot>) {
        Log.d(TAG, "  📝 Dentro de mostrarNotas(), registros: ${registros.size}")

        val conNotas = registros.filter {
            (it.getString("notas") ?: "").isNotBlank()
        }.take(5)

        Log.d(TAG, "  📝 Registros con notas: ${conNotas.size}")

        val cardNotas     = findViewById<CardView>(R.id.cardNotas)
        val container     = findViewById<LinearLayout>(R.id.containerNotas)
        val tvSinNotas    = findViewById<TextView>(R.id.tvSinNotas)
        val sdf           = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

        cardNotas.visibility = View.VISIBLE

        if (conNotas.isEmpty()) {
            Log.w(TAG, "  ⚠️ No hay notas, mostrando mensaje 'sin notas'")
            tvSinNotas.visibility = View.VISIBLE
            return
        }

        tvSinNotas.visibility = View.GONE
        val inflater = LayoutInflater.from(this)

        conNotas.forEachIndexed { i, doc ->
            val nota      = doc.getString("notas") ?: return@forEachIndexed
            val dia       = doc.getString("dia") ?: ""
            val fecha     = doc.getTimestamp("fecha")?.toDate()?.let { sdf.format(it) } ?: ""
            val ejercicios = doc.get("ejerciciosCompletadosNombres")
            @Suppress("UNCHECKED_CAST")
            val nombresEj = (ejercicios as? List<String>)?.joinToString(", ") ?: ""

            Log.d(TAG, "    📄 Nota ${i+1}: $dia - ${nota.take(30)}...")

            val itemView = inflater.inflate(R.layout.item_nota_paciente, container, false)
            itemView.findViewById<TextView>(R.id.tvFechaNota).text =
                if (fecha.isNotBlank()) "$dia · $fecha" else dia
            itemView.findViewById<TextView>(R.id.tvEjerciciosNota).text =
                if (nombresEj.isNotBlank()) "Ejercicio: $nombresEj" else ""
            itemView.findViewById<TextView>(R.id.tvTextoNota).text = nota

            container.addView(itemView)
        }

        Log.d(TAG, "  ✅ Card notas ahora VISIBLE con ${conNotas.size} notas")
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

        mapOf(
            "Lunes"     to R.id.dotLunes,
            "Martes"    to R.id.dotMartes,
            "Miércoles" to R.id.dotMiercoles,
            "Jueves"    to R.id.dotJueves,
            "Viernes"   to R.id.dotViernes,
            "Sábado"    to R.id.dotSabado,
            "Domingo"   to R.id.dotDomingo
        ).forEach { (dia, viewId) ->
            findViewById<View>(viewId).setBackgroundResource(
                if (dia in diasConEjercicio) R.drawable.circle_dot_active
                else R.drawable.circle_dot_inactive
            )
        }
    }
}