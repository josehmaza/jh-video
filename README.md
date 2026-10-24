# JH VIDEO

Es la app de videos para el radio del Tucson ix35. Hace lo mismo que JH MUSIC (explorador de pendrives, búsqueda, Mi Lista y reproductor), pero con videos. Tiene su propio repositorio, separado de JH MUSIC ([josehmaza/jh-music](https://github.com/josehmaza/jh-music)), para que su código no se mezcle con el de la música.

| Qué | Dónde |
|---|---|
| Maqueta web | `index.html`, `app.js`, `style.css` |
| App nativa Android | `android/` (paquete `com.jh.carvideo`) |
| APK | `JH-VIDEO-v<versión>.apk`, en esta carpeta |
| Compilar | `bash build.sh` (Gradle Wrapper con JDK 11) |
| Versión actual | **1.1.2** (`versionCode 9`), pendiente de probar en el auto |

JH MUSIC y JH VIDEO tienen paquetes distintos (`com.jh.caraudio` y `com.jh.carvideo`), así que se instalan juntas en el radio sin pisarse.

## El radio destino

Se hace para el radio Android genérico del **Hyundai Tucson ix35**, sin botones en el volante (todo por pantalla). La ficha completa y qué implica al desarrollar está en [JH MUSIC · 08 · El radio destino](https://github.com/josehmaza/jh-music/blob/main/Documentacion/08-radio-destino.md). Lo principal:

| Qué | Valor |
|---|---|
| Pantalla | **1024 × 600** a **160 dpi** (1 dp = 1 px), 60 Hz |
| Chip | Autochips **AC8227L**: 4 × Cortex-A7 a ~1,3 GHz (32 bits), GPU **Mali-400**. Gama baja |
| Android | Dice **10.0**, pero es API **27 = Android 8.1** |
| Memoria | eMMC de unos 35 MB/s; Ajustes dice 4 GB de RAM y 64 GB (sin confirmar) |
| Video | No muestra **AV1** (suena con el dibujo de "sin video") |

Diseñar y revisar a 1024 × 600: el emulador de 1280 × 720 engaña.

## Cómo se usa

### La pantalla

```
┌──────┬───────────────────────────────┬──────────────────────────┐
│  00  │ [Librería][Videos][Mi Lista]  │ ↻⤨      💡          ☰    │
│  35  │  Filtros / Buscador           │       VIDEO 16:9         │
│ ●USB │                               │                          │
│  ⌂   │  Lista                        │  Título                  │
│  🔈  │                               │ ⏮ Anterior | Siguiente ⏭ │
│  +   │                               │  Barra de progreso       │
│  −   │                               │                          │
│ ▮▮▮  │  Estado        [▶ Reproducir] │    ⏮      ⏯      ⏭       │
└──────┴───────────────────────────────┴──────────────────────────┘
```

- **Sin barra arriba:** la hora va en grande arriba de la tira de volumen (horas arriba, minutos abajo) con un punto **verde si hay pendrive** (gris si no). **Re-escanear USB** y la versión están en el menú ☰.
- **Tira de volumen (izquierda, 72):** HOME, MUTE, `+`, porcentaje, `−` y barra vertical, con botones grandes (60 × 52) para tocarlos manejando. `+` / `−` cambian de 10 en 10 %.
- **Explorador (centro):** siempre a la vista. Pestañas *Librería*, *Videos* (ícono de pantalla con play) y *Mi Lista*, con su número en una etiqueta chica al lado (sin paréntesis).
- **Reproductor (derecha):** el video en 16:9, de borde a borde y con las esquinas rectas; debajo, el título, las tarjetas anterior / siguiente, la barra de progreso y los controles. No tiene cápsula PLAYING, formato ni línea de artista.

### Pestañas

- **Librería:** el árbol de pendrives y carpetas. Nombre en 21, en 1 renglón que se desliza si es largo; filas de 60 con espacio entre una y otra, ícono de carpeta fijo y poca sangría. Un toque abre o cierra la carpeta y muestra su nombre completo en 2 renglones; **dos toques seguidos** (o el botón ▶) la abren en *Videos*; `+` la agrega a Mi Lista. Cada toque enciende la fila en cian un instante. La carpeta del video que suena muestra un ecualizador en su ▶ (quieto en pausa); la abierta en *Videos*, si no es la que suena, un borde cian fino. Al volver a Librería, la lista se mueve hasta la carpeta que suena.
- **Videos:** los videos de la carpeta activa, con buscador y chips de filtro, igual que *Canciones* en JH MUSIC.
- **Mi Lista:** funciona igual que Mi Mix: el + agrega, se puede reordenar y vaciar, y la pestaña muestra un punto mientras se reproduce. Sus textos son "vistos", "Ya visto" y "Fin de Mi Lista".

### Reproductor y controles

- **Anterior · play · siguiente:** íconos blancos grandes sin caja y play en un círculo blanco. Responden en **toda la franja de abajo del reproductor**, desde debajo de los tiempos hasta el borde de la pantalla: a la izquierda del play es anterior y a la derecha siguiente. El play responde también un poco a sus costados, para que un toque que falla por poco no cambie de video. Al tocar se ilumina solo el botón.
- **Doble toque** en anterior / siguiente (o en las tarjetas): retrocede / adelanta 10 s.
- **Tiempos con horas** (`1:12:30`) para películas y conciertos.
- **Sobre el video:**
  - **☰ (arriba a la derecha):** menú con **Aleatorio** y **Repetir video** (ON / OFF escrito) y **Re-escanear USB**; al pie, la versión. Se cierra solo a los 5 s, tocando fuera o en ✕.
  - **Indicadores (arriba a la izquierda):** los íconos de repetir y aleatorio, en cian, solo mientras están encendidos. No se tocan.
  - **💡 Solo audio (arriba al centro):** pone la imagen en **negro** (de noche no encandila, y no queda video a la vista del conductor) con un "Solo audio" muy tenue; el audio y los controles siguen. Activo, el foco se ve tachado y en cian. Sigue así al cambiar de video, hasta volver a tocarlo (no se guarda al cerrar la app).
  - **Avisos:** si algo falla (un video que no avanza, uno que no se puede abrir), aparece un cartel oscuro arriba al centro, con texto naranja (rojo si es grave), por 6 s. No tapa los botones.
- **Toques en el video:** un toque no hace nada; **doble toque** abre la pantalla completa (ventana de 280 ms).
- **Se recuerdan** aleatorio y repetir al cerrar la app.

### Pantalla completa

- Tapa todo menos la tira de volumen de la izquierda (72), porque el auto no tiene botones físicos.
- **Un toque** muestra u oculta los controles; **doble toque** sale. El botón Atrás también sale.
- **Arriba:** el título, el **💡 solo audio** al centro, y el **☰** y el botón de salir a la derecha. Debajo del título, los indicadores de repetir y aleatorio.
- **Abajo:** la barra de progreso (se puede arrastrar: la zona de toque mide 34 y la barra visible 8; muestra la posición mientras se arrastra y salta ahí al soltarla), el tiempo `mm:ss / mm:ss` y anterior, play y siguiente.
- Mientras se reproduce, los controles se ocultan solos a los 3 s (no mientras el ☰ está abierto); en pausa quedan a la vista. Con solo audio y los controles ocultos, la pantalla queda completamente negra.

### Volumen con un gesto

Arrastrar el dedo hacia arriba o hacia abajo sobre el reproductor (o en pantalla completa) sube o baja el volumen, con un indicador grande al centro. No empieza sobre el explorador (ahí se recorre la lista), las barras de progreso y de volumen, ni el buscador.

### Pendrives

- La app escanea al abrirse y vuelve a escanear sola al conectar o quitar un pendrive; si el radio no avisa, **Menú ☰ → Re-escanear USB**.
- Si no encuentra videos, en la línea de abajo y en un aviso dice el motivo: falta el permiso, no hay pendrive, o "Sin videos en udisk1 (120 archivos: 115 mp3, 5 jpg)".

## Diferencias con JH MUSIC

- **Sin Automix:** entre un video y otro no hay mezcla. Automix queda apagado siempre, así que tampoco se analiza el audio.
- **El explorador no se oculta** (salvo en pantalla completa): no hay doble toque en la carátula ni "Ocultar explorador" en el menú.
- **Solo audio** y los **avisos del monitor** son solo de JH VIDEO.
- **No abre en el último video** al encender el auto (se decidió no hacerlo).

## Cómo está hecho (nativo)

### Reproducción

- **Reproductor: ExoPlayer** (Media3 1.1.1, `VideoPlayer.java`). Lee el archivo por su cuenta, así que la duración, la posición y los saltos no dependen del `MediaPlayer` del radio.
  - Si ExoPlayer no puede con un archivo (formato que no conoce, o audio que no decodifica), ese archivo se abre con el `MediaPlayer` del radio, como antes. El monitor lo dice: "ⓘ Lo abre el reproductor del radio (ExoPlayer: no decodifica video MP42)".
  - Si el video no se puede decodificar pero el audio sí (por ejemplo AV1, que el radio no tiene), suena con ExoPlayer: la barra funciona y se ve el dibujo de "sin video" con el aviso "ⓘ El radio no puede mostrar video AV1: solo se escucha el audio". En el auto, el reproductor del radio tampoco lo mostraba (sonaba en negro) y no saltaba.
  - El APK pasó de 1,8 MB a 4 MB por ExoPlayer.
- **Tapa del video:** el video queda tapado (negro) hasta que llega el primer cuadro del archivo nuevo. Si suena 2,5 s sin ningún cuadro, aparece el dibujo de "sin video".
  - Antes quedaba a la vista el último cuadro del video anterior: en el auto, un `.avi` que el radio no podía mostrar se veía "congelado".
  - La tapa va encima del `TextureView` (no se lo oculta) para que siga recibiendo cuadros. Los cuadros que llegan en los primeros 200 ms son del video anterior.
- **MP4 fragmentados** (los que bajan los descargadores de YouTube): no dicen cuánto duran (`mvhd` = 0) ni traen índice al inicio (`sidx`). Solo tienen al final una tabla (`mfra`) con el tiempo y la posición de cada fragmento.
  - Ni el `MediaPlayer` ni ExoPlayer leen esa tabla: por eso el radio mostraba `00:00 / 00:00` y la barra no saltaba. El Android del emulador ni siquiera los abre.
  - `FragmentedMp4Index` la lee y se la da a ExoPlayer como mapa de saltos, con la duración real (fin del último fragmento). También da la duración de esos videos en la lista.
- **Saltos a cuadros IDR:** estos archivos marcan como cuadro clave también cuadros I que no son IDR (en *QUIÉREME*, 83 de 127). Si un salto empezaba en uno de esos, el decodificador mostraba la imagen verde y rota y a los 2 s el video se trababa.
  - `VideoExtractors`: en H.264 solo cuenta como clave un IDR, después de ver 2 IDR en el video (uno con un único IDR queda como viene).
  - El mapa del MP4 fragmentado solo ofrece fragmentos que empiezan con IDR. Se revisa al saltar, no al abrir.
  - Los saltos van al IDR más cercano (`CLOSEST_SYNC`): son inmediatos y caen a menos de unos 3 s de donde se suelta la barra.
- **Tiempo del video con el reproductor del radio** (solo los archivos que ExoPlayer no pudo abrir): el radio puede no dar la duración ni la posición (devuelve 0 o -1).
  - Duración: si el reproductor no la da, se usa la del archivo (`MediaMetadataRetriever`, `MediaExtractor` y la tabla del MP4 fragmentado).
  - Posición: si sigue en 0 durante 3 s mientras llegan cuadros nuevos (o no hay imagen y el radio dice que reproduce), manda un reloj propio que se detiene en pausa y se mueve con cada salto.
- **Barra de progreso de 0 a 1000:** en una película de 2 h, cada paso de 0 a 100 eran 72 s.
- **Barra que no puede saltar:** si el archivo no trae índice dice "⚠ Este video no deja saltar: el archivo no trae índice"; si no se sabe la duración, "⚠ No se puede saltar: no se sabe cuánto dura este video". En los dos casos la barra vuelve adonde va el video.

### El video en pantalla

- **El video en dos lugares:** hay un `TextureView` en el cuadro del reproductor (`videoTexture`) y otro en la pantalla completa (`layout_video_fullscreen.xml`, `videoTextureFs`). El mismo reproductor pasa de uno a otro con `setSurface()`.
  - En pausa se vuelve a pedir el cuadro exacto para que la superficie nueva no quede negra.
  - El video se abre en segundo plano. Así un archivo grande del USB no congela la pantalla mientras abre.
  - Mientras abre, nada le pide posición, duración ni saltos al reproductor (`playerReady()`).
  - Si el reproductor falla, se libera y el siguiente toque crea uno nuevo. El aviso aparece en el cartel sobre el video.
- **Proporción:** el video se ajusta sin deformarse con una matriz de transformación (`fitVideo`). Si la forma no coincide, quedan franjas negras.
- **Cuadro 16:9:** lo da `VideoFrameLayout` (alto = ancho × 9/16). Los márgenes de −18 dp cancelan el relleno de la columna. El video ya usa todo el ancho, así que sin la barra de arriba no crece: el alto que se gana queda abajo, para los botones.
- **Color del fondo:** sale de la imagen incluida en el archivo. Si no la trae, se copia en chico el cuadro que ya está en pantalla, 2,5 s después de empezar (`captureAmbientFromVideo`).
  - No usa `getFrameAtTime`: abría un segundo decodificador de video mientras sonaba el video y en el radio trababa la reproducción.
- **Toques en el video:** `videoTapListener` (en el cuadro y en la pantalla completa) cuenta los toques con 280 ms (`DOUBLE_TAP_TIMEOUT_MS`). Los botones sobre el video (☰, 💡) reciben su propio toque; los indicadores, el negro de solo audio y el cartel de aviso no reciben toques, así que pasan al video.
- **Pantalla completa** (`setVideoFullscreen`): agrega `layout_video_fullscreen.xml` sobre `android.R.id.content`, con margen izquierdo de 72 dp para dejar la tira. `setFsControlsVisible` / `scheduleFsHide` ocultan los controles a los 3 s mientras se reproduce.

### Controles para el auto (1.1.0)

- **Anterior · play · siguiente:** `ImageButton` de 74 dp (ícono de 50 dp) con fondo `bg_ctrl_icon` y play de 88 dp. `handleNavZoneTouch()`, al inicio de `dispatchTouchEvent()`: un toque que empieza dentro de `layoutPlayerPanel` y debajo de `layoutProgressBlock` se reparte en tres columnas: el play con 24 dp de más a cada lado (`PLAY_ZONE_EXTRA_DP`), anterior a su izquierda y siguiente a su derecha. Deja el botón `activated` 180 ms y llama a `performClick()`, así el doble toque ±10 s sigue igual. En pantalla completa no se usa (`isVideoFs`).
- **Menú ☰:** `btnVideoMenu` (sobre el video, fondo `bg_video_menu_btn`) y `btnFsMenu` (pantalla completa) abren `showPlayerMenu(vista)`: un `PopupWindow` sin foco y sin recorte con `layout/popup_player_menu.xml`, del tamaño del reproductor o de la pantalla completa. Filas: aleatorio, repetir video, re-escanear USB (`menuRescan`) y versión al pie (`menuVersion`). Mientras está abierto, los controles de la pantalla completa no se ocultan (al cerrarlo, `scheduleFsHide()`).
- **Indicadores:** `ivModeRepeat` / `ivModeShuffle` sobre el video e `ivFsModeRepeat` / `ivFsModeShuffle` en la pantalla completa (`updateModeIndicators()`). Aleatorio y repetir se guardan en `SharedPreferences` (`shuffle_on`, `repeat_on`).
- **Solo audio:** `layoutAudioOnly` (sobre el video) y `layoutFsAudioOnly` (pantalla completa): negro con "Solo audio" en `#33ffffff`, sin recibir toques. `setAudioOnly()` / `updateAudioOnlyViews()` los muestran y cambian el foco (`ic_bulb` / `ic_bulb_off`, en cian activo). El video se sigue decodificando detrás: al apagarlo, la imagen vuelve al instante.
- **Librería (1.1.1):** igual que JH MUSIC 2.9.6: `item_tree_folder.xml` de 60 dp sin línea de cantidad, sangría `2 + nivel × 12` dp, ícono fijo, nombre de 21 sp con marquesina, `+` de 46 dp y ▶ cuadrado de 52 × 48 dp con `MiniEqualizerView` en la carpeta que suena; `bg_tree_node_open` para la abierta; `scrollTreeToPlayingFolder()` al volver a Librería. Doble toque en una fila (350 ms, `FOLDER_DOUBLE_TAP_MS`) = `playFolderFromNode()`; cada toque llama a `folderTreeAdapter.flash(node)` (`bg_tree_node_flash`, 250 ms, y nombre completo en 2 renglones).
- **Pestañas con número en etiqueta** (`CountBadgeSpan`), **título de alto fijo** (36 dp, para que los botones no se muevan) y **`bg_pressed_cyan`** al tocar en vez del naranja de Android.
- **Sin barra arriba:** `layoutCarStatusBar` y `viewStatusDivider` quedan ocultos (el código los sigue usando). Reloj en `tvStripClockHour` / `tvStripClockMin` y punto del pendrive en `viewUsbDot` (`updateUsbDot()`).
- **Gesto de volumen:** en `dispatchTouchEvent()`, igual que JH MUSIC (14 dp para decidir, 240 dp = 100 %, `ACTION_CANCEL` a la vista de abajo cuando el gesto toma el toque), con el indicador `layout_blind_volume_hud.xml`. No empieza sobre el explorador (`isTouchInExplorer`, salvo en pantalla completa, donde el explorador queda tapado) ni sobre la barra de la pantalla completa.

### Escaneo y duraciones

- Lee las extensiones mp4, mkv, webm, avi, 3gp, mov, m4v, ts, mpg, mpeg, flv, wmv, vob, mts, m2ts, divx, asf, 3g2, ogv y f4v.
- Revisa las rutas conocidas de los radios (`/storage/udisk1`, `udisk2`, `usb0`, etc.) y cualquier otra memoria montada en `/storage`, como `/storage/1A2B-3C4D`. Deja fuera la memoria interna. Además usa `MediaStore.Video` para los archivos de los pendrives.
- **Duraciones de la lista:** se leen en un hilo de prioridad baja. Con un video sonando, se lee una cada 0,7 s para no quitarle el USB al video. Si Android no la sabe (MP4 fragmentado), se lee de la tabla del final.

### Monitor de carga (provisional)

Cuando algo se traba, `updateLoadMonitor()` muestra el aviso en un **cartel arriba al centro del video** (`tvVideoAlert`, fondo `bg_video_alert`, debajo del foco y del ☰) y en la pantalla completa (`tvFsAlert`), con texto naranja (rojo si es grave). Queda 6 s y no recibe toques.

- "⚠ Pantalla saturada: se trabó 1,8 s": la pantalla tardó más de 0,4 s en responder (en rojo desde 1,5 s). Lo mide un hilo aparte cada 200 ms.
- "⚠ El video no avanza hace 3 s": reproduciendo, no llegó ningún cuadro nuevo en 2,5 s (en archivos solo de audio, la posición no cambió).
- "⚠ No se pudo abrir: motivo (códigos)" o "⚠ El reproductor falló: motivo". Por ejemplo "⚠ No se pudo abrir: no decodifica video AV1 · radio 1/-2147483648". Si no cabe, se corta el final.
- "⚠ El radio no saltó a 34:00 (sigue en 1:20)", "ⓘ El reproductor no sabe cuánto dura el video…" o "ⓘ El radio no da la posición…".
- Si en ese momento se están leyendo duraciones, agrega "· leyendo USB (N)".

### Piezas de JH MUSIC sin uso

La cápsula, el formato, el láser, el resplandor y Automix quedan ocultos en el layout. Así el código compartido sigue funcionando sin cambios.

## Maqueta web

`index.html`, `app.js` y `style.css` reproducen la pantalla en un navegador, con videos de muestra dibujados en un `canvas` (o videos de la PC, con Re-escanear USB). Tiene lo mismo que la app: sin barra arriba y reloj en la tira (72 px), franja de anterior / play / siguiente (`navZoneAt()` sobre `#playerPanel`), ☰ y 💡 sobre el video y en la pantalla completa (el menú es una capa, `#playerMenuLayer`, que se ubica encima del reproductor o de la pantalla completa), indicadores, solo audio (`#audioOnlyCover`), carpetas grandes con doble toque y destello, gesto de volumen con su indicador (no empieza sobre `.explorer-panel`) y aleatorio / repetir recordados en `localStorage` (`jhvideo_shuffle`, `jhvideo_repeat`). No tiene el monitor de carga, así que no muestra los carteles de aviso.

`demo-menu-video.html` es el muestrario con las ubicaciones posibles del ☰ (se eligió la C, sobre el video).

## Historial

| Versión | Fecha | Cambios |
|---|---|---|
| 1.1.2 | 2026-10-24 | Arreglo: al abrir, el reproductor mostraba el nombre del primer video pero ▶ no hacía nada (no había lista) y la pestaña decía "Videos 0". Ahora queda listo el primer video del pendrive con su carpeta como la activa (`prepareFirstTrack()`, sin abrirlo para no cargar al radio): ▶ lo reproduce. Pendiente de probar en el auto. |
| 1.1.1 | 2026-10-24 | Diseño revisado a la pantalla real del radio (1024 × 600): número de las pestañas en etiqueta, filas de carpeta de 60 sin línea de cantidad, nombre completo al tocar, ecualizador en el ▶ de la carpeta que suena, borde fino en la abierta, la lista vuelve a la carpeta que suena, título de alto fijo (los botones ya no se mueven), cian al tocar en vez de naranja, y en la pantalla completa anterior / siguiente ya no quedan encendidos. Pendiente de probar en el auto. |
| 1.1.0 | 2026-10-23 | Controles para el auto (franja de anterior / play / siguiente, play más grande), menú ☰ sobre el video con aleatorio, repetir y re-escanear, indicadores, **solo audio**, carpetas grandes con doble toque, Librería y pestaña Videos con ícono propio, sin barra arriba (reloj en la tira), avisos en cartel sobre el video, tira de volumen de 72, volumen de 10 en 10, gesto de volumen con indicador. Pendiente de probar en el auto. |
| 1.0.5 | 2026-10-23 | Los videos que el radio no puede mostrar (AV1) suenan con el dibujo de "sin video"; ya no queda congelado el cuadro del video anterior. |
| 1.0.4 | 2026-10-21 | La barra salta y se sabe la duración en los videos bajados de YouTube (MP4 fragmentados); saltos solo a cuadros IDR. |
