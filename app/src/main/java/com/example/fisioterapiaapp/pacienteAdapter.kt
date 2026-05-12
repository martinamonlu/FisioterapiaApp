package com.example.fisioterapiaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.example.fisioterapiaapp.paciente.model.Paciente

// ADAPTER PROFESIONAL PARA LISTA DE PACIENTES
class PacienteAdapter(
    private val listaPacientes: List<Paciente>,
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    class PacienteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardPaciente: MaterialCardView = view.findViewById(R.id.cardPaciente)
        val tvIniciales: TextView = view.findViewById(R.id.tvIniciales)
        val tvNombrePaciente: TextView = view.findViewById(R.id.tvNombrePaciente)
        val tvDiagnostico: TextView = view.findViewById(R.id.tvDiagnostico)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvUltimaSesion: TextView = view.findViewById(R.id.tvUltimaSesion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        val paciente = listaPacientes[position]

        // Nombre completo (usando la propiedad del modelo)
        holder.tvNombrePaciente.text = paciente.nombreCompleto

        // Iniciales (primera letra de nombre y apellido)
        val iniciales = generarIniciales(paciente.nombre, paciente.apellidos)
        holder.tvIniciales.text = iniciales

        // Diagnóstico
        holder.tvDiagnostico.text = paciente.diagnostico.ifEmpty { "Sin diagnóstico" }

        // Estado basado en cuenta activa
        if (paciente.cuentaActivada) {
            holder.tvEstado.text = "Plan activo"
        } else {
            holder.tvEstado.text = "Pendiente de activación"
        }

        // Última sesión (por ahora valor por defecto)
        holder.tvUltimaSesion.text = "Hoy"

        // Click en la card completa
        holder.cardPaciente.setOnClickListener {
            onPacienteClick(paciente)
        }
    }

    override fun getItemCount(): Int = listaPacientes.size

    // Función para generar iniciales
    private fun generarIniciales(nombre: String, apellidos: String): String {
        val inicial1 = nombre.trim().firstOrNull()?.uppercaseChar() ?: ""
        val inicial2 = apellidos.trim().firstOrNull()?.uppercaseChar() ?: ""
        return "$inicial1$inicial2"
    }
}