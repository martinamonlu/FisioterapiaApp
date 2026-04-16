package com.example.fisioterapiaapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PerfilFisioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_fisio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnEdit = findViewById<ImageButton>(R.id.btnEdit)

        val etNombre = findViewById<EditText>(R.id.etNombre)

        // Volver al dashboard
        btnBack.setOnClickListener {
            finish() // cierra esta pantalla y vuelve atrás
        }

        // Activar modo edición
        btnEdit.setOnClickListener {
            etNombre.isEnabled = true
            etNombre.requestFocus()
        }



    }
}