package com.jh.carvideo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int PERM_REQUEST_CODE = 2024;

    // Barra Superior
    private TextView tvHeaderPath, tvClock;
    private Button btnRescanUsb;

    // Franja de Volumen Conductor
    private ImageButton btnNavHome;
    private ImageButton btnVolMute;
    private TextView tvVolPercent;
    private Button btnVolUp, btnVolDown;
    private VerticalVolumeBar seekVolume;
    private int currentVolPercent = 75;

    // Reproductor Central
    private TextView tvPlaybackStatus;
    private ImageButton btnToggleExplorer;
    private View btnAutoMix;              // interruptor Automix: cuadro + ícono + ON / OFF
    private ImageView ivAutoMixIcon;
    private TextView tvAutoMixState;
    private AmbientTheme ambientTheme;    // fondo de toda la app con el color de la carátula
    private AlbumGlowView albumGlow;      // resplandor de la carátula con el color de la canción
    private AutoMixer autoMixer;
    private AudioAnalyzer audioAnalyzer;
    private View layoutExplorerToggleZone;
    private View layoutAlbumArtCard;
    private ImageView ivAlbumArt;
    private NowPlayingPillView nowPlayingPill; // cápsula PAUSED / PLAYING / MIXING ⇄ con ecualizador
    private LaserRingView laserRing;           // borde láser giratorio de la carátula
    private TextView tvFormatBadge;
    private TextView tvTrackTitle, tvTrackArtist, tvTrackArtistDot, tvTrackAlbum;
    private View layoutPrevCard, layoutNextCard;
    private TextView tvPrevTrackPreview, tvNextTrackPreview;

    // Gestos globales del conductor para mostrar / ocultar explorador
    private float globalTouchStartX = 0f;
    private float globalTouchStartY = 0f;
    private long globalTouchStartTime = 0;
    private boolean globalTouchOnSeekbar = false;
    private boolean globalTouchInToggleZone = false;
    private boolean globalGestureHandled = false;
    private SeekBar seekTrackProgress;
    private TextView tvTimeCurrent, tvTimeTotal;
    private ImageButton btnShuffle, btnPrev, btnPlayPause, btnNext, btnRepeat;

    // Explorador y Reproductor
    private LinearLayout layoutExplorerPanel;
    private View layoutPlayerPanel;
    private Button tabFolders, tabTracks, tabMyMix;
    // Cabecera de Mi Mix
    private View layoutMixHeader;
    private TextView tvMixHeaderTitle, tvMixHeaderSub, tvMixEmpty;
    private ImageButton btnMixPlay;
    private Button btnMixConfirmClear, btnMixCancelClear;
    private ImageButton btnMixClear;
    private ImageButton btnMixReorder;
    private Button btnMixReorderDone;
    private boolean isReorderingMix = false;
    private EditText etSearch;
    private ImageButton btnSearchClear;
    private View pbSearchLoading;
    private TextView tvCurrentPath, tvStorageStatus;
    private Button btnPlayAll;
    private ListView listViewContent;
    private ImageButton btnCollapseAllTree;
    private View scrollSearchFilters;

    // FastScroller lateral ergonómico y burbuja flotante A-Z para 200+ canciones
    private LinearLayout layoutFastScrollerTrack;
    private View viewFastScrollThumb;
    private LinearLayout layoutFastScrollBubble;
    private TextView tvFastScrollBubbleLetter;
    private TextView tvFastScrollBubbleTrackNumber;
    private LinearLayout layoutSearchLoading;
    private final Handler fastScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable hideBubbleRunnable;

    // Detección de 1 toque (cambio de pista) vs 2 toques (adelantar/retroceder 10s)
    private static final int SEEK_SKIP_MS = 10000;
    private static final int DOUBLE_TAP_TIMEOUT_MS = 280;
    private final Handler navTapHandler = new Handler(Looper.getMainLooper());
    private Runnable nextTapRunnable = null;
    private Runnable prevTapRunnable = null;
    private int nextTapHits = 0;
    private int prevTapHits = 0;
    private final Runnable resetNextTapHitsRunnable = () -> nextTapHits = 0;
    private final Runnable resetPrevTapHitsRunnable = () -> prevTapHits = 0;

    // Adaptadores
    private TrackAdapter trackAdapter;
    private FolderAdapter folderAdapter;
    private FolderTreeAdapter folderTreeAdapter;
    private RecyclerView recyclerMixContent;
    private MixRecyclerAdapter mixRecyclerAdapter;
    private ItemTouchHelper mixItemTouchHelper;

    // Listas de datos
    private List<Track> allTracks = new ArrayList<>();
    private List<Track> currentPlaylist = new ArrayList<>();
    private List<Track> displayedTracks = new ArrayList<>();
    private List<FolderItem> folderList = new ArrayList<>();
    private List<FolderItem> displayedFolders = new ArrayList<>();
    private List<TreeNode> rootTreeNodes = new ArrayList<>();
    private List<TreeNode> visibleTreeNodes = new ArrayList<>();

    // Estado del Reproductor
    private int currentTrackIndex = -1; // posición dentro de playQueue
    private final List<Track> playQueue = new ArrayList<>();
    private boolean isPlaying = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean isMuted = false;
    private int currentTab = 0; // 0: Carpetas, 1: Pistas, 2: Mi Mix
    // Búsqueda de Pistas guardada al cambiar de pestaña (se descarta al abrir otra carpeta con [N ▶])
    private String savedTracksQuery = "";
    private int savedTracksFilter = FILTER_FOLDER;
    private String currentSelectedFolder = null;
    private String currentSelectedFolderUsbSource = null;

    // Filtros de Búsqueda y USBs
    public static final int FILTER_FOLDER = 0; // Esta Carpeta
    public static final int FILTER_ALL = 1;    // TODO
    public static final int FILTER_USB1 = 2;   // USB 1
    public static final int FILTER_USB2 = 3;   // USB 2

    private int currentSearchFilter = FILTER_FOLDER;
    private boolean hasUsb1 = false;
    private boolean hasUsb2 = false;

    private static final String[] USB1_MOUNT_PATHS = {"/storage/udisk1", "/mnt/media_rw/udisk1", "/mnt/udisk1", "/storage/usb0"};
    private static final String[] USB2_MOUNT_PATHS = {"/storage/udisk2", "/mnt/media_rw/udisk2", "/mnt/udisk2", "/storage/usb1"};

    // Re-escaneo automático al conectar/quitar un pendrive (con espera para agrupar los eventos seguidos)
    private final Runnable usbRescanRunnable = this::startUsbScan;
    private final BroadcastReceiver usbMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.removeCallbacks(usbRescanRunnable);
            handler.postDelayed(usbRescanRunnable, 1500);
        }
    };

    private TextView btnFilterFolder;
    private TextView btnFilterAll;
    private TextView btnFilterUsb1;
    private TextView btnFilterUsb2;

    // Búsqueda Asíncrona con Debounce
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable = null;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    // Lectura de duraciones en segundo plano (hilo propio para no retrasar carátulas ni escaneos)
    private final ExecutorService durationExecutor = Executors.newSingleThreadExecutor();
    private final ArrayDeque<Track> pendingDurations = new ArrayDeque<>(); // protegido por sí mismo
    private final Set<Track> visibleDurationTracks = new HashSet<>();      // protegido por pendingDurations
    private volatile int durationGeneration = 0;
    private volatile int searchVersion = 0;

    // Servicios del Sistema
    private AudioManager audioManager;
    private VideoPlayer videoPlayer;
    private Handler handler;
    private Runnable progressRunnable;
    private Runnable clockRunnable;
    private ExecutorService scanExecutor;
    private boolean isUserSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(Looper.getMainLooper());
        audioAnalyzer = new AudioAnalyzer();
        autoMixer = new AutoMixer(handler, autoMixHost, audioAnalyzer);
        scanExecutor = Executors.newSingleThreadExecutor();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Modo pantalla completa inmersiva para pantalla de auto
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setImmersiveMode();

        setContentView(R.layout.activity_main);

        initViews();
        setupFastScroller();
        buildTreeFromTracks(allTracks);
        updateVisibleTreeNodes();
        setupClock();
        setupVolumeControls();
        setupPlayerControls();
        setupVideoViews();
        // JH VIDEO no tiene Automix: entre videos no hay mezcla (así tampoco se analiza el audio)
        autoMixer.setEnabled(false);
        updateAutoMixButton();
        setupExplorerControls();
        setupProgressUpdater();
        startLoadMonitor();

        checkAndRequestStoragePermissions();

        // Detectar cuando se conecta o se quita un pendrive para actualizar Carpetas, Pistas y filtros
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        usbFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        usbFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        usbFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        usbFilter.addDataScheme("file");
        registerReceiver(usbMountReceiver, usbFilter);

        // Ocultar teclado por defecto y remover foco del buscador al abrir la app
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (etSearch != null) etSearch.clearFocus();
    }

    private void initViews() {
        // Barra Superior
        tvHeaderPath = findViewById(R.id.tvHeaderPath);
        if (tvHeaderPath != null) tvHeaderPath.setText("Q hubo jefe 😉");
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            try {
                String verName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                tvAppVersion.setText("v" + verName);
            } catch (Exception e) {
                tvAppVersion.setText("v1.7.0");
            }
        }
        tvClock = findViewById(R.id.tvClock);
        btnRescanUsb = findViewById(R.id.btnRescanUsb);

        // Franja de volumen (54dp)
        btnNavHome = findViewById(R.id.btnNavHome);
        btnVolMute = findViewById(R.id.btnVolMute);
        tvVolPercent = findViewById(R.id.tvVolPercent);
        btnVolUp = findViewById(R.id.btnVolUp);
        btnVolDown = findViewById(R.id.btnVolDown);
        seekVolume = findViewById(R.id.seekVolume);

        // Reproductor central
        tvPlaybackStatus = findViewById(R.id.tvPlaybackStatus);
        btnToggleExplorer = null; // JH VIDEO: sin lupa (el explorador solo se oculta en pantalla completa)
        btnAutoMix = findViewById(R.id.btnAutoMix);
        ivAutoMixIcon = findViewById(R.id.ivAutoMixIcon);
        tvAutoMixState = findViewById(R.id.tvAutoMixState);
        albumGlow = findViewById(R.id.albumGlow);
        View root = findViewById(R.id.layoutRoot);
        if (root != null) ambientTheme = new AmbientTheme(root, albumGlow, handler);
        layoutExplorerToggleZone = null; // sin zona de gestos para ocultar el explorador
        layoutAlbumArtCard = findViewById(R.id.layoutAlbumArtCard);
        ivAlbumArt = findViewById(R.id.ivAlbumArt);
        nowPlayingPill = findViewById(R.id.nowPlayingPill);
        laserRing = findViewById(R.id.laserRing);
        // La carátula se recorta con las esquinas redondeadas del interior (como overflow:hidden en la web)
        View albumInner = findViewById(R.id.layoutAlbumArtInner);
        if (albumInner != null) albumInner.setClipToOutline(true);
        tvFormatBadge = findViewById(R.id.tvFormatBadge);
        tvTrackTitle = findViewById(R.id.tvTrackTitle);
        tvTrackArtist = findViewById(R.id.tvTrackArtist);
        tvTrackArtistDot = findViewById(R.id.tvTrackArtistDot);
        tvTrackAlbum = findViewById(R.id.tvTrackAlbum);
        layoutPrevCard = findViewById(R.id.layoutPrevCard);
        layoutNextCard = findViewById(R.id.layoutNextCard);
        tvPrevTrackPreview = findViewById(R.id.tvPrevTrackPreview);
        tvNextTrackPreview = findViewById(R.id.tvNextTrackPreview);
        seekTrackProgress = findViewById(R.id.seekTrackProgress);
        tvTimeCurrent = findViewById(R.id.tvTimeCurrent);
        tvTimeTotal = findViewById(R.id.tvTimeTotal);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);

        // Explorador y reproductor
        layoutExplorerPanel = findViewById(R.id.layoutExplorerPanel);
        layoutPlayerPanel = findViewById(R.id.layoutPlayerPanel);
        tabFolders = findViewById(R.id.tabFolders);
        tabTracks = findViewById(R.id.tabTracks);
        tabMyMix = findViewById(R.id.tabMyMix);
        etSearch = findViewById(R.id.etSearch);
        btnSearchClear = findViewById(R.id.btnSearchClear);
        pbSearchLoading = findViewById(R.id.pbSearchLoading);
        layoutSearchLoading = findViewById(R.id.layoutSearchLoading);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        if (tvCurrentPath != null) {
            tvCurrentPath.setVisibility(View.GONE);
        }
        tvStorageStatus = findViewById(R.id.tvStorageStatus);
        btnPlayAll = findViewById(R.id.btnPlayAll);
        layoutMixHeader = findViewById(R.id.layoutMixHeader);
        tvMixHeaderTitle = findViewById(R.id.tvMixHeaderTitle);
        tvMixHeaderSub = findViewById(R.id.tvMixHeaderSub);
        tvMixEmpty = findViewById(R.id.tvMixEmpty);
        btnMixPlay = findViewById(R.id.btnMixPlay);
        btnMixClear = findViewById(R.id.btnMixClear);
        btnMixReorder = findViewById(R.id.btnMixReorder);
        btnMixReorderDone = findViewById(R.id.btnMixReorderDone);
        btnMixConfirmClear = findViewById(R.id.btnMixConfirmClear);
        btnMixCancelClear = findViewById(R.id.btnMixCancelClear);
        setupMixHeader();
        listViewContent = findViewById(R.id.listViewContent);
        if (listViewContent != null) {
            listViewContent.setVerticalScrollBarEnabled(false);
            listViewContent.setHorizontalScrollBarEnabled(false);
            listViewContent.setFastScrollEnabled(false);
            listViewContent.setFastScrollAlwaysVisible(false);
            listViewContent.setOverScrollMode(View.OVER_SCROLL_NEVER);
            listViewContent.setOnDragListener(new View.OnDragListener() {
                private int dragFromPos = -1;

                @Override
                public boolean onDrag(View v, DragEvent event) {
                    if (!isReorderingMix) return false;
                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            if (event.getLocalState() instanceof Integer) {
                                dragFromPos = (Integer) event.getLocalState();
                                return true;
                            }
                            return false;

                        case DragEvent.ACTION_DRAG_LOCATION:
                            int y = (int) event.getY();
                            int targetPos = listViewContent.pointToPosition((int) event.getX(), y);
                            if (targetPos != AdapterView.INVALID_POSITION && dragFromPos != -1 && targetPos != dragFromPos
                                    && targetPos >= 0 && targetPos < displayedTracks.size()) {
                                Track targetTrack = displayedTracks.get(targetPos);
                                if (!isMixOrphan(targetTrack)) {
                                    moveMixTrack(dragFromPos, targetPos);
                                    dragFromPos = targetPos;
                                }
                            }
                            // Auto-scroll suave en bordes superior e inferior
                            int h = listViewContent.getHeight();
                            if (y < 60) {
                                listViewContent.smoothScrollBy(-30, 60);
                            } else if (y > h - 60) {
                                listViewContent.smoothScrollBy(30, 60);
                            }
                            return true;

                        case DragEvent.ACTION_DROP:
                            int dropY = (int) event.getY();
                            int finalPos = listViewContent.pointToPosition((int) event.getX(), dropY);
                            if (finalPos != AdapterView.INVALID_POSITION && dragFromPos != -1 && finalPos != dragFromPos
                                    && finalPos >= 0 && finalPos < displayedTracks.size()) {
                                Track targetTrack = displayedTracks.get(finalPos);
                                if (!isMixOrphan(targetTrack)) {
                                    moveMixTrack(dragFromPos, finalPos);
                                }
                            }
                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:
                            dragFromPos = -1;
                            return true;
                    }
                    return true;
                }
            });
        }
        btnCollapseAllTree = findViewById(R.id.btnCollapseAllTree);
        if (btnCollapseAllTree != null) {
            btnCollapseAllTree.setOnClickListener(v -> collapseAllTreeNodes());
        }

        // Filtros de búsqueda (Esta Carpeta, TODO, USB 1, USB 2)
        scrollSearchFilters = findViewById(R.id.scrollSearchFilters);
        btnFilterFolder = findViewById(R.id.btnFilterFolder);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterUsb1 = findViewById(R.id.btnFilterUsb1);
        btnFilterUsb2 = findViewById(R.id.btnFilterUsb2);
        checkUsbPresence();

        // Adaptadores con listeners directos en la fila (evita robo de focus)
        trackAdapter = new TrackAdapter(this, displayedTracks,
                (track, position) -> {
                    hideKeyboard();
                    playTrack(position);
                },
                (track, position) -> onTrackMixButton(track),
                mixState
        );

        folderAdapter = new FolderAdapter(this, displayedFolders,
                (folder, position) -> {
                    hideKeyboard();
                    openFolder(folder.getName(), folder.getUsbSource());
                },
                (folder, position) -> addFolderToMix(tracksOfFolder(folder.getName(), folder.getUsbSource()), folder.getName())
        );

        folderTreeAdapter = new FolderTreeAdapter(this, visibleTreeNodes, new FolderTreeAdapter.OnTreeNodeClickListener() {
            @Override
            public void onNodeClick(TreeNode node, int position) {
                hideKeyboard();
                // Un toque solo explora (expande/contrae); la carpeta activa de Pistas se elige con [N ▶]
                if (node.hasChildren()) {
                    node.setExpanded(!node.isExpanded());
                    updateVisibleTreeNodes();
                }
            }

            @Override
            public void onNodePlayClick(TreeNode node, int position) {
                hideKeyboard();
                playFolderFromNode(node);
            }

            @Override
            public void onNodeAddMixClick(TreeNode node, int position) {
                addFolderToMix(getTracksForNode(node), node.getName());
            }
        });

        listViewContent.setAdapter(folderTreeAdapter);

        // RecyclerView para Mi Mix con apertura de hueco en vivo por hardware (GPU)
        recyclerMixContent = findViewById(R.id.recyclerMixContent);
        if (recyclerMixContent != null) {
            recyclerMixContent.setLayoutManager(new LinearLayoutManager(this));
            mixRecyclerAdapter = new MixRecyclerAdapter(this, displayedTracks, mixState,
                    viewHolder -> {
                        if (mixItemTouchHelper != null) {
                            mixItemTouchHelper.startDrag(viewHolder);
                        }
                    },
                    new MixRecyclerAdapter.OnMixActionListener() {
                        @Override
                        public void onTrackClick(Track track, int position) {
                            hideKeyboard();
                            playTrack(position);
                        }

                        @Override
                        public void onRemoveTrack(Track track, int position) {
                            onTrackMixButton(track);
                        }

                        @Override
                        public void onItemMoved(int fromPos, int toPos) {
                        }

                        @Override
                        public void onReorderDone() {
                            saveReorderedMixFromDisplayedTracks();
                        }
                    }
            );
            recyclerMixContent.setAdapter(mixRecyclerAdapter);

            ItemTouchHelper.SimpleCallback callback =
                    new ItemTouchHelper.SimpleCallback(
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                        @Override
                        public boolean onMove(@NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder,
                                              @NonNull RecyclerView.ViewHolder target) {
                            int fromPos = viewHolder.getAdapterPosition();
                            int toPos = target.getAdapterPosition();
                            if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) {
                                return false;
                            }
                            Track current = playingFromMix ? getCurrentTrack() : null;
                            boolean orphan = current != null && !isInMix(current);
                            if (orphan && (fromPos == 0 || toPos == 0)) {
                                return false; // Proteger la fila huérfana de ser movida o sobrepasada
                            }
                            mixRecyclerAdapter.moveItem(fromPos, toPos);
                            return true;
                        }

                        @Override
                        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        }

                        @Override
                        public boolean isLongPressDragEnabled() {
                            return false;
                        }

                        @Override
                        public boolean isItemViewSwipeEnabled() {
                            return false;
                        }

                        @Override
                        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                            super.onSelectedChanged(viewHolder, actionState);
                            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                                viewHolder.itemView.setScaleX(1.025f);
                                viewHolder.itemView.setScaleY(1.025f);
                                viewHolder.itemView.setBackgroundColor(Color.parseColor("#1f293d"));
                            }
                        }

                        @Override
                        public void clearView(@NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder) {
                            super.clearView(recyclerView, viewHolder);
                            viewHolder.itemView.setScaleX(1.0f);
                            viewHolder.itemView.setScaleY(1.0f);
                            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);

                            // Guardar el nuevo orden definitivo basado en la posición en pantalla
                            saveReorderedMixFromDisplayedTracks();

                            // Actualizar números de orden limpiamente después de que cae el elemento
                            recyclerView.post(() -> {
                                if (mixRecyclerAdapter != null) {
                                    mixRecyclerAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    };

            mixItemTouchHelper = new ItemTouchHelper(callback);
            mixItemTouchHelper.attachToRecyclerView(recyclerMixContent);
        }
    }

    // ==========================================
    // FASTSCROLLER ERGONÓMICO Y BURBUJA FLOTANTE A-Z PARA CONDUCCIÓN
    // ==========================================
    private void setupFastScroller() {
        layoutFastScrollerTrack = findViewById(R.id.layoutFastScrollerTrack);
        viewFastScrollThumb = findViewById(R.id.viewFastScrollThumb);
        layoutFastScrollBubble = findViewById(R.id.layoutFastScrollBubble);
        tvFastScrollBubbleLetter = findViewById(R.id.tvFastScrollBubbleLetter);
        tvFastScrollBubbleTrackNumber = findViewById(R.id.tvFastScrollBubbleTrackNumber);

        hideBubbleRunnable = () -> {
            if (layoutFastScrollBubble != null) {
                layoutFastScrollBubble.animate()
                        .alpha(0f)
                        .setDuration(220)
                        .withEndAction(() -> {
                            if (layoutFastScrollBubble != null) {
                                layoutFastScrollBubble.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        };

        if (listViewContent != null) {
            listViewContent.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {}

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (layoutFastScrollerTrack != null && layoutFastScrollerTrack.getVisibility() == View.VISIBLE && totalItemCount > 0) {
                        int trackHeight = layoutFastScrollerTrack.getHeight();
                        int thumbHeight = (viewFastScrollThumb != null && viewFastScrollThumb.getHeight() > 0) ? viewFastScrollThumb.getHeight() : 46;
                        float maxTranslation = Math.max(0, trackHeight - thumbHeight);
                        float ratio = (float) firstVisibleItem / (float) Math.max(1, totalItemCount - visibleItemCount);
                        ratio = Math.max(0f, Math.min(1f, ratio));
                        if (viewFastScrollThumb != null) {
                            viewFastScrollThumb.setTranslationY(ratio * maxTranslation);
                        }
                    }
                }
            });
        }

        if (layoutFastScrollerTrack != null) {
            layoutFastScrollerTrack.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE: {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            fastScrollHandler.removeCallbacks(hideBubbleRunnable);

                            int trackHeight = layoutFastScrollerTrack.getHeight();
                            if (trackHeight <= 0) return true;

                            float y = event.getY();
                            y = Math.max(0, Math.min(y, trackHeight));
                            float ratio = y / (float) trackHeight;

                            int thumbHeight = (viewFastScrollThumb != null && viewFastScrollThumb.getHeight() > 0) ? viewFastScrollThumb.getHeight() : 46;
                            float maxTranslation = Math.max(0, trackHeight - thumbHeight);
                            if (viewFastScrollThumb != null) {
                                viewFastScrollThumb.setTranslationY(ratio * maxTranslation);
                            }

                            int itemCount = (listViewContent != null && listViewContent.getAdapter() != null)
                                    ? listViewContent.getAdapter().getCount()
                                    : 0;

                            if (itemCount > 0) {
                                int targetIndex = Math.min((int) (ratio * itemCount), itemCount - 1);
                                listViewContent.setSelectionFromTop(targetIndex, 0);
                                updateFastScrollBubble(targetIndex, itemCount);
                            }
                            return true;
                        }

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            fastScrollHandler.removeCallbacks(hideBubbleRunnable);
                            fastScrollHandler.postDelayed(hideBubbleRunnable, 450);
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    private void updateFastScrollBubble(int position, int totalCount) {
        if (layoutFastScrollBubble == null) return;

        String letter = "#";
        if (listViewContent != null && listViewContent.getAdapter() == trackAdapter && position >= 0 && position < displayedTracks.size()) {
            Track t = displayedTracks.get(position);
            if (t != null && t.getTitle() != null && !t.getTitle().trim().isEmpty()) {
                char ch = Character.toUpperCase(t.getTitle().trim().charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    letter = String.valueOf(ch);
                }
            }
        } else if (listViewContent != null && listViewContent.getAdapter() == folderTreeAdapter && position >= 0 && position < visibleTreeNodes.size()) {
            TreeNode node = visibleTreeNodes.get(position);
            if (node != null && node.getName() != null && !node.getName().trim().isEmpty()) {
                char ch = Character.toUpperCase(node.getName().trim().charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    letter = String.valueOf(ch);
                }
            }
        }

        if (tvFastScrollBubbleLetter != null) {
            tvFastScrollBubbleLetter.setText(letter);
        }
        if (tvFastScrollBubbleTrackNumber != null) {
            tvFastScrollBubbleTrackNumber.setText((position + 1) + " / " + totalCount);
        }

        layoutFastScrollBubble.animate().cancel();
        layoutFastScrollBubble.setAlpha(1f);
        layoutFastScrollBubble.setVisibility(View.VISIBLE);
    }

    private void updateFastScrollerVisibility() {
        if (layoutFastScrollerTrack == null) return;
        int count = (listViewContent != null && listViewContent.getAdapter() != null)
                ? listViewContent.getAdapter().getCount()
                : 0;
        if (count > 12) {
            layoutFastScrollerTrack.setVisibility(View.VISIBLE);
        } else {
            layoutFastScrollerTrack.setVisibility(View.GONE);
        }
    }

    private void setupClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (tvClock != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    tvClock.setText(sdf.format(new Date()));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockRunnable);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) view = etSearch;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        }
        if (etSearch != null) {
            etSearch.clearFocus();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                globalTouchStartX = ev.getRawX();
                globalTouchStartY = ev.getRawY();
                globalTouchStartTime = System.currentTimeMillis();
                globalGestureHandled = false;
                globalTouchOnSeekbar = false;

                // Ocultar teclado táctil si se toca fuera de EditText
                View v = getCurrentFocus();
                if (v instanceof EditText) {
                    Rect outRect = new Rect();
                    v.getGlobalVisibleRect(outRect);
                    if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                        hideKeyboard();
                    }
                }

                // Proteger deslizadores (seekbar y volumen) de gestos de panel
                if (seekTrackProgress != null) {
                    Rect seekRect = new Rect();
                    if (seekTrackProgress.getGlobalVisibleRect(seekRect)) {
                        seekRect.inset(0, -25);
                        if (seekRect.contains((int) globalTouchStartX, (int) globalTouchStartY)) {
                            globalTouchOnSeekbar = true;
                        }
                    }
                }
                if (seekVolume != null) {
                    Rect volRect = new Rect();
                    if (seekVolume.getGlobalVisibleRect(volRect)) {
                        volRect.inset(-20, -20);
                        if (volRect.contains((int) globalTouchStartX, (int) globalTouchStartY)) {
                            globalTouchOnSeekbar = true;
                        }
                    }
                }

                // Restringir gesto de arrastrar/swipe ÚNICAMENTE a la zona roja del botón y franja libre
                globalTouchInToggleZone = false;
                if (layoutExplorerToggleZone != null) {
                    Rect toggleRect = new Rect();
                    if (layoutExplorerToggleZone.getGlobalVisibleRect(toggleRect)) {
                        toggleRect.inset(-25, -25);
                        if (toggleRect.contains((int) globalTouchStartX, (int) globalTouchStartY)) {
                            globalTouchInToggleZone = true;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!globalGestureHandled && globalTouchInToggleZone && !globalTouchOnSeekbar && layoutExplorerPanel != null) {
                    float dx = ev.getRawX() - globalTouchStartX;
                    float dy = ev.getRawY() - globalTouchStartY;

                    // Si el explorador está OCULTO: Jalar hacia la derecha muestra el explorador
                    if (layoutExplorerPanel.getVisibility() != View.VISIBLE) {
                        if (dx > 35 && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                            globalGestureHandled = true;
                            setExplorerVisible(true);
                            return true;
                        }
                    }
                    // Si el explorador está VISIBLE: Deslizar hacia la izquierda cierra el explorador
                    else {
                        if (dx < -35 && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                            globalGestureHandled = true;
                            hideKeyboard();
                            setExplorerVisible(false);
                            return true;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (globalGestureHandled) {
                    return true;
                }
                if (globalTouchInToggleZone && !globalTouchOnSeekbar && layoutExplorerPanel != null) {
                    float dx = ev.getRawX() - globalTouchStartX;
                    float dy = ev.getRawY() - globalTouchStartY;

                    if (layoutExplorerPanel.getVisibility() != View.VISIBLE) {
                        if (dx > 35 && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                            globalGestureHandled = true;
                            setExplorerVisible(true);
                            return true;
                        }
                    } else {
                        if (dx < -35 && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                            globalGestureHandled = true;
                            hideKeyboard();
                            setExplorerVisible(false);
                            return true;
                        }
                    }
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        hideKeyboard();

        // 0. En pantalla completa: volver al reproductor
        if (isVideoFs) {
            setVideoFullscreen(false);
            return;
        }

        // 1. Si hay texto en el buscador, borrar búsqueda y restaurar vista en la carpeta que está sonando
        if (etSearch != null && etSearch.getText().length() > 0) {
            etSearch.setText("");
            if (btnSearchClear != null) btnSearchClear.setVisibility(View.GONE);
            if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
            if (currentSelectedFolder != null && !currentPlaylist.isEmpty()) {
                currentSearchFilter = FILTER_FOLDER;
                updateFilterChipsVisibility();
                updateFilterChipsUI();
            }
            filterSearch("");
            return;
        }

        // 2. Si estamos dentro de una carpeta en Pistas (tab 1), volver a la lista de Carpetas (tab 0)
        if (currentTab == 1 && currentSelectedFolder != null) {
            selectTab(0);
            return;
        }

        // 3. Si estamos en Mi Mix (tab 2): si está reordenando, salir del modo reordenar primero; si no, volver a Carpetas (tab 0)
        if (currentTab == 2) {
            if (isReorderingMix) {
                isReorderingMix = false;
                showMixTab();
                return;
            }
            selectTab(0);
            return;
        }

        // 4. Si ya estamos en la raíz (tab 0), realizar la acción física del sistema (salir / minimizar al launcher del auto)
        super.onBackPressed();
    }

    /**
     * Mismo comportamiento que pulsar el botón físico HOME del auto:
     * Minimiza la app al launcher principal de la radio del auto instantáneamente.
     */
    private void goHome() {
        hideKeyboard();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(homeIntent);
        } catch (Exception e) {
            moveTaskToBack(true);
        }
    }

    // ==========================================
    // SISTEMA DE VOLUMEN RÁPIDO CONDUCTOR
    // Pasos de 5 en 5 y barra vertical sin interferencias
    // ==========================================
    private void setupVolumeControls() {
        updateVolumeUI();

        // Botón HOME en pantalla (Mismo comportamiento que el botón físico HOME del auto)
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> goHome());
        }

        btnVolUp.setOnClickListener(v -> adjustVolumeStep(true));
        btnVolDown.setOnClickListener(v -> adjustVolumeStep(false));
        btnVolMute.setOnClickListener(v -> toggleMute());

        seekVolume.setOnVolumeChangeListener((percent, fromUser) -> {
            if (fromUser && audioManager != null) {
                if (isMuted) setMuted(false);
                currentVolPercent = percent;
                int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int target = Math.round((percent / 100f) * max);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
                tvVolPercent.setText(percent + "%");
            }
        });
    }

    private void updateVolumeUI() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // El mute pudo quedar activo al salir con HOME o venir del sistema del radio: leer el estado real
            isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        }
        btnVolMute.setSelected(isMuted);
        btnVolMute.setImageResource(isMuted ? R.drawable.ic_vol_muted : R.drawable.ic_vol_on);
        if (isMuted) {
            // En mute getStreamVolume devuelve 0: se conserva el último porcentaje real para restaurarlo al desmutear
            tvVolPercent.setText("MUTE");
            return;
        }

        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int percent = max > 0 ? Math.round(((float) current / max) * 100) : 0;
        currentVolPercent = percent;

        tvVolPercent.setText(percent + "%");
        seekVolume.setProgress(percent);
    }

    private void adjustVolumeStep(boolean up) {
        if (audioManager == null) return;
        if (isMuted) setMuted(false);
        // Incremento / decremento de 5 en 5 exacto a pedido de Hernan
        int nextPercent = up ? Math.min(100, currentVolPercent + 5) : Math.max(0, currentVolPercent - 5);
        currentVolPercent = nextPercent;

        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int target = Math.round((currentVolPercent / 100f) * max);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);

        seekVolume.setProgress(currentVolPercent);
        tvVolPercent.setText(currentVolPercent + "%");
    }

    private void toggleMute() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        }
        setMuted(!isMuted);
    }

    private void setMuted(boolean mute) {
        if (audioManager == null) return;
        isMuted = mute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
        } else {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
        }
        // Al desmutear se lee el volumen real restaurado por el sistema (no el valor guardado en la app)
        updateVolumeUI();
    }

    // ==========================================
    // CONTROLES DE REPRODUCCIÓN (MEDIA PLAYER)
    // ==========================================
    private void setupPlayerControls() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        // Video: 1 toque no hace nada; doble toque abre la pantalla completa
        if (layoutAlbumArtCard != null) {
            layoutAlbumArtCard.setOnTouchListener(videoTapListener);
        }
        btnNext.setOnClickListener(v -> handleNextButtonClick());
        btnPrev.setOnClickListener(v -> handlePrevButtonClick());

        // Tarjetas flotantes anterior y siguiente
        if (layoutPrevCard != null) layoutPrevCard.setOnClickListener(v -> handlePrevButtonClick());
        if (layoutNextCard != null) layoutNextCard.setOnClickListener(v -> handleNextButtonClick());

        // Botón y Zona Táctil Ergonómica para alternar o deslizar el explorador de música
        setupExplorerToggleGestures();

        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setSelected(isShuffle);
            updateSecondaryButton(btnShuffle, isShuffle);
            autoMixer.invalidateNext();
            Toast.makeText(this, isShuffle ? "Aleatorio activado" : "Modo secuencial", Toast.LENGTH_SHORT).show();
        });

        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setSelected(isRepeat);
            updateSecondaryButton(btnRepeat, isRepeat);
            autoMixer.invalidateNext();
            Toast.makeText(this, isRepeat ? "Repetir video actual" : "Repetir carpeta", Toast.LENGTH_SHORT).show();
        });

        seekTrackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playerReady()) {
                    int duration = playbackDurationMs();
                    if (duration > 0) {
                        int currentMs = (int) ((progress / 1000f) * duration);
                        tvTimeCurrent.setText(formatMs(currentMs));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                if (playerReady()) {
                    autoMixer.finishMixNow(); // adelantar en medio de una mezcla la cancela
                    seekFromBar(seekBar.getProgress());
                }
            }
        });
    }

    /** Se soltó una de las barras (0-1000): salta ahí; si no se puede, las barras vuelven adonde va el video. */
    private void seekFromBar(int progress) {
        int duration = playbackDurationMs();
        if (duration <= 0) {
            showTimeNote("⚠ No se puede saltar: no se sabe cuánto dura este video");
            seekTrackProgress.setProgress(0);
            if (seekFsProgress != null) seekFsProgress.setProgress(0);
            return;
        }
        int targetMs = (int) ((progress / 1000f) * duration);
        int shownMs = seekPlayerTo(targetMs) ? targetMs : playbackPositionMs();
        seekTrackProgress.setProgress((int) (((float) shownMs / duration) * 1000));
        tvTimeCurrent.setText(formatMs(shownMs));
        updateFsProgress(shownMs, duration);
    }

    // ==========================================
    // CONTROLES ESTILO YOUTUBE MUSIC (iguales a la web)
    // ==========================================

    /**
     * Tamaños de los controles como la web: con el explorador abierto play 76 / anterior-siguiente 58 /
     * aleatorio-repetir 52dp; con el explorador oculto 88 / 66 / 58dp. Íconos: 34-38 / 36-40 / 26-28dp.
     */
    private void applyPlayerControlSizes(boolean expanded) {
        sizeControl(btnPlayPause, expanded ? 88 : 76, expanded ? 38 : 34);
        sizeControl(btnPrev, expanded ? 66 : 58, expanded ? 40 : 36);
        sizeControl(btnNext, expanded ? 66 : 58, expanded ? 40 : 36);
        sizeControl(btnShuffle, expanded ? 58 : 52, expanded ? 28 : 26);
        sizeControl(btnRepeat, expanded ? 58 : 52, expanded ? 28 : 26);
        updateSecondaryButton(btnShuffle, isShuffle);
        updateSecondaryButton(btnRepeat, isRepeat);

        // Con el explorador oculto, tarjetas, barra y controles no pasan de 540dp (centrados), como la web
        int maxWidth = expanded ? dp(540) : ViewGroup.LayoutParams.MATCH_PARENT;
        for (int id : new int[]{R.id.layoutAdjacentPreviews, R.id.layoutProgressBlock, R.id.layoutTransportControls}) {
            View v = findViewById(id);
            if (v == null) continue;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            int width = maxWidth;
            if (expanded && v.getParent() instanceof View) {
                View parent = (View) v.getParent();
                int available = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
                if (available > 0) width = Math.min(maxWidth, available);
            }
            if (lp.width != width) {
                lp.width = width;
                v.setLayoutParams(lp);
            }
        }
    }

    private void sizeControl(View button, int sizeDp, int iconDp) {
        if (button == null) return;
        ViewGroup.LayoutParams lp = button.getLayoutParams();
        int size = dp(sizeDp);
        if (lp.width != size || lp.height != size) {
            lp.width = size;
            lp.height = size;
            button.setLayoutParams(lp);
        }
        int pad = (size - dp(iconDp)) / 2;
        button.setPadding(pad, pad, pad, pad);
    }

    /**
     * Aleatorio / Repetir: ícono blanco (85 %) apagado; cian con un puntito de 6dp debajo encendido,
     * igual que la web. El puntito es el fondo del botón (se rearma con su tamaño).
     */
    private void updateSecondaryButton(ImageButton button, boolean active) {
        if (button == null) return;
        button.setSelected(active);
        button.setColorFilter(active ? Color.parseColor("#38bdf8") : Color.argb(217, 255, 255, 255), PorterDuff.Mode.SRC_IN);
        int size = button.getLayoutParams() != null && button.getLayoutParams().width > 0
                ? button.getLayoutParams().width : dp(52);
        if (!active) {
            button.setBackgroundResource(R.drawable.bg_ctrl_flat);
            return;
        }
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Color.parseColor("#38bdf8"));
        int dotSize = dp(6);
        int side = (size - dotSize) / 2;
        int bottom = dp(4);
        LayerDrawable bg = new LayerDrawable(new Drawable[]{dot});
        bg.setLayerInset(0, side, size - bottom - dotSize, side, bottom);
        button.setBackground(bg);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updatePlaybackStateUI() {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_ctrl_pause);
            tvPlaybackStatus.setText(autoMixer.isEnabled() ? "● REPRODUCIENDO · AUTOMIX" : "● REPRODUCIENDO");
            tvPlaybackStatus.setTextColor(Color.parseColor("#38bdf8"));
            if (nowPlayingPill != null) nowPlayingPill.setPlaying(true);
            if (laserRing != null) laserRing.setPlaying(true);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_ctrl_play);
            tvPlaybackStatus.setText("● EN PAUSA");
            tvPlaybackStatus.setTextColor(Color.parseColor("#f59e0b"));
            if (nowPlayingPill != null) nowPlayingPill.setPlaying(false);
            if (laserRing != null) laserRing.setPlaying(false);
        }
        // Activar marquesina en el título
        tvTrackTitle.setSelected(isPlaying);
        updateFsPlayState();
        // Punto de Mi Mix en la pestaña: late si suena, quieto en pausa
        refreshTabLabels();
    }

    // ==========================================
    // GESTOS ERGONÓMICOS DEL EXPLORADOR PARA EL CONDUCTOR
    // Botón de lupa (click conmuta) y zona libre roja lateral (jalar a la izq oculta, a la der muestra; toques ignorados)
    // ==========================================
    private void setupExplorerToggleGestures() {
        // La lupa está dentro de la zona de gestos: no es clicable por sí misma para que el gesto
        // de jalar funcione aunque empiece sobre ella; el toque se detecta en la zona.
        if (btnToggleExplorer != null) {
            btnToggleExplorer.setClickable(false);
            btnToggleExplorer.setFocusable(false);
        }
        // Automix está arriba a la derecha, fuera de la zona de gestos: botón normal
        if (btnAutoMix != null) {
            btnAutoMix.setOnClickListener(v -> toggleAutoMix());
        }
        // JH VIDEO: sin setupAlbumArtAutoSize(); el cuadro del video siempre es 16:9 del ancho de la columna
        applyPlayerControlSizes(false);

        if (layoutExplorerToggleZone != null) {
            // NUNCA asignar setOnClickListener a layoutExplorerToggleZone para evitar alternar con un simple toque accidental
            layoutExplorerToggleZone.setOnTouchListener(new View.OnTouchListener() {
                private float downX, downY;
                private boolean gestureHandled = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = event.getRawX();
                            downY = event.getRawY();
                            gestureHandled = false;
                            if (btnToggleExplorer != null && isTouchOnView(btnToggleExplorer, downX, downY)) {
                                btnToggleExplorer.setPressed(true);
                            }
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            if (!gestureHandled && layoutExplorerPanel != null) {
                                float dx = event.getRawX() - downX;
                                float dy = event.getRawY() - downY;
                                if (Math.abs(dx) > 35 && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                                    // Jalar hacia la IZQUIERDA (<-) oculta el explorador
                                    if (dx < -35 && layoutExplorerPanel.getVisibility() == View.VISIBLE) {
                                        gestureHandled = true;
                                        hideKeyboard();
                                        setExplorerVisible(false);
                                        return true;
                                    }
                                    // Jalar hacia la DERECHA (->) muestra el explorador
                                    else if (dx > 35 && layoutExplorerPanel.getVisibility() != View.VISIBLE) {
                                        gestureHandled = true;
                                        setExplorerVisible(true);
                                        return true;
                                    }
                                }
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                            if (btnToggleExplorer != null) btnToggleExplorer.setPressed(false);
                            if (!gestureHandled && layoutExplorerPanel != null) {
                                float totalDx = event.getRawX() - downX;
                                float totalDy = event.getRawY() - downY;
                                if (Math.abs(totalDx) > 35 && Math.abs(totalDx) > Math.abs(totalDy) * 1.1f) {
                                    if (totalDx < -35 && layoutExplorerPanel.getVisibility() == View.VISIBLE) {
                                        hideKeyboard();
                                        setExplorerVisible(false);
                                    } else if (totalDx > 35 && layoutExplorerPanel.getVisibility() != View.VISIBLE) {
                                        setExplorerVisible(true);
                                    }
                                } else if (Math.abs(totalDx) <= 35 && Math.abs(totalDy) <= 35
                                        && btnToggleExplorer != null && isTouchOnView(btnToggleExplorer, downX, downY)) {
                                    // Toque simple sobre la lupa: alterna el explorador
                                    toggleExplorerVisibility();
                                }
                                // Un toque simple en el resto de la zona libre NO hace nada (evita toques accidentales).
                            }
                            return true;

                        case MotionEvent.ACTION_CANCEL:
                            if (btnToggleExplorer != null) btnToggleExplorer.setPressed(false);
                            gestureHandled = false;
                            break;
                    }
                    return true;
                }
            });
        }
    }

    /**
     * Agranda el cuadro de la carátula para ocupar el espacio libre que queda sobre los botones
     * (sin esto, el espacio sobrante quedaba entre la barra de progreso y los botones).
     * Se calcula una vez, ya con la pantalla real del radio dibujada.
     */
    private void setupAlbumArtAutoSize() {
        final View controls = findViewById(R.id.layoutTransportControls);
        if (controls == null || layoutAlbumArtCard == null) return;
        controls.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!(controls instanceof ViewGroup) || controls.getHeight() == 0) return;
                int rowHeight = 0;
                ViewGroup row = (ViewGroup) controls;
                for (int i = 0; i < row.getChildCount(); i++) {
                    rowHeight = Math.max(rowHeight, row.getChildAt(i).getHeight());
                }
                if (rowHeight == 0) return;
                controls.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                float density = getResources().getDisplayMetrics().density;
                // Dejar una separación cómoda (8 dp arriba y abajo de los botones). La carátula se agranda con lo
                // que sobra: como el título deja 19 dp para la cápsula PLAYING que sobresale, la carátula queda
                // 19 dp más chica y el título y lo de abajo no se mueven
                int spare = controls.getHeight() - rowHeight - Math.round(16 * density);
                ViewGroup.LayoutParams lp = layoutAlbumArtCard.getLayoutParams();
                View parent = (View) layoutAlbumArtCard.getParent();
                // Que no choque con la lupa (izquierda) ni con Automix (derecha), ni pase de 260 dp
                int maxByWidth = parent.getWidth() - 2 * Math.round(66 * density);
                int size = Math.min(lp.height + Math.max(0, spare), Math.min(maxByWidth, Math.round(260 * density)));
                if (size > lp.height) {
                    lp.width = size;
                    lp.height = size;
                    layoutAlbumArtCard.setLayoutParams(lp);
                }
            }
        });
    }

    private boolean isTouchOnView(View view, float rawX, float rawY) {
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) return false;
        rect.inset(-10, -10);
        return rect.contains((int) rawX, (int) rawY);
    }

    private void toggleExplorerVisibility() {
        hideKeyboard();
        if (layoutExplorerPanel != null) {
            setExplorerVisible(layoutExplorerPanel.getVisibility() != View.VISIBLE);
        }
    }

    /** Muestra u oculta el explorador; igual que la web, la lupa queda resaltada mientras está oculto. */
    private void setExplorerVisible(boolean visible) {
        if (layoutExplorerPanel == null) return;
        layoutExplorerPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (btnToggleExplorer != null) btnToggleExplorer.setSelected(!visible);
        // Controles más grandes con el explorador oculto (como la web); después del nuevo layout
        applyPlayerControlSizes(!visible);
        if (layoutPlayerPanel != null) layoutPlayerPanel.post(() -> applyPlayerControlSizes(!visible));
    }

    // ==========================================
    // EXPLORADOR Y BÚSQUEDA INTELIGENTE
    // ==========================================
    private void setupExplorerControls() {
        tabFolders.setOnClickListener(v -> {
            hideKeyboard();
            selectTab(0);
        });
        tabTracks.setOnClickListener(v -> {
            hideKeyboard();
            selectTab(1);
        });
        tabMyMix.setOnClickListener(v -> {
            hideKeyboard();
            selectTab(2);
        });

        // Listeners de Chips de Filtro (Esta Carpeta, TODO, USB 1, USB 2)
        if (btnFilterFolder != null) {
            btnFilterFolder.setOnClickListener(v -> {
                hideKeyboard();
                setSearchFilter(FILTER_FOLDER);
                // Tocar la carpeta actual también lleva la lista a la canción que suena (1 antes arriba).
                // Con búsqueda, después de que lleguen los resultados
                if (etSearch != null && etSearch.getText().length() > 0) {
                    scrollToPlayingAfterSearch = true;
                } else {
                    scrollToPlayingTrack();
                }
            });
        }
        if (btnFilterAll != null) {
            btnFilterAll.setOnClickListener(v -> {
                hideKeyboard();
                setSearchFilter(FILTER_ALL);
            });
        }
        if (btnFilterUsb1 != null) {
            btnFilterUsb1.setOnClickListener(v -> {
                hideKeyboard();
                setSearchFilter(FILTER_USB1);
            });
        }
        if (btnFilterUsb2 != null) {
            btnFilterUsb2.setOnClickListener(v -> {
                hideKeyboard();
                setSearchFilter(FILTER_USB2);
            });
        }

        btnRescanUsb.setOnClickListener(v -> {
            hideKeyboard();
            Toast.makeText(this, "Escaneando pendrives USB...", Toast.LENGTH_SHORT).show();
            startUsbScan();
        });

        btnPlayAll.setOnClickListener(v -> {
            hideKeyboard();
            // Solo visible en Pistas: reproduce la lista que se está mostrando (Mi Mix tiene su propio Reproducir)
            if (!displayedTracks.isEmpty()) {
                playTrack(0);
            } else if (!currentPlaylist.isEmpty()) {
                displayedTracks.clear();
                displayedTracks.addAll(currentPlaylist);
                showDisplayedTracks();
                playTrack(0);
            }
        });

        // Búsqueda inteligente optimizada: debounce adaptativo (500ms con 1-2 letras, 250ms después)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String q = s.toString();
                btnSearchClear.setVisibility(q.trim().length() > 0 ? View.VISIBLE : View.GONE);

                // Cancelar búsqueda anterior encolada
                if (searchRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchRunnable);
                }
                searchVersion++;
                final int thisVersion = searchVersion;

                if (isReorderingMix) {
                    isReorderingMix = false;
                    if (recyclerMixContent != null) recyclerMixContent.setVisibility(View.GONE);
                    if (listViewContent != null) listViewContent.setVisibility(View.VISIBLE);
                }

                if (q.trim().isEmpty()) {
                    if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
                    if (listViewContent != null) {
                        listViewContent.setAlpha(1.0f);
                        listViewContent.setEnabled(true);
                    }
                    // Se conserva el chip activo; con el campo vacío Pistas muestra la carpeta activa
                    filterSearch("");
                    return;
                }

                // Mostrar de inmediato que está buscando: lista atenuada/borrosa y overlay central con spinner
                if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);
                if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.VISIBLE);
                if (listViewContent != null) {
                    listViewContent.setAlpha(0.25f);
                    listViewContent.setEnabled(false);
                }
                if (layoutFastScrollerTrack != null) {
                    layoutFastScrollerTrack.setVisibility(View.GONE);
                }
                tvCurrentPath.setText("⏳ Buscando: \"" + q + "\"...");

                // Esperar a que el usuario termine de teclear: con 1-2 letras casi todo coincide y
                // seguramente seguirá escribiendo, así que se espera más (500ms); si no, 250ms
                int debounceMs = q.trim().length() <= 2 ? 500 : 250;
                searchRunnable = () -> performAsyncSearch(q, thisVersion);
                searchDebounceHandler.postDelayed(searchRunnable, debounceMs);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearchClear.setOnClickListener(v -> {
            if (searchRunnable != null) {
                searchDebounceHandler.removeCallbacks(searchRunnable);
            }
            searchVersion++;
            etSearch.setText("");
            btnSearchClear.setVisibility(View.GONE);
            if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
            if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
            if (listViewContent != null) {
                listViewContent.setAlpha(1.0f);
                listViewContent.setEnabled(true);
            }
            hideKeyboard();
            // Al borrar la búsqueda se conserva el chip activo y se muestran las canciones de la carpeta activa
            updateFilterChipsVisibility();
            updateFilterChipsUI();
            filterSearch("");
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (searchRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchRunnable);
                }
                searchVersion++;
                performAsyncSearch(etSearch.getText().toString(), searchVersion);
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    /**
     * En el radio la carpeta de montaje (/storage/udisk2...) sigue existiendo vacía aunque se quite
     * el pendrive: solo se considera conectado si está montado y tiene contenido.
     */
    private static boolean isUsbMounted(String[] mountPaths) {
        for (String p : mountPaths) {
            File[] entries = new File(p).listFiles();
            if (entries != null && entries.length > 0) return true;
        }
        return false;
    }

    private void checkUsbPresence() {
        hasUsb1 = isUsbMounted(USB1_MOUNT_PATHS);
        hasUsb2 = isUsbMounted(USB2_MOUNT_PATHS);
        for (Track t : allTracks) {
            if ("USB 1".equalsIgnoreCase(t.getUsbSource()) || (t.getFilePath() != null && (t.getFilePath().contains("udisk1") || t.getFilePath().contains("usb0")))) {
                hasUsb1 = true;
            }
            if ("USB 2".equalsIgnoreCase(t.getUsbSource()) || (t.getFilePath() != null && (t.getFilePath().contains("udisk2") || t.getFilePath().contains("usb1")))) {
                hasUsb2 = true;
            }
        }
        updateFilterChipsVisibility();
    }

    private void setSearchFilter(int filter) {
        currentSearchFilter = filter;
        updateFilterChipsUI();
        if (etSearch != null && etSearch.getText().length() > 0) {
            filterSearch(etSearch.getText().toString());
        }
    }

    private void updateFilterChipsUI() {
        if (btnFilterFolder != null) {
            boolean sel = (currentSearchFilter == FILTER_FOLDER);
            btnFilterFolder.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            int color = sel ? Color.parseColor("#0d0f14") : Color.parseColor("#f1f5f9");
            btnFilterFolder.setTextColor(color);
            btnFilterFolder.setTypeface(null, Typeface.BOLD);
            if (currentSelectedFolder != null) {
                String usbNum = (currentSelectedFolderUsbSource != null && currentSelectedFolderUsbSource.contains("2")) ? "2" : (currentSelectedFolderUsbSource != null && currentSelectedFolderUsbSource.contains("1") ? "1" : "");
                String label = currentSelectedFolder.length() > 16 ? currentSelectedFolder.substring(0, 14) + "…" : currentSelectedFolder;
                btnFilterFolder.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                btnFilterFolder.setCompoundDrawablePadding(0);
                btnFilterFolder.setText(buildFolderFilterText(label, usbNum, color));
            }
        }

        if (btnFilterAll != null) {
            boolean sel = (currentSearchFilter == FILTER_ALL);
            btnFilterAll.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            btnFilterAll.setTextColor(sel ? Color.parseColor("#0d0f14") : Color.parseColor("#f1f5f9"));
            btnFilterAll.setTypeface(null, Typeface.BOLD);
        }

        if (btnFilterUsb1 != null) {
            boolean sel = (currentSearchFilter == FILTER_USB1);
            btnFilterUsb1.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            int color = sel ? Color.parseColor("#0d0f14") : Color.parseColor("#f1f5f9");
            btnFilterUsb1.setTextColor(color);
            btnFilterUsb1.setTypeface(null, Typeface.BOLD);
            Drawable[] drawables = btnFilterUsb1.getCompoundDrawables();
            if (drawables.length > 0 && drawables[0] != null) {
                drawables[0].mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }

        if (btnFilterUsb2 != null) {
            boolean sel = (currentSearchFilter == FILTER_USB2);
            btnFilterUsb2.setBackgroundResource(sel ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            int color = sel ? Color.parseColor("#0d0f14") : Color.parseColor("#f1f5f9");
            btnFilterUsb2.setTextColor(color);
            btnFilterUsb2.setTypeface(null, Typeface.BOLD);
            Drawable[] drawables = btnFilterUsb2.getCompoundDrawables();
            if (drawables.length > 0 && drawables[0] != null) {
                drawables[0].mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private CharSequence buildFolderFilterText(String label, String usbNum, int textColor) {
        if (usbNum == null || usbNum.isEmpty()) {
            return "📁 " + label;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append("📁  ");
        int start = ssb.length();
        ssb.append(" ");
        try {
            Drawable d = getDrawable(R.drawable.ic_usb_trident);
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                int size = (int) (14 * getResources().getDisplayMetrics().density);
                d.setBounds(0, 0, size, size);
                ssb.setSpan(new CenteredImageSpan(d), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {}
        ssb.append(" ").append(usbNum).append(" | ").append(label);
        return ssb;
    }

    /**
     * Pestañas como la web: elegida en cian lleno con texto e ícono oscuros; las demás translúcidas con
     * texto e ícono gris claro. Íconos: carpeta, lista y lista con + (los mismos dibujos que la web).
     */
    private void refreshTabLabels() {
        if (tabFolders == null) return;
        setTabLabel(tabFolders, R.drawable.ic_tab_folder, "Carpetas USB", currentTab == 0);
        setTabLabel(tabTracks, R.drawable.ic_tab_list, "Videos (" + currentPlaylist.size() + ")", currentTab == 1);
        setTabLabel(tabMyMix, R.drawable.ic_tab_mymix, "Mi Lista (" + (myMixLoaded ? mixTracks().size() : 0) + ")", currentTab == 2);
        addMixLiveDot();
    }

    // Punto de la pestaña Mi Mix mientras suena Mi Mix (opción C): late mientras suena, quieto en pausa.
    // Grande y sin brillo difuso, para que se vea nítido en la pantalla del radio
    private GradientDrawable mixLiveDot;
    private long mixLiveDotStartMs;
    private final Runnable mixLiveDotPulse = new Runnable() {
        @Override
        public void run() {
            if (mixLiveDot == null || !playingFromMix || !isPlaying) return;
            float t = ((SystemClock.uptimeMillis() - mixLiveDotStartMs) % 1200) / 1200f;
            float wave = (float) (0.5 + 0.5 * Math.cos(2 * Math.PI * t)); // 1 → 0 → 1
            mixLiveDot.setAlpha(Math.round(255 * (0.3f + 0.7f * wave)));
            if (tabMyMix != null) tabMyMix.invalidate();
            handler.postDelayed(this, 60);
        }
    };

    private void addMixLiveDot() {
        handler.removeCallbacks(mixLiveDotPulse);
        mixLiveDot = null;
        if (tabMyMix == null || !playingFromMix) return;
        mixLiveDot = new GradientDrawable();
        mixLiveDot.setShape(GradientDrawable.OVAL);
        mixLiveDot.setColor(Color.parseColor(currentTab == 2 ? "#0c0e12" : "#38bdf8"));
        mixLiveDot.setBounds(0, 0, dp(11), dp(11));
        SpannableStringBuilder ssb = new SpannableStringBuilder(tabMyMix.getText());
        ssb.append("  ");
        int start = ssb.length();
        ssb.append(" ");
        ssb.setSpan(new CenteredImageSpan(mixLiveDot), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tabMyMix.setText(ssb);
        if (isPlaying) {
            mixLiveDotStartMs = SystemClock.uptimeMillis();
            handler.post(mixLiveDotPulse);
        }
    }

    /**
     * Debajo del título: la carpeta de la canción, o "Mi Mix · 2 de 4" si lo que suena es Mi Mix
     * (así se sabe que Siguiente va a la siguiente de Mi Mix y cuánto falta). Opción 2.
     */
    private void updateTrackSource(Track track) {
        if (tvTrackAlbum == null || track == null) return;
        if (!playingFromMix) {
            tvTrackAlbum.setText(track.getAlbum());
            return;
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
        try {
            Drawable icon = getDrawable(R.drawable.ic_tab_mymix);
            if (icon != null) {
                icon = icon.mutate();
                icon.setColorFilter(Color.parseColor("#cbd5e1"), PorterDuff.Mode.SRC_IN);
                icon.setBounds(0, 0, dp(17), dp(17));
                ssb.setSpan(new CenteredImageSpan(icon), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {}
        ssb.append(" ");
        int start = ssb.length();
        ssb.append("Mi Lista");
        ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#cbd5e1")), start, ssb.length(), 0);
        ssb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, ssb.length(), 0);
        start = ssb.length();
        ssb.append(" · ").append(String.valueOf(currentTrackIndex + 1)).append(" de ").append(String.valueOf(getQueue().size()));
        ssb.setSpan(new ForegroundColorSpan(Color.parseColor("#94a3b8")), start, ssb.length(), 0);
        ssb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, ssb.length(), 0);
        tvTrackAlbum.setText(ssb);
    }

    private void setTabLabel(Button tab, int iconRes, String text, boolean selected) {
        if (tab == null) return;
        int color = selected ? Color.parseColor("#0c0e12") : Color.parseColor("#cbd5e1");
        SpannableStringBuilder ssb = new SpannableStringBuilder(" ");
        try {
            Drawable icon = getDrawable(iconRes);
            if (icon != null) {
                icon = icon.mutate();
                icon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                icon.setBounds(0, 0, dp(18), dp(18));
                ssb.setSpan(new CenteredImageSpan(icon), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {}
        ssb.append("  ").append(text);
        tab.setText(ssb);
        tab.setTextColor(color);
        tab.setTypeface(null, Typeface.BOLD);
        tab.setBackgroundResource(selected ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
    }

    private static class CenteredImageSpan extends ImageSpan {
        public CenteredImageSpan(Drawable drawable) {
            super(drawable);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            Drawable b = getDrawable();
            canvas.save();
            int transY = top + (bottom - top) / 2 - b.getBounds().height() / 2;
            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            Drawable d = getDrawable();
            Rect rect = d.getBounds();
            if (fm != null) {
                Paint.FontMetricsInt pfm = paint.getFontMetricsInt();
                fm.ascent = pfm.ascent;
                fm.descent = pfm.descent;
                fm.top = pfm.top;
                fm.bottom = pfm.bottom;
            }
            return rect.right;
        }
    }

    private void updateFilterChipsVisibility() {
        // La barra de filtros solo tiene utilidad en la pestaña Pistas (currentTab == 1).
        // En Carpetas USB (0) el árbol organiza los pendrives, y en Mi Mix (2) se muestra la lista completa.
        if (scrollSearchFilters != null) {
            scrollSearchFilters.setVisibility(currentTab == 1 ? View.VISIBLE : View.GONE);
        }
        if (currentTab != 1) {
            return;
        }

        if (btnFilterFolder != null) {
            if (currentTab == 1 && currentSelectedFolder != null) {
                btnFilterFolder.setVisibility(View.VISIBLE);
                boolean sel = (currentSearchFilter == FILTER_FOLDER);
                int color = sel ? Color.parseColor("#0d0f14") : Color.parseColor("#f1f5f9");
                String usbNum = (currentSelectedFolderUsbSource != null && currentSelectedFolderUsbSource.contains("2")) ? "2" : (currentSelectedFolderUsbSource != null && currentSelectedFolderUsbSource.contains("1") ? "1" : "");
                String label = currentSelectedFolder.length() > 16 ? currentSelectedFolder.substring(0, 14) + "…" : currentSelectedFolder;
                btnFilterFolder.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                btnFilterFolder.setCompoundDrawablePadding(0);
                btnFilterFolder.setText(buildFolderFilterText(label, usbNum, color));
            } else {
                btnFilterFolder.setVisibility(View.GONE);
            }
        }

        if (btnFilterAll != null) {
            btnFilterAll.setVisibility(View.VISIBLE);
        }

        boolean multipleUsb = (hasUsb1 && hasUsb2);

        // Si solo hay un pendrive conectado, TODO ya hace referencia a ese único USB,
        // por lo que los chips individuales USB 1 y USB 2 solo se muestran si ambos están presentes
        if (btnFilterUsb1 != null) {
            btnFilterUsb1.setVisibility(multipleUsb ? View.VISIBLE : View.GONE);
        }

        if (btnFilterUsb2 != null) {
            btnFilterUsb2.setVisibility(multipleUsb ? View.VISIBLE : View.GONE);
        }

        if (!multipleUsb && (currentSearchFilter == FILTER_USB1 || currentSearchFilter == FILTER_USB2)) {
            currentSearchFilter = FILTER_ALL;
        }

        updateFilterChipsUI();
    }

    private void selectTab(int tabIndex) {
        hideKeyboard();
        // Recordar la búsqueda de Pistas (texto + chip) para recuperarla al volver a la pestaña
        if (currentTab == 1 && etSearch != null) {
            savedTracksQuery = etSearch.getText().toString();
            savedTracksFilter = currentSearchFilter;
        }
        boolean restoreTracksSearch = tabIndex == 1 && !savedTracksQuery.trim().isEmpty();

        if (searchRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchRunnable);
        }
        searchVersion++;
        if (etSearch != null && etSearch.getText().length() > 0) {
            etSearch.setText("");
        }
        if (btnSearchClear != null) {
            btnSearchClear.setVisibility(View.GONE);
        }
        if (pbSearchLoading != null) {
            pbSearchLoading.setVisibility(View.GONE);
        }
        if (layoutSearchLoading != null) {
            layoutSearchLoading.setVisibility(View.GONE);
        }
        if (listViewContent != null) {
            listViewContent.setAlpha(1.0f);
            listViewContent.setEnabled(true);
        }

        currentTab = tabIndex;
        if (btnPlayAll != null) {
            // En Carpetas USB cada carpeta tiene su propio botón [N ▶]
            // En Mi Mix, Reproducir está en su cabecera
            btnPlayAll.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        }
        if (etSearch != null) {
            // El texto de ayuda indica qué busca cada pestaña
            etSearch.setHint(tabIndex == 0 ? "Buscar carpeta..." :
                    (tabIndex == 1 ? "Buscar video, artista o carpeta..." : "Buscar en Mi Lista..."));
        }
        refreshTabLabels();

        // Mi Mix: cabecera propia (Reproducir / Vaciar), filas numeradas por la lista y botón ✕
        mixConfirmClear = false;
        if (tabIndex != 2) {
            isReorderingMix = false;
            trackAdapter.setReorderMode(false);
            if (recyclerMixContent != null) recyclerMixContent.setVisibility(View.GONE);
            if (listViewContent != null) listViewContent.setVisibility(View.VISIBLE);
        }
        trackAdapter.setMixMode(tabIndex == 2);
        if (layoutMixHeader != null) layoutMixHeader.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        if (tvMixEmpty != null && tabIndex != 2) tvMixEmpty.setVisibility(View.GONE);

        if (tabIndex == 0) {
            if (etSearch.getText().toString().trim().isEmpty()) {
                listViewContent.setAdapter(folderTreeAdapter);
                if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.VISIBLE);
                updateVisibleTreeNodes();
            } else {
                listViewContent.setAdapter(folderAdapter);
                if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
            }
            if (currentSearchFilter == FILTER_FOLDER) {
                currentSearchFilter = FILTER_ALL;
            }
        } else if (tabIndex == 1) {
            if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
            listViewContent.setAdapter(trackAdapter);
            String usbTag = (currentSelectedFolderUsbSource != null && !currentSelectedFolderUsbSource.isEmpty()) ? "[" + currentSelectedFolderUsbSource + "] " : "";
            tvCurrentPath.setText(currentSelectedFolder != null ? "📁 " + usbTag + currentSelectedFolder : "🎵 Todas las Pistas");
            if (restoreTracksSearch) {
                // Hay una búsqueda pendiente: conservar el chip con el que se estaba buscando
                currentSearchFilter = savedTracksFilter;
            } else if (currentSelectedFolder != null) {
                currentSearchFilter = FILTER_FOLDER;
            } else if (currentSearchFilter == FILTER_FOLDER) {
                currentSearchFilter = FILTER_ALL;
            }
        } else {
            if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
            listViewContent.setAdapter(trackAdapter);
            tvCurrentPath.setText("🎧 Mi Lista");
            if (currentSearchFilter == FILTER_FOLDER) {
                currentSearchFilter = FILTER_ALL;
            }
        }

        updateFilterChipsVisibility();
        if (restoreTracksSearch) {
            etSearch.setText(savedTracksQuery);
            etSearch.setSelection(savedTracksQuery.length());
            // Relanzar la búsqueda de inmediato, sin esperar el debounce del TextWatcher
            if (searchRunnable != null) {
                searchDebounceHandler.removeCallbacks(searchRunnable);
            }
            searchVersion++;
            performAsyncSearch(savedTracksQuery, searchVersion);
        } else {
            filterByTab();
            // Al volver a Pistas o Mi Mix: la lista va a la canción que está sonando (si está en esa lista)
            if (tabIndex != 0) scrollToPlayingTrack();
        }
    }

    private boolean scrollToPlayingAfterSearch = false;

    /**
     * Lleva la lista a la canción que suena, si está en la lista mostrada: arriba queda una canción
     * antes, luego la que suena y luego las demás. Al volver a la pestaña o al tocar la carpeta actual.
     */
    private void scrollToPlayingTrack() {
        // Mi Mix se muestra en su RecyclerView (desde 2.7.0); Pistas y búsquedas, en el ListView
        boolean inMixList = recyclerMixContent != null && recyclerMixContent.getVisibility() == View.VISIBLE;
        if (!inMixList && (listViewContent == null || listViewContent.getAdapter() != trackAdapter)) return;
        Track playing = getCurrentTrack();
        if (playing == null) return;
        int index = -1;
        for (int i = 0; i < displayedTracks.size(); i++) {
            if (displayedTracks.get(i).getFilePath().equals(playing.getFilePath())) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        final int target = Math.max(0, index - 1);
        if (inMixList) {
            final LinearLayoutManager lm = (LinearLayoutManager) recyclerMixContent.getLayoutManager();
            if (lm != null) recyclerMixContent.post(() -> lm.scrollToPositionWithOffset(target, 0));
            return;
        }
        listViewContent.post(() -> listViewContent.setSelectionFromTop(target, 0));
    }

    private void openFolder(String folderName, String usbSource) {
        currentSelectedFolder = folderName;
        currentSelectedFolderUsbSource = usbSource;
        if (currentSelectedFolderUsbSource == null || currentSelectedFolderUsbSource.isEmpty()) {
            for (Track t : allTracks) {
                if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(folderName)) {
                    currentSelectedFolderUsbSource = t.getUsbSource();
                    break;
                }
            }
        }
        currentPlaylist.clear();
        for (Track t : allTracks) {
            if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(folderName)) {
                if (isTrackFromUsb(t, currentSelectedFolderUsbSource)) {
                    currentPlaylist.add(t);
                }
            }
        }

        currentSearchFilter = FILTER_FOLDER;
        savedTracksQuery = ""; // Se eligió otra carpeta: la búsqueda anterior de Pistas ya no aplica
        selectTab(1);
        String usbTag = (currentSelectedFolderUsbSource != null && !currentSelectedFolderUsbSource.isEmpty()) ? "[" + currentSelectedFolderUsbSource + "] " : "";
        tvCurrentPath.setText("📁 " + usbTag + folderName);
        monitorShownText = null;
        updateLoadMonitor();
    }

    private void openFolder(String folderName) {
        openFolder(folderName, null);
    }

    /** Canciones de una carpeta de los resultados de búsqueda (las mismas que abre openFolder). */
    private List<Track> tracksOfFolder(String folderName, String usbSource) {
        String usb = usbSource;
        if (usb == null || usb.isEmpty()) {
            for (Track t : allTracks) {
                if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(folderName)) {
                    usb = t.getUsbSource();
                    break;
                }
            }
        }
        List<Track> list = new ArrayList<>();
        for (Track t : allTracks) {
            if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(folderName) && isTrackFromUsb(t, usb)) {
                list.add(t);
            }
        }
        return list;
    }

    // ========================================================
    // ÁRBOL JERÁRQUICO DE CARPETAS USB (Estilo Web Maqueta Hernan)
    // ========================================================
    private void buildTreeFromTracks(List<Track> tracks) {
        rootTreeNodes.clear();

        if (tracks == null || tracks.isEmpty()) {
            // JH VIDEO: sin carpetas de ejemplo; solo los pendrives conectados, vacíos
            if (hasUsb1) rootTreeNodes.add(new TreeNode("usb1", "USB 1 (udisk1)", "/storage/udisk1/", "USB 1", true, 0));
            if (hasUsb2) rootTreeNodes.add(new TreeNode("usb2", "USB 2 (udisk2)", "/storage/udisk2/", "USB 2", true, 0));
            return;
        }

        Map<String, TreeNode> rootUsbMap = new LinkedHashMap<>();

        for (Track track : tracks) {
            String path = track.getFilePath() != null ? track.getFilePath() : track.getPath();
            String usb = track.getUsbSource();
            if (usb == null || usb.isEmpty()) {
                usb = (path != null && path.toLowerCase().contains("udisk2")) ? "USB 2" : "USB 1";
            }
            boolean isUsb2 = "USB 2".equalsIgnoreCase(usb) || (path != null && (path.toLowerCase().contains("udisk2") || path.toLowerCase().contains("usb1")));
            String usbId = isUsb2 ? "usb2" : "usb1";
            String usbName = isUsb2 ? "USB 2 (udisk2)" : "USB 1 (udisk1)";
            String usbPath = isUsb2 ? "/storage/udisk2/" : "/storage/udisk1/";

            TreeNode rootNode = rootUsbMap.get(usbId);
            if (rootNode == null) {
                rootNode = new TreeNode(usbId, usbName, usbPath, isUsb2 ? "USB 2" : "USB 1", true, 0);
                rootUsbMap.put(usbId, rootNode);
            }
            rootNode.incrementTotalCount();

            // Extraer ruta relativa de directorios (sin el nombre del archivo mp3)
            String relDir = "";
            if (path != null) {
                // Pendrive montado con otro nombre (p. ej. /storage/1A2B-3C4D): la ruta va desde su raíz
                String scanRoot = otherScanRootOf(path);
                if (scanRoot != null) {
                    relDir = path.substring(scanRoot.length() + 1);
                } else {
                    String lower = path.toLowerCase();
                    String[] usbPatterns = {"udisk2", "udisk1", "usb1", "usb0", "udisk3", "usb2"};
                    for (String pat : usbPatterns) {
                        int idx = lower.indexOf(pat);
                        if (idx != -1) {
                            int slashAfter = path.indexOf('/', idx);
                            if (slashAfter != -1 && slashAfter < path.length() - 1) {
                                relDir = path.substring(slashAfter + 1);
                            }
                            break;
                        }
                    }
                    if (relDir.isEmpty() && path.startsWith(usbPath)) {
                        relDir = path.substring(usbPath.length());
                    }
                }
            }

            int lastSlash = relDir.lastIndexOf('/');
            if (lastSlash != -1) {
                relDir = relDir.substring(0, lastSlash);
            } else {
                relDir = "";
            }

            if (!relDir.trim().isEmpty()) {
                String[] parts = relDir.split("/+");
                TreeNode currentNode = rootNode;
                String accumPath = usbPath;

                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim();
                    if (p.isEmpty()) continue;
                    accumPath += p + "/";

                    TreeNode child = currentNode.findChildByName(p);
                    if (child == null) {
                        String childId = usbId + "_" + p.replaceAll("[^a-zA-Z0-9]", "_") + "_" + i;
                        child = new TreeNode(childId, p, accumPath, currentNode.getUsbSource(), false, 0);
                        currentNode.addChild(child);
                    }
                    child.incrementTotalCount();
                    if (i == parts.length - 1) {
                        child.addDirectTrack(track);
                    }
                    currentNode = child;
                }
            } else {
                rootNode.addDirectTrack(track);
            }
        }

        if (hasUsb1 && !rootUsbMap.containsKey("usb1")) {
            rootUsbMap.put("usb1", new TreeNode("usb1", "USB 1 (udisk1)", "/storage/udisk1/", "USB 1", true, 0));
        }
        if (hasUsb2 && !rootUsbMap.containsKey("usb2")) {
            rootUsbMap.put("usb2", new TreeNode("usb2", "USB 2 (udisk2)", "/storage/udisk2/", "USB 2", true, 0));
        }

        for (TreeNode root : rootUsbMap.values()) {
            sortNodeChildren(root);
            rootTreeNodes.add(root);
        }
    }

    private void sortNodeChildren(TreeNode node) {
        if (node == null || !node.hasChildren()) return;
        Collections.sort(node.getChildren(), (c1, c2) -> {
            String n1 = c1.getName() != null ? c1.getName() : "";
            String n2 = c2.getName() != null ? c2.getName() : "";
            return n1.compareToIgnoreCase(n2);
        });
        for (TreeNode child : node.getChildren()) {
            sortNodeChildren(child);
        }
    }

    private void updateVisibleTreeNodes() {
        visibleTreeNodes.clear();

        List<TreeNode> rootsToDisplay = new ArrayList<>();
        if (currentSearchFilter == FILTER_USB1) {
            for (TreeNode r : rootTreeNodes) {
                if ("USB 1".equalsIgnoreCase(r.getUsbSource()) || "usb1".equalsIgnoreCase(r.getId())) {
                    rootsToDisplay.add(r);
                }
            }
        } else if (currentSearchFilter == FILTER_USB2) {
            for (TreeNode r : rootTreeNodes) {
                if ("USB 2".equalsIgnoreCase(r.getUsbSource()) || "usb2".equalsIgnoreCase(r.getId())) {
                    rootsToDisplay.add(r);
                }
            }
        } else {
            rootsToDisplay.addAll(rootTreeNodes);
        }

        for (TreeNode root : rootsToDisplay) {
            addVisibleNodeRecursive(root, 0, visibleTreeNodes);
        }

        if (folderTreeAdapter != null) {
            folderTreeAdapter.setVisibleNodes(visibleTreeNodes);
            Track currentTrack = getCurrentTrack();
            folderTreeAdapter.setCurrentPlayingTrack(currentTrack);
            folderTreeAdapter.setSelectedFolder(currentSelectedFolder);
        }
    }

    private void addVisibleNodeRecursive(TreeNode node, int level, List<TreeNode> output) {
        node.setLevel(level);
        output.add(node);
        if (node.isExpanded() && node.hasChildren()) {
            for (TreeNode child : node.getChildren()) {
                addVisibleNodeRecursive(child, level + 1, output);
            }
        }
    }

    private void collapseAllTreeNodes() {
        for (TreeNode root : rootTreeNodes) {
            setNodeExpandedRecursive(root, false);
        }
        updateVisibleTreeNodes();
    }

    private void setNodeExpandedRecursive(TreeNode node, boolean expanded) {
        if (node == null) return;
        node.setExpanded(expanded);
        if (node.hasChildren()) {
            for (TreeNode child : node.getChildren()) {
                setNodeExpandedRecursive(child, expanded);
            }
        }
    }

    /** El número de la pestaña Pistas es siempre el de la carpeta activa, nunca el de una búsqueda. */
    private void updateTracksTabCount() {
        refreshTabLabels();
    }

    private void playFolderFromNode(TreeNode node) {
        if (node == null) return;
        // Al pulsar el botón de la carpeta en Carpetas USB: abrirla en la pestaña Pistas sin autoreproducir
        openFolderFromNode(node, false);
    }

    private void openFolderFromNode(TreeNode node, boolean autoPlay) {
        if (node == null) return;
        currentSelectedFolder = node.getName();
        currentSelectedFolderUsbSource = node.getUsbSource();

        List<Track> folderTracks = getTracksForNode(node);
        if (!folderTracks.isEmpty()) {
            currentPlaylist.clear();
            currentPlaylist.addAll(folderTracks);
            updateTracksTabCount();
        }
        if (folderTreeAdapter != null) {
            folderTreeAdapter.setSelectedFolder(node.getName());
        }

        currentSearchFilter = FILTER_FOLDER;
        savedTracksQuery = ""; // Se eligió otra carpeta: la búsqueda anterior de Pistas ya no aplica
        selectTab(1);
        String usbTag = (currentSelectedFolderUsbSource != null && !currentSelectedFolderUsbSource.isEmpty()) ? "[" + currentSelectedFolderUsbSource + "] " : "";
        tvCurrentPath.setText("📁 " + usbTag + node.getName() + " (" + currentPlaylist.size() + ")");

        if (autoPlay && !displayedTracks.isEmpty()) {
            playTrack(0);
        }
    }

    private boolean isTrackFromUsb(Track t, String usbSource) {
        if (t == null || usbSource == null || usbSource.isEmpty()) return true;
        boolean wantUsb2 = usbSource.contains("2") || usbSource.toLowerCase().contains("udisk2");
        boolean wantUsb1 = usbSource.contains("1") || usbSource.toLowerCase().contains("udisk1");

        String tUsb = t.getUsbSource();
        String tPath = t.getFilePath() != null ? t.getFilePath().toLowerCase() : (t.getPath() != null ? t.getPath().toLowerCase() : "");
        boolean isTrackUsb2 = "USB 2".equalsIgnoreCase(tUsb) || tPath.contains("udisk2") || tPath.contains("usb1");

        if (wantUsb2) return isTrackUsb2;
        if (wantUsb1) return !isTrackUsb2;
        return true;
    }

    /**
     * Canciones que están directamente en la carpeta del nodo: las mismas que cuenta el botón [N ▶].
     * Si la carpeta solo tiene subcarpetas (o es la raíz del USB), devuelve todas las de sus subcarpetas.
     */
    private List<Track> getTracksForNode(TreeNode node) {
        List<Track> list = new ArrayList<>();
        if (node == null) return list;

        if (!node.getDirectTracks().isEmpty()) {
            list.addAll(node.getDirectTracks());
            return list;
        }

        Set<Track> subtreeTracks = new HashSet<>();
        collectSubtreeTracks(node, subtreeTracks);
        // Recorrer allTracks para conservar el orden alfabético global
        for (Track t : allTracks) {
            if (subtreeTracks.contains(t)) list.add(t);
        }
        return list;
    }

    private void collectSubtreeTracks(TreeNode node, Set<Track> out) {
        out.addAll(node.getDirectTracks());
        for (TreeNode child : node.getChildren()) {
            collectSubtreeTracks(child, out);
        }
    }

    private void revealPlayingFolderInTree(Track track) {
        if (track == null || rootTreeNodes.isEmpty()) return;
        String folder = track.getFolder();
        String path = track.getFilePath();

        for (TreeNode root : rootTreeNodes) {
            if (isTrackFromUsb(track, root.getUsbSource()) && expandPathToNode(root, folder, path)) {
                root.setExpanded(true);
            }
        }
        updateVisibleTreeNodes();
    }

    private boolean expandPathToNode(TreeNode current, String targetFolder, String targetPath) {
        if (current == null) return false;
        boolean matchesThis = (!current.isUsbRoot() && (
                (targetFolder != null && targetFolder.equalsIgnoreCase(current.getName())) ||
                (targetPath != null && current.getPath() != null && targetPath.startsWith(current.getPath()))
        ));

        boolean childMatched = false;
        if (current.hasChildren()) {
            for (TreeNode child : current.getChildren()) {
                if (expandPathToNode(child, targetFolder, targetPath)) {
                    childMatched = true;
                }
            }
        }

        if (childMatched || matchesThis) {
            current.setExpanded(true);
            return true;
        }
        return false;
    }

    private Track getCurrentTrack() {
        List<Track> queue = getQueue();
        if (currentTrackIndex >= 0 && currentTrackIndex < queue.size()) {
            return queue.get(currentTrackIndex);
        }
        return null;
    }

    private void filterByTab() {
        filterSearch(etSearch.getText().toString());
    }

    // ==========================================
    // ALGORITMO DE BÚSQUEDA INTELIGENTE
    // Normalización de tildes + deduplicación de letras + fuzzy match
    // ==========================================
    private String normalizeText(String text) {
        return SearchText.normalize(text);
    }

    private String dedupChars(String s) {
        return SearchText.dedup(s);
    }

    /** Resultado de búsqueda con su puntaje ya calculado (una vez por elemento, no en cada comparación del sort). */
    private static final class Scored<T> {
        final T item;
        final int score;

        Scored(T item, int score) {
            this.item = item;
            this.score = score;
        }
    }

    private static <T> List<T> sortByScore(List<Scored<T>> scored) {
        Collections.sort(scored, (a, b) -> Integer.compare(b.score, a.score));
        List<T> sorted = new ArrayList<>(scored.size());
        for (Scored<T> s : scored) sorted.add(s.item);
        return sorted;
    }

    private static String[] dedupTokens(String[] tokens) {
        String[] out = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) out[i] = SearchText.dedup(tokens[i]);
        return out;
    }

    private int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private boolean tokenMatches(String token, String word) {
        if (token == null || word == null) return false;
        if (word.contains(token)) return true;

        String tDedup = dedupChars(token);
        String wDedup = dedupChars(word);
        if (wDedup.contains(tDedup)) return true;
        if (tDedup.contains(wDedup) && wDedup.length() >= 4) return true;

        int maxDist = token.length() >= 6 ? 2 : (token.length() >= 4 ? 1 : 0);
        if (maxDist > 0 && Math.abs(token.length() - word.length()) <= maxDist) {
            if (levenshtein(token, word) <= maxDist) return true;
        }
        if (word.startsWith(token) || (token.length() >= 4 && word.startsWith(token.substring(0, token.length() - 1)))) {
            return true;
        }
        return false;
    }

    private boolean fastTrackMatches(Track track, String[] tokens, String[] tokensDedup) {
        Track.SearchKey key = track.getSearchKey();

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].isEmpty()) continue;
            // Búsqueda ultra rápida en subcadenas sobre texto ya normalizado
            if (key.haystack.contains(tokens[i])) continue;
            if (key.haystackDedup.contains(tokensDedup[i])) continue;
            return false;
        }
        return true;
    }

    /** Términos tal como se escribieron; null si la consulta no trae ñ ni tildes (no hay nada que premiar). */
    private static String[] exactTokens(String rawQuery, String normQuery) {
        String exact = SearchText.exactLower(rawQuery);
        return exact.equals(normQuery) ? null : SearchText.tokens(exact);
    }

    /** "ñaña" escrito así va antes que "nana": premio por cada término con la misma escritura. */
    private static int exactSpellingBonus(String exactTitle, String exactHaystack, String[] exactTokens) {
        if (exactTokens == null) return 0;
        int bonus = 0;
        for (String token : exactTokens) {
            if (token.isEmpty()) continue;
            if (exactTitle.contains(token)) bonus += 20000;
            else if (exactHaystack.contains(token)) bonus += 12000;
        }
        return bonus;
    }

    private int calculateTrackRelevance(Track track, String normQuery, String[] tokens, String[] exactTokens) {
        int score = 0;
        Track.SearchKey key = track.getSearchKey();
        String titleNorm = key.title;
        String artistNorm = key.artist;
        String albumNorm = key.album;
        String folderNorm = key.folder;

        String[] titleWords = key.titleWords;
        String[] artistWords = key.artistWords;

        if (titleNorm.equals(normQuery)) {
            score += 25000;
        } else if (titleNorm.startsWith(normQuery + " ") || titleNorm.startsWith(normQuery + "-")) {
            score += 18000;
        } else if (titleNorm.startsWith(normQuery)) {
            score += 12000;
        } else if (titleNorm.contains(normQuery)) {
            score += 6000;
        }

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            boolean wordExact = false;
            boolean wordPrefix = false;
            for (String w : titleWords) {
                if (w.equals(token)) {
                    wordExact = true;
                    break;
                }
                if (w.startsWith(token)) {
                    wordPrefix = true;
                }
            }

            if (wordExact) {
                score += 8000;
            } else if (wordPrefix) {
                score += 4000;
            } else if (titleNorm.contains(token)) {
                score += 2000;
            } else {
                score += 300;
            }

            boolean artExact = false;
            for (String w : artistWords) {
                if (w.equals(token)) {
                    artExact = true;
                    break;
                }
            }
            if (artExact) score += 3000;
            else if (artistNorm.contains(token)) score += 1000;

            if (folderNorm.contains(token) || albumNorm.contains(token)) {
                score += 400;
            }
        }

        score += exactSpellingBonus(key.exactTitle, key.exactHaystack, exactTokens);
        score -= Math.min(titleNorm.length(), 60);
        return score;
    }

    private int calculateFolderRelevance(FolderItem folder, String normQuery, String[] tokens, String[] exactTokens) {
        int score = 0;
        FolderItem.SearchKey key = folder.getSearchKey();
        String nameNorm = key.name;
        String[] words = key.words;

        if (nameNorm.equals(normQuery)) {
            score += 20000;
        } else if (nameNorm.startsWith(normQuery + " ") || nameNorm.startsWith(normQuery)) {
            score += 12000;
        } else if (nameNorm.contains(normQuery)) {
            score += 5000;
        }

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            boolean exact = false;
            for (String w : words) {
                if (w.equals(token)) {
                    exact = true;
                    break;
                }
            }
            if (exact) score += 6000;
            else if (nameNorm.contains(token)) score += 2000;
        }

        score += exactSpellingBonus(key.exactName, key.exactHaystack, exactTokens);
        score -= Math.min(nameNorm.length(), 50);
        return score;
    }

    private void performAsyncSearch(final String rawQuery, final int targetVersion) {
        final int tab = currentTab;
        final int filter = currentSearchFilter;
        final String folder = currentSelectedFolder;
        // Mi Mix se busca sobre una copia hecha aquí (el hilo de búsqueda no toca la lista)
        final List<Track> mixSnapshot = tab == 2 ? mixTracks() : null;
        runOnUiThread(() -> {
            if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);
            if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.VISIBLE);
            if (listViewContent != null) {
                listViewContent.setAlpha(0.25f);
                listViewContent.setEnabled(false);
            }
            if (layoutFastScrollerTrack != null) {
                layoutFastScrollerTrack.setVisibility(View.GONE);
            }
        });

        searchExecutor.execute(() -> {
            if (targetVersion != searchVersion) return;

            String norm = normalizeText(rawQuery);
            String[] tokens = SearchText.tokens(norm);
            String[] tokensDedup = dedupTokens(tokens);
            String[] exact = exactTokens(rawQuery, norm);

            if (tab == 0) {
                // PESTAÑA CARPETAS: Buscar con filtro USB 1 / USB 2 / TODO
                List<Scored<FolderItem>> scoredFolders = new ArrayList<>();
                for (FolderItem f : folderList) {
                    if (targetVersion != searchVersion) return;
                    if (filter == FILTER_USB1) {
                        if (!"USB 1".equalsIgnoreCase(f.getUsbSource()) && (f.getPath() == null || (!f.getPath().contains("udisk1") && !f.getPath().contains("usb0")))) {
                            continue;
                        }
                    } else if (filter == FILTER_USB2) {
                        if (!"USB 2".equalsIgnoreCase(f.getUsbSource()) && (f.getPath() == null || (!f.getPath().contains("udisk2") && !f.getPath().contains("usb1")))) {
                            continue;
                        }
                    }

                    FolderItem.SearchKey key = f.getSearchKey();
                    boolean match = true;
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i].isEmpty()) continue;
                        if (!key.haystack.contains(tokens[i]) && !key.haystackDedup.contains(tokensDedup[i])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) scoredFolders.add(new Scored<>(f, calculateFolderRelevance(f, norm, tokens, exact)));
                }

                List<FolderItem> matchedFolders = sortByScore(scoredFolders);

                if (targetVersion != searchVersion) return;

                runOnUiThread(() -> {
                    if (targetVersion != searchVersion) return;
                    if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
                    if (listViewContent != null) {
                        listViewContent.setAlpha(1.0f);
                        listViewContent.setEnabled(true);
                    }
                    if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
                    displayedFolders.clear();
                    displayedFolders.addAll(matchedFolders);
                    if (listViewContent.getAdapter() != folderAdapter) {
                        listViewContent.setAdapter(folderAdapter);
                    }
                    String filterLabel = filter == FILTER_USB1 ? " [USB 1]" : (filter == FILTER_USB2 ? " [USB 2]" : "");
                    tvCurrentPath.setText("📂 Carpetas" + filterLabel + ": \"" + rawQuery + "\" (" + displayedFolders.size() + ")");
                    folderAdapter.setFolders(displayedFolders);
                });

            } else if (tab == 1) {
                // PESTAÑA PISTAS: Buscar con filtro (Esta Carpeta, TODO, USB 1, USB 2)
                List<Track> sourceTracks = new ArrayList<>();
                if (filter == FILTER_FOLDER && folder != null) {
                    if (!currentPlaylist.isEmpty()) {
                        for (Track t : currentPlaylist) {
                            if (isTrackFromUsb(t, currentSelectedFolderUsbSource)) {
                                sourceTracks.add(t);
                            }
                        }
                    } else {
                        for (Track t : allTracks) {
                            if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(folder) && isTrackFromUsb(t, currentSelectedFolderUsbSource)) {
                                sourceTracks.add(t);
                            }
                        }
                    }
                } else if (filter == FILTER_USB1) {
                    for (Track t : allTracks) {
                        if (isTrackFromUsb(t, "USB 1")) {
                            sourceTracks.add(t);
                        }
                    }
                } else if (filter == FILTER_USB2) {
                    for (Track t : allTracks) {
                        if (isTrackFromUsb(t, "USB 2")) {
                            sourceTracks.add(t);
                        }
                    }
                } else {
                    sourceTracks.addAll(allTracks);
                }

                List<Scored<Track>> scoredTracks = new ArrayList<>();
                for (Track t : sourceTracks) {
                    if (targetVersion != searchVersion) return;
                    if (fastTrackMatches(t, tokens, tokensDedup)) {
                        scoredTracks.add(new Scored<>(t, calculateTrackRelevance(t, norm, tokens, exact)));
                    }
                }

                List<Track> matchedTracks = sortByScore(scoredTracks);

                if (targetVersion != searchVersion) return;

                runOnUiThread(() -> {
                    if (targetVersion != searchVersion) return;
                    if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
                    if (listViewContent != null) {
                        listViewContent.setAlpha(1.0f);
                        listViewContent.setEnabled(true);
                    }
                    displayedTracks.clear();
                    displayedTracks.addAll(matchedTracks);
                    if (listViewContent.getAdapter() != trackAdapter) {
                        listViewContent.setAdapter(trackAdapter);
                    }
                    String filterLabel = filter == FILTER_FOLDER ? " [" + (folder != null ? folder : "Carpeta") + "]" :
                            (filter == FILTER_USB1 ? " [USB 1]" : (filter == FILTER_USB2 ? " [USB 2]" : " [Todo]"));
                    // La cantidad de resultados va en la ruta; el número de la pestaña sigue siendo el de la carpeta
                    tvCurrentPath.setText("🔍 Pistas" + filterLabel + ": \"" + rawQuery + "\" (" + displayedTracks.size() + ")");
                    showDisplayedTracks();
                    updateFastScrollerVisibility();
                    if (scrollToPlayingAfterSearch) {
                        scrollToPlayingAfterSearch = false;
                        scrollToPlayingTrack();
                    }
                });

            } else {
                // PESTAÑA MI MIX: buscar dentro de la lista
                List<Track> mixPool = mixSnapshot;
                List<Scored<Track>> scoredFavs = new ArrayList<>();
                for (Track t : mixPool) {
                    if (targetVersion != searchVersion) return;
                    if (fastTrackMatches(t, tokens, tokensDedup)) {
                        scoredFavs.add(new Scored<>(t, calculateTrackRelevance(t, norm, tokens, exact)));
                    }
                }

                List<Track> matchedFavs = sortByScore(scoredFavs);

                if (targetVersion != searchVersion) return;

                runOnUiThread(() -> {
                    if (targetVersion != searchVersion) return;
                    if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                    if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
                    if (listViewContent != null) {
                        listViewContent.setAlpha(1.0f);
                        listViewContent.setEnabled(true);
                    }
                    displayedTracks.clear();
                    displayedTracks.addAll(matchedFavs);
                    if (listViewContent.getAdapter() != trackAdapter) {
                        listViewContent.setAdapter(trackAdapter);
                    }
                    tvCurrentPath.setText("🎧 Mi Lista: \"" + rawQuery + "\" (" + displayedTracks.size() + ")");
                    if (tvMixEmpty != null) tvMixEmpty.setVisibility(View.GONE);
                    updateMixHeader(mixTracks());
                    showDisplayedTracks();
                    updateFastScrollerVisibility();
                });
            }
        });
    }

    private void filterSearch(String rawQuery) {
        String norm = normalizeText(rawQuery);
        String[] tokens = SearchText.tokens(norm);
        boolean hasQuery = !norm.isEmpty() && tokens.length > 0 && !tokens[0].isEmpty();

        // Si hay búsqueda activa: ejecutar de forma asíncrona según la pestaña y filtro
        if (hasQuery) {
            searchVersion++;
            performAsyncSearch(rawQuery, searchVersion);
            return;
        }

        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
        if (layoutSearchLoading != null) layoutSearchLoading.setVisibility(View.GONE);
        if (listViewContent != null) {
            listViewContent.setAlpha(1.0f);
            listViewContent.setEnabled(true);
        }

        // Modo Normal por pestaña y filtro seleccionado
        if (currentTab == 0) {
            if (recyclerMixContent != null) recyclerMixContent.setVisibility(View.GONE);
            listViewContent.setVisibility(View.VISIBLE);
            if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.VISIBLE);
            if (currentSearchFilter == FILTER_USB1) {
                tvCurrentPath.setText("Carpetas USB 1 (Árbol)");
            } else if (currentSearchFilter == FILTER_USB2) {
                tvCurrentPath.setText("Carpetas USB 2 (Árbol)");
            } else {
                tvCurrentPath.setText("Carpetas USB (Árbol de Pendrives)");
            }

            updateVisibleTreeNodes();
            if (listViewContent.getAdapter() != folderTreeAdapter) {
                listViewContent.setAdapter(folderTreeAdapter);
            }

        } else if (currentTab == 1) {
            if (recyclerMixContent != null) recyclerMixContent.setVisibility(View.GONE);
            listViewContent.setVisibility(View.VISIBLE);
            if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
            displayedTracks.clear();

            // Sin texto en el buscador, Pistas muestra siempre la carpeta activa (la elegida con [N ▶]
            // o la de la última canción reproducida), sea cual sea el chip. Los chips solo definen dónde buscar.
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                displayedTracks.addAll(currentPlaylist);
            } else if (currentSelectedFolder != null) {
                for (Track t : allTracks) {
                    if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(currentSelectedFolder)) {
                        if (isTrackFromUsb(t, currentSelectedFolderUsbSource)) {
                            displayedTracks.add(t);
                        }
                    }
                }
            }
            // Si aún no se eligió ninguna carpeta, la lista queda vacía (no se elige una automáticamente)
            String usbTag = (currentSelectedFolderUsbSource != null && !currentSelectedFolderUsbSource.isEmpty()) ? "[" + currentSelectedFolderUsbSource + "] " : "";
            tvCurrentPath.setText(currentSelectedFolder != null ? "📁 " + usbTag + currentSelectedFolder + " (" + displayedTracks.size() + ")" : "🔍 Elige una carpeta o escribe para buscar");

            showDisplayedTracks();
            if (listViewContent.getAdapter() != trackAdapter) {
                listViewContent.setAdapter(trackAdapter);
            }
            updateTracksTabCount();
            trackAdapter.setCurrentPlayingTrack(getCurrentTrack());

        } else if (currentTab == 2) {
            if (btnCollapseAllTree != null) btnCollapseAllTree.setVisibility(View.GONE);
            showMixTab();
        }
        updateFastScrollerVisibility();
    }

    // ==========================================
    // MOTOR DE AUDIO NATIVO Y REPRODUCCIÓN
    // ==========================================
    private void playTrack(int index) {
        if (displayedTracks.isEmpty() || index < 0 || index >= displayedTracks.size()) return;

        Track track = displayedTracks.get(index);

        if (currentTab == 2) {
            // Desde Mi Mix (lista o búsqueda dentro de ella): suena Mi Mix desde esa canción.
            // La que suena fuera de la lista ya está sonando: no hace nada.
            if (isMixOrphan(track)) return;
            int position = mixTracks().indexOf(track);
            if (position >= 0) playMix(position, false);
            return;
        }

        // Cualquier otra lista deja Mi Mix: ahora suena esa carpeta / lista
        playingFromMix = false;
        int indexInPlaylist = currentPlaylist.indexOf(track);
        if (indexInPlaylist >= 0) {
            // La canción ya es de la carpeta activa: conservar esa lista tal cual (la del botón [N ▶])
            setPlayQueueFromFolder();
            currentTrackIndex = indexInPlaylist;
        } else if (track.getFolder() != null && !track.getFolder().isEmpty()) {
            // Canción de otra carpeta (p. ej. desde una búsqueda): su carpeta pasa a ser la activa
            currentSelectedFolder = track.getFolder();
            currentSelectedFolderUsbSource = track.getUsbSource();
            if (currentSelectedFolderUsbSource == null || currentSelectedFolderUsbSource.isEmpty()) {
                String p = track.getFilePath() != null ? track.getFilePath().toLowerCase() : "";
                if (p.contains("udisk2") || p.contains("usb2")) {
                    currentSelectedFolderUsbSource = "USB 2";
                } else if (p.contains("udisk1") || p.contains("usb1") || p.contains("usb0")) {
                    currentSelectedFolderUsbSource = "USB 1";
                }
            }
            updateFilterChipsVisibility();
            currentPlaylist.clear();
            for (Track t : allTracks) {
                if (t.getFolder() != null && t.getFolder().equalsIgnoreCase(currentSelectedFolder)) {
                    if (isTrackFromUsb(t, currentSelectedFolderUsbSource)) {
                        currentPlaylist.add(t);
                    }
                }
            }
            int folderIdx = -1;
            for (int i = 0; i < currentPlaylist.size(); i++) {
                if (currentPlaylist.get(i).getFilePath().equals(track.getFilePath())) {
                    folderIdx = i;
                    break;
                }
            }
            setPlayQueueFromFolder();
            currentTrackIndex = folderIdx >= 0 ? folderIdx : 0;
            updateTracksTabCount();
            if (folderTreeAdapter != null) {
                folderTreeAdapter.setSelectedFolder(currentSelectedFolder);
            }
        } else {
            playQueue.clear();
            playQueue.addAll(displayedTracks);
            currentTrackIndex = index;
        }

        if (!startPlayback(track)) {
            Toast.makeText(this, "Error al reproducir: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // INICIO DE REPRODUCCIÓN (compartido por toque, Siguiente/Anterior y Automix)
    // ==========================================
    private VideoPlayer createVideoPlayer() {
        return new VideoPlayer(this, handler, videoListener);
    }

    private final VideoPlayer.Listener videoListener = new VideoPlayer.Listener() {
        @Override
        public void onPrepared() {
            playerPrepared = true;
            videoPlayer.setVolume(1f);
            videoPlayer.start();
            clockRun(true);
            isPlaying = true;
            onTrackStarted(openingTrack);
            // ExoPlayer no pudo con este archivo y lo abrió el reproductor del radio: que se vea por qué
            String reason = videoPlayer.getRadioReason();
            if (reason != null) showTimeNote("ⓘ Lo abre el reproductor del radio (ExoPlayer: " + reason + ")");
        }

        @Override
        public void onCompletion() {
            onPlayerCompleted();
        }

        @Override
        public void onVideoSizeChanged(int width, int height) {
            onVideoSizeKnown(width, height);
        }

        @Override
        public void onError(String code) {
            onPlayerError(code);
        }
    };

    private void onPlayerCompleted() {
        if (isRepeat) {
            playTrackFromQueue(getQueue(), currentTrackIndex);
            return;
        }
        // Automix: si la canción terminó antes de lo previsto, la siguiente ya precargada entra al instante
        if (autoMixer.startPreparedNextNow()) return;
        playNextTrack();
    }

    private boolean startPlayback(Track track) {
        return startPlayback(track, false);
    }

    /**
     * Reproduce un video desde cero (inicio manual). Devuelve false si el archivo no se pudo abrir.
     * quickFade es el fundido de Automix entre canciones: JH VIDEO no tiene Automix, siempre es un corte directo.
     */
    private boolean startPlayback(Track track, boolean quickFade) {
        autoMixer.onManualPlayback();
        try {
            playerPrepared = false;
            isPlaying = false;
            if (videoPlayer == null) videoPlayer = createVideoPlayer();
            // El video se dibuja en el cuadro del reproductor o en la pantalla completa
            attachedVideoSurface = null;
            attachVideoSurface(false);
            // Se abre en segundo plano: un video grande del USB tarda en abrir y la pantalla no se congela.
            // Avisa con videoListener.onPrepared
            openingTrack = track;
            videoPlayer.open(track.getFilePath());
            showOpeningTrack(track);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Mientras el video se abre: su título en pantalla y la barra en cero. */
    private void showOpeningTrack(Track track) {
        handler.removeCallbacks(ambientCaptureRunnable);
        handler.removeCallbacks(seekCheckRunnable); // el salto que se revisaba era del video anterior
        resetStallWatch();
        resetPlaybackClock(track);
        updateNowPlayingInfo(track);
        tvTimeCurrent.setText("00:00");
        tvTimeTotal.setText("--:--");
        seekTrackProgress.setProgress(0);
        if (seekFsProgress != null) seekFsProgress.setProgress(0);
        if (tvFsTime != null) tvFsTime.setText("00:00 / --:--");
        updatePlaybackStateUI();
    }

    /** El video está abierto y se le pueden pedir posición, duración o saltos. */
    private boolean playerReady() {
        return videoPlayer != null && playerPrepared;
    }

    /**
     * El reproductor falló (archivo que no abre o el decodificador se cayó): se libera y el siguiente
     * toque crea uno nuevo. El aviso queda en la barra de arriba para saber qué pasó.
     */
    private void onPlayerError(String code) {
        boolean wasOpening = !playerPrepared;
        playerPrepared = false;
        isPlaying = false;
        attachedVideoSurface = null;
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
        // Primero el motivo: si no cabe, se corta el final (los códigos)
        playerErrorText = (wasOpening ? "⚠ No se pudo abrir: " : "⚠ El reproductor falló: ") + code;
        playerErrorUntil = SystemClock.uptimeMillis() + PLAYER_ERROR_HOLD_MS;
        updatePlaybackStateUI();
        updateLoadMonitor();
        if (wasOpening) Toast.makeText(this, "No se pudo reproducir este video (" + code + ")", Toast.LENGTH_LONG).show();
    }

    /** Actualiza la pantalla para la canción que acaba de empezar (a mano o por mezcla). */
    private void onTrackStarted(Track track) {
        onTrackStarted(track, false);
    }

    /**
     * smoothUi: mezcla automática. Igual que la web: los datos cambian al instante (sin vacíos ni
     * parpadeos) y solo la carátula cambia con fundido cruzado.
     */
    private void onTrackStarted(Track track, boolean smoothUi) {
        // Mi Mix: la que empieza a sonar queda marcada como "ya sonó"
        if (playingFromMix) {
            mixCurPos = currentTrackIndex;
            mixPlayedKeys.add(favoriteKey(track));
        }

        // Automix analiza en segundo plano dónde se apaga esta canción
        autoMixer.onTrackStarted(track);

        int durationMs = rawPlayerDuration();
        if (durationMs > 0) track.setDurationSec(durationMs / 1000);

        albumArtFadeForPath = smoothUi ? track.getFilePath() : null;
        showTrackOnScreen(track);
        refreshTabLabels(); // el punto de Mi Mix aparece o se va según lo que suena
        // En la pestaña Mi Mix: marcas ✓, resumen y la que suena fuera de la lista
        if (currentTab == 2 && etSearch != null && etSearch.getText().toString().trim().isEmpty()) showMixTab();
    }

    /** Pone en pantalla los datos de la canción que suena. */
    private void showTrackOnScreen(Track track) {
        // Marcar la canción en la lista mostrada (si está en ella)
        trackAdapter.setCurrentPlayingTrack(track);
        if (mixRecyclerAdapter != null) mixRecyclerAdapter.setCurrentPlayingTrack(track);
        updatePlaybackStateUI();

        // Actualizar información en pantalla con tipografía grande y clara
        updateNowPlayingInfo(track);

        int durationMs = 0;
        try {
            durationMs = playerReady() ? playbackDurationMs() : 0;
        } catch (Exception ignored) {}
        shownTotalMs = durationMs;
        tvTimeTotal.setText(durationMs > 0 ? formatMs(durationMs) : "--:--");
        int playerDuration = rawPlayerDuration();
        if (playerDuration <= 0) {
            showTimeNote("ⓘ El reproductor no sabe cuánto dura el video (" + playerDuration + ")"
                    + (fileDurationMs > 0 ? ": uso la del archivo" : ""));
        }
        tvTimeCurrent.setText("00:00");
        seekTrackProgress.setProgress(0);

        // Cargar carátula del archivo y bitrate exacto
        loadAlbumArt(track);

        // Previsualización de canción anterior y siguiente de la carpeta activa
        updatePreviews();
    }

    // Carátula real (detrás del disco y las barras): en la mezcla automática cambia con fundido cruzado
    private static final int ALBUM_ART_FADE_MS = 700;
    private String albumArtFadeForPath;   // canción cuya carátula debe entrar con fundido

    private int albumArtFadeToken = 0;    // cada cambio de carátula invalida el fundido anterior

    /**
     * Con carátula: se ve la imagen completa y el disco (dibujo de "sin carátula") se oculta.
     * Sin carátula: se ve el disco. La cápsula de estado queda siempre abajo.
     */
    private void setCoverMode(boolean hasCover) {
        // JH VIDEO: el dibujo de "sin video" lo maneja el tamaño del video (onVideoSizeKnown)
    }

    private void showAlbumArt(Bitmap art, boolean smooth) {
        albumArtFadeToken++;
        // JH VIDEO: la imagen no se muestra (se ve el video); solo pinta el fondo de la app con su color
        if (ambientTheme != null) ambientTheme.applyCover(art, AmbientTheme.DEFAULT_FADE_MS);
    }

    /** Fundido de opacidad con Handler (no Animator), para radios con animaciones desactivadas. */
    private void fadeViewAlpha(final View view, final float from, final float to, final Runnable onEnd) {
        final long start = SystemClock.uptimeMillis();
        final int token = albumArtFadeToken;
        view.setAlpha(from);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (token != albumArtFadeToken) return; // otra carátula ya tomó el control
                float t = Math.min(1f, (SystemClock.uptimeMillis() - start) / (float) ALBUM_ART_FADE_MS);
                view.setAlpha(from + (to - from) * t);
                if (t < 1f) {
                    handler.postDelayed(this, 30);
                } else if (onEnd != null) {
                    onEnd.run();
                }
            }
        });
    }

    // ==========================================
    // AUTOMIX: mezcla automática entre canciones
    // ==========================================
    private static final String PREF_AUTOMIX = "automix_enabled";

    private final AutoMixer.Host autoMixHost = new AutoMixer.Host() {
        @Override
        public MediaPlayer getCurrentPlayer() {
            return null; // JH VIDEO no tiene Automix: el mezclador no toca el reproductor de video
        }

        @Override
        public boolean isPlaybackActive() {
            return videoPlayer != null && isPlaying && !isUserSeeking;
        }

        @Override
        public AutoMixer.NextTrack pickNextTrack() {
            if (isRepeat) return null;
            List<Track> queue = getQueue();
            if (queue.size() < 2 || currentTrackIndex < 0 || currentTrackIndex >= queue.size()) return null;
            int index;
            if (playingFromMix) {
                // Mi Mix: después de la última no hay mezcla (se detiene)
                index = nextMixIndex();
                return index < 0 ? null : new AutoMixer.NextTrack(index, queue.get(index));
            }
            if (isShuffle) {
                // Aleatorio: se sortea ahora (no al terminar) y nunca la misma canción
                index = new Random().nextInt(queue.size() - 1);
                if (index >= currentTrackIndex) index++;
            } else {
                index = (currentTrackIndex + 1) % queue.size();
            }
            return new AutoMixer.NextTrack(index, queue.get(index));
        }

        @Override
        public boolean isStillNext(AutoMixer.NextTrack next) {
            if (next == null || isRepeat) return false;
            List<Track> queue = getQueue();
            return next.index < queue.size() && queue.get(next.index) == next.track;
        }

        @Override
        public MediaPlayer createPlayer() {
            return new MediaPlayer(); // no se usa: sin Automix no se precarga el siguiente
        }

        @Override
        public void onMixStarted(MediaPlayer incoming, AutoMixer.NextTrack next, boolean automatic) {
            incoming.release(); // no pasa: sin Automix no hay mezcla
        }

        @Override
        public void onMixVisualChanged(boolean mixing) {
            // Igual que la web: segundo haz láser magenta + cápsula MIXING ⇄ mientras dura la mezcla
            if (laserRing != null) laserRing.setMixing(mixing);
            if (nowPlayingPill != null) nowPlayingPill.setMixing(mixing);
        }
    };

    private void toggleAutoMix() {
        boolean on = !autoMixer.isEnabled();
        autoMixer.setEnabled(on);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(PREF_AUTOMIX, on).apply();
        updateAutoMixButton();
        updatePlaybackStateUI();
        Toast.makeText(this, on
                ? "Automix activado: mezcla de " + (AutoMixer.DEFAULT_MIX_MS / 1000) + " s y volumen igualado"
                : "Automix desactivado", Toast.LENGTH_SHORT).show();
    }

    /** Igual que la web (opción A): ON = cuadro cian lleno con brillo y el ícono oscuro; OFF = oscuro y gris. */
    private void updateAutoMixButton() {
        if (btnAutoMix == null) return;
        boolean on = autoMixer.isEnabled();
        btnAutoMix.setSelected(on);
        btnAutoMix.setBackgroundResource(on ? R.drawable.bg_automix_on : R.drawable.bg_automix_off);
        int color = Color.parseColor(on ? "#0c0e12" : "#64748b");
        if (ivAutoMixIcon != null) ivAutoMixIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        if (tvAutoMixState != null) {
            tvAutoMixState.setText(on ? "ON" : "OFF");
            tvAutoMixState.setTextColor(color);
        }
    }

    /**
     * Cola de lo que suena (anterior/siguiente). Es una copia: abrir otra carpeta en Pistas o
     * cambiar de pestaña no altera la cola hasta que se reproduzca algo de la nueva lista.
     */
    private List<Track> getQueue() {
        if (!playQueue.isEmpty()) return playQueue;
        return !currentPlaylist.isEmpty() ? currentPlaylist : displayedTracks;
    }

    private void setPlayQueueFromFolder() {
        playQueue.clear();
        playQueue.addAll(currentPlaylist);
    }

    /**
     * Tras re-escanear (p. ej. al quitar un pendrive): pasar la carpeta activa y la cola a las pistas
     * nuevas, quitar las que ya no existen y detener la reproducción si su pendrive se quitó.
     */
    private void reconcileListsAfterRescan(Map<String, Track> tracksByPath) {
        Track playing = getCurrentTrack();
        // La siguiente precargada puede ser del pendrive que se quitó: se vuelve a elegir
        autoMixer.invalidateNext();

        remapTracks(currentPlaylist, tracksByPath);
        if (currentPlaylist.isEmpty()) {
            currentSelectedFolder = null;
            currentSelectedFolderUsbSource = null;
        }
        remapTracks(playQueue, tracksByPath);

        Track stillPlaying = playing != null ? tracksByPath.get(playing.getFilePath()) : null;
        if (stillPlaying != null) {
            currentTrackIndex = getQueue().indexOf(stillPlaying);
            // Mi Mix: rearmar la cola con las canciones nuevas (un pendrive pudo quitarse o conectarse)
            syncMixQueue();
        } else if (playing != null) {
            // La canción que sonaba estaba en el pendrive que se quitó
            playingFromMix = false;
            autoMixer.onManualPlayback();
            if (videoPlayer != null) {
                videoPlayer.release();
                videoPlayer = null;
            }
            isPlaying = false;
            playQueue.clear();
            currentTrackIndex = -1;
            trackAdapter.setCurrentPlayingTrack(null);
            updatePlaybackStateUI();
        }
    }

    // ==========================================
    // MI MIX (reemplaza a Favoritos)
    // Una lista que se arma cuando quieras (botón + en cada canción o carpeta) y que suena entera,
    // en el orden en que se agregó, con Automix. Como la lista de Automix de Virtual DJ:
    // - Lo que se agrega mientras suena entra al final de la cola en vivo.
    // - Al terminar la última canción, se detiene.
    // - Las que ya sonaron quedan en la lista, marcadas con ✓.
    // ==========================================
    private static final String PREFS_NAME = "jh_music";
    private static final String PREF_FAVORITES = "favorite_paths"; // solo para pasar las estrellas viejas a Mi Mix
    private static final String PREF_MYMIX = "mymix_keys";

    /** Canciones de Mi Mix, en orden, por su ruta dentro del pendrive (se conservan aunque cambie de puerto). */
    private final List<String> myMixKeys = new ArrayList<>();
    private final Set<String> mixPlayedKeys = new HashSet<>();
    private boolean myMixLoaded = false;
    private boolean playingFromMix = false;  // la cola que suena es Mi Mix
    private int mixCurPos = 0;               // posición de la que suena dentro de la cola de Mi Mix
    private boolean mixConfirmClear = false; // mostrando "¿Vaciar Mi Lista?"

    /** Clave de una canción: ruta relativa al pendrive, para que se conserve aunque el USB cambie de puerto. */
    private static String favoriteKey(Track t) {
        return SearchText.relativeUsbPath(t.getFilePath());
    }

    /**
     * Carga Mi Mix guardado. La primera vez (sin Mi Mix guardado) pasan las estrellas de Favoritos:
     * primero en el orden A-Z de la biblioteca y después las de pendrives que no están conectados.
     */
    private void loadMyMix(List<Track> library) {
        if (myMixLoaded) return;
        myMixLoaded = true;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        myMixKeys.clear();
        if (prefs.contains(PREF_MYMIX)) {
            String saved = prefs.getString(PREF_MYMIX, "");
            for (String key : saved.split("\n")) {
                if (!key.isEmpty() && !myMixKeys.contains(key)) myMixKeys.add(key);
            }
            return;
        }
        Set<String> favorites = new HashSet<>(prefs.getStringSet(PREF_FAVORITES, new HashSet<>()));
        for (Track t : library) {
            String key = favoriteKey(t);
            if (favorites.remove(key) && !myMixKeys.contains(key)) myMixKeys.add(key);
        }
        List<String> rest = new ArrayList<>(favorites);
        Collections.sort(rest);
        myMixKeys.addAll(rest);
        saveMyMix();
    }

    private void saveMyMix() {
        StringBuilder sb = new StringBuilder();
        for (String key : myMixKeys) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(key);
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_MYMIX, sb.toString()).apply();
    }

    /** Canciones de Mi Mix conectadas ahora, en el orden de la lista. */
    private List<Track> mixTracks() {
        Map<String, Track> byKey = new HashMap<>();
        for (Track t : allTracks) byKey.put(favoriteKey(t), t);
        List<Track> list = new ArrayList<>();
        for (String key : myMixKeys) {
            Track t = byKey.get(key);
            if (t != null) list.add(t);
        }
        return list;
    }

    private boolean isInMix(Track t) {
        return t != null && myMixKeys.contains(favoriteKey(t));
    }

    /** La que suena es de Mi Mix pero ya no está en la lista (se quitó o se vació). */
    private boolean isMixOrphan(Track t) {
        if (!playingFromMix || t == null) return false;
        Track current = getCurrentTrack();
        return current != null && current.getFilePath().equals(t.getFilePath()) && !isInMix(t);
    }

    private final TrackAdapter.MixState mixState = new TrackAdapter.MixState() {
        @Override
        public boolean isInMix(Track track) {
            return MainActivity.this.isInMix(track);
        }

        @Override
        public boolean isPlayed(Track track) {
            return track != null && mixPlayedKeys.contains(favoriteKey(track));
        }

        @Override
        public boolean isOrphan(Track track) {
            return isMixOrphan(track);
        }
    };

    private void updateMixTabCount() {
        refreshTabLabels();
    }

    /** Agrega canciones al final de Mi Mix (sin repetir). Devuelve cuántas se agregaron. */
    private int addToMix(List<Track> tracks) {
        int added = 0;
        for (Track t : tracks) {
            String key = favoriteKey(t);
            if (!myMixKeys.contains(key)) {
                myMixKeys.add(key);
                added++;
            }
        }
        if (added > 0) onMixChanged();
        return added;
    }

    private void removeFromMix(Track t) {
        String key = favoriteKey(t);
        myMixKeys.remove(key);
        mixPlayedKeys.remove(key);
        onMixChanged();
    }

    private void clearMix() {
        myMixKeys.clear();
        mixPlayedKeys.clear();
        onMixChanged();
    }

    /** La que suena se había quitado: vuelve a su lugar en la lista (no al final, para no saltarse las siguientes). */
    private void reAddToMixInPlace(Track t) {
        String key = favoriteKey(t);
        if (myMixKeys.contains(key)) return;
        // Su lugar: antes de la primera canción de la lista que venía después en la cola
        List<Track> queue = getQueue();
        int insertAt = myMixKeys.size();
        for (int i = currentTrackIndex + 1; i < queue.size(); i++) {
            int idx = myMixKeys.indexOf(favoriteKey(queue.get(i)));
            if (idx >= 0) {
                insertAt = idx;
                break;
            }
        }
        myMixKeys.add(insertAt, key);
        onMixChanged();
    }

    private void onMixChanged() {
        saveMyMix();
        updateMixTabCount();
        syncMixQueue();
        if (currentTab == 2) filterSearch(etSearch != null ? etSearch.getText().toString() : "");
    }

    /**
     * Mientras suena Mi Mix, la cola se rearma con la lista actual (agregar / quitar en vivo).
     * Si se quitó la que suena, sigue sonando y después viene la que estaba a continuación.
     */
    private void syncMixQueue() {
        if (!playingFromMix) return;
        Track current = getCurrentTrack();
        List<Track> list = mixTracks();
        int idx = current != null ? list.indexOf(current) : -1;
        if (idx < 0 && current != null) {
            idx = Math.min(mixCurPos, list.size());
            list.add(idx, current);
        }
        playQueue.clear();
        playQueue.addAll(list);
        currentTrackIndex = Math.max(0, idx);
        mixCurPos = currentTrackIndex;
        // La siguiente precargada puede haber cambiado: Automix la vuelve a elegir
        autoMixer.invalidateNext();
        updatePreviews();
        updateTrackSource(getCurrentTrack()); // "2 de 4" cambia al agregar o quitar
    }

    /** Reproduce Mi Mix desde una posición. fromStart: botón Reproducir (borra las marcas ✓). */
    private void playMix(int position, boolean fromStart) {
        List<Track> list = mixTracks();
        if (list.isEmpty()) return;
        if (fromStart) mixPlayedKeys.clear();
        playingFromMix = true;
        playQueue.clear();
        playQueue.addAll(list);
        int index = Math.max(0, Math.min(position, list.size() - 1));
        playTrackFromQueue(playQueue, index, true);
    }

    /** Siguiente dentro de Mi Mix, o -1 si ya no queda ninguna (la lista se detiene). */
    private int nextMixIndex() {
        List<Track> queue = getQueue();
        if (isShuffle) {
            // En aleatorio: una que todavía no haya sonado
            List<Integer> pending = new ArrayList<>();
            for (int i = 0; i < queue.size(); i++) {
                if (i != currentTrackIndex && !mixPlayedKeys.contains(favoriteKey(queue.get(i)))) pending.add(i);
            }
            return pending.isEmpty() ? -1 : pending.get(new Random().nextInt(pending.size()));
        }
        return currentTrackIndex + 1 < queue.size() ? currentTrackIndex + 1 : -1;
    }

    /** Terminó la última canción de Mi Mix: se detiene y queda en pausa al inicio de esa canción. */
    private void finishMix() {
        autoMixer.onManualPlayback();
        if (playerReady()) {
            try {
                videoPlayer.pause();
                clockRun(false);
                seekPlayerTo(0);
            } catch (Exception ignored) {}
        }
        isPlaying = false;
        if (seekTrackProgress != null) seekTrackProgress.setProgress(0);
        if (tvTimeCurrent != null) tvTimeCurrent.setText(formatMs(0));
        updatePlaybackStateUI();
        showMixToast("Fin de Mi Lista");
    }

    /** Agrega una carpeta entera a Mi Mix y avisa cuántas entraron. */
    private void addFolderToMix(List<Track> tracks, String folderName) {
        int added = addToMix(tracks);
        if (added > 0) {
            showMixToast("+" + added + (added == 1 ? " video" : " videos") + " de " + folderName + " a Mi Lista");
        } else {
            showMixToast(folderName + " ya está en Mi Lista");
        }
    }

    /** Botón de Mi Mix de una fila: ✕ / ✓ quitan, + agrega; el + de la que suena fuera de la lista la devuelve. */
    private void onTrackMixButton(Track track) {
        if (isMixOrphan(track)) {
            reAddToMixInPlace(track);
            showMixToast("Vuelve a Mi Lista");
        } else if (isInMix(track)) {
            removeFromMix(track);
            showMixToast("Quitado de Mi Lista");
        } else {
            addToMix(Collections.singletonList(track));
            showMixToast(playingFromMix ? "+1 al final de Mi Lista" : "+1 a Mi Lista");
        }
    }

    /** Muestra la pestaña Mi Mix (sin búsqueda): cabecera, la que suena fuera de la lista y las canciones. */
    private void showMixTab() {
        List<Track> list = mixTracks();
        displayedTracks.clear();
        Track current = playingFromMix ? getCurrentTrack() : null;
        boolean orphan = current != null && !isInMix(current);
        if (orphan) displayedTracks.add(current);
        displayedTracks.addAll(list);
        showDisplayedTracks();
        if (recyclerMixContent != null) {
            listViewContent.setVisibility(View.GONE);
            recyclerMixContent.setVisibility(View.VISIBLE);
            mixRecyclerAdapter.setCurrentPlayingTrack(getCurrentTrack());
            mixRecyclerAdapter.setReorderMode(isReorderingMix);
            mixRecyclerAdapter.notifyDataSetChanged();
        } else {
            if (listViewContent.getAdapter() != trackAdapter) {
                listViewContent.setAdapter(trackAdapter);
            }
            trackAdapter.setCurrentPlayingTrack(getCurrentTrack());
            trackAdapter.setReorderMode(isReorderingMix);
        }
        tvCurrentPath.setText("🎧 Mi Lista (" + list.size() + ")");
        updateMixHeader(list);

        if (tvMixEmpty != null) {
            tvMixEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            tvMixEmpty.setText(orphan
                    ? "Tu Mi Lista está vacía\n\nLo que agregues con + se verá después del video actual.\nSi no agregas nada, se detiene al terminar."
                    : "Tu Mi Lista está vacía\n\nToca + en un video, o en una carpeta para agregarla entera.\nDespués reprodúcela y se ve todo seguido.");
        }
    }

    /** Resumen ("8 canciones · 30 min · 2 ✓ ya sonaron") y botones de la cabecera de Mi Mix. */
    private void updateMixHeader(List<Track> list) {
        if (layoutMixHeader == null) return;
        Track current = playingFromMix ? getCurrentTrack() : null;
        if (mixConfirmClear) {
            tvMixHeaderTitle.setText("¿Vaciar Mi Lista?");
            tvMixHeaderSub.setText("Se quitan los " + list.size() + ". El que se está viendo sigue.");
            btnMixPlay.setVisibility(View.GONE);
            btnMixClear.setVisibility(View.GONE);
            if (btnMixReorder != null) btnMixReorder.setVisibility(View.GONE);
            if (btnMixReorderDone != null) btnMixReorderDone.setVisibility(View.GONE);
            btnMixConfirmClear.setVisibility(View.VISIBLE);
            btnMixCancelClear.setVisibility(View.VISIBLE);
        } else if (isReorderingMix) {
            tvMixHeaderTitle.setText("REORDENAR MI LISTA");
            tvMixHeaderSub.setText("Arrastra la manija ≡ para cambiar el orden");
            btnMixPlay.setVisibility(View.GONE);
            btnMixClear.setVisibility(View.GONE);
            if (btnMixReorder != null) btnMixReorder.setVisibility(View.GONE);
            btnMixConfirmClear.setVisibility(View.GONE);
            btnMixCancelClear.setVisibility(View.GONE);
            if (btnMixReorderDone != null) btnMixReorderDone.setVisibility(View.VISIBLE);
        } else {
            SpannableStringBuilder title = new SpannableStringBuilder("MI LISTA");
            if (playingFromMix) {
                int start = title.length();
                title.append("   ● REPRODUCIENDO");
                title.setSpan(new ForegroundColorSpan(Color.rgb(0, 210, 255)), start, title.length(), 0);
                title.setSpan(new RelativeSizeSpan(0.7f), start, title.length(), 0);
            }
            tvMixHeaderTitle.setText(title);
            if (list.isEmpty()) {
                tvMixHeaderSub.setText("Vacío");
            } else {
                int totalSec = 0;
                int played = 0;
                for (Track t : list) {
                    totalSec += Math.max(0, t.getDurationSec());
                    if (mixPlayedKeys.contains(favoriteKey(t)) && t != current) played++;
                }
                int h = totalSec / 3600;
                int m = Math.round((totalSec % 3600) / 60f);
                String length = h > 0 ? h + " h " + m + " min" : m + " min";
                tvMixHeaderSub.setText(list.size() + (list.size() == 1 ? " video" : " videos") + " · " + length
                        + (played > 0 ? " · " + played + " vistos" : ""));
            }
            btnMixPlay.setVisibility(View.VISIBLE);
            btnMixClear.setVisibility(View.VISIBLE);
            if (btnMixReorder != null) {
                boolean hasSearch = etSearch != null && !etSearch.getText().toString().trim().isEmpty();
                btnMixReorder.setVisibility(hasSearch ? View.GONE : View.VISIBLE);
                btnMixReorder.setEnabled(list.size() > 1 && !hasSearch);
                btnMixReorder.setAlpha(list.size() > 1 && !hasSearch ? 1f : 0.4f);
            }
            if (btnMixReorderDone != null) btnMixReorderDone.setVisibility(View.GONE);
            btnMixConfirmClear.setVisibility(View.GONE);
            btnMixCancelClear.setVisibility(View.GONE);

            btnMixPlay.setEnabled(!list.isEmpty());
            btnMixPlay.setAlpha(list.isEmpty() ? 0.4f : 1f);
            btnMixClear.setEnabled(!list.isEmpty());
            btnMixClear.setAlpha(list.isEmpty() ? 0.4f : 1f);
        }
    }

    private void setupMixHeader() {
        if (layoutMixHeader == null) return;
        btnMixPlay.setOnClickListener(v -> {
            hideKeyboard();
            playMix(0, true);
        });
        if (btnMixReorder != null) {
            btnMixReorder.setOnClickListener(v -> {
                hideKeyboard();
                isReorderingMix = true;
                showMixTab();
                updateMixHeader(mixTracks());
            });
        }
        if (btnMixReorderDone != null) {
            btnMixReorderDone.setTextColor(Color.parseColor("#0c0e12"));
            btnMixReorderDone.setText("✓ Listo");
            btnMixReorderDone.setOnClickListener(v -> {
                hideKeyboard();
                isReorderingMix = false;
                saveReorderedMixFromDisplayedTracks();
                showMixTab();
                updateMixHeader(mixTracks());
                showMixToast("Nuevo orden guardado");
            });
        }
        btnMixClear.setOnClickListener(v -> {
            mixConfirmClear = true;
            updateMixHeader(mixTracks());
        });
        btnMixConfirmClear.setOnClickListener(v -> {
            mixConfirmClear = false;
            clearMix();
            showMixToast("Mi Lista vacía");
        });
        btnMixCancelClear.setOnClickListener(v -> {
            mixConfirmClear = false;
            updateMixHeader(mixTracks());
        });
    }

    /**
     * Reordena una canción de Mi Mix de una posición a otra al arrastrar.
     * Sincroniza la lista y persiste el nuevo orden atómicamente.
     */
    private void moveMixTrack(int fromAdapterPos, int toAdapterPos) {
        if (fromAdapterPos == toAdapterPos) return;
        if (fromAdapterPos < 0 || fromAdapterPos >= displayedTracks.size() ||
                toAdapterPos < 0 || toAdapterPos >= displayedTracks.size()) return;

        Track moved = displayedTracks.remove(fromAdapterPos);
        displayedTracks.add(toAdapterPos, moved);
        trackAdapter.notifyDataSetChanged();
        saveReorderedMixFromDisplayedTracks();
    }

    /**
     * Guarda el nuevo orden de Mi Mix en base a la lista mostrada en pantalla tras arrastrar y soltar.
     * Sincroniza myMixKeys, SharedPreferences y la cola de reproducción de forma atómica y segura.
     */
    private void saveReorderedMixFromDisplayedTracks() {
        if (displayedTracks == null || displayedTracks.isEmpty()) return;

        Track current = playingFromMix ? getCurrentTrack() : null;
        boolean orphan = current != null && !isInMix(current);
        int startIndex = orphan ? 1 : 0;

        List<String> newOrderKeys = new ArrayList<>();
        for (int i = startIndex; i < displayedTracks.size(); i++) {
            Track t = displayedTracks.get(i);
            if (t != null) {
                String key = favoriteKey(t);
                if (key != null && !key.isEmpty() && !newOrderKeys.contains(key)) {
                    newOrderKeys.add(key);
                }
            }
        }

        // Conservar al final las canciones de pendrives que no estén conectados ahora
        for (String oldKey : myMixKeys) {
            if (!newOrderKeys.contains(oldKey)) {
                newOrderKeys.add(oldKey);
            }
        }

        myMixKeys.clear();
        myMixKeys.addAll(newOrderKeys);
        saveMyMix();
        syncMixQueue();
        updateMixHeader(mixTracks());
    }

    // Aviso corto al pie del explorador ("+1 a Mi Mix", "Fin de Mi Mix"...)
    private TextView mixToastView;
    private final Runnable hideMixToastRunnable = () -> {
        if (mixToastView != null) mixToastView.setVisibility(View.GONE);
    };

    private void showMixToast(String text) {
        FrameLayout root = findViewById(android.R.id.content);
        if (root == null) return;
        float density = getResources().getDisplayMetrics().density;
        if (mixToastView == null) {
            mixToastView = new TextView(this);
            mixToastView.setTextColor(Color.parseColor("#e2e8f0"));
            mixToastView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            mixToastView.setTypeface(null, Typeface.BOLD);
            mixToastView.setSingleLine(true);
            mixToastView.setPadding(Math.round(20 * density), Math.round(10 * density),
                    Math.round(20 * density), Math.round(10 * density));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12 * density);
            bg.setColor(Color.argb(240, 12, 16, 24));
            bg.setStroke(Math.max(1, Math.round(density)), Color.argb(128, 56, 189, 248));
            mixToastView.setBackground(bg);
            mixToastView.setClickable(false);
            root.addView(mixToastView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }
        mixToastView.setText(text);
        mixToastView.setVisibility(View.VISIBLE);
        mixToastView.bringToFront();

        // Centrado abajo del explorador (o de la pantalla si el explorador está oculto)
        mixToastView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        View anchor = layoutExplorerPanel != null && layoutExplorerPanel.getVisibility() == View.VISIBLE
                && layoutExplorerPanel.getWidth() > 0 ? layoutExplorerPanel : root;
        int[] anchorLoc = new int[2];
        int[] rootLoc = new int[2];
        anchor.getLocationInWindow(anchorLoc);
        root.getLocationInWindow(rootLoc);
        float centerX = anchorLoc[0] - rootLoc[0] + anchor.getWidth() / 2f;
        float bottom = anchorLoc[1] - rootLoc[1] + anchor.getHeight() - 56 * density;
        mixToastView.setX(centerX - mixToastView.getMeasuredWidth() / 2f);
        mixToastView.setY(bottom - mixToastView.getMeasuredHeight());

        handler.removeCallbacks(hideMixToastRunnable);
        handler.postDelayed(hideMixToastRunnable, 1600);
    }

    // ==========================================
    // DURACIÓN REAL DE LAS PISTAS EN SEGUNDO PLANO
    // Las que MediaStore no conoce se leen del archivo, primero las de la lista visible
    // ==========================================
    private void showDisplayedTracks() {
        trackAdapter.setTracks(displayedTracks);
        prioritizeDurations(displayedTracks);
    }

    private void startDurationFill(List<Track> tracks) {
        final int generation = ++durationGeneration;
        synchronized (pendingDurations) {
            pendingDurations.clear();
            visibleDurationTracks.clear();
            for (Track t : tracks) {
                if (!t.hasDuration()) pendingDurations.add(t);
            }
            if (pendingDurations.isEmpty()) return;
        }
        durationExecutor.execute(() -> fillDurations(generation));
    }

    /** Pasa al frente de la cola las pistas de la lista que se está mostrando. */
    private void prioritizeDurations(List<Track> visible) {
        synchronized (pendingDurations) {
            visibleDurationTracks.clear();
            for (int i = visible.size() - 1; i >= 0; i--) {
                Track t = visible.get(i);
                if (t.hasDuration()) continue;
                visibleDurationTracks.add(t);
                if (pendingDurations.remove(t)) pendingDurations.addFirst(t);
            }
        }
    }

    private static final long DURATION_GAP_WHILE_PLAYING_MS = 700;

    private void fillDurations(int generation) {
        // Prioridad baja: la pantalla y el video van primero
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        long lastRefresh = System.currentTimeMillis();
        boolean visibleChanged = false;
        while (generation == durationGeneration) {
            Track t;
            boolean isVisible;
            synchronized (pendingDurations) {
                t = pendingDurations.pollFirst();
                isVisible = t != null && visibleDurationTracks.contains(t);
            }
            if (t == null) break;
            if (t.hasDuration()) continue;
            readDurationFromFile(t);
            visibleChanged |= isVisible;
            // Con un video sonando, una lectura cada 0,7 s: no le quita el USB al video
            if (isPlaying) {
                try {
                    Thread.sleep(DURATION_GAP_WHILE_PLAYING_MS);
                } catch (InterruptedException e) {
                    return;
                }
            }

            long now = System.currentTimeMillis();
            if (visibleChanged && now - lastRefresh > 500) {
                lastRefresh = now;
                visibleChanged = false;
                // handler.post publica en el hilo de UI los valores escritos aquí
                handler.post(() -> {
                    trackAdapter.notifyDataSetChanged();
                    if (mixRecyclerAdapter != null) mixRecyclerAdapter.notifyDataSetChanged();
                });
            }
        }
        if (visibleChanged && generation == durationGeneration) {
            handler.post(() -> {
                trackAdapter.notifyDataSetChanged();
                if (mixRecyclerAdapter != null) mixRecyclerAdapter.notifyDataSetChanged();
            });
        }
    }

    private void readDurationFromFile(Track t) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(t.getFilePath());
            String durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durMs != null) {
                int sec = (int) (Long.parseLong(durMs) / 1000);
                if (sec > 0) t.setDurationSec(sec);
            }
            String bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null) {
                int kbps = Math.round(Integer.parseInt(bitrate) / 1000f);
                if (kbps > 0) t.setFormat(getExtension(t.getFilePath()) + " " + kbps + "K");
            }
        } catch (Exception ignored) {
            // Archivo dañado o pendrive retirado: queda como "--:--"
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {}
        }
        if (!t.hasDuration()) {
            // MP4 fragmentado (los que bajan de YouTube): Android no sabe cuánto dura; lo dice la tabla del final
            int ms = FragmentedMp4Index.readDurationMs(t.getFilePath());
            if (ms > 0) t.setDurationSec(ms / 1000);
        }
    }

    private static void remapTracks(List<Track> list, Map<String, Track> tracksByPath) {
        List<Track> remapped = new ArrayList<>(list.size());
        for (Track t : list) {
            Track fresh = tracksByPath.get(t.getFilePath());
            if (fresh != null) remapped.add(fresh);
        }
        list.clear();
        list.addAll(remapped);
    }

    private void playTrackFromQueue(List<Track> queue, int index) {
        playTrackFromQueue(queue, index, false);
    }

    private void playTrackFromQueue(List<Track> queue, int index, boolean quickFade) {
        if (queue.isEmpty() || index < 0 || index >= queue.size()) return;
        currentTrackIndex = index;
        startPlayback(queue.get(index), quickFade);
    }

    private void togglePlayPause() {
        if (videoPlayer == null) {
            // Nada abierto (o el reproductor falló): vuelve a abrir el video actual
            List<Track> queue = getQueue();
            if (!queue.isEmpty()) {
                playTrackFromQueue(queue, currentTrackIndex >= 0 && currentTrackIndex < queue.size() ? currentTrackIndex : 0);
            }
            return;
        }
        if (!playerPrepared) return; // el video todavía se está abriendo

        if (videoPlayer.isPlaying()) {
            // Pausa en medio de una mezcla: quedarse solo con la canción nueva
            autoMixer.finishMixNow();
            videoPlayer.pause();
            clockRun(false);
            isPlaying = false;
        } else {
            videoPlayer.start();
            clockRun(true);
            isPlaying = true;
        }
        updatePlaybackStateUI();
    }

    private void playNextTrack() {
        // Automix: si la siguiente ya está precargada, usarla directo (instantáneo con pendrive lento)
        if (autoMixer.skipToPreparedNext()) return;
        List<Track> queue = getQueue();
        if (queue.isEmpty()) return;
        if (playingFromMix) {
            // Mi Mix no vuelve a empezar: después de la última se detiene
            int mixNext = nextMixIndex();
            if (mixNext < 0) {
                finishMix();
            } else {
                playTrackFromQueue(queue, mixNext, true);
            }
            return;
        }
        int nextIndex;
        if (isShuffle) {
            nextIndex = new Random().nextInt(queue.size());
        } else {
            nextIndex = (currentTrackIndex + 1) % queue.size();
        }
        playTrackFromQueue(queue, nextIndex, true);
    }

    private void playPreviousTrack() {
        List<Track> queue = getQueue();
        if (queue.isEmpty()) return;
        // En Mi Mix, Anterior en la primera la vuelve a empezar (no salta a la última)
        int prevIndex = playingFromMix ? Math.max(0, currentTrackIndex - 1)
                : (currentTrackIndex - 1 + queue.size()) % queue.size();
        playTrackFromQueue(queue, prevIndex, true);
    }

    /** 1 toque = siguiente canción; 2 toques (doble touch) = adelanta 10 segundos */
    private void handleNextButtonClick() {
        if (nextTapRunnable != null) {
            navTapHandler.removeCallbacks(nextTapRunnable);
            nextTapRunnable = null;
        }
        nextTapHits++;
        if (nextTapHits == 1) {
            nextTapRunnable = () -> {
                nextTapHits = 0;
                nextTapRunnable = null;
                playNextTrack();
            };
            navTapHandler.postDelayed(nextTapRunnable, DOUBLE_TAP_TIMEOUT_MS);
        } else {
            // Doble toque (o toques adicionales seguidos): avanza 10s
            seekRelative(SEEK_SKIP_MS, nextTapHits - 1);
            navTapHandler.removeCallbacks(resetNextTapHitsRunnable);
            navTapHandler.postDelayed(resetNextTapHitsRunnable, 450);
        }
    }

    /** 1 toque = canción anterior; 2 toques (doble touch) = retrocede 10 segundos */
    private void handlePrevButtonClick() {
        if (prevTapRunnable != null) {
            navTapHandler.removeCallbacks(prevTapRunnable);
            prevTapRunnable = null;
        }
        prevTapHits++;
        if (prevTapHits == 1) {
            prevTapRunnable = () -> {
                prevTapHits = 0;
                prevTapRunnable = null;
                playPreviousTrack();
            };
            navTapHandler.postDelayed(prevTapRunnable, DOUBLE_TAP_TIMEOUT_MS);
        } else {
            // Doble toque (o toques adicionales seguidos): retrocede 10s
            seekRelative(-SEEK_SKIP_MS, prevTapHits - 1);
            navTapHandler.removeCallbacks(resetPrevTapHitsRunnable);
            navTapHandler.postDelayed(resetPrevTapHitsRunnable, 450);
        }
    }

    // Aviso +10s / -10s igual que la web (#seekFeedbackToast): recuadro oscuro con borde cian,
    // centrado en el reproductor al 45% de su alto; aparece con un leve agrandado y se apaga a los 0,8 s.
    // Animado con Handler (no con Animator) por los radios con animaciones desactivadas.
    private static final int SEEK_FEEDBACK_SHOW_MS = 800;
    private static final int SEEK_FEEDBACK_FADE_MS = 250;
    private TextView seekFeedbackView;
    private long seekFeedbackStartMs;
    private boolean seekFeedbackHiding;

    private final Runnable seekFeedbackRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - seekFeedbackStartMs;
            float t;
            if (!seekFeedbackHiding) {
                t = Math.min(1f, elapsed / (float) SEEK_FEEDBACK_FADE_MS);
                seekFeedbackView.setAlpha(t);
                float scale = 1f + 0.05f * t;               // crece hasta 1,05
                seekFeedbackView.setScaleX(scale);
                seekFeedbackView.setScaleY(scale);
                if (elapsed >= SEEK_FEEDBACK_SHOW_MS) {
                    seekFeedbackHiding = true;
                    seekFeedbackStartMs = SystemClock.uptimeMillis();
                }
            } else {
                t = Math.min(1f, elapsed / (float) SEEK_FEEDBACK_FADE_MS);
                seekFeedbackView.setAlpha(1f - t);
                float scale = 1.05f - 0.1f * t;              // se achica hasta 0,95
                seekFeedbackView.setScaleX(scale);
                seekFeedbackView.setScaleY(scale);
                if (t >= 1f) {
                    seekFeedbackView.setVisibility(View.GONE);
                    return;
                }
            }
            handler.postDelayed(this, 30);
        }
    };

    private void showSeekFeedback(String text) {
        FrameLayout root = findViewById(android.R.id.content);
        if (root == null) return;
        float density = getResources().getDisplayMetrics().density;
        if (seekFeedbackView == null) {
            seekFeedbackView = new TextView(this);
            seekFeedbackView.setTextColor(Color.rgb(0, 210, 255));
            seekFeedbackView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            seekFeedbackView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            seekFeedbackView.setPadding(Math.round(22 * density), Math.round(10 * density),
                    Math.round(22 * density), Math.round(10 * density));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(14 * density);
            bg.setColor(Color.argb(235, 12, 16, 24));
            bg.setStroke(Math.max(1, Math.round(density)), Color.argb(115, 0, 210, 255));
            seekFeedbackView.setBackground(bg);
            seekFeedbackView.setClickable(false);
            seekFeedbackView.setVisibility(View.GONE);
            root.addView(seekFeedbackView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        }
        seekFeedbackView.setText(text);
        seekFeedbackView.setVisibility(View.VISIBLE);
        seekFeedbackView.bringToFront();

        // Centrar en el reproductor (como en la web: left 50%, top 45%)
        seekFeedbackView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        View anchor = layoutPlayerPanel != null && layoutPlayerPanel.getWidth() > 0 ? layoutPlayerPanel : root;
        int[] anchorLoc = new int[2];
        int[] rootLoc = new int[2];
        anchor.getLocationInWindow(anchorLoc);
        root.getLocationInWindow(rootLoc);
        float centerX = anchorLoc[0] - rootLoc[0] + anchor.getWidth() / 2f;
        float centerY = anchorLoc[1] - rootLoc[1] + anchor.getHeight() * 0.45f;
        seekFeedbackView.setX(centerX - seekFeedbackView.getMeasuredWidth() / 2f);
        seekFeedbackView.setY(centerY - seekFeedbackView.getMeasuredHeight() / 2f);

        // Reiniciar la animación (toques seguidos actualizan el texto: +10s, +20s...)
        handler.removeCallbacks(seekFeedbackRunnable);
        seekFeedbackHiding = false;
        seekFeedbackStartMs = SystemClock.uptimeMillis() - (seekFeedbackView.getAlpha() > 0.5f ? SEEK_FEEDBACK_FADE_MS : 0);
        handler.post(seekFeedbackRunnable);
    }

    /** Adelanta o retrocede deltaMs milisegundos con feedback visual */
    private void seekRelative(int deltaMs, int consecutiveHits) {
        if (!playerReady()) return;
        try {
            autoMixer.finishMixNow();
            int current = playbackPositionMs();
            int duration = playbackDurationMs();
            int target = current + deltaMs;
            if (target < 0) target = 0;
            if (duration > 0 && target > duration) target = Math.max(0, duration - 1000);
            if (!seekPlayerTo(target)) return;

            // Reflejar cambio inmediato en barra de progreso y reloj
            if (duration > 0) {
                int progress = (int) (((float) target / duration) * 1000);
                seekTrackProgress.setProgress(progress);
                tvTimeCurrent.setText(formatMs(target));
                updateFsProgress(target, duration);
            }

            // Aviso breve centrado en el reproductor (solo texto: +10s / -10s)
            int totalSec = (deltaMs > 0 ? 10 : -10) * Math.max(1, consecutiveHits);
            showSeekFeedback(totalSec > 0 ? ("+" + totalSec + "s") : (totalSec + "s"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePreviews() {
        List<Track> queue = getQueue();
        if (queue.isEmpty()) return;
        int prevIdx = (currentTrackIndex - 1 + queue.size()) % queue.size();
        int nextIdx = (currentTrackIndex + 1) % queue.size();

        tvPrevTrackPreview.setText(queue.get(prevIdx).getTitle());
        if (playingFromMix && !isShuffle && currentTrackIndex >= queue.size() - 1) {
            tvNextTrackPreview.setText("■ Fin de Mi Lista");
        } else {
            tvNextTrackPreview.setText(queue.get(nextIdx).getTitle());
        }
    }

    private void loadAlbumArt(Track track) {
        if (track == null) return;
        final String filePath = track.getFilePath();
        scanExecutor.execute(() -> {
            Bitmap art = null;
            String exactFormat = null;
            String embeddedArtist = null;
            int durationMs = 0;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(filePath);
                String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) {
                    try {
                        durationMs = (int) Long.parseLong(durStr.trim());
                    } catch (Exception ignored) {}
                }
                byte[] raw = mmr.getEmbeddedPicture();
                if (raw != null) {
                    art = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                }
                String bitrateStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                if (bitrateStr != null && !bitrateStr.isEmpty()) {
                    try {
                        int bitrate = Integer.parseInt(bitrateStr);
                        if (bitrate > 0) {
                            int kbps = Math.round(bitrate / 1000f);
                            if (kbps > 0) {
                                String ext = getExtension(filePath);
                                exactFormat = ext + " " + kbps + "K";
                            }
                        }
                    } catch (Exception ignored) {}
                }
                embeddedArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                if (embeddedArtist == null || embeddedArtist.trim().isEmpty()) {
                    embeddedArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
                }
                mmr.release();
            } catch (Exception ignored) {}
            if (durationMs <= 0) durationMs = readDurationWithExtractor(filePath);
            if (durationMs <= 0) durationMs = FragmentedMp4Index.readDurationMs(filePath);

            final Bitmap finalArt = art;
            final int finalDuration = durationMs;
            final String finalFormat = exactFormat;
            final String finalArtist = (embeddedArtist != null && !embeddedArtist.trim().isEmpty()) ? embeddedArtist.trim() : null;
            handler.post(() -> {
                // En una mezcla automática la carátula cambia con fundido cruzado
                applyFileDuration(filePath, track, finalDuration);
                boolean smooth = filePath != null && filePath.equals(albumArtFadeForPath);
                if (smooth) albumArtFadeForPath = null;
                if (finalArt != null) {
                    showAlbumArt(finalArt, smooth);
                } else {
                    // Video sin imagen incluida: el color sale de un cuadro del video cuando ya se esté viendo
                    ambientCapturePath = filePath;
                    handler.removeCallbacks(ambientCaptureRunnable);
                    handler.postDelayed(ambientCaptureRunnable, AMBIENT_CAPTURE_DELAY_MS);
                }
                if (finalFormat != null) {
                    track.setFormat(finalFormat);
                    List<Track> queue = getQueue();
                    if (currentTrackIndex >= 0 && currentTrackIndex < queue.size()) {
                        if (filePath.equals(queue.get(currentTrackIndex).getFilePath())) {
                            tvFormatBadge.setText(finalFormat);
                        }
                    }
                }
                if (finalArtist != null) {
                    if (track.getArtist() == null || track.getArtist().trim().isEmpty() || "Pendrive".equalsIgnoreCase(track.getArtist())) {
                        track.setArtist(finalArtist);
                        List<Track> queue = getQueue();
                        if (currentTrackIndex >= 0 && currentTrackIndex < queue.size()) {
                            if (filePath.equals(queue.get(currentTrackIndex).getFilePath())) {
                                updateNowPlayingInfo(track);
                            }
                        }
                    }
                }
            });
        });
    }

    private void updateNowPlayingInfo(Track track) {
        if (track == null) return;
        tvTrackTitle.setText(track.getTitle());
        String art = track.getArtist();
        if (art != null && !art.trim().isEmpty() && !"Pendrive".equalsIgnoreCase(art.trim())) {
            tvTrackArtist.setVisibility(View.VISIBLE);
            tvTrackArtist.setText(art);
            if (tvTrackArtistDot != null) tvTrackArtistDot.setVisibility(View.VISIBLE);
        } else {
            tvTrackArtist.setVisibility(View.GONE);
            if (tvTrackArtistDot != null) tvTrackArtistDot.setVisibility(View.GONE);
        }
        updateTrackSource(track);
        tvFormatBadge.setText(track.getFormat());
        if (tvFsTitle != null) {
            tvFsTitle.setText(art != null && !art.trim().isEmpty() && !"Pendrive".equalsIgnoreCase(art.trim())
                    ? track.getTitle() + " • " + art.trim() : track.getTitle());
        }
        monitorShownText = null;
        updateLoadMonitor();

        if (folderTreeAdapter != null) {
            folderTreeAdapter.setCurrentPlayingTrack(track);
            revealPlayingFolderInTree(track);
        }
    }

    private void loadAlbumArt(String filePath) {
        if (filePath != null) {
            Track t = new Track(filePath, "", "", "", "", "", "MP3 320K", 0, "");
            loadAlbumArt(t);
        }
    }

    private void setupProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (playerReady() && isPlaying && !isUserSeeking) {
                    try {
                        checkPlayerPosition(rawPlayerPosition());
                        int current = playbackPositionMs();
                        int total = playbackDurationMs();
                        watchStall(current);
                        tvTimeCurrent.setText(formatMs(current));
                        if (total != shownTotalMs) {
                            shownTotalMs = total;
                            tvTimeTotal.setText(total > 0 ? formatMs(total) : "--:--");
                        }
                        if (total > 0) {
                            int progress = (int) (((float) current / total) * 1000);
                            seekTrackProgress.setProgress(progress);
                            updateFsProgress(current, total);
                        } else if (tvFsTime != null && !isFsSeeking) {
                            tvFsTime.setText(formatMs(current) + " / --:--");
                        }
                    } catch (Exception ignored) {}
                } else {
                    resetStallWatch();
                }
                updateLoadMonitor();
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private String formatMs(int ms) {
        int totalSec = Math.max(0, ms) / 1000;
        int h = totalSec / 3600;
        int m = (totalSec % 3600) / 60;
        int s = totalSec % 60;
        // Películas y conciertos con horas (1:12:30), como la web
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    // ==========================================
    // ESCANEO TOTAL Y NATIVO DE PENDRIVES USB
    // ==========================================
    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERM_REQUEST_CODE);
                return;
            }
        }
        startUsbScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST_CODE) {
            startUsbScan();
        }
    }

    private void startUsbScan() {
        tvStorageStatus.setText("Escaneando USB 1 y USB 2...");

        scanExecutor.execute(() -> {
            List<Track> foundTracks = new ArrayList<>();
            Set<String> seenPaths = new HashSet<>();

            // Montajes de Pendrives USB en plataformas de auto (AC8227L, Allwinner, Rockchip, MTK, etc.)
            String[] targetUsbRoots = {
                    "/storage/udisk1",
                    "/storage/udisk2",
                    "/storage/udisk3",
                    "/storage/usb0",
                    "/storage/usb1",
                    "/mnt/media_rw/udisk1",
                    "/mnt/media_rw/udisk2",
                    "/mnt/udisk1",
                    "/mnt/udisk2"
            };

            // Además de las rutas conocidas, cualquier memoria montada en /storage: algunos radios montan el
            // pendrive con su número de serie (p. ej. /storage/1A2B-3C4D)
            ScanStats stats = new ScanStats();
            List<String> rootPaths = new ArrayList<>();
            boolean usb1Taken = false;
            for (File dir : findUsbRoots(targetUsbRoots)) {
                String rootPath = dir.getAbsolutePath();
                String lowerRoot = rootPath.toLowerCase();
                String usbTag;
                if (lowerRoot.contains("udisk2") || lowerRoot.contains("usb1")) {
                    usbTag = "USB 2";
                } else if (lowerRoot.contains("udisk") || lowerRoot.contains("usb") || !usb1Taken) {
                    usbTag = "USB 1";
                } else {
                    usbTag = "USB 2";
                }
                if ("USB 1".equals(usbTag)) usb1Taken = true;
                rootPaths.add(rootPath);
                stats.roots.add(dir.getName());
                scanDirectory(dir, foundTracks, seenPaths, usbTag, 0, stats);
            }
            scanRootPaths = rootPaths;

            // Escaneo complementario mediante MediaStore (SOLO pistas ubicadas en USB 1 o USB 2)
            Map<String, Integer> mediaStoreDurations = new HashMap<>();
            try {
                Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String[] projection = {
                        MediaStore.Video.Media.TITLE,
                        MediaStore.Video.Media.ARTIST,
                        MediaStore.Video.Media.ALBUM,
                        MediaStore.Video.Media.DURATION,
                        MediaStore.Video.Media.DATA
                };

                Cursor cursor = getContentResolver().query(videoUri, projection, null, null, null);

                if (cursor != null) {
                    int titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
                    int artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST);
                    int albumCol = cursor.getColumnIndex(MediaStore.Video.Media.ALBUM);
                    int durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
                    int dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

                    while (cursor.moveToNext()) {
                        String path = cursor.getString(dataCol);
                        // Pista ya encontrada recorriendo carpetas: solo aprovechar su duración real
                        if (path != null && seenPaths.contains(path)) {
                            long durMs = cursor.getLong(durCol);
                            if (durMs > 0) mediaStoreDurations.put(path, (int) (durMs / 1000));
                            continue;
                        }
                        // Filtro estricto: solo incluir canciones que provengan de pendrives USB
                        if (path != null && (path.contains("udisk") || path.contains("usb") || isUnderAnyRoot(path, rootPaths))
                                && !seenPaths.contains(path)) {
                            File f = new File(path);
                            if (f.exists()) {
                                seenPaths.add(path);
                                String title = cursor.getString(titleCol);
                                String artist = cursor.getString(artistCol);
                                String album = cursor.getString(albumCol);
                                int durSec = (int) (cursor.getLong(durCol) / 1000);

                                if (title == null || title.isEmpty() || title.equalsIgnoreCase("<unknown>")) {
                                    title = f.getName().replaceFirst("[.][^.]+$", "");
                                }
                                if (artist == null || artist.equalsIgnoreCase("<unknown>")) {
                                    artist = "Artista";
                                }
                                String folder = f.getParentFile() != null ? f.getParentFile().getName() : "USB";
                                String usbTag = (path.contains("udisk2") || path.contains("usb1")) ? "USB 2" : "USB 1";

                                String format = buildFormatBadge(f.getName(), f.length(), durSec);
                                foundTracks.add(new Track(path, title, artist, album, folder, f.getParent(), format, durSec, usbTag));
                            }
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Duración real (y bitrate estimado con ella) para las pistas que MediaStore ya conoce
            for (Track t : foundTracks) {
                Integer durSec = t.hasDuration() ? null : mediaStoreDurations.get(t.getFilePath());
                if (durSec != null) {
                    t.setDurationSec(durSec);
                    File f = new File(t.getFilePath());
                    t.setFormat(buildFormatBadge(f.getName(), f.length(), durSec));
                }
            }

            // Agrupar carpetas
            Map<String, FolderItem> folderMap = new HashMap<>();
            for (Track t : foundTracks) {
                String fName = t.getFolder();
                if (!folderMap.containsKey(fName)) {
                    folderMap.put(fName, new FolderItem(fName, t.getPath(), 1, t.getUsbSource()));
                } else {
                    folderMap.get(fName).incrementCount();
                }
            }

            List<FolderItem> generatedFolders = new ArrayList<>(folderMap.values());

            // Precalcular aquí (hilo de escaneo) el texto normalizado del buscador, para que cada búsqueda solo compare
            for (Track t : foundTracks) t.getSearchKey();
            for (FolderItem f : generatedFolders) f.getSearchKey();

            boolean fUsb1 = false;
            boolean fUsb2 = false;
            for (Track t : foundTracks) {
                if ("USB 1".equalsIgnoreCase(t.getUsbSource()) || (t.getFilePath() != null && (t.getFilePath().contains("udisk1") || t.getFilePath().contains("usb0")))) {
                    fUsb1 = true;
                }
                if ("USB 2".equalsIgnoreCase(t.getUsbSource()) || (t.getFilePath() != null && (t.getFilePath().contains("udisk2") || t.getFilePath().contains("usb1")))) {
                    fUsb2 = true;
                }
                if (fUsb1 && fUsb2) break;
            }
            if (!fUsb1) fUsb1 = isUsbMounted(USB1_MOUNT_PATHS);
            if (!fUsb2) fUsb2 = isUsbMounted(USB2_MOUNT_PATHS);
            final boolean u1 = fUsb1;
            final boolean u2 = fUsb2;

            handler.post(() -> {
                hasUsb1 = u1;
                hasUsb2 = u2;
                updateFilterChipsVisibility();

                // Ordenar pistas alfabéticamente por título (A-Z)
                Collections.sort(foundTracks, (t1, t2) -> {
                    String s1 = t1.getTitle() != null ? t1.getTitle().trim() : "";
                    String s2 = t2.getTitle() != null ? t2.getTitle().trim() : "";
                    return s1.compareToIgnoreCase(s2);
                });

                // Ordenar carpetas alfabéticamente por nombre (A-Z)
                Collections.sort(generatedFolders, (f1, f2) -> {
                    String n1 = f1.getName() != null ? f1.getName().trim() : "";
                    String n2 = f2.getName() != null ? f2.getName().trim() : "";
                    return n1.compareToIgnoreCase(n2);
                });

                // Mi Mix guardado en el radio (la primera vez, con las estrellas de Favoritos)
                loadMyMix(foundTracks);
                Map<String, Track> tracksByPath = new HashMap<>();
                for (Track t : foundTracks) {
                    tracksByPath.put(t.getFilePath(), t);
                }

                allTracks.clear();
                allTracks.addAll(foundTracks);

                folderList.clear();
                folderList.addAll(generatedFolders);

                reconcileListsAfterRescan(tracksByPath);
                updateMixTabCount();
                startDurationFill(allTracks);

                buildTreeFromTracks(allTracks);
                updateVisibleTreeNodes();

                if (allTracks.isEmpty()) {
                    String why = describeEmptyScan(stats);
                    tvStorageStatus.setText(why);
                    Toast.makeText(this, why, Toast.LENGTH_LONG).show();
                } else {
                    tvStorageStatus.setText("Pendrives listos (" + folderList.size() + " carpetas, " + allTracks.size() + " videos)");
                }
                // Pistas solo cuenta la carpeta elegida (0 hasta que se elija una)
                updateTracksTabCount();

                filterByTab();

                if (!allTracks.isEmpty() && currentTrackIndex == -1) {
                    // Cargar primera canción en vista previa
                    Track first = allTracks.get(0);
                    tvTrackTitle.setText(first.getTitle());
                    tvTrackArtist.setText(first.getArtist());
                    tvTrackAlbum.setText(first.getAlbum());
                    tvFormatBadge.setText(first.getFormat());
                    tvTimeTotal.setText(first.getFormattedDuration());
                    loadAlbumArt(first);
                    updatePreviews();
                }
            });
        });
    }

    private void scanDirectory(File dir, List<Track> tracks, Set<String> seen, String usbTag, int depth, ScanStats stats) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 6) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!name.startsWith(".") && !name.equalsIgnoreCase("Android") && !name.equalsIgnoreCase("lost.dir") && !name.equalsIgnoreCase("System Volume Information")) {
                    scanDirectory(file, tracks, seen, usbTag, depth + 1, stats);
                }
            } else {
                String path = file.getAbsolutePath();
                stats.files++;
                if (!seen.contains(path)) {
                    String lower = file.getName().toLowerCase();
                    if (!isVideoFile(lower)) stats.countOther(lower);
                    if (isVideoFile(lower)) {
                        seen.add(path);

                        String cleanName = file.getName().replaceFirst("[.][^.]+$", "");
                        String title = cleanName;
                        String artist = "";
                        if (cleanName.contains("-")) {
                            String[] parts = cleanName.split("-", 2);
                            artist = parts[0].trim();
                            title = parts[1].trim();
                        }

                        String folder = file.getParentFile() != null ? file.getParentFile().getName() : "USB";
                        // Duración 0 = desconocida: se toma de MediaStore o se lee del archivo en segundo plano
                        String format = buildFormatBadge(file.getName(), file.length(), 0);
                        tracks.add(new Track(path, title, artist, folder, folder, file.getParent(), format, 0, usbTag));
                    }
                }
            }
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0) ? fileName.substring(dot + 1).toUpperCase() : "MP3";
    }

    private String buildFormatBadge(String fileName, long fileSize, int durSec) {
        String ext = getExtension(fileName);
        int kbps = 320; // Calidad estándar de referencia

        if (durSec > 0 && fileSize > 0) {
            int calculated = (int) ((fileSize * 8L) / (durSec * 1000L));
            if (calculated >= 32 && calculated <= 3000) {
                if (ext.equalsIgnoreCase("MP3")) {
                    if (calculated >= 285) kbps = 320;
                    else if (calculated >= 225) kbps = 256;
                    else if (calculated >= 175) kbps = 192;
                    else if (calculated >= 145) kbps = 160;
                    else if (calculated >= 115) kbps = 128;
                    else if (calculated >= 80) kbps = 96;
                    else kbps = calculated;
                } else if (ext.equalsIgnoreCase("FLAC") || ext.equalsIgnoreCase("WAV")) {
                    kbps = (calculated > 500) ? calculated : 1411;
                } else {
                    kbps = calculated;
                }
            }
        } else {
            if (ext.equalsIgnoreCase("FLAC") || ext.equalsIgnoreCase("WAV")) {
                kbps = 1411;
            } else if (ext.equalsIgnoreCase("M4A") || ext.equalsIgnoreCase("AAC")) {
                kbps = 256;
            }
        }
        return ext + " " + kbps + "K";
    }

    // ==========================================
    // MANDOS DEL VOLANTE DEL AUTO Y HARDWARE
    // ==========================================
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                togglePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                playNextTrack();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                playPreviousTrack();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                adjustVolumeStep(true);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                adjustVolumeStep(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                toggleMute();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ==========================================
    // JH VIDEO: SUPERFICIES DEL VIDEO Y PANTALLA COMPLETA
    // El mismo MediaPlayer dibuja en el cuadro del reproductor o en la pantalla completa (setSurface)
    // ==========================================
    private static final String[] VIDEO_EXTENSIONS = {
            ".mp4", ".mkv", ".webm", ".avi", ".3gp", ".mov", ".m4v", ".ts", ".mpg", ".mpeg", ".flv", ".wmv",
            ".vob", ".mts", ".m2ts", ".divx", ".asf", ".3g2", ".ogv", ".f4v"
    };
    private static final int FS_CONTROLS_HIDE_MS = 3000; // reproduciendo, los controles se ocultan solos

    private TextureView videoTexture;       // video en el reproductor (16:9 de borde a borde)
    private TextureView videoTextureFs;     // video en pantalla completa
    private Surface videoSurface;
    private Surface videoSurfaceFs;
    private Surface attachedVideoSurface;   // la que tiene el reproductor ahora
    private int videoWidth = 0;
    private int videoHeight = 0;
    private View layoutVideoFs;
    private View layoutFsControls;
    private TextView tvFsTitle, tvFsTime;
    private SeekBar seekFsProgress;
    private ImageButton btnFsPlay;
    private boolean playerPrepared = false; // el archivo actual abrió bien (se le pueden pedir tiempos y saltos)
    private Track openingTrack;             // el que se está abriendo (o el último que abrió)
    private boolean isVideoFs = false;
    private boolean isFsSeeking = false;
    private final Runnable hideFsControlsRunnable = () -> setFsControlsVisible(false);

    // Raíces de los pendrives del último escaneo (sin "/" al final)
    private volatile List<String> scanRootPaths = new ArrayList<>();

    /** Lo que vio el último escaneo, para explicar en pantalla por qué no hay videos. */
    private static final class ScanStats {
        final List<String> roots = new ArrayList<>();
        int files = 0;
        final Map<String, Integer> otherExts = new HashMap<>();

        void countOther(String lowerName) {
            int dot = lowerName.lastIndexOf('.');
            String ext = dot > 0 && dot < lowerName.length() - 1 ? lowerName.substring(dot + 1) : "sin extensión";
            Integer n = otherExts.get(ext);
            otherExts.put(ext, n == null ? 1 : n + 1);
        }
    }

    /** Pendrives montados: las rutas conocidas de radios de auto y cualquier otra memoria en /storage. */
    private static List<File> findUsbRoots(String[] knownRoots) {
        List<File> roots = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String p : knownRoots) addUsbRoot(new File(p), roots, seen);
        File[] mounts = new File("/storage").listFiles();
        if (mounts != null) {
            for (File mount : mounts) {
                String name = mount.getName();
                // emulated / self / sdcard0 = memoria interna del radio
                if (name.equals("emulated") || name.equals("self") || name.equals("sdcard0")) continue;
                addUsbRoot(mount, roots, seen);
            }
        }
        return roots;
    }

    private static void addUsbRoot(File dir, List<File> roots, Set<String> seen) {
        if (!dir.isDirectory() || dir.list() == null) return; // no montado o sin permiso
        String key;
        try {
            key = dir.getCanonicalPath();
        } catch (Exception e) {
            key = dir.getAbsolutePath();
        }
        if (seen.add(key)) roots.add(dir); // /storage/udisk1 y su enlace cuentan una sola vez
    }

    private static boolean isUnderAnyRoot(String path, List<String> roots) {
        for (String root : roots) {
            if (path.startsWith(root + "/")) return true;
        }
        return false;
    }

    /** Raíz del escaneo que contiene la ruta, si es un montaje sin "udisk" / "usb" en el nombre. */
    private String otherScanRootOf(String path) {
        for (String root : scanRootPaths) {
            String lower = root.toLowerCase();
            if (lower.contains("udisk") || lower.contains("usb")) continue;
            if (path.startsWith(root + "/")) return root;
        }
        return null;
    }

    /** "Sin videos en udisk1 (120 archivos: 115 mp3, 5 jpg)": dice qué se leyó para saber la causa. */
    private String describeEmptyScan(ScanStats stats) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return "Sin permiso para leer el USB: actívalo en Ajustes › Aplicaciones › JH VIDEO › Permisos";
        }
        if (stats.roots.isEmpty()) return "No se detecta ningún pendrive USB";
        StringBuilder sb = new StringBuilder("Sin videos en ");
        sb.append(android.text.TextUtils.join(", ", stats.roots)).append(" (").append(stats.files)
                .append(stats.files == 1 ? " archivo" : " archivos");
        if (!stats.otherExts.isEmpty()) {
            List<Map.Entry<String, Integer>> exts = new ArrayList<>(stats.otherExts.entrySet());
            Collections.sort(exts, (a, b) -> b.getValue() - a.getValue());
            sb.append(": ");
            for (int i = 0; i < Math.min(4, exts.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(exts.get(i).getValue()).append(' ').append(exts.get(i).getKey());
            }
        }
        return sb.append(')').toString();
    }

    private static boolean isVideoFile(String lowerName) {
        for (String ext : VIDEO_EXTENSIONS) {
            if (lowerName.endsWith(ext)) return true;
        }
        return false;
    }

    private void setupVideoViews() {
        videoTexture = findViewById(R.id.videoTexture);
        if (videoTexture != null) {
            videoTexture.setSurfaceTextureListener(new VideoSurfaceListener(false));
            videoTexture.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitVideo(videoTexture));
        }

        // La pantalla completa va encima de todo, menos la tira de volumen de la izquierda
        FrameLayout content = findViewById(android.R.id.content);
        if (content == null) return;
        layoutVideoFs = getLayoutInflater().inflate(R.layout.layout_video_fullscreen, content, false);
        content.addView(layoutVideoFs);
        layoutVideoFs.setOnTouchListener(videoTapListener);
        videoTextureFs = layoutVideoFs.findViewById(R.id.videoTextureFs);
        videoTextureFs.setSurfaceTextureListener(new VideoSurfaceListener(true));
        videoTextureFs.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitVideo(videoTextureFs));
        layoutFsControls = layoutVideoFs.findViewById(R.id.layoutFsControls);
        tvFsTitle = layoutVideoFs.findViewById(R.id.tvFsTitle);
        tvFsTime = layoutVideoFs.findViewById(R.id.tvFsTime);
        seekFsProgress = layoutVideoFs.findViewById(R.id.seekFsProgress);
        btnFsPlay = layoutVideoFs.findViewById(R.id.btnFsPlay);

        layoutVideoFs.findViewById(R.id.btnFsExit).setOnClickListener(v -> setVideoFullscreen(false));
        btnFsPlay.setOnClickListener(v -> {
            togglePlayPause();
            scheduleFsHide();
        });
        layoutVideoFs.findViewById(R.id.btnFsPrev).setOnClickListener(v -> {
            handlePrevButtonClick();
            scheduleFsHide();
        });
        layoutVideoFs.findViewById(R.id.btnFsNext).setOnClickListener(v -> {
            handleNextButtonClick();
            scheduleFsHide();
        });

        // Barra que se arrastra: mientras se arrastra muestra la posición; al soltar va ahí
        seekFsProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || !playerReady()) return;
                try {
                    int duration = playbackDurationMs();
                    if (duration > 0) showFsTime((int) ((progress / 1000f) * duration), duration);
                } catch (Exception ignored) {}
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isFsSeeking = true;
                handler.removeCallbacks(hideFsControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isFsSeeking = false;
                if (playerReady()) {
                    try {
                        seekFromBar(seekBar.getProgress());
                    } catch (Exception ignored) {}
                }
                scheduleFsHide();
            }
        });
        updateFsPlayState();
    }

    /** Superficie de un TextureView: al estar lista, el reproductor dibuja en ella si es la que toca. */
    private final class VideoSurfaceListener implements TextureView.SurfaceTextureListener {
        private final boolean fullscreen;

        VideoSurfaceListener(boolean fullscreen) {
            this.fullscreen = fullscreen;
        }

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture st, int width, int height) {
            Surface surface = new Surface(st);
            if (fullscreen) videoSurfaceFs = surface; else videoSurface = surface;
            fitVideo(fullscreen ? videoTextureFs : videoTexture);
            applyVideoSurface();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture st, int width, int height) {
            fitVideo(fullscreen ? videoTextureFs : videoTexture);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture st) {
            Surface surface = fullscreen ? videoSurfaceFs : videoSurface;
            if (fullscreen) videoSurfaceFs = null; else videoSurface = null;
            if (surface != null && surface == attachedVideoSurface) {
                // Se fue la superficie en uso: sigue el sonido y el video pasa a la otra (si está)
                try {
                    if (videoPlayer != null) videoPlayer.setSurface(null);
                } catch (Exception ignored) {}
                attachedVideoSurface = null;
                applyVideoSurface();
            }
            if (surface != null) surface.release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture st) {
            lastFrameAt = SystemClock.uptimeMillis();
        }
    }

    /** Pasa el video a la superficie que corresponde (pantalla completa o reproductor). */
    private void applyVideoSurface() {
        // Si el archivo no se pudo abrir, el reproductor ya se liberó (videoPlayer == null).
        // Mientras abre también se cambia: así entrar a pantalla completa en ese momento no queda negro
        if (videoPlayer != null) attachVideoSurface(playerPrepared);
    }

    /** refreshFrame: en pausa vuelve a pedir el cuadro actual (si no, la superficie nueva queda negra). */
    private void attachVideoSurface(boolean refreshFrame) {
        if (videoPlayer == null) return;
        Surface target = isVideoFs && videoSurfaceFs != null ? videoSurfaceFs : videoSurface;
        if (target == attachedVideoSurface) return;
        try {
            videoPlayer.setSurface(target);
            attachedVideoSurface = target;
            if (refreshFrame && target != null && !isPlaying) {
                videoPlayer.redraw(playbackPositionMs());
            }
        } catch (Exception ignored) {}
    }

    private void onVideoSizeKnown(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        fitVideo(videoTexture);
        fitVideo(videoTextureFs);
        // Sin imagen (archivo solo de audio): se ve el dibujo de "sin video" y no el último cuadro
        boolean hasPicture = width > 0 && height > 0;
        View placeholder = findViewById(R.id.layoutAlbumPlaceholder);
        if (placeholder != null) placeholder.setVisibility(hasPicture ? View.GONE : View.VISIBLE);
        if (videoTexture != null) videoTexture.setAlpha(hasPicture ? 1f : 0f);
        if (videoTextureFs != null) videoTextureFs.setAlpha(hasPicture ? 1f : 0f);
    }

    /** Video entero dentro del cuadro sin deformarlo (franjas negras si no es de la misma forma). */
    private void fitVideo(TextureView view) {
        if (view == null) return;
        int w = view.getWidth();
        int h = view.getHeight();
        if (w == 0 || h == 0 || videoWidth == 0 || videoHeight == 0) {
            if (w > 0 && h > 0) view.setTransform(null);
            return;
        }
        float scale = Math.min(w / (float) videoWidth, h / (float) videoHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(videoWidth * scale / w, videoHeight * scale / h, w / 2f, h / 2f);
        view.setTransform(matrix);
    }

    // Antes se sacaba un cuadro con MediaMetadataRetriever.getFrameAtTime: abría un segundo decodificador
    // de video mientras el reproductor usaba el suyo, y en el radio eso trababa la reproducción.
    // Ahora se copia en chico (64x36) el cuadro que ya está en pantalla.
    private static final long AMBIENT_CAPTURE_DELAY_MS = 2500;
    private String ambientCapturePath;
    private final Runnable ambientCaptureRunnable = this::captureAmbientFromVideo;

    private void captureAmbientFromVideo() {
        List<Track> queue = getQueue();
        boolean stillPlaying = ambientCapturePath != null && currentTrackIndex >= 0 && currentTrackIndex < queue.size()
                && ambientCapturePath.equals(queue.get(currentTrackIndex).getFilePath());
        if (!stillPlaying) return;
        if (!playerPrepared) {
            // Todavía abriendo: probar un poco después
            handler.postDelayed(ambientCaptureRunnable, AMBIENT_CAPTURE_DELAY_MS);
            return;
        }
        Bitmap frame = null;
        TextureView view = isVideoFs && videoTextureFs != null ? videoTextureFs : videoTexture;
        if (videoWidth > 0 && videoHeight > 0 && view != null && view.isAvailable()) {
            try {
                frame = view.getBitmap(64, 36);
            } catch (Exception ignored) {}
        }
        showAlbumArt(frame, false);
    }

    // ==========================================
    // TIEMPO DEL VIDEO: algunos radios no dan la duración o la posición del video (devuelven 0 o -1)
    // y la barra quedaba en 00:00. Entonces se usa la duración leída del archivo y un reloj propio
    // que avanza mientras suena
    // ==========================================
    private static final long TIME_NOTE_HOLD_MS = 8000;
    private int fileDurationMs;          // duración leída del archivo (0: no se sabe)
    private int shownTotalMs = -2;       // total puesto en pantalla
    private boolean positionFromClock;   // el reproductor no avanza su posición: manda el reloj propio
    private int clockBaseMs;             // posición al último arranque, pausa o salto
    private long clockStartedAt;         // 0: reloj detenido
    private long rawZeroSince;           // desde cuándo el reproductor dice posición 0 mientras suena
    private volatile long lastFrameAt;   // último cuadro de video que llegó a la pantalla
    private String timeNote;
    private long timeNoteUntil;

    private int rawPlayerDuration() {
        try {
            return videoPlayer != null ? videoPlayer.getDuration() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private int rawPlayerPosition() {
        try {
            return videoPlayer != null ? videoPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Duración del reproductor; si no la da, la del archivo. */
    private int playbackDurationMs() {
        int d = rawPlayerDuration();
        return d > 0 ? d : fileDurationMs;
    }

    /** Posición del reproductor; si no la da, la del reloj propio. */
    private int playbackPositionMs() {
        if (!positionFromClock) return rawPlayerPosition();
        long run = clockStartedAt > 0 ? SystemClock.uptimeMillis() - clockStartedAt : 0;
        int pos = (int) (clockBaseMs + run);
        int total = playbackDurationMs();
        return total > 0 ? Math.min(pos, total) : pos;
    }

    private void resetPlaybackClock(Track track) {
        positionFromClock = false;
        clockBaseMs = 0;
        clockStartedAt = 0;
        rawZeroSince = 0;
        shownTotalMs = -2;
        timeNote = null;
        fileDurationMs = track != null && track.hasDuration() ? track.getDurationSec() * 1000 : 0;
    }

    /** Arranca o detiene el reloj propio (siempre corre a la par del reproductor). */
    private void clockRun(boolean run) {
        long now = SystemClock.uptimeMillis();
        if (clockStartedAt > 0) clockBaseMs += (int) (now - clockStartedAt);
        clockStartedAt = run ? now : 0;
    }

    /** Salta a ms. False si este video no deja saltar (queda donde estaba y lo dice arriba). */
    private boolean seekPlayerTo(int ms) {
        if (ms > 0 && !videoPlayer.canSeek()) {
            showTimeNote("⚠ Este video no deja saltar: el archivo no trae índice");
            return false;
        }
        videoPlayer.seekTo(ms);
        clockBaseMs = ms;
        if (clockStartedAt > 0) clockStartedAt = SystemClock.uptimeMillis();
        // El reproductor del radio a veces no salta: en 2 s se revisa para avisarlo
        handler.removeCallbacks(seekCheckRunnable);
        if (videoPlayer.usesRadioPlayer() && !positionFromClock) {
            seekCheckTargetMs = ms;
            seekCheckAt = SystemClock.uptimeMillis();
            handler.postDelayed(seekCheckRunnable, SEEK_CHECK_DELAY_MS);
        }
        return true;
    }

    private static final long SEEK_CHECK_DELAY_MS = 2000;
    private int seekCheckTargetMs;
    private long seekCheckAt;
    private final Runnable seekCheckRunnable = this::checkSeekLanded;

    /** Con el reproductor del radio: si la posición no quedó cerca de adonde se saltó, avisarlo. */
    private void checkSeekLanded() {
        if (!playerReady() || !videoPlayer.usesRadioPlayer()) return;
        int expected = seekCheckTargetMs + (isPlaying ? (int) (SystemClock.uptimeMillis() - seekCheckAt) : 0);
        int raw = rawPlayerPosition();
        if (Math.abs(raw - expected) > 5000) {
            showTimeNote("⚠ El radio no saltó a " + formatMs(seekCheckTargetMs) + " (sigue en " + formatMs(raw) + ")");
        }
    }

    /** Llegan cuadros nuevos pero la posición sigue en 0 por 3 s: el reproductor no la informa. */
    private void checkPlayerPosition(int raw) {
        // ExoPlayer siempre sabe la posición: el reloj propio es solo para el reproductor del radio
        if (positionFromClock || videoPlayer == null || !videoPlayer.usesRadioPlayer()) return;
        long now = SystemClock.uptimeMillis();
        if (raw > 0) {
            rawZeroSince = 0;
            return;
        }
        if (rawZeroSince == 0) {
            rawZeroSince = now;
            return;
        }
        boolean framesFlowing = lastFrameAt > 0 && now - lastFrameAt < 1000;
        if (framesFlowing && now - rawZeroSince >= 3000) {
            positionFromClock = true;
            showTimeNote("ⓘ El radio no da la posición del video: uso un reloj propio");
        }
    }

    private void showTimeNote(String note) {
        timeNote = note;
        timeNoteUntil = SystemClock.uptimeMillis() + TIME_NOTE_HOLD_MS;
        updateLoadMonitor();
    }

    /** Duración leída del archivo en segundo plano: sirve si el reproductor no la da. */
    private void applyFileDuration(String path, Track track, int durationMs) {
        if (durationMs <= 0 || path == null) return;
        if (!track.hasDuration()) track.setDurationSec(durationMs / 1000);
        List<Track> queue = getQueue();
        if (currentTrackIndex >= 0 && currentTrackIndex < queue.size()
                && path.equals(queue.get(currentTrackIndex).getFilePath())) {
            fileDurationMs = durationMs;
        }
    }

    /** Duración según las pistas del archivo (lee el contenedor de otra forma que MediaMetadataRetriever). */
    private static int readDurationWithExtractor(String path) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
            long maxUs = 0;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    maxUs = Math.max(maxUs, format.getLong(MediaFormat.KEY_DURATION));
                }
            }
            return (int) (maxUs / 1000);
        } catch (Exception e) {
            return 0;
        } finally {
            extractor.release();
        }
    }

    // ==========================================
    // MONITOR DE CARGA (provisional): avisa en la barra de arriba cuando la pantalla se traba,
    // el video deja de avanzar o el reproductor falla, para saber por qué se congela
    // ==========================================
    private static final long MONITOR_PING_MS = 200;        // cada 200 ms se mide cuánto tarda la pantalla en responder
    private static final long MONITOR_LAG_ALERT_MS = 400;   // más que esto: la pantalla se trabó
    private static final long MONITOR_LAG_SEVERE_MS = 1500; // más que esto: aviso en rojo
    private static final long MONITOR_HOLD_MS = 6000;       // el aviso queda a la vista 6 s
    private static final long VIDEO_STALL_MS = 2500;        // reproduciendo sin avanzar: video trabado
    private static final long PLAYER_ERROR_HOLD_MS = 10000;
    private static final String HEADER_GREETING = "Q hubo jefe 😉";

    private HandlerThread monitorThread;
    private Handler monitorHandler;
    private volatile long monitorPingPostedAt;   // 0: no hay medición pendiente
    private volatile long monitorWorstLagMs;     // peor espera desde la última lectura
    private long monitorAlertLagMs;
    private long monitorAlertUntil;
    private int stallLastPosMs = -1;
    private long stallSinceMs;
    private long stallMs;
    private String playerErrorText;
    private long playerErrorUntil;
    private String monitorShownText;

    // Corre en la pantalla: cuánto esperó desde que se pidió
    private final Runnable monitorPong = () -> {
        long lag = SystemClock.uptimeMillis() - monitorPingPostedAt;
        if (lag > monitorWorstLagMs) monitorWorstLagMs = lag;
        monitorPingPostedAt = 0;
    };

    // Corre en su propio hilo: aunque la pantalla esté congelada, sigue contando
    private final Runnable monitorPing = new Runnable() {
        @Override
        public void run() {
            long sentAt = monitorPingPostedAt;
            if (sentAt == 0) {
                monitorPingPostedAt = SystemClock.uptimeMillis();
                handler.post(monitorPong);
            }
            monitorHandler.postDelayed(this, MONITOR_PING_MS);
        }
    };

    private void startLoadMonitor() {
        monitorThread = new HandlerThread("jh-monitor");
        monitorThread.start();
        monitorHandler = new Handler(monitorThread.getLooper());
        monitorHandler.post(monitorPing);
    }

    private void resetStallWatch() {
        stallLastPosMs = -1;
        stallMs = 0;
        lastFrameAt = 0;
    }

    /** Reproduciendo: si la posición no cambia en 2,5 s el video está trabado (USB lento o decodificador). */
    private void watchStall(int positionMs) {
        long now = SystemClock.uptimeMillis();
        if (videoWidth > 0 && videoHeight > 0 && lastFrameAt > 0) {
            stallMs = now - lastFrameAt;
            return;
        }
        if (positionMs != stallLastPosMs) {
            stallLastPosMs = positionMs;
            stallSinceMs = now;
            stallMs = 0;
        } else {
            stallMs = now - stallSinceMs;
        }
    }

    /** Pone el aviso en la barra de arriba (en lugar del saludo) o vuelve al saludo si todo anda bien. */
    private void updateLoadMonitor() {
        if (tvHeaderPath == null) return;
        long now = SystemClock.uptimeMillis();
        long lag = monitorWorstLagMs;
        monitorWorstLagMs = 0;
        if (lag >= MONITOR_LAG_ALERT_MS) {
            monitorAlertLagMs = now < monitorAlertUntil ? Math.max(monitorAlertLagMs, lag) : lag;
            monitorAlertUntil = now + MONITOR_HOLD_MS;
        }

        String text;
        boolean severe;
        if (playerErrorText != null && now < playerErrorUntil) {
            text = playerErrorText;
            severe = true;
        } else if (now < monitorAlertUntil) {
            text = String.format(Locale.US, "⚠ Pantalla saturada: se trabó %.1f s", monitorAlertLagMs / 1000f)
                    .replace('.', ',');
            severe = monitorAlertLagMs >= MONITOR_LAG_SEVERE_MS;
        } else if (stallMs >= VIDEO_STALL_MS) {
            text = "⚠ El video no avanza hace " + (stallMs / 1000) + " s";
            severe = true;
        } else if (timeNote != null && now < timeNoteUntil) {
            text = timeNote;
            severe = false;
        } else {
            text = null;
            severe = false;
        }

        if (text != null) {
            // Qué más está trabajando en ese momento
            int pending;
            synchronized (pendingDurations) {
                pending = pendingDurations.size();
            }
            if (pending > 0) text += " · leyendo USB (" + pending + ")";
        }
        String shown = text != null ? text : HEADER_GREETING;
        if (shown.equals(monitorShownText)) return;
        monitorShownText = shown;
        tvHeaderPath.setText(shown);
        tvHeaderPath.setTextColor(text == null ? Color.parseColor("#38bdf8")
                : Color.parseColor(severe ? "#ef4444" : "#f59e0b"));
    }

    // Toques sobre el video: 1 toque y doble toque (280 ms, como la web).
    // Reproductor: 1 toque nada, doble toque pantalla completa. Pantalla completa: 1 toque controles, doble salir
    private long lastVideoTapAt = 0;
    private final Runnable videoSingleTapRunnable = () -> {
        lastVideoTapAt = 0;
        if (isVideoFs && layoutFsControls != null) {
            setFsControlsVisible(layoutFsControls.getVisibility() != View.VISIBLE);
        }
    };

    private final View.OnTouchListener videoTapListener = new View.OnTouchListener() {
        private float downX, downY;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getX() - downX) > dp(20) || Math.abs(event.getY() - downY) > dp(20)) return true;
                    long now = SystemClock.uptimeMillis();
                    if (lastVideoTapAt > 0 && now - lastVideoTapAt <= DOUBLE_TAP_TIMEOUT_MS) {
                        handler.removeCallbacks(videoSingleTapRunnable);
                        lastVideoTapAt = 0;
                        setVideoFullscreen(!isVideoFs);
                    } else {
                        lastVideoTapAt = now;
                        handler.removeCallbacks(videoSingleTapRunnable);
                        handler.postDelayed(videoSingleTapRunnable, DOUBLE_TAP_TIMEOUT_MS);
                    }
                    return true;
            }
            return true;
        }
    };

    /**
     * Pantalla completa como las apps de video para auto: desaparece todo (barra de arriba, explorador y
     * controles) menos la tira de volumen de la izquierda, porque el auto no tiene botones físicos.
     */
    private void setVideoFullscreen(boolean on) {
        if (layoutVideoFs == null || on == isVideoFs) return;
        hideKeyboard();
        View statusBar = findViewById(R.id.layoutCarStatusBar);
        View divider = findViewById(R.id.viewStatusDivider);
        if (on) {
            isVideoFs = true;
            // Sin la barra de arriba la tira de volumen ocupa todo el alto, como en la web
            if (statusBar != null) statusBar.setVisibility(View.GONE);
            if (divider != null) divider.setVisibility(View.GONE);
            layoutVideoFs.setVisibility(View.VISIBLE);
            layoutVideoFs.bringToFront();
            updateFsPlayState();
            if (playerReady()) {
                try {
                    updateFsProgress(playbackPositionMs(), playbackDurationMs());
                } catch (Exception ignored) {}
            }
            // Al entrar se ven los controles un momento
            setFsControlsVisible(true);
            applyVideoSurface();
        } else {
            isVideoFs = false;
            handler.removeCallbacks(hideFsControlsRunnable);
            handler.removeCallbacks(videoSingleTapRunnable);
            // Primero el video vuelve al reproductor y después se quita la pantalla completa
            applyVideoSurface();
            layoutVideoFs.setVisibility(View.GONE);
            if (statusBar != null) statusBar.setVisibility(View.VISIBLE);
            if (divider != null) divider.setVisibility(View.VISIBLE);
        }
        setImmersiveMode();
    }

    private void setFsControlsVisible(boolean show) {
        if (layoutFsControls == null) return;
        handler.removeCallbacks(hideFsControlsRunnable);
        layoutFsControls.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) scheduleFsHide();
    }

    /** Reproduciendo, los controles se ocultan a los 3 s; en pausa quedan a la vista. */
    private void scheduleFsHide() {
        handler.removeCallbacks(hideFsControlsRunnable);
        if (isVideoFs && isPlaying && !isFsSeeking) {
            handler.postDelayed(hideFsControlsRunnable, FS_CONTROLS_HIDE_MS);
        }
    }

    private void updateFsPlayState() {
        if (btnFsPlay != null) btnFsPlay.setImageResource(isPlaying ? R.drawable.ic_ctrl_pause : R.drawable.ic_ctrl_play);
        if (!isVideoFs || layoutFsControls == null) return;
        if (!isPlaying) {
            setFsControlsVisible(true);
        } else if (layoutFsControls.getVisibility() == View.VISIBLE) {
            scheduleFsHide();
        }
    }

    private void updateFsProgress(int currentMs, int totalMs) {
        if (seekFsProgress == null || isFsSeeking || totalMs <= 0) return;
        seekFsProgress.setProgress((int) (((float) currentMs / totalMs) * 1000));
        showFsTime(currentMs, totalMs);
    }

    private void showFsTime(int currentMs, int totalMs) {
        if (tvFsTime != null) tvFsTime.setText(formatMs(currentMs) + " / " + formatMs(totalMs));
    }


    private void setImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveMode();
            updateVolumeUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setImmersiveMode();
        if (etSearch != null) {
            etSearch.clearFocus();
        }
        hideKeyboard();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
        if (etSearch != null) {
            etSearch.clearFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
        }
        if (clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
        if (searchRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchRunnable);
        }
        handler.removeCallbacks(usbRescanRunnable);
        handler.removeCallbacks(seekFeedbackRunnable);
        handler.removeCallbacks(hideFsControlsRunnable);
        handler.removeCallbacks(videoSingleTapRunnable);
        handler.removeCallbacks(ambientCaptureRunnable);
        handler.removeCallbacks(seekCheckRunnable);
        if (monitorThread != null) monitorThread.quit();
        try {
            unregisterReceiver(usbMountReceiver);
        } catch (Exception ignored) {}
        durationGeneration++;
        durationExecutor.shutdownNow();
        autoMixer.release();
        audioAnalyzer.shutdown();
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
        if (scanExecutor != null) {
            scanExecutor.shutdown();
        }
        if (searchExecutor != null) {
            searchExecutor.shutdown();
        }
    }
}
