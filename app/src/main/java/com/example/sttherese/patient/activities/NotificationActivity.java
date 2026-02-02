package com.example.sttherese.patient.activities;

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

    private FirebaseAuth auth;
    private DatabaseReference notificationsRef;

    private List<Notification> notifList;
    private List<Notification> allNotifications;
    private NotificationAdapter adapter;

    private boolean showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        Log.d(TAG, "=== NotificationActivity Started ===");

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in!");
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Current User ID: " + userId);


        FirebaseDatabase database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app");
        notificationsRef = database.getReference("notifications");

        // Initialize UI elements
        backBtn = findViewById(R.id.buttonBack);
        btnAll = findViewById(R.id.btnAll);
        btnUnread = findViewById(R.id.btnUnread);
        markAllRead = findViewById(R.id.markAllRead);
        allCount = findViewById(R.id.allCount);
        unreadCount = findViewById(R.id.unreadCount);
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        noNotifCard = findViewById(R.id.NoNotifCard);

        backBtn.setOnClickListener(v -> onBackPressed());

        // Initialize lists
        notifList = new ArrayList<>();
        allNotifications = new ArrayList<>();

        // Setup adapter
        adapter = new NotificationAdapter(notifList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set initial filter state
        setActiveFilter(true);

        Log.d(TAG, "Listening to: notifications/" + userId);

        // Listen for changes in Realtime Database
        notificationsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "=== Data Changed ===");
                Log.d(TAG, "Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Children count: " + snapshot.getChildrenCount());

                allNotifications.clear();

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Log.d(TAG, "Processing child: " + childSnapshot.getKey());

                    try {
                        Notification n = childSnapshot.getValue(Notification.class);
                        if (n != null) {
                            n.setId(childSnapshot.getKey());
                            allNotifications.add(n);
                            Log.d(TAG, "✅ Added notification: " + n.getTitle());
                        } else {
                            Log.e(TAG, "❌ Notification is null after parsing");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error parsing notification: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "Total notifications loaded: " + allNotifications.size());

                // Sort by timestamp (newest first)
                Collections.sort(allNotifications, (n1, n2) ->
                        Long.compare(n2.getTimestamp(), n1.getTimestamp())
                );

                // Update display based on current filter
                if (showingAll) {
                    showAllNotifications();
                } else {
                    showUnreadNotifications();
                }

                updateCounts();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Database error: " + error.getMessage());
                error.toException().printStackTrace();
            }
        });

        // Filter: All Notifications
        btnAll.setOnClickListener(v -> {
            Log.d(TAG, "All button clicked");
            showingAll = true;
            setActiveFilter(true);
            showAllNotifications();
        });

        // Filter: Unread Notifications
        btnUnread.setOnClickListener(v -> {
            Log.d(TAG, "Unread button clicked");
            showingAll = false;
            setActiveFilter(false);
            showUnreadNotifications();
        });

        // Mark all as read
        markAllRead.setOnClickListener(v -> {
            Log.d(TAG, "Mark all as read clicked");
            for (Notification n : allNotifications) {
                if (!n.isRead()) {
                    notificationsRef.child(userId).child(n.getId())
                            .child("isRead").setValue(true);
                }
            }
        });
    }

    private void showAllNotifications() {
        Log.d(TAG, "showAllNotifications() - Total: " + allNotifications.size());

        notifList.clear();
        notifList.addAll(allNotifications);
        adapter.notifyDataSetChanged();

        boolean isEmpty = notifList.isEmpty();
        noNotifCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        Log.d(TAG, "RecyclerView visibility: " + (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d(TAG, "NoNotifCard visibility: " + (noNotifCard.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
    }

    private void showUnreadNotifications() {
        Log.d(TAG, "showUnreadNotifications()");

        notifList.clear();
        for (Notification n : allNotifications) {
            if (!n.isRead()) {
                notifList.add(n);
            }
        }
        adapter.notifyDataSetChanged();

        boolean isEmpty = notifList.isEmpty();
        noNotifCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        Log.d(TAG, "Unread count: " + notifList.size());
    }

    private void updateCounts() {
        int total = allNotifications.size();
        int unread = 0;

        for (Notification n : allNotifications) {
            if (!n.isRead()) {
                unread++;
            }
        }

        Log.d(TAG, "Updating counts - Total: " + total + ", Unread: " + unread);

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