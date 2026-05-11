package com.example.fisioterapiaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PacienteAdapter(
    private val listaPacientes: List<Paciente>,
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    class PacienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePaciente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        val paciente = listaPacientes[position]
        holder.tvNombre.text = "${paciente.nombre} ${paciente.apellidos}"
        holder.itemView.setOnClickListener {
            onPacienteClick(paciente)
        }
    }

    override fun getItemCount(): Int = listaPacientes.size
}