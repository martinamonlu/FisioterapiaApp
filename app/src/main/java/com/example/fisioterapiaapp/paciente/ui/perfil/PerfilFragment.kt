package com.example.fisioterapiaapp.paciente.ui.perfil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisioterapiaapp.MainActivity
import com.example.fisioterapiaapp.databinding.FragmentPerfilBinding
import com.example.fisioterapiaapp.paciente.model.UiState
import com.example.fisioterapiaapp.paciente.util.SessionManager
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.DashboardPacienteViewModel
import com.example.fisioterapiaapp.paciente.viewmodel.InformesViewModel

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val vmPaciente: DashboardPacienteViewModel by viewModels()
    private val vmInformes: InformesViewModel by viewModels()
    private lateinit var informesAdapter: InformesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarInformesAdapter()
        observarDatos()
        vmInformes.cargar()
        vmPaciente.cargarDashboard() // recargar adherencia

        binding.btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres salir?")
                .setPositiveButton("Salir") { _, _ ->
                    SessionManager(requireContext()).cerrarSesion()
                    startActivity(
                        Intent(requireContext(), MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun configurarInformesAdapter() {
        informesAdapter = InformesAdapter { informe ->
            if (informe.urlPdf.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(informe.urlPdf)))
            }
        }
        binding.rvInformes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = informesAdapter
        }
    }

    private fun observarDatos() {
        vmPaciente.paciente.observe(viewLifecycleOwner) { paciente ->
            paciente?.let {
                binding.tvNombre.text = "${it.nombre} ${it.apellidos}"
                binding.tvEmail.text = it.email
                binding.tvDeporte.text =
                    "Deporte: ${if (it.deporte.isBlank()) "No especificado" else it.deporte}"
                binding.tvLesion.text =
                    "Diagnóstico: ${if (it.diagnostico.isBlank()) "No especificado" else it.diagnostico}"
            }
        }

        vmPaciente.adherencia.observe(viewLifecycleOwner) { pct ->
            binding.tvAdherenciaTotal.text = "Adherencia total: ${"%.0f".format(pct)}%"
        }

        vmInformes.informes.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressInformes.visible()
                    binding.rvInformes.gone()
                    binding.tvSinInformes.gone()
                }
                is UiState.Success -> {
                    binding.progressInformes.gone()
                    if (state.data.isEmpty()) {
                        binding.rvInformes.gone()
                        binding.tvSinInformes.visible()
                        binding.tvSinInformes.text = "Todavía no tienes informes generados"
                    } else {
                        binding.rvInformes.visible()
                        binding.tvSinInformes.gone()
                        informesAdapter.submitList(state.data)
                    }
                }
                is UiState.Error -> {
                    binding.progressInformes.gone()
                    binding.tvSinInformes.visible()
                    binding.tvSinInformes.text = state.mensaje
                }
                else -> binding.progressInformes.gone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
