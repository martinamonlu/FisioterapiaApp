package com.example.fisioterapiaapp.paciente.ui.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.databinding.ItemDiaEjerciciosBinding
import com.example.fisioterapiaapp.databinding.ItemEjercicioPlanBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan

class DiasEjerciciosAdapter(
    private val onEjercicioClick: (EjercicioPlan) -> Unit
) : ListAdapter<DiaItem, DiasEjerciciosAdapter.DiaVH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DiaItem>() {
            override fun areItemsTheSame(a: DiaItem, b: DiaItem) = a.dia == b.dia
            override fun areContentsTheSame(a: DiaItem, b: DiaItem) = a == b
        }
    }

    inner class DiaVH(private val binding: ItemDiaEjerciciosBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DiaItem) {
            binding.tvTituloDia.text = item.dia
            binding.tvNumEjercicios.text =
                if (item.ejercicios.isEmpty()) "Descanso"
                else "${item.ejercicios.size} ejercicios"

            if (item.ejercicios.isEmpty()) {
                binding.tvDescanso.visibility = View.VISIBLE
                binding.containerEjerciciosDia.removeAllViews()
                binding.containerEjerciciosDia.visibility = View.GONE
            } else {
                binding.tvDescanso.visibility = View.GONE
                binding.containerEjerciciosDia.visibility = View.VISIBLE
                poblarEjercicios(item.ejercicios)
            }
            // Botón "Registrar sesión" del día se controla desde el dashboard;
            // aquí se oculta para no duplicar flujos.
            binding.btnRegistrarDia.visibility = View.GONE
        }

        private fun poblarEjercicios(ejercicios: List<EjercicioPlan>) {
            binding.containerEjerciciosDia.removeAllViews()
            val inflater = LayoutInflater.from(binding.root.context)
            ejercicios.forEach { ej ->
                val b = ItemEjercicioPlanBinding.inflate(
                    inflater, binding.containerEjerciciosDia, false
                )
                b.tvNombreEjercicioPlan.text =
                    ej.nombreEjercicio.ifBlank { ej.tipo }
                b.tvDetalleEjercicioPlan.text =
                    "${ej.series} × ${ej.repeticiones} · ${ej.cargaTexto}"
                b.btnVideoEjercicioPlan.visibility =
                    if (!ej.videoUrl.isNullOrBlank()) View.VISIBLE else View.GONE
                b.root.setOnClickListener { onEjercicioClick(ej) }
                b.btnVideoEjercicioPlan.setOnClickListener { onEjercicioClick(ej) }
                binding.containerEjerciciosDia.addView(b.root)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DiaVH(
        ItemDiaEjerciciosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: DiaVH, position: Int) = holder.bind(getItem(position))
}
