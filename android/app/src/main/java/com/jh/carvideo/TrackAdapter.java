package com.jh.carvideo;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends BaseAdapter implements SectionIndexer {

    public interface OnTrackClickListener {
        void onTrackClick(Track track, int position);
    }

    /** Botón de Mi Mix de cada fila: + agregar, ✓ ya está (quitar) o ✕ quitar (dentro de Mi Mix). */
    public interface OnMixButtonClickListener {
        void onMixButtonClick(Track track, int position);
    }

    /** Estado de Mi Mix que la lista necesita para dibujar cada fila. */
    public interface MixState {
        boolean isInMix(Track track);

        /** Ya sonó en esta sesión de Mi Mix (se muestra apagada con ✓). */
        boolean isPlayed(Track track);

        /** Suena, pero ya no está en Mi Mix (se quitó o se vació la lista). */
        boolean isOrphan(Track track);
    }

    private Context context;
    private List<Track> tracks;
    // Canción que suena, identificada por su archivo (no por posición): así no queda marcada
    // otra fila al mostrar otra carpeta, una búsqueda o Mi Mix
    private String currentPlayingPath = null;
    private int selectedMarqueeIndex = -1;
    private final Handler clickHandler = new Handler(Looper.getMainLooper());
    private Runnable singleClickRunnable = null;
    private int clickCount = 0;
    private int lastClickedPosition = -1;
    private static final long DOUBLE_TAP_TIMEOUT = 280; // ms para confirmar si fue 1 solo toque o doble toque
    private OnTrackClickListener trackClickListener;
    private OnMixButtonClickListener mixButtonListener;
    private final MixState mixState;
    private boolean mixMode = false; // mostrando la pestaña Mi Mix
    private boolean reorderMode = false; // modo reordenar canciones en Mi Mix

    private String[] sections = new String[0];
    private int[] sectionPositions = new int[0];

    public TrackAdapter(Context context, List<Track> tracks, OnTrackClickListener trackListener,
                        OnMixButtonClickListener mixListener, MixState mixState) {
        this.context = context;
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        this.trackClickListener = trackListener;
        this.mixButtonListener = mixListener;
        this.mixState = mixState;
        buildSections();
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        this.selectedMarqueeIndex = -1; // la posición expandida era de la lista anterior
        buildSections();
        notifyDataSetChanged();
    }

    private void buildSections() {
        if (tracks == null || tracks.isEmpty()) {
            sections = new String[0];
            sectionPositions = new int[0];
            return;
        }
        List<String> sectionList = new ArrayList<>();
        List<Integer> positionList = new ArrayList<>();

        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            String title = (t != null && t.getTitle() != null) ? t.getTitle().trim() : "";
            String initial = "#";
            if (!title.isEmpty()) {
                char ch = Character.toUpperCase(title.charAt(0));
                if (ch >= 'A' && ch <= 'Z') {
                    initial = String.valueOf(ch);
                }
            }
            if (!sectionList.contains(initial)) {
                sectionList.add(initial);
                positionList.add(i);
            }
        }
        sections = sectionList.toArray(new String[0]);
        sectionPositions = new int[positionList.size()];
        for (int i = 0; i < positionList.size(); i++) {
            sectionPositions[i] = positionList.get(i);
        }
    }

    @Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionPositions == null || sectionPositions.length == 0) return 0;
        if (sectionIndex < 0) return 0;
        if (sectionIndex >= sectionPositions.length) return sectionPositions[sectionPositions.length - 1];
        return sectionPositions[sectionIndex];
    }

    @Override
    public int getSectionForPosition(int position) {
        if (sectionPositions == null || sectionPositions.length == 0) return 0;
        for (int i = sectionPositions.length - 1; i >= 0; i--) {
            if (position >= sectionPositions[i]) {
                return i;
            }
        }
        return 0;
    }

    public void setCurrentPlayingTrack(Track track) {
        this.currentPlayingPath = track != null ? track.getFilePath() : null;
        this.selectedMarqueeIndex = -1;
        notifyDataSetChanged();
    }

    /** En la pestaña Mi Mix las filas se numeran por su orden en la lista y el botón es ✕ (quitar). */
    public void setMixMode(boolean mixMode) {
        if (this.mixMode == mixMode) return;
        this.mixMode = mixMode;
        notifyDataSetChanged();
    }

    /** Activa o desactiva el modo de reordenar canciones en Mi Mix (la ✕ pasa a ser ≡ para arrastrar). */
    public void setReorderMode(boolean reorderMode) {
        if (this.reorderMode == reorderMode) return;
        this.reorderMode = reorderMode;
        notifyDataSetChanged();
    }

    public boolean isReorderMode() {
        return reorderMode;
    }

    public void setSelectedMarqueeIndex(int index) {
        this.selectedMarqueeIndex = index;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tracks.size();
    }

    @Override
    public Track getItem(int position) {
        if (position >= 0 && position < tracks.size()) {
            return tracks.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView tvIndex;
        CarMarqueeTextView tvTitle;
        TextView tvArtist;
        TextView tvUsbBadge;
        TextView tvDuration;
        ImageButton btnMix;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
            holder = new ViewHolder();
            holder.tvIndex = convertView.findViewById(R.id.tvItemIndex);
            holder.tvTitle = convertView.findViewById(R.id.tvItemTitle);
            holder.tvArtist = convertView.findViewById(R.id.tvItemArtist);
            holder.tvUsbBadge = convertView.findViewById(R.id.tvItemUsbBadge);
            holder.tvDuration = convertView.findViewById(R.id.tvItemDuration);
            holder.btnMix = convertView.findViewById(R.id.btnItemMix);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Track track = getItem(position);
        if (track != null) {
            boolean isPlaying = currentPlayingPath != null && currentPlayingPath.equals(track.getFilePath());
            boolean isSelectedForMarquee = (position == selectedMarqueeIndex) || isPlaying;
            // Mi Mix: la que suena fuera de la lista va primero; las que ya sonaron quedan apagadas con ✓
            boolean isOrphan = mixMode && mixState != null && mixState.isOrphan(track);
            boolean wasPlayed = mixMode && !isPlaying && mixState != null && mixState.isPlayed(track);
            convertView.setAlpha(wasPlayed ? 0.5f : 1f);

            if (isPlaying) {
                holder.tvIndex.setText("▶");
                holder.tvIndex.setTextColor(Color.parseColor("#38bdf8"));
                holder.tvTitle.setTextColor(Color.parseColor("#38bdf8"));
                convertView.setBackgroundColor(Color.parseColor("#2438bdf8"));
            } else if (wasPlayed) {
                holder.tvIndex.setText("✓");
                holder.tvIndex.setTextColor(Color.parseColor("#22c55e"));
                holder.tvTitle.setTextColor(Color.parseColor("#94a3b8"));
                convertView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                // En Mi Mix el número es la posición en la lista (sin contar la que suena fuera de ella)
                int number = position + 1;
                if (mixMode && position > 0 && mixState != null && mixState.isOrphan(getItem(0))) number--;
                holder.tvIndex.setText(String.format("%02d", number));
                holder.tvIndex.setTextColor(Color.parseColor("#94a3b8"));
                holder.tvTitle.setTextColor(isSelectedForMarquee ? Color.parseColor("#38bdf8") : Color.parseColor("#f1f5f9"));
                convertView.setBackgroundColor(isSelectedForMarquee ? Color.parseColor("#0fffffff") : Color.TRANSPARENT);
            }

            holder.tvTitle.setText(track.getTitle());
            if (position == selectedMarqueeIndex) {
                // Modo expandido: texto completo sin recortar
                holder.tvTitle.setSingleLine(false);
                holder.tvTitle.setMaxLines(4);
                holder.tvTitle.setCarMarqueeEnabled(false);
                holder.tvTitle.setEllipsize(null);
                holder.tvTitle.setTextColor(Color.parseColor("#38bdf8"));
                convertView.setBackgroundColor(Color.parseColor("#14ffffff"));
            } else {
                // Modo normal de 1 línea: la canción que suena se desplaza como en la web; el resto se corta con "…"
                holder.tvTitle.setSingleLine(true);
                holder.tvTitle.setMaxLines(1);
                holder.tvTitle.setCarMarqueeEnabled(isPlaying);
                holder.tvTitle.setTextColor(isPlaying ? Color.parseColor("#38bdf8")
                        : (wasPlayed ? Color.parseColor("#94a3b8") : Color.parseColor("#f1f5f9")));
                convertView.setBackgroundColor(isPlaying ? Color.parseColor("#2438bdf8") : Color.TRANSPARENT);
            }

            String artist = track.getArtist();
            String album = track.getAlbum();
            String subtitle;
            if (artist != null && !artist.trim().isEmpty() && !"Pendrive".equalsIgnoreCase(artist.trim())) {
                subtitle = artist + " • " + album;
            } else {
                subtitle = album != null ? album : "";
            }
            if (isOrphan) {
                subtitle = "Sonando · ya no está en Mi Lista";
            } else if (wasPlayed) {
                subtitle = "Ya visto • " + subtitle;
            }
            holder.tvArtist.setText(subtitle);

            // Badge de almacenamiento con icono USB vectorial y número
            String source = track.getUsbSource();
            String path = track.getFilePath();
            if (source == null && path != null) {
                if (path.contains("udisk2") || path.contains("usb1")) source = "USB 2";
                else source = "USB 1";
            }
            if ("USB 2".equalsIgnoreCase(source) || (source != null && source.contains("2"))) {
                int color = Color.parseColor("#c084fc");
                holder.tvUsbBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_usb_trident, 0, 0, 0);
                holder.tvUsbBadge.setCompoundDrawablePadding((int) (2 * context.getResources().getDisplayMetrics().density));
                holder.tvUsbBadge.setText("2");
                holder.tvUsbBadge.setBackgroundResource(R.drawable.bg_pill_usb2);
                holder.tvUsbBadge.setTextColor(color);
                Drawable[] d = holder.tvUsbBadge.getCompoundDrawables();
                if (d.length > 0 && d[0] != null) d[0].mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else {
                int color = Color.parseColor("#38bdf8");
                holder.tvUsbBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_usb_trident, 0, 0, 0);
                holder.tvUsbBadge.setCompoundDrawablePadding((int) (2 * context.getResources().getDisplayMetrics().density));
                holder.tvUsbBadge.setText("1");
                holder.tvUsbBadge.setBackgroundResource(R.drawable.bg_pill_usb1);
                holder.tvUsbBadge.setTextColor(color);
                Drawable[] d = holder.tvUsbBadge.getCompoundDrawables();
                if (d.length > 0 && d[0] != null) d[0].mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }

            holder.tvDuration.setText(track.getFormattedDuration());

            // Botón de Mi Mix: en modo reordenar es ≡ (manija para arrastrar), en normal es ✕ (quitar), ✓ si ya está, + si no
            final View rowView = convertView;
            if (mixMode && reorderMode && !isOrphan) {
                holder.btnMix.setImageResource(R.drawable.ic_mix_reorder);
                holder.btnMix.setBackgroundResource(R.drawable.bg_btn_action);
                holder.btnMix.setContentDescription("Arrastrar para reordenar");
                holder.btnMix.setOnClickListener(null);
                holder.btnMix.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        ClipData data = ClipData.newPlainText("position", String.valueOf(position));
                        View.DragShadowBuilder shadow = new View.DragShadowBuilder(rowView);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            rowView.startDragAndDrop(data, shadow, position, 0);
                        } else {
                            rowView.startDrag(data, shadow, position, 0);
                        }
                        return true;
                    }
                    return false;
                });

                // En modo reordenar desactivar clics en la fila para evitar reproducción accidental al arrastrar
                holder.tvIndex.setOnClickListener(null);
                rowView.setOnClickListener(null);
                holder.tvTitle.setOnClickListener(null);
            } else {
                holder.btnMix.setOnTouchListener(null);

                if (mixMode && !isOrphan) {
                    holder.btnMix.setImageResource(R.drawable.ic_mix_remove);
                    holder.btnMix.setBackgroundResource(R.drawable.bg_btn_action);
                    holder.btnMix.setContentDescription("Quitar de Mi Lista");
                } else if (mixState != null && mixState.isInMix(track)) {
                    holder.btnMix.setImageResource(R.drawable.ic_mix_check);
                    holder.btnMix.setBackgroundResource(R.drawable.bg_mix_btn_active);
                    holder.btnMix.setContentDescription("En Mi Lista (toca para quitar)");
                } else {
                    holder.btnMix.setImageResource(R.drawable.ic_mix_add);
                    holder.btnMix.setBackgroundResource(R.drawable.bg_btn_action);
                    holder.btnMix.setContentDescription("Agregar a Mi Lista");
                }

                holder.btnMix.setOnClickListener(v -> {
                    if (mixButtonListener != null) {
                        mixButtonListener.onMixButtonClick(track, position);
                        notifyDataSetChanged();
                    }
                });

                // 1. Click en el icono de la izquierda (tvIndex): REPRODUCIR DE INMEDIATO (1 toque)
                holder.tvIndex.setOnClickListener(v -> {
                    if (trackClickListener != null) {
                        trackClickListener.onTrackClick(track, position);
                    }
                });

                // 2. Click en la fila o en el texto:
                // 1 toque: Muestra el nombre completo (espera 280ms para confirmar que no fue doble toque)
                // 2 toques: Cancela la apertura del nombre y reproduce directamente la canción
                View.OnClickListener rowClickListener = v -> {
                    if (position != lastClickedPosition) {
                        if (singleClickRunnable != null) {
                            clickHandler.removeCallbacks(singleClickRunnable);
                            singleClickRunnable = null;
                        }
                        clickCount = 0;
                        lastClickedPosition = position;
                    }

                    clickCount++;

                    if (clickCount == 1) {
                        singleClickRunnable = () -> {
                            // Confirmado 1 solo toque: Expandir o contraer para leer todo
                            clickCount = 0;
                            singleClickRunnable = null;
                            if (selectedMarqueeIndex == position) {
                                selectedMarqueeIndex = -1; // Toggle para contraer
                            } else {
                                selectedMarqueeIndex = position; // Expandir para leer todo
                            }
                            notifyDataSetChanged();
                        };
                        clickHandler.postDelayed(singleClickRunnable, DOUBLE_TAP_TIMEOUT);
                    } else if (clickCount >= 2) {
                        // Confirmado 2 toques (doble toque): CANCELAR la apertura del nombre y reproducir de inmediato
                        if (singleClickRunnable != null) {
                            clickHandler.removeCallbacks(singleClickRunnable);
                            singleClickRunnable = null;
                        }
                        clickCount = 0;
                        lastClickedPosition = -1;
                        if (trackClickListener != null) {
                            trackClickListener.onTrackClick(track, position);
                        }
                    }
                };

                convertView.setOnClickListener(rowClickListener);
                holder.tvTitle.setOnClickListener(rowClickListener);
            }
        }

        return convertView;
    }
}
