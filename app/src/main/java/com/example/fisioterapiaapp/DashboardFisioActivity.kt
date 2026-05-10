package com.example.fisioterapiaapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// DASHBOARD DEL FISIOTERAPEUTA
// Pantalla principal tras iniciar sesión
// Muestra la lista de pacientes y botones de acción en la parte inferior
class DashboardFisioActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // Lista mutable de pacientes (datos de la base de datos Firebase)
    private val listaPacientes = mutableListOf<Paciente>()

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
        setContentView(R.layout.activity_dashboard_fisio)

        // CONFIGURACIÓN DEL RECYCLERVIEW
        val rvPacientes = findViewById<RecyclerView>(R.id.rvPacientes)
        rvPacientes.layoutManager = LinearLayoutManager(this)
        adaptador = PacienteAdapter(listaPacientes)
        rvPacientes.adapter = adaptador

        cargarPacientes()

        // BOTONES DE NAVEGACIÓN INFERIOR
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

    private fun cargarPacientes() {

        val uid = com.google.firebase.auth.FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid ?: return

        db.collection("pacientes")
            .whereEqualTo("fisioterapeutaId", uid)
            .addSnapshotListener { snapshots, error ->

                if (error != null) {
                    Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                listaPacientes.clear()

                for (doc in snapshots!!) {

                    Toast.makeText(this, doc.getString("nombre"), Toast.LENGTH_SHORT).show()

                    val nombre = doc.getString("nombre") ?: ""
                    val apellidos = doc.getString("apellidos") ?: ""

                    listaPacientes.add(Paciente("$nombre $apellidos"))
                }

                adaptador.notifyDataSetChanged()
            }
    }

}