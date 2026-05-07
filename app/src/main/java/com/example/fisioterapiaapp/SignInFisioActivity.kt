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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInFisioActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.sign_in_fisio)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ENLACE CON ELEMENTOS DE LA INTERFAZ
        val etEmail = findViewById<EditText>(R.id.etDni) // Usamos el mismo ID pero ahora para email
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnEntrar = findViewById<Button>(R.id.btn_entrar)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // LÓGICA DE INICIO DE SESIÓN CON FIREBASE
        btnEntrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validación de campos vacíos
            when {
                email.isEmpty() -> {
                    etEmail.error = "El email es obligatorio"
                    etEmail.requestFocus()
                }
                password.isEmpty() -> {
                    etPassword.error = "La contraseña es obligatoria"
                    etPassword.requestFocus()
                }
                else -> {
                    iniciarSesionConFirebase(email, password)
                }
            }
        }

        // IR A PANTALLA DE REGISTRO
        tvRegistrarse.setOnClickListener {
            val intent = Intent(this, SignUpFisioActivity::class.java)
            startActivity(intent)
        }

        // BOTÓN ATRÁS
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun iniciarSesionConFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Verificar que el usuario es un fisioterapeuta Y está aprobado
                db.collection("usuarios")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val tipo = document.getString("tipo")
                            val aprobado = document.getBoolean("aprobado") ?: false

                            when {
                                tipo != "fisio" -> {
                                    // No es fisioterapeuta
                                    Toast.makeText(
                                        this,
                                        "Esta cuenta no es de fisioterapeuta",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    auth.signOut()
                                }
                                !aprobado -> {
                                    // Es fisio pero NO está aprobado
                                    Toast.makeText(
                                        this,
                                        "Tu cuenta está pendiente de aprobación por el administrador",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    auth.signOut()
                                }
                                else -> {
                                    // Es fisio Y está aprobado - permitir acceso
                                    Toast.makeText(this, "Bienvenid@", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, DashboardFisioActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        } else {
                            // El documento no existe en Firestore
                            Toast.makeText(
                                this,
                                "Error: Usuario no encontrado en la base de datos",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al verificar usuario: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                // Manejar errores de autenticación
                val mensaje = when {
                    e.message?.contains("no user record") == true ||
                            e.message?.contains("password is invalid") == true ||
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                        "Email o contraseña incorrectos"
                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"
                    else -> "Error de autenticación: ${e.message}"
                }

                Snackbar.make(
                    findViewById(android.R.id.content),
                    mensaje,
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }
}