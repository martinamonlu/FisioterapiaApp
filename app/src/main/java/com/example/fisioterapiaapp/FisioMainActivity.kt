package com.example.fisioterapiaapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fisioterapiaapp.fisio.FisioPacientesFragment
import com.example.fisioterapiaapp.fisio.FisioPerfilFragment
import com.example.fisioterapiaapp.fisio.FisioConfigFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.fisioterapiaapp.fisio.FisioAddPacienteFragment

class FisioMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fisio_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavFisio)

        // Cargar fragment inicial
        cargarFragment(FisioPacientesFragment())
        bottomNav.selectedItemId = R.id.nav_pacientes

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pacientes -> {
                    cargarFragment(FisioPacientesFragment())
                    true
                }
                R.id.nav_añadir -> {
                    cargarFragment(FisioAddPacienteFragment())
                    true
                }
                R.id.nav_perfil -> {
                    cargarFragment(FisioPerfilFragment())
                    true
                }
                R.id.nav_config -> {
                    cargarFragment(FisioConfigFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fisioFragmentContainer, fragment)
            .commit()
    }
}