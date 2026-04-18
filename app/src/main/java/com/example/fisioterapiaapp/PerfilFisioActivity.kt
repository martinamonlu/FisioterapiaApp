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

// PANTALLA DE PERFIL DEL FISIOTERAPEUTA
// Funcionalidad II: ver y modificar los datos personales del fisio
class PerfilFisioActivity : AppCompatActivity() {

    // Lista de todos los campos editables del perfil
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

        // Agrupamos los campos para activarlos/desactivarlos todos a la vez
        camposEditables = listOf(etNombre, etApellidos, etNacimiento, etColegiado, etEspecialidad, etEmail)

        // CARGA DE DATOS: rellena los campos con los valores guardados en PerfilFisioData
        // Así los cambios se mantienen aunque se destruya y recree la Activity
        etNombre.setText(PerfilFisioData.nombre)
        etApellidos.setText(PerfilFisioData.apellidos)
        etNacimiento.setText(PerfilFisioData.nacimiento)
        etColegiado.setText(PerfilFisioData.colegiado)
        etEspecialidad.setText(PerfilFisioData.especialidad)
        etEmail.setText(PerfilFisioData.email)

        // Estado inicial: campos en modo sólo lectura, botón guardar oculto
        setCamposHabilitados(false)
        btnGuardar.visibility = android.view.View.GONE

        // BOTÓN ATRÁS: cierra esta pantalla y vuelve al dashboard
        btnBack.setOnClickListener {
            finish()
        }

        // BOTÓN EDITAR: activa todos los campos para que el fisio pueda modificarlos
        btnEdit.setOnClickListener {
            setCamposHabilitados(true)
            etNombre.requestFocus()
            btnGuardar.visibility = android.view.View.VISIBLE
            Toast.makeText(this, getString(R.string.msg_modo_edicion), Toast.LENGTH_SHORT).show()
        }

        // BOTÓN GUARDAR: persiste los datos en PerfilFisioData y vuelve a modo lectura
        btnGuardar.setOnClickListener {
            // Validación: el nombre no puede estar vacío
            if (etNombre.text.toString().trim().isEmpty()) {
                etNombre.error = getString(R.string.error_campo_obligatorio)
                etNombre.requestFocus()
                return@setOnClickListener
            }
            // Guardamos los valores en el objeto compartido para que sobrevivan a la recreación
            PerfilFisioData.nombre       = etNombre.text.toString().trim()
            PerfilFisioData.apellidos    = etApellidos.text.toString().trim()
            PerfilFisioData.nacimiento   = etNacimiento.text.toString().trim()
            PerfilFisioData.colegiado    = etColegiado.text.toString().trim()
            PerfilFisioData.especialidad = etEspecialidad.text.toString().trim()
            PerfilFisioData.email        = etEmail.text.toString().trim()

            setCamposHabilitados(false)
            btnGuardar.visibility = android.view.View.GONE
            Toast.makeText(this, getString(R.string.msg_perfil_guardado), Toast.LENGTH_SHORT).show()
        }
    }

    // Habilita o deshabilita todos los campos editables del perfil
    private fun setCamposHabilitados(habilitado: Boolean) {
        camposEditables.forEach { campo ->
            campo.isEnabled = habilitado
        }
    }
}