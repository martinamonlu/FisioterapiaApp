package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInPacienteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_paciente)

        val etEmail = findViewById<EditText>(R.id.etEmailPaciente)
        val etPassword = findViewById<EditText>(R.id.etPasswordPaciente)
        val btnEntrar = findViewById<Button>(R.id.btnEntrarPaciente)
        val btnBack = findViewById<ImageButton>(R.id.btnBackPaciente)

        btnEntrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                email.isEmpty() -> etEmail.error = "Email obligatorio"
                password.isEmpty() -> etPassword.error = "Contraseña obligatoria"
                else -> verificarPaciente(email, password)
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }


    private fun verificarPaciente(email: String, password: String) {

        db.collection("pacientes")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {

                    Toast.makeText(
                        this,
                        "No existe ningún paciente con ese email",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val pacienteDoc = documents.documents[0]

                val estadoCuenta =
                    pacienteDoc.getString("estadoCuenta") ?: "pendiente"

                if (estadoCuenta == "pendiente") {

                    activarCuentaPaciente(
                        pacienteDoc.id,
                        email,
                        password
                    )

                } else {

                    iniciarSesion(email, password)
                }
            }
    }



    private fun iniciarSesion(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener

                // Verificar si es primer login
                db.collection("pacientes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { documents ->

                        val document = documents.documents.firstOrNull()
                        if (document == null) return@addOnSuccessListener

                        val primerLogin =
                            document.getBoolean("primerLogin") ?: false

                        if (primerLogin) {
                            // Obligar a cambiar contraseña
                            mostrarDialogoCambioContraseña()
                        } else {
                            // Ir al dashboard del paciente
                            Toast.makeText(this, "Bienvenid@", Toast.LENGTH_SHORT).show()
                            // TODO: Cuando crees DashboardPacienteActivity, descomenta estas líneas:
                            // val intent = Intent(this, DashboardPacienteActivity::class.java)
                            // startActivity(intent)
                            // finish()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Email o contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun activarCuentaPaciente(
        pacienteId: String,
        email: String,
        password: String
    ) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->

                val authUid = authResult.user?.uid ?: return@addOnSuccessListener

                db.collection("pacientes")
                    .document(pacienteId)
                    .update(
                        mapOf(
                            "userId" to authUid,
                            "estadoCuenta" to "activa"
                        )
                    )
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Cuenta activada correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        iniciarSesion(email, password)
                    }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Error al activar cuenta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }



    private fun mostrarDialogoCambioContraseña() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cambio_password, null)
        val etNuevaPassword = dialogView.findViewById<EditText>(R.id.etNuevaPassword)
        val etConfirmarPassword = dialogView.findViewById<EditText>(R.id.etConfirmarPassword)

        AlertDialog.Builder(this)
            .setTitle("Cambio de contraseña obligatorio")
            .setMessage("Por seguridad, debes cambiar tu contraseña temporal")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Cambiar") { _, _ ->
                val nueva = etNuevaPassword.text.toString()
                val confirmar = etConfirmarPassword.text.toString()

                when {
                    nueva.length < 6 -> {
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                        mostrarDialogoCambioContraseña()
                    }
                    nueva != confirmar -> {
                        Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                        mostrarDialogoCambioContraseña()
                    }
                    else -> cambiarContraseña(nueva)
                }
            }
            .show()
    }

    private fun cambiarContraseña(nuevaPassword: String) {
        val user = auth.currentUser ?: return

        user.updatePassword(nuevaPassword)
            .addOnSuccessListener {
                // Actualizar en Firestore que ya no es primer login
                db.collection("pacientes")
                    .whereEqualTo("userId", user.uid)
                    .get()
                    .addOnSuccessListener { documents ->

                        val pacienteDoc = documents.documents.firstOrNull()
                        if (pacienteDoc == null) return@addOnSuccessListener

                        db.collection("pacientes")
                            .document(pacienteDoc.id)
                            .update("primerLogin", false)

                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Contraseña actualizada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Toast.makeText(this, "¡Bienvenid@ a Rehapp!", Toast.LENGTH_SHORT)
                                    .show()

                                auth.signOut()

                                Toast.makeText(this, "Contraseña actualizada. Vuelve a iniciar sesión", Toast.LENGTH_LONG).show()

                                startActivity(Intent(this, SignInPacienteActivity::class.java))
                                finish()

                                // TODO: Cuando crees DashboardPacienteActivity, descomenta:
                                // val intent = Intent(this, DashboardPacienteActivity::class.java)
                                // startActivity(intent)
                                // finish()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cambiar contraseña: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}