package com.example.fisioterapiaapp.fisio

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.fisioterapiaapp.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FisioConfigFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var switchPush: SwitchCompat
    private lateinit var switchEmail: SwitchCompat
    private lateinit var seekBarUmbral: SeekBar
    private lateinit var tvUmbralValor: TextView
    private lateinit var spinnerIdioma: Spinner
    private lateinit var rowPrivacidad: LinearLayout
    private lateinit var layoutPrivacidadContenido: LinearLayout
    private lateinit var rowCambiarEmail: LinearLayout
    private lateinit var rowCambiarPassword: LinearLayout
    private lateinit var tvEmailActual: TextView
    private lateinit var btnGuardar: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_fisio_config_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchPush              = view.findViewById(R.id.switchNotifPush)
        switchEmail             = view.findViewById(R.id.switchNotifEmail)
        seekBarUmbral           = view.findViewById(R.id.seekBarUmbral)
        tvUmbralValor           = view.findViewById(R.id.tvUmbralValor)
        spinnerIdioma           = view.findViewById(R.id.spinnerIdioma)
        rowPrivacidad           = view.findViewById(R.id.rowPrivacidad)
        layoutPrivacidadContenido = view.findViewById(R.id.layoutPrivacidadContenido)
        rowCambiarEmail         = view.findViewById(R.id.rowCambiarEmail)
        rowCambiarPassword      = view.findViewById(R.id.rowCambiarPassword)
        tvEmailActual           = view.findViewById(R.id.tvEmailActual)
        btnGuardar              = view.findViewById(R.id.btnGuardarConfig)

        configurarSpinnerIdioma()
        cargarConfiguracion()

        seekBarUmbral.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                tvUmbralValor.text = "$progress%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        rowPrivacidad.setOnClickListener {
            val visible = layoutPrivacidadContenido.visibility == View.VISIBLE
            layoutPrivacidadContenido.visibility = if (visible) View.GONE else View.VISIBLE
        }

        rowCambiarEmail.setOnClickListener { mostrarDialogoCambiarEmail() }
        rowCambiarPassword.setOnClickListener { mostrarDialogoCambiarPassword() }
        btnGuardar.setOnClickListener { guardarConfiguracion() }
    }

    // ── Spinner de idioma ──────────────────────────────────────────────────────

    private val idiomas = listOf("Español", "English")
    private val codigosIdioma = listOf("es", "en")

    private fun configurarSpinnerIdioma() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            idiomas
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIdioma.adapter = adapter

        val prefs = requireContext().getSharedPreferences("rehapp_prefs", Context.MODE_PRIVATE)
        val idiomaGuardado = prefs.getString("idioma", "es") ?: "es"
        val idx = codigosIdioma.indexOf(idiomaGuardado).coerceAtLeast(0)
        spinnerIdioma.setSelection(idx)
    }

    // ── Cargar configuración desde Firestore ───────────────────────────────────

    private fun cargarConfiguracion() {
        val uid = auth.currentUser?.uid ?: return

        tvEmailActual.text = auth.currentUser?.email ?: ""

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                switchPush.isChecked  = doc.getBoolean("notifPush")  ?: true
                switchEmail.isChecked = doc.getBoolean("notifEmail") ?: false
                val umbral = (doc.getLong("umbralAdherencia") ?: 60L).toInt()
                seekBarUmbral.progress = umbral
                tvUmbralValor.text = "$umbral%"
            }
    }

    // ── Guardar configuración en Firestore ────────────────────────────────────

    private fun guardarConfiguracion() {
        val uid = auth.currentUser?.uid ?: return

        val idiomaSeleccionado = codigosIdioma[spinnerIdioma.selectedItemPosition]

        val prefs = requireContext().getSharedPreferences("rehapp_prefs", Context.MODE_PRIVATE)
        val idiomaAnterior = prefs.getString("idioma", "es")
        prefs.edit().putString("idioma", idiomaSeleccionado).apply()

        db.collection("usuarios").document(uid)
            .update(
                mapOf(
                    "notifPush"         to switchPush.isChecked,
                    "notifEmail"        to switchEmail.isChecked,
                    "umbralAdherencia"  to seekBarUmbral.progress
                )
            )
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()

                if (idiomaSeleccionado != idiomaAnterior) {
                    Toast.makeText(
                        requireContext(),
                        "Reinicia la aplicación para aplicar el nuevo idioma",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ── Diálogo: cambiar email ─────────────────────────────────────────────────

    private fun mostrarDialogoCambiarEmail() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cambiar_credencial, null)
        val tvTitulo    = dialogView.findViewById<TextView>(R.id.tvTituloCredencial)
        val etCampo1    = dialogView.findViewById<EditText>(R.id.etCampo1)
        val etCampo2    = dialogView.findViewById<EditText>(R.id.etCampo2)
        val etPassword  = dialogView.findViewById<EditText>(R.id.etPasswordConfirm)
        val tvLabel1    = dialogView.findViewById<TextView>(R.id.tvLabel1)
        val tvLabel2    = dialogView.findViewById<TextView>(R.id.tvLabel2)

        tvTitulo.text  = "Cambiar email"
        tvLabel1.text  = "Nuevo email"
        tvLabel2.text  = "Confirmar nuevo email"
        etCampo1.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT
        etCampo2.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cambiar") { _, _ ->
                val nuevoEmail    = etCampo1.text.toString().trim()
                val confirmar     = etCampo2.text.toString().trim()
                val passwordActual = etPassword.text.toString()

                when {
                    nuevoEmail.isEmpty() || confirmar.isEmpty() || passwordActual.isEmpty() ->
                        Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                    nuevoEmail != confirmar ->
                        Toast.makeText(requireContext(), "Los emails no coinciden", Toast.LENGTH_SHORT).show()
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(nuevoEmail).matches() ->
                        Toast.makeText(requireContext(), "Email no válido", Toast.LENGTH_SHORT).show()
                    else -> cambiarEmail(nuevoEmail, passwordActual)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEmail(nuevoEmail: String, passwordActual: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val cred = EmailAuthProvider.getCredential(email, passwordActual)
        user.reauthenticate(cred)
            .addOnSuccessListener {
                user.updateEmail(nuevoEmail)
                    .addOnSuccessListener {
                        tvEmailActual.text = nuevoEmail
                        db.collection("usuarios").document(user.uid)
                            .update("email", nuevoEmail)
                        Toast.makeText(requireContext(), "Email actualizado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Diálogo: cambiar contraseña ────────────────────────────────────────────

    private fun mostrarDialogoCambiarPassword() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cambiar_credencial, null)
        val tvTitulo    = dialogView.findViewById<TextView>(R.id.tvTituloCredencial)
        val etCampo1    = dialogView.findViewById<EditText>(R.id.etCampo1)
        val etCampo2    = dialogView.findViewById<EditText>(R.id.etCampo2)
        val etPassword  = dialogView.findViewById<EditText>(R.id.etPasswordConfirm)
        val tvLabel1    = dialogView.findViewById<TextView>(R.id.tvLabel1)
        val tvLabel2    = dialogView.findViewById<TextView>(R.id.tvLabel2)

        tvTitulo.text = "Cambiar contraseña"
        tvLabel1.text = "Nueva contraseña"
        tvLabel2.text = "Confirmar nueva contraseña"
        etCampo1.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        etCampo2.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cambiar") { _, _ ->
                val nueva     = etCampo1.text.toString()
                val confirmar = etCampo2.text.toString()
                val actual    = etPassword.text.toString()

                when {
                    nueva.isEmpty() || confirmar.isEmpty() || actual.isEmpty() ->
                        Toast.makeText(requireContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                    nueva.length < 8 ->
                        Toast.makeText(requireContext(), "Mínimo 8 caracteres", Toast.LENGTH_SHORT).show()
                    !nueva.any { it.isDigit() } || !nueva.any { it.isLetter() } ->
                        Toast.makeText(requireContext(), "Debe tener letras y números", Toast.LENGTH_SHORT).show()
                    nueva != confirmar ->
                        Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    else -> cambiarPassword(actual, nueva)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarPassword(passwordActual: String, passwordNueva: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        val cred = EmailAuthProvider.getCredential(email, passwordActual)
        user.reauthenticate(cred)
            .addOnSuccessListener {
                user.updatePassword(passwordNueva)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
            }
    }
}
