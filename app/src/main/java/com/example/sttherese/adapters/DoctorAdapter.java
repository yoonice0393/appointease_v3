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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.ViewHolder> {

    private Context context;
    private List<Doctor> doctors = new ArrayList<>();
    private OnDoctorClickListener listener;
    private Query query;
    private ListenerRegistration registration;

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    public DoctorAdapter(Context context, OnDoctorClickListener listener, Query query) {
        this.context = context;
        this.listener = listener;
        this.query = query;
        listenToQuery();
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doctor doctor = doctors.get(position);
        holder.tvDoctorName.setText(doctor.getName());
        holder.tvDoctorSpecialty.setText(doctor.getSpecialty());

        Glide.with(context)
                .load(doctor.getAvatarUrl())
                .placeholder(R.drawable.ic_doctor)
                .circleCrop()
                .into(holder.ivDoctorAvatar);

        holder.itemView.setOnClickListener(v -> listener.onDoctorClick(doctor));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoctorName, tvDoctorSpecialty;
        ImageView ivDoctorAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorNameCard);
            tvDoctorSpecialty = itemView.findViewById(R.id.tvDoctorSpecialtyCard);
            ivDoctorAvatar = itemView.findViewById(R.id.ivDoctorImage);
        }
    }
}
