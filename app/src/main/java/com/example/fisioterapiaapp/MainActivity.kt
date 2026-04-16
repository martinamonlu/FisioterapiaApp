package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

// MAIN ACTIVITY (pantalla de bienvenida)
// Permite elegir el tipo de usuario (Fisioterapeuta o Paciente)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ENLACE CON ELEMENTOS DE LA INTERFAZ (XML → Kotlin)
        val botonFisio = findViewById<Button>(R.id.btn_fisio) // Botón para acceso de fisioterapeuta
        val botonPaciente = findViewById<Button>(R.id.btn_paciente) // Botón para acceso de paciente


        // LÓGICA DE INTERACCIÓN CON LOS BOTONES
        // Acceso como fisioterapeuta:
        botonFisio.setOnClickListener {
            // Un aviso Toast para comprobar que el botón funciona
            Toast.makeText(this, "Yendo al registro de fisios...", Toast.LENGTH_SHORT).show()
            // Cambio de pantalla hacia el login de fisioterapeuta
            val intent = Intent(this, SignInFisioActivity::class.java)
            startActivity(intent)
        }

        // Acceso como paciente: funcionalidad todavía no implementada
        botonPaciente.setOnClickListener {
            Toast.makeText(this, "Acceso de pacientes próximamente", Toast.LENGTH_SHORT).show()
        }

    }
}