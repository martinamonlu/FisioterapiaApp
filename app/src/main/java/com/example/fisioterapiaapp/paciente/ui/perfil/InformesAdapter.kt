package com.example.fisioterapiaapp.paciente.ui.perfil

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.databinding.ItemInformeBinding
import com.example.fisioterapiaapp.paciente.model.Informe
import com.example.fisioterapiaapp.paciente.util.toFormattedDate

class InformesAdapter(
    private val onInformeClick: (Informe) -> Unit
) : ListAdapter<Informe, InformesAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Informe>() {
            override fun areItemsTheSame(a: Informe, b: Informe) = a.id == b.id
            override fun areContentsTheSame(a: Informe, b: Informe) = a == b
        }
    }

    inner class VH(private val binding: ItemInformeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(informe: Informe) {
            binding.tvTituloInforme.text = informe.titulo
            binding.tvFechaInforme.text = informe.fechaGeneracion.toFormattedDate()
            if (informe.comentarioFisio.isNotEmpty()) {
                binding.tvComentarioFisio.visibility = android.view.View.VISIBLE
                binding.tvComentarioFisio.text = informe.comentarioFisio
            } else {
                binding.tvComentarioFisio.visibility = android.view.View.GONE
            }
            binding.btnVerPdf.setOnClickListener { onInformeClick(informe) }
            binding.btnVerPdf.isEnabled = informe.urlPdf.isNotEmpty()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemInformeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
