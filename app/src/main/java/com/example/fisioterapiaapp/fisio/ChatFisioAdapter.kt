package com.example.fisioterapiaapp.fisio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fisioterapiaapp.databinding.ItemMensajeFisioBinding
import com.example.fisioterapiaapp.databinding.ItemMensajePacienteBinding
import com.example.fisioterapiaapp.paciente.model.MensajeChat
import com.example.fisioterapiaapp.paciente.util.toFormattedDate

/**
 * Adapter del chat desde la perspectiva del fisio.
 * Los mensajes propios (esPaciente=false) aparecen a la DERECHA con burbuja
 * coloreada, y los del paciente (esPaciente=true) aparecen a la IZQUIERDA.
 * Es el inverso del ChatAdapter del paciente.
 */
class ChatFisioAdapter : ListAdapter<MensajeChat, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_SALIENTE  = 0  // mensaje del fisio  → derecha
        private const val TYPE_ENTRANTE  = 1  // mensaje del paciente → izquierda

        val DIFF = object : DiffUtil.ItemCallback<MensajeChat>() {
            override fun areItemsTheSame(a: MensajeChat, b: MensajeChat) = a.id == b.id
            override fun areContentsTheSame(a: MensajeChat, b: MensajeChat) = a == b
        }
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position).esPaciente) TYPE_ENTRANTE else TYPE_SALIENTE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_SALIENTE) {
            SalienteVH(
                ItemMensajePacienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            EntranteVH(
                ItemMensajeFisioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is SalienteVH -> holder.bind(msg)
            is EntranteVH -> holder.bind(msg)
        }
    }

    /** Mensaje del fisio — burbuja derecha (reutiliza el estilo de burbuja del paciente) */
    class SalienteVH(private val b: ItemMensajePacienteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MensajeChat) {
            b.tvTextoPaciente.text = msg.texto
            b.tvHoraPaciente.text = msg.timestamp.toFormattedDate("HH:mm")
        }
    }

    /** Mensaje del paciente — burbuja izquierda */
    class EntranteVH(private val b: ItemMensajeFisioBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: MensajeChat) {
            b.tvTextoFisio.text = msg.texto
            b.tvHoraFisio.text = msg.timestamp.toFormattedDate("HH:mm")
        }
    }
}
