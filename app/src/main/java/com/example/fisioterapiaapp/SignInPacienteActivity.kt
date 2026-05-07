package com.example.fisioterapiaapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class SignInPacienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sign_in_paciente)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ELEMENTOS INTERFAZ
        val etDni = findViewById<EditText>(R.id.etDniPaciente)
        val etPassword = findViewById<EditText>(R.id.etPasswordPaciente)
        val btnEntrar = findViewById<Button>(R.id.btnEntrarPaciente)
        val btnBack = findViewById<ImageButton>(R.id.btnBackPaciente)

        // LOGIN
        btnEntrar.setOnClickListener {

            val dni = etDni.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                dni.isEmpty() -> {
                    etDni.error = getString(R.string.error_dni)
                }

                password.isEmpty() -> {
                    etPassword.error = getString(R.string.error_password)
                }

                // CREDENCIALES DE PRUEBA
                dni == "87654321B" && password == "paciente123" -> {

                    Toast.makeText(
                        this,
                        getString(R.string.msg_bienvenida),
                        Toast.LENGTH_SHORT
                    ).show()

                    // FUTURA PANTALLA DEL PACIENTE
                    // startActivity(Intent(this, DashboardPacienteActivity::class.java))

                }

                else -> {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.msg_credenciales_incorrectas),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        // BOTÓN ATRÁS
        btnBack.setOnClickListener {
            finish()
        }
    }
}