package com.jh.carvideo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FolderTreeAdapter extends BaseAdapter {

    public interface OnTreeNodeClickListener {
        void onNodeClick(TreeNode node, int position);
        void onNodePlayClick(TreeNode node, int position);

        /** + : agregar la carpeta entera a Mi Mix. */
        void onNodeAddMixClick(TreeNode node, int position);
    }

    private Context context;
    private List<TreeNode> visibleNodes;
    private OnTreeNodeClickListener listener;
    private Track currentPlayingTrack;
    private String selectedFolder;

    public FolderTreeAdapter(Context context, List<TreeNode> visibleNodes, OnTreeNodeClickListener listener) {
        this.context = context;
        this.visibleNodes = visibleNodes != null ? visibleNodes : new ArrayList<>();
        this.listener = listener;
    }

    public void setVisibleNodes(List<TreeNode> nodes) {
        this.visibleNodes = nodes != null ? nodes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentPlayingTrack(Track track) {
        this.currentPlayingTrack = track;
        notifyDataSetChanged();
    }

    public void setSelectedFolder(String folder) {
        this.selectedFolder = folder;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return visibleNodes.size();
    }

    @Override
    public TreeNode getItem(int position) {
        if (position >= 0 && position < visibleNodes.size()) {
            return visibleNodes.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        LinearLayout layoutTreeItem;
        View viewIndent;
        ImageView ivTreeArrow;
        FrameLayout frameTreeIcon;
        ImageView ivTreeIcon;
        TextView tvTreeTitle;
        LinearLayout btnFolderPlay;
        View btnTreeAddMix;
        TextView tvTreeDirectCount;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_tree_folder, parent, false);
            holder = new ViewHolder();
            holder.layoutTreeItem = convertView.findViewById(R.id.layoutTreeItem);
            holder.viewIndent = convertView.findViewById(R.id.viewIndent);
            holder.ivTreeArrow = convertView.findViewById(R.id.ivTreeArrow);
            holder.frameTreeIcon = convertView.findViewById(R.id.frameTreeIcon);
            holder.ivTreeIcon = convertView.findViewById(R.id.ivTreeIcon);
            holder.tvTreeTitle = convertView.findViewById(R.id.tvTreeTitle);
            holder.btnFolderPlay = convertView.findViewById(R.id.btnFolderPlay);
            holder.btnTreeAddMix = convertView.findViewById(R.id.btnTreeAddMix);
            holder.tvTreeDirectCount = convertView.findViewById(R.id.tvTreeDirectCount);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TreeNode node = getItem(position);
        if (node != null) {
            float density = context.getResources().getDisplayMetrics().density;

            // Indentación según el nivel de profundidad
            int indentPx = (int) ((8 + node.getLevel() * 20) * density);
            ViewGroup.LayoutParams lp = holder.viewIndent.getLayoutParams();
            if (lp != null) {
                lp.width = indentPx;
                holder.viewIndent.setLayoutParams(lp);
            }

            // Flecha de despliegue (> o v)
            if (node.hasChildren()) {
                holder.ivTreeArrow.setVisibility(View.VISIBLE);
                if (node.isExpanded()) {
                    holder.ivTreeArrow.setImageResource(R.drawable.ic_expand_more);
                } else {
                    holder.ivTreeArrow.setImageResource(R.drawable.ic_chevron_right);
                }
            } else {
                // Si no tiene subcarpetas, ocultar flecha pero mantener el espaciado
                holder.ivTreeArrow.setVisibility(View.INVISIBLE);
            }

            // Icono según sea Raíz de Pendrive o Carpeta
            if (node.isUsbRoot()) {
                holder.frameTreeIcon.setBackgroundResource(R.drawable.bg_tree_icon_usb);
                holder.ivTreeIcon.setImageResource(R.drawable.ic_usb_trident);
                holder.ivTreeIcon.setColorFilter(Color.parseColor("#10b981"), PorterDuff.Mode.SRC_IN);
            } else {
                holder.frameTreeIcon.setBackgroundResource(R.drawable.bg_tree_icon_folder);
                holder.ivTreeIcon.setImageResource(R.drawable.ic_tree_folder);
                holder.ivTreeIcon.setColorFilter(Color.parseColor("#38bdf8"), PorterDuff.Mode.SRC_IN);
            }

            // Título con auto-scroll horizontal si es largo
            holder.tvTreeTitle.setText(node.getName());
            holder.tvTreeTitle.setSelected(true);

            // Botón Reproducir [ N ▶ ] (Solo si tiene canciones directas > 0)
            if (node.getDirectTrackCount() > 0) {
                holder.btnFolderPlay.setVisibility(View.VISIBLE);
                holder.tvTreeDirectCount.setText(String.valueOf(node.getDirectTrackCount()));
                holder.btnFolderPlay.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNodePlayClick(node, position);
                    }
                });
                holder.btnTreeAddMix.setVisibility(View.VISIBLE);
                holder.btnTreeAddMix.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onNodeAddMixClick(node, position);
                    }
                });
            } else {
                holder.btnFolderPlay.setVisibility(View.GONE);
                holder.btnFolderPlay.setOnClickListener(null);
                holder.btnTreeAddMix.setVisibility(View.GONE);
                holder.btnTreeAddMix.setOnClickListener(null);
            }

            // Resaltado de la carpeta que está sonando actualmente
            boolean isPlayingFolder = false;
            if (currentPlayingTrack != null && !node.isUsbRoot()) {
                if (node.getName() != null && node.getName().equalsIgnoreCase(currentPlayingTrack.getFolder())) {
                    isPlayingFolder = true;
                }
            }
            boolean isSelected = (selectedFolder != null && selectedFolder.equalsIgnoreCase(node.getName()));

            if (isPlayingFolder) {
                holder.layoutTreeItem.setBackgroundResource(R.drawable.bg_tree_node_playing);
            } else if (isSelected) {
                holder.layoutTreeItem.setBackgroundResource(R.drawable.bg_tree_node_playing);
            } else {
                holder.layoutTreeItem.setBackgroundResource(R.drawable.bg_tree_node);
            }

            // Clic en la flecha de despliegue
            holder.ivTreeArrow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNodeClick(node, position);
                }
            });

            // Clic en el icono
            holder.frameTreeIcon.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNodeClick(node, position);
                }
            });

            // Clic en la fila (expandir / contraer y seleccionar)
            convertView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNodeClick(node, position);
                }
            });
        }

        return convertView;
    }
}
