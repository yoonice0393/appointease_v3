package com.example.sttherese.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private final List<Appointment> appointments = new ArrayList<>();
    private final Context context;
    private final OnAppointmentClickListener listener;
    private final OnDataStatusChangeListener statusListener;
    private ListenerRegistration firestoreListener;
    private List<Appointment> originalAppointments = new ArrayList<>();
    private String activeSearchQuery = null;
    private final String mode;

    public interface OnAppointmentClickListener {
        void onItemClick(Appointment appointment);
    }

    public interface OnDataStatusChangeListener {
        void onDataLoaded(int itemCount);
    }

    public AppointmentAdapter(Context context, OnAppointmentClickListener listener, Query query,
                              OnDataStatusChangeListener statusListener, String mode) {
        this.context = context;
        this.listener = listener;
        this.statusListener = statusListener;
        this.mode = mode;

        // Listen for Firestore changes
        firestoreListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("AppointmentAdapter", "Listen failed.", e);
                if (statusListener != null) statusListener.onDataLoaded(0);
                return;
            }

            if (snapshots == null) return;

            List<Appointment> currentAppointments = new ArrayList<>(appointments);

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                Appointment appointment = dc.getDocument().toObject(Appointment.class);
                appointment.setId(dc.getDocument().getId());

                if ("doctor".equalsIgnoreCase(mode)) {
                    // DOCTOR SIDE: Fetch patient info
                    fetchPatientInfo(appointment);
                } else if ("patient".equalsIgnoreCase(mode)) {
                    // PATIENT SIDE: Fetch doctor info
                    fetchDoctorInfo(appointment);
                }

                switch (dc.getType()) {
                    case ADDED:
                        currentAppointments.add(appointment);
                        break;
                    case MODIFIED:
                        currentAppointments.removeIf(a -> a.getId().equals(appointment.getId()));
                        currentAppointments.add(appointment);
                        break;
                    case REMOVED:
                        currentAppointments.removeIf(a -> a.getId().equals(appointment.getId()));
                        break;
                }
            }

            updateData(currentAppointments);

            // Apply active search if exists
            if (activeSearchQuery != null && !activeSearchQuery.isEmpty()) {
                filterByName(activeSearchQuery);
            } else if (statusListener != null) {
                statusListener.onDataLoaded(appointments.size());
            }
        });
    }

    private void fetchPatientInfo(Appointment appointment) {
        if (appointment.getUserId() == null || appointment.getUserId().isEmpty()) {
            Log.w("AppointmentAdapter", "No userId for appointment: " + appointment.getId());
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("patients")
                .whereEqualTo("userId", appointment.getUserId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        var userDoc = querySnapshot.getDocuments().get(0);

                        String firstName = userDoc.getString("first_name");
                        String lastName = userDoc.getString("last_name");
                        String name = userDoc.getString("name");

                        // Build full name
                        String patientName = (firstName != null && lastName != null)
                                ? firstName + " " + lastName
                                : (name != null ? name : "Patient");

                        String avatar = userDoc.getString("avatar");

                        appointment.setPatientName(patientName);
                        appointment.setPatientAvatar(avatar);

                        // Re-apply filter if active
                        if (activeSearchQuery != null && !activeSearchQuery.isEmpty()) {
                            filterByName(activeSearchQuery);
                        } else {
                            notifyDataSetChanged();
                        }
                    } else {
                        Log.w("AppointmentAdapter", "No patient found for userId: " + appointment.getUserId());
                        appointment.setPatientName("Patient");
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e("AppointmentAdapter", "Failed to load patient info", ex);
                    appointment.setPatientName("Patient");
                });
    }

    private void fetchDoctorInfo(Appointment appointment) {
        if (appointment.getDoctorId() == null || appointment.getDoctorId().isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("doctors")
                .whereEqualTo("doctorId", appointment.getDoctorId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        var doc = querySnapshot.getDocuments().get(0);

                        String name = doc.getString("name");
                        String avatar = doc.getString("avatar");
                        String specialty = doc.getString("specialty");

                        appointment.setDoctorName(name);
                        appointment.setDoctorAvatar(avatar);
                        appointment.setSpecialty(specialty);

                        notifyDataSetChanged();

                        if (statusListener != null) {
                            statusListener.onDataLoaded(appointments.size());
                        }
                    } else {
                        Log.w("AppointmentAdapter", "No doctor found for doctorId: " + appointment.getDoctorId());
                    }
                })
                .addOnFailureListener(ex ->
                        Log.e("AppointmentAdapter", "Failed to load doctor info", ex));
    }

    public void filterByName(String query) {
        activeSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        appointments.clear();

        if (activeSearchQuery == null) {
            // Show all if no search query
            appointments.addAll(originalAppointments);
        } else {
            String lowerCaseQuery = activeSearchQuery.toLowerCase(Locale.getDefault());

            for (Appointment appointment : originalAppointments) {
                String searchTarget = null;

                if ("doctor".equalsIgnoreCase(mode)) {
                    searchTarget = appointment.getPatientName();
                } else if ("patient".equalsIgnoreCase(mode)) {
                    searchTarget = appointment.getDoctorName();
                }

                // Filter based on name
                if (searchTarget != null &&
                        searchTarget.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    appointments.add(appointment);
                }
            }
        }

        notifyDataSetChanged();

        if (statusListener != null) {
            statusListener.onDataLoaded(appointments.size());
        }
    }

    private void updateData(List<Appointment> newAppointments) {
        this.appointments.clear();
        this.appointments.addAll(newAppointments);

        this.originalAppointments.clear();
        this.originalAppointments.addAll(newAppointments);

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_appointment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        if (appointment == null) return;

        String topName, subtitle, avatarUrl;
        int placeholderResId;

        if ("patient".equalsIgnoreCase(mode)) {
            // PATIENT sees DOCTOR info
            topName = safeText(appointment.getDoctorName(), "Doctor");
            subtitle = safeText(appointment.getSpecialty(), "Specialty");
            avatarUrl = appointment.getDoctorAvatar();
            placeholderResId = R.drawable.ic_doctor_placeholder;

        } else {
            // DOCTOR sees PATIENT info
            topName = safeText(appointment.getPatientName(), "Patient");
            subtitle = safeText(appointment.getSpecialty(), "Service");
            avatarUrl = appointment.getPatientAvatar();
            placeholderResId = R.drawable.ic_patient_placeholder;
        }

        // Bind text
        holder.tvName.setText(topName);
        holder.tvSubtitle.setText(subtitle);
        holder.tvAppointmentDate.setText(safeText(appointment.getDate(), "No date"));
        holder.tvAppointmentTime.setText(safeText(appointment.getTime(), "No time"));

        // Bind avatar
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(placeholderResId)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(placeholderResId);
        }

        // Click listener
        if (listener != null) {
            holder.btnNavigate.setOnClickListener(v -> listener.onItemClick(appointment));
        }
    }

    private String safeText(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value : fallback;
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, btnNavigate;
        TextView tvName, tvSubtitle, tvAppointmentDate, tvAppointmentTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
        }
    }

    public void removeListener() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}