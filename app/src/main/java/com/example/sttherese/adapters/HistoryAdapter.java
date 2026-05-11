package com.example.sttherese.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.DateFormatter;
import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

public class HistoryAdapter extends FirestoreRecyclerAdapter<Appointment, HistoryAdapter.HistoryViewHolder> {

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnHistoryClickListener {
        void onHistoryClick(Appointment appointment, DocumentSnapshot snapshot);
    }

    private boolean hasMoreData = true;
    private boolean loading = false;
    private boolean loadMoreTriggered = false;

    public void setHasMoreData(boolean hasMoreData) {
        this.hasMoreData = hasMoreData;
        this.loadMoreTriggered = false;
    }

    public boolean hasMoreData() {
        return hasMoreData;
    }

    private final OnItemCountChangeListener itemCountChangeListener;
    private final OnLoadMoreListener onLoadMoreListener;
    private final OnHistoryClickListener onHistoryClickListener;

    public HistoryAdapter(Query query, OnItemCountChangeListener countListener, OnLoadMoreListener loadMoreListener) {
        this(query, countListener, loadMoreListener, null);
    }

    public HistoryAdapter(Query query, OnItemCountChangeListener countListener, OnLoadMoreListener loadMoreListener, OnHistoryClickListener clickListener) {
        super(new FirestoreRecyclerOptions.Builder<Appointment>()
                .setQuery(query, Appointment.class)
                .build());
        this.itemCountChangeListener = countListener;
        this.onLoadMoreListener = loadMoreListener;
        this.onHistoryClickListener = clickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull HistoryViewHolder holder, int position, @NonNull Appointment model) {
        String service = model.getSpecialty() != null ? model.getSpecialty() : "Unknown Service";
        String doctor = model.getDoctorName() != null ? model.getDoctorName() : "Doctor TBA";
        String dateString = model.getDate() != null ? model.getDate() : "N/A";
        String status = model.getStatus() != null ? model.getStatus() : "pending";

        // 1. Service/Type
        holder.tvServiceType.setText(service);

        // 2. Doctor Name
        holder.tvDoctorName.setText("Doctor: " + doctor);

        holder.tvDate.setText(DateFormatter.formatToFullDate(dateString));

        // 4. Status Button - UPDATE THIS DYNAMICALLY
        updateStatusButton(holder.btnStatus, status);
        holder.itemView.setOnClickListener(v -> {
            if (onHistoryClickListener != null) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onHistoryClickListener.onHistoryClick(model, getSnapshots().getSnapshot(adapterPosition));
                }
            }
        });

    }

    private void updateStatusButton(MaterialButton button, String status) {
        String displayStatus = status.toUpperCase();
        button.setText(displayStatus);

        // Set colors based on status
        switch (status.toLowerCase()) {
            case "completed":
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#508C5A3D"))); // Green
                break;
            case "pending":
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#508C5A3D"))); // Orange
                break;
            case "confirmed":
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#508C5A3D"))); // Blue
                break;
            case "cancelled":
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#508C5A3D"))); // Red
                break;
            default:
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#508C5A3D"))); // Gray
                break;
        }
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (itemCountChangeListener != null) {
            itemCountChangeListener.onCountChange(getItemCount());
        }
        loading = false;
    }
    public void updateQuery(Query newQuery) {
        // Stop listening to old query
        stopListening();

        // Update options with new query
        updateOptions(new FirestoreRecyclerOptions.Builder<Appointment>()
                .setQuery(newQuery, Appointment.class)
                .build());

        // Start listening to new query
        startListening();
    }
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceType;
        TextView tvDate;
        TextView tvDoctorName;
        MaterialButton btnStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvDate = itemView.findViewById(R.id.tvDateHistory);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorNameHistory);
            btnStatus = itemView.findViewById(R.id.btnStatusHistory);
        }
    }
}
