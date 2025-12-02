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
import com.google.android.material.button.MaterialButton;
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
                        doctor.setId(doc.getId());
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

        if (holder.tvDoctorScheduleTime != null) {
            holder.tvDoctorScheduleTime.setText("Loading Schedule...");
            fetchAndDisplaySchedule(doctor.getId(), holder.tvDoctorScheduleTime);
        }

        Glide.with(context)
                .load(doctor.getAvatarUrl())
                .placeholder(R.drawable.ic_doctor_placeholder)
                .circleCrop()
                .into(holder.ivDoctorAvatar);

        // ✅ FIX: Check if button exists. If yes, use button click; if no, use card click
        if (holder.btnViewSchedule != null) {
            // Layout WITH button - only button is clickable
            holder.itemView.setOnClickListener(null);
            holder.btnViewSchedule.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDoctorClick(doctor);
                }
            });
        } else {
            // Layout WITHOUT button - entire card is clickable (for selection dialog)
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDoctorClick(doctor);
                }
            });
        }
    }

    private void fetchAndDisplaySchedule(String doctorId, TextView textView) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
        String todayAbbr = dayFormat.format(Calendar.getInstance().getTime()).toUpperCase(Locale.US);
        String scheduleDocId = doctorId + "_" + todayAbbr;

        db.collection("clinic_schedules")
                .document(scheduleDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String startTime = documentSnapshot.getString("start_time");
                        String endTime = documentSnapshot.getString("end_time");
                        String displayTime = formatTimeRange(startTime, endTime);
                        textView.setText("Today: " + displayTime);
                    } else {
                        textView.setText("Today: No Schedule");
                    }
                })
                .addOnFailureListener(e -> {
                    textView.setText("Schedule Error");
                });
    }

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
        MaterialButton btnViewSchedule;
        TextView tvDoctorName, tvDoctorSpecialty;
        TextView tvDoctorScheduleTime;
        ImageView ivDoctorAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorNameCard);
            tvDoctorSpecialty = itemView.findViewById(R.id.tvDoctorSpecialtyCard);
            ivDoctorAvatar = itemView.findViewById(R.id.ivDoctorImage);
            btnViewSchedule = itemView.findViewById(R.id.btnViewSchedule);
            tvDoctorScheduleTime = itemView.findViewById(R.id.tvDoctorScheduleTime);
        }
    }
}