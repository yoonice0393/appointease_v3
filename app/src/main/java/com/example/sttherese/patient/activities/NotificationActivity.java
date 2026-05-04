package com.example.sttherese.patient.activities;

import android.content.SharedPreferences;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotifActivity";

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
    private String patientFirestoreId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        patientFirestoreId = prefs.getString("patient_firestore_id", null);

        initializeUI();
        setupRecyclerView();

        if (patientFirestoreId == null) {
            fetchIdAndStartListening();
        } else {
            startListening();
        }
    }

    private void fetchIdAndStartListening() {
        String authUid = FirebaseAuth.getInstance().getUid();
        if (authUid == null) return;

        FirebaseFirestore.getInstance().collection("patients")
                .whereEqualTo("userId", authUid).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        patientFirestoreId = q.getDocuments().get(0).getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                .putString("patient_firestore_id", patientFirestoreId).apply();
                        startListening();
                    }
                });
    }

    private void startListening() {
        Log.d(TAG, "Listening for patient notifications under path: notifications/" + patientFirestoreId);
        // FIXED PATH: Match the structure /notifications/{patientFirestoreId}
        notificationsRef = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("notifications").child(patientFirestoreId);

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

            @Override public void onCancelled(DatabaseError error) {
                Log.e(TAG, "DB Error: " + error.getMessage());
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
        btnAll.setOnClickListener(v -> { showingAll = true; setActiveFilter(true); refreshDisplay(); });
        btnUnread.setOnClickListener(v -> { showingAll = false; setActiveFilter(false); refreshDisplay(); });
        markAllRead.setOnClickListener(v -> {
            if (patientFirestoreId == null || notificationsRef == null) return;
            for (Notification n : allNotifications) {
                if (!n.isRead()) notificationsRef.child(n.getId()).child("isRead").setValue(true);
            }
        });
    }

    private void setupRecyclerView() {
        notifList = new ArrayList<>();
        allNotifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        setActiveFilter(true);
    }

    private void refreshDisplay() {
        notifList.clear();
        if (showingAll) notifList.addAll(allNotifications);
        else for (Notification n : allNotifications) if (!n.isRead()) notifList.add(n);
        adapter.notifyDataSetChanged();
        noNotifCard.setVisibility(notifList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(notifList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateCounts() {
        int total = allNotifications.size(), unread = 0;
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
