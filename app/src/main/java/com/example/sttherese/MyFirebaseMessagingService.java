package com.example.sttherese;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.sttherese.patient.activities.NotificationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private static final String CHANNEL_ID = "st_therese_notif_channel_v4";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendTokenToServer(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        saveNotificationToRTDB(remoteMessage);

        String title = "St. Therese Clinic";
        String body = "You have a new update.";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().getOrDefault("title", title);
            body = remoteMessage.getData().getOrDefault("body", remoteMessage.getData().getOrDefault("message", body));
        }

        showNotification(title, body);
    }

    private void saveNotificationToRTDB(RemoteMessage remoteMessage) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role_type", "patient");
        String appId = "doctor".equals(role) ? prefs.getString("doctor_doc_id", null) : prefs.getString("patient_firestore_id", null);

        if (appId == null) return; // Wait for ID to be ready

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", appId);
        notifData.put("title", remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : remoteMessage.getData().get("title"));
        notifData.put("message", remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : remoteMessage.getData().getOrDefault("body", remoteMessage.getData().get("message")));
        notifData.put("isRead", false);
        notifData.put("timestamp", System.currentTimeMillis());
        if (remoteMessage.getData().containsKey("status")) notifData.put("status", remoteMessage.getData().get("status"));
        if (remoteMessage.getData().containsKey("type")) notifData.put("type", remoteMessage.getData().get("type"));

        // PATH FIXED: Now saved under /notifications/{appId} to match your structure
        FirebaseDatabase.getInstance("https://appointease-7aa63-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("notifications").child(appId).push().setValue(notifData);
    }

    private void showNotification(String title, String body) {
        createNotificationChannel();
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
                .setAutoCancel(true).setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_HIGH).setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Clinic Updates", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    public static void sendTokenToServer(Context context, String fcmToken) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || fcmToken == null) return;

        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String role = prefs.getString("user_role_type", "patient");
        String appId = "doctor".equals(role) ? prefs.getString("doctor_doc_id", null) : prefs.getString("patient_firestore_id", null);

        if (appId == null) return; // Wait for Firestore ID lookup

        JSONObject json = new JSONObject();
        try {
            json.put("uid", appId);
            json.put("fcmToken", fcmToken);
            json.put("platform", "android");
            json.put("role", role);
        } catch (JSONException e) { return; }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
                .newCall(new Request.Builder().url(SERVER_URL).post(body).build()).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {}
                    @Override public void onResponse(Call call, Response response) throws IOException { response.close(); }
                });
    }
}
