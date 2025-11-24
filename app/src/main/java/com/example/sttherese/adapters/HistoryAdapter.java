package com.example.sttherese.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryAdapter extends FirestoreRecyclerAdapter<Appointment, HistoryAdapter.HistoryViewHolder> {

    public interface OnLoadMoreListener {
        void onLoadMore();
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

    public HistoryAdapter(Query query, OnItemCountChangeListener countListener, OnLoadMoreListener loadMoreListener) {
        super(new FirestoreRecyclerOptions.Builder<Appointment>()
                .setQuery(query, Appointment.class)
                .build());
        this.itemCountChangeListener = countListener;
        this.onLoadMoreListener = loadMoreListener;
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

        // 3. Date Formatting
        try {
            SimpleDateFormat firebaseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = firebaseFormat.parse(dateString);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            holder.tvDate.setText(displayFormat.format(date));
        } catch (Exception e) {
            holder.tvDate.setText(dateString);
        }

        // 4. Status Button - UPDATE THIS DYNAMICALLY
        updateStatusButton(holder.btnStatus, status);

        // Pagination trigger
        if (position >= getItemCount() - 1 && hasMoreData && !loading && !loadMoreTriggered && onLoadMoreListener != null) {
            loading = true;
            loadMoreTriggered = true;
            onLoadMoreListener.onLoadMore();
        }
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