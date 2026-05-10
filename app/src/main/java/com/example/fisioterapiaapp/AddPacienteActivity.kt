package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddPacienteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_paciente)

        val etNombre = findViewById<EditText>(R.id.etNombrePaciente)
        val etApellidos = findViewById<EditText>(R.id.etApellidosPaciente)
        val etDni = findViewById<EditText>(R.id.etDniPaciente)
        val etEmail = findViewById<EditText>(R.id.etEmailPaciente)
        val spinnerDiagnostico = findViewById<Spinner>(R.id.spinnerDiagnostico)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPaciente)

        // CONFIGURAR EL SPINNER CON LAS OPCIONES DE DIAGNÓSTICO
        val patologias = resources.getStringArray(R.array.patologias)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, patologias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiagnostico.adapter = adapter

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val diagnostico = spinnerDiagnostico.selectedItem.toString()

            // Validaciones
            when {
                nombre.isEmpty() -> {
                    etNombre.error = "Campo obligatorio"
                    etNombre.requestFocus()
                }
                apellidos.isEmpty() -> {
                    etApellidos.error = "Campo obligatorio"
                    etApellidos.requestFocus()
                }
                dni.isEmpty() -> {
                    etDni.error = "Campo obligatorio"
                    etDni.requestFocus()
                }
                email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Email válido obligatorio"
                    etEmail.requestFocus()
                }
                else -> {
                    // Generar contraseña temporal (DNI del paciente)
                    val contraseñaTemporal = dni

                    // Crear usuario en Firebase Authentication
                    crearCuentaPaciente(email, contraseñaTemporal, nombre, apellidos, dni, diagnostico)
                }
            }
        }
    }

    private fun crearCuentaPaciente(
        email: String,
        password: String,
        nombre: String,
        apellidos: String,
        dni: String,
        diagnostico: String
    ) {

        val fisioUid = auth.currentUser?.uid

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Guardar datos del paciente en Firestore
                val paciente = hashMapOf(
                    "nombre" to nombre,
                    "apellidos" to apellidos,
                    "dni" to dni,
                    "email" to email,
                    "diagnostico" to diagnostico,
                    "fisioterapeutaId" to fisioUid,
                    "userId" to userId,
                    "primerLogin" to true,
                    "fechaRegistro" to com.google.firebase.Timestamp.now()
                )

                db.collection("pacientes")
                    .document(userId)
                    .set(paciente)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Paciente registrado. Usuario: $email | Contraseña temporal: $dni",
                            Toast.LENGTH_LONG
                        ).show()

                        // IR DIRECTO A CREAR PLAN
                        val intent = Intent(this, CrearPlanActivity::class.java).apply {
                            putExtra("pacienteId", userId)
                            putExtra("nombrePaciente", "$nombre $apellidos")
                        }
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al guardar paciente: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al crear cuenta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}