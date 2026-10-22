package com.jh.carvideo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends BaseAdapter {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folder, int position);
    }

    /** + : agregar la carpeta entera a Mi Mix. */
    public interface OnFolderAddMixListener {
        void onFolderAddMix(FolderItem folder, int position);
    }

    private Context context;
    private List<FolderItem> folders;
    private OnFolderClickListener folderClickListener;
    private OnFolderAddMixListener addMixListener;

    public FolderAdapter(Context context, List<FolderItem> folders, OnFolderClickListener listener,
                         OnFolderAddMixListener addMixListener) {
        this.context = context;
        this.folders = folders != null ? folders : new ArrayList<>();
        this.folderClickListener = listener;
        this.addMixListener = addMixListener;
    }

    public void setFolders(List<FolderItem> folders) {
        this.folders = folders != null ? folders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return folders.size();
    }

    @Override
    public FolderItem getItem(int position) {
        if (position >= 0 && position < folders.size()) {
            return folders.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        TextView tvName;
        TextView tvPath;
        TextView tvUsbBadge;
        TextView tvTrackCount;
        View btnAddMix;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tvFolderName);
            holder.tvPath = convertView.findViewById(R.id.tvFolderPath);
            holder.tvUsbBadge = convertView.findViewById(R.id.tvFolderUsbBadge);
            holder.tvTrackCount = convertView.findViewById(R.id.tvFolderTrackCount);
            holder.btnAddMix = convertView.findViewById(R.id.btnFolderAddMix);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FolderItem folder = getItem(position);
        if (folder != null) {
            holder.tvName.setText(folder.getName());

            // Formatear ruta sin prefijo redundante en USB 1 y USB 2
            String path = folder.getPath();
            String badgeText = folder.getUsbSource();
            if (path != null) {
                if (path.startsWith("/storage/udisk1")) {
                    path = path.replace("/storage/udisk1", "");
                    badgeText = "USB 1";
                } else if (path.startsWith("/storage/udisk2")) {
                    path = path.replace("/storage/udisk2", "");
                    badgeText = "USB 2";
                } else if (path.contains("sdcard") || path.contains("emulated")) {
                    badgeText = "INTERNA";
                }
                if (!path.startsWith("/")) path = "/" + path;
                if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
            }
            holder.tvPath.setText(path);

            // Estilos del Badge
            if ("USB 2".equalsIgnoreCase(badgeText) || "udisk2".equalsIgnoreCase(badgeText) || (badgeText != null && badgeText.contains("2"))) {
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

            // Solo número limpio sin la palabra "pistas"
            holder.tvTrackCount.setText(String.valueOf(folder.getTrackCount()));

            holder.btnAddMix.setVisibility(folder.getTrackCount() > 0 ? View.VISIBLE : View.GONE);
            holder.btnAddMix.setOnClickListener(v -> {
                if (addMixListener != null) {
                    addMixListener.onFolderAddMix(folder, position);
                }
            });

            // Click directo en la fila
            convertView.setOnClickListener(v -> {
                if (folderClickListener != null) {
                    folderClickListener.onFolderClick(folder, position);
                }
            });
        }

        return convertView;
    }
}
