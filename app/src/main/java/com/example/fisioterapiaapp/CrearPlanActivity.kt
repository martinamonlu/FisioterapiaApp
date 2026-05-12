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
import android.widget.CheckBox

class CrearPlanActivity : AppCompatActivity() {

    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var containerEjercicios: LinearLayout
    private lateinit var tvNombrePaciente: TextView
    private lateinit var tvTitulo: TextView
    private lateinit var spinnerFase: Spinner
    private lateinit var etDuracion: EditText
    private lateinit var etObjetivos: EditText
    private lateinit var btnCrearPlan: Button

    private var pacienteId     = ""
    private var nombrePaciente = ""
    private var planId         = ""       // vacío = crear, no vacío = editar
    private var modoEdicion    = false
    private var contadorEjercicios = 0
    private var ejercicioViewActual: EjercicioView? = null
    private val ejerciciosViews = mutableListOf<EjercicioView>()

    private val fases = arrayOf(
        "Fase aguda",
        "Fase subaguda",
        "Fase de recuperación funcional",
        "Fase de mantenimiento"
    )
    private val tiposEjercicio = arrayOf(
        "Movilidad", "Fuerza", "Resistencia", "Equilibrio",
        "Coordinación", "Flexibilidad", "Propiocepción"
    )

    companion object {
        private const val REQUEST_VIDEO_PICK = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_plan)

        pacienteId     = intent.getStringExtra("pacienteId") ?: ""
        nombrePaciente = intent.getStringExtra("nombrePaciente") ?: ""
        planId         = intent.getStringExtra("planId") ?: ""
        modoEdicion    = intent.getBooleanExtra("modoEdicion", false)

        tvTitulo             = findViewById(R.id.tvTitulo)
        tvNombrePaciente     = findViewById(R.id.tvNombrePaciente)
        spinnerFase          = findViewById(R.id.spinnerFase)
        etDuracion           = findViewById(R.id.etDuracion)
        etObjetivos          = findViewById(R.id.etObjetivos)
        containerEjercicios  = findViewById(R.id.containerEjercicios)
        btnCrearPlan         = findViewById(R.id.btnCrearPlan)
        val btnAnadirEjercicio = findViewById<Button>(R.id.btnAnadirEjercicio)
        val btnBack            = findViewById<ImageButton>(R.id.btnBack)

        tvNombrePaciente.text = "Para: $nombrePaciente"

        val adapterFases = ArrayAdapter(this, android.R.layout.simple_spinner_item, fases)
        adapterFases.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFase.adapter = adapterFases

        if (modoEdicion) {
            tvTitulo.text    = "Editar Plan"
            btnCrearPlan.text = "Guardar cambios"
            cargarPlanExistente()
        } else {
            anadirEjercicio(null)
        }

        btnAnadirEjercicio.setOnClickListener { anadirEjercicio(null) }
        btnCrearPlan.setOnClickListener { validarYGuardar() }
        btnBack.setOnClickListener { finish() }
    }

    // ── Carga plan existente (modo edición) ────────────────────────────────────

    private fun cargarPlanExistente() {
        if (planId.isEmpty()) return
        db.collection("planes_ejercicio").document(planId).get()
            .addOnSuccessListener { doc ->
                val fase      = doc.getString("fase") ?: ""
                val duracion  = doc.getLong("duracionSemanas")?.toInt() ?: 0
                val objetivos = doc.getString("objetivos") ?: ""

                val faseIdx = fases.indexOfFirst { it == fase }.coerceAtLeast(0)
                spinnerFase.setSelection(faseIdx)
                etDuracion.setText(duracion.toString())
                etObjetivos.setText(objetivos)

                @Suppress("UNCHECKED_CAST")
                val ejercicios = (doc.get("ejercicios") as? List<Map<String, Any>>) ?: emptyList()
                ejercicios.forEach { datos -> anadirEjercicio(datos) }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar plan: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ── Añadir bloque de ejercicio al formulario ───────────────────────────────

    private fun anadirEjercicio(datos: Map<String, Any>?) {
        contadorEjercicios++
        val inflater   = LayoutInflater.from(this)
        val ejercicioView = inflater.inflate(R.layout.item_ejercicio, containerEjercicios, false)

        val tvNumero          = ejercicioView.findViewById<TextView>(R.id.tvNumeroEjercicio)
        val btnEliminar       = ejercicioView.findViewById<ImageButton>(R.id.btnEliminarEjercicio)
        val spinnerTipo       = ejercicioView.findViewById<Spinner>(R.id.spinnerTipoEjercicio)
        val etNombre          = ejercicioView.findViewById<EditText>(R.id.etNombreEjercicio)
        val etDescripcion     = ejercicioView.findViewById<EditText>(R.id.etDescripcion)
        val etRepeticiones    = ejercicioView.findViewById<EditText>(R.id.etRepeticiones)
        val etSeries          = ejercicioView.findViewById<EditText>(R.id.etSeries)
        val etPeso            = ejercicioView.findViewById<EditText>(R.id.etPeso)
        val cbLunes           = ejercicioView.findViewById<CheckBox>(R.id.cbLunes)
        val cbMartes          = ejercicioView.findViewById<CheckBox>(R.id.cbMartes)
        val cbMiercoles       = ejercicioView.findViewById<CheckBox>(R.id.cbMiercoles)
        val cbJueves          = ejercicioView.findViewById<CheckBox>(R.id.cbJueves)
        val cbViernes         = ejercicioView.findViewById<CheckBox>(R.id.cbViernes)
        val cbSabado          = ejercicioView.findViewById<CheckBox>(R.id.cbSabado)
        val cbDomingo         = ejercicioView.findViewById<CheckBox>(R.id.cbDomingo)
        val btnSeleccionarVideo = ejercicioView.findViewById<Button>(R.id.btnSeleccionarVideo)
        val tvNombreVideo     = ejercicioView.findViewById<TextView>(R.id.tvNombreVideo)

        tvNumero.text = "Ejercicio $contadorEjercicios"

        val adapterTipos = ArrayAdapter(this, android.R.layout.simple_spinner_item, tiposEjercicio)
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipos

        val ejObj = EjercicioView(
            view = ejercicioView,
            etNombreEjercicio = etNombre,
            etDescripcion     = etDescripcion,
            spinnerTipo       = spinnerTipo,
            etRepeticiones    = etRepeticiones,
            etSeries          = etSeries,
            etPeso            = etPeso,
            cbLunes           = cbLunes,
            cbMartes          = cbMartes,
            cbMiercoles       = cbMiercoles,
            cbJueves          = cbJueves,
            cbViernes         = cbViernes,
            cbSabado          = cbSabado,
            cbDomingo         = cbDomingo,
            tvNombreVideo     = tvNombreVideo,
            videoUri          = null,
            videoUrlExistente = null
        )

        // Pre-rellenar si venimos en modo edición
        if (datos != null) {
            etNombre.setText(datos["nombreEjercicio"] as? String ?: "")
            etDescripcion.setText(datos["descripcion"] as? String ?: "")

            val tipoStr  = datos["tipo"] as? String ?: ""
            val tipoIdx  = tiposEjercicio.indexOfFirst { it == tipoStr }.coerceAtLeast(0)
            spinnerTipo.setSelection(tipoIdx)

            etRepeticiones.setText(datos["repeticiones"]?.toString() ?: "")
            etSeries.setText(datos["series"]?.toString() ?: "")
            etPeso.setText(datos["peso"] as? String ?: "")

            @Suppress("UNCHECKED_CAST")
            val dias = datos["diasSemana"] as? List<String> ?: emptyList()
            cbLunes.isChecked     = "Lunes"      in dias
            cbMartes.isChecked    = "Martes"     in dias
            cbMiercoles.isChecked = "Miércoles"  in dias
            cbJueves.isChecked    = "Jueves"     in dias
            cbViernes.isChecked   = "Viernes"    in dias
            cbSabado.isChecked    = "Sábado"     in dias
            cbDomingo.isChecked   = "Domingo"    in dias

            val videoUrl = datos["videoUrl"] as? String
            ejObj.videoUrlExistente = videoUrl
            if (!videoUrl.isNullOrEmpty()) {
                tvNombreVideo.text = "Vídeo guardado"
                tvNombreVideo.setTextColor(getColor(R.color.text_primary))
            }
        }

        ejerciciosViews.add(ejObj)

        btnSeleccionarVideo.setOnClickListener {
            ejercicioViewActual = ejObj
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(intent, "Seleccionar vídeo"), REQUEST_VIDEO_PICK)
        }

        btnEliminar.setOnClickListener {
            if (ejerciciosViews.size > 1) {
                containerEjercicios.removeView(ejercicioView)
                ejerciciosViews.remove(ejObj)
                renumerarEjercicios()
            } else {
                Toast.makeText(this, "Debe haber al menos un ejercicio", Toast.LENGTH_SHORT).show()
            }
        }

        containerEjercicios.addView(ejercicioView)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                ejercicioViewActual?.let {
                    it.videoUri = uri
                    it.videoUrlExistente = null  // el nuevo vídeo reemplaza al existente
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
            if (cursor.moveToFirst() && nameIndex != -1) nombre = cursor.getString(nameIndex)
        }
        return nombre
    }

    private fun renumerarEjercicios() {
        ejerciciosViews.forEachIndexed { index, ev ->
            ev.view.findViewById<TextView>(R.id.tvNumeroEjercicio).text = "Ejercicio ${index + 1}"
        }
    }

    // ── Validación y guardado ──────────────────────────────────────────────────

    private fun validarYGuardar() {
        val fase     = spinnerFase.selectedItem.toString()
        val duracion = etDuracion.text.toString().trim()
        val objetivos = etObjetivos.text.toString().trim()

        when {
            duracion.isEmpty() -> { etDuracion.error = "Campo obligatorio"; etDuracion.requestFocus(); return }
            objetivos.isEmpty() -> { etObjetivos.error = "Campo obligatorio"; etObjetivos.requestFocus(); return }
        }

        val ejercicios = mutableListOf<HashMap<String, Any?>>()

        for ((index, ev) in ejerciciosViews.withIndex()) {
            val nombre       = ev.etNombreEjercicio.text.toString().trim()
            val descripcion  = ev.etDescripcion.text.toString().trim()
            val tipo         = ev.spinnerTipo.selectedItem.toString()
            val repeticiones = ev.etRepeticiones.text.toString().trim()
            val series       = ev.etSeries.text.toString().trim()
            val peso         = ev.etPeso.text.toString().trim()
            val dias = mutableListOf<String>().apply {
                if (ev.cbLunes.isChecked)     add("Lunes")
                if (ev.cbMartes.isChecked)    add("Martes")
                if (ev.cbMiercoles.isChecked) add("Miércoles")
                if (ev.cbJueves.isChecked)    add("Jueves")
                if (ev.cbViernes.isChecked)   add("Viernes")
                if (ev.cbSabado.isChecked)    add("Sábado")
                if (ev.cbDomingo.isChecked)   add("Domingo")
            }

            when {
                nombre.isEmpty() -> {
                    ev.etNombreEjercicio.error = "Obligatorio"
                    ev.etNombreEjercicio.requestFocus()
                    Toast.makeText(this, "Añade el nombre del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
                repeticiones.isEmpty() -> {
                    ev.etRepeticiones.error = "Obligatorio"
                    ev.etRepeticiones.requestFocus()
                    Toast.makeText(this, "Completa repeticiones del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
                series.isEmpty() -> {
                    ev.etSeries.error = "Obligatorio"
                    ev.etSeries.requestFocus()
                    Toast.makeText(this, "Completa series del ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
                dias.isEmpty() -> {
                    Toast.makeText(this, "Selecciona al menos un día en el ejercicio ${index + 1}", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            ejercicios.add(hashMapOf(
                "nombreEjercicio" to nombre,
                "descripcion"     to descripcion,
                "tipo"            to tipo,
                "repeticiones"    to repeticiones.toInt(),
                "series"          to series.toInt(),
                "peso"            to peso,
                "diasSemana"      to dias,
                "videoUrl"        to ev.videoUrlExistente  // null si no hay vídeo ni url existente
            ))
        }

        if (modoEdicion) {
            actualizarPlan(fase, duracion.toInt(), objetivos, ejercicios)
        } else {
            crearPlan(fase, duracion.toInt(), objetivos, ejercicios)
        }
    }

    // ── Crear plan nuevo ───────────────────────────────────────────────────────

    private fun crearPlan(fase: String, duracion: Int, objetivos: String, ejercicios: List<HashMap<String, Any?>>) {
        btnCrearPlan.isEnabled = false
        btnCrearPlan.text = "Creando..."

        val plan = hashMapOf(
            "pacienteId"      to pacienteId,
            "fisioterapeutaId" to auth.currentUser?.uid,
            "fase"            to fase,
            "duracionSemanas" to duracion,
            "objetivos"       to objetivos,
            "ejercicios"      to ejercicios,
            "fechaCreacion"   to com.google.firebase.Timestamp.now(),
            "activo"          to true
        )

        db.collection("planes_ejercicio").add(plan)
            .addOnSuccessListener { ref -> subirVideos(ref.id, ejercicios) }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear plan: ${e.message}", Toast.LENGTH_LONG).show()
                btnCrearPlan.isEnabled = true
                btnCrearPlan.text = "Crear Plan"
            }
    }

    // ── Actualizar plan existente ──────────────────────────────────────────────

    private fun actualizarPlan(fase: String, duracion: Int, objetivos: String, ejercicios: List<HashMap<String, Any?>>) {
        btnCrearPlan.isEnabled = false
        btnCrearPlan.text = "Guardando..."

        db.collection("planes_ejercicio").document(planId)
            .update(mapOf(
                "fase"            to fase,
                "duracionSemanas" to duracion,
                "objetivos"       to objetivos,
                "ejercicios"      to ejercicios
            ))
            .addOnSuccessListener { subirVideos(planId, ejercicios) }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                btnCrearPlan.isEnabled = true
                btnCrearPlan.text = "Guardar cambios"
            }
    }

    // ── Subir vídeos nuevos ────────────────────────────────────────────────────

    private fun subirVideos(targetPlanId: String, ejercicios: List<HashMap<String, Any?>>) {
        val nuevosVideos = ejerciciosViews.indices.filter { ejerciciosViews[it].videoUri != null }

        if (nuevosVideos.isEmpty()) {
            finalizarCreacion()
            return
        }

        var subidos = 0
        for (index in nuevosVideos) {
            val uri = ejerciciosViews[index].videoUri!!
            val ref = storage.reference
                .child("videos_ejercicios")
                .child("$targetPlanId/ejercicio_$index.mp4")

            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        ejercicios[index]["videoUrl"] = downloadUri.toString()
                        subidos++
                        if (subidos == nuevosVideos.size) actualizarUrlsVideos(targetPlanId, ejercicios)
                    }
                }
                .addOnFailureListener {
                    subidos++
                    if (subidos == nuevosVideos.size) actualizarUrlsVideos(targetPlanId, ejercicios)
                }
        }
    }

    private fun actualizarUrlsVideos(targetPlanId: String, ejercicios: List<HashMap<String, Any?>>) {
        db.collection("planes_ejercicio").document(targetPlanId)
            .update("ejercicios", ejercicios)
            .addOnSuccessListener { finalizarCreacion() }
            .addOnFailureListener { finalizarCreacion() }
    }

    private fun finalizarCreacion() {
        val msg = if (modoEdicion) "Plan actualizado correctamente" else "Plan creado exitosamente"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    // ── Data class auxiliar ────────────────────────────────────────────────────

    private data class EjercicioView(
        val view: android.view.View,
        val etNombreEjercicio: EditText,
        val etDescripcion: EditText,
        val spinnerTipo: Spinner,
        val etRepeticiones: EditText,
        val etSeries: EditText,
        val etPeso: EditText,
        val cbLunes: CheckBox,
        val cbMartes: CheckBox,
        val cbMiercoles: CheckBox,
        val cbJueves: CheckBox,
        val cbViernes: CheckBox,
        val cbSabado: CheckBox,
        val cbDomingo: CheckBox,
        val tvNombreVideo: TextView,
        var videoUri: Uri?,
        var videoUrlExistente: String?
    )
}
