package com.example.fisioterapiaapp.paciente.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.paciente.ui.dashboard.PacienteMainActivity
import com.example.fisioterapiaapp.paciente.util.SessionManager
import com.example.fisioterapiaapp.paciente.viewmodel.AuthPacienteViewModel
import com.example.fisioterapiaapp.paciente.viewmodel.LoginResultado

/**
 * Login del paciente (MVVM).
 * Reutiliza el layout `sign_in_paciente.xml` ya existente (con email) y
 * delega toda la lógica en `AuthPacienteViewModel`, alineado con el flujo
 * de activación creado por el fisio (estadoCuenta + primerLogin).
 */
class SignInPacienteActivity : AppCompatActivity() {

    private val vm: AuthPacienteViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnEntrar: Button
    private lateinit var btnBack: ImageButton
    private lateinit var sessionManager: SessionManager

    private var passwordIntroducida: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_in_paciente)

        sessionManager = SessionManager(this)

        etEmail    = findViewById(R.id.etEmailPaciente)
        etPassword = findViewById(R.id.etPasswordPaciente)
        btnEntrar  = findViewById(R.id.btnEntrarPaciente)
        btnBack    = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnEntrar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            when {
                email.isEmpty() -> etEmail.error = "Email obligatorio"
                password.isEmpty() -> etPassword.error = "Contraseña obligatoria"
                else -> {
                    passwordIntroducida = password
                    btnEntrar.isEnabled = false
                    vm.login(email, password)
                }
            }
        }

        vm.estado.observe(this) { estado ->
            when (estado) {
                is LoginResultado.Cargando -> {
                    btnEntrar.isEnabled = false
                    btnEntrar.text = "Entrando..."
                }
                is LoginResultado.ActivarCuenta -> {
                    // Cuenta pendiente: crear cuenta en Auth con la pass introducida
                    Toast.makeText(
                        this,
                        "Activando tu cuenta...",
                        Toast.LENGTH_SHORT
                    ).show()
                    vm.activarCuenta(estado.pacienteId, estado.email, passwordIntroducida)
                }
                is LoginResultado.PrimerLogin -> {
                    btnEntrar.isEnabled = true
                    btnEntrar.text = getString(R.string.btn_entrar)
                    sessionManager.guardarSesion(estado.pacienteId)
                    val i = Intent(this, CambiarPasswordActivity::class.java).apply {
                        putExtra("pacienteId", estado.pacienteId)
                        putExtra("passwordActual", passwordIntroducida)
                    }
                    startActivity(i)
                    finish()
                }
                is LoginResultado.Ok -> {
                    btnEntrar.isEnabled = true
                    btnEntrar.text = getString(R.string.btn_entrar)
                    sessionManager.guardarSesion(estado.pacienteId)  // ← puede estar fallando
                    startActivity(Intent(this, PacienteMainActivity::class.java))
                    finish()
                }
                is LoginResultado.Error -> {
                    btnEntrar.isEnabled = true
                    btnEntrar.text = getString(R.string.btn_entrar)
                    Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
