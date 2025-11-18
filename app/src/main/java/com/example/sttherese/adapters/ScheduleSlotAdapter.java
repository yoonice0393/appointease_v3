package com.example.sttherese.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.models.ScheduleSlot; // Assuming the model is updated

import java.util.List;

public class ScheduleSlotAdapter extends RecyclerView.Adapter<ScheduleSlotAdapter.ViewHolder> {

    public interface OnSlotInteractionListener {
        void onBookSlotClicked(ScheduleSlot slot);
    }

    private final List<ScheduleSlot> slots;
    private final OnSlotInteractionListener listener;
    private final Context context;

    public ScheduleSlotAdapter(Context context, List<ScheduleSlot> slots, OnSlotInteractionListener listener) {
        this.context = context;
        this.slots = slots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleSlot slot = slots.get(position);

        // ✅ FIX 1: Map the 'day' from the model to the new tvScheduleDayName TextView
        holder.tvDayName.setText(slot.getDay());
        holder.tvDayShort.setText(slot.getDayShort());

        // ✅ FIX 2: Map the display date to the existing tvScheduleDate TextView
        holder.tvDate.setText(slot.getDate());

        // ✅ FIX 3: Combine Start and End Times for the single time TextView
        String timeRange = String.format("%s - %s", slot.getStartTime(), slot.getEndTime());
        holder.tvTime.setText(timeRange);

//// Use ContextCompat for safer color retrieval (assuming green_available/grey_booked are defined)
//        if ("Available".equalsIgnoreCase(slot.getStatus())) {
//            holder.btnBook.setText("Book");
//            // Assuming R.color.green_available is a valid resource, or define one (e.g., #81C784)
//            // holder.btnBook.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green_available));
//            holder.btnBook.setEnabled(true);
//            // **✅ FIX: Set listener on the new button**
//            holder.btnBook.setOnClickListener(v -> listener.onBookSlotClicked(slot));
//        } else {
//            // Logic for a booked or unavailable day
//            holder.btnBook.setText("Booked");
//            // Assuming R.color.grey_booked is a valid resource
//            // holder.btnBook.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.grey_booked));
//            holder.btnBook.setEnabled(false);
//            holder.btnBook.setOnClickListener(null);
//        }

        // **❗ IMPORTANT: Populate dayShort in DoctorsActivity.java (See step 3)**
        holder.tvDayName.setText(slot.getDay());
        holder.tvDayShort.setText(slot.getDayShort());
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // ✅ NEW: TextView for the day name
        TextView tvDayName;
        TextView tvDayShort;
        TextView tvDate;
        TextView tvTime;
//        Button btnBook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // ✅ Initialization for the new ID
            tvDayName = itemView.findViewById(R.id.tvScheduleDayName);
            tvDayShort=itemView.findViewById(R.id.tvDayShort);
            tvDate = itemView.findViewById(R.id.tvScheduleDate);
            tvTime = itemView.findViewById(R.id.tvScheduleTime);
//            btnBook = itemView.findViewById(R.id.btnBookSlot);
        }
    }
}