package com.example.sttherese.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.models.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notifications) {
        this(notifications, null);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = notifications.get(position);
        holder.title.setText(n.getTitle());
        holder.message.setText(n.getMessage());
        holder.unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
        holder.card.setCardElevation(n.isRead() ? 0f : 4f);
        holder.icon.setImageResource(getIconForNotification(n));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(n);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // ADDED: Method to update the list
    public void updateList(List<Notification> newNotifications) {
        this.notifications.clear();
        this.notifications.addAll(newNotifications);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, message;
        ImageView icon;
        View unreadDot;
        CardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.notificationCard);
            icon = itemView.findViewById(R.id.notificationIcon);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }

    private int getIconForNotification(Notification notification) {
        String text = ((notification.getTitle() == null ? "" : notification.getTitle()) + " " +
                (notification.getMessage() == null ? "" : notification.getMessage()) + " " +
                (notification.getType() == null ? "" : notification.getType())).toLowerCase();

        if (text.contains("today") || text.contains("scheduled") || text.contains("appointment")) {
            return R.drawable.ic_clock2;
        }
        if (text.contains("completed") || text.contains("finished")) {
            return R.drawable.ic_info;
        }
        return R.drawable.ic_notif;
    }
}
