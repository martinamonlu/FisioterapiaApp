package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.paciente.model.Paciente
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardFisioActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lista mutable de pacientes (datos de la base de datos Firebase)
    private val listaPacientes = mutableListOf<Paciente>()
    private val listaFiltrada = mutableListOf<Paciente>()

    // Adaptador declarado aquí para poder actualizarlo desde el launcher
    private lateinit var adaptador: PacienteAdapter

    // LAUNCHER: espera el resultado de AddPacienteActivity
    // Cuando el usuario registra un paciente, esta función recibe los datos
    private val addPacienteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val nombre    = result.data?.getStringExtra("nombre")    ?: ""
            val apellidos = result.data?.getStringExtra("apellidos") ?: ""
            val nombreCompleto = "$nombre $apellidos".trim()

            if (nombreCompleto.isNotEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_paciente_registrado, nombre, apellidos),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Habilitar edge-to-edge para móviles con notch
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_dashboard_fisio)

        // CONFIGURACIÓN DEL RECYCLERVIEW
        val rvPacientes = findViewById<RecyclerView>(R.id.rvPacientes)
        rvPacientes.layoutManager = LinearLayoutManager(this)

        // Adapter con click listener para ir a detalle del paciente
        adaptador = PacienteAdapter(listaFiltrada) { paciente ->
            // Click en paciente → ir a detalle
            val intent = Intent(this, DetallePacienteActivity::class.java).apply {
                putExtra("pacienteId", paciente.id)
                putExtra("nombre", paciente.nombre)
                putExtra("apellidos", paciente.apellidos)
                putExtra("email", paciente.email)
                putExtra("diagnostico", paciente.diagnostico)
            }
            startActivity(intent)
        }
        rvPacientes.adapter = adaptador

        // Cargar pacientes desde Firestore
        cargarPacientes()

        // BÚSQUEDA (OPCIONAL)
        val etBuscar = findViewById<EditText>(R.id.etBuscar)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarPacientes(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // BOTONES DE NAVEGACIÓN INFERIOR (ORIGINALES)
        val btnPerfil  = findViewById<ImageButton>(R.id.btnPerfil)
        val btnAdd     = findViewById<ImageButton>(R.id.btnAddPaciente)
        val btnConfig  = findViewById<ImageButton>(R.id.btnConfig)

        btnPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilFisioActivity::class.java))
        }

        btnConfig.setOnClickListener {
            Toast.makeText(this, getString(R.string.msg_config), Toast.LENGTH_SHORT).show()
        }

        // Abre AddPacienteActivity y espera resultado con el launcher
        btnAdd.setOnClickListener {
            val intent = Intent(this, AddPacienteActivity::class.java)
            addPacienteLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar al volver (por si se añadió un paciente)
        cargarPacientes()
    }

    private fun cargarPacientes() {
        val fisioId = auth.currentUser?.uid

        // Si no hay fisio autenticado, cargar todos (modo de prueba)
        if (fisioId == null) {
            cargarTodosPacientes()
            return
        }

        // Cargar solo los pacientes del fisio autenticado
        db.collection("pacientes")
            .whereEqualTo("fisioterapeutaId", fisioId)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Toast.makeText(this, "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                listaPacientes.clear()

                for (doc in snapshots!!) {
                    val paciente = Paciente(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        diagnostico = doc.getString("diagnostico") ?: "",
                        fisioterapeutaId = doc.getString("fisioterapeutaId") ?: "",
                        userId = doc.getString("userId"),
                        estadoCuenta = doc.getString("estadoCuenta") ?: "pendiente"
                    )
                    listaPacientes.add(paciente)
                }

                filtrarPacientes("")
            }
    }

    private fun cargarTodosPacientes() {
        // Modo de prueba: cargar todos los pacientes
        db.collection("pacientes")
            .addSnapshotListener { snapshots, error ->

                if (error != null) return@addSnapshotListener

                listaPacientes.clear()

                for (doc in snapshots!!) {
                    val paciente = Paciente(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        diagnostico = doc.getString("diagnostico") ?: "",
                        fisioterapeutaId = doc.getString("fisioterapeutaId") ?: "",
                        userId = doc.getString("userId"),
                        estadoCuenta = doc.getString("estadoCuenta") ?: "pendiente"
                    )
                    listaPacientes.add(paciente)
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