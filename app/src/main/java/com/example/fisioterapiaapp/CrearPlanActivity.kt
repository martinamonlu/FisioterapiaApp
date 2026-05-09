package com.example.fisioterapiaapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CrearPlanActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var containerEjercicios: LinearLayout
    private lateinit var tvNombrePaciente: TextView
    private lateinit var spinnerFase: Spinner
    private lateinit var etDuracion: EditText
    private lateinit var etObjetivos: EditText
    private lateinit var btnCrearPlan: Button

    private var pacienteId: String = ""
    private var nombrePaciente: String = ""
    private var contadorEjercicios = 0

    // Lista para almacenar los datos de cada ejercicio
    private val ejerciciosViews = mutableListOf<EjercicioView>()

    companion object {
        private const val REQUEST_VIDEO_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_plan)

        // Recibir datos del paciente
        pacienteId = intent.getStringExtra("pacienteId") ?: ""
        nombrePaciente = intent.getStringExtra("nombrePaciente") ?: ""

        // Enlaces con elementos
        tvNombrePaciente = findViewById(R.id.tvNombrePaciente)
        spinnerFase = findViewById(R.id.spinnerFase)
        etDuracion = findViewById(R.id.etDuracion)
        etObjetivos = findViewById(R.id.etObjetivos)
        containerEjercicios = findViewById(R.id.containerEjercicios)
        val btnAnadirEjercicio = findViewById<Button>(R.id.btnAnadirEjercicio)
        btnCrearPlan = findViewById(R.id.btnCrearPlan)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Mostrar nombre del paciente
        tvNombrePaciente.text = "Para: $nombrePaciente"

        // Configurar spinner de fases
        val fases = arrayOf(
            "Fase aguda",
            "Fase subaguda",
            "Fase de recuperación funcional",
            "Fase de mantenimiento"
        )
        val adapterFases = ArrayAdapter(this, android.R.layout.simple_spinner_item, fases)
        adapterFases.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFase.adapter = adapterFases

        // Añadir primer ejercicio por defecto
        anadirEjercicio()

        // Botón añadir ejercicio
        btnAnadirEjercicio.setOnClickListener {
            anadirEjercicio()
        }

        // Botón crear plan
        btnCrearPlan.setOnClickListener {
            validarYCrearPlan()
        }

        // Botón atrás
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun anadirEjercicio() {
        contadorEjercicios++

        // Inflar el layout del ejercicio
        val inflater = LayoutInflater.from(this)
        val ejercicioView = inflater.inflate(R.layout.item_ejercicio, containerEjercicios, false)

        // Configurar elementos
        val tvNumero = ejercicioView.findViewById<TextView>(R.id.tvNumeroEjercicio)
        val btnEliminar = ejercicioView.findViewById<ImageButton>(R.id.btnEliminarEjercicio)
        val spinnerTipo = ejercicioView.findViewById<Spinner>(R.id.spinnerTipoEjercicio)
        val etRepeticiones = ejercicioView.findViewById<EditText>(R.id.etRepeticiones)
        val etSeries = ejercicioView.findViewById<EditText>(R.id.etSeries)
        val etPeso = ejercicioView.findViewById<EditText>(R.id.etPeso)
        val etFrecuencia = ejercicioView.findViewById<EditText>(R.id.etFrecuencia)
        val btnSeleccionarVideo = ejercicioView.findViewById<Button>(R.id.btnSeleccionarVideo)
        val tvNombreVideo = ejercicioView.findViewById<TextView>(R.id.tvNombreVideo)

        tvNumero.text = "Ejercicio $contadorEjercicios"

        // Configurar spinner de tipos de ejercicio
        val tipos = arrayOf(
            "Movilidad",
            "Fuerza",
            "Resistencia",
            "Equilibrio",
            "Coordinación",
            "Flexibilidad",
            "Propiocepción"
        )
        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipos

        // Crear objeto para almacenar referencias
        val ejercicioViewObj = EjercicioView(
            view = ejercicioView,
            spinnerTipo = spinnerTipo,
            etRepeticiones = etRepeticiones,
            etSeries = etSeries,
            etPeso = etPeso,
            etFrecuencia = etFrecuencia,
            tvNombreVideo = tvNombreVideo,
            videoUri = null
        )

        ejerciciosViews.add(ejercicioViewObj)

        // Botón seleccionar vídeo
        btnSeleccionarVideo.setOnClickListener {
            seleccionarVideo(ejercicioViewObj)
        }

        // Botón eliminar
        btnEliminar.setOnClickListener {
            if (ejerciciosViews.size > 1) {
                containerEjercicios.removeView(ejercicioView)
                ejerciciosViews.remove(ejercicioViewObj)
                renumerarEjercicios()
            } else {
                Toast.makeText(this, "Debe haber al menos un ejercicio", Toast.LENGTH_SHORT).show()
            }
        }

        // Añadir al contenedor
        containerEjercicios.addView(ejercicioView)
    }

    private fun seleccionarVideo(ejercicioView: EjercicioView) {
        // Guardar referencia del ejercicio actual para saber a cuál asignar el vídeo
        ejercicioView.view.tag = ejercicioView

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Seleccionar vídeo"), REQUEST_VIDEO_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Encontrar el ejercicio que solicitó el vídeo
                val ejercicioView = ejerciciosViews.find { it.view.tag == it }
                ejercicioView?.let {
                    it.videoUri = uri
                    it.tvNombreVideo.text = obtenerNombreArchivo(uri)
                    it.tvNombreVideo.setTextColor(getColor(R.color.text_primary))
                }
            }
        }
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "video_seleccionado.mp4"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                nombre = cursor.getString(nameIndex)
            }
        }
        return nombre
    }

    private fun renumerarEjercicios() {
        ejerciciosViews.forEachIndexed { index, ejercicioView ->
            val tvNumero = ejercicioView.view.findViewById<TextView>(R.id.tvNumeroEjercicio)
            tvNumero.text = "Ejercicio ${index + 1}"
        }
    }

    private fun validarYCrearPlan() {
        val fase = spinnerFase.selectedItem.toString()
        val duracion = etDuracion.text.toString().trim()
        val objetivos = etObjetivos.text.toString().trim()

        // Validaciones
        when {
            duracion.isEmpty() -> {
                etDuracion.error = "Campo obligatorio"
                etDuracion.requestFocus()
                return
            }
            objetivos.isEmpty() -> {
                etObjetivos.error = "Campo obligatorio"
                etObjetivos.requestFocus()
                return
            }
        }

        // Validar ejercicios
        val ejercicios = mutableListOf<HashMap<String, Any?>>()

        for ((index, ejercicioView) in ejerciciosViews.withIndex()) {
            val tipo = ejercicioView.spinnerTipo.selectedItem.toString()
            val repeticiones = ejercicioView.etRepeticiones.text.toString().trim()
            val series = ejercicioView.etSeries.text.toString().trim()
            val peso = ejercicioView.etPeso.text.toString().trim()
            val frecuencia = ejercicioView.etFrecuencia.text.toString().trim()

            when {
                repeticiones.isEmpty() -> {
                    ejercicioView.etRepeticiones.error = "Obligatorio"
                    ejercicioView.etRepeticiones.requestFocus()
                    Toast.makeText(this, "Completa todos los campos del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
                series.isEmpty() -> {
                    ejercicioView.etSeries.error = "Obligatorio"
                    ejercicioView.etSeries.requestFocus()
                    Toast.makeText(this, "Completa todos los campos del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
                frecuencia.isEmpty() -> {
                    ejercicioView.etFrecuencia.error = "Obligatorio"
                    ejercicioView.etFrecuencia.requestFocus()
                    Toast.makeText(this, "Completa todos los campos del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Crear objeto ejercicio (sin URL de vídeo por ahora, se subirá después)
            val ejercicio = hashMapOf<String, Any?>(
                "tipo" to tipo,
                "repeticiones" to repeticiones.toInt(),
                "series" to series.toInt(),
                "peso" to peso,
                "frecuencia" to frecuencia.toInt(),
                "videoUrl" to null // Se actualizará después de subir
            )

            ejercicios.add(ejercicio)
        }

        // Todo validado, crear plan
        crearPlan(fase, duracion.toInt(), objetivos, ejercicios)
    }

    private fun crearPlan(fase: String, duracion: Int, objetivos: String, ejercicios: List<HashMap<String, Any?>>) {
        btnCrearPlan.isEnabled = false
        btnCrearPlan.text = "Creando..."

        val plan = hashMapOf(
            "pacienteId" to pacienteId,
            "fisioterapeutaId" to auth.currentUser?.uid,
            "fase" to fase,
            "duracionSemanas" to duracion,
            "objetivos" to objetivos,
            "ejercicios" to ejercicios,
            "fechaCreacion" to com.google.firebase.Timestamp.now(),
            "activo" to true
        )

        db.collection("planes_ejercicio")
            .add(plan)
            .addOnSuccessListener { documentReference ->
                // Plan creado, ahora subir vídeos si los hay
                subirVideos(documentReference.id, ejercicios)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear plan: ${e.message}", Toast.LENGTH_LONG).show()
                btnCrearPlan.isEnabled = true
                btnCrearPlan.text = "Crear Plan"
            }
    }

    private fun subirVideos(planId: String, ejercicios: List<HashMap<String, Any?>>) {
        var videosSubidos = 0
        val totalVideos = ejerciciosViews.count { it.videoUri != null }

        if (totalVideos == 0) {
            // No hay vídeos, terminar
            finalizarCreacion()
            return
        }

        ejerciciosViews.forEachIndexed { index, ejercicioView ->
            ejercicioView.videoUri?.let { uri ->
                val storageRef = storage.reference
                    .child("videos_ejercicios")
                    .child("$planId/ejercicio_$index.mp4")

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Actualizar URL del vídeo en el ejercicio
                            ejercicios[index]["videoUrl"] = downloadUri.toString()

                            videosSubidos++
                            if (videosSubidos == totalVideos) {
                                // Todos los vídeos subidos, actualizar plan
                                actualizarPlanConVideos(planId, ejercicios)
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Error al subir vídeo, pero continuar
                        videosSubidos++
                        if (videosSubidos == totalVideos) {
                            actualizarPlanConVideos(planId, ejercicios)
                        }
                    }
            }
        }
    }

    private fun actualizarPlanConVideos(planId: String, ejercicios: List<HashMap<String, Any?>>) {
        db.collection("planes_ejercicio")
            .document(planId)
            .update("ejercicios", ejercicios)
            .addOnSuccessListener {
                finalizarCreacion()
            }
            .addOnFailureListener {
                finalizarCreacion() // Continuar aunque falle la actualización
            }
    }

    private fun finalizarCreacion() {
        Toast.makeText(this, "Plan creado exitosamente", Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    // Clase auxiliar para almacenar referencias de cada ejercicio
    private data class EjercicioView(
        val view: android.view.View,
        val spinnerTipo: Spinner,
        val etRepeticiones: EditText,
        val etSeries: EditText,
        val etPeso: EditText,
        val etFrecuencia: EditText,
        val tvNombreVideo: TextView,
        var videoUri: Uri?
    )
}