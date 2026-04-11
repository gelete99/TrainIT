# TrainIT

TrainIT es una aplicación Android de entrenamiento personal orientada al entrenamiento autónomo asistido por IA.

Permite al usuario registrar entrenamientos manualmente, consultar su historial, gestionar su perfil y generar planes semanales personalizados a partir de sus datos y su actividad previa. La aplicación utiliza Firebase como backend y persistencia en la nube.

## Características principales

* Registro e inicio de sesión de usuarios.
* Gestión del perfil deportivo del usuario.
* Registro manual de entrenamientos.
* Consulta del historial de entrenamientos.
* Generación de planes semanales personalizados mediante IA.
* Persistencia del plan y de los datos del usuario en Firebase.
* Interfaz desarrollada con Jetpack Compose.

## Tecnologías utilizadas

### App Android

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose

### Backend y persistencia

* Firebase Authentication
* Firebase Firestore
* Firebase Cloud Functions

### IA

* OpenAI API gpt-4.1-mini desde Firebase Functions

## Instalación

### Requisitos previos

* Android Studio
* JDK 11
* Node.js 22
* Firebase CLI
* Un proyecto de Firebase configurado

### Ejecutar la app

1. Clona el repositorio.
2. Abre el proyecto en Android Studio.
3. Añade el archivo `app/google-services.json`.
4. Sincroniza Gradle.
5. Ejecuta la aplicación en un emulador o dispositivo Android.

## Configuración de Firebase

Para que el proyecto funcione correctamente, es necesario configurar Firebase.

1. Crea un proyecto en Firebase.
2. Registra la aplicación Android.
3. Descarga el archivo `google-services.json`.
4. Coloca el archivo en:
   `app/google-services.json`
5. Activa los servicios:

    * Firebase Authentication
    * Firestore Database
    * Cloud Functions

## Configuración de Functions

La generación del plan semanal se realiza desde Firebase Functions.

Desde la carpeta `functions`:

```
npm install
npm run build
```

Si vas a desplegar funciones:

```
firebase login
firebase functions:secrets:set OPENAI\_API\_KEY
firebase deploy --only functions
```

## Arquitectura

TrainIT sigue una arquitectura modular sencilla basada en tres partes:

* **UI**: pantallas desarrolladas con Jetpack Compose.
* **Repositorios**: capa de acceso a datos y comunicación con Firebase.
* **Backend**: Firebase Authentication, Firestore y Cloud Functions.



## Estructura general del proyecto

```

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

## Pruebas unitarias

El proyecto incluye pruebas unitarias básicas.

Para ejecutarlas:

```
./gradlew testDebugUnitTest
```

Para ejecutar pruebas de UI:

```
./gradlew connectedDebugAndroidTest
```

## Notas de configuración

Se recomienda no subir al repositorio archivos locales o sensibles como:

* `.env`
* `local.properties`
* `.idea/`
* `app/google-services.json`

## Estado del proyecto

Este proyecto ha sido desarrollado como Trabajo de Fin de Grado (TFG).

## Autor

Ángel Fernández Aguilar

