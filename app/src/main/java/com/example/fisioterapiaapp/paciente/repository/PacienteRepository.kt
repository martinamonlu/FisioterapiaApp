package com.example.fisioterapiaapp.paciente.repository

import com.example.fisioterapiaapp.paciente.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository del paciente. Todas las queries se filtran por el
 * usuario autenticado para que las Security Rules sean efectivas.
 *
 * Esquema Firestore (creado por el fisio en AddPacienteActivity / CrearPlanActivity):
 *   pacientes/{pacienteId}           → campo `userId` = uid de Firebase Auth
 *   planes_ejercicio/{planId}        → campo `pacienteId` = id del documento de paciente
 *   registros_sesion/{registroId}    → campo `pacienteId`
 *   alertas/{alertaId}               → campo `pacienteId`
 *   conversaciones/{convId}/mensajes → convId = "{pacienteUid}_{fisioUid}"
 *   informes/{informeId}             → campo `pacienteId`
 */
class PacienteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private val uid: String? get() = auth.currentUser?.uid

    // ── Helpers ────────────────────────────────────────────────────

    /** Busca el documento de paciente cuyo campo userId coincide con el uid actual. */
    private suspend fun documentoPacienteActual(): com.google.firebase.firestore.DocumentSnapshot? {
        val u = uid ?: return null
        val snap = db.collection("pacientes")
            .whereEqualTo("userId", u)
            .limit(1)
            .get().await()
        return snap.documents.firstOrNull()
    }

    suspend fun pacienteIdActual(): String? = documentoPacienteActual()?.id

    // ── Paciente ───────────────────────────────────────────────────

    suspend fun getPaciente(): Paciente? {
        val doc = documentoPacienteActual() ?: return null
        return doc.toObject(Paciente::class.java)?.copy(id = doc.id)
    }

    /** Flow del documento del paciente (cambios en tiempo real). */
    fun observarPaciente(): Flow<Paciente?> = callbackFlow {
        val u = uid
        if (u == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = db.collection("pacientes")
            .whereEqualTo("userId", u)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("REPO", "observarPaciente error: ${err.message}")
                    trySend(null)
                    return@addSnapshotListener  // NO relanzar la excepción
                }
                val doc = snap?.documents?.firstOrNull()
                trySend(doc?.toObject(Paciente::class.java)?.copy(id = doc.id))
            }
        awaitClose { reg.remove() }
    }
    // ── Plan activo ────────────────────────────────────────────────

    suspend fun getPlanActivo(): Plan? {
        val pid = pacienteIdActual() ?: return null
        val snap = db.collection("planes_ejercicio")
            .whereEqualTo("pacienteId", pid)
            .whereEqualTo("activo", true)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .limit(1)
            .get().await()
        val doc = snap.documents.firstOrNull() ?: return null
        return doc.toObject(Plan::class.java)?.copy(id = doc.id)
    }

    fun observarPlanActivo(): Flow<Plan?> = callbackFlow {
        val pid = try { pacienteIdActual() } catch (e: Exception) { null }
        if (pid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = db.collection("planes_ejercicio")
            .whereEqualTo("pacienteId", pid)
            .whereEqualTo("activo", true)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("REPO", "observarPlan error: ${err.message}")
                    trySend(null)
                    return@addSnapshotListener  // NO relanzar la excepción
                }
                val doc = snap?.documents?.firstOrNull()
                trySend(doc?.toObject(Plan::class.java)?.copy(id = doc.id))
            }
        awaitClose { reg.remove() }
    }

    // ── Registros post-sesión ──────────────────────────────────────

    suspend fun guardarRegistro(registro: RegistroSesion): Result<String> = runCatching {
        val pid = pacienteIdActual() ?: error("Paciente no encontrado")
        val r = registro.copy(pacienteId = pid)
        val ref = db.collection("registros_sesion").add(r).await()
        ref.id
    }

    suspend fun getRegistrosDePlan(planId: String): List<RegistroSesion> {
        val pid = pacienteIdActual() ?: return emptyList()
        val snap = db.collection("registros_sesion")
            .whereEqualTo("pacienteId", pid)
            .whereEqualTo("planId", planId)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(RegistroSesion::class.java)?.copy(id = it.id)
        }
    }

    suspend fun getRegistroDia(planId: String, dia: String): RegistroSesion? {
        val pid = pacienteIdActual() ?: return null
        val snap = db.collection("registros_sesion")
            .whereEqualTo("pacienteId", pid)
            .whereEqualTo("planId", planId)
            .whereEqualTo("dia", dia)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(1)
            .get().await()
        return snap.documents.firstOrNull()
            ?.toObject(RegistroSesion::class.java)
            ?.copy(id = snap.documents.first().id)
    }

    // ── Alertas ────────────────────────────────────────────────────

    fun observarAlertas(): Flow<List<Alerta>> = callbackFlow {
        val pid = try { pacienteIdActual() } catch (e: Exception) { null }
        if (pid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("alertas")
            .whereEqualTo("pacienteId", pid)
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("REPO", "observarAlertas error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(Alerta::class.java)?.copy(id = it.id) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // ── Chat ───────────────────────────────────────────────────────

    private suspend fun conversacionId(): String? {
        val pacienteDoc = documentoPacienteActual() ?: return null
        val u = uid ?: return null
        val fisioId = pacienteDoc.getString("fisioterapeutaId") ?: return null
        return "${u}_$fisioId"
    }

    fun observarChat(): Flow<List<MensajeChat>> = callbackFlow {
        val convId = try { conversacionId() } catch (e: Exception) { null }
        if (convId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("conversaciones")
            .document(convId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    android.util.Log.e("REPO", "observarChat error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(MensajeChat::class.java)?.copy(id = it.id) }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun enviarMensaje(texto: String): Result<Unit> = runCatching {
        val convId = conversacionId() ?: error("Conversación no disponible")
        val u = uid ?: error("Sin usuario")
        val msg = MensajeChat(
            texto = texto,
            emisorId = u,
            esPaciente = true
        )
        db.collection("conversaciones")
            .document(convId)
            .collection("mensajes")
            .add(msg).await()
    }

    // ── Informes ───────────────────────────────────────────────────

    suspend fun getInformes(): List<Informe> {
        val pid = pacienteIdActual() ?: return emptyList()
        val snap = db.collection("informes")
            .whereEqualTo("pacienteId", pid)
            .orderBy("fechaGeneracion", Query.Direction.DESCENDING)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(Informe::class.java)?.copy(id = it.id)
        }
    }

    // ── Adherencia / Progreso ──────────────────────────────────────

    /**
     * Calcula adherencia y métricas semana a semana.
     * Devuelve la lista de PuntoProgreso (uno por semana del plan).
     */
    suspend fun calcularPuntosPorSemana(plan: Plan): List<PuntoProgreso> {
        val registros = getRegistrosDePlan(plan.id)
        if (registros.isEmpty() || plan.duracionSemanas == 0) return emptyList()

        // Sesiones esperadas por semana = suma de días marcados en cada ejercicio
        val sesionesPorSemana = plan.ejercicios.sumOf { it.diasSemana.size }
        if (sesionesPorSemana == 0) return emptyList()

        val fechaInicio = plan.fechaCreacion?.toDate()?.time ?: return emptyList()
        val msSemana = 7L * 24 * 60 * 60 * 1000

        return (1..plan.duracionSemanas).map { sem ->
            val inicioSem = fechaInicio + (sem - 1) * msSemana
            val finSem = inicioSem + msSemana
            val regsSem = registros.filter {
                val t = it.fecha.toDate().time
                t in inicioSem until finSem
            }
            val completadas = regsSem.count { it.sesionCompletada }
            val adherencia = (completadas.toFloat() / sesionesPorSemana) * 100f
            val evaProm = if (regsSem.isNotEmpty()) regsSem.map { it.eva }.average().toFloat() else 0f
            val rpeProm = if (regsSem.isNotEmpty()) regsSem.map { it.rpe }.average().toFloat() else 0f
            val cargaProm = if (regsSem.isNotEmpty())
                regsSem.map { it.pesoUsado.toFloatOrNull() ?: 0f }.average().toFloat()
            else 0f

            PuntoProgreso(
                semana = sem,
                eva = evaProm,
                rpe = rpeProm,
                adherencia = adherencia.coerceIn(0f, 100f),
                carga = cargaProm
            )
        }
    }
}
