package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

// PANTALLA DE INICIO DE SESIÓN
// Permite iniciar sesión o registrarse si el usuario es nuevo
class SignInFisioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sign_in_fisio)
        // VALERIA: esto no lo entiendo y no se si sobra? ajusta el padding para evitar que la interfaz quede tapada por las barras del sistema?
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ENLACE CON ELEMENTOS DE LA INTERFAZ (XML → Kotlin)
        val etDni = findViewById<EditText>(R.id.etDni)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnEntrar = findViewById<Button>(R.id.btn_entrar)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // LÓGICA DE VALIDACIÓN DEL INICIO DE SESIÓN
        btnEntrar.setOnClickListener {
            val dniIngresado = etDni.text.toString()
            val passwordIngresada = etPassword.text.toString()

            // 1ª Validación: ¿Campos vacíos?
            if (dniIngresado.isEmpty()) {
                // etDni.error = getString(R.string.error_dni) --> lo quitaria para tenerlo igual que el error de contraseña
                etDni.error = "El DNI es obligatorio"
            }
            else if (passwordIngresada.isEmpty()) {
                etPassword.error = "La contraseña es obligatoria"
            }
            // 2ª Validación: ¿Son las credenciales correctas?
            // Al no usar una base de datos con DNI y contraseña, lo simplificamos con unos predeterminados
            else if (dniIngresado == "12345678A" && passwordIngresada == "admin123") {
                Toast.makeText(this, "Bienvenid@", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashboardFisioActivity::class.java)
                startActivity(intent)
                finish()
            }
            // 3ª Validación: Credenciales incorrectas
            else {
                Snackbar.make(findViewById(android.R.id.content), "DNI o contraseña incorrectos", Snackbar.LENGTH_LONG).show()
            }
        }

        // LÓGICA PARA IR A LA PANTALLA DE REGISTRO (Nueva cuenta): funcionalidad todavía no implementada
        tvRegistrarse.setOnClickListener {
            Toast.makeText(this, "Registro de usuarios próximamente", Toast.LENGTH_SHORT).show()
            //val intent = Intent(this, SignUpFisioActivity::class.java)
            //startActivity(intent)
        }

        // LÓGICA DEL BOTÓN ATRÁS
        btnBack.setOnClickListener {
            // Cerramos esta actividad para volver a la principal
            finish()
        }
    }
}