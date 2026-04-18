package com.example.fisioterapiaapp

// ALMACÉN TEMPORAL DEL PERFIL DEL FISIOTERAPEUTA
// Guarda los datos en memoria mientras la app está abierta.
// Al cerrar la app los datos se pierden (limitación del prototipo sin base de datos).
object PerfilFisioData {
    var nombre:       String = "Juan"
    var apellidos:    String = "Pérez García"
    var nacimiento:   String = "01/01/1990"
    var colegiado:    String = "12345"
    var especialidad: String = "Fisioterapia deportiva"
    var email:        String = "fisio@email.com"
}
