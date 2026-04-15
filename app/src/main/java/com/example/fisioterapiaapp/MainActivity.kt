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

        // --- CÓDIGO PARA LOS BOTONES ---

        // 1. Enlazamos los botones del diseño (XML) con el código (Kotlin)
        val botonFisio = findViewById<Button>(R.id.btn_fisio)
        val botonPaciente = findViewById<Button>(R.id.btn_paciente)

        // 2. ¿Qué pasa al pulsar "Soy Fisioterapeuta"?
        botonFisio.setOnClickListener {
            // Un aviso Toast para comprobar que el botón funciona
            Toast.makeText(this, "Yendo al registro de fisios...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SignInFisioActivity::class.java)
            startActivity(intent)

            // Más adelante usaremos este hueco para hacer el salto de pantalla (Intent)
        }

        // 3. ¿Qué pasa al pulsar "Soy Paciente"?

        botonPaciente.setOnClickListener { view ->
            Toast.makeText(this, "Acceso de pacientes próximamente", Toast.LENGTH_SHORT).show()
        }


    }
}