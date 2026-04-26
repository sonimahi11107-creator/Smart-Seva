package com.example.smartseva;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG          = "FCMService";
    private static final String CHANNEL_ID   = "SmartSeva_Channel";
    private static final String CHANNEL_NAME = "Smart Seva Notifications";

    // ── 1. Message receive ───────────────────────────────
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title  = "Smart Seva";
        String body   = "Aapke liye ek update hai!";
        String screen = null;

        // Notification payload
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null)
                title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null)
                body = message.getNotification().getBody();
        }

        // Data payload overrides notification payload
        Map<String, String> data = message.getData();
        if (!data.isEmpty()) {
            if (data.containsKey("title"))  title  = data.get("title");
            if (data.containsKey("body"))   body   = data.get("body");
            if (data.containsKey("screen")) screen = data.get("screen");
        }

        sendNotification(title, body, screen);
    }

    // ── 2. Token refresh — Firestore mein save karo ──────
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM Token refreshed: " + token);
        saveFCMToken(token);
    }

    // ── 3. Token Firestore mein save karo ────────────────
    private void saveFCMToken(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not logged in — token not saved");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", token);
        update.put("tokenUpdatedAt", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(update)
                .addOnSuccessListener(v -> Log.d(TAG, "Token saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Token save failed", e));
    }

    // ── 4. Notification show karo ────────────────────────
    void sendNotification(String title, String body, String screen) {
        // Screen routing
        Intent intent;
        if ("dashboard".equals(screen)) {
            intent = new Intent(this, DashboardActivity.class);
        } else if ("tasks".equals(screen)) {
            intent = new Intent(this, CreateTaskActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Unique ID so notifications stack instead of replacing each other
        int notifId = (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, notifId, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return; // null safety

        // Channel — Android 8+ ke liye (ideally app start pe banao)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Smart Seva app notifications");
            manager.createNotificationChannel(channel);
        }

        manager.notify(notifId, builder.build());
    }
}