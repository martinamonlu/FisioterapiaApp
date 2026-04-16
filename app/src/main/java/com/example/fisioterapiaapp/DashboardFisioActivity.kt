package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

// DASHBOARD DEL FISIOTERAPEUTA
// Pantalla principal tras iniciar sesión
// Muestra una lista de pacientes y botones de acción en la parte inferior
class DashboardFisioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_fisio)

        // DATOS DE PRUEBA: lista de pacientes de ejemplo
        val pacientesEjemplo = listOf(
            Paciente("Juan Pérez García"),
            Paciente("María Rodríguez López")
        )

        // CONFIGURACIÓN DEL RECYCLERVIEW (muestra la lista de pacientes en pantalla)
        val rvPacientes = findViewById<RecyclerView>(R.id.rvPacientes)
        // Define cómo se organizan los elementos (lista vertical)
        rvPacientes.layoutManager = LinearLayoutManager(this)
        // Asocia los datos con el adaptador para mostrarlos en pantalla
        rvPacientes.adapter = PacienteAdapter(pacientesEjemplo)

        // BOTONES DE NAVEGACIÓN INFERIOR
        val btnPerfil = findViewById<ImageButton>(R.id.btnPerfil)
        val btnAdd = findViewById<ImageButton>(R.id.btnAddPaciente)
        val btnConfig = findViewById<ImageButton>(R.id.btnConfig)

        // LÓGICA DE INTERACCIÓN CON LOS BOTONES
        btnPerfil.setOnClickListener {
            //Toast.makeText(this, "Perfil del Fisioterapeuta", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PerfilFisioActivity::class.java)
            startActivity(intent)
        }

        btnConfig.setOnClickListener {
            Toast.makeText(this, "Ajustes de la aplicación", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            // Aquí luego abriremos la pantalla de añadir
            val intent = Intent(this, AddPacienteActivity::class.java)
            startActivity(intent)
        }
    }
}