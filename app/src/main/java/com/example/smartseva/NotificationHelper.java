package com.example.smartseva;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    // Channel IDs
    static final String CHANNEL_TASKS  = "channel_tasks";
    static final String CHANNEL_STATUS = "channel_status";
    static final String CHANNEL_APPLY  = "channel_apply";

    // ── Channels create karo ─────────────────────────────
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_TASKS, "New Tasks",
                    NotificationManager.IMPORTANCE_HIGH));

            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_STATUS, "Task Status Updates",
                    NotificationManager.IMPORTANCE_DEFAULT));

            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_APPLY, "Applications",
                    NotificationManager.IMPORTANCE_HIGH));
        }
    }

    // ── 1. Naya task ─────────────────────────────────────
    public static void notifyNewTask(Context context, String taskTitle) {
        send(context, CHANNEL_TASKS, 1001,
                "📋 Naya Task Create Hua!",
                taskTitle + " — volunteers ki zaroorat hai",
                DashboardActivity.class);
    }

    // ── 2. Status change ─────────────────────────────────
    public static void notifyStatusChange(Context context,
                                          String taskTitle, String newStatus) {
        send(context, CHANNEL_STATUS, 1002,
                "🔄 Task Status Update",
                taskTitle + " → " + newStatus,
                DashboardActivity.class);
    }

    // ── 3. Volunteer ne apply kiya ───────────────────────
    public static void notifyNewApplication(Context context,
                                            String volunteerName, String taskTitle) {
        send(context, CHANNEL_APPLY, 1003,
                "🙋 Naya Application Aaya!",
                (volunteerName != null ? volunteerName : "Volunteer")
                        + " ne apply kiya: " + taskTitle,
                DashboardActivity.class);
    }

    // ── 4. Application result ────────────────────────────
    public static void notifyApplicationResult(Context context,
                                               String taskTitle, boolean accepted) {
        send(context, CHANNEL_STATUS, 1004,
                accepted ? "✅ Application Accept Hua!"
                        : "❌ Application Reject Hua",
                taskTitle + (accepted
                        ? " — Congratulations! Taiyaar ho jao."
                        : " — Agli baar zaroor try karo."),
                DashboardActivity.class);
    }

    // ── Internal send helper ─────────────────────────────
    private static void send(Context context, String channel,
                             int id, String title, String message, Class<?> target) {
        try {
            Intent intent = new Intent(context, target);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pi = PendingIntent.getActivity(
                    context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, channel)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(message))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .setContentIntent(pi);

            NotificationManagerCompat nm =
                    NotificationManagerCompat.from(context);
            if (android.os.Build.VERSION.SDK_INT < 33 ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.POST_NOTIFICATIONS) == 0) {
                nm.notify(id, builder.build());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}