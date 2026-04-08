# TrainIT

Aplicación Android nativa orientada al entrenamiento autónomo asistido por IA. TrainIT permite registrar entrenamientos manualmente, consultar historial, mantener el perfil del usuario y generar un plan semanal personalizado a partir de sus datos y de su actividad previa.

## Tabla de contenidos

- [Descripción general](#descripción-general)
- [Características principales](#características-principales)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Modelo de datos y Firestore](#modelo-de-datos-y-firestore)
- [Instalación para desarrolladores](#instalación-para-desarrolladores)
- [Configuración de Firebase](#configuración-de-firebase)
- [API de Firebase Functions](#api-de-firebase-functions)
- [Flujo funcional de la aplicación](#flujo-funcional-de-la-aplicación)
- [Comandos útiles](#comandos-útiles)

---

## Descripción general

TrainIT es un proyecto de TFG de Ingeniería Informática que combina:

- **Registro manual de entrenamientos**
- **Persistencia cloud con Firebase**
- **Generación de planes semanales mediante IA**
- **Adaptación de recomendaciones a partir del historial del usuario**
- **Interfaz Android moderna con Jetpack Compose**

La aplicación está pensada como un híbrido entre:

- diario de entrenamiento,
- entrenador virtual,
- generador de planificación personalizada.

---

## Características principales

### Autenticación
- Registro de usuario con email, contraseña y nombre de usuario.
- Inicio de sesión con email o con username.
- Gestión de sesión con Firebase Authentication.

### Perfil de usuario
- Edición de objetivo, nivel, género, altura, peso, edad y días por semana.
- Persistencia de datos en Firestore.
- Control de `onboardingCompleted` para habilitar el flujo principal.

### Entrenamientos
- Registro manual de entrenamientos.
- Campos principales: tipo, duración, RPE, fecha y notas.
- Listado histórico ordenado por fecha descendente.
- Eliminación de entrenamientos.

### Plan semanal con IA
- Generación de plan semanal personalizado.
- Uso del perfil y de los últimos entrenamientos como contexto.
- Respuesta estructurada en JSON.
- Visualización por días con sesiones, recomendaciones y notas de seguridad.
- Posibilidad de añadir días del plan al historial de entrenamientos.

### Experiencia de usuario
- UI declarativa con Jetpack Compose.
- Tema visual personalizado tipo fitness/SaaS.
- Navegación entre Home, Plan, Historial y Perfil.
- Barra inferior + acción rápida para registrar entrenamiento.

---

## Stack tecnológico

### Frontend Android
- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Lifecycle APIs

### Backend y persistencia
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Functions 

### IA
- OpenAI API consumida desde Cloud Functions
- Generación de plan semanal estructurado

### Tooling
- Android Studio
- Gradle Kotlin DSL
- Node.js
- Firebase CLI
- TypeScript

---

## Arquitectura

La solución está organizada en una arquitectura sencilla y modular:

1. **Capa UI**
   - Pantallas Compose.
   - Navegación con `NavHost`.
   - Gestión de estado principalmente desde Compose.
   - En autenticación existe soporte con `AuthViewModel`.

2. **Capa de datos / repositorios**
   - Encapsula acceso a Firebase Auth, Firestore y Cloud Functions.
   - Repositorios principales:
     - `AuthRepository`
     - `UserRepository`
     - `WorkoutRepository`
     - `PlanRepository`
     - `AiRepository`

3. **Backend**
   - Firestore para perfil, historial y planes.
   - Cloud Function callable `generatePlan` para construir el plan con IA.
   - OpenAI como servicio externo de inferencia.


## Estructura del proyecto

TrainIT/
├── app/
│   ├── src/main/java/com/example/trainit/
│   │   ├── auth/
│   │   │   ├── AuthRepository.kt
│   │   │   ├── AuthUiState.kt
│   │   │   └── AuthViewModel.kt
│   │   ├── data/
│   │   │   ├── model/
│   │   │   │   ├── AiPlan.kt
│   │   │   │   ├── UserProfile.kt
│   │   │   │   └── Workout.kt
│   │   │   ├── AiRepository.kt
│   │   │   ├── PlanRepository.kt
│   │   │   ├── UserRepository.kt
│   │   │   └── WorkoutRepository.kt
│   │   ├── nav/
│   │   │   ├── AppNavGraph.kt
│   │   │   └── Routes.kt
│   │   ├── ui/
│   │   │   ├── components/
│   │   │   │   └── AppBottomBar.kt
│   │   │   └── theme/
│   │   │       ├── screens/
│   │   │       │   ├── HomeScreen.kt
│   │   │       │   ├── PlanScreen.kt
│   │   │       │   ├── HistoryScreen.kt
│   │   │       │   ├── ProfileScreen.kt
│   │   │       │   ├── LogWorkoutScreen.kt
│   │   │       │   ├── LoginScreen.kt
│   │   │       │   ├── RegisterScreen.kt
│   │   │       │   ├── OnboardingScreen.kt
│   │   │       │   └── SplashScreen.kt
│   │   │       ├── Color.kt
│   │   │       ├── Theme.kt
│   │   │       └── Type.kt
│   │   └── MainActivity.kt
│   └── build.gradle.kts
├── functions/
│   ├── src/
│   │   └── index.ts
│   └── package.json
├── firebase.json
├── .firebaserc
└── README.md
```

---

## Modelo de datos y Firestore

### Colección principal

users/
└── {uid}
    ├── username
    ├── email
    ├── gender
    ├── level
    ├── goal
    ├── daysPerWeek
    ├── heightCm
    ├── weightKg
    ├── age
    ├── onboardingCompleted
    ├── workouts/
    │   └── {workoutId}
    │       ├── id
    │       ├── type
    │       ├── durationMin
    │       ├── rpe
    │       ├── date
    │       └── notes
    ├── plans/
    │   └── latest
    │       ├── generatedAt
    │       └── plan
    └── planCompletions/
        └── {completionId}


### Colecciones auxiliares


usernames/
└── {usernameLower}
    └── email

ai_usage/
└── {uid}
    ├── lastReset
    └── count


### Modelos principales

#### `UserProfile`
- `username`
- `email`
- `gender`
- `level`
- `goal`
- `daysPerWeek`
- `heightCm`
- `weightKg`
- `age`
- `onboardingCompleted`

#### `Workout`
- `id`
- `type`
- `durationMin`
- `rpe`
- `date`
- `notes`

#### `AiPlan`
- `generatedAt`
- `assessment`
- `recommendations`
- `weeklyPlan`
- `safetyNotes`

#### `DayPlan`
- `day`
- `isTrainingDay`
- `focus`
- `durationMin`
- `targetRpe`
- `notes`
- `session`

#### `Exercise`
- `name`
- `sets`
- `reps`

#### `Assessment`
- `summary`
- `strengths`
- `improvements`

---

## Instalación para desarrolladores

## Requisitos previos

### Android
- Android Studio reciente
- Android SDK configurado
- JDK 11
- Dispositivo Android o emulador

### Backend
- Node.js 22
- npm
- Firebase CLI

Instalación recomendada de Firebase CLI:

```bash
npm install -g firebase-tools
```

---

## Clonado del proyecto

```bash
git clone https://github.com/gelete99/TrainIT.git
cd TrainIT
```

---

## Configuración del módulo Android

1. Abre el proyecto en **Android Studio**.
2. Espera a que Gradle sincronice dependencias.
3. Comprueba que tienes instalado el SDK necesario.
4. Verifica que el archivo `app/google-services.json` corresponde a tu proyecto Firebase.

### Compilación local

Desde Android Studio:
- Abrir proyecto
- Sync Gradle
- Ejecutar en emulador o dispositivo

Desde terminal:

```bash
./gradlew assembleDebug
```

> En Windows usa `gradlew.bat assembleDebug`.

---

## Configuración del backend Functions

Entrar en la carpeta de funciones e instalar dependencias:

```bash
cd functions
npm install
```

Compilar TypeScript:

```bash
npm run build
```

Ejecutar emulador de funciones:

```bash
npm run serve
```

Desplegar funciones:

```bash
npm run deploy
```

---

## Configuración de Firebase

## 1. Crear proyecto en Firebase

1. Crea un proyecto en Firebase Console.
2. Añade una app Android con el package name:

```text
com.example.trainit
```

3. Descarga el archivo `google-services.json`.
4. Colócalo en:

```text
app/google-services.json
```

---

## 2. Habilitar Authentication

En Firebase Console:

- Ve a **Authentication**.
- Habilita el proveedor **Email/Password**.

---

## 3. Habilitar Firestore

1. Crea una base de datos Firestore.
2. Configura reglas adecuadas para desarrollo y producción.
3. Crea índices si Firestore los solicita al usar consultas ordenadas.

### Recomendación de reglas mínimas para desarrollo

> Ajusta estas reglas antes de pasar a producción.

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    match /usernames/{username} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    match /ai_usage/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## 4. Configurar Firebase Functions

Desde la raíz del proyecto:

```bash
firebase login
firebase use <tu-project-id>
```

Si quieres actualizar `.firebaserc` al nuevo proyecto:

```bash
firebase use --add
```

---

## 5. Configurar secreto de OpenAI

La función `generatePlan` requiere el secreto `OPENAI_API_KEY`.

Defínelo así:

```bash
firebase functions:secrets:set OPENAI_API_KEY
```

Después despliega las funciones:

```bash
firebase deploy --only functions
```

---

## 6. Emuladores recomendados para desarrollo

Puedes usar emuladores de Firebase para validar funciones antes de desplegar:

```bash
cd functions
npm run serve
```

Si quieres trabajar también con Auth y Firestore en local, amplía la configuración de emuladores en `firebase.json`.

---

## API de Firebase Functions

Actualmente el backend expone una función principal.

### `generatePlan`

Genera un plan semanal personalizado en base al perfil del usuario y sus últimos entrenamientos.

#### Tipo
- **Callable function** (Firebase Functions v2)

#### Región
- `europe-west1`

#### Autenticación
- Requiere usuario autenticado.

#### Entrada
La llamada actual se realiza sin payload funcional adicional:

```json
{}
```

#### Contexto usado internamente
La función obtiene automáticamente:
- `uid` desde `request.auth`
- perfil desde `users/{uid}`
- historial reciente desde `users/{uid}/workouts`
- contador diario desde `ai_usage/{uid}`

#### Respuesta esperada

```json
{
  "generatedAt": 1710000000000,
  "assessment": {
    "summary": "",
    "strengths": [""],
    "improvements": [""]
  },
  "recommendations": [""],
  "weeklyPlan": [
    {
      "day": "Lunes",
      "isTrainingDay": true,
      "focus": "",
      "durationMin": 45,
      "targetRpe": 7,
      "session": [
        { "name": "", "sets": 3, "reps": "8-10" }
      ],
      "notes": ""
    }
  ],
  "safetyNotes": [""]
}
```

#### Reglas de negocio aplicadas por backend
- El plan debe tener siempre **7 días**.
- Los días van de **Lunes a Domingo**.
- El número de días de entrenamiento debe coincidir con `daysPerWeek`.
- Se aplica limitación diaria de generación por usuario.
- Se usan como contexto un máximo de **10 entrenamientos recientes**.

#### Errores funcionales posibles
- `unauthenticated`: el usuario no ha iniciado sesión.
- `failed-precondition`: el perfil no existe o no está completo.
- `resource-exhausted`: límite diario alcanzado o problema de cuota del proveedor.
- `internal`: error al generar o parsear la respuesta de IA.

#### Ejemplo de consumo desde Android

```kotlin
val functions = FirebaseFunctions.getInstance("europe-west1")
val result = functions
    .getHttpsCallable("generatePlan")
    .call(emptyMap<String, Any>())
    .await()
```

> La persistencia del plan no la realiza la función directamente: el cliente recibe el JSON, lo parsea y después lo guarda en `users/{uid}/plans/latest` mediante `PlanRepository`.

---

## Flujo funcional de la aplicación

### 1. Arranque
- `SplashScreen` comprueba si existe sesión.
- Si no hay sesión, redirige a Login.
- Si hay sesión, verifica `onboardingCompleted`.
- Si el onboarding está completo, entra a Home.

### 2. Onboarding
- El usuario define género, nivel, objetivo, días por semana y métricas físicas.
- Se actualiza el documento `users/{uid}`.

### 3. Registro de entrenamientos
- El usuario registra manualmente sesiones.
- Se guardan en `users/{uid}/workouts`.

### 4. Generación del plan IA
- El cliente llama a `generatePlan`.
- La función lee perfil + últimos entrenamientos.
- OpenAI devuelve un JSON estructurado.
- El cliente parsea la respuesta y guarda el plan como `latest`.

### 5. Consulta y seguimiento
- El plan puede visualizarse por días.
- Se pueden registrar días del plan como entrenamientos completados.
- El historial y estadísticas alimentan futuras recomendaciones.

---

## Comandos útiles

### Android

```bash
./gradlew assembleDebug
./gradlew test
```

### Functions

```bash
cd functions
npm install
npm run build
npm run serve
npm run deploy
npm run logs
```

### Firebase

```bash
firebase login
firebase use <tu-project-id>
firebase deploy --only functions
firebase functions:secrets:set OPENAI_API_KEY
```

---

## Autor 

ANGEL FERNÁNDEZ AGUILAR

Proyecto TFG: **TrainIT**.
