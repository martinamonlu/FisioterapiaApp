package com.example.fisioterapiaapp.paciente.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.databinding.ItemMensajePacienteBinding
import com.example.fisioterapiaapp.databinding.ItemMensajeFisioBinding
import com.example.fisioterapiaapp.paciente.model.MensajeChat
import com.example.fisioterapiaapp.paciente.util.toFormattedDate

class ChatAdapter : ListAdapter<MensajeChat, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val VIEW_TYPE_PACIENTE = 0
        private const val VIEW_TYPE_FISIO    = 1

        val DIFF = object : DiffUtil.ItemCallback<MensajeChat>() {
            override fun areItemsTheSame(a: MensajeChat, b: MensajeChat) = a.id == b.id
            override fun areContentsTheSame(a: MensajeChat, b: MensajeChat) = a == b
        }
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position).esPaciente) VIEW_TYPE_PACIENTE else VIEW_TYPE_FISIO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_PACIENTE) {
            PacienteVH(ItemMensajePacienteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            FisioVH(ItemMensajeFisioBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is PacienteVH -> holder.bind(msg)
            is FisioVH    -> holder.bind(msg)
        }
    }

    class PacienteVH(private val b: ItemMensajePacienteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MensajeChat) {
            b.tvTextoPaciente.text = msg.texto
            b.tvHoraPaciente.text = msg.timestamp.toFormattedDate("HH:mm")
        }
    }

    class FisioVH(private val b: ItemMensajeFisioBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MensajeChat) {
            b.tvTextoFisio.text = msg.texto
            b.tvHoraFisio.text = msg.timestamp.toFormattedDate("HH:mm")
        }
    }
}
