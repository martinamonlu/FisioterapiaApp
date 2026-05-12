# Rehapp — Aplicación de Fisioterapia y Rehabilitación

Aplicación Android de seguimiento fisioterapéutico enfocado en rehabilitación de lesiones deportivas.
Permite al fisioterapeuta gestionar pacientes y crear planes de ejercicio personalizados, mientras que el paciente puede seguir su rutina diaria, registrar cada sesión y comunicarse con su fisio en tiempo real.

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | XML Views + ViewBinding + Material 3 |
| Arquitectura | MVVM (paciente y fisio) |
| Autenticación | Firebase Authentication |
| Base de datos | Cloud Firestore |
| Almacenamiento | Firebase Storage |
| Notificaciones | Firebase Cloud Messaging (FCM) |
| Vídeo | ExoPlayer (Media3) |
| Gráficas | MPAndroidChart |
| Cifrado local | EncryptedSharedPreferences (AES-256) |

---

## Requisitos previos

- **Android Studio** Hedgehog (2023.1.1) o superior
- **JDK 17**
- **Android SDK** API 26+ (minSdk 26, targetSdk 35)
- Cuenta de **Firebase** con proyecto activo
- Conexión a internet durante la primera ejecución

---

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/<usuario>/FisioterapiaApp.git
cd FisioterapiaApp
```

### 2. Crear el proyecto Firebase

1. Ve a [console.firebase.google.com](https://console.firebase.google.com)
2. Crea un proyecto nuevo → **Rehapp**
3. Añade una app Android con package name: `com.example.fisioterapiaapp`
4. Descarga el fichero `google-services.json` y colócalo en `app/`

### 3. Activar servicios Firebase

| Servicio | Configuración |
|---|---|
| **Authentication** | Activa el proveedor *Correo electrónico/contraseña* |
| **Firestore** | Crea la base de datos en modo producción, región `europe-west1` |
| **Storage** | Activa con la región por defecto |
| **Cloud Messaging** | No requiere configuración adicional |

### 4. Reglas de seguridad Firestore

En **Firestore → Reglas**, pega y publica:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function esFisio() {
      let u = get(/databases/$(database)/documents/usuarios/$(request.auth.uid));
      return u.data.tipo == 'fisio' && u.data.aprobado == true;
    }

    match /usuarios/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    match /pacientes/{docId} {
      allow read: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        resource.data.fisioterapeutaId == request.auth.uid
      );
      allow create: if request.auth != null && esFisio();
      allow update: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        resource.data.fisioterapeutaId == request.auth.uid
      );
      allow delete: if false;
    }

    match /planes_ejercicio/{planId} {
      allow read: if request.auth != null && (
        resource.data.fisioterapeutaId == request.auth.uid ||
        resource.data.pacienteId == request.auth.uid
      );
      allow create, update: if request.auth != null && esFisio();
      allow delete: if false;
    }

    match /registros_sesion/{registroId} {
      allow read:   if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if false;
    }

    match /alertas/{alertaId} {
      allow read: if request.auth != null && (
        resource.data.pacienteId == request.auth.uid || esFisio()
      );
      allow update: if request.auth != null &&
        resource.data.pacienteId == request.auth.uid &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly(['leida']);
      allow create: if request.auth != null;
      allow delete: if false;
    }

    match /conversaciones/{convId}/mensajes/{msgId} {
      allow read: if request.auth != null &&
                     convId.matches('.*' + request.auth.uid + '.*');
      allow create: if request.auth != null &&
                       convId.matches('.*' + request.auth.uid + '.*') &&
                       request.resource.data.emisorId == request.auth.uid;
      allow update, delete: if false;
    }

    match /informes/{informeId} {
      allow read: if request.auth != null && (
        resource.data.pacienteId == request.auth.uid ||
        resource.data.fisioterapeutaId == request.auth.uid
      );
      allow create: if request.auth != null && esFisio();
      allow update, delete: if false;
    }
  }
}
```

### 5. Reglas de Storage

En **Storage → Reglas**, pega y publica:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /videos_ejercicios/{allPaths=**} {
      allow read:  if request.auth != null;
      allow write: if request.auth != null;
    }
    match /informes/{pacienteId}/{fileName} {
      allow read:  if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### 6. Crear el primer fisioterapeuta

1. En la app, pulsa **"Soy fisioterapeuta"** → **Registrarse**
2. Rellena todos los campos y crea la cuenta
3. En **Firebase Console → Firestore → colección `usuarios`**, abre el documento del fisio recién creado y cambia `aprobado: false` a `aprobado: true`
4. Ya puedes iniciar sesión como fisio

### 7. Abrir en Android Studio

```
File → Open → selecciona la carpeta FisioterapiaApp
```

Espera a que termine el Gradle sync y pulsa **Run** (▶).

---

## Flujo principal de la aplicación

```
MainActivity
├── Fisioterapeuta
│   ├── Registrar paciente (crea cuenta provisional con DNI como contraseña)
│   ├── Crear / editar plan de ejercicios (con vídeos demostrativos)
│   ├── Ver detalle del paciente → ejercicios, progreso, calendario
│   ├── Chat en tiempo real con el paciente
│   └── Configuración (notificaciones, umbral adherencia, idioma, credenciales)
│
└── Paciente
    ├── Primer login: activa cuenta + cambia contraseña obligatoria
    ├── Dashboard diario: ejercicios de hoy con check inline (series, reps, dolor EVA)
    ├── Plan completo semanal con detalle de cada ejercicio y vídeo
    ├── Progreso: gráficas de dolor y esfuerzo semana a semana
    ├── Chat con el fisioterapeuta
    └── Perfil: datos personales e informes generados
```

---

## Estructura del proyecto

```
app/src/main/java/com/example/fisioterapiaapp/
│
├── fisio/                          # Módulo del fisioterapeuta
│   ├── FisioMainActivity           # Contenedor con bottom navigation
│   ├── FisioPacientesFragment      # Lista de pacientes
│   ├── FisioAddPacienteFragment    # Registrar nuevo paciente
│   ├── FisioPerfilFragment         # Perfil del fisio
│   ├── FisioConfigFragment         # Configuración (notifs, umbral, idioma...)
│   ├── ChatFisioActivity           # Chat fisio ↔ paciente
│   └── ChatFisioAdapter            # Adapter del chat (burbujas invertidas)
│
├── paciente/
│   ├── ui/
│   │   ├── auth/
│   │   │   ├── SignInPacienteActivity    # Login del paciente
│   │   │   └── CambiarPasswordActivity  # Cambio obligatorio en primer login
│   │   ├── dashboard/
│   │   │   ├── DashboardFragment        # Ejercicios de hoy + gráfica
│   │   │   └── PacienteMainActivity     # Contenedor bottom nav paciente
│   │   ├── exercise/
│   │   │   └── EjercicioDetalleActivity # Detalle + registro inline de sesión
│   │   ├── chat/
│   │   │   ├── ChatFragment             # Chat del paciente
│   │   │   └── ChatAdapter              # Adapter burbujas
│   │   ├── perfil/
│   │   │   ├── PerfilFragment           # Datos + informes del paciente
│   │   │   └── InformesAdapter
│   │   ├── plan/
│   │   │   └── PlanFragment             # Plan semanal completo
│   │   ├── progreso/
│   │   │   └── ProgresoFragment         # Gráficas de evolución
│   │   └── registro/
│   │       └── RegistroSesionActivity   # Registro post-sesión (RPE, fatiga)
│   ├── model/
│   │   └── Models.kt                    # Paciente, Plan, EjercicioPlan, MensajeChat...
│   ├── repository/
│   │   └── PacienteRepository.kt        # Acceso a Firestore + corrutinas
│   ├── viewmodel/
│   │   ├── AuthPacienteViewModel.kt
│   │   ├── DashboardPacienteViewModel.kt
│   │   └── ViewModels.kt
│   └── util/
│       ├── SessionManager.kt            # Sesión cifrada + timeout 30 min
│       ├── Extensions.kt
│       └── RehappMessagingService.kt    # FCM
│
├── MainActivity.kt                  # Pantalla de bienvenida (fisio / paciente)
├── DetallePacienteActivity.kt       # Vista del fisio sobre un paciente
├── DetalleSemanaActivity.kt         # Detalle semanal del plan (vista fisio)
├── CrearPlanActivity.kt             # Crear / editar plan de ejercicios
├── SignInFisioActivity.kt
└── SignUpFisioActivity.kt
```

---

## Funcionalidades implementadas

- [x] Registro y login de fisioterapeutas con aprobación de administrador
- [x] Creación de pacientes con cuenta provisional (contraseña = DNI)
- [x] Activación de cuenta en el primer login + cambio de contraseña obligatorio
- [x] Creación y edición de planes de ejercicio con vídeos demostrativos
- [x] Vista semanal del plan para el fisio (ejercicios por día)
- [x] Dashboard diario del paciente con registro inline (series, reps, carga, dolor EVA)
- [x] Chat en tiempo real fisio ↔ paciente (Firebase Firestore)
- [x] Gráficas de evolución (dolor EVA y esfuerzo RPE por semana)
- [x] Métricas de adherencia al plan
- [x] Notificaciones push via FCM
- [x] Configuración del fisio (notifs, umbral adherencia, idioma, credenciales)
- [x] Sesión cifrada con timeout de 30 minutos (EncryptedSharedPreferences)
- [x] Cumplimiento RGPD: `allowBackup=false`, tráfico HTTPS, datos en región europea

## Líneas futuras

- [ ] Generación de informes PDF exportables
- [ ] Soporte multiidioma completo (EN/ES)
- [ ] Notificaciones por email
- [ ] Panel de administrador para aprobación de fisios

---

## Consideraciones de seguridad

- Las contraseñas las gestiona exclusivamente Firebase Authentication (no se almacenan en Firestore)
- Los datos se almacenan en servidores de Google en región `europe-west1` (UE)
- La sesión local se cifra con AES-256-GCM mediante `EncryptedSharedPreferences`
- `usesCleartextTraffic="false"` — solo comunicaciones HTTPS
- Los datos de cada paciente solo son accesibles por él mismo y su fisioterapeuta asignado

---

## Licencia

Proyecto académico — Máster Universitario en Ingeniería Informática.
