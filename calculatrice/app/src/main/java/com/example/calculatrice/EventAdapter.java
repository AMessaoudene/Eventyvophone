package com.example.calculatrice;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

    private List<EventEntity> data;
    private OnItemClickListener listener;
    private OnItemLongClickListener longListener;

    public interface OnItemClickListener {
        void onItemClick(EventEntity event);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(EventEntity event);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener l) { this.longListener = l; }

    public EventAdapter(List<EventEntity> data) { this.data = data; }

    public void setData(List<EventEntity> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        EventEntity e = data.get(position);
        holder.tvName.setText(e.name);

        String meta = e.startDate;
        if (e.endDate != null && !e.endDate.isEmpty()) meta += " - " + e.endDate;
        meta += " • ";
        meta += (e.isOnline ? "Online" : (e.location != null ? e.location : "Offline"));
        holder.tvMeta.setText(meta);

        if (e.imageUri != null && !e.imageUri.isEmpty()) {
            try { holder.img.setImageURI(Uri.parse(e.imageUri)); }
            catch (Exception ex) { holder.img.setImageResource(android.R.drawable.ic_menu_report_image); }
        } else {
            holder.img.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(e);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onItemLongClick(e);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvMeta;

        VH(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.item_img);
            tvName = itemView.findViewById(R.id.item_name);
            tvMeta = itemView.findViewById(R.id.item_meta);
        }
    }
}
