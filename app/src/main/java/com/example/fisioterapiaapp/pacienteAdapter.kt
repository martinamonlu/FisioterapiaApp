// Este código es el encargado de decir "pon el nombre A en el botón 1".

package com.example.fisioterapiaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Clase para definir qué datos tiene un paciente
data class Paciente(val nombre: String)

class PacienteAdapter(private val listaPacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    class PacienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePaciente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.tvNombre.text = listaPacientes[position].nombre
    }

    override fun getItemCount(): Int = listaPacientes.size
}