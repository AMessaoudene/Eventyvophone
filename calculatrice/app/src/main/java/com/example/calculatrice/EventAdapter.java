package com.example.calculatrice;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> implements android.widget.Filterable {

    private List<EventEntity> data;
    private List<EventEntity> fullList; // Copy for filtering
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EventEntity event);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public EventAdapter(List<EventEntity> data) {
        this.data = data != null ? data : new ArrayList<>();
        this.fullList = new ArrayList<>(this.data);
    }

    public void updateData(List<EventEntity> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        this.fullList = new ArrayList<>(this.data);
        notifyDataSetChanged();
    }

    @Override
    public android.widget.Filter getFilter() {
        return new android.widget.Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<EventEntity> filteredList = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(fullList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (EventEntity item : fullList) {
                        if (item.name != null && item.name.toLowerCase().contains(filterPattern)) {
                            filteredList.add(item);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                data = (List<EventEntity>) results.values;
                notifyDataSetChanged();
            }
        };
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

        holder.tvParticipation.setVisibility(View.VISIBLE);
        if (e.hasParticipationForm) {
            holder.tvParticipation.setText("Participation form open");
        } else if (e.isFree) {
            holder.tvParticipation.setText("Free entry");
        } else {
            holder.tvParticipation.setText("Paid event");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(e);
        });
    }

    @Override
    public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName, tvMeta, tvParticipation;

        VH(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.item_img);
            tvName = itemView.findViewById(R.id.item_name);
            tvMeta = itemView.findViewById(R.id.item_meta);
            tvParticipation = itemView.findViewById(R.id.item_participation_badge);
        }
    }
}
