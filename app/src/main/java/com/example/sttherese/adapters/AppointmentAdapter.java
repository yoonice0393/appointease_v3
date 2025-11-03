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

    private List<Appointment> appointments = new ArrayList<>();
    private Context context;
    private OnItemClickListener listener;
    private ListenerRegistration firestoreListener;

    public interface OnItemClickListener {
        void onItemClick(Appointment appointment);
    }

    public AppointmentAdapter(Context context, OnItemClickListener listener, Query query) {
        this.context = context;
        this.listener = listener;

        // Listen for real-time updates
        firestoreListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e("AppointmentAdapter", "Listen failed.", e);
                return;
            }

            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED:
                        appointments.add(dc.getDocument().toObject(Appointment.class));
                        break;
                    case MODIFIED:
                        Appointment updated = dc.getDocument().toObject(Appointment.class);
                        for (int i = 0; i < appointments.size(); i++) {
                            if (appointments.get(i).getId() == updated.getId()) {
                                appointments.set(i, updated);
                                break;
                            }
                        }
                        break;
                    case REMOVED:
                        Appointment removed = dc.getDocument().toObject(Appointment.class);
                        appointments.removeIf(a -> a.getId() == removed.getId());
                        break;
                }
            }
            notifyDataSetChanged();
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
        holder.tvDoctorName.setText(appointment.getDoctorName());
        holder.tvDoctorSpecialty.setText(appointment.getSpecialty());
        holder.tvAppointmentDate.setText(appointment.getDate());
        holder.tvAppointmentTime.setText(appointment.getTime());

        if (appointment.getDoctorAvatar() != null && !appointment.getDoctorAvatar().isEmpty()) {
            Glide.with(context).load(appointment.getDoctorAvatar())
                    .placeholder(R.drawable.ic_doctor).circleCrop().into(holder.ivDoctorAvatar);
        } else {
            holder.ivDoctorAvatar.setImageResource(R.drawable.ic_doctor);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(appointment));
        holder.btnNavigate.setOnClickListener(v -> listener.onItemClick(appointment));
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDoctorAvatar, btnNavigate;
        TextView tvDoctorName, tvDoctorSpecialty, tvAppointmentDate, tvAppointmentTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDoctorAvatar = itemView.findViewById(R.id.ivDoctorAvatar);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvDoctorSpecialty = itemView.findViewById(R.id.tvDoctorSpecialty);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvAppointmentTime = itemView.findViewById(R.id.tvAppointmentTime);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
        }
    }

    public void removeListener() {
        if (firestoreListener != null) firestoreListener.remove();
    }
}
