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

        // BOTÓN GUARDAR: desactiva los campos y confirma que los datos se han guardado
        btnGuardar.setOnClickListener {
            // Validación: el nombre no puede estar vacío
            if (etNombre.text.toString().trim().isEmpty()) {
                etNombre.error = getString(R.string.error_campo_obligatorio)
                etNombre.requestFocus()
                return@setOnClickListener
            }
            // TODO: aquí se persistiría en base de datos cuando se implemente
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