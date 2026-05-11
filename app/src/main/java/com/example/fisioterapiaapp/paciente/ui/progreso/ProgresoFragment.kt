package com.example.fisioterapiaapp.paciente.ui.progreso

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fisioterapiaapp.databinding.FragmentProgresoBinding
import com.example.fisioterapiaapp.paciente.model.PuntoProgreso
import com.example.fisioterapiaapp.paciente.model.UiState
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.ProgresoViewModel
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class ProgresoFragment : Fragment() {

    private var _binding: FragmentProgresoBinding? = null
    private val binding get() = _binding!!
    private val vm: ProgresoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgresoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarGraficas()
        vm.cargar()
        observarDatos()
    }

    private fun configurarGraficas() {
        // ── Gráfica 1: EVA (dolor) y Carga de entrenamiento ─
        binding.chartEvaVsCarga.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            legend.apply {
                form = Legend.LegendForm.LINE
                isEnabled = true
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 10f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
        }

        // ── Gráfica 2: Adherencia por semana ────────────────
        binding.chartAdherencia.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawValueAboveBar(true)
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
        }

        // ── Gráfica 3: RPE (esfuerzo percibido) ─────────────
        binding.chartRpe.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            legend.isEnabled = false
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 6f
                axisMaximum = 20f
            }
            axisRight.isEnabled = false
        }
    }

    private fun observarDatos() {
        vm.puntos.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressProgreso.visible()
                    binding.scrollGraficas.gone()
                    binding.tvSinDatos.gone()
                }
                is UiState.Success -> {
                    binding.progressProgreso.gone()
                    if (state.data.isEmpty()) {
                        binding.scrollGraficas.gone()
                        binding.tvSinDatos.visible()
                        binding.tvSinDatos.text =
                            "Aún no hay datos registrados. Completa sesiones para ver tu evolución."
                    } else {
                        binding.scrollGraficas.visible()
                        binding.tvSinDatos.gone()
                        poblarGraficas(state.data)
                    }
                }
                is UiState.Error -> {
                    binding.progressProgreso.gone()
                    binding.tvSinDatos.visible()
                    binding.tvSinDatos.text = state.mensaje
                }
                else -> {
                    binding.progressProgreso.gone()
                }
            }
        }
    }

    private fun poblarGraficas(puntos: List<PuntoProgreso>) {
        val semanas = puntos.map { "S${it.semana}" }

        // ── EVA vs Carga ─────────────────────────────────────
        val entriesEva = puntos.mapIndexed { i, p -> Entry(i.toFloat(), p.eva) }
        val entriesCarga = puntos.mapIndexed { i, p ->
            Entry(i.toFloat(), p.carga / 10f)  // normalizar carga para misma escala
        }

        val dsEva = LineDataSet(entriesEva, "Dolor (EVA)").apply {
            color = Color.parseColor("#E53935")
            setCircleColor(Color.parseColor("#E53935"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val dsCarga = LineDataSet(entriesCarga, "Carga (norm.)").apply {
            color = Color.parseColor("#1E88E5")
            setCircleColor(Color.parseColor("#1E88E5"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            enableDashedLine(10f, 5f, 0f)
        }

        binding.chartEvaVsCarga.apply {
            data = LineData(dsEva, dsCarga)
            xAxis.valueFormatter = IndexAxisValueFormatter(semanas)
            animateX(800)
            invalidate()
        }

        // ── Adherencia ───────────────────────────────────────
        val entriesAdh = puntos.mapIndexed { i, p ->
            BarEntry(i.toFloat(), p.adherencia)
        }
        val dsAdh = BarDataSet(entriesAdh, "Adherencia %").apply {
            val colores = puntos.map { p ->
                when {
                    p.adherencia >= 80 -> Color.parseColor("#43A047")
                    p.adherencia >= 50 -> Color.parseColor("#FB8C00")
                    else               -> Color.parseColor("#E53935")
                }
            }
            colors = colores
            valueTextSize = 9f
        }
        binding.chartAdherencia.apply {
            data = BarData(dsAdh).also { it.barWidth = 0.7f }
            xAxis.valueFormatter = IndexAxisValueFormatter(semanas)
            animateY(800)
            invalidate()
        }

        // ── RPE ──────────────────────────────────────────────
        val entriesRpe = puntos.mapIndexed { i, p -> Entry(i.toFloat(), p.rpe) }
        val dsRpe = LineDataSet(entriesRpe, "Esfuerzo (RPE)").apply {
            color = Color.parseColor("#8E24AA")
            setCircleColor(Color.parseColor("#8E24AA"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.chartRpe.apply {
            data = LineData(dsRpe)
            xAxis.valueFormatter = IndexAxisValueFormatter(semanas)
            animateX(800)
            invalidate()
        }

        // ── Resumen textual ──────────────────────────────────
        val ultimosPuntos = puntos.takeLast(4)
        val adherenciaMedia = ultimosPuntos.map { it.adherencia }.average().toFloat()
        val evaMedia = ultimosPuntos.map { it.eva }.average().toFloat()
        binding.tvResumen.text =
            "Últimas 4 semanas → Adherencia media: ${"%.0f".format(adherenciaMedia)}% | EVA medio: ${"%.1f".format(evaMedia)}/10"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
