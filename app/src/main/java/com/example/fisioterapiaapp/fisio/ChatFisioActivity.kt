package com.example.fisioterapiaapp.fisio

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.paciente.model.MensajeChat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.android.material.textfield.TextInputEditText

class ChatFisioActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvMensajes: RecyclerView
    private lateinit var etMensaje: TextInputEditText
    private lateinit var tvSinMensajes: TextView
    private lateinit var tvCuentaPendiente: TextView
    private lateinit var adapter: ChatFisioAdapter

    private var convId = ""
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_fisio)

        val pacienteId     = intent.getStringExtra("pacienteId") ?: ""
        val nombrePaciente = intent.getStringExtra("nombrePaciente") ?: "Paciente"

        // Cabecera
        findViewById<TextView>(R.id.tvNombreChatFisio).text = nombrePaciente
        findViewById<ImageButton>(R.id.btnBackChat).setOnClickListener { finish() }

        // Referencias UI
        rvMensajes       = findViewById(R.id.rvMensajesFisio)
        etMensaje        = findViewById(R.id.etMensajeFisio)
        tvSinMensajes    = findViewById(R.id.tvSinMensajesFisio)
        tvCuentaPendiente = findViewById(R.id.tvCuentaPendiente)

        // RecyclerView
        adapter = ChatFisioAdapter()
        rvMensajes.layoutManager = LinearLayoutManager(this).also { it.stackFromEnd = true }
        rvMensajes.adapter = adapter

        // Botón enviar
        findViewById<ImageButton>(R.id.btnEnviarFisio).setOnClickListener {
            val texto = etMensaje.text?.toString()?.trim() ?: ""
            if (texto.isEmpty()) return@setOnClickListener
            enviarMensaje(texto)
            etMensaje.setText("")
        }

        if (pacienteId.isNotEmpty()) {
            iniciarChat(pacienteId)
        }
    }

    private fun iniciarChat(pacienteId: String) {
        val fisioUid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el userId del paciente (Auth UID) desde su documento
        db.collection("pacientes").document(pacienteId).get()
            .addOnSuccessListener { doc ->
                val pacienteUserId = doc.getString("userId")

                if (pacienteUserId == null) {
                    // Cuenta pendiente: el paciente nunca ha iniciado sesión
                    tvCuentaPendiente.visibility = View.VISIBLE
                    rvMensajes.visibility        = View.GONE
                    findViewById<View>(R.id.layoutInputFisio).visibility = View.GONE
                    return@addOnSuccessListener
                }

                // ID de conversación = {pacienteAuthUid}_{fisioUid}
                convId = "${pacienteUserId}_${fisioUid}"
                observarMensajes()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar el paciente", Toast.LENGTH_SHORT).show()
            }
    }

    private fun observarMensajes() {
        listener = db.collection("conversaciones")
            .document(convId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("CHAT_FISIO", "Error: ${err.message}")
                    return@addSnapshotListener
                }

                val mensajes = snap?.documents
                    ?.mapNotNull { it.toObject(MensajeChat::class.java)?.copy(id = it.id) }
                    ?: emptyList()

                adapter.submitList(mensajes) {
                    if (mensajes.isNotEmpty()) {
                        rvMensajes.scrollToPosition(mensajes.size - 1)
                    }
                }

                tvSinMensajes.visibility = if (mensajes.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun enviarMensaje(texto: String) {
        if (convId.isEmpty()) return
        val fisioUid = auth.currentUser?.uid ?: return

        val msg = hashMapOf(
            "texto"     to texto,
            "emisorId"  to fisioUid,
            "esPaciente" to false,
            "timestamp" to Timestamp.now()
        )

        db.collection("conversaciones")
            .document(convId)
            .collection("mensajes")
            .add(msg)
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
