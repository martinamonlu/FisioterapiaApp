package com.example.fisioterapiaapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFisioActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var camposEditables: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_fisio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ENLACE CON ELEMENTOS DE LA INTERFAZ
        val btnBack        = findViewById<ImageButton>(R.id.btnBack)
        val btnEdit        = findViewById<ImageButton>(R.id.btnEdit)
        val btnGuardar     = findViewById<Button>(R.id.btnGuardarPerfil)
        val etNombre       = findViewById<EditText>(R.id.etNombre)
        val etApellidos    = findViewById<EditText>(R.id.etApellidos)
        val etNacimiento   = findViewById<EditText>(R.id.etNacimiento)
        val etColegiado    = findViewById<EditText>(R.id.etColegiado)
        val etEspecialidad = findViewById<EditText>(R.id.etEspecialidad)
        val etEmail        = findViewById<EditText>(R.id.etEmail)

        // Agrupamos los campos
        camposEditables = listOf(etNombre, etApellidos, etNacimiento, etColegiado, etEspecialidad, etEmail)

        // Estado inicial: campos en modo sólo lectura, botón guardar oculto
        setCamposHabilitados(false)
        btnGuardar.visibility = android.view.View.GONE

        // CARGAR DATOS REALES DE FIREBASE
        cargarDatosUsuario(etNombre, etApellidos, etNacimiento, etColegiado, etEspecialidad, etEmail)

        // BOTÓN ATRÁS
        btnBack.setOnClickListener {
            finish()
        }

        // BOTÓN EDITAR
        btnEdit.setOnClickListener {
            setCamposHabilitados(true)
            etNombre.requestFocus()
            btnGuardar.visibility = android.view.View.VISIBLE
            Toast.makeText(this, getString(R.string.msg_modo_edicion), Toast.LENGTH_SHORT).show()
        }

        // BOTÓN GUARDAR
        btnGuardar.setOnClickListener {
            if (etNombre.text.toString().trim().isEmpty()) {
                etNombre.error = getString(R.string.error_campo_obligatorio)
                etNombre.requestFocus()
                return@setOnClickListener
            }

            // Guardar en Firebase
            guardarDatosUsuario(
                etNombre.text.toString().trim(),
                etApellidos.text.toString().trim(),
                etNacimiento.text.toString().trim(),
                etColegiado.text.toString().trim(),
                etEspecialidad.text.toString().trim(),
                etEmail.text.toString().trim()
            )

            setCamposHabilitados(false)
            btnGuardar.visibility = android.view.View.GONE
        }
    }

    private fun cargarDatosUsuario(
        etNombre: EditText,
        etApellidos: EditText,
        etNacimiento: EditText,
        etColegiado: EditText,
        etEspecialidad: EditText,
        etEmail: EditText
    ) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etNombre.setText(document.getString("nombre") ?: "")
                    etApellidos.setText(document.getString("apellidos") ?: "")
                    etNacimiento.setText(document.getString("fechaNacimiento") ?: "")
                    etColegiado.setText(document.getString("numeroColegiado") ?: "")
                    etEspecialidad.setText(document.getString("especialidad") ?: "")
                    etEmail.setText(document.getString("email") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarDatosUsuario(
        nombre: String,
        apellidos: String,
        fechaNacimiento: String,
        numeroColegiado: String,
        especialidad: String,
        email: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        val datosActualizados = hashMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "fechaNacimiento" to fechaNacimiento,
            "numeroColegiado" to numeroColegiado,
            "especialidad" to especialidad,
            "email" to email
        )

        db.collection("usuarios")
            .document(userId)
            .update(datosActualizados as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.msg_perfil_guardado), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setCamposHabilitados(habilitado: Boolean) {
        camposEditables.forEach { campo ->
            campo.isEnabled = habilitado
        }
    }
}