package com.example.fisioterapiaapp.paciente.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fisioterapiaapp.MainActivity
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.databinding.ActivityPacienteMainBinding
import com.google.firebase.auth.FirebaseAuth

class PacienteMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPacienteMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("PACIENTE_MAIN", "onCreate START")

        binding = ActivityPacienteMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        android.util.Log.d("PACIENTE_MAIN", "binding OK")

        if (FirebaseAuth.getInstance().currentUser == null) {
            android.util.Log.d("PACIENTE_MAIN", "sin usuario → login")
            irAlLogin()
            return
        }

        android.util.Log.d("PACIENTE_MAIN", "usuario OK → navegación")
        configurarNavegacion()
        android.util.Log.d("PACIENTE_MAIN", "onCreate END")
    }

    private fun configurarNavegacion() {
        android.util.Log.d("PACIENTE_MAIN", "configurarNavegacion START")
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_paciente) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavPaciente.setupWithNavController(navController)
        android.util.Log.d("PACIENTE_MAIN", "configurarNavegacion END")
    }

    private fun irAlLogin() {
        FirebaseAuth.getInstance().signOut()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}