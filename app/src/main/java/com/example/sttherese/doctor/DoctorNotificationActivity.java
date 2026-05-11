package com.example.sttherese.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sttherese.R;
import com.example.sttherese.adapters.NotificationAdapter;
import com.example.sttherese.models.Notification;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorNotificationActivity extends AppCompatActivity {

    private static final String TAG = "DoctorNotifActivity";

    private ImageView backBtn;
    private LinearLayout btnAll, btnUnread;
    private TextView markAllRead, allCount, unreadCount;
    private RecyclerView recyclerView;
    private CardView noNotifCard;

    private DatabaseReference notificationsRef;
    private List<Notification> notifList;
    private List<Notification> allNotifications;
    private NotificationAdapter adapter;

    private boolean showingAll = true;
    private String doctorDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_notification);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        doctorDocId = prefs.getString("doctor_doc_id", null);

        initializeUI();
        setupRecyclerView();

        if (doctorDocId == null) {
            // Self-healing: Fetch Doctor Firestore ID if missing
            fetchIdAndStartListening();
        } else {
            startListening();
        }
    }

    private void fetchIdAndStartListening() {
        String authUid = FirebaseAuth.getInstance().getUid();
        if (authUid == null) return;

        FirebaseFirestore.getInstance().collection("doctors")
                .whereEqualTo("user_id", authUid).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        doctorDocId = q.getDocuments().get(0).getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("doctor_doc_id", doctorDocId).apply();
                        startListening();
                    }
                });
    }

    private void startListening() {
        Log.d(TAG, "Listening for doctor notifications under path: notifications/" + doctorDocId);
        // FIXED PATH: Match the structure /notifications/{doctorDocId}
        notificationsRef = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("notifications").child(doctorDocId);

        notificationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allNotifications.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Notification n = child.getValue(Notification.class);
                            if (n != null) {
                                n.setId(child.getKey());
                                allNotifications.add(n);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parsing error", e);
                        }
                    }
                }

                Collections.sort(allNotifications, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                refreshDisplay();
                updateCounts();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "DB error: " + error.getMessage());
            }
        });
    }

    private void initializeUI() {
        backBtn = findViewById(R.id.buttonBack);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        markAllRead = findViewById(R.id.markAllRead);
        allCount = findViewById(R.id.allCount);
        unreadCount = findViewById(R.id.unreadCount);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        noNotifCard = findViewById(R.id.NoNotifCard);

        backBtn.setOnClickListener(v -> onBackPressed());

        btnAll.setOnClickListener(v -> {
            showingAll = true;
            setActiveFilter(true);
            refreshDisplay();
        });

        btnUnread.setOnClickListener(v -> {
            showingAll = false;
            setActiveFilter(false);
            refreshDisplay();
        });

        markAllRead.setOnClickListener(v -> {
            if (doctorDocId == null || notificationsRef == null) return;
            Map<String, Object> updates = new HashMap<>();
            for (Notification n : allNotifications) {
                if (!n.isRead() && n.getId() != null) {
                    n.setIsRead(true);
                    updates.put(n.getId() + "/isRead", true);
                }
            }
            refreshDisplay();
            updateCounts();
            if (!updates.isEmpty()) notificationsRef.updateChildren(updates);
        });
    }

    private void setupRecyclerView() {
        notifList = new ArrayList<>();
        allNotifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifList, this::handleNotificationClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        setActiveFilter(true);
    }

    private void handleNotificationClick(Notification notification) {
        if (notification == null) return;

        markNotificationRead(notification);

        showNotificationDetailDialog(notification);
    }

    private void showNotificationDetailDialog(Notification notification) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView message = dialogView.findViewById(R.id.textNotificationDialogMessage);
        MaterialButton openButton = dialogView.findViewById(R.id.buttonOpenNotification);
        MaterialButton closeButton = dialogView.findViewById(R.id.buttonCloseNotification);

        String title = notification.getTitle() != null ? notification.getTitle() : "Notification";
        String body = notification.getMessage() != null ? notification.getMessage() : "";
        message.setText(body.trim().isEmpty() ? title : title + "\n" + body);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        openButton.setOnClickListener(v -> {
            dialog.dismiss();
            openRelatedPage(notification);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void markNotificationRead(Notification notification) {
        if (notification == null || notification.isRead()) return;
        notification.setIsRead(true);
        refreshDisplay();
        updateCounts();
        if (notificationsRef != null && notification.getId() != null) {
            notificationsRef.child(notification.getId()).child("isRead").setValue(true);
        }
    }

    private void openRelatedPage(Notification notification) {
        String routeText = ((notification.getType() == null ? "" : notification.getType()) + " " +
                (notification.getStatus() == null ? "" : notification.getStatus()) + " " +
                (notification.getTitle() == null ? "" : notification.getTitle()) + " " +
                (notification.getMessage() == null ? "" : notification.getMessage())).toLowerCase();

        if (routeText.contains("completed") || routeText.contains("finished")) {
            startActivity(new Intent(this, DoctorHistoryActivity.class));
        } else {
            startActivity(new Intent(this, DoctorCalendarActivity.class));
        }
    }

    private void refreshDisplay() {
        notifList.clear();
        if (showingAll) {
            for (Notification n : allNotifications) if (!n.isRead()) notifList.add(n);
            for (Notification n : allNotifications) if (n.isRead()) notifList.add(n);
        } else {
            for (Notification n : allNotifications) if (!n.isRead()) notifList.add(n);
        }
        adapter.notifyDataSetChanged();
        
        boolean empty = notifList.isEmpty();
        noNotifCard.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateCounts() {
        int total = allNotifications.size();
        int unread = 0;
        for (Notification n : allNotifications) if (!n.isRead()) unread++;
        allCount.setText(String.valueOf(total));
        unreadCount.setText(String.valueOf(unread));
        markAllRead.setVisibility(unread > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    private void setActiveFilter(boolean isAll) {
        btnAll.setBackgroundResource(isAll ? R.drawable.filter_button_active : R.drawable.filter_button_bg);
        btnUnread.setBackgroundResource(!isAll ? R.drawable.filter_button_active : R.drawable.filter_button_bg);
    }
}
