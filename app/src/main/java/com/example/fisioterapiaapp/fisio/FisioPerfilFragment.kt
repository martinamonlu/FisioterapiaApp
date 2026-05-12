package com.example.fisioterapiaapp.fisio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.fisioterapiaapp.MainActivity
import com.example.fisioterapiaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FisioPerfilFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var camposEditables: List<EditText>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_fisio_perfil_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNombre       = view.findViewById<EditText>(R.id.etNombre)
        val etApellidos    = view.findViewById<EditText>(R.id.etApellidos)
        val etNacimiento   = view.findViewById<EditText>(R.id.etNacimiento)
        val etColegiado    = view.findViewById<EditText>(R.id.etColegiado)
        val etEspecialidad = view.findViewById<EditText>(R.id.etEspecialidad)
        val etEmail        = view.findViewById<EditText>(R.id.etEmail)
        val btnEditar      = view.findViewById<Button>(R.id.btnEditarPerfil)
        val btnGuardar     = view.findViewById<Button>(R.id.btnGuardarPerfil)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesionFisio)

        camposEditables = listOf(etNombre, etApellidos, etNacimiento, etColegiado, etEspecialidad, etEmail)

        setCamposHabilitados(false)
        btnGuardar.visibility = View.GONE

        cargarDatos(etNombre, etApellidos, etNacimiento, etColegiado, etEspecialidad, etEmail)

        btnEditar.setOnClickListener {
            setCamposHabilitados(true)
            etNombre.requestFocus()
            btnGuardar.visibility = View.VISIBLE
            btnEditar.visibility = View.GONE
        }

        btnGuardar.setOnClickListener {
            if (etNombre.text.toString().trim().isEmpty()) {
                etNombre.error = "Campo obligatorio"
                etNombre.requestFocus()
                return@setOnClickListener
            }
            guardarDatos(
                etNombre.text.toString().trim(),
                etApellidos.text.toString().trim(),
                etNacimiento.text.toString().trim(),
                etColegiado.text.toString().trim(),
                etEspecialidad.text.toString().trim(),
                etEmail.text.toString().trim()
            )
            setCamposHabilitados(false)
            btnGuardar.visibility = View.GONE
            btnEditar.visibility = View.VISIBLE
        }

        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres salir?")
                .setPositiveButton("Salir") { _, _ ->
                    auth.signOut()
                    startActivity(
                        Intent(requireContext(), MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun cargarDatos(
        etNombre: EditText, etApellidos: EditText, etNacimiento: EditText,
        etColegiado: EditText, etEspecialidad: EditText, etEmail: EditText
    ) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etNombre.setText(doc.getString("nombre") ?: "")
                    etApellidos.setText(doc.getString("apellidos") ?: "")
                    etNacimiento.setText(doc.getString("fechaNacimiento") ?: "")
                    etColegiado.setText(doc.getString("numeroColegiado") ?: "")
                    etEspecialidad.setText(doc.getString("especialidad") ?: "")
                    etEmail.setText(doc.getString("email") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun guardarDatos(
        nombre: String, apellidos: String, fechaNacimiento: String,
        numeroColegiado: String, especialidad: String, email: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val datos = hashMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "fechaNacimiento" to fechaNacimiento,
            "numeroColegiado" to numeroColegiado,
            "especialidad" to especialidad,
            "email" to email
        )
        db.collection("usuarios").document(uid)
            .update(datos as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setCamposHabilitados(habilitado: Boolean) {
        camposEditables.forEach { it.isEnabled = habilitado }
    }
}