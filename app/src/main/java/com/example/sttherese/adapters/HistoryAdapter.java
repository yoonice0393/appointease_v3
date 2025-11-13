package com.example.sttherese.adapters;


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
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

// Assuming you have this interface for callback in your project
// public interface OnItemCountChangeListener { void onCountChange(int count); }

public class HistoryAdapter extends FirestoreRecyclerAdapter<Appointment, HistoryAdapter.HistoryViewHolder> {

    private final OnItemCountChangeListener itemCountChangeListener;

    public HistoryAdapter(Query query, OnItemCountChangeListener listener) {
        super(new FirestoreRecyclerOptions.Builder<Appointment>()
                .setQuery(query, Appointment.class)
                .build());
        this.itemCountChangeListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull HistoryViewHolder holder, int position, @NonNull Appointment model) {
        // NOTE: Firestore maps 'appointmentType' field from bookings to 'specialty' in model
        String service = model.getSpecialty() != null ? model.getSpecialty() : "Unknown Service";
        String doctor = model.getDoctorName() != null ? model.getDoctorName() : "Doctor TBA";
        String dateString = model.getDate() != null ? model.getDate() : "N/A";

        // 1. Service/Type
        holder.tvServiceType.setText(service);

        // 2. Doctor Name (assuming doctorName is denormalized in the booking)
        holder.tvDoctorName.setText("Doctor: " + doctor);

        // 3. Date Formatting (Assuming date format is "yyyy-MM-dd")
        try {
            SimpleDateFormat firebaseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = firebaseFormat.parse(dateString);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            holder.tvDate.setText(displayFormat.format(date));
        } catch (Exception e) {
            holder.tvDate.setText(dateString); // Fallback to raw date string
        }

        // Status button remains "COMPLETED" as we are only querying completed statuses
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_card, parent, false);
        return new HistoryViewHolder(view);
    }

    // This is important for managing empty state in the activity
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (itemCountChangeListener != null) {
            itemCountChangeListener.onCountChange(getItemCount());
        }
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        // Updated IDs for clarity
        TextView tvServiceType;
        TextView tvDate;
        TextView tvDoctorName;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Mapped to the new explicit IDs
            tvServiceType = itemView.findViewById(R.id.tvServiceTypeHistory);
            tvDate = itemView.findViewById(R.id.tvDateHistory);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorNameHistory);
        }
    }
}