# AMUKISENSE - Monitoreo Colaborativo de Ruido Urbano

App Android para registrar y monitorear contaminación acústica en tiempo real. Construida con Kotlin, Jetpack Compose y Firebase.

## 🎯 Funcionalidades Principales

### Autenticación
- Login con email/contraseña y Google Sign-In
- Registro de usuarios con validación
- Recuperación de contraseña
- Sesión persistente

### Captura de Datos
- **Registro Manual**: Medición de dB en tiempo real con micrófono + GPS
- **Quejas Ciudadanas**: Reporte de fuentes de ruido con descripción e impacto
- **Captura Fotográfica**: Evidencia visual + medición simultánea de ruido
- **Registro Automático**: Tracking de ruido durante desplazamiento (200/500/1000m)

### Visualización
- **Mapa Interactivo**: Mediciones geocalizadas con indicadores de nivel
- **Mapa de Exploración**: Vista de quejas y calles ruidosas
- **Dashboard Comunitario**: Estadísticas por distrito, evolución temporal y heatmap horario

### Gestión de Datos
- **Mis Registros**: Lista, edita y elimina tus mediciones (manuales, automáticos, capturas)
- **Mis Quejas**: Gestión completa de reportes de ruido
- **Estadísticas Personales**: Resumen de aportes individuales y ranking comunitario

## 🏗️ Arquitectura del Proyecto

```
app/src/main/java/com/example/amukisenseapp/
├── data/
│   ├── model/           # Modelos de datos (Usuario, Registro, Config, etc.)
│   └── repository/      # Repositorios para Firebase (Auth y Firestore)
├── navigation/          # Configuración de Navigation Compose
├── ui/
│   ├── screens/         # Pantallas de la aplicación
│   ├── theme/           # Tema y colores de Material3
│   └── viewmodel/       # ViewModels para manejo de estado
└── MainActivity.kt      # Actividad principal
```

## 🛠️ Stack Tecnológico

- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI declarativa moderna
- **Material Design 3** - Componentes y theming
- **Architecture Components**:
  - ViewModel (estado y lógica de negocio)
  - StateFlow (flujos reactivos)
  - Navigation Compose
  - Lifecycle (gestión de ciclo de vida)
- **Firebase**:
  - Authentication (Email + Google OAuth)
  - Firestore (base de datos NoSQL)
  - Storage (imágenes)
- **Google Services**:
  - Maps SDK (visualización geoespacial)
  - Location Services (FusedLocationProviderClient)
  - Geocoding (direcciones legibles)
- **Accompanist** - Permisos runtime
- **CameraX** - Captura de fotos
- **Coil** - Carga eficiente de imágenes
- **Geohash** - Indexación espacial (búsquedas por proximidad)

## ⚙️ Configuración del Proyecto

### 1. Requisitos Previos
- Android Studio Hedgehog o superior
- JDK 11 o superior
- Cuenta de Firebase con proyecto configurado
- SDK de Android con nivel mínimo 24 (Android 7.0)

### 2. Configuración de Firebase

#### a) Crear proyecto en Firebase Console
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un nuevo proyecto o usa uno existente
3. Añade una aplicación Android con package name: `com.example.amukisenseapp`

#### b) Configurar Authentication
1. En Firebase Console, ve a **Authentication**
2. Habilita **Email/Password**
3. Habilita **Google Sign-In**
4. Para Google Sign-In, obtén el SHA-1 de tu keystore:
   ```powershell
   cd android
   .\gradlew signingReport
   ```
5. Añade el SHA-1 en la configuración de la app en Firebase

#### c) Configurar Firestore Database
1. Ve a **Firestore Database** en Firebase Console
2. Crea la base de datos en modo **producción**
3. Configura las reglas de seguridad (ver sección más abajo)

#### d) Configurar Storage
1. Ve a **Storage** en Firebase Console
2. Habilita Storage con reglas por defecto
3. Ajusta reglas de seguridad (ver sección más abajo)

#### e) Descargar google-services.json
1. En la configuración del proyecto Firebase, descarga `google-services.json`
2. Colócalo en `app/google-services.json` (ya existe en el proyecto, reemplázalo con el tuyo)

### 3. Reglas de Seguridad Firebase

#### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Usuarios: solo lectura pública, escritura del propietario
    match /usuarios/{userId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Registros: lectura pública, escritura autenticada
    match /registros/{registroId} {
      allow read: if true;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null && 
                               resource.data.uid_usuario == request.auth.uid;
    }
    
    // Lugares: lectura pública
    match /lugares/{lugarId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

#### Storage Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /imagenes_usuarios/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 4. Compilar y Ejecutar

```powershell
# Sincronizar dependencias
.\gradlew build

# Instalar en dispositivo/emulador
.\gradlew installDebug

# O desde Android Studio: Run > Run 'app'
```

## 📱 Permisos Requeridos

La aplicación solicita los siguientes permisos en runtime:

- **CAMERA**: Para captura de fotos en registro con captura
- **RECORD_AUDIO**: Para medir nivel de decibelios
- **ACCESS_FINE_LOCATION**: Para obtener coordenadas GPS de las mediciones

## 🗄️ Estructura de Base de Datos Firestore

### Colección `usuarios`
```javascript
usuarios/{uid} {
  nombre_usuario: String,
  correo: String,
  provider: String,              // "email" | "google"
  fecha_union: Timestamp,
  foto_perfil_url: String?,
  numero_usuario: Number?,
  config: {
    notificaciones: Boolean,
    registro_automatico: Boolean,
    distancia_metros: Number,    // 200, 500, 1000
    auto_registro_activado: Boolean
  },
  stats: {
    registros_manual: Number,
    registros_auto: Number,
    quejas: Number
  }
}
```

### Colección `registros`
```javascript
registros/{id_registro} {
  uid_usuario: String,
  tipo: String,                   // "manual" | "auto" | "queja" | "captura"
  db: Number,
  coordenadas: {
    lat: Number,
    lng: Number
  },
  geohash: String?,               // Índice espacial (precisión 9)
  tipo_lugar: String?,            // "Parque" | "Calle" | "Casa" | etc.
  sensacion: String?,             // "Tranquilo" | "Molesto" | "Insoportable"
  comentario: String?,
  origen_ruido: String?,          // Solo quejas
  impacto: String?,               // Solo quejas
  fecha: Timestamp,
  imagen_url: String?,            // URL de Storage
  direccion: String?,             // Geocodificación inversa
  auto_generado: Boolean,
  distancia_m: Number?,           // Distancia desde último registro auto
  visible_publico: Boolean
}
```

## 🎨 Assets e Iconos

Los iconos deben colocarse en la carpeta `app/src/main/res/drawable/`. Los iconos exportados desde Figma están en:
```
C:\Users\User\Downloads\ICONOS
```

Cópialos manualmente a la carpeta `res/drawable` y ajusta los nombres si es necesario.

## 📱 Navegación

```
Login/Registro
    ↓
Mapa (Home)
    ├── Registro Manual (FAB)
    ├── Registro con Captura
    └── Registro de Queja
    
Bottom Navigation:
├── Mapa Medidas (marcadores de registros)
├── Mapa Explorar (quejas y calles)
├── Estadísticas
│   ├── Mis Stats (perfil + contadores)
│   ├── Mis Registros (CRUD completo)
│   ├── Mis Quejas (CRUD completo)
│   └── Estadísticas Todos (ranking comunitario)
├── Comunidad
│   ├── Dashboard (gráficos analíticos)
│   └── Miembros Unidos (total usuarios)
└── Configuración
    ├── Perfil
    ├── Notificaciones
    ├── Registro Automático
    └── Cerrar sesión
```

## 📝 Notas Técnicas

### ✅ Captura Real de Audio del Micrófono (IMPLEMENTADO)

La aplicación implementa captura **REAL** de audio usando la clase `AudioRecorder` que utiliza `AudioRecord` de Android para medir decibelios en tiempo real.

#### Implementación técnica:

**Archivo**: `app/src/main/java/com/example/amukisenseapp/util/AudioRecorder.kt`

```kotlin
// Configuración de audio
- Sample Rate: 44100 Hz (frecuencia estándar de audio)
- Channel: MONO (suficiente para medir amplitud)
- Encoding: PCM 16 bits (valores de -32768 a 32767)
- Buffer Size: Calculado dinámicamente según el dispositivo

// Proceso de medición
1. AudioRecord captura muestras del micrófono cada 500ms
2. Se calcula el RMS (Root Mean Square) de las muestras
   Formula: RMS = sqrt( sum(sample²) / n )
3. Se convierte RMS a decibelios
   Formula: dB = 20 * log10(RMS / 32767) + 90
4. Se limita el rango a 30-120 dB (rango realista)
```

#### Pantallas con captura real activa:

| Pantalla | Estado | Rango dB | Función |
|----------|--------|----------|---------|
| **RegistroManualScreen** | ✅ Implementado | 30-120 dB | Medición estándar de ruido ambiental |
| **RegistroQuejaScreen** | ✅ Implementado | 30-120 dB | Documentar quejas de ruido excesivo |
| **RegistroCapturaScreen** | ✅ Implementado | 30-120 dB | Medición + foto simultánea |

#### Código de uso:

```kotlin
// Las pantallas usan AudioRecorder así:
val audioRecorder = remember { AudioRecorder() }

LaunchedEffect(Unit) {
    if (permissionsState.allPermissionsGranted) {
        audioRecorder.startRecording().collect { dbValue ->
            viewModel.updateDb(dbValue) // Actualiza la UI
        }
    }
}

DisposableEffect(Unit) {
    onDispose {
        audioRecorder.stopRecording() // CRÍTICO: liberar recursos
    }
}
```

#### Importante:

⚠️ **Calibración**: La conversión a dB es una aproximación matemática. Para mediciones profesionales certificadas se requeriría:
- Calibración con sonómetro de referencia clase I/II
- Compensación por características del micrófono del dispositivo
- Aplicación de curvas de ponderación (A-weighting, C-weighting) según norma IEC 61672
- Consideración de la respuesta en frecuencia del micrófono

✅ **Para este proyecto**: La implementación actual es suficiente para:
- Comparaciones relativas entre mediciones
- Identificación de patrones de ruido
- Registro de quejas ciudadanas
- Visualización de zonas ruidosas en el mapa

### Ubicación GPS
La ubicación se obtiene mediante `FusedLocationProviderClient`. En el código actual se simula; para implementar:

```kotlin
val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
fusedLocationClient.lastLocation.addOnSuccessListener { location ->
    viewModel.updateLocation(location)
}
```

### Captura de Fotos
Se usa `ActivityResultContracts.TakePicture()` para capturar fotos. Las imágenes se comprimen antes de subir a Firebase Storage para optimizar almacenamiento.

## 🧪 Pruebas Funcionales

### Flujo básico
1. Registra una cuenta nueva (email + contraseña)
2. Activa permisos de ubicación y micrófono
3. En el mapa, presiona el FAB y selecciona "Registro Manual"
4. Observa la medición en tiempo real del micrófono
5. Completa el formulario y guarda
6. Verifica en "Mis Registros" que aparece el nuevo registro
7. Edita o elimina el registro desde la lista

### Verificación en Firebase
- **Firestore**: Revisa que el documento se creó en `registros/{id}`
- **Contadores**: Valida que `usuarios/{uid}.stats.registros_manual` incrementó
- **Storage**: Si hay imagen, verifica en `imagenes_usuarios/{uid}/`

## 🐛 Solución de Problemas

### Error: "google-services.json not found"
- Asegúrate de que `google-services.json` esté en `app/google-services.json`
- Sincroniza el proyecto con Gradle

### Error: "PERMISSION_DENIED: Missing or insufficient permissions"
- Revisa las reglas de seguridad en Firestore
- Verifica que el usuario esté autenticado

### Error en Google Sign-In: "DEVELOPER_ERROR"
- Verifica que el SHA-1 esté configurado en Firebase Console
- Asegúrate de que `google-services.json` sea el correcto

### Permisos no solicitados
- En Android 6.0+, los permisos deben solicitarse en runtime
- La app usa Accompanist Permissions para esto

## 🗺️ Roadmap

### ✅ Completado
- Sistema de autenticación dual (email + Google)
- Captura real de audio con AudioRecord (30-120 dB)
- GPS tracking con FusedLocationProviderClient
- Mapas interactivos (Google Maps SDK)
- CRUD completo de registros y quejas
- Dashboard con gráficos analíticos (quejas por distrito, heatmap horario)
- Filtrado geoespacial (radio 1km con Haversine)
- Sistema de estadísticas personales y comunitarias

### 🔄 En desarrollo
- Clustering de marcadores en el mapa
- Notificaciones push para alertas de ruido
- Exportación de reportes (PDF/CSV)
- Modo offline con sincronización

### 📋 Backlog
- Machine learning para clasificación automática de fuentes de ruido
- Integración con APIs municipales
- Sistema de gamificación y badges
- Modo oscuro

## 🤝 Contribuir

Este es un proyecto académico abierto a mejoras. Si encuentras un bug o tienes una sugerencia:

1. Abre un issue describiendo el problema
2. Fork el repo y crea tu rama (`git checkout -b fix/mi-fix`)
3. Commitea tus cambios (`git commit -m 'Fix: descripción'`)
4. Push a tu fork (`git push origin fix/mi-fix`)
5. Abre un Pull Request

### Convenciones de código
- Kotlin style guide oficial
- Compose best practices
- Nombres descriptivos en español (UI) e inglés (código)
- ViewModels con StateFlow para UI reactiva
- Repository pattern para acceso a datos

## 📄 Licencia

MIT License - Proyecto educativo de código abierto

---

**Tech Stack:** Kotlin • Jetpack Compose • Firebase • Google Maps
