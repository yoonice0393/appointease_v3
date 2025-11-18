package com.example.sttherese.adapters; // Use the same package

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
// Ensure you use the correct imports
import com.example.sttherese.R;
import com.example.sttherese.models.Appointment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Renamed the class
public class DoctorHistoryAdapter extends FirestoreRecyclerAdapter<Appointment, DoctorHistoryAdapter.DoctorHistoryViewHolder> {


    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final OnItemCountChangeListener itemCountChangeListener;

    public DoctorHistoryAdapter(Query query, OnItemCountChangeListener listener) {
        super(new FirestoreRecyclerOptions.Builder<Appointment>()
                .setQuery(query, Appointment.class)
                .build());
        this.itemCountChangeListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull DoctorHistoryViewHolder holder, int position, @NonNull Appointment model) {

        // **STEP 1: Clear and prepare for ASYNCHRONOUS lookup**
        holder.tvPatientName.setText("Loading Patient...");

        String appointmentUserId = model.getUserId(); // This is the user ID from the appointment (e.g., CdHK51q5u...)

        if (appointmentUserId != null && !appointmentUserId.isEmpty()) {

            // 2. Perform a QUERY on the 'patients' collection
            //    Find the patient document where the 'userId' field matches the appointment's userId.
            db.collection("patients")
                    .whereEqualTo("userId", appointmentUserId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {

                            // Found the patient document! Extract the data from the first result.
                            DocumentSnapshot patientDoc = querySnapshot.getDocuments().get(0);

                            String firstName = patientDoc.getString("first_name");
                            String lastName = patientDoc.getString("last_name");

                            String fullName = "";
                            if (firstName != null) fullName += firstName;
                            if (lastName != null) {
                                if (!fullName.isEmpty()) fullName += " ";
                                fullName += lastName;
                            }

                            // 3. Display the result
                            if (holder.getAdapterPosition() == position) {
                                holder.tvPatientName.setText(fullName);
                                model.setPatientName(fullName);
                            }
                        } else {
                            // Patient document not found for this userId
                            if (holder.getAdapterPosition() == position) {
                                holder.tvPatientName.setText("Patient: Profile Not Found (ID mismatch)");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Adapter", "Error fetching patient profile: " + e.getMessage());
                        if (holder.getAdapterPosition() == position) {
                            holder.tvPatientName.setText("Patient: Error Loading Data");
                        }
                    });
        } else {
            holder.tvPatientName.setText("Patient: ID Missing in Appointment");
        }

        // 4. Bind the rest of the data immediately (Date, Service, Status)

        // Service/Type
        String service = model.getSpecialty() != null ? model.getSpecialty() : "Unknown Service";
        holder.tvServiceType.setText(service);

        // 4. Date & Time Formatting
        String dateString = model.getDate() != null ? model.getDate() : "N/A";
        String timeString = model.getTime() != null ? model.getTime() : "";
        String formattedDate = dateString;
        try {
            SimpleDateFormat firebaseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = firebaseFormat.parse(dateString);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            formattedDate = displayFormat.format(date);
        } catch (Exception e) {
            // Fallback
        }
        holder.tvDate.setText(formattedDate + " at " + timeString);

        // 4. Status
        String status = model.getStatus() != null ? model.getStatus().toUpperCase() : "UNKNOWN";
        holder.tvStatusButton.setText(status);
        // (Add logic here to set background color for the button if needed)
    }

    @NonNull
    @Override
    public DoctorHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You can use the same XML (history_card.xml) if you map the IDs correctly,
        // or create a dedicated layout (history_card_doctor.xml).
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.doctor_history_card, parent, false);
        return new DoctorHistoryViewHolder(view);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (itemCountChangeListener != null) {
            itemCountChangeListener.onCountChange(getItemCount());
        }
    }

    // Renamed the ViewHolder
    public static class DoctorHistoryViewHolder extends RecyclerView.ViewHolder {
        // Mapped to history_card.xml views (using original IDs for DoctorName, Date, etc.)
        TextView tvPatientName; // We will use tvDoctorNameHistory for this
        TextView tvServiceType;
        TextView tvDate;
        TextView tvStatusButton;

        public DoctorHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // CRITICAL MAPPING: Use tvDoctorNameHistory ID for the Patient's Name display
            tvPatientName = itemView.findViewById(R.id.tvPatientNameHistory);
            tvServiceType = itemView.findViewById(R.id.tvServiceTypeHistory);
            tvDate = itemView.findViewById(R.id.tvDateHistory);
            tvStatusButton = itemView.findViewById(R.id.button);
        }
    }
}