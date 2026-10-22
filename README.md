# JH VIDEO

Es la app de videos para el radio del Tucson ix35. Hace lo mismo que JH MUSIC (explorador de pendrives, búsqueda, Mi Lista y reproductor), pero con videos. Está en `jh-video/`, separada de JH MUSIC (`jh-music/`), para que su código no se mezcle con el de la música.

| Qué | Dónde |
|---|---|
| Maqueta web (la que se validó) | `index.html`, `app.js`, `style.css` |
| App nativa Android | `android/` (paquete `com.jh.carvideo`) |
| APK | `JH-VIDEO-v<versión>.apk`, en esta carpeta |
| Compilar | `bash build.sh` (Gradle Wrapper con JDK 11) |

JH MUSIC y JH VIDEO tienen paquetes distintos (`com.jh.caraudio` y `com.jh.carvideo`), así que se instalan juntas en el radio sin pisarse.

## Diferencias con JH MUSIC

- **Explorador:** tiene las pestañas *Carpetas USB*, *Videos (N)* y *Mi Lista (N)*. Mi Lista funciona igual que Mi Mix: el + agrega, se puede reordenar y vaciar, y muestra un punto mientras suena. Sus textos son "vistos", "Ya visto" y "Fin de Mi Lista".
- **Reproductor:**
  - El video es 16:9, va de borde a borde de la columna y tiene las esquinas rectas.
  - Debajo van el título, las tarjetas anterior/siguiente, la barra de progreso y los controles.
  - No tiene lupa ni cápsula PLAYING, tampoco formato MP4 ni línea de artista. El explorador queda siempre a la vista.
- **Sin Automix:** entre un video y otro no hay mezcla. Automix queda apagado siempre, así que tampoco se analiza el audio.
- **Gestos sobre el video** (con una ventana de 280 ms para el doble toque):
  - En el reproductor, un toque no hace nada y el doble toque abre la pantalla completa.
  - En pantalla completa, un toque muestra u oculta los controles y el doble toque sale. El botón Atrás también sale.
- **Pantalla completa:**
  - Tapa todo menos la tira de volumen de la izquierda, porque el auto no tiene botones físicos.
  - Arriba van el título y el botón de salir.
  - Abajo va la barra de progreso. Se puede arrastrar (la zona de toque mide 34 dp y la barra visible 8 dp), muestra la posición mientras se arrastra y salta ahí al soltarla.
  - También abajo van el tiempo `mm:ss / mm:ss` y los botones anterior, play y siguiente.
  - Mientras se reproduce, los controles se ocultan solos a los 3 s; en pausa quedan a la vista.
- **Tiempos con horas** (`1:12:30`) para películas y conciertos.
- **Barra de progreso de 0 a 1000:** en una película de 2 h, cada paso de 0 a 100 eran 72 s.

## Cómo está hecho (nativo)

- **Reproductor: ExoPlayer** (Media3 1.1.1, `VideoPlayer.java`). Lee el archivo por su cuenta, así que la duración, la posición y los saltos no dependen del `MediaPlayer` del radio.
  - Si ExoPlayer no puede con un archivo (formato que no conoce, o video o audio que no decodifica), ese archivo se abre con el `MediaPlayer` del radio, como antes. El monitor lo dice: "ⓘ Lo abre el reproductor del radio (ExoPlayer: no decodifica video AV1)".
  - El APK pasó de 1,8 MB a 4 MB por ExoPlayer.
- **MP4 fragmentados** (los que bajan los descargadores de YouTube): no dicen cuánto duran (`mvhd` = 0) ni traen índice al inicio (`sidx`). Solo tienen al final una tabla (`mfra`) con el tiempo y la posición de cada fragmento.
  - Ni el `MediaPlayer` ni ExoPlayer leen esa tabla: por eso el radio mostraba `00:00 / 00:00` y la barra no saltaba. El Android del emulador ni siquiera los abre.
  - `FragmentedMp4Index` la lee y se la da a ExoPlayer como mapa de saltos, con la duración real (fin del último fragmento). También da la duración de esos videos en la lista.
- **Saltos a cuadros IDR:** estos archivos marcan como cuadro clave también cuadros I que no son IDR (en *QUIÉREME*, 83 de 127). Si un salto empezaba en uno de esos, el decodificador mostraba la imagen verde y rota y a los 2 s el video se trababa.
  - `VideoExtractors`: en H.264 solo cuenta como clave un IDR, después de ver 2 IDR en el video (uno con un único IDR queda como viene).
  - El mapa del MP4 fragmentado solo ofrece fragmentos que empiezan con IDR. Se revisa al saltar, no al abrir.
  - Los saltos van al IDR más cercano (`CLOSEST_SYNC`): son inmediatos y caen a menos de unos 3 s de donde se suelta la barra.
- **El video en dos lugares:** hay un `TextureView` en el cuadro del reproductor (`videoTexture`) y otro en la pantalla completa (`layout_video_fullscreen.xml`, `videoTextureFs`). El mismo reproductor pasa de uno a otro con `setSurface()`.
  - En pausa se vuelve a pedir el cuadro exacto para que la superficie nueva no quede negra.
  - El video se abre en segundo plano. Así un archivo grande del USB no congela la pantalla mientras abre.
  - Mientras abre, nada le pide posición, duración ni saltos al reproductor (`playerReady()`).
  - Si el reproductor falla, se libera y el siguiente toque crea uno nuevo. El aviso queda en la barra de arriba.
- **Proporción:** el video se ajusta sin deformarse con una matriz de transformación (`fitVideo`). Si la forma no coincide, quedan franjas negras.
- **Cuadro 16:9:** lo da `VideoFrameLayout` (alto = ancho × 9/16). Los márgenes de −18 dp cancelan el relleno de la columna.
- **Color del fondo:** sale de la imagen incluida en el archivo. Si no la trae, se copia en chico el cuadro que ya está en pantalla, 2,5 s después de empezar (`captureAmbientFromVideo`).
  - No usa `getFrameAtTime`: abría un segundo decodificador de video mientras sonaba el video y en el radio trababa la reproducción.
- **Tiempo del video con el reproductor del radio** (solo los archivos que ExoPlayer no pudo abrir): el radio puede no dar la duración ni la posición (devuelve 0 o -1).
  - Duración: si el reproductor no la da, se usa la del archivo (`MediaMetadataRetriever`, `MediaExtractor` y la tabla del MP4 fragmentado).
  - Posición: si sigue en 0 durante 3 s mientras llegan cuadros nuevos, manda un reloj propio que se detiene en pausa y se mueve con cada salto.
  - Si a los 2 s de un salto la posición no quedó cerca, el monitor dice "⚠ El radio no saltó a 34:00 (sigue en 1:20)".
  - El monitor también avisa "ⓘ El reproductor no sabe cuánto dura el video…" o "ⓘ El radio no da la posición…".
- **Barra que no puede saltar:** si el archivo no trae índice dice "⚠ Este video no deja saltar: el archivo no trae índice"; si no se sabe la duración, "⚠ No se puede saltar: no se sabe cuánto dura este video". En los dos casos la barra vuelve adonde va el video.
- **Duraciones de la lista:** se leen en un hilo de prioridad baja. Con un video sonando, se lee una cada 0,7 s para no quitarle el USB al video. Si Android no la sabe (MP4 fragmentado), se lee de la tabla del final.
- **Monitor de carga (provisional):** reemplaza el saludo de la barra de arriba cuando algo se traba. El aviso queda 6 s.
  - "⚠ Pantalla saturada: se trabó 1,8 s": la pantalla tardó más de 0,4 s en responder (en rojo desde 1,5 s). Lo mide un hilo aparte cada 200 ms.
  - "⚠ El video no avanza hace 3 s": reproduciendo, no llegó ningún cuadro nuevo en 2,5 s (en archivos solo de audio, la posición no cambió).
  - "⚠ No se pudo abrir: motivo (códigos)" o "⚠ El reproductor falló: motivo". Por ejemplo "⚠ No se pudo abrir: no decodifica video AV1 · radio 1/-2147483648". Si no cabe, se corta el final.
  - Si en ese momento se están leyendo duraciones, agrega "· leyendo USB (N)".
- **Escaneo:**
  - Lee las extensiones mp4, mkv, webm, avi, 3gp, mov, m4v, ts, mpg, mpeg, flv, wmv, vob, mts, m2ts, divx, asf, 3g2, ogv y f4v.
  - Revisa las rutas conocidas de los radios (`/storage/udisk1`, `udisk2`, `usb0`, etc.) y cualquier otra memoria montada en `/storage`, como `/storage/1A2B-3C4D`. Deja fuera la memoria interna.
  - Además usa `MediaStore.Video` para los archivos de los pendrives.
  - Si no encuentra videos, no muestra carpetas de ejemplo. En la línea de abajo y en un aviso dice el motivo: falta el permiso, no hay pendrive, o "Sin videos en udisk1 (120 archivos: 115 mp3, 5 jpg)".
- **Piezas de JH MUSIC sin uso:** la cápsula, el formato, el láser, el resplandor y Automix quedan ocultas en el layout. Así el código compartido sigue funcionando sin cambios.
