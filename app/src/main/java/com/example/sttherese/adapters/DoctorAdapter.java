package com.example.sttherese.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sttherese.R;
import com.example.sttherese.models.Doctor;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.ViewHolder> {

    private Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Doctor> doctors = new ArrayList<>();
    private OnDoctorClickListener listener;
    private Query query;
    private ListenerRegistration registration;
    private final int itemLayoutId;

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    public DoctorAdapter(Context context, OnDoctorClickListener listener, Query query, int layoutId) {
        this.context = context;
        this.listener = listener;
        this.query = query;
        this.itemLayoutId = layoutId;
        listenToQuery();
    }
    public DoctorAdapter(Context context, OnDoctorClickListener listener, Query query) {
        // Call the main 4-argument constructor, passing the default layout ID
        this(context, listener, query, R.layout.item_doctor);
    }

    private void listenToQuery() {
        if (query != null) {
            registration = query.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Toast.makeText(context, "Error loading doctors", Toast.LENGTH_SHORT).show();
                    return;
                }
                doctors.clear();
                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Doctor doctor = doc.toObject(Doctor.class);
                        doctor.setId(doc.getId()); // Set Firestore document ID
                        doctors.add(doctor);
                    }
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void removeListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    public void updateList(List<Doctor> newList) {
        doctors.clear();
        doctors.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(itemLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doctor doctor = doctors.get(position);
        holder.tvDoctorName.setText(doctor.getName());
        holder.tvDoctorSpecialty.setText(doctor.getSpecialty());

        // 1. Reset text while loading
        holder.tvDoctorScheduleTime.setText("Loading Schedule...");

        // 2. Fetch and display the schedule time
        fetchAndDisplaySchedule(doctor.getId(), holder.tvDoctorScheduleTime); // <-- NEW CALL

        Glide.with(context)
                .load(doctor.getAvatarUrl())
                .placeholder(R.drawable.ic_doctor_placeholder)
                .circleCrop()
                .into(holder.ivDoctorAvatar);

        holder.itemView.setOnClickListener(v -> listener.onDoctorClick(doctor));
    }

    private void fetchAndDisplaySchedule(String doctorId, TextView textView) {
        // Get today's day abbreviation (e.g., "THU")
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
        String todayAbbr = dayFormat.format(Calendar.getInstance().getTime()).toUpperCase(Locale.US);

        // Construct the schedule document ID (e.g., "D001_THU")
        String scheduleDocId = doctorId + "_" + todayAbbr;

        db.collection("clinic_schedules")
                .document(scheduleDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String startTime = documentSnapshot.getString("start_time"); // e.g., "9:00"
                        String endTime = documentSnapshot.getString("end_time");     // e.g., "14:00"

                        // Convert to a user-friendly format (e.g., 9:00 AM - 2:00 PM)
                        String displayTime = formatTimeRange(startTime, endTime);
                        textView.setText("Today: " + displayTime);
                    } else {
                        textView.setText("Today: No Schedule");
                    }
                })
                .addOnFailureListener(e -> {
                    textView.setText("Schedule Error");
                    // Log.e("DoctorAdapter", "Failed to fetch schedule for " + doctorId, e); // Good practice to log
                });
    }

    /**
     * Converts "H:mm" time strings (e.g., 9:00, 14:00) into "h:mm a" format (e.g., 9:00 AM, 2:00 PM).
     */
    private String formatTimeRange(String startTimeStr, String endTimeStr) {
        if (startTimeStr == null || endTimeStr == null) return "N/A";

        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

            String startDisplay = displayFormat.format(dbFormat.parse(startTimeStr));
            String endDisplay = displayFormat.format(dbFormat.parse(endTimeStr));

            return startDisplay + " - " + endDisplay;

        } catch (Exception e) {
            return "Invalid Format";
        }
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvDoctorSpecialty;
        TextView tvDoctorScheduleTime;
        ImageView ivDoctorAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorNameCard);
            tvDoctorSpecialty = itemView.findViewById(R.id.tvDoctorSpecialtyCard);
            ivDoctorAvatar = itemView.findViewById(R.id.ivDoctorImage);
            tvDoctorScheduleTime = itemView.findViewById(R.id.tvDoctorScheduleTime);
        }
    }
}
