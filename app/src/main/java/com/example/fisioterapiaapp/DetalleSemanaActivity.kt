package com.example.fisioterapiaapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class DetalleSemanaActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_semana)

        val planId          = intent.getStringExtra("planId") ?: ""
        val semana          = intent.getIntExtra("semana", 1)
        val duracionSemanas = intent.getIntExtra("duracionSemanas", 1)
        val nombre          = intent.getStringExtra("nombre") ?: ""

        findViewById<TextView>(R.id.tvTituloDetalleSemana).text =
            "$nombre · Semana $semana/$duracionSemanas"

        findViewById<ImageButton>(R.id.btnBackDetalleSemana).setOnClickListener { finish() }

        cargarDetalleSemana(planId, semana)
    }

    private fun cargarDetalleSemana(planId: String, semana: Int) {
        db.collection("planes_ejercicio").document(planId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val ejercicios = (doc.get("ejercicios") as? List<Map<String, Any>>) ?: emptyList()
                // TODO: siguiente paso — mostrar ejercicios agrupados por día
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar detalle", Toast.LENGTH_SHORT).show()
            }
    }
}