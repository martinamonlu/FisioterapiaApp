package com.example.fisioterapiaapp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class DashboardFisioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_fisio)

        // 1. Datos de ejemplo
        val pacientesEjemplo = listOf(
            Paciente("Juan Pérez García"),
            Paciente("María Rodríguez López")
        )

        // 2. Configurar RecyclerView
        val rvPacientes = findViewById<RecyclerView>(R.id.rvPacientes)
        rvPacientes.layoutManager = LinearLayoutManager(this)
        rvPacientes.adapter = PacienteAdapter(pacientesEjemplo)

        // 3. Botones inferiores
        val btnPerfil = findViewById<ImageButton>(R.id.btnPerfil)
        val btnAdd = findViewById<ImageButton>(R.id.btnAddPaciente)
        val btnConfig = findViewById<ImageButton>(R.id.btnConfig)

        btnPerfil.setOnClickListener {
            Toast.makeText(this, "Perfil del Fisioterapeuta", Toast.LENGTH_SHORT).show()
        }

        btnConfig.setOnClickListener {
            Toast.makeText(this, "Ajustes de la aplicación", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            // Aquí luego abriremos la pantalla de añadir
        }
    }
}