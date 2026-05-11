package com.example.fisioterapiaapp.paciente.ui.exercise

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.fisioterapiaapp.databinding.ActivityEjercicioDetalleBinding
import com.example.fisioterapiaapp.paciente.model.EjercicioPlan

class EjercicioDetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EJERCICIO = "extra_ejercicio"
    }

    private lateinit var binding: ActivityEjercicioDetalleBinding
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private var videoUrl: String? = null

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

        videoUrl = ejercicio.videoUrl
        mostrarEjercicio(ejercicio)
        binding.btnAtras.setOnClickListener { finish() }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun mostrarEjercicio(ej: EjercicioPlan) {
        binding.tvNombreEjercicio.text =
            ej.nombreEjercicio.ifBlank { ej.tipo }
        binding.tvTipoEjercicio.text = ej.tipo
        binding.tvSeries.text = ej.series.toString()
        binding.tvRepeticiones.text = ej.repeticiones.toString()
        binding.tvCarga.text = ej.cargaTexto
        binding.tvFrecuencia.text =
            if (ej.diasSemana.isEmpty()) "—" else "${ej.diasSemana.size} /sem"

        // Descripción no existe en el modelo Firestore (lo guarda el fisio
        // como `tipo`); ocultamos la card si no hay nada que mostrar.
        binding.cardDescripcion.visibility = View.GONE

        if (!ej.videoUrl.isNullOrBlank()) {
            binding.playerView.visibility = View.VISIBLE
            binding.layoutSinVideo.visibility = View.GONE
        } else {
            binding.playerView.visibility = View.GONE
            binding.layoutSinVideo.visibility = View.VISIBLE
        }
    }

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
        if (player == null && !url.isNullOrBlank()) {
            inicializarPlayer(url)
        }
    }

    private fun liberarPlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentItem = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }
}
