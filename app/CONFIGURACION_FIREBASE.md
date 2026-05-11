# Rehapp – Guía de Configuración Firebase

## 1. Crear el proyecto en Firebase Console

1. Ve a https://console.firebase.google.com
2. Crea un nuevo proyecto → nombre: **Rehapp**
3. Activa Google Analytics (opcional)

---

## 2. Registrar la aplicación Android

1. En Firebase Console → **Añadir app → Android**
2. Package name: `com.example.fisioterapiaapp`
3. Descarga el archivo `google-services.json`
4. Colócalo en: `app/src/main/google-services.json`

---

## 3. Activar Authentication

1. Firebase Console → **Authentication → Métodos de inicio de sesión**
2. Activar: **Correo electrónico/contraseña**

> **Importante**: Los pacientes se autentican con el patrón de email:
> `{DNI}@rehapp.paciente` (ej: `12345678A@rehapp.paciente`)
> El DNI se convierte a mayúsculas automáticamente.

---

## 4. Configurar Firestore

1. Firebase Console → **Firestore Database → Crear base de datos**
2. Modo: **Producción** (con reglas restrictivas)
3. Región: `europe-west1` (o la más cercana)

---

## 5. Reglas de seguridad Firestore

Copia estas reglas en **Firestore → Reglas**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Pacientes: solo puede leer/escribir su propio documento
    match /pacientes/{pacienteId} {
      allow read, write: if request.auth != null
                          && request.auth.uid == pacienteId;
    }

    // Planes: el paciente puede leer su plan activo
    match /planes/{planId} {
      allow read: if request.auth != null
                   && resource.data.pacienteId == request.auth.uid;
      // Solo el fisio (autenticado con rol fisio) puede escribir
      allow write: if false; // Gestionado desde backend/fisio
    }

    // Semanas del plan
    match /planes/{planId}/semanas/{semanaId} {
      allow read: if request.auth != null
                   && get(/databases/$(database)/documents/planes/$(planId))
                      .data.pacienteId == request.auth.uid;
      allow write: if false;
    }

    // Registros post-sesión: paciente puede crear y leer los suyos
    match /registros/{registroId} {
      allow read: if request.auth != null
                   && resource.data.pacienteId == request.auth.uid;
      allow create: if request.auth != null
                     && request.resource.data.pacienteId == request.auth.uid;
      allow update: if request.auth != null
                     && resource.data.pacienteId == request.auth.uid;
      allow delete: if false;
    }

    // Alertas: paciente puede leer y marcar como leídas
    match /alertas/{alertaId} {
      allow read: if request.auth != null
                   && resource.data.pacienteId == request.auth.uid;
      allow update: if request.auth != null
                     && resource.data.pacienteId == request.auth.uid
                     && request.resource.data.diff(resource.data).affectedKeys()
                        .hasOnly(['leida']);
      allow write: if false;
    }

    // Chat: acceso solo si el uid aparece en el ID de conversación
    // Formato convId: "{pacienteId}_{fisioId}"
    match /conversaciones/{convId}/mensajes/{msgId} {
      allow read: if request.auth != null
                   && convId.matches('.*' + request.auth.uid + '.*');
      allow create: if request.auth != null
                     && convId.matches('.*' + request.auth.uid + '.*')
                     && request.resource.data.emisorId == request.auth.uid;
      allow update, delete: if false;
    }

    // Informes: solo lectura del propio paciente
    match /informes/{informeId} {
      allow read: if request.auth != null
                   && resource.data.pacienteId == request.auth.uid;
      allow write: if false;
    }

  }
}
```

---

## 6. Estructura de datos Firestore

### Colección `pacientes/{uid}`
```json
{
  "uid": "firebase_uid",
  "nombre": "Ana",
  "apellidos": "García López",
  "dni": "12345678A",
  "email": "12345678a@rehapp.paciente",
  "fechaNacimiento": Timestamp,
  "deporte": "Atletismo",
  "tipoLesion": "Rodilla derecha",
  "sexo": "F",
  "primerLogin": true,
  "planActivo": "planId_ref",
  "fisioId": "fisio_uid",
  "creadoEn": Timestamp
}
```

### Colección `planes/{planId}`
```json
{
  "pacienteId": "firebase_uid",
  "fisioId": "fisio_uid",
  "faseRecuperacion": "Fase aguda",
  "duracionSemanas": 16,
  "semanaActual": 1,
  "activo": true,
  "objetivosTerapeuticos": "Recuperar rango de movimiento...",
  "creadoEn": Timestamp
}
```

### Subcolección `planes/{planId}/semanas/{semanaId}`
```json
{
  "numeroSemana": 1,
  "dias": {
    "lunes": [
      {
        "nombre": "Extensión de rodilla",
        "tipo": "Fortalecimiento",
        "series": 3,
        "repeticiones": 10,
        "cargaKg": 5.0,
        "frecuenciaSemana": 3,
        "urlVideo": "https://...",
        "descripcion": "Sentado en silla..."
      }
    ],
    "martes": [],
    "miercoles": [...],
    "jueves": [],
    "viernes": [...],
    "sabado": [],
    "domingo": []
  }
}
```

### Colección `registros/{registroId}`
```json
{
  "pacienteId": "firebase_uid",
  "planId": "planId_ref",
  "semana": 1,
  "dia": "lunes",
  "fecha": Timestamp,
  "sesionCompletada": true,
  "seriesCompletadas": 3,
  "repsCompletadas": 10,
  "eva": 3,
  "rpe": 12,
  "fatiga": 6,
  "notas": "Ligera molestia en la rodilla",
  "creadoEn": Timestamp
}
```

### Colección `alertas/{alertaId}`
```json
{
  "pacienteId": "firebase_uid",
  "tipo": "EVA_ALTO",
  "titulo": "Dolor elevado detectado",
  "descripcion": "Has registrado un dolor de 8/10...",
  "leida": false,
  "urgente": true,
  "creadoEn": Timestamp
}
```

### Colección `conversaciones/{convId}/mensajes/{msgId}`
```
convId = "{pacienteId}_{fisioId}"
```
```json
{
  "texto": "Hola, tengo una duda...",
  "emisorId": "firebase_uid",
  "esPaciente": true,
  "timestamp": Timestamp
}
```

### Colección `informes/{informeId}`
```json
{
  "pacienteId": "firebase_uid",
  "titulo": "Informe semana 4",
  "urlPdf": "https://firebasestorage.googleapis.com/...",
  "comentarioFisio": "Buena evolución...",
  "fechaGeneracion": Timestamp
}
```

---

## 7. Firebase Storage

Para los PDFs de informes y vídeos de ejercicios:

**Reglas de Storage:**
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Vídeos de ejercicios (lectura para pacientes autenticados)
    match /videos/{videoId} {
      allow read: if request.auth != null;
      allow write: if false;
    }
    // Informes PDF (solo el paciente propietario)
    match /informes/{pacienteId}/{informeId} {
      allow read: if request.auth != null && request.auth.uid == pacienteId;
      allow write: if false;
    }
  }
}
```

---

## 8. Activar Cloud Messaging (FCM)

1. Firebase Console → **Cloud Messaging**
2. No requiere configuración adicional; el servicio `RehappMessagingService`
   gestiona automáticamente el token y los canales de notificación.

---

## 9. Crear el primer paciente (proceso manual del fisio)

1. En Firebase Authentication → Añadir usuario:
   - Email: `{DNI_MAYUSCULAS}@rehapp.paciente`
   - Contraseña temporal: se comunicará al paciente
2. En Firestore → colección `pacientes` → nuevo documento con ID = uid generado
3. Establecer `primerLogin: true` para forzar cambio de contraseña

---

## ⚠️ Consideraciones de seguridad (RGPD)

- Todos los datos se almacenan en región europea (`europe-west1`)
- Las contraseñas se gestionan exclusivamente por Firebase Auth (no se almacenan)
- Las sesiones tienen timeout de 30 minutos (gestionado por `SessionManager`)
- Los datos sensibles en el dispositivo se cifran con `EncryptedSharedPreferences` (AES256-GCM)
- `allowBackup="false"` en el manifest impide copias de seguridad automáticas
- `usesCleartextTraffic="false"` fuerza HTTPS en todas las comunicaciones
