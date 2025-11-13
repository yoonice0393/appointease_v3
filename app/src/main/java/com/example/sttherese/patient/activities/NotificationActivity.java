package com.example.sttherese.patient.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.NotificationAdapter;
import com.example.sttherese.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ImageView backBtn;
    private LinearLayout btnAll, btnUnread;
    private TextView markAllRead, allCount, unreadCount;
    private RecyclerView recyclerView;
    private CardView noNotifCard;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private List<Notification> notifList;
    private List<Notification> allNotifications;
    private NotificationAdapter adapter;

    private boolean showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI elements
        backBtn = findViewById(R.id.buttonBack);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        markAllRead = findViewById(R.id.markAllRead);
        allCount = findViewById(R.id.allCount);
        unreadCount = findViewById(R.id.unreadCount);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        noNotifCard = findViewById(R.id.NoNotifCard);

        backBtn.setOnClickListener(v -> onBackPressed());

        notifList = new ArrayList<>();
        allNotifications = new ArrayList<>();

//        adapter = new NotificationAdapter(notifList, notification -> {
//            // Mark as read when clicked
//            if (!notification.isRead()) {
//                db.collection("notifications")
//                        .document(notification.getId())
//                        .update("isRead", true)
//                        .addOnSuccessListener(aVoid -> {
//                            notification.setRead(true);
//                            adapter.notifyDataSetChanged();
//                            updateCounts();
//                        });
//            }
//        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String userId = auth.getCurrentUser().getUid();

        // Set initial filter state
        setActiveFilter(true);

        // Fetch and listen for changes in notifications
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Notifications", "Listen failed.", error);
                        return;
                    }

                    allNotifications.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Notification n = doc.toObject(Notification.class);
                            if (n != null) {
                                n.setId(doc.getId());
                                allNotifications.add(n);
                            }
                        }
                    }

                    // Update display based on current filter
                    if (showingAll) {
                        showAllNotifications();
                    } else {
                        showUnreadNotifications();
                    }

                    updateCounts();
                });

        // Filter: All Notifications
        btnAll.setOnClickListener(v -> {
            showingAll = true;
            setActiveFilter(true);
            showAllNotifications();
        });

        // Filter: Unread Notifications
        btnUnread.setOnClickListener(v -> {
            showingAll = false;
            setActiveFilter(false);
            showUnreadNotifications();
        });

        // Mark all as read
        markAllRead.setOnClickListener(v -> {
            for (Notification n : allNotifications) {
                if (!n.isRead()) {
                    db.collection("notifications")
                            .document(n.getId())
                            .update("isRead", true);
                }
            }
        });
    }

    private void showAllNotifications() {
        notifList.clear();
        notifList.addAll(allNotifications);
        adapter.notifyDataSetChanged();
        noNotifCard.setVisibility(notifList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(notifList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showUnreadNotifications() {
        notifList.clear();
        for (Notification n : allNotifications) {
            if (!n.isRead()) {
                notifList.add(n);
            }
        }
        adapter.notifyDataSetChanged();
        noNotifCard.setVisibility(notifList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(notifList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateCounts() {
        int total = allNotifications.size();
        int unread = 0;

        for (Notification n : allNotifications) {
            if (!n.isRead()) {
                unread++;
            }
        }

        allCount.setText(String.valueOf(total));
        unreadCount.setText(String.valueOf(unread));

        // Hide mark all as read if no unread notifications
        markAllRead.setVisibility(unread > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void setActiveFilter(boolean isAll) {
        if (isAll) {
            btnAll.setBackgroundResource(R.drawable.filter_button_active);
            btnUnread.setBackgroundResource(R.drawable.filter_button_bg);
        } else {
            btnAll.setBackgroundResource(R.drawable.filter_button_bg);
            btnUnread.setBackgroundResource(R.drawable.filter_button_active);
        }
    }
}