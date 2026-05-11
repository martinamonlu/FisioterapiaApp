package com.example.fisioterapiaapp.paciente.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.databinding.ItemEjercicioDiaBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan

class EjerciciosDiaAdapter(
    private val onEjercicioClick: (EjercicioPlan) -> Unit
) : ListAdapter<EjercicioPlan, EjerciciosDiaAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<EjercicioPlan>() {
            override fun areItemsTheSame(a: EjercicioPlan, b: EjercicioPlan) =
                a.tipo == b.tipo && a.nombreEjercicio == b.nombreEjercicio
            override fun areContentsTheSame(a: EjercicioPlan, b: EjercicioPlan) = a == b
        }
    }

    inner class VH(private val binding: ItemEjercicioDiaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ejercicio: EjercicioPlan) {
            binding.tvNombreEjercicio.text =
                ejercicio.nombreEjercicio.ifBlank { ejercicio.tipo }
            binding.tvTipoEjercicio.text = ejercicio.tipo
            binding.tvSeriesReps.text = "${ejercicio.series} × ${ejercicio.repeticiones}"
            binding.tvCarga.text = ejercicio.cargaTexto
            binding.btnVerVideo.visibility =
                if (!ejercicio.videoUrl.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onEjercicioClick(ejercicio) }
            binding.btnVerVideo.setOnClickListener { onEjercicioClick(ejercicio) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemEjercicioDiaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
