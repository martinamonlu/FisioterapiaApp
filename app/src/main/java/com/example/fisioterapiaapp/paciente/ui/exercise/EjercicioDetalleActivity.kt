package com.example.fisioterapiaapp.paciente.ui.exercise

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.fisioterapiaapp.databinding.ActivityEjercicioDetalleBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan
import com.example.fisioterapiaapp.paciente.model.RegistroSesion
import com.example.fisioterapiaapp.paciente.repository.PacienteRepository
import kotlinx.coroutines.launch

class EjercicioDetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EJERCICIO = "extra_ejercicio"
        const val EXTRA_PLAN_ID   = "extra_plan_id"
        const val EXTRA_DIA       = "extra_dia"
        const val EXTRA_INDEX     = "extra_index"
        const val RESULT_INDEX    = "result_index"
    }

    private lateinit var binding: ActivityEjercicioDetalleBinding
    private val repo = PacienteRepository()

    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private var videoUrl: String? = null

    // Datos del registro
    private var planId = ""
    private var dia    = ""
    private var ejercicioIndex = -1
    private var seriesVal = 0
    private var repsVal   = 0
    private var evaVal    = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEjercicioDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ejercicio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_EJERCICIO, EjercicioPlan::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_EJERCICIO)
        } ?: run { finish(); return }

        planId         = intent.getStringExtra(EXTRA_PLAN_ID) ?: ""
        dia            = intent.getStringExtra(EXTRA_DIA) ?: ""
        ejercicioIndex = intent.getIntExtra(EXTRA_INDEX, -1)

        videoUrl = ejercicio.videoUrl
        mostrarEjercicio(ejercicio)
        binding.btnAtras.setOnClickListener { finish() }
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Mostrar sección de registro solo si venimos del dashboard (planId presente)
        if (planId.isNotEmpty() && ejercicioIndex >= 0) {
            binding.cardRegistro.visibility = View.VISIBLE
            inicializarFormRegistro(ejercicio)
            verificarCompletadoHoy()
        }
    }

    // ── Mostrar datos del ejercicio ────────────────────────────────────────────

    private fun mostrarEjercicio(ej: EjercicioPlan) {
        binding.tvNombreEjercicio.text = ej.nombreEjercicio.ifBlank { ej.tipo }
        binding.tvTipoEjercicio.text   = ej.tipo
        binding.tvSeries.text          = ej.series.toString()
        binding.tvRepeticiones.text    = ej.repeticiones.toString()
        binding.tvCarga.text           = ej.cargaTexto
        binding.tvFrecuencia.text      =
            if (ej.diasSemana.isEmpty()) "—" else "${ej.diasSemana.size} /sem"

        if (ej.descripcion.isNotBlank()) {
            binding.cardDescripcion.visibility = View.VISIBLE
            binding.tvDescripcion.text = ej.descripcion
        } else {
            binding.cardDescripcion.visibility = View.GONE
        }

        if (!ej.videoUrl.isNullOrBlank()) {
            binding.playerView.visibility    = View.VISIBLE
            binding.layoutSinVideo.visibility = View.GONE
        } else {
            binding.playerView.visibility    = View.GONE
            binding.layoutSinVideo.visibility = View.VISIBLE
        }
    }

    // ── Formulario de registro ─────────────────────────────────────────────────

    private fun inicializarFormRegistro(ej: EjercicioPlan) {
        // Pre-rellenar con valores prescritos
        seriesVal = ej.series.coerceAtLeast(1)
        repsVal   = ej.repeticiones.coerceAtLeast(1)

        binding.tvSeriesVal.text = seriesVal.toString()
        binding.tvRepsVal.text   = repsVal.toString()
        binding.etPesoRealizado.setText(if (ej.peso.isBlank() || ej.peso == "Sin carga") "" else ej.peso)

        // Contadores series
        binding.btnMenosSeries.setOnClickListener {
            if (seriesVal > 1) { seriesVal--; binding.tvSeriesVal.text = seriesVal.toString() }
        }
        binding.btnMasSeries.setOnClickListener {
            seriesVal++; binding.tvSeriesVal.text = seriesVal.toString()
        }

        // Contadores reps
        binding.btnMenosReps.setOnClickListener {
            if (repsVal > 1) { repsVal--; binding.tvRepsVal.text = repsVal.toString() }
        }
        binding.btnMasReps.setOnClickListener {
            repsVal++; binding.tvRepsVal.text = repsVal.toString()
        }

        // Slider EVA
        binding.seekEvaEjercicio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                evaVal = value
                binding.tvEvaValEjercicio.text = "$value – ${etiquetaEva(value)}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        binding.tvEvaValEjercicio.text = "0 – Sin dolor"

        // Botón completar
        binding.btnCompletarEjercicio.setOnClickListener { guardarCompletado() }
    }

    // ── Verificar si ya completado hoy ────────────────────────────────────────

    private fun verificarCompletadoHoy() {
        lifecycleScope.launch {
            try {
                val registros = repo.getRegistrosDePlan(planId)
                val registroHoy = registros.find {
                    it.dia == dia && it.ejercicioIndex == ejercicioIndex
                }
                if (registroHoy != null) {
                    mostrarEstadoCompletado(registroHoy)
                }
            } catch (e: Exception) {
                // Si falla la consulta, simplemente no mostramos el estado completado
            }
        }
    }

    private fun mostrarEstadoCompletado(registro: RegistroSesion) {
        binding.layoutFormRegistro.visibility = View.GONE
        binding.layoutCompletado.visibility   = View.VISIBLE

        val peso = registro.pesoUsado.ifBlank { "sin carga" }
        val dolor = etiquetaEva(registro.eva)
        binding.tvResumenCompletado.text =
            "${registro.seriesCompletadas} series × ${registro.repsCompletadas} reps" +
            " · $peso · Dolor: ${registro.eva}/10 ($dolor)"
    }

    // ── Guardar registro ───────────────────────────────────────────────────────

    private fun guardarCompletado() {
        binding.btnCompletarEjercicio.isEnabled = false
        binding.btnCompletarEjercicio.text = "Guardando..."

        val nombreEjercicio = binding.tvNombreEjercicio.text.toString()
        val pesoTexto = binding.etPesoRealizado.text?.toString()?.trim() ?: ""

        val registro = RegistroSesion(
            planId                      = planId,
            ejercicioIndex              = ejercicioIndex,
            dia                        = dia,
            sesionCompletada            = true,
            ejerciciosCompletados       = listOf(ejercicioIndex),
            ejerciciosCompletadosNombres = listOf(nombreEjercicio),
            seriesCompletadas           = seriesVal,
            repsCompletadas             = repsVal,
            pesoUsado                   = pesoTexto,
            eva                        = evaVal
        )

        lifecycleScope.launch {
            repo.guardarRegistro(registro)
                .onSuccess {
                    mostrarEstadoCompletado(registro)
                    Toast.makeText(
                        this@EjercicioDetalleActivity,
                        "¡Ejercicio completado!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Devolver resultado al dashboard para marcar el checkbox
                    val resultIntent = Intent().putExtra(RESULT_INDEX, ejercicioIndex)
                    setResult(RESULT_OK, resultIntent)
                }
                .onFailure {
                    Toast.makeText(
                        this@EjercicioDetalleActivity,
                        "Error al guardar: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnCompletarEjercicio.isEnabled = true
                    binding.btnCompletarEjercicio.text = "Marcar como completado"
                }
        }
    }

    // ── Etiquetas EVA ──────────────────────────────────────────────────────────

    private fun etiquetaEva(v: Int) = when (v) {
        0       -> "Sin dolor"
        in 1..3 -> "Dolor leve"
        in 4..6 -> "Dolor moderado"
        in 7..9 -> "Dolor intenso"
        else    -> "Dolor insoportable"
    }

    // ── ExoPlayer ─────────────────────────────────────────────────────────────

    private fun inicializarPlayer(url: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.playWhenReady = playWhenReady
            exo.seekTo(currentItem, playbackPosition)
            exo.prepare()
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) inicializarPlayerSiNecesario()
    }

    public override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || player == null) inicializarPlayerSiNecesario()
    }

    public override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT <= 23) liberarPlayer()
    }

    public override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) liberarPlayer()
    }

    private fun inicializarPlayerSiNecesario() {
        val url = videoUrl
        if (player == null && !url.isNullOrBlank()) inicializarPlayer(url)
    }

    private fun liberarPlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentItem      = it.currentMediaItemIndex
            playWhenReady    = it.playWhenReady
            it.release()
        }
        player = null
    }
}
