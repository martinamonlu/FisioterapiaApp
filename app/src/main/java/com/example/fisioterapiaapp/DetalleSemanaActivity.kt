package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class DetalleSemanaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private val diasSemana = listOf(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_semana)

        val planId          = intent.getStringExtra("planId") ?: ""
        val semana          = intent.getIntExtra("semana", 1)
        val duracionSemanas = intent.getIntExtra("duracionSemanas", 1)
        val nombre          = intent.getStringExtra("nombre") ?: ""
        val pacienteId      = intent.getStringExtra("pacienteId") ?: ""

        findViewById<TextView>(R.id.tvTituloDetalleSemana).text =
            "$nombre · Semana $semana/$duracionSemanas"

        findViewById<ImageButton>(R.id.btnBackDetalleSemana).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnEditarPlanDetalle).setOnClickListener {
            val i = Intent(this, CrearPlanActivity::class.java).apply {
                putExtra("planId", planId)
                putExtra("pacienteId", pacienteId)
                putExtra("nombrePaciente", nombre)
                putExtra("modoEdicion", true)
            }
            startActivity(i)
        }

        if (planId.isNotEmpty()) {
            cargarDetalleSemana(planId)
        } else {
            Toast.makeText(this, "Error: plan no encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDetalleSemana(planId: String) {
        db.collection("planes_ejercicio").document(planId).get()
            .addOnSuccessListener { doc ->
                val fase      = doc.getString("fase") ?: ""
                val objetivos = doc.getString("objetivos") ?: ""
                val duracion  = doc.getLong("duracionSemanas")?.toInt() ?: 0

                @Suppress("UNCHECKED_CAST")
                val ejercicios = (doc.get("ejercicios") as? List<Map<String, Any>>) ?: emptyList()

                val container = findViewById<LinearLayout>(R.id.containerDetalleSemana)
                container.removeAllViews()

                añadirInfoPlan(container, fase, objetivos, duracion)

                for (dia in diasSemana) {
                    val ejerciciosDia = ejercicios.filter { ej ->
                        @Suppress("UNCHECKED_CAST")
                        val diaList = ej["diasSemana"] as? List<String> ?: emptyList()
                        dia in diaList
                    }
                    añadirTarjetaDia(container, dia, ejerciciosDia)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar el plan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun añadirInfoPlan(container: LinearLayout, fase: String, objetivos: String, duracion: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_plan_info_fisio, container, false)
        view.findViewById<TextView>(R.id.tvFasePlan).text = fase
        view.findViewById<TextView>(R.id.tvObjetivosPlan).text = objetivos
        view.findViewById<TextView>(R.id.tvDuracionPlan).text = "$duracion semanas"
        container.addView(view)
    }

    private fun añadirTarjetaDia(container: LinearLayout, dia: String, ejercicios: List<Map<String, Any>>) {
        val diaView = LayoutInflater.from(this)
            .inflate(R.layout.item_dia_ejercicios, container, false)

        diaView.findViewById<TextView>(R.id.tvTituloDia).text = dia
        val num = ejercicios.size
        diaView.findViewById<TextView>(R.id.tvNumEjercicios).text =
            if (num == 0) "Descanso" else "$num ejercicio${if (num > 1) "s" else ""}"

        // Ocultar botón registrar sesión (es solo para el paciente)
        diaView.findViewById<MaterialButton>(R.id.btnRegistrarDia).visibility = View.GONE

        val ejContainer = diaView.findViewById<LinearLayout>(R.id.containerEjerciciosDia)
        val tvDescanso  = diaView.findViewById<TextView>(R.id.tvDescanso)

        if (ejercicios.isEmpty()) {
            tvDescanso.visibility = View.VISIBLE
            ejContainer.visibility = View.GONE
        } else {
            tvDescanso.visibility = View.GONE
            ejContainer.visibility = View.VISIBLE

            for (ej in ejercicios) {
                val ejView = LayoutInflater.from(this)
                    .inflate(R.layout.item_ejercicio_dia, ejContainer, false)

                val nombre = ej["nombreEjercicio"] as? String ?: ""
                val tipo   = ej["tipo"] as? String ?: ""
                val reps   = ej["repeticiones"]?.toString() ?: ""
                val series = ej["series"]?.toString() ?: ""
                val peso   = ej["peso"] as? String ?: ""

                ejView.findViewById<TextView>(R.id.tvNombreEjercicio).text = nombre
                ejView.findViewById<TextView>(R.id.tvTipoEjercicio).text = tipo
                ejView.findViewById<TextView>(R.id.tvSeriesReps).text =
                    if (series.isNotEmpty() && reps.isNotEmpty()) "${series}×${reps}" else ""
                ejView.findViewById<TextView>(R.id.tvCarga).text =
                    if (peso.isNotEmpty()) "· $peso" else ""

                // Ocultar estado (solo relevante para paciente) e icono
                ejView.findViewById<ImageView>(R.id.ivEstado).visibility = View.GONE
                ejView.findViewById<MaterialButton>(R.id.btnVerVideo).visibility = View.GONE

                ejContainer.addView(ejView)
            }
        }

        container.addView(diaView)
    }
}
