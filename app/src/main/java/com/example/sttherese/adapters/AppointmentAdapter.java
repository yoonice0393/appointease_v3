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

    public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

        private final List<Appointment> appointments = new ArrayList<>();
        private final Context context;
        private final OnAppointmentClickListener listener;
        private final OnDataStatusChangeListener statusListener;
        private ListenerRegistration firestoreListener;

        private final String mode; // "patient" or "doctor"

        // Listener interfaces
        public interface OnAppointmentClickListener {
            void onItemClick(Appointment appointment);
        }

        public interface OnDataStatusChangeListener {
            void onDataLoaded(int itemCount);
        }

        // Updated constructor
            public AppointmentAdapter(Context context, OnAppointmentClickListener listener, Query query,
                                  OnDataStatusChangeListener statusListener, String mode) {
            this.context = context;
            this.listener = listener;
            this.statusListener = statusListener;
            this.mode = mode; // define whether it's patient or doctor side

            // Listen for Firestore changes
            firestoreListener = query.addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.e("AppointmentAdapter", "Listen failed.", e);
                    if (statusListener != null) statusListener.onDataLoaded(0);
                    return;
                }

                List<Appointment> currentAppointments = new ArrayList<>(appointments);

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Appointment appointment = dc.getDocument().toObject(Appointment.class);
                    appointment.setId(dc.getDocument().getId());

                    if ("doctor".equalsIgnoreCase(mode)) {
                        // Doctor side: fetch patient info from patients collection
                        if (appointment.getUserId() != null && !appointment.getUserId().isEmpty()) {

                            // 🚨 CRITICAL FIX: Change from .document() to a .whereEqualTo() query
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("patients")
                                    .whereEqualTo("userId", appointment.getUserId()) // ✅ Query the 'userId' field
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(querySnapshots -> { // Change parameter name to querySnapshots
                                        if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                            // Get the one matching document
                                            com.google.firebase.firestore.DocumentSnapshot userDoc = querySnapshots.getDocuments().get(0);

                                            // Use the correct field names from the patient collection (e.g., 'first_name', 'last_name')
                                            // Assuming your 'Appointment' model has a field for the full name, you may need to concatenate:
                                            String firstName = userDoc.getString("first_name"); // Based on screenshot
                                            String lastName = userDoc.getString("last_name");
                                            String pname = (firstName != null && lastName != null) ? firstName + " " + lastName : userDoc.getString("name");

                                            String pavatar = userDoc.getString("avatar");

                                            appointment.setPatientName(pname);
                                            appointment.setPatientAvatar(pavatar);

                                            // refresh the UI when data is fetched
                                            notifyDataSetChanged();
                                        } else {
                                            Log.w("AppointmentAdapter", "Patient document not found for Auth UID: " + appointment.getUserId());
                                        }
                                    })
                                    .addOnFailureListener(ex ->
                                            Log.e("AppointmentAdapter", "Failed to load patient info", ex));
                        }

                    } else if ("patient".equalsIgnoreCase(mode)) {
                        // Patient side: fetch doctor info from doctors collection
                        if (appointment.getDoctorId() != null && !appointment.getDoctorId().isEmpty()) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("doctors")
                                    .whereEqualTo("doctorId", appointment.getDoctorId())
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                            com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                            String dname = doc.getString("name");
                                            String davatar = doc.getString("avatar");
                                            String specialty = doc.getString("specialty");
                                            appointment.setDoctorName(dname);
                                            appointment.setDoctorAvatar(davatar);
                                            appointment.setSpecialty(specialty);
                                            if (statusListener != null) statusListener.onDataLoaded(appointments.size());
                                            notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(ex -> Log.e("AppointmentAdapter", "Failed to load doctor info", ex));
                        }
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

                appointments.clear();
                appointments.addAll(currentAppointments);
                notifyDataSetChanged();

                if (statusListener != null) statusListener.onDataLoaded(appointments.size());
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Appointment appointment = appointments.get(position);
            if (appointment == null) return;

            String topName, subtitle, avatarUrl, placeholderType;

            if ("patient".equalsIgnoreCase(mode)) {
                // PATIENT sees DOCTOR info
                topName = safeText(appointment.getDoctorName(), "Doctor");
                subtitle = safeText(appointment.getSpecialty(), "Specialty not specified");
                avatarUrl = appointment.getDoctorAvatar();
                placeholderType = "doctor";

                // 🔹 Fetch doctor info if not yet loaded
                if ((appointment.getDoctorName() == null || appointment.getDoctorName().isEmpty()) &&
                        appointment.getDoctorId() != null && !appointment.getDoctorId().isEmpty()) {

                    FirebaseFirestore.getInstance()
                            .collection("doctors")
                            .whereEqualTo("doctorId", appointment.getDoctorId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                    var doc = querySnapshot.getDocuments().get(0);
                                    appointment.setDoctorName(doc.getString("name"));
                                    appointment.setDoctorAvatar(doc.getString("avatar"));
                                    appointment.setSpecialty(doc.getString("specialty"));

                                    // ✅ Update UI for this holder
                                    holder.tvName.setText(doc.getString("name"));
                                    holder.tvSubtitle.setText(doc.getString("specialty"));
                                    Glide.with(context)
                                            .load(doc.getString("avatar"))
                                            .placeholder(R.drawable.ic_doctor)
                                            .circleCrop()
                                            .into(holder.ivAvatar);
                                }
                            })
                            .addOnFailureListener(e -> Log.e("AppointmentAdapter", "Failed to load doctor info", e));
                }

            } else {
                // DOCTOR sees PATIENT info
                topName = safeText(appointment.getPatientName(), "Patient");
                subtitle = safeText(appointment.getService(), "Service not specified");
                avatarUrl = appointment.getPatientAvatar();
                placeholderType = "user";

                // 🔹 Fetch patient info if not yet loaded
                if ((appointment.getPatientName() == null || appointment.getPatientName().isEmpty()) &&
                        appointment.getUserId() != null && !appointment.getUserId().isEmpty()) {

                    FirebaseFirestore.getInstance()
                            .collection("patients")
                            .document(appointment.getUserId())
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc != null && userDoc.exists()) {
                                    appointment.setPatientName(userDoc.getString("name"));
                                    appointment.setPatientAvatar(userDoc.getString("avatar"));

                                    // ✅ Update UI for this holder
                                    holder.tvName.setText(userDoc.getString("name"));
                                    Glide.with(context)
                                            .load(userDoc.getString("avatar"))
                                            .placeholder(R.drawable.ic_doctor_placeholder)
                                            .circleCrop()
                                            .into(holder.ivAvatar);
                                }
                            })
                            .addOnFailureListener(e -> Log.e("AppointmentAdapter", "Failed to load patient info", e));
                }
            }

            // Text binding
            holder.tvName.setText(topName);
            holder.tvSubtitle.setText(subtitle);
            holder.tvAppointmentDate.setText(safeText(appointment.getDate(), "No date"));
            holder.tvAppointmentTime.setText(safeText(appointment.getTime(), "No time"));

            // Avatar binding
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                Glide.with(context)
                        .load(avatarUrl)
                        .placeholder(
                                placeholderType.equals("doctor") ?
                                        R.drawable.ic_doctor :
                                        R.drawable.ic_doctor_placeholder)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(
                        placeholderType.equals("doctor") ?
                                R.drawable.ic_doctor :
                                R.drawable.ic_doctor_placeholder);
            }

            // Button click
            if (listener != null) {
                holder.btnNavigate.setOnClickListener(v -> listener.onItemClick(appointment));
            }
        }


        // 🔹 Helper method to handle null/empty text gracefully
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
                ivAvatar = itemView.findViewById(R.id.ivAvatar);           // new id from revised layout
                tvName = itemView.findViewById(R.id.tvName);              // was tvDoctorName before
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);      // was tvDoctorSpecialty before
                tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
                tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
                btnNavigate = itemView.findViewById(R.id.btnNavigate);
            }
        }


        public void removeListener() {
            if (firestoreListener != null) firestoreListener.remove();
        }
    }
