package com.example.fisioterapiaapp.paciente.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fisioterapiaapp.databinding.ActivityCambiarPasswordBinding
import com.example.fisioterapiaapp.paciente.ui.dashboard.PacienteMainActivity
import com.example.fisioterapiaapp.paciente.viewmodel.AuthPacienteViewModel

/**
 * Cambio de contraseña obligatorio en el primer login.
 * Recibe del SignInPacienteActivity:
 *   - pacienteId: documento Firestore
 *   - passwordActual: contraseña temporal con la que entró
 */
class CambiarPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCambiarPasswordBinding
    private val vm: AuthPacienteViewModel by viewModels()

    private var pacienteId: String = ""
    private var passwordActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCambiarPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pacienteId     = intent.getStringExtra("pacienteId") ?: ""
        passwordActual = intent.getStringExtra("passwordActual") ?: ""

        // Impedir volver atrás sin cambiar la contraseña
        onBackPressedDispatcher.addCallback(this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(
                        this@CambiarPasswordActivity,
                        "Debes cambiar tu contraseña para continuar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Pre-rellenar el campo de contraseña actual con la temporal
        binding.etActual.setText(passwordActual)

        binding.btnCambiar.setOnClickListener {
            val actual    = binding.etActual.text.toString()
            val nueva     = binding.etNueva.text.toString()
            val confirmar = binding.etConfirmar.text.toString()

            when {
                actual.isEmpty() -> {
                    binding.etActual.error = "Obligatorio"
                }
                nueva.length < 8 -> {
                    binding.etNueva.error = "Mínimo 8 caracteres"
                }
                !nueva.any { it.isDigit() } || !nueva.any { it.isLetter() } -> {
                    binding.etNueva.error = "Debe tener letras y números"
                }
                nueva != confirmar -> {
                    binding.etConfirmar.error = "No coincide"
                }
                else -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCambiar.isEnabled = false
                    vm.cambiarPassword(actual, nueva, pacienteId)
                }
            }
        }

        vm.cambioPasswordOk.observe(this) { ok ->
            if (ok == true) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, PacienteMainActivity::class.java))
                finishAffinity()
            }
        }

        vm.errorCambioPassword.observe(this) { err ->
            if (err != null) {
                binding.progressBar.visibility = View.GONE
                binding.btnCambiar.isEnabled = true
                Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            }
        }
    }
}
