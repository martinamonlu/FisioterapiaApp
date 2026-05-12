package com.example.fisioterapiaapp.fisio

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.DetallePacienteActivity
import com.example.fisioterapiaapp.PacienteAdapter
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.paciente.model.Paciente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FisioPacientesFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val listaPacientes = mutableListOf<Paciente>()
    private val listaFiltrada = mutableListOf<Paciente>()
    private lateinit var adaptador: PacienteAdapter

    private val addPacienteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val nombre = result.data?.getStringExtra("nombre") ?: ""
            val apellidos = result.data?.getStringExtra("apellidos") ?: ""
            if (nombre.isNotEmpty()) {
                Toast.makeText(requireContext(), "Paciente $nombre $apellidos registrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_fisio_pacientes_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvPacientes = view.findViewById<RecyclerView>(R.id.rvPacientes)
        rvPacientes.layoutManager = LinearLayoutManager(requireContext())

        adaptador = PacienteAdapter(listaFiltrada) { paciente ->
            val intent = Intent(requireContext(), DetallePacienteActivity::class.java).apply {
                putExtra("pacienteId", paciente.id)
                putExtra("nombre", paciente.nombre)
                putExtra("apellidos", paciente.apellidos)
                putExtra("email", paciente.email)
                putExtra("diagnostico", paciente.diagnostico)
            }
            startActivity(intent)
        }
        rvPacientes.adapter = adaptador

        val etBuscar = view.findViewById<EditText>(R.id.etBuscar)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarPacientes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarPacientes()
    }

    override fun onResume() {
        super.onResume()
        cargarPacientes()
    }

    private fun cargarPacientes() {
        val fisioId = auth.currentUser?.uid ?: return

        db.collection("pacientes")
            .whereEqualTo("fisioterapeutaId", fisioId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                listaPacientes.clear()

                for (doc in snapshots!!) {
                    listaPacientes.add(
                        Paciente(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            apellidos = doc.getString("apellidos") ?: "",
                            email = doc.getString("email") ?: "",
                            diagnostico = doc.getString("diagnostico") ?: "",
                            fisioterapeutaId = doc.getString("fisioterapeutaId") ?: "",
                            userId = doc.getString("userId"),
                            estadoCuenta = doc.getString("estadoCuenta") ?: "pendiente"
                        )
                    )
                }
                filtrarPacientes("")
            }
    }

    private fun filtrarPacientes(busqueda: String) {
        listaFiltrada.clear()
        if (busqueda.isEmpty()) {
            listaFiltrada.addAll(listaPacientes)
        } else {
            val query = busqueda.lowercase()
            listaFiltrada.addAll(
                listaPacientes.filter {
                    it.nombre.lowercase().contains(query) ||
                            it.apellidos.lowercase().contains(query) ||
                            it.diagnostico.lowercase().contains(query)
                }
            )
        }
        adaptador.notifyDataSetChanged()
    }
}