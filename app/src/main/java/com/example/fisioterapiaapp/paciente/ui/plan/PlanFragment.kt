package com.example.fisioterapiaapp.paciente.ui.plan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisioterapiaapp.databinding.FragmentPlanBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan
import com.example.fisioterapiaapp.paciente.model.Plan
import com.example.fisioterapiaapp.paciente.ui.exercise.EjercicioDetalleActivity
import com.example.fisioterapiaapp.paciente.util.DIAS_SEMANA
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.PlanViewModel

/**
 * Lista los 7 días de la semana y, dentro de cada uno, los ejercicios que
 * tocan en ese día según `diasSemana` del plan.
 */
class PlanFragment : Fragment() {

    private var _binding: FragmentPlanBinding? = null
    private val binding get() = _binding!!
    private val vm: PlanViewModel by viewModels()
    private lateinit var adapter: DiasEjerciciosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DiasEjerciciosAdapter { ejercicio: EjercicioPlan ->
            val i = Intent(requireContext(), EjercicioDetalleActivity::class.java).apply {
                putExtra(EjercicioDetalleActivity.EXTRA_EJERCICIO, ejercicio)
            }
            startActivity(i)
        }
        binding.rvDias.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PlanFragment.adapter
        }

        binding.btnSemanaAnterior.setOnClickListener { vm.semanaAnterior() }
        binding.btnSemanaSiguiente.setOnClickListener { vm.semanaSiguiente() }

        vm.semanaActual.observe(viewLifecycleOwner) { sem ->
            val dur = vm.plan.value?.duracionSemanas ?: 0
            binding.tvSemana.text = if (dur > 0) "Semana $sem / $dur" else "Semana $sem"
        }

        vm.plan.observe(viewLifecycleOwner) { plan ->
            binding.progressPlan.gone()
            if (plan == null) {
                binding.tvSinPlan.visible()
                binding.rvDias.gone()
            } else {
                binding.tvSinPlan.gone()
                binding.rvDias.visible()
                mostrarSemana(plan)
            }
        }
        vm.cargar()
    }

    private fun mostrarSemana(plan: Plan) {
        val items = DIAS_SEMANA.map { dia ->
            DiaItem(
                dia = dia,
                ejercicios = plan.ejercicios.filter { it.seRealizaEn(dia) }
            )
        }
        adapter.submitList(items)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Estructura inmutable para el RecyclerView de días. */
data class DiaItem(
    val dia: String,
    val ejercicios: List<EjercicioPlan>
)
