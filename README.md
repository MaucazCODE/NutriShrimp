# 🦐 NutriShrimp

Aplicación Android para el control y monitoreo remoto de un **alimentador automático de camarón**, conectada al equipo por **Firebase Realtime Database**. Proyecto de titulación — **ESPOL**.

---

## 📱 Qué hace

La app es el lado de mando de un alimentador flotante. El firmware del equipo publica sus sensores en Firebase y escucha las órdenes que la app escribe en esa misma base, así que todo el diálogo entre teléfono y equipo pasa por unos pocos nodos.

### 📊 Monitoreo

Lectura en vivo de los tres sensores del estanque:

| Sensor | Nodo Firebase | Vista |
|---|---|---|
| 🌡️ Temperatura | `temperatura` | `Termometro` |
| 🧪 pH | `ph` | Barra de nivel |
| 🔊 Nivel de sonido | `nivelSonido` | `BarChartView` |

El muestreo del histórico corre en `HistorialMuestras` y arranca desde `AlimentadorApp` (al iniciar la app, no al abrir la vista), de modo que la pantalla de registro siempre encuentra datos ya acumulados.

### 🗺️ Mapa GPS y ruta automática

La pantalla **Mapa GPS** usa OpenStreetMap. Toca el mapa para añadir puntos, consultar el marcador `actual` en tiempo real y, al activar el modo automático, enviar en una sola actualización de Firebase:

```text
ruta: [{ latitud: <number>, longitud: <number> }, ...]
automatico: true
```

La ubicación actual se lee desde `actual` con el formato `{ latitud: <number>, longitud: <number> }`. Se requiere conexión a Internet para cargar los mosaicos del mapa.

### 🎮 Control

Dos ramas, separadas por `opcControlActivity`:

**🍤 Alimentación** — enciende los motores de descarga, regula el `pwm` y dispara el aspersor.

| Orden | Nodo |
|---|---|
| Motores de alimentación | `motores` |
| Velocidad | `pwm` |
| Aspersor | `aspersor` |
| Paro de emergencia | `paro` |

**🕹️ Navegación** — mando direccional de cuatro sectores (`DPadView`) para mover el equipo por el estanque.

| Dirección | Nodo |
|---|---|
| Adelante | `nav/adelante` |
| Atrás | `nav/atras` |
| Giro horario | `nav/ho` |
| Giro antihorario | `nav/aho` |

Las cuatro banderas se escriben **siempre juntas** con `updateChildren`, así que por construcción nunca puede haber dos direcciones activas a la vez: la orden nueva y el apagado de las demás viajan en la misma escritura. 🛡️

### 🛑 Paro de emergencia

El botón PARO no solo detiene la navegación: apaga las cuatro direcciones, los motores de alimentación y pone `pwm` a cero, todo en una única escritura para que no quede nada a medias. Además, `controlNavActivity` suelta el mando en `onStop()` — si la vista deja de verse (back, home, pantalla apagada) no puede quedarse una orden viva moviendo el equipo.

### 📈 Registro

`RegistroAlimentacion` pesa cada descarga por el tiempo que los motores estuvieron encendidos, y también arranca desde `AlimentadorApp`: si solo escuchara mientras su vista está visible, se perdería el final de casi todas las descargas.

---

## 🗺️ Flujo de pantallas

```
MainActivity (login)
   ├── crearUsuarioActivity
   └── opcionesActivity
         ├── monitoreoActivity ......... sensores en vivo + registroActivity
         ├── mapaGpsActivity ........... ruta GPS + modo automático
         └── opcControlActivity
               ├── controlActivity ..... alimentación, PWM, aspersor
               └── controlNavActivity .. mando direccional + PARO
```

---

## 🧱 Estructura

```
app/src/main/java/com/example/app_alimentador/
├── AlimentadorApp.java ......... Application: arranca los seguimientos persistentes
├── MainActivity.java ........... Login
├── crearUsuarioActivity.java ... Alta de usuario
├── opcionesActivity.java ....... Monitoreo o control
├── mapaGpsActivity.java ........ Ruta GPS y coordenada actual en vivo
├── monitoreoActivity.java ...... Sensores en vivo
├── registroActivity.java ....... Histórico y gráficas
├── opcControlActivity.java ..... Alimentación o navegación
├── controlActivity.java ........ Motores, PWM, aspersor
├── controlNavActivity.java ..... Mando direccional y PARO
├── progActivity.java ........... Programación de ciclos (maqueta)
│
├── HistorialMuestras.java ...... Muestreo del histórico
├── RegistroAlimentacion.java ... Bitácora de descargas
├── Lecturas.java ............... Lectura de Firebase tolerante al tipo
│
├── DPadView.java ............... Mando direccional de 4 sectores
├── BarChartView.java ........... Barras
├── AlimentacionChartView.java .. Gráfica de alimentación
├── TablaMuestras.java .......... Tabla del histórico
├── Termometro.java ............. Termómetro
└── Aspersion.java .............. Indicador de aspersión
```

---

## 🔧 Compilar

```bash
./gradlew assembleDebug
```

| | |
|---|---|
| ☕ Java | 11 |
| 🤖 minSdk / targetSdk | 24 / 35 |
| 🐘 Gradle | 8.11.1 |
| 🔥 Backend | Firebase Realtime Database |

Requiere dos archivos que **no se versionan**, por contener configuración local o credenciales:

- 🔑 `app/google-services.json` — configuración del proyecto de Firebase. Se descarga desde la consola de Firebase, en los ajustes del proyecto.
- 📂 `local.properties` — ruta al SDK de Android (`sdk.dir=...`). Android Studio lo genera solo.

---

## ⚠️ Estado y limitaciones conocidas

- 🔐 **El login es de demostración.** Los usuarios viven en un `HashMap` estático en `MainActivity`, con credenciales fijas en el código. No hay autenticación real ni persistencia: los usuarios creados se pierden al cerrar el proceso.
- ⏰ **`progActivity` es una maqueta.** Recoge cantidad, intervalo y hora de inicio, pero no escribe en Firebase ni arma temporizador alguno — "Programar" solo confirma por Toast.
- 🌗 **La app no tiene tema oscuro.** Los layouts fijan a mano la superficie clara, así que la variante `values-night` se ancla al tema Light a propósito: con el padre `DayNight`, un dispositivo en modo oscuro repintaba el texto de blanco sobre el fondo claro y los controles quedaban invisibles.
- 🧪 **Lectura tolerante al tipo.** `Lecturas` no asume el tipo de los nodos porque el firmware puede escribir enteros, decimales o cadenas; `getValue(Integer.class)` lanzaba `DatabaseException` en los dos últimos casos.

---

## ☁️ Nota sobre el historial

Este repositorio se creó a partir de una copia limpia del proyecto, que hasta entonces vivía en iCloud Drive. iCloud había generado copias en conflicto (`X 2.java`, `X(1).java`) y dejado varios archivos en estado *deshidratado* e irrecuperable, al punto de que faltaban `controlNavActivity.java` y `res/values/themes.xml` y el proyecto no compilaba. 🧹

**Trabajar aquí, en local — no dentro de la carpeta sincronizada.**
