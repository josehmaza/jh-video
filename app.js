/**
 * JH VIDEO - Hyundai Tucson ix35
 * Maqueta del reproductor de videos para auto, hecha a partir de la de JH MUSIC (simulación web)
 */

function initApp() {

  // =================== ESTADO INICIAL Y DATOS ===================
  // Videos de muestra: se dibujan en un canvas (no hacen falta archivos). Para probar con videos
  // reales, en la maqueta "Re-escanear USB" abre una carpeta de la PC.
  const VIDEO_SAMPLES = [
    // [título, artista o canal, carpeta (ruta en el pendrive), duración, formato, color 1, color 2]
    ["Colegiala", "Rodolfo Aicardi", "/storage/udisk1/Videos musicales/", "03:45", "MP4 720P", "#f97316", "#be185d"],
    ["Cumbia del Sol", "Los Ángeles Azules", "/storage/udisk1/Videos musicales/", "04:10", "MP4 1080P", "#facc15", "#ea580c"],
    ["El Aguacate", "Julio Jaramillo", "/storage/udisk1/Videos musicales/", "03:20", "MP4 480P", "#22c55e", "#14532d"],
    ["Guaracadita", "DJ Fuego", "/storage/udisk1/Videos musicales/", "03:15", "MP4 720P", "#22d3ee", "#7c3aed"],
    ["Hasta la Raíz", "Corazón Serrano", "/storage/udisk1/Videos musicales/", "04:02", "MP4 1080P", "#e11d48", "#4c0519"],
    ["La Bomba", "Azul Azul", "/storage/udisk1/Videos musicales/", "03:50", "MP4 720P", "#3b82f6", "#1e1b4b"],
    ["Maldito Corazón", "Batahola", "/storage/udisk1/Videos musicales/", "04:20", "MP4 720P", "#a855f7", "#3b0764"],
    ["Torres Gemelas", "Delfín Quishpe", "/storage/udisk1/Videos musicales/", "03:55", "MP4 480P", "#64748b", "#0f172a"],
    ["La Venada", "Angel Guaraca", "/storage/udisk1/Videos musicales/Chicha/", "04:05", "MP4 720P", "#84cc16", "#365314"],
    ["Mi Chequera", "Bayron Caicedo", "/storage/udisk1/Videos musicales/Chicha/", "03:40", "MP4 720P", "#f59e0b", "#78350f"],
    ["Concierto en Vivo Quito 2023", "Tierra Canela", "/storage/udisk1/Conciertos/", "1:12:30", "MKV 1080P", "#0ea5e9", "#082f49"],
    ["Éxitos Bailables en Vivo", "Euler Caicedo", "/storage/udisk1/Conciertos/", "58:10", "MP4 720P", "#ec4899", "#500724"],
    ["Gira Ecuador 2024", "Las Ñañas", "/storage/udisk1/Conciertos/", "45:20", "MP4 720P", "#f43f5e", "#1e293b"],
    ["Amor Eterno (Karaoke)", "Rocío Dúrcal", "/storage/udisk1/Karaoke/", "04:55", "MP4 720P", "#8b5cf6", "#1e1b4b"],
    ["Hermoso Cariño (Karaoke)", "Vicente Fernández", "/storage/udisk1/Karaoke/", "03:10", "MP4 480P", "#d97706", "#451a03"],
    ["La Bikina (Karaoke)", "Luis Miguel", "/storage/udisk1/Karaoke/", "03:30", "AVI 480P", "#14b8a6", "#042f2e"],
    ["Coco", "Pixar", "/storage/udisk1/Películas/", "1:45:00", "MKV 1080P", "#fb923c", "#431407"],
    ["Toy Story", "Pixar", "/storage/udisk1/Películas/", "1:21:00", "MP4 720P", "#60a5fa", "#172554"],
    ["Laguna de Quilotoa", "Grabación propia", "/storage/udisk2/Viaje/", "15:02", "MOV 1080P", "#10b981", "#022c22"],
    ["Mitad del Mundo", "Grabación propia", "/storage/udisk2/Viaje/", "08:15", "MP4 1080P", "#fbbf24", "#422006"],
    ["Ruta a Baños", "Grabación propia", "/storage/udisk2/Viaje/", "12:40", "MP4 1080P", "#2dd4bf", "#134e4a"]
  ];
  const ON_MY_LIST = ["Colegiala", "Guaracadita", "Concierto en Vivo Quito 2023"];

  function durationToSec(text) {
    return text.split(':').reduce((acc, v) => acc * 60 + parseInt(v, 10), 0);
  }

  // Miniatura de muestra (SVG): da el color al fondo de la app, como la carátula en JH MUSIC
  function samplePoster(colorA, colorB) {
    const svg =
      `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="90" viewBox="0 0 160 90">` +
      `<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">` +
      `<stop offset="0" stop-color="${colorA}"/><stop offset="1" stop-color="${colorB}"/></linearGradient></defs>` +
      `<rect width="160" height="90" fill="url(#g)"/></svg>`;
    return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
  }

  const sampleTracks = VIDEO_SAMPLES.map(([title, artist, path, duration, format, colorA, colorB], i) => {
    const parts = path.split('/').filter(Boolean);
    return {
      id: i + 1,
      title: title,
      artist: artist,
      album: parts[parts.length - 1],
      folder: parts[parts.length - 1],
      path: path,
      duration: duration,
      durationSec: durationToSec(duration),
      format: format,
      favorite: ON_MY_LIST.includes(title),
      audioUrl: null,      // con un archivo real, la URL del video
      simColors: [colorA, colorB],
      coverUrl: samplePoster(colorA, colorB)
    };
  });

  // Helper de ordenamiento alfabético natural para títulos de pistas (A-Z)
  function sortTracksAlphabetically(trackList) {
    if (!trackList || !Array.isArray(trackList)) return trackList;
    return trackList.sort((a, b) => {
      const t1 = (a.title || a.name || '').trim();
      const t2 = (b.title || b.name || '').trim();
      return t1.localeCompare(t2, 'es', { numeric: true, sensitivity: 'base' });
    });
  }

  // Ordenar videos de muestra alfabéticamente por título
  sortTracksAlphabetically(sampleTracks);

  // Carpetas para la búsqueda de la pestaña Carpetas, sacadas de los videos de muestra
  const foldersList = [];
  (function buildFoldersList() {
    const map = {};
    sampleTracks.forEach(t => {
      const parts = t.path.split('/').filter(Boolean);
      // cada carpeta de la ruta (sin storage ni udiskN), con los videos que tiene directamente
      for (let k = 2; k < parts.length; k++) {
        const p = '/' + parts.slice(0, k + 1).join('/') + '/';
        if (!map[p]) map[p] = { name: parts[k], path: p, count: 0, usbSource: p.includes('udisk2') ? 'USB 2' : 'USB 1' };
        if (k === parts.length - 1) map[p].count++;
      }
    });
    foldersList.push(...Object.values(map));
  })();

  // Árbol de Carpetas USB armado con los videos de muestra
  const defaultTreeFolders = [];

  let currentTreeData = buildTreeFromTracks(sampleTracks);

  // Nodos colapsados por defecto para que el usuario los vaya abriendo según su gusto
  let expandedTreeNodes = new Set();

  let masterTracks = [...sampleTracks];
  let currentPlaylist = [...sampleTracks];
  // La canción cargada en el reproductor. Abrir una carpeta cambia currentPlaylist, pero no lo que suena:
  // el árbol usa esta para saber cuál es la carpeta que suena (ecualizador)
  let loadedTrack = null;
  let currentTrackIndex = 0;
  let isPlaying = false;
  let isShuffle = false;
  let isRepeat = false;
  let currentVolume = 75;
  let previousVolume = 75;
  let isMuted = false;
  let activeTab = "folders"; // "folders", "playlist", "mymix"
  let currentFolder = "Videos musicales";
  let currentFolderUsbSource = "USB 1";
  // Lo que muestra Pistas (carpeta activa). Va aparte de la cola (currentPlaylist) para que,
  // mientras suena Mi Lista, puedas abrir carpetas y agregar canciones sin cortar la lista.
  let pistasTracks = [...sampleTracks];

  // =================== MI LISTA (reemplaza a Favoritos) ===================
  // Una lista que armas cuando quieras (botón + en cada canción o carpeta) y que después suena
  // entera, en el orden en que agregaste, con Automix. Como la lista de Automix de Virtual DJ:
  // - Lo que agregas mientras suena entra al final de la cola en vivo.
  // - Al terminar la última canción, se detiene.
  // - Las que ya sonaron quedan en la lista, marcadas con ✓.
  let myMixIds = [];
  try {
    const saved = JSON.parse(localStorage.getItem('jhvideo_mymix') || 'null');
    if (Array.isArray(saved)) myMixIds = saved;
  } catch (e) {}
  if (myMixIds.length === 0) {
    // Las estrellas de Favoritos pasan a Mi Lista (en la app, la primera vez)
    myMixIds = sampleTracks.filter(t => t.favorite).map(t => t.id);
  }
  const mixPlayedIds = new Set();
  let playingFromMix = false;  // la cola que suena es Mi Lista
  let mixCurPos = 0;           // posición de la que suena dentro de Mi Lista
  let isReorderingMix = false; // modo de reordenar canciones en Mi Lista (la ✕ pasa a ser ≡)
  let justLandedMixId = null;

  function animateMixFLIP(mutateDomFn) {
    const items = Array.from(trackListContainer.querySelectorAll('.mix-item:not(.mix-orphan)'));
    const firstRects = new Map();
    items.forEach(el => firstRects.set(el, el.getBoundingClientRect().top));

    mutateDomFn();

    items.forEach(el => {
      const firstTop = firstRects.get(el);
      const lastTop = el.getBoundingClientRect().top;
      const delta = firstTop - lastTop;
      if (delta !== 0) {
        el.style.transition = 'none';
        el.style.transform = `translateY(${delta}px)`;
        el.offsetHeight; // reflow
        el.style.transition = 'transform 0.22s cubic-bezier(0.2, 0.8, 0.2, 1)';
        el.style.transform = 'translateY(0)';
      }
    });
  }

  // Audio HTML5 y Sintetizador Web Audio
  const audio = document.getElementById('audioPlayer');
  let audioContext = null;
  let synthOscillator = null;
  let synthGain = null;
  let simInterval = null;
  let simCurrentSec = 0;

  // =================== ELEMENTOS DEL DOM ===================
  const frameWrapper = document.getElementById('frameWrapper');
  const clockDisplay = document.getElementById('clockDisplay');
  const driverModeBadge = document.getElementById('driverModeBadge');
  const playbackStatusText = document.getElementById('playbackStatusText');
  const trackTitle = document.getElementById('trackTitle');
  const trackArtist = document.getElementById('trackArtist');
  const trackAlbum = document.getElementById('trackAlbum');
  const formatBadge = document.getElementById('formatBadge');
  const albumContainer = document.getElementById('albumContainer');
  const albumArt = document.getElementById('albumArt');
  const albumPlaceholder = document.getElementById('albumPlaceholder');
  const currentPathLabel = document.getElementById('currentPathLabel');
  
  // Progreso
  const progressBarContainer = document.getElementById('progressBarContainer');
  const progressCurrent = document.getElementById('progressCurrent');
  const currentTimeEl = document.getElementById('currentTime');
  const totalDurationEl = document.getElementById('totalDuration');

  // Botones de Reproducción
  const btnPlayPause = document.getElementById('btnPlayPause');
  const playIcon = document.getElementById('playIcon');
  const pauseIcon = document.getElementById('pauseIcon');
  const btnPrev = document.getElementById('btnPrev');
  const btnNext = document.getElementById('btnNext');
  const playerMenuLayer = document.getElementById('playerMenuLayer');
  const menuShuffle = document.getElementById('menuShuffle');
  const menuRepeat = document.getElementById('menuRepeat');

  // Modo Reproductor Expandido / Buscador de Archivos
  const dashboardGrid = document.querySelector('.dashboard-grid');
  const btnToggleExpand = document.getElementById('btnToggleExpand');
  const btnAutoMix = document.getElementById('btnAutoMix');

  // =================== AUTOMIX (simulado en la maqueta) ===================
  // Misma regla que la app nativa: mezcla de 5 s, 2 s en mixes de 10 min o más,
  // sin mezcla en canciones cortas (menos de 3 veces la mezcla) ni con Repetir.
  const AUTOMIX_SEC = 5;
  const AUTOMIX_LONG_TRACK_SEC = 600;
  const AUTOMIX_LONG_TRACK_MIX_SEC = 2;
  let isAutoMix = false;
  let isMixing = false;
  let mixEndTimer = null;
  try {
    isAutoMix = false; // JH VIDEO no mezcla entre videos
  } catch (e) {}

  function updateAutoMixButton() {
    const artBox = document.getElementById('albumContainer');
    if (artBox) artBox.classList.toggle('is-mixing', isMixing); // activa el segundo haz láser magenta en la carátula
    const pillStatusText = document.getElementById('pillStatusText');
    if (pillStatusText) {
      if (isMixing) {
        pillStatusText.textContent = "MIXING ⇄";
      } else if (isPlaying) {
        pillStatusText.textContent = "PLAYING";
      } else {
        pillStatusText.textContent = "PAUSED";
      }
    }
    if (!btnAutoMix) return;
    btnAutoMix.classList.toggle('is-active', isAutoMix);
    const automixState = document.getElementById('automixState');
    if (automixState) automixState.textContent = isAutoMix ? 'ON' : 'OFF';
    btnAutoMix.title = isAutoMix ? "Automix activado: toca para desactivar" : "Automix: mezcla automática e igualar volumen entre canciones";
  }

  function autoMixSecondsFor(current, next) {
    if (!current || !next) return 0;
    let mix = AUTOMIX_SEC;
    if (current.durationSec >= AUTOMIX_LONG_TRACK_SEC) mix = Math.min(mix, AUTOMIX_LONG_TRACK_MIX_SEC);
    if (current.durationSec < mix * 3 || next.durationSec < mix * 3) return 0;
    return mix;
  }

  /** Siguiente para mezclar: en aleatorio se sortea ahora y nunca es la misma canción. */
  function pickAutoMixNextIndex() {
    const n = currentPlaylist.length;
    if (!isShuffle) return (currentTrackIndex + 1) % n;
    let index = Math.floor(Math.random() * (n - 1));
    if (index >= currentTrackIndex) index++;
    return index;
  }

  const ALBUM_ART_FADE_MS = 700;

  /** Carátula vieja → nueva: la vieja (copia encima) se desvanece mientras la nueva queda debajo. */
  function crossfadeAlbumArt(oldVisible, oldSrc) {
    const container = document.getElementById('albumContainer');
    const img = document.getElementById('albumArt');
    if (!container || !img) return;
    const newVisible = !img.classList.contains('hidden');
    if (oldVisible) {
      const ghost = document.createElement('img');
      ghost.src = oldSrc;
      ghost.className = 'album-art-img art-ghost np-fade';
      container.appendChild(ghost);
      setTimeout(() => ghost.classList.add('np-hidden'), 30);
      setTimeout(() => ghost.remove(), ALBUM_ART_FADE_MS + 100);
    } else if (newVisible) {
      img.classList.add('np-fade', 'np-hidden');
      setTimeout(() => img.classList.remove('np-hidden'), 30);
    }
  }

  /** El fondo pasa al color de la canción nueva durante toda la mezcla (o vuelve a 1,2 s). */
  function setThemeFadeSeconds(sec) {
    if (!frameWrapper) return;
    if (sec) frameWrapper.style.setProperty('--th-dur', sec + 's');
    else frameWrapper.style.removeProperty('--th-dur');
  }

  function startAutoMix(nextIndex, mixSec) {
    // Activa la mezcla visual en la carátula (Doble Láser Magenta + Cápsula MIXING ⇄)
    isMixing = true;
    // El color del fondo acompaña la mezcla: pasa de una canción a la otra en lo que dura
    setThemeFadeSeconds(mixSec);
    clearTimeout(mixEndTimer);
    mixEndTimer = setTimeout(() => {
      isMixing = false;
      setThemeFadeSeconds(0);
      updateAutoMixButton();
      updatePlayStateUI();
    }, mixSec * 1000);
    updateAutoMixButton();
    updatePlayStateUI();

    // Cambio instantáneo y limpio de datos: sin vacíos negros ni parpadeos de textos
    const artImg = document.getElementById('albumArt');
    const oldArtVisible = !!artImg && !artImg.classList.contains('hidden');
    const oldArtSrc = artImg ? artImg.src : '';
    loadTrack(nextIndex, true);
    crossfadeAlbumArt(oldArtVisible, oldArtSrc);
  }

  function cancelAutoMix() {
    if (!isMixing) return;
    clearTimeout(mixEndTimer);
    isMixing = false;
    setThemeFadeSeconds(0);
    updateAutoMixButton();
    updatePlayStateUI();
  }

  if (btnAutoMix) {
    updateAutoMixButton();
    btnAutoMix.addEventListener('click', () => {
      isAutoMix = !isAutoMix;
      if (!isAutoMix) cancelAutoMix();
      try {
        localStorage.setItem('jhvideo_automix', isAutoMix ? '1' : '0');
      } catch (e) {}
      updateAutoMixButton();
      updatePlayStateUI();
    });
  }

  // Previsualización de Canción Anterior y Posterior
  const adjPrevCard = document.getElementById('adjPrevCard');
  const prevTrackTitle = document.getElementById('prevTrackTitle');
  const adjNextCard = document.getElementById('adjNextCard');
  const nextTrackTitle = document.getElementById('nextTrackTitle');

  // Controles de Volumen (Solución a botones apagados de noche)
  const volPercent = document.getElementById('volPercent');
  const volRange = document.getElementById('volRange');
  const btnVolDown = document.getElementById('btnVolDown');
  const btnVolUp = document.getElementById('btnVolUp');
  const btnMute = document.getElementById('btnMute');
  const iconUnmuted = document.getElementById('iconUnmuted');
  const iconMuted = document.getElementById('iconMuted');

  // Explorador y Pestañas
  const tabButtons = document.querySelectorAll('.tab-btn');
  const trackListContainer = document.getElementById('trackListContainer');
  const songCountEl = document.getElementById('songCount');
  const searchInput = document.getElementById('searchInput');
  const clearSearch = document.getElementById('clearSearch');
  const searchSpinner = document.getElementById('searchSpinner');
  const searchOverlayLoading = document.getElementById('searchOverlayLoading');
  const crumbRoot = document.getElementById('crumbRoot');
  const crumbFolder = document.getElementById('crumbFolder');
  const fileInput = document.getElementById('fileInput');
  const btnToggleFullscreen = document.getElementById('btnToggleFullscreen');
  const treeActions = document.getElementById('treeActions');
  const btnCollapseAllTree = document.getElementById('btnCollapseAllTree');

  // =================== FONDO CON EL COLOR DE LA CARÁTULA ===================
  // Como YouTube Music: el fondo de toda la app (también el explorador) toma el color de la carátula
  // y cambia con un fundido suave. El color se oscurece y se limita para que el texto blanco
  // siempre se lea bien manejando.
  const THEME_DEFAULT = [23, 60, 92]; // sin carátula: azul petróleo de la app
  const GLOW_DEFAULT = [56, 189, 248]; // sin carátula: el resplandor cian de siempre
  let themeToken = 0;

  function rgbToHsl(r, g, b) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    const l = (max + min) / 2;
    if (max === min) return [0, 0, l];
    const d = max - min;
    const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    let h;
    if (max === r) h = (g - b) / d + (g < b ? 6 : 0);
    else if (max === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    return [h / 6, s, l];
  }

  function hslToRgb(h, s, l) {
    if (s === 0) return [l * 255, l * 255, l * 255];
    const hue = (p, q, t) => {
      if (t < 0) t += 1;
      if (t > 1) t -= 1;
      if (t < 1 / 6) return p + (q - p) * 6 * t;
      if (t < 1 / 2) return q;
      if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
      return p;
    };
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    return [hue(p, q, h + 1 / 3) * 255, hue(p, q, h) * 255, hue(p, q, h - 1 / 3) * 255];
  }

  /** Color representativo de la carátula: promedio dando más peso a los colores vivos. */
  function extractCoverColor(src, callback) {
    const img = new Image();
    img.onload = () => {
      try {
        const size = 24; // muy pocos píxeles: rápido y ya da el color general
        const canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, size, size);
        const data = ctx.getImageData(0, 0, size, size).data;
        let r = 0, g = 0, b = 0, total = 0;
        for (let i = 0; i < data.length; i += 4) {
          const R = data[i], G = data[i + 1], B = data[i + 2];
          const max = Math.max(R, G, B), min = Math.min(R, G, B);
          const sat = max === 0 ? 0 : (max - min) / max;
          const weight = 0.08 + sat * sat * (max / 255);
          r += R * weight; g += G * weight; b += B * weight; total += weight;
        }
        callback(total > 0 ? [r / total, g / total, b / total] : null);
      } catch (e) {
        callback(null);
      }
    };
    img.onerror = () => callback(null);
    img.src = src;
  }

  /** Oscurece y limita el color para que el texto blanco tenga buen contraste. */
  function toThemeColor(rgb) {
    const [h, s, l] = rgbToHsl(rgb[0], rgb[1], rgb[2]);
    const sat = s < 0.08 ? s : Math.max(0.35, Math.min(0.75, s)); // grises quedan grises
    const light = Math.max(0.22, Math.min(0.36, l));
    return hslToRgb(h, sat, light);
  }

  /** Resplandor de la carátula: el color de la canción, más claro y vivo. */
  function toGlowColor(rgb) {
    const [h, s] = rgbToHsl(rgb[0], rgb[1], rgb[2]);
    return s < 0.08 ? [200, 210, 225] : hslToRgb(h, Math.max(0.6, s), 0.58);
  }

  function applyTheme(rgb, glow) {
    if (!frameWrapper) return;
    frameWrapper.style.setProperty('--th-r', Math.round(rgb[0]));
    frameWrapper.style.setProperty('--th-g', Math.round(rgb[1]));
    frameWrapper.style.setProperty('--th-b', Math.round(rgb[2]));
    frameWrapper.style.setProperty('--gl-r', Math.round(glow[0]));
    frameWrapper.style.setProperty('--gl-g', Math.round(glow[1]));
    frameWrapper.style.setProperty('--gl-b', Math.round(glow[2]));
  }

  function updateDynamicTheme(track) {
    const token = ++themeToken;
    if (!track || !track.coverUrl) {
      applyTheme(THEME_DEFAULT, GLOW_DEFAULT);
      return;
    }
    extractCoverColor(track.coverUrl, (rgb) => {
      if (token !== themeToken) return; // ya cambió de canción
      if (rgb) {
        applyTheme(toThemeColor(rgb), toGlowColor(rgb));
      } else {
        applyTheme(THEME_DEFAULT, GLOW_DEFAULT);
      }
    });
  }

  // =================== MI LISTA: LISTA Y COLA ===================
  function allTracksPool() {
    return masterTracks.length > 0 ? masterTracks : sampleTracks;
  }

  /** Canciones de Mi Lista en el orden en que se agregaron. */
  function mixTracks() {
    const byId = new Map(allTracksPool().map(t => [t.id, t]));
    return myMixIds.map(id => byId.get(id)).filter(Boolean);
  }

  function isInMix(track) {
    return !!track && myMixIds.includes(track.id);
  }

  function saveMix() {
    try {
      localStorage.setItem('jhvideo_mymix', JSON.stringify(myMixIds));
    } catch (e) {}
  }

  function updateMixTabCount() {
    const el = document.getElementById('mixCount');
    if (el) el.textContent = mixTracks().length;
  }

  /** Muestra lo que muestra Pistas; mientras suena Mi Lista, la cola no se toca. */
  function setPistasTracks(list) {
    pistasTracks = list;
    if (!playingFromMix) currentPlaylist = list;
  }

  /**
   * Mientras suena Mi Lista, la cola se rearma con la lista actual (agregar / quitar en vivo).
   * Si se quitó la que suena, sigue sonando y después viene la que estaba a continuación.
   */
  function syncMixQueue() {
    if (!playingFromMix) return;
    const cur = currentPlaylist[currentTrackIndex];
    const list = mixTracks();
    let idx = cur ? list.findIndex(t => t.id === cur.id) : -1;
    if (idx < 0 && cur) {
      idx = Math.min(mixCurPos, list.length);
      list.splice(idx, 0, cur);
    }
    currentPlaylist = list;
    currentTrackIndex = Math.max(0, idx);
    mixCurPos = currentTrackIndex;
    updatePreviews();
    updateTrackSource(currentPlaylist[currentTrackIndex]); // "2 de 4" cambia al agregar o quitar
  }

  /** Agrega canciones al final de Mi Lista (sin repetir). Devuelve cuántas se agregaron. */
  function addToMix(tracks) {
    let added = 0;
    tracks.forEach(t => {
      if (t && !myMixIds.includes(t.id)) {
        myMixIds.push(t.id);
        added++;
      }
    });
    if (added > 0) {
      saveMix();
      updateMixTabCount();
      syncMixQueue();
    }
    return added;
  }

  function removeFromMix(track) {
    myMixIds = myMixIds.filter(id => id !== track.id);
    mixPlayedIds.delete(track.id);
    saveMix();
    updateMixTabCount();
    syncMixQueue();
  }

  function clearMix() {
    myMixIds = [];
    mixPlayedIds.clear();
    saveMix();
    updateMixTabCount();
    syncMixQueue();
  }

  /** Reproduce Mi Lista desde una posición. fromStart: botón Reproducir (borra las marcas ✓). */
  function playMix(position, fromStart) {
    const list = mixTracks();
    if (list.length === 0) return;
    if (fromStart) mixPlayedIds.clear();
    cancelAutoMix();
    playingFromMix = true;
    currentPlaylist = list;
    loadTrack(Math.max(0, Math.min(position, list.length - 1)), true);
  }

  /** Siguiente dentro de Mi Lista, o -1 si ya no queda ninguna (la lista se detiene). */
  function nextMixIndex() {
    const n = currentPlaylist.length;
    if (isShuffle) {
      // En aleatorio: una que todavía no haya sonado
      const pending = [];
      for (let i = 0; i < n; i++) {
        if (i !== currentTrackIndex && !mixPlayedIds.has(currentPlaylist[i].id)) pending.push(i);
      }
      return pending.length > 0 ? pending[Math.floor(Math.random() * pending.length)] : -1;
    }
    return currentTrackIndex + 1 < n ? currentTrackIndex + 1 : -1;
  }

  /** Terminó la última canción de Mi Lista: se detiene y queda en pausa al inicio de esa canción. */
  function finishMix() {
    cancelAutoMix();
    stopSimulatedAudio();
    audio.pause();
    isPlaying = false;
    simCurrentSec = 0;
    if (audio.src) audio.currentTime = 0;
    progressCurrent.style.width = "0%";
    currentTimeEl.textContent = "00:00";
    updatePlayStateUI();
    showMixToast("Fin de Mi Lista");
  }

  let mixToastTimer = null;
  /** Aviso corto al pie del explorador ("+1 a Mi Lista", "Fin de Mi Lista"...). */
  function showMixToast(text) {
    let toast = document.getElementById('mixToast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'mixToast';
      toast.className = 'mix-toast';
      const wrapper = document.querySelector('.track-list-wrapper') || document.body;
      wrapper.appendChild(toast);
    }
    toast.textContent = text;
    toast.classList.add('is-visible');
    clearTimeout(mixToastTimer);
    mixToastTimer = setTimeout(() => toast.classList.remove('is-visible'), 1600);
  }

  /** Botón + / ✓ de cada canción (Pistas y búsquedas). */
  const MIX_PLUS_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>';
  const MIX_CHECK_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="5 12.5 10 17.5 19 7"/></svg>';
  const MIX_REMOVE_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round"><line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/></svg>';
  const MIX_REORDER_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/></svg>';
  const MIX_HEADER_REORDER_SVG = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="15" x2="7" y2="5"/><polyline points="4 8 7 5 10 8"/><line x1="17" y1="9" x2="17" y2="19"/><polyline points="14 16 17 19 20 16"/></svg>';
  const MIX_TAB_ICON = '<svg class="mix-source-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="3" y1="6" x2="14" y2="6"/><line x1="3" y1="12" x2="14" y2="12"/><line x1="3" y1="18" x2="10" y2="18"/><line x1="18" y1="13" x2="18" y2="21"/><line x1="14" y1="17" x2="22" y2="17"/></svg>';

  function mixToggleButtonHtml(track) {
    const inMix = isInMix(track);
    return `<button class="btn-item-mix ${inMix ? 'is-in-mix' : ''}" title="${inMix ? 'En Mi Lista (toca para quitar)' : 'Agregar a Mi Lista'}">${inMix ? MIX_CHECK_SVG : MIX_PLUS_SVG}</button>`;
  }

  function bindMixToggleButton(itemEl, track) {
    const btn = itemEl.querySelector('.btn-item-mix');
    if (!btn) return;
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      searchInput.blur();
      if (isInMix(track)) {
        removeFromMix(track);
        showMixToast("Quitada de Mi Lista");
      } else {
        addToMix([track]);
        showMixToast(playingFromMix ? "+1 al final de Mi Lista" : "+1 a Mi Lista");
      }
      const inMix = isInMix(track);
      btn.classList.toggle('is-in-mix', inMix);
      btn.innerHTML = inMix ? MIX_CHECK_SVG : MIX_PLUS_SVG;
      btn.title = inMix ? 'En Mi Lista (toca para quitar)' : 'Agregar a Mi Lista';
    });
  }

  /** Agrega una carpeta entera (sus canciones directas, en orden A-Z). */
  function addFolderToMix(tracks, folderName) {
    const added = addToMix(tracks);
    if (added > 0) {
      showMixToast(`+${added} ${added === 1 ? 'video' : 'videos'} de ${folderName} a Mi Lista`);
    } else {
      showMixToast(`${folderName} ya está en Mi Lista`);
    }
  }

  // =================== RELOJ DE AUTO ===================
  function updateClock() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    clockDisplay.textContent = `${hours}:${minutes}`;
    // Reloj de la tira de volumen (la barra de arriba está oculta): horas arriba, minutos abajo
    document.getElementById('stripClockH').textContent = hours;
    document.getElementById('stripClockM').textContent = minutes;
  }
  setInterval(updateClock, 1000);
  updateClock();
  updateMixTabCount();

  // =================== CARGA Y REPRODUCCIÓN ===================
  function loadTrack(index, autoPlay = true) {
    if (index < 0) index = currentPlaylist.length - 1;
    if (index >= currentPlaylist.length) index = 0;
    currentTrackIndex = index;
    const track = currentPlaylist[currentTrackIndex];
    if (!track) return;
    loadedTrack = track;

    // Actualizar UI
    trackTitle.textContent = track.title;
    trackArtist.textContent = track.artist;
    updateTrackSource(track);
    formatBadge.textContent = track.format || 'MP4';
    totalDurationEl.textContent = track.duration;
    currentTimeEl.textContent = "00:00";
    progressCurrent.style.width = "0%";
    simCurrentSec = 0;

    // Carátula: con imagen, se ve completa y las barras animadas siguen encima (abajo, sobre una
    // sombra suave); el disco solo se muestra cuando no hay carátula
    albumPlaceholder.classList.remove('hidden');
    if (track.coverUrl) {
      albumArt.src = track.coverUrl;
      albumArt.classList.remove('hidden');
      albumContainer.classList.add('has-cover');
    } else {
      albumArt.classList.add('hidden');
      albumContainer.classList.remove('has-cover');
    }
    updateDynamicTheme(track);

    // Mensaje de saludo en cabecera
    currentPathLabel.textContent = "Q hubo jefe 😉";

    // Fuente de Audio
    if (window.AndroidBridge && window.AndroidBridge.playNativeAudio && track.filePath) {
      stopSimulatedAudio();
      audio.pause();
      if (autoPlay) playAudio();
    } else if (track.audioUrl) {
      stopSimulatedAudio();
      audio.src = track.audioUrl;
      audio.volume = isMuted ? 0 : (currentVolume / 100);
      if (autoPlay) playAudio();
    } else {
      // Video de muestra (se dibuja en el canvas): soltar el archivo anterior
      audio.pause();
      audio.removeAttribute('src');
      audio.load();
      if (autoPlay) startSimulatedAudio();
    }

    updateVideoSurface();

    // Mi Lista: el que empieza queda marcado como "ya visto"
    if (playingFromMix) {
      mixCurPos = currentTrackIndex;
      mixPlayedIds.add(track.id);
    }

    updatePreviews();

    renderList();

    // Que se vea la fila de la canción que empezó, moviendo la lista lo justo (no si ya se ve)
    scrollListToRow(trackListContainer.querySelector('.list-item.is-playing'), false);
  }

  /** Tarjetas Anterior / Siguiente. En Mi Lista, después de la última dice "Fin de Mi Lista". */
  function updatePreviews() {
    if (currentPlaylist.length > 0) {
      const prevIdx = (currentTrackIndex - 1 + currentPlaylist.length) % currentPlaylist.length;
      const nextIdx = (currentTrackIndex + 1) % currentPlaylist.length;
      const prevTrackObj = currentPlaylist[prevIdx];
      const nextTrackObj = currentPlaylist[nextIdx];
      const mixEnds = playingFromMix && !isShuffle && currentTrackIndex >= currentPlaylist.length - 1;

      if (prevTrackObj && prevTrackTitle) {
        prevTrackTitle.textContent = `${prevTrackObj.title} • ${prevTrackObj.artist}`;
        if (adjPrevCard) adjPrevCard.setAttribute('title', `Anterior: ${prevTrackObj.title} - ${prevTrackObj.artist}`);
      }
      if (mixEnds && nextTrackTitle) {
        nextTrackTitle.textContent = "■ Fin de Mi Lista";
        if (adjNextCard) adjNextCard.setAttribute('title', 'Es el último video de Mi Lista');
      } else if (nextTrackObj && nextTrackTitle) {
        nextTrackTitle.textContent = `${nextTrackObj.title} • ${nextTrackObj.artist}`;
        if (adjNextCard) adjNextCard.setAttribute('title', `Siguiente: ${nextTrackObj.title} - ${nextTrackObj.artist}`);
      }
    }

    // Auto-scroll / Marquesina para títulos largos
    refreshAllMarquees();
  }

  // =================== AUTO-SCROLL / MARQUESINA PARA TÍTULOS LARGOS ===================
  function applyMarquee(element, container) {
    if (!element || !container) return;

    // Resetear para recalcular limpio
    element.classList.remove('is-scrolling');
    container.classList.remove('has-overflow');
    element.style.removeProperty('--scroll-dist');
    element.style.removeProperty('--scroll-dur');

    setTimeout(() => {
      const elWidth = element.scrollWidth;
      const containerWidth = container.clientWidth;
      const overflow = elWidth - containerWidth;

      if (overflow > 4) {
        container.classList.add('has-overflow');
        // Velocidad suave automotriz (pausa al inicio, desplazamiento gradual y pausa al final)
        const duration = Math.max(6, Math.min(16, (overflow / 25) + 4));
        element.style.setProperty('--scroll-dist', `-${overflow + 10}px`);
        element.style.setProperty('--scroll-dur', `${duration}s`);
        element.classList.add('is-scrolling');
      } else {
        container.classList.remove('has-overflow');
      }
    }, 80);
  }

  function refreshAllMarquees() {
    applyMarquee(trackTitle, trackTitle ? trackTitle.parentElement : null);
    applyMarquee(prevTrackTitle, prevTrackTitle ? prevTrackTitle.parentElement : null);
    applyMarquee(nextTrackTitle, nextTrackTitle ? nextTrackTitle.parentElement : null);
    
    // Auto-scroll / marquesina en chip de carpeta activa si su nombre es largo
    const chipText = document.getElementById('chipFolderText');
    const chipWrap = chipFilterFolder ? chipFilterFolder.querySelector('.chip-folder-text-wrap') : null;
    if (chipText && chipWrap) {
      applyMarquee(chipText, chipWrap);
    }

    // Auto-scroll / marquesina también para la canción que está sonando en la lista de pistas
    if (trackListContainer) {
      const activeItemTitle = trackListContainer.querySelector('.list-item.is-playing .item-title');
      if (activeItemTitle) {
        applyMarquee(activeItemTitle, activeItemTitle.parentElement);
      }
    }
  }

  let nativeTrackerInterval = null;

  function startNativeAudioTracker() {
    clearInterval(nativeTrackerInterval);
    nativeTrackerInterval = setInterval(() => {
      if (!isPlaying) return;
      if (window.AndroidBridge && window.AndroidBridge.getNativeAudioPosition) {
        const posMs = window.AndroidBridge.getNativeAudioPosition();
        const durMs = window.AndroidBridge.getNativeAudioDuration();
        if (durMs > 0) {
          const percent = (posMs / durMs) * 100;
          progressCurrent.style.width = `${percent}%`;
          currentTimeEl.textContent = formatTime(Math.floor(posMs / 1000));
          totalDurationEl.textContent = formatTime(Math.floor(durMs / 1000));
        }
      }
    }, 400);
  }

  function playAudio() {
    const track = currentPlaylist[currentTrackIndex];
    if (!track) return;

    // Si estamos en la pantalla del auto, usar el MediaPlayer nativo de Android
    if (window.AndroidBridge && window.AndroidBridge.playNativeAudio && track.filePath) {
      window.AndroidBridge.playNativeAudio(track.filePath);
      isPlaying = true;
      updatePlayStateUI();
      startNativeAudioTracker();
      return;
    }

    if (track.audioUrl) {
      audio.play().then(() => {
        isPlaying = true;
        updatePlayStateUI();
      }).catch(err => {
        console.warn("Audio autoplay bloqueado por el navegador:", err);
        startSimulatedAudio();
      });
    } else {
      startSimulatedAudio();
    }
  }

  function pauseAudio() {
    cancelAutoMix(); // pausa en medio de una mezcla: queda solo la canción nueva
    isPlaying = false;
    clearInterval(nativeTrackerInterval);
    if (window.AndroidBridge && window.AndroidBridge.pauseNativeAudio) {
      window.AndroidBridge.pauseNativeAudio();
    }
    audio.pause();
    stopSimulatedAudio();
    updatePlayStateUI();
  }

  function togglePlayPause() {
    if (isPlaying) {
      pauseAudio();
    } else {
      playAudio();
    }
  }

  /**
   * Debajo del título: la carpeta de la canción, o "Mi Lista · 2 de 4" si lo que suena es Mi Lista
   * (así se sabe que Siguiente va a la siguiente de Mi Lista y cuánto falta).
   */
  function updateTrackSource(track) {
    if (!track) return;
    if (playingFromMix) {
      trackAlbum.innerHTML = `<span class="mix-source">${MIX_TAB_ICON}Mi Lista</span><span class="mix-source-pos"> · ${currentTrackIndex + 1} de ${currentPlaylist.length}</span>`;
    } else {
      trackAlbum.textContent = track.album || track.folder;
    }
    updateMixLiveDot();
  }

  /** Punto de la pestaña Mi Lista: visible mientras la cola es Mi Lista; late si está sonando. */
  function updateMixLiveDot() {
    const dot = document.getElementById('mixLiveDot');
    if (!dot) return;
    dot.classList.toggle('hidden', !playingFromMix);
    dot.classList.toggle('is-playing', playingFromMix && isPlaying);
  }

  function updatePlayStateUI() {
    updateMixLiveDot();
    // El ecualizador del botón de la carpeta que suena se mueve solo mientras suena
    document.body.classList.toggle('is-playing', isPlaying);
    const pillStatusText = document.getElementById('pillStatusText');
    if (isPlaying) {
      playIcon.classList.add('hidden');
      pauseIcon.classList.remove('hidden');
      albumContainer.classList.add('is-playing');
      nowPlayingPillEl.classList.add('is-playing');
      if (pillStatusText) {
        pillStatusText.textContent = isMixing ? "MIXING ⇄" : "PLAYING";
      }
      if (driverModeBadge) {
        driverModeBadge.classList.add('is-playing');
        driverModeBadge.classList.remove('is-paused');
      }
      if (playbackStatusText) {
        playbackStatusText.textContent = isMixing ? "MEZCLANDO ⇄"
          : (isAutoMix ? "REPRODUCIENDO · AUTOMIX" : "REPRODUCIENDO");
      }
    } else {
      playIcon.classList.remove('hidden');
      pauseIcon.classList.add('hidden');
      albumContainer.classList.remove('is-playing');
      nowPlayingPillEl.classList.remove('is-playing');
      if (pillStatusText) {
        pillStatusText.textContent = "PAUSED";
      }
      if (driverModeBadge) {
        driverModeBadge.classList.remove('is-playing');
        driverModeBadge.classList.add('is-paused');
      }
      if (playbackStatusText) {
        playbackStatusText.textContent = "EN PAUSA";
      }
    }
    updateVideoSurface();
    renderList();
  }

  // Generador de audio de prueba interactivo (Web Audio API)
  function startSimulatedAudio() {
    isPlaying = true;
    updatePlayStateUI();
    clearInterval(simInterval);

    // Sonido sutil para saber que está sonando algo real en los altavoces
    try {
      if (!audioContext) {
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (audioContext.state === 'suspended') {
        audioContext.resume();
      }
    } catch(e) {}

    simInterval = setInterval(() => {
      if (!isPlaying) return;
      simCurrentSec++;
      const track = currentPlaylist[currentTrackIndex];
      const maxSec = track ? track.durationSec : 240;

      // Automix: al llegar a los últimos segundos, pasar a la siguiente mezclando
      if (isAutoMix && !isMixing && !isRepeat && currentPlaylist.length > 1) {
        // En Mi Lista no se vuelve a empezar: después de la última no hay mezcla (se detiene)
        const nextIdx = playingFromMix ? nextMixIndex() : pickAutoMixNextIndex();
        const next = nextIdx >= 0 ? currentPlaylist[nextIdx] : null;
        const mixSec = autoMixSecondsFor(track, next);
        if (mixSec > 0 && maxSec - simCurrentSec <= mixSec) {
          startAutoMix(nextIdx, mixSec);
          return;
        }
      }

      if (simCurrentSec >= maxSec) {
        simCurrentSec = 0;
        if (isRepeat) {
          loadTrack(currentTrackIndex, true);
        } else {
          nextTrack();
        }
        return;
      }

      const percent = (simCurrentSec / maxSec) * 100;
      progressCurrent.style.width = `${percent}%`;
      currentTimeEl.textContent = formatTime(simCurrentSec);
    }, 1000);
  }

  function stopSimulatedAudio() {
    clearInterval(simInterval);
  }

  function nextTrack() {
    cancelAutoMix();
    if (playingFromMix) {
      const idx = nextMixIndex();
      if (idx < 0) {
        finishMix(); // era la última de Mi Lista
      } else {
        loadTrack(idx, true);
      }
      return;
    }
    if (isShuffle) {
      const rand = Math.floor(Math.random() * currentPlaylist.length);
      loadTrack(rand, true);
    } else {
      loadTrack(currentTrackIndex + 1, true);
    }
  }

  function prevTrack() {
    cancelAutoMix();
    if (simCurrentSec > 3 || (playingFromMix && currentTrackIndex === 0)) {
      // Si ya avanzó más de 3 segundos (o es la primera de Mi Lista), reiniciar la pista
      simCurrentSec = 0;
      currentTimeEl.textContent = "00:00";
      progressCurrent.style.width = "0%";
      if (audio.src) audio.currentTime = 0;
    } else {
      loadTrack(currentTrackIndex - 1, true);
    }
  }

  function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  // Eventos del Audio real si se cargó un archivo
  audio.addEventListener('timeupdate', () => {
    if (!audio.duration) return;
    const percent = (audio.currentTime / audio.duration) * 100;
    progressCurrent.style.width = `${percent}%`;
    currentTimeEl.textContent = formatTime(audio.currentTime);
  });

  audio.addEventListener('ended', () => {
    if (isRepeat) {
      audio.currentTime = 0;
      audio.play();
    } else {
      nextTrack();
    }
  });

  // Salto en la barra de progreso
  progressBarContainer.addEventListener('click', (e) => {
    const rect = progressBarContainer.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const ratio = Math.max(0, Math.min(1, clickX / rect.width));
    const track = currentPlaylist[currentTrackIndex];

    if (window.AndroidBridge && window.AndroidBridge.seekNativeAudio && track && track.filePath) {
      const durMs = window.AndroidBridge.getNativeAudioDuration();
      if (durMs > 0) {
        window.AndroidBridge.seekNativeAudio(Math.floor(ratio * durMs));
        progressCurrent.style.width = `${ratio * 100}%`;
        currentTimeEl.textContent = formatTime(Math.floor((ratio * durMs) / 1000));
      }
    } else if (audio.src && audio.duration) {
      audio.currentTime = ratio * audio.duration;
    } else if (track) {
      simCurrentSec = Math.floor(ratio * track.durationSec);
      progressCurrent.style.width = `${ratio * 100}%`;
      currentTimeEl.textContent = formatTime(simCurrentSec);
    }
  });

  // =================== SISTEMA DE VOLUMEN RÁPIDO (BORDE DEL CONDUCTOR) ===================
  const volMeterWrap = document.getElementById('volMeterWrap');
  const volMeterFill = document.getElementById('volMeterFill');
  const volMeterThumb = document.getElementById('volMeterThumb');

  const blindVolumeHud = document.getElementById('blindVolumeHud');
  const blindHudFill = document.getElementById('blindHudFill');
  const blindHudPercent = document.getElementById('blindHudPercent');
  const blindHudIcon = document.getElementById('blindHudIcon');
  let hudTimeout = null;

  function updateVolumeHud(vol, show = true) {
    if (!blindVolumeHud) return;
    if (blindHudFill) blindHudFill.style.height = `${vol}%`;
    if (blindHudPercent) blindHudPercent.textContent = `${vol}%`;

    if (blindHudIcon) {
      if (vol === 0 || isMuted) {
        blindHudIcon.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>`;
        blindHudIcon.style.color = '#ef4444';
      } else if (vol < 35) {
        blindHudIcon.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`;
        blindHudIcon.style.color = '#38bdf8';
      } else {
        blindHudIcon.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`;
        blindHudIcon.style.color = '#38bdf8';
      }
    }

    if (show) {
      blindVolumeHud.classList.add('is-visible');
      if (hudTimeout) clearTimeout(hudTimeout);
      hudTimeout = setTimeout(() => {
        blindVolumeHud.classList.remove('is-visible');
      }, 1100);
    }
  }

  function setVolume(val, showHud = false) {
    currentVolume = Math.max(0, Math.min(100, Math.round(val)));
    volPercent.textContent = `${currentVolume}%`;
    if (volMeterFill) volMeterFill.style.height = `${currentVolume}%`;
    if (volMeterThumb) volMeterThumb.style.bottom = `${currentVolume}%`;
    
    if (currentVolume === 0) {
      isMuted = true;
    } else {
      isMuted = false;
    }
    updateMuteUI();

    audio.volume = isMuted ? 0 : (currentVolume / 100);

    if (showHud) {
      updateVolumeHud(currentVolume, true);
    }

    // Integración directa con el sistema de audio de la pantalla del auto
    if (window.AndroidBridge && window.AndroidBridge.setSystemVolume) {
      try { window.AndroidBridge.setSystemVolume(currentVolume); } catch(e) {}
    }
  }

  function toggleMute() {
    if (isMuted) {
      isMuted = false;
      setVolume(previousVolume > 0 ? previousVolume : 50, true);
    } else {
      previousVolume = currentVolume;
      isMuted = true;
      audio.volume = 0;
      volPercent.textContent = "0%";
      if (volMeterFill) volMeterFill.style.height = "0%";
      if (volMeterThumb) volMeterThumb.style.bottom = "0%";
      updateVolumeHud(0, true);
    }
    updateMuteUI();

    // Integración mute nativo en auto
    if (window.AndroidBridge && window.AndroidBridge.muteSystemVolume) {
      try { window.AndroidBridge.muteSystemVolume(isMuted); } catch(e) {}
    }
  }

  function updateMuteUI() {
    if (isMuted) {
      btnMute.classList.add('is-muted');
      iconUnmuted.classList.add('hidden');
      iconMuted.classList.remove('hidden');
    } else {
      btnMute.classList.remove('is-muted');
      iconUnmuted.classList.remove('hidden');
      iconMuted.classList.add('hidden');
    }
  }

  // Interacción táctil y de arrastre en la barra vertical
  if (volMeterWrap) {
    let isDraggingVol = false;

    function updateVolFromPointer(e) {
      const rect = volMeterWrap.getBoundingClientRect();
      const clientY = e.clientY;
      const offsetY = clientY - rect.top;
      const ratio = 1 - (offsetY / rect.height);
      const val = Math.max(0, Math.min(100, Math.round(ratio * 100)));
      setVolume(val, true);
    }

    volMeterWrap.addEventListener('pointerdown', (e) => {
      isDraggingVol = true;
      volMeterWrap.setPointerCapture(e.pointerId);
      updateVolFromPointer(e);
    });

    volMeterWrap.addEventListener('pointermove', (e) => {
      if (isDraggingVol) updateVolFromPointer(e);
    });

    volMeterWrap.addEventListener('pointerup', (e) => {
      isDraggingVol = false;
      try { volMeterWrap.releasePointerCapture(e.pointerId); } catch(err) {}
    });

    volMeterWrap.addEventListener('pointercancel', () => {
      isDraggingVol = false;
    });
  }

  btnVolDown.addEventListener('click', () => setVolume(currentVolume - 10, true));
  btnVolUp.addEventListener('click', () => setVolume(currentVolume + 10, true));
  btnMute.addEventListener('click', toggleMute);

  // =================== GESTO CIEGO VERTICAL: CONTROL DE VOLUMEN EN PANTALLA ===================
  let isBlindVolDragging = false;
  let blindVolStartX = 0;
  let blindVolStartY = 0;
  let blindVolStartVal = 0;
  let blindVolPointerId = null;
  let hasPassedDeadzone = false;
  let suppressClickAfterDrag = false;

  function canStartBlindVolumeGesture(target, clientX, clientY) {
    if (!target) return false;
    // Solo excluir si se está escribiendo activamente en la caja de búsqueda
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
      return false;
    }
    // No interferir si se arrastra horizontalmente la barra de avance de video (seekbar)
    if (target.closest('#progressBarContainer')) {
      return false;
    }
    // No en el explorador (igual que la app): ahí deslizar arriba / abajo es el scroll de la lista
    if (target.closest('.explorer-panel')) {
      return false;
    }
    // Excluir la franja libre de jalar/ocultar el explorador (reservada para swipe horizontal del panel)
    if (typeof isInsideExplorerDragZone === 'function' && isInsideExplorerDragZone(clientX, clientY)) {
      return false;
    }
    // Toda la pantalla restante responde al gesto ciego
    return true;
  }

  window.addEventListener('pointerdown', (e) => {
    if (e.button !== 0 && e.pointerType === 'mouse') return;
    if (!canStartBlindVolumeGesture(e.target, e.clientX, e.clientY)) return;

    isBlindVolDragging = true;
    hasPassedDeadzone = false;
    blindVolStartX = e.clientX;
    blindVolStartY = e.clientY;
    blindVolStartVal = currentVolume;
    blindVolPointerId = e.pointerId;
  }, { passive: true });

  window.addEventListener('pointermove', (e) => {
    if (!isBlindVolDragging || e.pointerId !== blindVolPointerId) return;

    const dx = e.clientX - blindVolStartX;
    const dy = blindVolStartY - e.clientY; // Hacia arriba = subir (+), hacia abajo = bajar (-)

    if (!hasPassedDeadzone) {
      // Si el movimiento es horizontal (ej. jalar panel o deslizar): CANCELAR volumen de inmediato
      if (Math.abs(dx) > 14 && Math.abs(dx) > Math.abs(dy)) {
        isBlindVolDragging = false;
        return;
      }
      // Solo activar si el gesto es netamente VERTICAL (bloqueo direccional)
      if (Math.abs(dy) > 14 && Math.abs(dy) > Math.abs(dx) * 1.2) {
        hasPassedDeadzone = true;
        suppressClickAfterDrag = true;
      } else {
        return;
      }
    }

    // 240px de recorrido vertical = 100% de volumen (progresión suave y controlable)
    const deltaPercent = (dy / 240) * 100;
    const targetVol = Math.max(0, Math.min(100, Math.round(blindVolStartVal + deltaPercent)));
    setVolume(targetVol, true);
  }, { passive: true });

  const endBlindVolDrag = (e) => {
    if (isBlindVolDragging && (!e || e.pointerId === blindVolPointerId)) {
      isBlindVolDragging = false;
      hasPassedDeadzone = false;
      blindVolPointerId = null;
      if (suppressClickAfterDrag) {
        setTimeout(() => { suppressClickAfterDrag = false; }, 120);
      }
    }
  };

  window.addEventListener('pointerup', endBlindVolDrag, { passive: true });
  window.addEventListener('pointercancel', endBlindVolDrag, { passive: true });

  // Evitar clicks accidentales tras soltar el arrastre de volumen
  window.addEventListener('click', (e) => {
    if (suppressClickAfterDrag) {
      e.stopPropagation();
      e.preventDefault();
      suppressClickAfterDrag = false;
    }
  }, true);

  // =================== BOTONES DE NAVEGACIÓN Y CONTROL ===================
  btnPlayPause.addEventListener('click', togglePlayPause);

  // Click en el cuadro grande de la carátula: Pausa / Reproduce rápido (fácil toque en conducción)
  if (albumContainer) {
    albumContainer.style.cursor = 'pointer';
    albumContainer.setAttribute('title', 'Tocar para Pausar / Reproducir');
    albumContainer.addEventListener('click', onVideoFrameTap);
  }

  // Feedback visual de adelanto/retroceso centrado en el reproductor
  let seekToastTimer = null;
  function showSeekFeedback(text) {
    let toast = document.getElementById('seekFeedbackToast');
    const container = document.getElementById('playerPanel') || document.body;
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'seekFeedbackToast';
      toast.style.cssText = 'position:absolute;top:45%;left:50%;transform:translate(-50%,-50%);background:rgba(12,16,24,0.92);border:1px solid rgba(0,210,255,0.45);box-shadow:0 8px 24px rgba(0,0,0,0.8),0 0 16px rgba(0,210,255,0.3);color:#00d2ff;padding:10px 22px;border-radius:14px;font-size:1.15rem;font-weight:800;font-family:var(--font-mono);z-index:999;pointer-events:none;transition:opacity 0.25s,transform 0.25s;backdrop-filter:blur(8px);white-space:nowrap;';
      container.appendChild(toast);
    } else if (toast.parentElement !== container) {
      container.appendChild(toast);
    }
    toast.textContent = text;
    toast.style.opacity = '1';
    toast.style.transform = 'translate(-50%,-50%) scale(1.05)';
    clearTimeout(seekToastTimer);
    seekToastTimer = setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translate(-50%,-50%) scale(0.95)';
    }, 800);
  }

  function seekRelative(deltaSec, consecutiveHits = 1) {
    const track = currentPlaylist[currentTrackIndex];
    if (!track) return;
    if (isMixing) cancelAutoMix();

    let curSec = 0;
    let totalSec = track.durationSec || 0;

    if (window.AndroidBridge && window.AndroidBridge.seekNativeAudio && track.filePath) {
      const durMs = window.AndroidBridge.getNativeAudioDuration() || (totalSec * 1000);
      const curMs = (window.AndroidBridge.getNativeAudioPosition ? window.AndroidBridge.getNativeAudioPosition() : 0);
      let targetMs = curMs + (deltaSec * 1000);
      if (targetMs < 0) targetMs = 0;
      if (durMs > 0 && targetMs > durMs) targetMs = Math.max(0, durMs - 1000);
      window.AndroidBridge.seekNativeAudio(Math.floor(targetMs));
      curSec = Math.floor(targetMs / 1000);
      totalSec = Math.floor(durMs / 1000);
    } else if (audio.src && audio.duration) {
      audio.currentTime = Math.max(0, Math.min(audio.duration - 1, audio.currentTime + deltaSec));
      curSec = Math.floor(audio.currentTime);
      totalSec = Math.floor(audio.duration);
    } else {
      simCurrentSec = Math.max(0, Math.min(track.durationSec - 1, simCurrentSec + deltaSec));
      curSec = simCurrentSec;
    }

    if (totalSec > 0) {
      const ratio = Math.max(0, Math.min(1, curSec / totalSec));
      progressCurrent.style.width = `${ratio * 100}%`;
      currentTimeEl.textContent = formatTime(curSec);
    }

    const totalSecSkipped = (deltaSec > 0 ? 10 : -10) * Math.max(1, consecutiveHits);
    const label = totalSecSkipped > 0 ? `+${totalSecSkipped}s` : `${totalSecSkipped}s`; // solo texto, sin ícono
    showSeekFeedback(label);
  }

  // 1 Touch = Siguiente / Anterior pista
  // 2 Touch (Doble toque en 280ms) = Adelanta / Retrocede 10 segundos
  const DOUBLE_TAP_TIMEOUT_MS = 280;
  let nextTapTimer = null;
  let prevTapTimer = null;
  let nextTapHits = 0;
  let prevTapHits = 0;
  let resetNextHitsTimer = null;
  let resetPrevHitsTimer = null;

  function handleNextTouch() {
    if (nextTapTimer) {
      clearTimeout(nextTapTimer);
      nextTapTimer = null;
    }
    nextTapHits++;
    if (nextTapHits === 1) {
      nextTapTimer = setTimeout(() => {
        nextTapHits = 0;
        nextTapTimer = null;
        nextTrack();
      }, DOUBLE_TAP_TIMEOUT_MS);
    } else {
      seekRelative(10, nextTapHits - 1);
      clearTimeout(resetNextHitsTimer);
      resetNextHitsTimer = setTimeout(() => { nextTapHits = 0; }, 450);
    }
  }

  function handlePrevTouch() {
    if (prevTapTimer) {
      clearTimeout(prevTapTimer);
      prevTapTimer = null;
    }
    prevTapHits++;
    if (prevTapHits === 1) {
      prevTapTimer = setTimeout(() => {
        prevTapHits = 0;
        prevTapTimer = null;
        prevTrack();
      }, DOUBLE_TAP_TIMEOUT_MS);
    } else {
      seekRelative(-10, prevTapHits - 1);
      clearTimeout(resetPrevHitsTimer);
      resetPrevHitsTimer = setTimeout(() => { prevTapHits = 0; }, 450);
    }
  }

  btnNext.addEventListener('click', handleNextTouch);
  btnPrev.addEventListener('click', handlePrevTouch);

  // =================== ZONAS DE ANTERIOR / PLAY / SIGUIENTE PARA EL AUTO (igual que la app) ===================
  // Toda la franja de abajo del reproductor (debajo de los tiempos, hasta el borde) responde, en tres
  // columnas: el play con 24px de más a cada lado, a su izquierda anterior y a su derecha siguiente.
  // Solo se ilumina el botón. En pantalla completa no se usa: tiene sus propios botones
  const PLAY_ZONE_EXTRA = 24;
  const playerPanel = document.getElementById('playerPanel');
  const navZoneTop = document.querySelector('.progress-section');

  function navZoneAt(x, y) {
    if (frameWrapper.classList.contains('is-video-fs')) return null;
    const panel = playerPanel.getBoundingClientRect();
    const top = navZoneTop.getBoundingClientRect().bottom;
    const play = btnPlayPause.getBoundingClientRect();
    if (x < panel.left || x > panel.right || y < top || y > panel.bottom) return null;
    if (x < play.left - PLAY_ZONE_EXTRA) return btnPrev;
    if (x > play.right + PLAY_ZONE_EXTRA) return btnNext;
    return btnPlayPause;
  }

  playerPanel.addEventListener('pointerdown', (e) => {
    const target = navZoneAt(e.clientX, e.clientY);
    if (!target) return;
    target.classList.add('pressed');
    setTimeout(() => target.classList.remove('pressed'), 180);
  }, true);

  // Un clic fuera de los botones, dentro de la franja, va al botón de su columna
  playerPanel.addEventListener('click', (e) => {
    const target = navZoneAt(e.clientX, e.clientY);
    if (!target || target.contains(e.target)) return;
    e.stopPropagation();
    target.click();
  }, true);

  // Clicks directos en las tarjetas flotantes
  if (adjPrevCard) {
    adjPrevCard.addEventListener('click', handlePrevTouch);
  }
  if (adjNextCard) {
    adjNextCard.addEventListener('click', handleNextTouch);
  }

  // Alternador de Buscador de Música y Archivos / Modo Reproductor Completo
  let isPlayerExpanded = false;

  function setPlayerExpanded(expanded) {
    if (!dashboardGrid || !btnToggleExpand) return;
    isPlayerExpanded = expanded;
    if (isPlayerExpanded) {
      dashboardGrid.classList.add('is-expanded');
      btnToggleExpand.classList.add('is-active');
      btnToggleExpand.title = "Mostrar explorador de música y carpetas";
    } else {
      dashboardGrid.classList.remove('is-expanded');
      btnToggleExpand.classList.remove('is-active');
      btnToggleExpand.title = "Ocultar explorador (reproductor a pantalla completa)";
    }
    setTimeout(refreshAllMarquees, 320);
  }

  function toggleExplorer() {
    setPlayerExpanded(!isPlayerExpanded);
  }

  // Soporta tanto click/toque directo como gesto de arrastre
  if (btnToggleExpand) {
    btnToggleExpand.style.cursor = "pointer";
    btnToggleExpand.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleExplorer();
    });
  }

  // =================== ZONA EXCLUSIVA DE GESTOS TÁCTILES / ARRASTRE ===================
  // El área de touch y arrastrar para alternar el explorador se restringe ÚNICAMENTE
  // a la zona delimitada por el botón de explorador y la franja vertical a su lado (área roja)
  let isDragGestureActive = false;
  let touchStartX = 0;
  let touchStartY = 0;

  function isInsideExplorerDragZone(clientX, clientY) {
    if (!btnToggleExpand || !playerPanel) return false;
    const btnRect = btnToggleExpand.getBoundingClientRect();
    const playerRect = playerPanel.getBoundingClientRect();
    const albumContainer = document.getElementById('albumContainer');
    const albumRect = albumContainer ? albumContainer.getBoundingClientRect() : null;

    // Área roja delimitada por Hernan:
    // - Horizontal: Desde el borde izquierdo del panel del reproductor hasta pasar el botón (+20px)
    // - Vertical: Desde la parte superior del botón (-10px) hasta el fondo de la carátula (+10px)
    const minX = Math.max(0, Math.min(playerRect.left, btnRect.left - 15));
    const maxX = btnRect.right + 22;
    const minY = Math.max(0, btnRect.top - 10);
    const maxY = albumRect ? (albumRect.bottom + 10) : (btnRect.bottom + 200);

    return clientX >= minX && clientX <= maxX && clientY >= minY && clientY <= maxY;
  }

  function handleSwipeAction(dx, dy) {
    if (Math.abs(dx) < 35 || Math.abs(dx) < Math.abs(dy) * 1.1) return;

    // Con el explorador en la izquierda:
    // Si el explorador está ABIERTO: arrastrar hacia la izquierda (<-) lo CIERRA (expande reproductor)
    if (!isPlayerExpanded && dx < -35) {
      setPlayerExpanded(true);
    }
    // Si el explorador está CERRADO: arrastrar hacia la derecha (->) lo ABRE
    else if (isPlayerExpanded && dx > 35) {
      setPlayerExpanded(false);
    }
  }

  // Toques en pantalla táctil (solo inician si el dedo cayó en el área roja)
  window.addEventListener('touchstart', (e) => {
    if (e.touches && e.touches.length === 1) {
      const x = e.touches[0].clientX;
      const y = e.touches[0].clientY;
      if (isInsideExplorerDragZone(x, y)) {
        isDragGestureActive = true;
        touchStartX = x;
        touchStartY = y;
      } else {
        isDragGestureActive = false;
      }
    }
  }, { passive: true });

  window.addEventListener('touchend', (e) => {
    if (isDragGestureActive && e.changedTouches && e.changedTouches.length === 1) {
      const dx = e.changedTouches[0].clientX - touchStartX;
      const dy = e.changedTouches[0].clientY - touchStartY;
      isDragGestureActive = false;
      handleSwipeAction(dx, dy);
    }
  }, { passive: true });

  // Arrastre con ratón en PC (solo en el área roja)
  let isMouseDraggingZone = false;
  let mouseStartX = 0;
  let mouseStartY = 0;

  window.addEventListener('mousedown', (e) => {
    if (e.button === 0 && isInsideExplorerDragZone(e.clientX, e.clientY)) {
      isMouseDraggingZone = true;
      mouseStartX = e.clientX;
      mouseStartY = e.clientY;
    }
  });

  window.addEventListener('mouseup', (e) => {
    if (isMouseDraggingZone) {
      const dx = e.clientX - mouseStartX;
      const dy = e.clientY - mouseStartY;
      isMouseDraggingZone = false;
      handleSwipeAction(dx, dy);
    }
  });

  window.addEventListener('resize', () => {
    refreshAllMarquees();
  });

  // =================== MENÚ ☰: ALEATORIO Y REPETIR VIDEO (igual que la app) ===================
  // Se abre encima del reproductor o, en pantalla completa, encima del video. Se cierra solo a los 5 s
  // sin tocarlo (cada toque en una opción vuelve a contar), tocando fuera o en ✕
  const PLAYER_MENU_AUTO_CLOSE_MS = 5000;
  let playerMenuTimer = null;

  function setPlayerMenuOpen(open) {
    clearTimeout(playerMenuTimer);
    if (open) {
      const inFs = frameWrapper.classList.contains('is-video-fs');
      const over = (inFs ? albumContainer : playerPanel).getBoundingClientRect();
      const base = frameWrapper.getBoundingClientRect();
      Object.assign(playerMenuLayer.style, {
        left: (over.left - base.left) + 'px',
        top: (over.top - base.top) + 'px',
        width: over.width + 'px',
        height: over.height + 'px'
      });
      refreshPlayerMenu();
      clearTimeout(fsHideTimer); // en pantalla completa los controles no se ocultan con el menú abierto
      playerMenuTimer = setTimeout(() => setPlayerMenuOpen(false), PLAYER_MENU_AUTO_CLOSE_MS);
    }
    const wasOpen = !playerMenuLayer.classList.contains('hidden');
    playerMenuLayer.classList.toggle('hidden', !open);
    if (!open && wasOpen && frameWrapper.classList.contains('is-video-fs')) showFsOverlay(true);
  }

  function refreshPlayerMenu() {
    menuShuffle.classList.toggle('is-on', isShuffle);
    menuShuffle.querySelector('.player-menu-state').textContent = isShuffle ? 'ON' : 'OFF';
    menuRepeat.classList.toggle('is-on', isRepeat);
    menuRepeat.querySelector('.player-menu-state').textContent = isRepeat ? 'ON' : 'OFF';
    // Indicadores sobre el video y debajo del título en pantalla completa
    document.querySelectorAll('[data-mode-icon]').forEach(icon => {
      icon.classList.toggle('hidden', !(icon.dataset.modeIcon === 'repeat' ? isRepeat : isShuffle));
    });
  }

  // Aleatorio y repetir recuerdan cómo quedaron (en la app, en sus preferencias)
  function saveModes() {
    try {
      localStorage.setItem('jhvideo_shuffle', isShuffle ? '1' : '0');
      localStorage.setItem('jhvideo_repeat', isRepeat ? '1' : '0');
    } catch (err) {}
  }
  try {
    isShuffle = localStorage.getItem('jhvideo_shuffle') === '1';
    isRepeat = localStorage.getItem('jhvideo_repeat') === '1';
  } catch (err) {}
  refreshPlayerMenu();

  document.getElementById('btnVideoMenu').addEventListener('click', (e) => {
    e.stopPropagation();
    setPlayerMenuOpen(true);
  });
  document.getElementById('playerMenuBackdrop').addEventListener('click', () => setPlayerMenuOpen(false));
  document.getElementById('playerMenuClose').addEventListener('click', () => setPlayerMenuOpen(false));

  menuShuffle.addEventListener('click', () => {
    isShuffle = !isShuffle;
    saveModes();
    setPlayerMenuOpen(true);
  });

  menuRepeat.addEventListener('click', () => {
    isRepeat = !isRepeat;
    saveModes();
    setPlayerMenuOpen(true);
  });

  // Re-escanear USB (antes en la barra de arriba): cierra el menú y hace lo mismo que aquel botón
  document.getElementById('menuRescan').addEventListener('click', () => {
    setPlayerMenuOpen(false);
    document.getElementById('btnRescanUsb').click();
  });
  document.getElementById('menuVersion').textContent =
    'JH VIDEO ' + document.getElementById('appVersionBadge').textContent.replace('JH VIDEO ', '');

  // =================== SOLO AUDIO (foco) ===================
  // De noche la imagen puede encandilar, o no se permite video a la vista del conductor: tapa la imagen
  // en negro y el audio sigue, con los controles a mano. Sigue así al cambiar de video
  let audioOnly = false;
  const audioOnlyCover = document.getElementById('audioOnlyCover');

  function setAudioOnly(on) {
    audioOnly = on;
    audioOnlyCover.classList.toggle('hidden', !on);
    document.querySelectorAll('#btnAudioOnly, #fsAudioOnly').forEach(btn => btn.classList.toggle('is-on', on));
  }

  document.getElementById('btnAudioOnly').addEventListener('click', (e) => {
    e.stopPropagation();
    setAudioOnly(!audioOnly);
  });

  // =================== UTILIDADES DE BÚSQUEDA Y ALMACENAMIENTO ===================
  function normalizeText(text) {
    if (!text) return '';
    return text.toString()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  // Minúsculas conservando ñ y tildes: para premiar la escritura exacta ("ñaña" antes que "nana")
  function exactLower(text) {
    if (!text) return '';
    return text.toString().normalize('NFC').toLowerCase().trim();
  }

  // Términos tal como se escribieron; null si la consulta no trae ñ ni tildes (no hay nada que premiar)
  function exactTokens(rawQuery, normQuery) {
    const raw = exactLower(rawQuery);
    if (raw === normQuery) return null;
    return raw.split(/\s+/).filter(t => t.length > 0);
  }

  function exactSpellingBonus(titleText, fullText, rawTokens) {
    if (!rawTokens) return 0;
    const title = exactLower(titleText);
    const full = exactLower(fullText);
    let bonus = 0;
    rawTokens.forEach(token => {
      if (title.includes(token)) bonus += 20000;
      else if (full.includes(token)) bonus += 12000;
    });
    return bonus;
  }

  function dedupChars(s) {
    return s.replace(/([a-z])\1+/g, '$1');
  }

  function levenshtein(a, b) {
    if (a === b) return 0;
    if (!a.length) return b.length;
    if (!b.length) return a.length;
    const d = [];
    for (let i = 0; i <= a.length; i++) d[i] = [i];
    for (let j = 0; j <= b.length; j++) d[0][j] = j;
    for (let i = 1; i <= a.length; i++) {
      for (let j = 1; j <= b.length; j++) {
        const cost = a[i - 1] === b[j - 1] ? 0 : 1;
        d[i][j] = Math.min(
          d[i - 1][j] + 1,
          d[i][j - 1] + 1,
          d[i - 1][j - 1] + cost
        );
      }
    }
    return d[a.length][b.length];
  }

  function tokenMatchesWord(token, word) {
    if (!token || !word) return false;
    if (word.includes(token)) return true;
    
    // Deduplicación de letras consecutivas (ej. "guarracha" -> "guaracha")
    const tDedup = dedupChars(token);
    const wDedup = dedupChars(word);
    if (wDedup.includes(tDedup)) return true;
    if (tDedup.includes(wDedup) && wDedup.length >= 4) return true;

    // Distancia de edición para errores de tipeo al conducir
    const maxDist = token.length >= 6 ? 2 : (token.length >= 4 ? 1 : 0);
    if (maxDist > 0 && Math.abs(token.length - word.length) <= maxDist) {
      if (levenshtein(token, word) <= maxDist) return true;
    }
    // Prefijo
    if (word.startsWith(token) || (token.length >= 4 && word.startsWith(token.slice(0, -1)))) return true;

    return false;
  }

  function trackMatchesTokens(track, tokens) {
    const haystack = normalizeText(`${track.title} ${track.artist} ${track.album || ''} ${track.folder || ''} ${track.path || ''}`);
    const haystackDedup = dedupChars(haystack);
    const haystackWords = haystack.split(/[^a-z0-9]+/);

    return tokens.every(token => {
      // 1. Subcadena directa
      if (haystack.includes(token)) return true;
      // 2. Subcadena con letras duplicadas colapsadas
      const tokenDedup = dedupChars(token);
      if (haystackDedup.includes(tokenDedup)) return true;
      // 3. Comparación difusa palabra por palabra
      return haystackWords.some(word => tokenMatchesWord(token, word));
    });
  }

  // =================== PUNTUACIÓN DE RELEVANCIA ===================
  function getTrackRelevance(track, rawQuery, normQuery, tokens) {
    let score = 0;
    const titleNorm = normalizeText(track.title || '');
    const artistNorm = normalizeText(track.artist || '');
    const albumNorm = normalizeText(track.album || '');
    const folderNorm = normalizeText(track.folder || '');
    const titleWords = titleNorm.split(/[^a-z0-9]+/).filter(Boolean);
    const artistWords = artistNorm.split(/[^a-z0-9]+/).filter(Boolean);

    // 1. Coincidencia directa completa al inicio del título
    if (titleNorm === normQuery) {
      score += 25000;
    } else if (titleNorm.startsWith(normQuery + ' ') || titleNorm.startsWith(normQuery + '-')) {
      score += 18000;
    } else if (titleNorm.startsWith(normQuery)) {
      score += 12000;
    } else if (titleNorm.includes(normQuery)) {
      score += 6000;
    }

    // 2. Coincidencia token por token
    tokens.forEach(token => {
      // Título
      if (titleWords.includes(token)) {
        score += 8000;
      } else if (titleWords.some(w => w.startsWith(token))) {
        score += 4000;
      } else if (titleNorm.includes(token)) {
        score += 2000;
      } else if (titleWords.some(w => tokenMatchesWord(token, w))) {
        score += 300;
      }

      // Artista
      if (artistWords.includes(token)) {
        score += 3000;
      } else if (artistWords.some(w => w.startsWith(token))) {
        score += 1500;
      } else if (artistNorm.includes(token)) {
        score += 800;
      }

      // Carpeta / Álbum
      if (folderNorm.includes(token) || albumNorm.includes(token)) {
        score += 400;
      }
    });

    // 3. Escritura exacta con ñ/tildes: "ñaña" va antes que "nana"
    score += exactSpellingBonus(track.title,
      `${track.title} ${track.artist} ${track.album || ''} ${track.folder || ''} ${track.path || ''}`,
      exactTokens(rawQuery, normQuery));

    // Desempate fino: títulos más concisos y directos ganan prioridad
    score -= Math.min(titleNorm.length, 60);
    return score;
  }

  function getFolderRelevance(folder, rawQuery, normQuery, tokens) {
    let score = 0;
    const nameNorm = normalizeText(folder.name || '');
    const words = nameNorm.split(/[^a-z0-9]+/).filter(Boolean);

    if (nameNorm === normQuery) {
      score += 20000;
    } else if (nameNorm.startsWith(normQuery + ' ') || nameNorm.startsWith(normQuery)) {
      score += 12000;
    } else if (nameNorm.includes(normQuery)) {
      score += 5000;
    }

    tokens.forEach(token => {
      if (words.includes(token)) score += 6000;
      else if (words.some(w => w.startsWith(token))) score += 3000;
      else if (nameNorm.includes(token)) score += 1000;
    });

    score += exactSpellingBonus(folder.name, `${folder.name} ${folder.path || ''}`, exactTokens(rawQuery, normQuery));

    score -= Math.min(nameNorm.length, 50);
    return score;
  }

  // Icono oficial vectorial de USB (tridente universal)
  const USB_TRIDENT_SVG = `<svg class="usb-icon" viewBox="0 0 24 24"><path d="M15 7v4h1v2h-3V5h2l-3-4-3 4h2v8H8v-2.07c.7-.37 1.2-1.08 1.2-1.93 0-1.2-.99-2.18-2.2-2.18-1.21 0-2.2.98-2.2 2.18 0 .85.5 1.56 1.2 1.93V13c0 1.11.89 2 2 2h3v3.05c-.71.37-1.2 1.1-1.2 1.95a2.2 2.2 0 0 0 4.4 0c0-.85-.49-1.58-1.2-1.95V15h3c1.11 0 2-.89 2-2v-2h1V7h-4z"/></svg>`;

  function isSameUsb(track, targetUsbSource) {
    if (!targetUsbSource || !track) return true;
    const want2 = targetUsbSource.includes('2') || targetUsbSource.toLowerCase().includes('udisk2');
    const want1 = targetUsbSource.includes('1') || targetUsbSource.toLowerCase().includes('udisk1');
    const tUsb = track.usbSource || '';
    const tPath = (track.path || '').toLowerCase();
    const isTrack2 = tUsb === 'USB 2' || tPath.includes('udisk2') || tPath.includes('usb1');
    if (want2) return isTrack2;
    if (want1) return !isTrack2;
    return true;
  }

  // Generador de etiquetas para USB 1, USB 2 y Memoria Interna
  function getStorageBadgeHtml(path) {
    if (!path) return '';
    const p = path.toLowerCase();
    if (p.includes('udisk1') || p.includes('usb1')) {
      return `<span class="badge-storage usb1">${USB_TRIDENT_SVG} 1</span>`;
    } else if (p.includes('udisk2') || p.includes('usb2')) {
      return `<span class="badge-storage usb2">${USB_TRIDENT_SVG} 2</span>`;
    } else if (p.includes('sdcard') || p.includes('internal') || p.includes('emulated')) {
      return `<span class="badge-storage internal">INTERNA</span>`;
    }
    return '';
  }

  // Formateador de ruta limpia: en USB1 y USB2 omite '/storage/udiskX' para evitar redundancia
  function formatDisplayPath(path) {
    if (!path) return '';
    let p = path;
    if (p.startsWith('/storage/udisk1')) {
      p = p.replace('/storage/udisk1', '');
    } else if (p.startsWith('/storage/udisk2')) {
      p = p.replace('/storage/udisk2', '');
    }
    // Asegurar que comience con /
    if (!p.startsWith('/')) p = '/' + p;
    // Quitar barra final para estética limpia
    if (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
    return p;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // =================== FILTROS DE BÚSQUEDA Y USBS ===================
  let currentSearchFilter = 'folder'; // 'folder' | 'all' | 'usb1' | 'usb2'
  const searchFiltersBar = document.getElementById('searchFiltersBar');
  const filterChips = document.querySelectorAll('.filter-chip');
  const chipFilterFolder = document.getElementById('chipFilterFolder');
  const chipFilterAll = document.getElementById('chipFilterAll');
  const chipFilterUsb1 = document.getElementById('chipFilterUsb1');
  const chipFilterUsb2 = document.getElementById('chipFilterUsb2');

  function updateFolderChipContent() {
    if (!chipFilterFolder) return;

    let usbBadgeEl = document.getElementById('chipFolderUsbBadge');
    let textEl = document.getElementById('chipFolderText');

    if (!usbBadgeEl || !textEl) {
      chipFilterFolder.innerHTML = `
        <span class="chip-folder-icon">📁</span>
        <span class="chip-folder-usb-badge hidden" id="chipFolderUsbBadge"></span>
        <span class="chip-folder-text-wrap">
          <span class="chip-folder-text" id="chipFolderText"></span>
        </span>
      `;
      usbBadgeEl = document.getElementById('chipFolderUsbBadge');
      textEl = document.getElementById('chipFolderText');
    }

    const folderIconEl = chipFilterFolder.querySelector('.chip-folder-icon');

    let usbNum = '';
    if (currentFolderUsbSource) {
      const src = currentFolderUsbSource.toLowerCase();
      if (src.includes('1') || src.includes('udisk1')) usbNum = '1';
      else if (src.includes('2') || src.includes('udisk2')) usbNum = '2';
    }

    if (folderIconEl) folderIconEl.classList.remove('hidden');

    if (usbNum) {
      usbBadgeEl.innerHTML = `${USB_TRIDENT_SVG}<span>${usbNum}</span><span class="chip-folder-sep">|</span>`;
      usbBadgeEl.classList.remove('hidden');
    } else {
      usbBadgeEl.innerHTML = '';
      usbBadgeEl.classList.add('hidden');
    }

    textEl.textContent = currentFolder || '';
    const fullTitle = (usbNum ? `[USB ${usbNum}] ` : '') + (currentFolder || '');
    chipFilterFolder.setAttribute('title', fullTitle);
    chipFilterFolder.classList.remove('hidden');

    const wrapEl = chipFilterFolder.querySelector('.chip-folder-text-wrap');
    if (wrapEl && textEl) {
      setTimeout(() => applyMarquee(textEl, wrapEl), 80);
    }
  }

  function updateFilterChipsUI() {
    // La barra de filtros solo tiene utilidad en la pestaña Pistas (activeTab === 'playlist').
    // En Carpetas USB el árbol organiza los pendrives, y en Favoritos se muestra la lista VIP completa.
    if (searchFiltersBar) {
      searchFiltersBar.classList.toggle('hidden', activeTab !== 'playlist');
    }

    if (activeTab === 'playlist' && currentFolder) {
      updateFolderChipContent();
    } else {
      if (chipFilterFolder) {
        chipFilterFolder.classList.add('hidden');
        if (currentSearchFilter === 'folder') {
          currentSearchFilter = 'all';
        }
      }
    }

    const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
    const hasUsb1 = allPool.some(t => t.usbSource === 'USB 1' || (t.path && (t.path.includes('udisk1') || t.path.includes('usb0'))));
    const hasUsb2 = allPool.some(t => t.usbSource === 'USB 2' || (t.path && (t.path.includes('udisk2') || t.path.includes('usb1'))));
    const multipleUsb = hasUsb1 && hasUsb2;

    // Si solo hay un pendrive conectado, TODO ya hace referencia a ese único USB,
    // por lo que los chips USB 1 y USB 2 solo se muestran si ambos están presentes
    if (chipFilterUsb1) chipFilterUsb1.classList.toggle('hidden', !multipleUsb);
    if (chipFilterUsb2) chipFilterUsb2.classList.toggle('hidden', !multipleUsb);

    if (!multipleUsb && (currentSearchFilter === 'usb1' || currentSearchFilter === 'usb2')) {
      currentSearchFilter = 'all';
    }

    filterChips.forEach(chip => {
      const f = chip.getAttribute('data-filter');
      if (f === currentSearchFilter) {
        chip.classList.add('active');
      } else {
        chip.classList.remove('active');
      }
    });
  }

  filterChips.forEach(chip => {
    chip.addEventListener('click', () => {
      searchInput.blur();
      currentSearchFilter = chip.getAttribute('data-filter');
      updateFilterChipsUI();
      if (searchInput.value.trim().length > 0) {
        renderList();
      }
      // Tocar la carpeta actual lleva la lista a la canción que suena (por si te perdiste moviéndola)
      if (currentSearchFilter === 'folder') scrollListToPlaying();
    });
  });

  // Ocultar teclado táctil al tocar fuera del cuadro de búsqueda
  document.addEventListener('pointerdown', (e) => {
    if (!e.target.closest('.search-box')) {
      searchInput.blur();
    }
  });

  // Botón HOME Táctil Conductor (Misma función que el botón físico HOME del auto)
  const btnNavHome = document.getElementById('btnNavHome') || document.getElementById('btnNavBack');
  if (btnNavHome) {
    btnNavHome.addEventListener('click', () => {
      searchInput.blur();
      if (searchInput.value.trim().length > 0) {
        searchInput.value = '';
        clearSearch.classList.add('hidden');
      }
      if (tabButtons[0]) {
        tabButtons[0].click();
      }
    });
  }

  // =================== PESTAÑAS Y RENDERIZADO ===================
  // Función para revelar y desplegar automáticamente la carpeta de la canción que está sonando
  function revealPlayingFolderInTree() {
    const currentTrack = loadedTrack || currentPlaylist[currentTrackIndex];
    if (!currentTrack) return;

    const targetFolder = currentTrack.folder || currentFolder;
    const targetPath = currentTrack.path || '';
    const targetUsb = currentTrack.usbSource || (targetPath.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1');

    function findNodeChain(nodes, chain = []) {
      for (const node of nodes) {
        const nextChain = [...chain, node.id];
        // Buscar primero recursivamente en los hijos para priorizar el nodo exacto más profundo
        if (node.children && node.children.length > 0) {
          const found = findNodeChain(node.children, nextChain);
          if (found) return found;
        }

        const isExactMatch = (node.name === targetFolder) ||
          (targetPath && node.path && !node.isUsbRoot && (targetPath.replace(/\/+$/, '') === node.path.replace(/\/+$/, '')));

        if (isExactMatch) {
          return nextChain;
        }
      }
      return null;
    }

    const chain = findNodeChain(currentTreeData);
    if (chain && chain.length > 0) {
      chain.forEach(nodeId => expandedTreeNodes.add(nodeId));
    } else {
      if (targetUsb === 'USB 2') {
        expandedTreeNodes.add('usb2');
      } else {
        expandedTreeNodes.add('usb1');
      }
    }

    // Si la canción es de USB 2 y el filtro estaba en USB 1, poner en TODO para que aparezca
    if (targetUsb === 'USB 2' && currentSearchFilter === 'usb1') {
      currentSearchFilter = 'all';
    } else if (targetUsb === 'USB 1' && currentSearchFilter === 'usb2') {
      currentSearchFilter = 'all';
    }
  }

  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      searchInput.blur();
      // Limpiar búsqueda y loaders al cambiar de pestaña
      if (searchTimer) clearTimeout(searchTimer);
      searchInput.value = '';
      clearSearch.classList.add('hidden');
      if (searchSpinner) searchSpinner.classList.add('hidden');
      if (searchOverlayLoading) searchOverlayLoading.classList.add('hidden');
      trackListContainer.classList.remove('is-searching');

      tabButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeTab = btn.getAttribute('data-tab');
      if (activeTab === 'folders') {
        searchInput.placeholder = "Buscar entre carpetas...";
        currentSearchFilter = 'all';
        revealPlayingFolderInTree();
      } else if (activeTab === 'playlist') {
        searchInput.placeholder = "Buscar video, artista o carpeta...";
        if (currentFolder) {
          currentSearchFilter = 'folder';
        } else {
          currentSearchFilter = 'all';
        }
      } else if (activeTab === 'mymix') {
        mixConfirmClear = false;
        searchInput.placeholder = "Buscar en Mi Lista...";
        if (currentSearchFilter === 'folder') {
          currentSearchFilter = 'all';
        }
      }
      renderList();
      // Al volver a Pistas o Mi Lista: la lista va a la canción que está sonando (si está en esa lista)
      if (activeTab !== 'folders') {
        scrollListToPlaying();
      }
    });
  });

  /**
   * Mueve solo la lista (no la página) para que se vea la fila. center: la deja en el medio;
   * si no, la mueve lo justo y solo si no se ve. (scrollIntoView también movía toda la pantalla.)
   */
  function scrollListToRow(row, center) {
    if (!row) return;
    const list = trackListContainer;
    const top = row.getBoundingClientRect().top - list.getBoundingClientRect().top + list.scrollTop;
    const bottom = top + row.offsetHeight;
    if (center) {
      list.scrollTop = Math.max(0, top - (list.clientHeight - row.offsetHeight) / 2);
    } else if (top < list.scrollTop) {
      list.scrollTop = top;
    } else if (bottom > list.scrollTop + list.clientHeight) {
      list.scrollTop = bottom - list.clientHeight;
    }
  }

  /**
   * Lleva la lista a la canción que suena (si está en la lista): arriba queda una canción antes,
   * luego la que suena y luego las demás. Al volver a Pistas / Mi Lista o al tocar la carpeta actual.
   */
  function scrollListToPlaying() {
    const row = trackListContainer.querySelector('.list-item.is-playing');
    if (!row) return;
    let before = row.previousElementSibling;
    while (before && !before.classList.contains('list-item')) before = before.previousElementSibling;
    const list = trackListContainer;
    const target = before || row;
    const top = target.getBoundingClientRect().top - list.getBoundingClientRect().top + list.scrollTop;
    list.scrollTop = before ? Math.max(0, top - 4) : 0;
  }

  // Qué lista se está mostrando: si se vuelve a dibujar la misma, se conserva el desplazamiento
  let lastListViewKey = null;

  function renderList() {
    const viewKey = [activeTab, searchInput.value, currentFolder, currentFolderUsbSource, currentSearchFilter].join('|');
    const keepScroll = viewKey === lastListViewKey;
    const previousScroll = trackListContainer.scrollTop;
    lastListViewKey = viewKey;
    trackListContainer.innerHTML = '';
    updateFilterChipsUI();

    const rawQuery = searchInput.value;
    const normQuery = normalizeText(rawQuery);
    const tokens = normQuery.split(/\s+/).filter(t => t.length > 0);

    // Mostrar botones de Colapsar/Descolapsar únicamente en la pestaña de Carpetas USB sin búsqueda activa
    if (treeActions) {
      treeActions.classList.toggle('hidden', activeTab !== 'folders' || tokens.length > 0);
    }

    // BÚSQUEDA ACTIVA SEGÚN LA PESTAÑA SELECCIONADA
    if (tokens.length > 0) {
      if (activeTab === 'folders') {
        // PESTAÑA CARPETAS: Buscar ÚNICAMENTE en Carpetas
        let fPool = foldersList;
        if (currentSearchFilter === 'usb1') {
          fPool = fPool.filter(f => f.path.includes('udisk1') || f.path.includes('usb0') || f.badge === 'USB 1');
        } else if (currentSearchFilter === 'usb2') {
          fPool = fPool.filter(f => f.path.includes('udisk2') || f.path.includes('usb1') || f.badge === 'USB 2');
        }

        const matchedFolders = fPool.filter(f => {
          const text = normalizeText(f.name + " " + f.path);
          const dedup = dedupChars(text);
          return tokens.every(token => text.includes(token) || dedup.includes(dedupChars(token)));
        });

        matchedFolders.sort((a, b) => getFolderRelevance(b, rawQuery, normQuery, tokens) - getFolderRelevance(a, rawQuery, normQuery, tokens));

        const filterLabel = currentSearchFilter === 'usb1' ? ' [USB 1]' : (currentSearchFilter === 'usb2' ? ' [USB 2]' : '');
        crumbFolder.innerHTML = `📂 Carpetas${filterLabel}: "<strong>${escapeHtml(rawQuery)}</strong>" (${matchedFolders.length})`;

        if (matchedFolders.length === 0) {
          trackListContainer.innerHTML = `
            <div class="empty-search-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="empty-icon"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              <div class="empty-title">Sin carpetas para "<strong>${escapeHtml(rawQuery)}</strong>"</div>
              <div class="empty-desc">No se encontraron carpetas coincidentes en este origen.</div>
              <button class="btn-clear-search-action" id="btnClearSearchAction">Borrar búsqueda y ver todo</button>
            </div>
          `;
          const btnReset = document.getElementById('btnClearSearchAction');
          if (btnReset) {
            btnReset.addEventListener('click', () => {
              searchInput.value = '';
              clearSearch.classList.add('hidden');
              searchInput.blur();
              renderList();
            });
          }
          return;
        }

        matchedFolders.forEach(folder => {
          const folderEl = document.createElement('div');
          folderEl.className = 'list-item';
          const hasFolderMusic = (folder.count || 0) > 0;
          const playBtnHtml = hasFolderMusic ? `
            <button class="btn-folder-addmix" title="Agregar esta carpeta a Mi Lista">${MIX_PLUS_SVG}</button>
            <button class="btn-folder-play" title="Reproducir inmediatamente esta carpeta">
              <span class="track-count-num">${folder.count}</span>
              <div class="play-circle">
                <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
              </div>
            </button>
          ` : '';

          folderEl.innerHTML = `
            <div class="item-left">
              <div class="item-icon-wrap">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
              </div>
              <div class="item-info">
                <span class="item-title">${folder.name}</span>
                <div class="item-subtitle">
                  ${getStorageBadgeHtml(folder.path)}
                  <span class="path-text">${formatDisplayPath(folder.path)}</span>
                </div>
              </div>
            </div>
            ${playBtnHtml}
          `;

          const addMixBtn = folderEl.querySelector('.btn-folder-addmix');
          if (addMixBtn) {
            addMixBtn.addEventListener('click', (e) => {
              e.stopPropagation();
              addFolderToMix(tracksOfFolderNode(folder), folder.name);
            });
          }

          const playBtn = folderEl.querySelector('.btn-folder-play');
          if (playBtn) {
            playBtn.addEventListener('click', (e) => {
              e.stopPropagation();
              playFolderTree(folder);
            });
          }

          folderEl.addEventListener('click', () => {
            searchInput.blur();
            currentFolder = folder.name;
            currentFolderUsbSource = folder.usbSource || folder.badge || (folder.path && folder.path.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1');
            crumbFolder.textContent = `${currentFolderUsbSource ? '[' + currentFolderUsbSource + '] ' : ''}${folder.name}`;
            currentSearchFilter = 'folder';
            
            const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
            const folderTracks = allPool.filter(t => t.folder === folder.name);
            setPistasTracks(folderTracks.length > 0 ? folderTracks : [...allPool]);
            tabButtons[1].click();
          });

          trackListContainer.appendChild(folderEl);
        });
        return;
      }

      // PESTAÑAS PISTAS O FAVORITOS: Buscar en pistas
      const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
      let searchPool = allPool;

      if (activeTab === 'mymix') {
        searchPool = mixTracks();
      } else {
        if (currentSearchFilter === 'folder' && currentFolder) {
          searchPool = allPool.filter(t => t.folder === currentFolder && isSameUsb(t, currentFolderUsbSource));
        }
      }

      if (currentSearchFilter === 'usb1') {
        searchPool = searchPool.filter(t => t.usbSource === 'USB 1' || (t.path && (t.path.includes('udisk1') || t.path.includes('usb0'))));
      } else if (currentSearchFilter === 'usb2') {
        searchPool = searchPool.filter(t => t.usbSource === 'USB 2' || (t.path && (t.path.includes('udisk2') || t.path.includes('usb1'))));
      }

      const filterLabel = (activeTab === 'mymix') ? ' [Mi Lista]' :
          (currentSearchFilter === 'folder' ? ` [${currentFolder}]` : (currentSearchFilter === 'usb1' ? ' [USB 1]' : (currentSearchFilter === 'usb2' ? ' [USB 2]' : ' [TODO]')));
      crumbFolder.innerHTML = `🔍 Búsqueda${filterLabel}: "<strong>${escapeHtml(rawQuery)}</strong>"`;
      
      const searchResults = searchPool.filter(t => trackMatchesTokens(t, tokens));
      searchResults.sort((a, b) => getTrackRelevance(b, rawQuery, normQuery, tokens) - getTrackRelevance(a, rawQuery, normQuery, tokens));

      if (activeTab !== 'mymix') songCountEl.textContent = searchResults.length;

      if (searchResults.length === 0) {
        trackListContainer.innerHTML = `
          <div class="empty-search-box">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="empty-icon"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <div class="empty-title">Sin resultados en ${filterLabel} para "<strong>${escapeHtml(rawQuery)}</strong>"</div>
            <div class="empty-desc">No se encontraron videos en este origen. Cambia el filtro o prueba otro término.</div>
            <button class="btn-clear-search-action" id="btnClearSearchAction">Borrar término de búsqueda</button>
          </div>
        `;
        const btnReset = document.getElementById('btnClearSearchAction');
        if (btnReset) {
          btnReset.addEventListener('click', () => {
            searchInput.value = '';
            clearSearch.classList.add('hidden');
            searchInput.blur();
            renderList();
          });
        }
        return;
      }

      searchResults.forEach((track) => {
        const isCurrent = (currentPlaylist[currentTrackIndex]?.id === track.id);
        const itemEl = document.createElement('div');
        itemEl.className = `list-item ${isCurrent ? 'is-playing' : ''}`;
        
        itemEl.innerHTML = `
          <div class="item-left">
            <div class="item-icon-wrap">
              ${isCurrent && isPlaying ? `
                <div class="sound-wave-bars" style="height:14px; gap:2px;">
                  <span style="height:12px;background:#0c0e12"></span>
                  <span style="height:8px;background:#0c0e12"></span>
                  <span style="height:14px;background:#0c0e12"></span>
                </div>
              ` : `
                <svg viewBox="0 0 24 24" fill="currentColor" style="width:16px;height:16px;"><path d="M8 5v14l11-7z"/></svg>
              `}
            </div>
            <div class="item-info">
              <div class="item-title-wrap">
                <span class="item-title">${track.title}</span>
              </div>
              <div class="item-subtitle">
                ${getStorageBadgeHtml(track.path)}
                <span class="path-text">${track.artist} • ${track.album || track.folder}</span>
              </div>
            </div>
          </div>
          <div class="item-right">
            <span class="item-duration">${track.duration}</span>
            ${mixToggleButtonHtml(track)}
          </div>
        `;

        // 1. Click en el botón de play al principio: SOLO AQUÍ SE REPRODUCE
        const playBtn = itemEl.querySelector('.item-icon-wrap');
        playBtn.setAttribute('title', 'Reproducir');
        playBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();

          // Búsqueda dentro de Mi Lista: suena Mi Lista desde esa canción
          if (activeTab === 'mymix') {
            playMix(mixTracks().findIndex(t => t.id === track.id), false);
            return;
          }

          // 1. Setear la carpeta actual a la carpeta de la canción seleccionada
          currentFolder = track.folder;
          currentFolderUsbSource = track.usbSource || (track.path && track.path.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1');

          // 2. Cargar en currentPlaylist todas las canciones de esa carpeta
          const folderTracks = allPool.filter(t => t.folder === track.folder && (track.usbSource ? t.usbSource === track.usbSource : true));
          playingFromMix = false; // deja Mi Lista: ahora suena la carpeta de la canción
          setPistasTracks(folderTracks.length > 0 ? folderTracks : [...allPool]);

          // 3. Índice de la pista en esa carpeta
          const realFolderIdx = currentPlaylist.findIndex(t => t.id === track.id);

          // 4. Actualizar el chip de carpeta para que muestre el nuevo nombre con marquesina si es largo
          if (chipFilterFolder) {
            updateFolderChipContent();
          }

          // 5. Reproducir la canción (actualiza reproductor izquierdo, metadata y previews)
          loadTrack(realFolderIdx >= 0 ? realFolderIdx : 0, true);

          // 6. La lista de búsqueda NO se oculta, sigue mostrando los resultados
          // Marcamos visualmente esta canción como la que está sonando
          trackListContainer.querySelectorAll('.list-item').forEach(el => el.classList.remove('is-playing'));
          itemEl.classList.add('is-playing');
          const iconWrap = itemEl.querySelector('.item-icon-wrap');
          if (iconWrap) {
            iconWrap.innerHTML = `
              <div class="sound-wave-bars" style="height:14px; gap:2px;">
                <span style="height:12px;background:#0c0e12"></span>
                <span style="height:8px;background:#0c0e12"></span>
                <span style="height:14px;background:#0c0e12"></span>
              </div>
            `;
          }
        });

        // 2. Click en el texto:
        // 1 toque: Mostrar/ocultar nombre completo (espera 280ms para confirmar)
        // 2 toques: Cancela la apertura y reproduce la canción directamente
        const infoEl = itemEl.querySelector('.item-info');
        infoEl.setAttribute('title', '1 toque: ver nombre completo • 2 toques: reproducir');
        let clickCount = 0;
        let clickTimer = null;
        infoEl.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();
          clickCount++;

          if (clickCount === 1) {
            clickTimer = setTimeout(() => {
              // 1 toque confirmado: EXPANDIR / COLAPSAR
              clickCount = 0;
              clickTimer = null;
              const wasExpanded = itemEl.classList.contains('is-expanded');
              trackListContainer.querySelectorAll('.list-item.is-expanded').forEach(el => {
                el.classList.remove('is-expanded');
              });
              if (!wasExpanded) {
                itemEl.classList.add('is-expanded');
              }
            }, 280);
          } else if (clickCount >= 2) {
            // 2 toques: CANCELAR la apertura y REPRODUCIR DIRECTO
            if (clickTimer) {
              clearTimeout(clickTimer);
              clickTimer = null;
            }
            clickCount = 0;
            playBtn.click();
          }
        });

        // Botón + / ✓ de Mi Lista
        bindMixToggleButton(itemEl, track);

        trackListContainer.appendChild(itemEl);
      });
      return;
    }

    // MODO NORMAL (SIN BÚSQUEDA) - ÁRBOL JERÁRQUICO DE CARPETAS USB
    if (activeTab === 'folders') {
      let filteredRoots = currentTreeData;
      if (currentSearchFilter === 'usb1') {
        filteredRoots = currentTreeData.filter(n => n.usbSource === 'USB 1' || n.id === 'usb1');
        crumbFolder.textContent = "Carpetas USB 1 (Árbol)";
      } else if (currentSearchFilter === 'usb2') {
        filteredRoots = currentTreeData.filter(n => n.usbSource === 'USB 2' || n.id === 'usb2');
        crumbFolder.textContent = "Carpetas USB 2 (Árbol)";
      } else {
        crumbFolder.textContent = "Carpetas USB (Árbol de Pendrives)";
      }

      function renderTreeNode(node, level = 0) {
        const hasChildren = node.children && node.children.length > 0;
        const isExpanded = expandedTreeNodes.has(node.id);
        const currentTrack = loadedTrack || currentPlaylist[currentTrackIndex];
        const isPlayingFolder = currentTrack && (
          (node.name === currentTrack.folder) ||
          (currentTrack.path && node.path && !node.isUsbRoot && (currentTrack.path.replace(/\/+$/, '') === node.path.replace(/\/+$/, '')))
        );
        const isActive = isPlayingFolder || (currentFolder === node.name);
        const indentPx = 4 + (level * 14); // poca sangría: el nombre necesita el ancho en 1024x600

        // Si la carpeta tiene 0 músicas directas (ej: es solo contenedor de subcarpetas o raíz),
        // se lista normalmente pero NO debe salir el botón de play.
        // Solo las carpetas con canciones directas (> 0) muestran el botón de play con su número.
        const directTracksCount = (typeof node.directCount === 'number')
          ? node.directCount
          : (hasChildren ? 0 : (node.count || 0));
        const hasDirectMusic = directTracksCount > 0;

        const nodeEl = document.createElement('div');
        // La que suena lleva un ecualizador en su botón ▶; la abierta (si no suena), borde fino
        nodeEl.className = `tree-node-item ${isPlayingFolder ? 'is-playing-folder' : (isActive ? 'is-active-folder' : '')} ${fullTitleNodeId === node.id ? 'show-full-title' : ''}`;
        nodeEl.style.paddingLeft = `${indentPx}px`;
        nodeEl.dataset.nodeId = node.id;

        const isUsbRoot = !!node.isUsbRoot;

        let playBtnHtml = '';
        if (hasDirectMusic) {
          playBtnHtml = `
            <button class="btn-folder-addmix" title="Agregar ${escapeHtml(node.name)} a Mi Lista">${MIX_PLUS_SVG}</button>
            <button class="btn-folder-play is-square" title="Reproducir ${escapeHtml(node.name)}">
              ${isPlayingFolder
                ? '<span class="folder-eq"><i></i><i></i><i></i></span>'
                : '<div class="play-circle"><svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg></div>'}
            </button>
          `;
        }

        // Nombre en 1 renglón (el CSS lo corta con "…"; tocado, completo en 2 renglones). Sin la línea de
        // cantidad: si tiene canciones lo dicen + y ▶; si solo tiene subcarpetas, la flecha
        const rawFolderName = node.name || '';
        const folderGlyph = `<svg class="tree-folder-glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>`;

        nodeEl.innerHTML = `
          <div class="tree-node-left">
            <div class="tree-toggle-arrow ${isExpanded ? 'is-expanded' : ''} ${!hasChildren ? 'no-children' : ''}">
              ${hasChildren ? `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" style="width:16px;height:16px;">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              ` : `
                <span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:rgba(148,163,184,0.35);"></span>
              `}
            </div>
            ${isUsbRoot ? `
            <div class="tree-node-icon is-usb-root">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:20px;height:20px;">
                  <rect x="6" y="2" width="12" height="20" rx="2"></rect>
                  <line x1="9" y1="6" x2="9" y2="8"></line>
                  <line x1="15" y1="6" x2="15" y2="8"></line>
                  <line x1="9" y1="18" x2="15" y2="18"></line>
                </svg>
            </div>` : ''}
            ${isUsbRoot ? '' : `<span class="tree-fixed-glyph">${folderGlyph}</span>`}
            <div class="tree-node-info" title="${escapeHtml(rawFolderName)}">
              <span class="tree-node-title">${escapeHtml(rawFolderName)}</span>
            </div>
          </div>
          ${playBtnHtml}
        `;

        // + : agregar la carpeta entera a Mi Lista
        const addMixBtn = nodeEl.querySelector('.btn-folder-addmix');
        if (addMixBtn) {
          addMixBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            addFolderToMix(tracksOfFolderNode(node), node.name);
          });
        }

        // Click en botón Play [ N ▶ ] (Solo si la carpeta tiene canciones directas)
        const playBtn = nodeEl.querySelector('.btn-folder-play');
        if (playBtn) {
          playBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            playFolderTree(node);
          });
        }

        // Click en la fila: expandir / contraer y preparar pistas
        nodeEl.addEventListener('click', () => {
          searchInput.blur();
          // Dos toques seguidos = lo mismo que [N ▶] (el segundo toque deja la carpeta abierta o cerrada como estaba)
          const now = Date.now();
          const doubleTap = lastTreeTap.id === node.id && now - lastTreeTap.at <= FOLDER_DOUBLE_TAP_MS;
          lastTreeTap = doubleTap ? { id: null, at: 0 } : { id: node.id, at: now };
          fullTitleNodeId = node.id;
          if (hasChildren) {
            if (expandedTreeNodes.has(node.id)) {
              expandedTreeNodes.delete(node.id);
            } else {
              expandedTreeNodes.add(node.id);
            }
          }
          currentFolder = node.name;
          currentFolderUsbSource = node.usbSource || (node.path && node.path.includes('udisk2') ? 'USB 2' : 'USB 1');
          const usbTag = currentFolderUsbSource ? `[${currentFolderUsbSource}] ` : '';
          crumbFolder.textContent = `${usbTag}${node.name}`;
          
          // Actualizar playlist para esta carpeta
          const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
          const folderTracks = allPool.filter(t => {
            if (!isSameUsb(t, currentFolderUsbSource)) return false;
            if (t.folder === node.name) return true;
            if (t.path && node.path && t.path.startsWith(node.path)) return true;
            return false;
          });
          if (folderTracks.length > 0) {
            sortTracksAlphabetically(folderTracks);
            setPistasTracks(folderTracks);
            songCountEl.textContent = pistasTracks.length;
          }

          renderList();
          // La fila se enciende un instante: se nota que el toque entró
          const tappedEl = trackListContainer.querySelector(`[data-node-id="${CSS.escape(String(node.id))}"]`);
          if (tappedEl) {
            tappedEl.classList.add('is-tapped');
            setTimeout(() => tappedEl.classList.remove('is-tapped'), 250);
          }
          if (doubleTap && hasDirectMusic) playFolderTree(node);
        });

        trackListContainer.appendChild(nodeEl);

        // Si el nodo está desplegado y tiene subcarpetas, renderizarlas con su nivel de sangría
        if (hasChildren && isExpanded) {
          node.children.forEach(child => {
            renderTreeNode(child, level + 1);
          });
        }
      }

      filteredRoots.forEach(rootNode => {
        renderTreeNode(rootNode, 0);
      });

      // Auto-scroll a la carpeta que está sonando actualmente
      setTimeout(() => {
        const playingFolderEl = trackListContainer.querySelector('.tree-node-item.is-playing-folder');
        if (playingFolderEl) {
          scrollListToRow(playingFolderEl, false);
        }
      }, 70);
    } 
    else if (activeTab === 'mymix') {
      renderMixTab();
    }
    else if (activeTab === 'playlist') {
      let filtered = pistasTracks;
      const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;

      {
        if (currentSearchFilter === 'usb1') {
          filtered = allPool.filter(t => t.usbSource === 'USB 1' || (t.path && (t.path.includes('udisk1') || t.path.includes('usb0'))));
          crumbFolder.textContent = `Videos USB 1 (${filtered.length})`;
        } else if (currentSearchFilter === 'usb2') {
          filtered = allPool.filter(t => t.usbSource === 'USB 2' || (t.path && (t.path.includes('udisk2') || t.path.includes('usb1'))));
          crumbFolder.textContent = `Videos USB 2 (${filtered.length})`;
        } else if (currentSearchFilter === 'all') {
          filtered = [...allPool];
          crumbFolder.textContent = `Todas las pistas (${filtered.length})`;
        } else {
          filtered = pistasTracks;
          const usbTag = currentFolderUsbSource ? `[${currentFolderUsbSource}] ` : '';
          crumbFolder.textContent = `${usbTag}${currentFolder} (${filtered.length})`;
        }
        sortTracksAlphabetically(filtered);
      }

      songCountEl.textContent = filtered.length;

      filtered.forEach((track, idx) => {
        const isCurrent = (currentPlaylist[currentTrackIndex]?.id === track.id);
        const itemEl = document.createElement('div');
        itemEl.className = `list-item ${isCurrent ? 'is-playing' : ''}`;
        
        itemEl.innerHTML = `
          <div class="item-left">
            <div class="item-icon-wrap">
              ${isCurrent && isPlaying ? `
                <div class="sound-wave-bars" style="height:14px; gap:2px;">
                  <span style="height:12px;background:#0c0e12"></span>
                  <span style="height:8px;background:#0c0e12"></span>
                  <span style="height:14px;background:#0c0e12"></span>
                </div>
              ` : `
                <svg viewBox="0 0 24 24" fill="currentColor" style="width:16px;height:16px;"><path d="M8 5v14l11-7z"/></svg>
              `}
            </div>
            <div class="item-info">
              <div class="item-title-wrap">
                <span class="item-title">${track.title}</span>
              </div>
              <div class="item-subtitle">
                ${getStorageBadgeHtml(track.path)}
                <span class="path-text">${track.artist} • ${track.album || track.folder}</span>
              </div>
            </div>
          </div>
          <div class="item-right">
            <span class="item-duration">${track.duration}</span>
            ${mixToggleButtonHtml(track)}
          </div>
        `;

        // 1. Click en el botón de play al principio: SOLO AQUÍ SE REPRODUCE
        const playBtn = itemEl.querySelector('.item-icon-wrap');
        playBtn.setAttribute('title', 'Reproducir');
        playBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();
          if (playingFromMix) {
            // Tocar una canción de Pistas deja Mi Lista: ahora suena esta lista
            playingFromMix = false;
            currentPlaylist = filtered;
          }
          const realIdx = currentPlaylist.findIndex(t => t.id === track.id);
          loadTrack(realIdx >= 0 ? realIdx : idx, true);
        });

        // 2. Click en el texto:
        // 1 toque: Mostrar/ocultar nombre completo
        // 2. Click en el texto:
        // 1 toque: Mostrar/ocultar nombre completo (espera 280ms para confirmar)
        // 2 toques: Cancela la apertura y reproduce la canción directamente
        const infoEl = itemEl.querySelector('.item-info');
        infoEl.setAttribute('title', '1 toque: ver nombre completo • 2 toques: reproducir');
        let clickCount = 0;
        let clickTimer = null;
        infoEl.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();
          clickCount++;

          if (clickCount === 1) {
            clickTimer = setTimeout(() => {
              // 1 toque confirmado: EXPANDIR / COLAPSAR
              clickCount = 0;
              clickTimer = null;
              const wasExpanded = itemEl.classList.contains('is-expanded');
              trackListContainer.querySelectorAll('.list-item.is-expanded').forEach(el => {
                el.classList.remove('is-expanded');
              });
              if (!wasExpanded) {
                itemEl.classList.add('is-expanded');
              }
            }, 280);
          } else if (clickCount >= 2) {
            // 2 toques: CANCELAR la apertura y REPRODUCIR DIRECTO
            if (clickTimer) {
              clearTimeout(clickTimer);
              clickTimer = null;
            }
            clickCount = 0;
            playBtn.click();
          }
        });

        // Botón + / ✓ de Mi Lista
        bindMixToggleButton(itemEl, track);

        trackListContainer.appendChild(itemEl);
      });
    }

    // Misma lista: queda donde estaba (antes volvía arriba al empezar una canción)
    if (keepScroll) trackListContainer.scrollTop = previousScroll;

    // Activar marquesina en la canción que está sonando en la lista
    setTimeout(refreshAllMarquees, 70);
    setTimeout(updateWebFastScroller, 80);
  }

  // =================== PESTAÑA MI LISTA ===================
  /** Minutos totales para el resumen ("12 canciones · 48 min"). */
  function formatMixLength(tracks) {
    const totalSec = tracks.reduce((sum, t) => sum + (t.durationSec || 0), 0);
    const h = Math.floor(totalSec / 3600);
    const m = Math.round((totalSec % 3600) / 60);
    return h > 0 ? `${h} h ${m} min` : `${m} min`;
  }

  function renderMixTab() {
    const list = mixTracks();
    updateMixTabCount();
    crumbFolder.textContent = `Mi Lista (${list.length})`;

    // Cabecera: resumen + Reproducir / Reordenar / Vaciar
    const header = document.createElement('div');
    header.className = 'mix-header';
    const playingNowId = playingFromMix ? currentPlaylist[currentTrackIndex]?.id : null;
    const playedCount = list.filter(t => mixPlayedIds.has(t.id) && t.id !== playingNowId).length;
    if (mixConfirmClear) {
      header.innerHTML = `
        <div class="mix-header-info">
          <span class="mix-header-title">¿Vaciar Mi Lista?</span>
          <span class="mix-header-sub">Se quitan las ${list.length}. El que se está viendo sigue.</span>
        </div>
        <div class="mix-header-actions">
          <button class="btn-mix-action is-danger" id="btnMixConfirmClear">Sí, vaciar</button>
          <button class="btn-mix-action" id="btnMixCancelClear">No</button>
        </div>
      `;
    } else if (isReorderingMix) {
      header.innerHTML = `
        <div class="mix-header-info">
          <span class="mix-header-title">REORDENAR MI LISTA</span>
          <span class="mix-header-sub">Arrastra la manija ≡ para cambiar el orden</span>
        </div>
        <div class="mix-header-actions">
          <button class="btn-mix-action is-primary btn-mix-reorder-done" id="btnMixReorderDone" title="Terminar de ordenar">✓ Listo</button>
        </div>
      `;
    } else {
      const summary = list.length === 0
        ? 'Vacío'
        : `${list.length} ${list.length === 1 ? 'video' : 'videos'} · ${formatMixLength(list)}${playedCount > 0 ? ` · ${playedCount} vistos` : ''}`;
      header.innerHTML = `
        <div class="mix-header-info">
          <span class="mix-header-title">MI LISTA${playingFromMix ? ' <span class="mix-live-badge">● REPRODUCIENDO</span>' : ''}</span>
          <span class="mix-header-sub">${summary}</span>
        </div>
        <div class="mix-header-actions">
          <!-- Solo el ▶ (sin texto): deja espacio para el resumen -->
          <button class="btn-mix-action is-primary btn-mix-play-icon" id="btnMixPlay" title="Reproducir Mi Lista" ${list.length === 0 ? 'disabled' : ''}>
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.6v12.8a1 1 0 0 0 1.53.85l10.2-6.4a1 1 0 0 0 0-1.7L9.53 4.75A1 1 0 0 0 8 5.6z"/></svg>
          </button>
          <button class="btn-mix-action btn-mix-reorder" id="btnMixReorder" title="Reordenar videos de Mi Lista" ${list.length <= 1 ? 'disabled' : ''}>
            ${MIX_HEADER_REORDER_SVG}
          </button>
          <button class="btn-mix-action" id="btnMixClear" title="Vaciar Mi Lista" ${list.length === 0 ? 'disabled' : ''}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
          </button>
        </div>
      `;
    }
    trackListContainer.appendChild(header);

    const btnPlay = header.querySelector('#btnMixPlay');
    if (btnPlay) btnPlay.addEventListener('click', () => playMix(0, true));
    const btnReorder = header.querySelector('#btnMixReorder');
    if (btnReorder) btnReorder.addEventListener('click', () => { isReorderingMix = true; renderList(); });
    const btnDone = header.querySelector('#btnMixReorderDone');
    if (btnDone) btnDone.addEventListener('click', () => { isReorderingMix = false; showMixToast("Nuevo orden guardado"); renderList(); });
    const btnClear = header.querySelector('#btnMixClear');
    if (btnClear) btnClear.addEventListener('click', () => { mixConfirmClear = true; renderList(); });
    const btnConfirm = header.querySelector('#btnMixConfirmClear');
    if (btnConfirm) btnConfirm.addEventListener('click', () => {
      mixConfirmClear = false;
      clearMix();
      showMixToast("Mi Lista vacía");
      renderList();
    });
    const btnCancel = header.querySelector('#btnMixCancelClear');
    if (btnCancel) btnCancel.addEventListener('click', () => { mixConfirmClear = false; renderList(); });

    // La que suena ya no está en la lista (se quitó o se vació Mi Lista): sigue sonando y se muestra
    // arriba, para que se entienda qué suena y que lo nuevo entra después de ella
    const orphan = playingFromMix ? currentPlaylist[currentTrackIndex] : null;
    if (orphan && !isInMix(orphan)) {
      const orphanEl = document.createElement('div');
      orphanEl.className = 'list-item mix-item is-playing mix-orphan';
      orphanEl.innerHTML = `
        <div class="item-left">
          <div class="item-icon-wrap">
            ${isPlaying ? `
              <div class="sound-wave-bars" style="height:14px; gap:2px;">
                <span style="height:12px;background:#0c0e12"></span>
                <span style="height:8px;background:#0c0e12"></span>
                <span style="height:14px;background:#0c0e12"></span>
              </div>` : `<svg viewBox="0 0 24 24" fill="currentColor" style="width:16px;height:16px;"><path d="M8 5v14l11-7z"/></svg>`}
          </div>
          <div class="item-info">
            <div class="item-title-wrap">
              <span class="item-title">${orphan.title}</span>
            </div>
            <div class="item-subtitle">
              <span class="path-text">Sonando · ya no está en Mi Lista</span>
            </div>
          </div>
        </div>
        <div class="item-right">
          <span class="item-duration">${orphan.duration}</span>
          ${mixToggleButtonHtml(orphan)}
        </div>
      `;
      // + : volver a agregarla en su lugar (no al final, para no saltarse las que venían después)
      orphanEl.querySelector('.btn-item-mix').addEventListener('click', (e) => {
        e.stopPropagation();
        myMixIds.splice(Math.min(mixCurPos, myMixIds.length), 0, orphan.id);
        saveMix();
        updateMixTabCount();
        syncMixQueue();
        showMixToast("Vuelve a Mi Lista");
        renderList();
      });
      trackListContainer.appendChild(orphanEl);
    }

    if (list.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'empty-search-box';
      empty.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="empty-icon"><line x1="3" y1="6" x2="14" y2="6"/><line x1="3" y1="12" x2="14" y2="12"/><line x1="3" y1="18" x2="10" y2="18"/><line x1="18" y1="13" x2="18" y2="21"/><line x1="14" y1="17" x2="22" y2="17"/></svg>
        <div class="empty-title">Tu Mi Lista está vacío</div>
        <div class="empty-desc">${orphan && !isInMix(orphan)
          ? 'Lo que agregues con <strong>+</strong> se verá después del video actual. Si no agregas nada, se detiene al terminar.'
          : 'Toca <strong>+</strong> en un video, o en una carpeta para agregarla entera. Después reprodúcela y se ve todo seguido.'}</div>
      `;
      trackListContainer.appendChild(empty);
      return;
    }

    const playingId = currentPlaylist[currentTrackIndex]?.id;
    list.forEach((track, position) => {
      const isCurrent = playingFromMix && playingId === track.id;
      const wasPlayed = !isCurrent && mixPlayedIds.has(track.id);
      const itemEl = document.createElement('div');
      itemEl.className = `list-item mix-item ${isCurrent ? 'is-playing' : ''} ${wasPlayed ? 'is-played' : ''} ${track.id === justLandedMixId ? 'pulse-landed' : ''}`;

      let iconHtml;
      if (isCurrent && isPlaying) {
        iconHtml = `
          <div class="sound-wave-bars" style="height:14px; gap:2px;">
            <span style="height:12px;background:#0c0e12"></span>
            <span style="height:8px;background:#0c0e12"></span>
            <span style="height:14px;background:#0c0e12"></span>
          </div>`;
      } else if (wasPlayed) {
        iconHtml = MIX_CHECK_SVG; // ya sonó
      } else {
        iconHtml = `<span class="mix-position">${String(position + 1).padStart(2, '0')}</span>`;
      }

      itemEl.innerHTML = `
        <div class="item-left">
          <div class="item-icon-wrap">${iconHtml}</div>
          <div class="item-info">
            <div class="item-title-wrap">
              <span class="item-title">${track.title}</span>
            </div>
            <div class="item-subtitle">
              ${getStorageBadgeHtml(track.path)}
              <span class="path-text">${wasPlayed ? 'Ya visto • ' : ''}${track.artist} • ${track.album || track.folder}</span>
            </div>
          </div>
        </div>
        <div class="item-right">
          <span class="item-duration">${track.duration}</span>
          ${isReorderingMix
            ? `<button class="btn-item-mix is-drag-handle" title="Arrastrar para mover">${MIX_REORDER_SVG}</button>`
            : `<button class="btn-item-mix is-remove" title="Quitar de Mi Lista">${MIX_REMOVE_SVG}</button>`}
        </div>
      `;

      if (isReorderingMix) {
        itemEl.classList.add('is-reordering');
        const handle = itemEl.querySelector('.is-drag-handle');

        let isDragging = false;
        let floatingClone = null;
        let placeholder = null;
        let startPointerY = 0;
        let pointerId = null;
        let currentClientY = 0;
        let autoScrollRaf = null;

        const updatePlaceholderPosition = () => {
          if (!isDragging || !floatingClone || !placeholder) return;
          const floatingRect = floatingClone.getBoundingClientRect();
          const floatingMiddleY = floatingRect.top + floatingRect.height / 2;

          const siblings = Array.from(trackListContainer.querySelectorAll('.mix-item:not(.mix-orphan)')).filter(el => el !== placeholder && el.style.display !== 'none');

          let targetSibling = null;
          let insertBefore = false;

          for (let sib of siblings) {
            const rect = sib.getBoundingClientRect();
            if (floatingMiddleY >= rect.top && floatingMiddleY <= rect.bottom) {
              targetSibling = sib;
              insertBefore = floatingMiddleY < (rect.top + rect.height / 2);
              break;
            }
          }

          if (targetSibling) {
            const currentPlaceholderPos = Array.from(trackListContainer.children).indexOf(placeholder);
            const newTargetPos = Array.from(trackListContainer.children).indexOf(targetSibling);

            if (currentPlaceholderPos !== newTargetPos) {
              animateMixFLIP(() => {
                if (insertBefore) {
                  trackListContainer.insertBefore(placeholder, targetSibling);
                } else {
                  trackListContainer.insertBefore(placeholder, targetSibling.nextSibling);
                }
                const allMixItems = Array.from(trackListContainer.querySelectorAll('.mix-item:not(.mix-orphan), .gap-placeholder'));
                const newIdx = allMixItems.indexOf(placeholder) + 1;
                placeholder.textContent = `[ Espacio disponible · Puesto ${newIdx} ]`;
              });
            }
          }
        };

        const autoScrollLoop = () => {
          if (!isDragging) return;
          const containerRect = trackListContainer.getBoundingClientRect();
          const topThreshold = containerRect.top + 70;
          const bottomThreshold = containerRect.bottom - 70;

          let scrollSpeed = 0;
          if (currentClientY < topThreshold) {
            const factor = Math.min(1, Math.max(0, (topThreshold - currentClientY) / 70));
            scrollSpeed = -Math.round(4 + factor * 16);
          } else if (currentClientY > bottomThreshold) {
            const factor = Math.min(1, Math.max(0, (currentClientY - bottomThreshold) / 70));
            scrollSpeed = Math.round(4 + factor * 16);
          }

          if (scrollSpeed !== 0) {
            const oldScroll = trackListContainer.scrollTop;
            trackListContainer.scrollTop += scrollSpeed;
            if (trackListContainer.scrollTop !== oldScroll) {
              updatePlaceholderPosition();
            }
          }

          autoScrollRaf = requestAnimationFrame(autoScrollLoop);
        };

        const onPointerDown = (e) => {
          if (e.button !== 0 && e.pointerType === 'mouse') return;
          e.preventDefault();
          pointerId = e.pointerId;
          handle.setPointerCapture(pointerId);
          isDragging = true;
          startPointerY = e.clientY;
          currentClientY = e.clientY;

          const startRect = itemEl.getBoundingClientRect();

          // 1. Tarjeta Flotante Sólida (Opaca, elevada con sombra 3D)
          floatingClone = itemEl.cloneNode(true);
          floatingClone.className = 'list-item mix-item floating-drag-clone';
          floatingClone.style.width = `${startRect.width}px`;
          floatingClone.style.top = `${startRect.top}px`;
          floatingClone.style.left = `${startRect.left}px`;
          document.body.appendChild(floatingClone);

          // 2. Hueco Real en el DOM
          placeholder = document.createElement('div');
          placeholder.className = 'gap-placeholder';
          placeholder.textContent = `[ Espacio disponible · Puesto ${position + 1} ]`;

          itemEl.style.display = 'none';
          trackListContainer.insertBefore(placeholder, itemEl);

          // Iniciar bucle de auto-scroll
          autoScrollRaf = requestAnimationFrame(autoScrollLoop);
        };

        const onPointerMove = (e) => {
          if (!isDragging || !floatingClone) return;
          currentClientY = e.clientY;
          const deltaY = e.clientY - startPointerY;
          floatingClone.style.transform = `translateY(${deltaY}px) scale(1.02)`;

          updatePlaceholderPosition();
        };

        const onPointerUp = (e) => {
          if (!isDragging) return;
          isDragging = false;
          if (autoScrollRaf) {
            cancelAnimationFrame(autoScrollRaf);
            autoScrollRaf = null;
          }
          try { handle.releasePointerCapture(pointerId); } catch (_) {}

          const placeholderRect = placeholder.getBoundingClientRect();
          const cloneRect = floatingClone.getBoundingClientRect();
          const glideDeltaY = placeholderRect.top - cloneRect.top;

          floatingClone.style.transition = 'transform 0.18s cubic-bezier(0.2, 0.8, 0.2, 1)';
          floatingClone.style.transform += ` translateY(${glideDeltaY}px) scale(1)`;

          setTimeout(() => {
            if (floatingClone) floatingClone.remove();

            const allMixItems = Array.from(trackListContainer.querySelectorAll('.mix-item:not(.mix-orphan), .gap-placeholder'));
            const newIndex = allMixItems.indexOf(placeholder);
            if (placeholder) placeholder.remove();

            if (newIndex >= 0 && newIndex !== position) {
              const [movedId] = myMixIds.splice(position, 1);
              myMixIds.splice(newIndex, 0, movedId);
              justLandedMixId = movedId;
              saveMix();
              syncMixQueue();
              renderList();
              showMixToast(`✓ ${track.title.slice(0, 15)}... al puesto ${newIndex + 1}`);
              setTimeout(() => { justLandedMixId = null; }, 900);
            } else {
              renderList();
            }
          }, 180);
        };

        handle.addEventListener('pointerdown', onPointerDown);
        handle.addEventListener('pointermove', onPointerMove);
        handle.addEventListener('pointerup', onPointerUp);
        handle.addEventListener('pointercancel', onPointerUp);

      } else {
        // Número / ícono: reproducir Mi Lista desde esta canción
        const playBtn = itemEl.querySelector('.item-icon-wrap');
        playBtn.setAttribute('title', 'Reproducir Mi Lista desde aquí');
        playBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();
          playMix(position, false);
        });

        // Texto: 1 toque ver nombre completo, 2 toques reproducir (igual que en Pistas)
        const infoEl = itemEl.querySelector('.item-info');
        let clickCount = 0;
        let clickTimer = null;
        infoEl.addEventListener('click', (e) => {
          e.stopPropagation();
          searchInput.blur();
          clickCount++;
          if (clickCount === 1) {
            clickTimer = setTimeout(() => {
              clickCount = 0;
              clickTimer = null;
              const wasExpanded = itemEl.classList.contains('is-expanded');
              trackListContainer.querySelectorAll('.list-item.is-expanded').forEach(el => el.classList.remove('is-expanded'));
              if (!wasExpanded) itemEl.classList.add('is-expanded');
            }, 280);
          } else {
            if (clickTimer) clearTimeout(clickTimer);
            clickTimer = null;
            clickCount = 0;
            playBtn.click();
          }
        });

        // ✕: quitar de Mi Lista
        itemEl.querySelector('.btn-item-mix').addEventListener('click', (e) => {
          e.stopPropagation();
          removeFromMix(track);
          showMixToast("Quitada de Mi Lista");
          renderList();
        });
      }

      trackListContainer.appendChild(itemEl);
    });

    // Que se vea la que está sonando
    setTimeout(() => {
      const playingEl = trackListContainer.querySelector('.mix-item.is-playing');
      scrollListToRow(playingEl, false);
    }, 70);
  }

  // =================== FASTSCROLLER TÁCTIL ERGONÓMICO Y BURBUJA A-Z ===================
  const webFastScroller = document.getElementById('webFastScroller');
  const webFastThumb = document.getElementById('webFastThumb');
  const webFastBubble = document.getElementById('webFastBubble');
  const webBubbleLetter = document.getElementById('webBubbleLetter');
  const webBubbleCount = document.getElementById('webBubbleCount');
  let webHideBubbleTimer = null;

  function updateWebFastScroller() {
    if (!webFastScroller || !trackListContainer) return;
    const items = trackListContainer.querySelectorAll('.list-item');
    if (items.length > 8) {
      webFastScroller.style.display = 'flex';
      syncWebThumbPosition();
    } else {
      webFastScroller.style.display = 'none';
      if (webFastBubble) webFastBubble.classList.remove('is-active');
    }
  }

  function syncWebThumbPosition() {
    if (!webFastScroller || !webFastThumb || !trackListContainer) return;
    const maxScroll = trackListContainer.scrollHeight - trackListContainer.clientHeight;
    if (maxScroll <= 0) {
      webFastThumb.style.transform = 'translateY(0)';
      return;
    }
    const ratio = Math.max(0, Math.min(1, trackListContainer.scrollTop / maxScroll));
    const scrollerHeight = webFastScroller.clientHeight;
    const thumbHeight = webFastThumb.clientHeight || 44;
    const maxTravel = Math.max(0, scrollerHeight - thumbHeight);
    webFastThumb.style.transform = `translateY(${ratio * maxTravel}px)`;
  }

  if (trackListContainer) {
    trackListContainer.addEventListener('scroll', () => {
      syncWebThumbPosition();
    }, { passive: true });
  }

  function handleFastScroll(clientY) {
    if (!webFastScroller || !trackListContainer) return;
    const rect = webFastScroller.getBoundingClientRect();
    const scrollerHeight = rect.height;
    if (scrollerHeight <= 0) return;

    let offsetY = clientY - rect.top;
    offsetY = Math.max(0, Math.min(offsetY, scrollerHeight));
    const ratio = offsetY / scrollerHeight;

    const maxScroll = trackListContainer.scrollHeight - trackListContainer.clientHeight;
    trackListContainer.scrollTop = ratio * maxScroll;

    const thumbHeight = webFastThumb ? (webFastThumb.clientHeight || 44) : 44;
    const maxTravel = Math.max(0, scrollerHeight - thumbHeight);
    if (webFastThumb) {
      webFastThumb.style.transform = `translateY(${ratio * maxTravel}px)`;
    }

    const items = trackListContainer.querySelectorAll('.list-item');
    if (items.length > 0) {
      const targetIdx = Math.min(Math.floor(ratio * items.length), items.length - 1);
      const targetItem = items[targetIdx];
      const titleEl = targetItem ? targetItem.querySelector('.item-title') : null;
      let letter = '#';
      if (titleEl && titleEl.textContent) {
        const firstChar = titleEl.textContent.trim().charAt(0).toUpperCase();
        if (firstChar >= 'A' && firstChar <= 'Z') letter = firstChar;
      }

      if (webBubbleLetter) webBubbleLetter.textContent = letter;
      if (webBubbleCount) webBubbleCount.textContent = `${targetIdx + 1} / ${items.length}`;

      clearTimeout(webHideBubbleTimer);
      if (webFastBubble) webFastBubble.classList.add('is-active');
    }
  }

  if (webFastScroller) {
    let isDraggingScroller = false;

    webFastScroller.addEventListener('mousedown', (e) => {
      isDraggingScroller = true;
      handleFastScroll(e.clientY);
    });

    window.addEventListener('mousemove', (e) => {
      if (isDraggingScroller) {
        handleFastScroll(e.clientY);
      }
    });

    window.addEventListener('mouseup', () => {
      if (isDraggingScroller) {
        isDraggingScroller = false;
        clearTimeout(webHideBubbleTimer);
        webHideBubbleTimer = setTimeout(() => {
          if (webFastBubble) webFastBubble.classList.remove('is-active');
        }, 450);
      }
    });

    webFastScroller.addEventListener('touchstart', (e) => {
      if (e.touches && e.touches.length === 1) {
        isDraggingScroller = true;
        handleFastScroll(e.touches[0].clientY);
      }
    }, { passive: true });

    window.addEventListener('touchmove', (e) => {
      if (isDraggingScroller && e.touches && e.touches.length === 1) {
        handleFastScroll(e.touches[0].clientY);
      }
    }, { passive: true });

    window.addEventListener('touchend', () => {
      if (isDraggingScroller) {
        isDraggingScroller = false;
        clearTimeout(webHideBubbleTimer);
        webHideBubbleTimer = setTimeout(() => {
          if (webFastBubble) webFastBubble.classList.remove('is-active');
        }, 450);
      }
    }, { passive: true });
  }

  // =================== GESTIÓN Y REPRODUCCIÓN DEL ÁRBOL JERÁRQUICO ===================
  /** Canciones directas de una carpeta del árbol (o de un resultado de búsqueda de carpetas), A-Z. */
  function tracksOfFolderNode(node) {
    const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
    const nodeUsb = node.usbSource || (node.path && node.path.includes('udisk2') ? 'USB 2' : 'USB 1');
    let folderTracks = allPool.filter(t => {
      if (!isSameUsb(t, nodeUsb)) return false;
      if (t.folder === node.name) return true;
      if (t.path && node.path) {
        const normTrack = t.path.replace(/\/+$/, '');
        const normNode = node.path.replace(/\/+$/, '');
        if (normTrack === normNode) return true;
      }
      return false;
    });

    if (folderTracks.length === 0) {
      folderTracks = allPool.filter(t => t.path && node.path && t.path.startsWith(node.path));
    }

    sortTracksAlphabetically(folderTracks);
    return folderTracks;
  }

  // Dos toques seguidos en la fila de una carpeta = botón [N ▶] (igual que la app)
  const FOLDER_DOUBLE_TAP_MS = 350;
  let lastTreeTap = { id: null, at: 0 };
  // Carpeta tocada por última vez: su nombre se muestra completo, en hasta 2 renglones
  let fullTitleNodeId = null;

  function playFolderTree(node) {
    const allPool = masterTracks.length > 0 ? masterTracks : sampleTracks;
    const folderTracks = tracksOfFolderNode(node);

    setPistasTracks(folderTracks.length > 0 ? folderTracks : [...allPool]);
    currentFolder = node.name;
    currentFolderUsbSource = node.usbSource || (node.path && node.path.includes('udisk2') ? 'USB 2' : 'USB 1');
    const usbTag = currentFolderUsbSource ? `[${currentFolderUsbSource}] ` : '';
    crumbFolder.textContent = `${usbTag}${node.name}`;
    songCountEl.textContent = pistasTracks.length;

    // Abrir lista de pistas mostrando las canciones de la carpeta seleccionada (sin autoreproducir a pedido de Hernan)
    tabButtons[1].click();
  }

  function getAllTreeIds(nodes) {
    let ids = [];
    nodes.forEach(n => {
      ids.push(n.id);
      if (n.children && n.children.length > 0) {
        ids = ids.concat(getAllTreeIds(n.children));
      }
    });
    return ids;
  }

  // Botón "Colapsar Todo" (Solo icono, flechas hacia adentro)
  if (btnCollapseAllTree) {
    btnCollapseAllTree.addEventListener('click', (e) => {
      e.stopPropagation();
      expandedTreeNodes.clear();
      renderList();
    });
  }

  // Búsqueda en vivo inteligente
  let searchTimer = null;
  searchInput.addEventListener('input', () => {
    const val = searchInput.value.trim();
    if (val.length > 0) {
      clearSearch.classList.remove('hidden');
      if (searchSpinner) searchSpinner.classList.remove('hidden');
      crumbFolder.innerHTML = `⏳ Buscando: "<strong>${escapeHtml(val)}</strong>"...`;

      // EFECTO VISUAL INMEDIATO: Listado actual borroso y overlay flotante de carga
      trackListContainer.classList.add('is-searching');
      if (searchOverlayLoading) searchOverlayLoading.classList.remove('hidden');
      if (webFastScroller) webFastScroller.style.display = 'none';
    } else {
      clearSearch.classList.add('hidden');
      if (searchSpinner) searchSpinner.classList.add('hidden');
      trackListContainer.classList.remove('is-searching');
      if (searchOverlayLoading) searchOverlayLoading.classList.add('hidden');
      // Preservar el filtro que el usuario tiene activo (USB 1, USB 2, TODO, Carpeta)
      updateFilterChipsUI();
      renderList();
      return;
    }

    if (searchTimer) clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      renderList();
      trackListContainer.classList.remove('is-searching');
      if (searchOverlayLoading) searchOverlayLoading.classList.add('hidden');
      if (searchSpinner) searchSpinner.classList.add('hidden');
    }, 180);
  });

  // Botón X para borrar búsqueda y ocultar teclado
  clearSearch.addEventListener('click', () => {
    if (searchTimer) clearTimeout(searchTimer);
    searchInput.value = '';
    clearSearch.classList.add('hidden');
    if (searchSpinner) searchSpinner.classList.add('hidden');
    trackListContainer.classList.remove('is-searching');
    if (searchOverlayLoading) searchOverlayLoading.classList.add('hidden');
    searchInput.blur();
    // Preservar el filtro que el usuario tiene activo (USB 1, USB 2, TODO, Carpeta)
    updateFilterChipsUI();
    renderList();
  });

  // Ocultar teclado virtual al presionar Enter en el buscador
  searchInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      searchInput.blur();
    }
  });

  crumbRoot.addEventListener('click', () => {
    tabButtons[0].click();
  });

  // =================== BOTÓN RE-ESCANEAR USB (NATIVO / AUTO) ===================
  const btnRescanUsb = document.getElementById('btnRescanUsb');
  const rescanLabel = document.getElementById('rescanLabel');

  if (btnRescanUsb) {
    btnRescanUsb.addEventListener('click', () => {
      if (!window.AndroidBridge && fileInput) {
        fileInput.click();
        return;
      }
      btnRescanUsb.classList.add('is-spinning');
      if (rescanLabel) rescanLabel.textContent = "Escaneando...";

      // En Android nativo, re-escanear los dos pendrives udisk1 y udisk2
      if (window.AndroidBridge && window.AndroidBridge.rescanUsbDrives) {
        window.AndroidBridge.rescanUsbDrives();
      }

      setTimeout(() => {
        btnRescanUsb.classList.remove('is-spinning');
        if (rescanLabel) rescanLabel.textContent = "USB Actualizado";
        setTimeout(() => {
          if (rescanLabel) rescanLabel.textContent = "Re-escanear USB";
        }, 1600);
        renderList();
      }, 900);
    });
  }

  if (fileInput) {
    fileInput.addEventListener('change', (e) => {
      const files = Array.from(e.target.files).filter(f => f.type.startsWith('video/') || /\.(mp4|mkv|webm|mov|avi|m4v|3gp)$/i.test(f.name));
      if (files.length === 0) {
        showMixToast("No hay videos en esa carpeta");
        return;
      }
      const loaded = files.map((file, i) => {
        // La carpeta elegida hace de pendrive: "Viaje/Baños/video.mp4" -> /storage/udisk1/Viaje/Baños/
        const relDir = (file.webkitRelativePath || file.name).split('/').slice(0, -1).join('/');
        const dirParts = relDir.split('/').filter(Boolean);
        const cleanName = file.name.replace(/\.[^/.]+$/, "");
        const parts = cleanName.split(' - ');
        const t = {
          id: Date.now() + i,
          title: parts.length > 1 ? parts.slice(1).join(' - ').trim() : cleanName,
          artist: parts.length > 1 ? parts[0].trim() : "Video",
          album: dirParts[dirParts.length - 1] || "PC",
          folder: dirParts[dirParts.length - 1] || "PC",
          path: "/storage/udisk1/" + (relDir ? relDir + "/" : ""),
          duration: "--:--",
          durationSec: 0,
          format: file.name.split('.').pop().toUpperCase(),
          favorite: false,
          audioUrl: URL.createObjectURL(file)
        };
        readVideoInfo(t);
        return t;
      });
      sortTracksAlphabetically(loaded);
      masterTracks = loaded;
      currentTreeData = buildTreeFromTracks(loaded);
      foldersList.length = 0;
      const map = {};
      loaded.forEach(t => {
        if (!map[t.path]) map[t.path] = { name: t.folder, path: t.path, count: 0, usbSource: "USB 1" };
        map[t.path].count++;
      });
      foldersList.push(...Object.values(map));
      currentPlaylist = loaded.filter(t => t.path === loaded[0].path);
      pistasTracks = currentPlaylist;
      playingFromMix = false;
      currentFolder = loaded[0].folder;
      songCountEl.textContent = currentPlaylist.length;
      showMixToast(`${loaded.length} ${loaded.length === 1 ? 'video' : 'videos'} de la PC`);
      tabButtons[0].click();
      loadTrack(0, false);
      fileInput.value = '';
    });
  }

  /** Duración, resolución y una imagen del video real (para el color del fondo). */
  function readVideoInfo(t) {
    const probe = document.createElement('video');
    probe.preload = 'metadata';
    probe.muted = true;
    probe.src = t.audioUrl;
    probe.addEventListener('loadedmetadata', () => {
      t.durationSec = Math.round(probe.duration) || 0;
      t.duration = formatTime(t.durationSec);
      if (probe.videoHeight) t.format = t.format + ' ' + probe.videoHeight + 'P';
      probe.currentTime = Math.min(5, (probe.duration || 0) / 3);
      renderList();
      if (currentPlaylist[currentTrackIndex] === t) {
        totalDurationEl.textContent = t.duration;
        formatBadge.textContent = t.format;
      }
    });
    probe.addEventListener('seeked', () => {
      try {
        const c = document.createElement('canvas');
        c.width = 64; c.height = 36;
        c.getContext('2d').drawImage(probe, 0, 0, 64, 36);
        t.coverUrl = c.toDataURL('image/jpeg', 0.7);
        if (currentPlaylist[currentTrackIndex] === t) updateDynamicTheme(t);
      } catch (e) {}
      probe.removeAttribute('src');
      probe.load();
    }, { once: true });
  }

  // =================== PANTALLA COMPLETA AUTO ===================
  if (btnToggleFullscreen && frameWrapper) {
    btnToggleFullscreen.addEventListener('click', () => {
      frameWrapper.classList.toggle('is-fullscreen');
    });
  }

  // =================== CONTROLES POR TECLADO ===================
  // Simulación de los botones del volante del auto
  window.addEventListener('keydown', (e) => {
    if (document.activeElement === searchInput) return;

    if (e.code === 'Space') {
      e.preventDefault();
      togglePlayPause();
    } else if (e.code === 'ArrowRight') {
      nextTrack();
    } else if (e.code === 'ArrowLeft') {
      prevTrack();
    } else if (e.code === 'ArrowUp') {
      e.preventDefault();
      setVolume(currentVolume + 10);
    } else if (e.code === 'ArrowDown') {
      e.preventDefault();
      setVolume(currentVolume - 10);
    } else if (e.key === 'm' || e.key === 'M') {
      toggleMute();
    }
  });

  // Constructor de árbol dinámico jerárquico a partir de canciones reales leídas por Android
  function buildTreeFromTracks(tracks) {
    if (!tracks || tracks.length === 0) return JSON.parse(JSON.stringify(defaultTreeFolders));

    const rootUsbMap = {};
    tracks.forEach(track => {
      const usb = track.usbSource || (track.path && track.path.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1');
      const usbId = usb === 'USB 2' ? 'usb2' : 'usb1';
      const usbName = `${usb} (${usb === 'USB 2' ? 'udisk2' : 'udisk1'})`;
      const usbPath = usb === 'USB 2' ? '/storage/udisk2/' : '/storage/udisk1/';

      if (!rootUsbMap[usbId]) {
        rootUsbMap[usbId] = {
          id: usbId,
          name: usbName,
          isUsbRoot: true,
          usbSource: usb,
          path: usbPath,
          count: 0,
          directCount: 0,
          folders: {}
        };
      }
      rootUsbMap[usbId].count++;

      let relPath = track.path || '';
      if (relPath.startsWith(usbPath)) {
        relPath = relPath.slice(usbPath.length);
      }
      const parts = relPath.split('/').filter(p => p.trim().length > 0);
      if (parts.length > 0) {
        let curLvl = rootUsbMap[usbId].folders;
        let accumPath = usbPath;
        parts.forEach((p, idx) => {
          accumPath += p + '/';
          if (!curLvl[p]) {
            curLvl[p] = {
              id: `${usbId}_${p.replace(/[^a-zA-Z0-9]/g, '_')}_${idx}`,
              name: p,
              usbSource: usb,
              path: accumPath,
              count: 0,
              directCount: 0,
              childrenMap: {}
            };
          }
          curLvl[p].count++;
          if (idx === parts.length - 1) {
            curLvl[p].directCount++;
          }
          curLvl = curLvl[p].childrenMap;
        });
      } else {
        rootUsbMap[usbId].directCount++;
      }
    });

    function mapChildren(obj) {
      return Object.values(obj).map(item => ({
        id: item.id,
        name: item.name,
        usbSource: item.usbSource,
        path: item.path,
        count: item.count,
        directCount: item.directCount || 0,
        children: mapChildren(item.childrenMap)
      }));
    }

    return Object.values(rootUsbMap).map(u => ({
      id: u.id,
      name: u.name,
      isUsbRoot: true,
      usbSource: u.usbSource,
      path: u.path,
      count: u.count,
      directCount: u.directCount || 0,
      children: mapChildren(u.folders)
    }));
  }

  // Callback global invocado desde Android cuando se terminan de escanear los pendrives
  window.onUsbMusicLoaded = function(tracks) {
    if (!tracks || tracks.length === 0) {
      console.log("No se encontraron videos en los pendrives.");
      return;
    }

    sortTracksAlphabetically(tracks);
    masterTracks = [...tracks];
    currentPlaylist = tracks;
    pistasTracks = tracks;
    playingFromMix = false;
    updateMixTabCount();

    // Extraer todas las carpetas únicas de ambos pendrives
    const foldersMap = {};
    tracks.forEach(t => {
      const fName = t.folder || "Memoria USB";
      if (!foldersMap[fName]) {
        foldersMap[fName] = { 
          name: fName, 
          path: t.path, 
          count: 0,
          usbSource: t.usbSource || (t.path && t.path.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1')
        };
      }
      foldersMap[fName].count++;
    });

    foldersList.length = 0;
    foldersList.push(...Object.values(foldersMap));

    // Reconstruir árbol jerárquico dinámico (colapsado por defecto)
    currentTreeData = buildTreeFromTracks(tracks);
    expandedTreeNodes = new Set();

    songCountEl.textContent = currentPlaylist.length;
    currentFolder = currentPlaylist[0].folder || "USB";
    currentFolderUsbSource = currentPlaylist[0].usbSource || (currentPlaylist[0].path && currentPlaylist[0].path.toLowerCase().includes('udisk2') ? 'USB 2' : 'USB 1');
    const usbTag = currentFolderUsbSource ? `[${currentFolderUsbSource}] ` : '';
    crumbFolder.textContent = `${usbTag}${currentFolder}`;



    // Cargar la primera canción real del pendrive
    loadTrack(0, false);
    renderList();
  };

  // Fin de canción en reproductor nativo de Android
  window.onNativeAudioEnded = function() {
    if (isRepeat) {
      loadTrack(currentTrackIndex, true);
    } else {
      nextTrack();
    }
  };

  // Botón manual para re-escanear pendrives
  const btnUsbLoad = document.getElementById('btnUsbLoad');
  const usbLoadLabel = document.getElementById('usbLoadLabel');
  if (btnUsbLoad) {
    btnUsbLoad.addEventListener('click', (e) => {
      if (window.AndroidBridge && window.AndroidBridge.scanUsbMusicAsync) {
        e.preventDefault();
        window.AndroidBridge.scanUsbMusicAsync();
        if (usbLoadLabel) usbLoadLabel.textContent = "Buscando en USBs...";
        setTimeout(() => {
          if (usbLoadLabel) usbLoadLabel.textContent = "Cargar desde Pendrive";
        }, 3000);
      }
    });
  }

  // =================== JH VIDEO: CUADRO DEL VIDEO ===================
  const simCanvas = document.getElementById('simCanvas');
  const simCtx = simCanvas ? simCanvas.getContext('2d') : null;
  const nowPlayingPillEl = document.getElementById('nowPlayingPill');
  const fsOverlay = document.getElementById('fsOverlay');
  let simFrame = null;

  /** Qué se ve en el cuadro: el video real o el de muestra (canvas). */
  function updateVideoSurface() {
    const track = currentPlaylist[currentTrackIndex];
    const real = !!(track && track.audioUrl);
    albumContainer.classList.toggle('has-video', real);
    albumContainer.classList.toggle('has-sim', !!track && !real);
    if (albumPlaceholder) albumPlaceholder.classList.toggle('hidden', !!track);
    const fsTitle = document.getElementById('fsTitle');
    if (fsTitle && track) fsTitle.textContent = `${track.title} • ${track.artist}`;
    const fsPlayIcon = fsOverlay && fsOverlay.querySelector('.fs-play-icon');
    const fsPauseIcon = fsOverlay && fsOverlay.querySelector('.fs-pause-icon');
    if (fsPlayIcon) fsPlayIcon.classList.toggle('hidden', isPlaying);
    if (fsPauseIcon) fsPauseIcon.classList.toggle('hidden', !isPlaying);
    drawSimFrame();
    if (isPlaying && !real) startSimLoop(); else stopSimLoop();
    // Pantalla completa: en pausa los controles quedan a la vista; al reanudar se ocultan solos
    if (frameWrapper.classList.contains('is-video-fs') && fsOverlay) {
      if (!isPlaying || fsOverlay.classList.contains('is-visible')) showFsOverlay(true);
    }
  }

  /** Dibuja un cuadro del video de muestra: degradado, figuras que se mueven, título y tiempo. */
  function drawSimFrame() {
    const track = currentPlaylist[currentTrackIndex];
    if (!simCtx || !track || track.audioUrl) return;
    const W = simCanvas.width, H = simCanvas.height;
    const [a, b] = track.simColors || ['#334155', '#0f172a'];
    const t = (performance.now() / 1000);
    const g = simCtx.createLinearGradient(0, 0, W, H);
    g.addColorStop(0, a);
    g.addColorStop(1, b);
    simCtx.fillStyle = g;
    simCtx.fillRect(0, 0, W, H);
    const move = isPlaying ? t : 0;
    for (let k = 0; k < 5; k++) {
      const x = (W * (0.15 + 0.18 * k) + Math.sin(move * (0.6 + k * 0.15) + k) * 60);
      const y = (H * 0.55 + Math.cos(move * (0.5 + k * 0.2) + k * 2) * 70);
      simCtx.fillStyle = `rgba(255,255,255,${0.06 + k * 0.025})`;
      simCtx.beginPath();
      simCtx.arc(x, y, 40 + k * 14, 0, Math.PI * 2);
      simCtx.fill();
    }
    simCtx.fillStyle = 'rgba(0,0,0,0.25)';
    simCtx.fillRect(0, H - 70, W, 70);
    simCtx.fillStyle = '#ffffff';
    simCtx.font = '800 34px "Plus Jakarta Sans", Arial, sans-serif';
    simCtx.fillText(track.title.length > 30 ? track.title.slice(0, 29) + '…' : track.title, 24, H - 30);
    simCtx.font = '600 18px "Plus Jakarta Sans", Arial, sans-serif';
    simCtx.fillStyle = 'rgba(255,255,255,0.8)';
    simCtx.fillText(track.artist, 24, H - 8);
    simCtx.font = '800 14px "JetBrains Mono", monospace';
    simCtx.fillStyle = 'rgba(255,255,255,0.7)';
    simCtx.fillText('VIDEO DE MUESTRA', W - 170, 28);
  }

  function startSimLoop() {
    if (simFrame) return;
    const loop = () => {
      drawSimFrame();
      simFrame = requestAnimationFrame(loop);
    };
    simFrame = requestAnimationFrame(loop);
  }

  function stopSimLoop() {
    if (simFrame) cancelAnimationFrame(simFrame);
    simFrame = null;
  }

  // =================== JH VIDEO: PANTALLA COMPLETA ===================
  let fsHideTimer = null;

  function setVideoFullscreen(on) {
    frameWrapper.classList.toggle('is-video-fs', on);
    showFsOverlay(on);
  }

  /** Muestra los controles de pantalla completa; mientras se reproduce, se ocultan solos a los 3 s. */
  function showFsOverlay(show) {
    clearTimeout(fsHideTimer);
    if (!fsOverlay) return;
    fsOverlay.classList.toggle('is-visible', show);
    if (show && isPlaying) fsHideTimer = setTimeout(() => fsOverlay.classList.remove('is-visible'), 3000);
  }

  // Toques en el video: doble toque entra o sale de pantalla completa; un toque, solo en pantalla
  // completa, muestra u oculta los controles. Se espera 280 ms para saber si viene un segundo toque
  // (lo mismo que Siguiente / Anterior en JH MUSIC)
  const VIDEO_DOUBLE_TAP_MS = 280;
  let videoTapTimer = null;

  function onVideoFrameTap(e) {
    if (e.target.closest('.fs-btn') || e.target.closest('.fs-progress') || e.target.closest('.video-float-btn')) return;
    const inFs = frameWrapper.classList.contains('is-video-fs');
    if (videoTapTimer) {
      // segundo toque: doble toque
      clearTimeout(videoTapTimer);
      videoTapTimer = null;
      setVideoFullscreen(!inFs);
      return;
    }
    videoTapTimer = setTimeout(() => {
      videoTapTimer = null;
      if (frameWrapper.classList.contains('is-video-fs')) {
        showFsOverlay(!fsOverlay.classList.contains('is-visible'));
      }
    }, VIDEO_DOUBLE_TAP_MS);
  }

  function bindFs(id, fn) {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', (e) => {
      e.stopPropagation();
      fn();
      showFsOverlay(true);
    });
  }
  bindFs('fsExit', () => {
    setPlayerMenuOpen(false);
    setVideoFullscreen(false);
  });
  bindFs('fsAudioOnly', () => setAudioOnly(!audioOnly));
  document.getElementById('fsMenu').addEventListener('click', (e) => {
    e.stopPropagation();
    setPlayerMenuOpen(true);
  });
  bindFs('fsPlay', () => togglePlayPause());
  bindFs('fsPrev', () => {
    const el = document.getElementById('fsPrev');
    if (el) {
      el.classList.add('pressed');
      setTimeout(() => el.classList.remove('pressed'), 180);
    }
    handlePrevTouch();
  });
  bindFs('fsNext', () => {
    const el = document.getElementById('fsNext');
    if (el) {
      el.classList.add('pressed');
      setTimeout(() => el.classList.remove('pressed'), 180);
    }
    handleNextTouch();
  });
  // Barra de tiempo de pantalla completa: se toca o se arrastra. Mientras se arrastra muestra adónde
  // vas; al soltar salta ahí, con la misma lógica que la barra del reproductor
  const fsProgressBar = document.getElementById('fsProgressBar');
  let fsDragRatio = null;

  function fsRatioAt(clientX) {
    const rect = fsProgressBar.querySelector('.fs-track').getBoundingClientRect();
    return Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
  }

  function showFsPosition(ratio) {
    const track = currentPlaylist[currentTrackIndex];
    const total = audio.src && audio.duration ? audio.duration : (track ? track.durationSec : 0);
    document.getElementById('fsProgress').style.width = (ratio * 100) + '%';
    document.getElementById('fsThumb').style.left = (ratio * 100) + '%';
    document.getElementById('fsTime').textContent = `${formatTime(ratio * total)} / ${totalDurationEl.textContent}`;
  }

  if (fsProgressBar) {
    fsProgressBar.addEventListener('click', (e) => e.stopPropagation());
    fsProgressBar.addEventListener('pointerdown', (e) => {
      e.stopPropagation();
      try { fsProgressBar.setPointerCapture(e.pointerId); } catch (err) {}
      fsDragRatio = fsRatioAt(e.clientX);
      clearTimeout(fsHideTimer); // que no se oculten los controles mientras se arrastra
      showFsPosition(fsDragRatio);
    });
    fsProgressBar.addEventListener('pointermove', (e) => {
      if (fsDragRatio === null) return;
      fsDragRatio = fsRatioAt(e.clientX);
      showFsPosition(fsDragRatio);
    });
    const finish = (e) => {
      if (fsDragRatio === null) return;
      const ratio = fsRatioAt(e.clientX);
      fsDragRatio = null;
      const mainRect = progressBarContainer.getBoundingClientRect();
      progressBarContainer.dispatchEvent(new MouseEvent('click', { clientX: mainRect.left + ratio * mainRect.width }));
      showFsOverlay(true);
    };
    fsProgressBar.addEventListener('pointerup', finish);
    fsProgressBar.addEventListener('pointercancel', () => { fsDragRatio = null; showFsOverlay(true); });
  }

  // La barra de pantalla completa copia el tiempo de la del reproductor
  setInterval(() => {
    if (!frameWrapper.classList.contains('is-video-fs')) return;
    const fill = document.getElementById('fsProgress');
    const time = document.getElementById('fsTime');
    if (fsDragRatio !== null) return; // mientras se arrastra manda la posición del dedo
    const thumb = document.getElementById('fsThumb');
    if (fill) fill.style.width = progressCurrent.style.width || '0%';
    if (thumb) thumb.style.left = progressCurrent.style.width || '0%';
    if (time) time.textContent = `${currentTimeEl.textContent} / ${totalDurationEl.textContent}`;
  }, 250);

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && frameWrapper.classList.contains('is-video-fs')) setVideoFullscreen(false);
  });

  // Sincronizar volumen nativo y escanear pendrive si está en la pantalla del auto
  if (window.AndroidBridge) {
    try {
      if (window.AndroidBridge.getSystemVolume) {
        const sysVol = window.AndroidBridge.getSystemVolume();
        if (sysVol >= 0 && sysVol <= 100) setVolume(sysVol);
      }
      if (window.AndroidBridge.scanUsbMusicAsync) {
        window.AndroidBridge.scanUsbMusicAsync();
      }
    } catch(e) {
      console.warn("AndroidBridge inicialización:", e);
    }
  }

  // Inicialización
  songCountEl.textContent = currentPlaylist.length;
  if (searchInput) {
    searchInput.placeholder = (activeTab === 'folders') ? "Buscar entre carpetas..." : "Buscar video, artista o carpeta...";
  }
  loadTrack(0, false);
  updateVideoSurface();
  renderList();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
