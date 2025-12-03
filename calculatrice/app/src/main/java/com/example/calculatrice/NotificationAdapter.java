package com.example.calculatrice;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private List<NotificationEntity> data;

    public NotificationAdapter(List<NotificationEntity> data) {
        this.data = data != null ? data : new ArrayList<>();
    }

    public void updateData(List<NotificationEntity> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationEntity item = data.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvMessage.setText(item.message);
        
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(item.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(timeAgo);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;

        VH(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}
