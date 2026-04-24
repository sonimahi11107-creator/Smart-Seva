package com.example.smartseva;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "SmartSeva_Channel";
    private static final String CHANNEL_NAME = "Smart Seva Notifications";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "Smart Seva";
        String body  = "Aapke liye ek update hai!";

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle();
            body  = message.getNotification().getBody();
        }

        // Data payload bhi check karo
        if (message.getData().size() > 0) {
            if (message.getData().containsKey("title"))
                title = message.getData().get("title");
            if (message.getData().containsKey("body"))
                body = message.getData().get("body");
        }

        sendNotification(title, body, message.getData().get("screen"));
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Token ko Firebase mein save karo — teammate handle karega
        // saveFCMToken(token);
    }

    void sendNotification(String title, String body, String screen) {
        // Click pe kaunsi screen khulegi
        Intent intent;
        if ("dashboard".equals(screen)) {
            intent = new Intent(this, DashboardActivity.class);
        } else if ("tasks".equals(screen)) {
            intent = new Intent(this, CreateTaskActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
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

        // Android 8+ ke liye channel zaroori hai
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Smart Seva app notifications");
            manager.createNotificationChannel(channel);
        }

        manager.notify(0, builder.build());
    }
}