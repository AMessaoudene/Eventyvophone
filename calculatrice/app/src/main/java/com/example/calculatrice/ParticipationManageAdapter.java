package com.example.calculatrice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParticipationManageAdapter extends RecyclerView.Adapter<ParticipationManageAdapter.VH> {

    interface Listener {
        void onAccept(ParticipationEntity entity);

        void onRefuse(ParticipationEntity entity);

        void onViewProfile(ParticipationEntity entity);
    }

    private final List<ParticipationEntity> data = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public ParticipationManageAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ParticipationEntity> entries) {
        data.clear();
        if (entries != null) {
            data.addAll(entries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participation_entry, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ParticipationEntity entity = data.get(position);
        holder.tvName.setText(entity.fullName);
        holder.tvEmail.setText(entity.email);
        holder.tvStatus.setText(entity.status.toUpperCase(Locale.getDefault()));
        holder.tvSubmitted.setText(formatter.format(new Date(entity.createdAt)));
        holder.tvPhone.setText(entity.phone == null || entity.phone.isEmpty() ? "No phone" : entity.phone);

        int statusColor;
        switch (entity.status) {
            case "accepted":
                statusColor = 0xFF2E7D32;
                break;
            case "refused":
                statusColor = 0xFFD32F2F;
                break;
            default:
                statusColor = 0xFF795548;
                break;
        }
        holder.tvStatus.setTextColor(statusColor);

        boolean isPending = "pending".equals(entity.status);
        boolean isAccepted = "accepted".equals(entity.status);

        holder.btnAccept.setText(isAccepted ? "Show QR" : "Accept");
        holder.btnAccept.setEnabled(isPending || isAccepted);
        holder.btnRefuse.setEnabled(isPending);

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(entity));
        holder.btnRefuse.setOnClickListener(v -> listener.onRefuse(entity));
        holder.btnDetails.setOnClickListener(v -> listener.onViewProfile(entity));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvEmail;
        final TextView tvPhone;
        final TextView tvStatus;
        final TextView tvSubmitted;
        final Button btnAccept;
        final Button btnRefuse;
        final ImageButton btnDetails;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvParticipantName);
            tvEmail = itemView.findViewById(R.id.tvParticipantEmail);
            tvPhone = itemView.findViewById(R.id.tvParticipantPhone);
            tvStatus = itemView.findViewById(R.id.tvParticipantStatus);
            tvSubmitted = itemView.findViewById(R.id.tvParticipantSubmitted);
            btnAccept = itemView.findViewById(R.id.btnParticipantAccept);
            btnRefuse = itemView.findViewById(R.id.btnParticipantRefuse);
            btnDetails = itemView.findViewById(R.id.btnParticipantDetails);
        }
    }
}

