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
import com.example.sttherese.DateFormatter;
import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Comparator;
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
    private String activeDateFilter = null;
    private String activeMinDate = null;
    private List<String> activeAllowedStatuses = null;
    private int activeLimit = -1;
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

            List<Appointment> currentAppointments = new ArrayList<>(originalAppointments);

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
            applyActiveFilters();
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
                        com.google.firebase.firestore.DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);

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

                        applyActiveFilters();
                    } else {
                        Log.w("AppointmentAdapter", "No patient found for userId: " + appointment.getUserId());
                        appointment.setPatientName("Patient");
                        applyActiveFilters();
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e("AppointmentAdapter", "Failed to load patient info", ex);
                    appointment.setPatientName("Patient");
                    applyActiveFilters();
                });
    }

    private void fetchDoctorInfo(Appointment appointment) {
        if (appointment.getDoctorId() == null || appointment.getDoctorId().isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("doctors")
                .document(appointment.getDoctorId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("name");
                        String avatar = doc.getString("avatar");
                        String specialty = doc.getString("specialty");

                        appointment.setDoctorName(name);
                        appointment.setDoctorAvatar(avatar);
                        appointment.setSpecialty(specialty);
                    } else {
                        Log.w("AppointmentAdapter", "No doctor found for doctorId: " + appointment.getDoctorId());
                    }

                    applyActiveFilters();
                })
                .addOnFailureListener(ex ->
                        Log.e("AppointmentAdapter", "Failed to load doctor info", ex));
    }

    public void filterByName(String query) {
        activeSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        applyActiveFilters();
    }

    public void setDateFilter(String dateFilter) {
        activeDateFilter = (dateFilter != null && !dateFilter.trim().isEmpty()) ? dateFilter.trim() : null;
        applyActiveFilters();
    }

    public void setAllowedStatuses(List<String> statuses) {
        activeAllowedStatuses = statuses;
        applyActiveFilters();
    }

    public void applyFilters(String query, String dateFilter, List<String> allowedStatuses) {
        activeSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        activeDateFilter = (dateFilter != null && !dateFilter.trim().isEmpty()) ? dateFilter.trim() : null;
        activeMinDate = null;
        activeAllowedStatuses = allowedStatuses;
        activeLimit = -1;
        applyActiveFilters();
    }

    public void applyFilters(String query, String dateFilter, String minDate, List<String> allowedStatuses) {
        activeSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        activeDateFilter = (dateFilter != null && !dateFilter.trim().isEmpty()) ? dateFilter.trim() : null;
        activeMinDate = (minDate != null && !minDate.trim().isEmpty()) ? minDate.trim() : null;
        activeAllowedStatuses = allowedStatuses;
        activeLimit = -1;
        applyActiveFilters();
    }

    public void applyFilters(String query, String dateFilter, String minDate, List<String> allowedStatuses, int limit) {
        activeSearchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        activeDateFilter = (dateFilter != null && !dateFilter.trim().isEmpty()) ? dateFilter.trim() : null;
        activeMinDate = (minDate != null && !minDate.trim().isEmpty()) ? minDate.trim() : null;
        activeAllowedStatuses = allowedStatuses;
        activeLimit = limit;
        applyActiveFilters();
    }

    private void applyActiveFilters() {
        appointments.clear();

        String lowerCaseQuery = activeSearchQuery == null
                ? null
                : activeSearchQuery.toLowerCase(Locale.getDefault());

        for (Appointment appointment : originalAppointments) {
            if (!matchesStatusFilter(appointment)) {
                continue;
            }

            if (!matchesDateFilter(appointment)) {
                continue;
            }

            if (lowerCaseQuery != null) {
                String searchTarget = "";

                if ("doctor".equalsIgnoreCase(mode)) {
                    searchTarget = safeText(appointment.getPatientName(), "") + " " +
                            safeText(appointment.getSpecialty(), "") + " " +
                            safeText(appointment.getAppointmentType(), "") + " " +
                            safeText(appointment.getDate(), "");
                } else if ("patient".equalsIgnoreCase(mode)) {
                    searchTarget = safeText(appointment.getDoctorName(), "") + " " +
                            safeText(appointment.getSpecialty(), "") + " " +
                            safeText(appointment.getAppointmentType(), "") + " " +
                            safeText(appointment.getDate(), "");
                }

                // Filter based on name
                if (searchTarget != null &&
                        searchTarget.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    appointments.add(appointment);
                }
            } else {
                appointments.add(appointment);
            }

            if (activeLimit > 0 && appointments.size() >= activeLimit) {
                break;
            }
        }

        notifyDataSetChanged();

        if (statusListener != null) {
            statusListener.onDataLoaded(appointments.size());
        }
    }

    private boolean matchesStatusFilter(Appointment appointment) {
        if (activeAllowedStatuses == null || activeAllowedStatuses.isEmpty()) {
            return true;
        }

        String status = safeText(appointment.getStatus(), "").toLowerCase(Locale.getDefault());
        for (String allowedStatus : activeAllowedStatuses) {
            if (status.equals(safeText(allowedStatus, "").toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDateFilter(Appointment appointment) {
        String appointmentDate = appointment.getDate();
        if (activeDateFilter != null) {
            return activeDateFilter.equals(appointmentDate);
        }

        return activeMinDate == null || (appointmentDate != null && appointmentDate.compareTo(activeMinDate) >= 0);
    }

    private void updateData(List<Appointment> newAppointments) {
        newAppointments.sort(Comparator
                .comparing((Appointment appointment) -> safeText(appointment.getDate(), "9999-12-31"))
                .thenComparing(appointment -> safeText(appointment.getTime(), "23:59")));
        this.originalAppointments.clear();
        this.originalAppointments.addAll(newAppointments);
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
        holder.tvAppointmentDate.setText(formatDisplayDate(appointment.getDate()));
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

    private String formatDisplayDate(String date) {
        return date != null && !date.trim().isEmpty()
                ? DateFormatter.formatToFullDate(date)
                : "No date";
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
