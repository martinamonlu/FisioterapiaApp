package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFisioActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up_fisio)

        // Enlaces con elementos de la interfaz
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etNumeroColegiado = findViewById<EditText>(R.id.etNumeroColegiado)
        val etEspecialidad = findViewById<EditText>(R.id.etEspecialidad)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmarPassword = findViewById<EditText>(R.id.etConfirmarPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvIniciarSesion = findViewById<TextView>(R.id.tvIniciarSesion)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Botón de registro
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val numeroColegiado = etNumeroColegiado.text.toString().trim()
            val especialidad = etEspecialidad.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmarPassword = etConfirmarPassword.text.toString()

            // Validaciones
            when {
                nombre.isEmpty() -> {
                    etNombre.error = "El nombre es obligatorio"
                    etNombre.requestFocus()
                }
                apellidos.isEmpty() -> {
                    etApellidos.error = "Los apellidos son obligatorios"
                    etApellidos.requestFocus()
                }
                email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Ingresa un email válido"
                    etEmail.requestFocus()
                }
                numeroColegiado.isEmpty() -> {
                    etNumeroColegiado.error = "El número de colegiado es obligatorio"
                    etNumeroColegiado.requestFocus()
                }
                especialidad.isEmpty() -> {
                    etEspecialidad.error = "La especialidad es obligatoria"
                    etEspecialidad.requestFocus()
                }
                password.isEmpty() -> {
                    etPassword.error = "La contraseña es obligatoria"
                    etPassword.requestFocus()
                }
                password.length < 6 -> {
                    etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                    etPassword.requestFocus()
                }
                confirmarPassword.isEmpty() -> {
                    etConfirmarPassword.error = "Confirma tu contraseña"
                    etConfirmarPassword.requestFocus()
                }
                password != confirmarPassword -> {
                    etConfirmarPassword.error = "Las contraseñas no coinciden"
                    etConfirmarPassword.requestFocus()
                }
                else -> {
                    // Todo validado, proceder con el registro
                    registrarFisioterapeuta(nombre, apellidos, email, numeroColegiado, especialidad, password)
                }
            }
        }

        // Link "Inicia sesión" - vuelve al login
        tvIniciarSesion.setOnClickListener {
            finish()
        }

        // Botón atrás
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun registrarFisioterapeuta(
        nombre: String,
        apellidos: String,
        email: String,
        numeroColegiado: String,
        especialidad: String,
        password: String
    ) {
        // Mostrar progreso
        Toast.makeText(this, "Creando cuenta...", Toast.LENGTH_SHORT).show()

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Crear documento del fisioterapeuta en Firestore
                val fisioterapeuta = hashMapOf(
                    "tipo" to "fisio",
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "email" to email,
                    "numeroColegiado" to numeroColegiado,
                    "especialidad" to especialidad,
                    "aprobado" to false,  // NUEVO: Pendiente de aprobación
                    "fechaRegistro" to com.google.firebase.Timestamp.now()
                )

                // Guardar en la colección "usuarios"
                db.collection("usuarios")
                    .document(userId)
                    .set(fisioterapeuta)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Cuenta creada. Pendiente de aprobación por el administrador.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Cerrar sesión automáticamente (no puede acceder hasta ser aprobado)
                        auth.signOut()

                        // Volver al login
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al guardar datos: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                // Manejar errores específicos de Firebase Authentication
                val mensaje = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Este email ya está registrado"
                    e.message?.contains("network error") == true ->
                        "Error de conexión. Verifica tu internet"
                    else -> "Error al crear cuenta: ${e.message}"
                }

                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            }
    }
}