package com.jh.carvideo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

/**
 * Adaptador de alto rendimiento basado en RecyclerView para Mi Mix.
 * Soporta ItemTouchHelper para abrir hueco en vivo de forma fluida y acelerada por GPU.
 */
public class MixRecyclerAdapter extends RecyclerView.Adapter<MixRecyclerAdapter.ViewHolder> {

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnMixActionListener {
        void onTrackClick(Track track, int position);
        void onRemoveTrack(Track track, int position);
        void onItemMoved(int fromPos, int toPos);
        void onReorderDone();
    }

    private final Context context;
    private final List<Track> tracks;
    private final OnStartDragListener dragListener;
    private final OnMixActionListener actionListener;
    private final TrackAdapter.MixState mixState;

    private String currentPlayingPath;
    private boolean reorderMode = false;

    public MixRecyclerAdapter(Context context,
                              List<Track> tracks,
                              TrackAdapter.MixState mixState,
                              OnStartDragListener dragListener,
                              OnMixActionListener actionListener) {
        this.context = context;
        this.tracks = tracks;
        this.mixState = mixState;
        this.dragListener = dragListener;
        this.actionListener = actionListener;
    }

    public void setCurrentPlayingTrack(Track track) {
        this.currentPlayingPath = track != null ? track.getFilePath() : null;
        notifyDataSetChanged();
    }

    public void setReorderMode(boolean reorderMode) {
        if (this.reorderMode == reorderMode) return;
        this.reorderMode = reorderMode;
        notifyDataSetChanged();
    }

    public boolean isReorderMode() {
        return reorderMode;
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= tracks.size() ||
            toPosition < 0 || toPosition >= tracks.size()) {
            return;
        }
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(tracks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(tracks, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = tracks.get(position);
        if (track == null) return;

        boolean isPlaying = currentPlayingPath != null && currentPlayingPath.equals(track.getFilePath());
        boolean isOrphan = mixState != null && mixState.isOrphan(track);
        boolean wasPlayed = !isPlaying && mixState != null && mixState.isPlayed(track);

        holder.itemView.setAlpha(wasPlayed ? 0.5f : 1f);

        if (isPlaying) {
            holder.tvIndex.setText("▶");
            holder.tvIndex.setTextColor(Color.parseColor("#38bdf8"));
            holder.tvTitle.setTextColor(Color.parseColor("#38bdf8"));
            holder.itemView.setBackgroundColor(Color.parseColor("#2438bdf8"));
        } else if (wasPlayed) {
            holder.tvIndex.setText("✓");
            holder.tvIndex.setTextColor(Color.parseColor("#22c55e"));
            holder.tvTitle.setTextColor(Color.parseColor("#94a3b8"));
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        } else {
            int number = position + 1;
            if (position > 0 && mixState != null && mixState.isOrphan(tracks.get(0))) number--;
            holder.tvIndex.setText(String.format("%02d", number));
            holder.tvIndex.setTextColor(Color.parseColor("#94a3b8"));
            holder.tvTitle.setTextColor(Color.parseColor("#f1f5f9"));
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.tvTitle.setText(track.getTitle());
        holder.tvTitle.setSingleLine(true);
        holder.tvTitle.setMaxLines(1);
        holder.tvTitle.setCarMarqueeEnabled(isPlaying);

        String artist = track.getArtist();
        String album = track.getAlbum();
        String subtitle = (artist != null && !artist.trim().isEmpty() && !"Pendrive".equalsIgnoreCase(artist.trim()))
                ? artist + " • " + album : (album != null ? album : "");
        if (isOrphan) {
            subtitle = "Sonando · ya no está en Mi Lista";
        } else if (wasPlayed) {
            subtitle = "Ya visto • " + subtitle;
        }
        holder.tvArtist.setText(subtitle);

        // USB Badge
        String source = track.getUsbSource();
        String path = track.getFilePath();
        if (source == null && path != null) {
            source = (path.contains("udisk2") || path.contains("usb1")) ? "USB 2" : "USB 1";
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

        if (reorderMode && !isOrphan) {
            holder.btnMix.setImageResource(R.drawable.ic_mix_reorder);
            holder.btnMix.setBackgroundResource(R.drawable.bg_btn_action);
            holder.btnMix.setContentDescription("Arrastrar para reordenar");
            holder.btnMix.setOnClickListener(null);

            holder.btnMix.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (dragListener != null) {
                        dragListener.onStartDrag(holder);
                    }
                    return true;
                }
                return false;
            });

            holder.itemView.setOnClickListener(null);
            holder.tvIndex.setOnClickListener(null);
        } else {
            holder.btnMix.setOnTouchListener(null);
            holder.btnMix.setImageResource(R.drawable.ic_mix_remove);
            holder.btnMix.setBackgroundResource(R.drawable.bg_btn_action);
            holder.btnMix.setContentDescription("Quitar de Mi Lista");

            holder.btnMix.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION && actionListener != null) {
                    actionListener.onRemoveTrack(tracks.get(currentPos), currentPos);
                }
            });

            View.OnClickListener clickPlay = v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION && actionListener != null) {
                    actionListener.onTrackClick(tracks.get(currentPos), currentPos);
                }
            };
            holder.tvIndex.setOnClickListener(clickPlay);
            holder.itemView.setOnClickListener(clickPlay);
        }
    }

    @Override
    public int getItemCount() {
        return tracks != null ? tracks.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIndex;
        final CarMarqueeTextView tvTitle;
        final TextView tvArtist;
        final TextView tvUsbBadge;
        final TextView tvDuration;
        final ImageButton btnMix;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvItemIndex);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvArtist = itemView.findViewById(R.id.tvItemArtist);
            tvUsbBadge = itemView.findViewById(R.id.tvItemUsbBadge);
            tvDuration = itemView.findViewById(R.id.tvItemDuration);
            btnMix = itemView.findViewById(R.id.btnItemMix);
        }
    }
}
