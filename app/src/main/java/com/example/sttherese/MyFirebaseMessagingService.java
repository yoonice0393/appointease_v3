package com.example.sttherese;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.sttherese.patient.activities.NotificationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String SERVER_URL = "https://sttherese-api.onrender.com/save-fcm-token1.php";
    private static final String CHANNEL_ID = "appointment_notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Save notification to Realtime Database
        saveNotificationToRTDB(remoteMessage);

        // Show push notification
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body);
        }
    }

    /**
     * Save incoming notification to Realtime Database
     */
    private void saveNotificationToRTDB(RemoteMessage remoteMessage) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "User not logged in, cannot save notification");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Saving notification for user: " + userId);

        RemoteMessage.Notification notification = remoteMessage.getNotification();

        if (notification == null) {
            Log.e(TAG, "Notification payload is null");
            return;
        }

        Log.d(TAG, "Title: " + notification.getTitle());
        Log.d(TAG, "Body: " + notification.getBody());

        // Prepare notification data
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", userId);
        notifData.put("title", notification.getTitle());
        notifData.put("message", notification.getBody());
        notifData.put("isRead", false);
        notifData.put("timestamp", System.currentTimeMillis());

        if (remoteMessage.getData().containsKey("type")) {
            notifData.put("type", remoteMessage.getData().get("type"));
            Log.d(TAG, "Type: " + remoteMessage.getData().get("type"));
        }
        if (remoteMessage.getData().containsKey("status")) {
            notifData.put("status", remoteMessage.getData().get("status"));
            Log.d(TAG, "Status: " + remoteMessage.getData().get("status"));
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app");
        DatabaseReference notificationsRef = database.getReference("notifications")
                .child(userId)
                .push();


        notificationsRef.setValue(notifData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Notification saved successfully to RTDB: " + notificationsRef.getKey());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving notification to RTDB", e);
                });
    }
    private void showNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Appointment Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for appointment updates");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendTokenToServer(String fcmToken) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.e(TAG, "Cannot send token: FCM token is null or empty");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "Cannot send token: User not logged in. Token will be sent after login.");
            return;
        }

        String uid = currentUser.getUid();
        Log.d(TAG, "Sending FCM token to server for UID: " + uid);

        JSONObject json = new JSONObject();
        try {
            json.put("uid", uid);
            json.put("fcmToken", fcmToken);
            json.put("platform", "android");
        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error", e);
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send FCM token to server", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "No response body";

                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM token sent successfully!");
                    Log.d(TAG, "Response: " + responseBody);
                } else {
                    Log.e(TAG, "Server returned error: " + response.code());
                    Log.e(TAG, "Error response: " + responseBody);
                }
            }
        });
    }
}