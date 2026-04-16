// Código es el encargado de decir "pon el nombre A en el botón 1"

package com.example.fisioterapiaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// CLASE PACIENTE
// Define la estructura de datos de cada paciente (en este caso, solo el nombre)
data class Paciente(val nombre: String)

// ADAPTADOR DEL RECYCLERVIEW
// Se encarga de enlazar los datos con el diseño de cada elemento (item_paciente.xml)
class PacienteAdapter(private val listaPacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    // VIEWHOLDER : representa cada elemento individual de la lista
    class PacienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombrePaciente)
    }

    // CREACIÓN DE CADA ITEM (cuando RecyclerView necesita uno nuevo)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    // ASIGNACIÓN DE DATOS: conecta cada paciente con su vista correspondiente
    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.tvNombre.text = listaPacientes[position].nombre
    }

    // NÚMERO TOTAL DE ELEMENTOS: indica cuántos pacientes hay en la lista
    override fun getItemCount(): Int = listaPacientes.size

}