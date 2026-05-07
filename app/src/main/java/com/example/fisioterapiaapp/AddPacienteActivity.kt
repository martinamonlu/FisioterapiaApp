package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

// PANTALLA DE REGISTRO DE NUEVO PACIENTE
// Funcionalidad I: el fisioterapeuta añade los datos de un nuevo paciente
// El diagnóstico se selecciona de un desplegable (Spinner) con patologías predefinidas
class AddPacienteActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_paciente)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ENLACE CON ELEMENTOS DE LA INTERFAZ
        val btnBack            = findViewById<ImageButton>(R.id.btnBack)
        val etNombre           = findViewById<EditText>(R.id.etNombrePaciente)
        val etApellidos        = findViewById<EditText>(R.id.etApellidosPaciente)
        val etFechaNac         = findViewById<EditText>(R.id.etFechaNacPaciente)
        val etDni              = findViewById<EditText>(R.id.etDniPaciente)
        val etTelefono         = findViewById<EditText>(R.id.etTelefonoPaciente)
        val spinnerDiagnostico = findViewById<Spinner>(R.id.spinnerDiagnostico)
        val btnGuardar         = findViewById<Button>(R.id.btnGuardarPaciente)

        // CONFIGURACIÓN DEL SPINNER DE DIAGNÓSTICO
        // Las opciones vienen de strings.xml → array "patologias"
        // ArrayAdapter conecta el array con el Spinner usando el estilo de fila estándar de Android
        val adaptadorSpinner = ArrayAdapter.createFromResource(
            this,
            R.array.patologias,
            android.R.layout.simple_spinner_item        // cómo se ve la opción seleccionada
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // cómo se ve el desplegable abierto
            spinnerDiagnostico.adapter = adapter
        }

        // BOTÓN ATRÁS: cancela sin guardar
        btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // BOTÓN GUARDAR: valida los campos obligatorios y devuelve el paciente al Dashboard
        btnGuardar.setOnClickListener {
            val nombre      = etNombre.text.toString().trim()
            val apellidos   = etApellidos.text.toString().trim()
            val dni         = etDni.text.toString().trim()
            // El Spinner siempre tiene una opción seleccionada, no necesita validación
            val diagnostico = spinnerDiagnostico.selectedItem.toString()

            // VALIDACIÓN: campos obligatorios
            when {
                nombre.isEmpty() -> {
                    etNombre.error = getString(R.string.error_campo_obligatorio)
                    etNombre.requestFocus()
                }
                apellidos.isEmpty() -> {
                    etApellidos.error = getString(R.string.error_campo_obligatorio)
                    etApellidos.requestFocus()
                }
                dni.isEmpty() -> {
                    etDni.error = getString(R.string.error_campo_obligatorio)
                    etDni.requestFocus()
                }
                else -> {
                    val paciente = hashMapOf(
                        "nombre" to nombre,
                        "apellidos" to apellidos,
                        "dni" to dni,
                        "diagnostico" to diagnostico,
                        "telefono" to etTelefono.text.toString().trim()
                    )

                    db.collection("pacientes")
                        .add(paciente)
                        .addOnSuccessListener {

                            val resultIntent = Intent().apply {
                                putExtra("nombre", nombre)
                                putExtra("apellidos", apellidos)
                                putExtra("diagnostico", diagnostico)
                            }

                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Error al guardar paciente",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }
    }
}