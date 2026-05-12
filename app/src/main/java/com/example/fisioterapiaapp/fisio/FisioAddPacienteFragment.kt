package com.example.fisioterapiaapp.fisio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.fisioterapiaapp.CrearPlanActivity
import com.example.fisioterapiaapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FisioAddPacienteFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_fisio_add_paciente_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNombre = view.findViewById<EditText>(R.id.etNombrePaciente)
        val etApellidos = view.findViewById<EditText>(R.id.etApellidosPaciente)
        val etDni = view.findViewById<EditText>(R.id.etDniPaciente)
        val etEmail = view.findViewById<EditText>(R.id.etEmailPaciente)
        val spinnerDiagnostico = view.findViewById<Spinner>(R.id.spinnerDiagnostico)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarPaciente)

        val patologias = resources.getStringArray(R.array.patologias)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, patologias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiagnostico.adapter = adapter

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val diagnostico = spinnerDiagnostico.selectedItem.toString()

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
                else -> crearCuentaPaciente(email, nombre, apellidos, dni, diagnostico)
            }
        }
    }

    private fun crearCuentaPaciente(
        email: String,
        nombre: String,
        apellidos: String,
        dni: String,
        diagnostico: String
    ) {
        val fisioUid = auth.currentUser?.uid ?: return
        val pacienteId = db.collection("pacientes").document().id

        val paciente = hashMapOf(
            "nombre" to nombre,
            "apellidos" to apellidos,
            "dni" to dni,
            "email" to email,
            "diagnostico" to diagnostico,
            "fisioterapeutaId" to fisioUid,
            "userId" to null,
            "primerLogin" to true,
            "estadoCuenta" to "pendiente",
            "fechaRegistro" to com.google.firebase.Timestamp.now()
        )

        db.collection("pacientes")
            .document(pacienteId)
            .set(paciente)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Paciente registrado correctamente", Toast.LENGTH_LONG).show()

                // Ir a crear plan manteniendo la pila de fragments
                val intent = Intent(requireContext(), CrearPlanActivity::class.java).apply {
                    putExtra("pacienteId", pacienteId)
                    putExtra("nombrePaciente", "$nombre $apellidos")
                }
                startActivity(intent)

                // Volver al fragment de pacientes tras registrar
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fisioFragmentContainer, FisioPacientesFragment())
                    .commit()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar paciente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}