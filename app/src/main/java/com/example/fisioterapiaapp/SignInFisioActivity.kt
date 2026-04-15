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

class SignInFisioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // OJO: Asegúrate de que el nombre del layout coincide con tu archivo XML
        setContentView(R.layout.sign_in_fisio)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Enlazamos los componentes del nuevo XML
        val etDni = findViewById<EditText>(R.id.etDni)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnEntrar = findViewById<Button>(R.id.btn_entrar)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        // 1.1 Enlazamos el botón de atrás
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // 2. Lógica de VALIDACIÓN del Inicio de Sesión
        btnEntrar.setOnClickListener {
            val dniIngresado = etDni.text.toString()
            val passwordIngresada = etPassword.text.toString()

            // 1ª Validación: ¿Campos vacíos?
            if (dniIngresado.isEmpty()) {
                etDni.error = getString(R.string.error_dni)
            } else if (passwordIngresada.isEmpty()) {
                etPassword.error = "La contraseña es obligatoria"
            }
            // 2ª Validación: ¿Son las credenciales de las desarrolladoras?
            else if (dniIngresado == "12345678A" && passwordIngresada == "admin123") {
                // Al no usar una base de datos con DNI y contraseña, lo simplificamos con unos predeterminados
                Toast.makeText(this, "Bienvenid@", Toast.LENGTH_SHORT).show()

                // Aquí saltaremos a la pantalla principal del fisio (Pacientes)
                val intent = Intent(this, DashboardFisioActivity::class.java)
                startActivity(intent)
                finish()
            }
            // 3ª Validación: Credenciales incorrectas
            else {
                android.util.Log.d("PRUEBA", "El código ha llegado al fallo de credenciales")
                Snackbar.make(findViewById(android.R.id.content), "DNI o contraseña incorrectos", Snackbar.LENGTH_LONG).show()
            }

        }

        // Lógica para ir a la pantalla de REGISTRO (Nueva cuenta)
        //tvRegistrarse.setOnClickListener {
            // Lo dejamos para el caso práctico
            //val intent = Intent(this, SignUpFisioActivity::class.java)
            //startActivity(intent)
        //}

        // 3. LÓGICA DEL BOTÓN ATRÁS
        btnBack.setOnClickListener {
            // Cerramos esta actividad para volver a la principal
            finish()
        }
    }
}