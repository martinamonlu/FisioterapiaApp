package com.example.fisioterapiaapp.paciente.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fisioterapiaapp.R
import com.example.fisioterapiaapp.databinding.FragmentDashboardPacienteBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan
import com.example.fisioterapiaapp.paciente.ui.registro.RegistroSesionActivity
import com.example.fisioterapiaapp.paciente.util.diaDeHoy
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.DashboardPacienteViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardPacienteBinding? = null
    private val binding get() = _binding!!
    private val vm: DashboardPacienteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardPacienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvDiaHoy.text = "Hoy: ${diaDeHoy()}"
        observarDatos()
        vm.cargarDashboard()
    }

    private fun observarDatos() {
        vm.paciente.observe(viewLifecycleOwner) { paciente ->
            paciente?.let {
                binding.tvBienvenida.text = "Hola, ${it.nombre} 👋"
            }
        }

        vm.plan.observe(viewLifecycleOwner) { plan ->
            if (plan == null) {
                binding.tvSemanaInfo.text = "Sin plan activo"
                binding.tvFase.text = ""
                binding.btnRegistrarSesion.isEnabled = false
            } else {
                binding.tvSemanaInfo.text = "Semana 1 / ${plan.duracionSemanas}"
                binding.tvFase.text = "Fase: ${plan.fase}"
                binding.btnRegistrarSesion.isEnabled = true
                binding.btnRegistrarSesion.setOnClickListener {
                    val intent = Intent(requireContext(), RegistroSesionActivity::class.java).apply {
                        putExtra(RegistroSesionActivity.EXTRA_PLAN_ID, plan.id)
                        putExtra(RegistroSesionActivity.EXTRA_DIA, diaDeHoy())
                    }
                    startActivity(intent)
                }
            }
        }

        vm.ejerciciosHoy.observe(viewLifecycleOwner) { lista ->
            binding.progressEjercicios.gone()
            binding.containerEjerciciosHoy.removeAllViews()
            if (lista.isEmpty()) {
                binding.tvSinEjercicios.visible()
            } else {
                binding.tvSinEjercicios.gone()
                mostrarEjerciciosConCheckbox(lista)
            }
        }

        vm.alertas.observe(viewLifecycleOwner) { alertas ->
            val pendientes = alertas.filter { !it.leida }
            if (pendientes.isNotEmpty()) {
                binding.cardAlerta.visible()
                binding.tvAlerta.text = pendientes.first().descripcion
                binding.btnCerrarAlerta.setOnClickListener { binding.cardAlerta.gone() }
            } else {
                binding.cardAlerta.gone()
            }
        }

        vm.adherencia.observe(viewLifecycleOwner) { pct ->
            binding.tvAdherencia.text = "Adherencia: ${"%.0f".format(pct)}%"
            binding.progressAdherencia.progress = pct.toInt()
        }

        vm.registroHoy.observe(viewLifecycleOwner) { registro ->
            if (registro?.sesionCompletada == true) {
                binding.btnRegistrarSesion.text = "✓ Sesión registrada"
                binding.btnRegistrarSesion.isEnabled = false
            }
        }
    }

    private fun mostrarEjerciciosConCheckbox(ejercicios: List<EjercicioPlan>) {
        val inflater = LayoutInflater.from(requireContext())
        ejercicios.forEach { ej ->
            val itemView = inflater.inflate(
                R.layout.item_ejercicio_hoy,
                binding.containerEjerciciosHoy,
                false
            )
            itemView.findViewById<TextView>(R.id.tvNombreEjercicioHoy).text =
                ej.nombreEjercicio.ifBlank { ej.tipo }
            itemView.findViewById<TextView>(R.id.tvDetalleEjercicioHoy).text =
                "${ej.series} series × ${ej.repeticiones} reps · ${ej.cargaTexto}"
            // El checkbox es visual; el registro real se hace en RegistroSesionActivity
            itemView.findViewById<CheckBox>(R.id.checkEjercicio).isClickable = false
            binding.containerEjerciciosHoy.addView(itemView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}