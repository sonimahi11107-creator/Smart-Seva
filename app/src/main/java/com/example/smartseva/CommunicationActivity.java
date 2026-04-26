package com.example.smartseva;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommunicationActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseDatabase rtdb;
    FirebaseFirestore db;

    // Tabs
    Button btnTabChat, btnTabAnnouncements, btnTabComments;
    LinearLayout panelChat, panelAnnouncements, panelComments;

    // Chat
    LinearLayout layoutMessages;
    EditText etChatMessage;
    Button btnSendChat;
    ScrollView scrollChat;
    String chatPartnerId, chatPartnerName;

    // Announcements
    LinearLayout layoutAnnouncements;
    EditText etAnnouncement;
    Button btnSendAnnouncement;
    ScrollView scrollAnnouncements;

    // Task Comments
    LinearLayout layoutComments;
    EditText etComment;
    Button btnSendComment;
    ScrollView scrollComments;
    Spinner spinnerTasks;
    String selectedTaskId = "general";

    // User info
    String currentUserId, currentUserName, currentRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        mAuth = FirebaseAuth.getInstance();
        rtdb  = FirebaseDatabase.getInstance();
        db    = FirebaseFirestore.getInstance();

        android.content.SharedPreferences prefs =
                getSharedPreferences("SmartSeva", MODE_PRIVATE);
        currentRole = prefs.getString("role", "Volunteer");
        currentUserName = prefs.getString(
                currentRole.equals("NGO") ? "orgName"
                        : "volunteerName", "User");

        currentUserId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "unknown";

        chatPartnerId   = getIntent()
                .getStringExtra("chatPartnerId");
        chatPartnerName = getIntent()
                .getStringExtra("chatPartnerName");

        bindViews();
        setListeners();
        switchTab("chat");
    }

    void bindViews() {
        btnTabChat          = findViewById(R.id.btnTabChat);
        btnTabAnnouncements = findViewById(
                R.id.btnTabAnnouncements);
        btnTabComments      = findViewById(
                R.id.btnTabComments);
        panelChat           = findViewById(R.id.panelChat);
        panelAnnouncements  = findViewById(
                R.id.panelAnnouncements);
        panelComments       = findViewById(
                R.id.panelComments);
        layoutMessages      = findViewById(
                R.id.layoutMessages);
        etChatMessage       = findViewById(
                R.id.etChatMessage);
        btnSendChat         = findViewById(
                R.id.btnSendChat);
        scrollChat          = findViewById(
                R.id.scrollChat);
        layoutAnnouncements = findViewById(
                R.id.layoutAnnouncements);
        etAnnouncement      = findViewById(
                R.id.etAnnouncement);
        btnSendAnnouncement = findViewById(
                R.id.btnSendAnnouncement);
        scrollAnnouncements = findViewById(
                R.id.scrollAnnouncements);
        layoutComments      = findViewById(
                R.id.layoutComments);
        etComment           = findViewById(
                R.id.etComment);
        btnSendComment      = findViewById(
                R.id.btnSendComment);
        scrollComments      = findViewById(
                R.id.scrollComments);
        spinnerTasks        = findViewById(
                R.id.spinnerTasks);
    }

    void setListeners() {
        findViewById(R.id.btnBackComm)
                .setOnClickListener(v -> finish());

        btnTabChat.setOnClickListener(v ->
                switchTab("chat"));
        btnTabAnnouncements.setOnClickListener(v ->
                switchTab("announcements"));
        btnTabComments.setOnClickListener(v ->
                switchTab("comments"));

        btnSendChat.setOnClickListener(v -> sendChatMessage());
        btnSendAnnouncement.setOnClickListener(v ->
                sendAnnouncement());
        btnSendComment.setOnClickListener(v ->
                sendTaskComment());

        // Task spinner
        spinnerTasks.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view,
                            int position, long id) {
                        selectedTaskId = "task_" + position;
                        loadTaskComments(selectedTaskId);
                    }
                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {}
                });

        loadTasksForSpinner();
    }

    // ── TAB SWITCHING ─────────────────────────────────────

    void switchTab(String tab) {
        panelChat.setVisibility(
                tab.equals("chat") ? View.VISIBLE : View.GONE);
        panelAnnouncements.setVisibility(
                tab.equals("announcements")
                        ? View.VISIBLE : View.GONE);
        panelComments.setVisibility(
                tab.equals("comments")
                        ? View.VISIBLE : View.GONE);

        setTabActive(btnTabChat,
                tab.equals("chat"));
        setTabActive(btnTabAnnouncements,
                tab.equals("announcements"));
        setTabActive(btnTabComments,
                tab.equals("comments"));

        if (tab.equals("chat")) loadChat();
        if (tab.equals("announcements")) loadAnnouncements();
        if (tab.equals("comments"))
            loadTaskComments(selectedTaskId);
    }

    void setTabActive(Button btn, boolean active) {
        btn.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(
                        active ? "#1A1A2E" : "#F3F4F6")));
        btn.setTextColor(Color.parseColor(
                active ? "#FFFFFF" : "#6B7280"));
    }

    // ── CHAT ──────────────────────────────────────────────

    void loadChat() {
        layoutMessages.removeAllViews();

        // Chat ID — sorted UIDs se banao
        String chatId = getChatId();

        rtdb.getReference("chats").child(chatId)
                .addChildEventListener(
                        new ChildEventListener() {
                            @Override
                            public void onChildAdded(
                                    DataSnapshot snap,
                                    String prev) {
                                String senderId = snap.child("senderId")
                                        .getValue(String.class);
                                String senderName = snap.child("senderName")
                                        .getValue(String.class);
                                String message = snap.child("message")
                                        .getValue(String.class);
                                Long timestamp = snap.child("timestamp")
                                        .getValue(Long.class);

                                boolean isMe = currentUserId
                                        .equals(senderId);
                                addMessageBubble(message,
                                        senderName, timestamp,
                                        isMe);
                            }
                            @Override public void onChildChanged(
                                    DataSnapshot s, String p) {}
                            @Override public void onChildRemoved(
                                    DataSnapshot s) {}
                            @Override public void onChildMoved(
                                    DataSnapshot s, String p) {}
                            @Override public void onCancelled(
                                    DatabaseError e) {}
                        });
    }

    void sendChatMessage() {
        String msg = etChatMessage.getText()
                .toString().trim();
        if (msg.isEmpty()) return;

        String chatId = getChatId();
        Map<String, Object> message = new HashMap<>();
        message.put("senderId",   currentUserId);
        message.put("senderName", currentUserName);
        message.put("message",    msg);
        message.put("timestamp",
                System.currentTimeMillis());

        rtdb.getReference("chats")
                .child(chatId).push().setValue(message)
                .addOnSuccessListener(v ->
                        etChatMessage.setText(""));

        // Notification
        NotificationHelper.notifyNewTask(this,
                "New message from " + currentUserName);
    }

    String getChatId() {
        if (chatPartnerId != null
                && !chatPartnerId.isEmpty()) {
            // Sort IDs for consistent chat room
            String[] ids = {currentUserId, chatPartnerId};
            Arrays.sort(ids);
            return ids[0] + "_" + ids[1];
        }
        return "general_chat";
    }

    // ── ANNOUNCEMENTS ─────────────────────────────────────

    void loadAnnouncements() {
        layoutAnnouncements.removeAllViews();

        rtdb.getReference("announcements")
                .orderByChild("timestamp")
                .limitToLast(50)
                .addChildEventListener(
                        new ChildEventListener() {
                            @Override
                            public void onChildAdded(
                                    DataSnapshot snap,
                                    String prev) {
                                String title = snap.child("title")
                                        .getValue(String.class);
                                String body = snap.child("body")
                                        .getValue(String.class);
                                String sender = snap.child("senderName")
                                        .getValue(String.class);
                                Long ts = snap.child("timestamp")
                                        .getValue(Long.class);
                                addAnnouncementCard(
                                        title, body, sender, ts);
                            }
                            @Override public void onChildChanged(
                                    DataSnapshot s, String p) {}
                            @Override public void onChildRemoved(
                                    DataSnapshot s) {}
                            @Override public void onChildMoved(
                                    DataSnapshot s, String p) {}
                            @Override public void onCancelled(
                                    DatabaseError e) {}
                        });
    }

    void sendAnnouncement() {
        if (!currentRole.equals("NGO")) {
            Toast.makeText(this,
                    "Only NGOs can send announcements!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String text = etAnnouncement.getText()
                .toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> ann = new HashMap<>();
        ann.put("title",      "📢 Announcement");
        ann.put("body",       text);
        ann.put("senderName", currentUserName);
        ann.put("senderId",   currentUserId);
        ann.put("timestamp",
                System.currentTimeMillis());

        rtdb.getReference("announcements")
                .push().setValue(ann)
                .addOnSuccessListener(v -> {
                    etAnnouncement.setText("");
                    Toast.makeText(this,
                            "Announcement sent! ✅",
                            Toast.LENGTH_SHORT).show();

                    // Notify all volunteers
                    NotificationHelper.notifyNewTask(this,
                            "📢 " + currentUserName
                                    + ": " + text);
                });
    }

    // ── TASK COMMENTS ─────────────────────────────────────

    void loadTasksForSpinner() {
        List<String> taskNames = new ArrayList<>();
        taskNames.add("General Discussion");

        for (LocalTaskStore.LocalTask t :
                LocalTaskStore.getInstance().getTasks()) {
            taskNames.add(t.title);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                taskNames);
        spinnerTasks.setAdapter(adapter);
    }

    void loadTaskComments(String taskId) {
        layoutComments.removeAllViews();

        rtdb.getReference("task_comments")
                .child(taskId)
                .orderByChild("timestamp")
                .limitToLast(100)
                .addChildEventListener(
                        new ChildEventListener() {
                            @Override
                            public void onChildAdded(
                                    DataSnapshot snap,
                                    String prev) {
                                String sender = snap.child("senderName")
                                        .getValue(String.class);
                                String comment = snap.child("comment")
                                        .getValue(String.class);
                                Long ts = snap.child("timestamp")
                                        .getValue(Long.class);
                                boolean isMe = currentUserId.equals(
                                        snap.child("senderId")
                                                .getValue(String.class));
                                addCommentBubble(
                                        comment, sender, ts, isMe);
                            }
                            @Override public void onChildChanged(
                                    DataSnapshot s, String p) {}
                            @Override public void onChildRemoved(
                                    DataSnapshot s) {}
                            @Override public void onChildMoved(
                                    DataSnapshot s, String p) {}
                            @Override public void onCancelled(
                                    DatabaseError e) {}
                        });
    }

    void sendTaskComment() {
        String comment = etComment.getText()
                .toString().trim();
        if (comment.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("senderId",   currentUserId);
        data.put("senderName", currentUserName);
        data.put("comment",    comment);
        data.put("timestamp",
                System.currentTimeMillis());

        rtdb.getReference("task_comments")
                .child(selectedTaskId).push()
                .setValue(data)
                .addOnSuccessListener(v ->
                        etComment.setText(""));
    }

    // ── UI BUILDERS ───────────────────────────────────────

    void addMessageBubble(String message, String sender,
                          Long timestamp, boolean isMe) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(
                isMe ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams wp =
                new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(0, 0, 0, 12);
        wrapper.setLayoutParams(wp);

        // Sender name
        if (!isMe) {
            TextView senderTv = new TextView(this);
            senderTv.setText(sender);
            senderTv.setTextSize(11f);
            senderTv.setTextColor(
                    Color.parseColor("#6B7280"));
            LinearLayout.LayoutParams sp =
                    new LinearLayout.LayoutParams(-2, -2);
            sp.setMargins(8, 0, 0, 4);
            senderTv.setLayoutParams(sp);
            wrapper.addView(senderTv);
        }

        // Bubble
        TextView bubble = new TextView(this);
        bubble.setText(message);
        bubble.setTextSize(14f);
        bubble.setTextColor(isMe
                ? Color.WHITE
                : Color.parseColor("#111827"));
        bubble.setBackgroundColor(isMe
                ? Color.parseColor("#1A1A2E")
                : Color.parseColor("#F3F4F6"));
        bubble.setPadding(28, 16, 28, 16);
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(-2, -2);
        bp.setMargins(
                isMe ? 80 : 0, 0,
                isMe ? 0 : 80, 0);
        bp.gravity = isMe ? Gravity.END : Gravity.START;
        bubble.setLayoutParams(bp);
        wrapper.addView(bubble);

        // Time
        if (timestamp != null) {
            TextView time = new TextView(this);
            time.setText(new SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault())
                    .format(new Date(timestamp)));
            time.setTextSize(10f);
            time.setTextColor(
                    Color.parseColor("#9CA3AF"));
            LinearLayout.LayoutParams tp =
                    new LinearLayout.LayoutParams(-2, -2);
            tp.gravity =
                    isMe ? Gravity.END : Gravity.START;
            tp.setMargins(8, 4, 8, 0);
            time.setLayoutParams(tp);
            wrapper.addView(time);
        }

        layoutMessages.addView(wrapper);

        // Auto scroll to bottom
        scrollChat.post(() ->
                scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    void addAnnouncementCard(String title, String body,
                             String sender, Long timestamp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(28, 20, 28, 20);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cp);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hp =
                new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 0, 0, 8);
        header.setLayoutParams(hp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title != null ? title : "📢");
        titleTv.setTextSize(14f);
        titleTv.setTextColor(
                Color.parseColor("#111827"));
        titleTv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        titleTv.setLayoutParams(tp);
        header.addView(titleTv);

        if (timestamp != null) {
            TextView time = new TextView(this);
            time.setText(new SimpleDateFormat(
                    "dd MMM, hh:mm a",
                    Locale.getDefault())
                    .format(new Date(timestamp)));
            time.setTextSize(10f);
            time.setTextColor(
                    Color.parseColor("#9CA3AF"));
            header.addView(time);
        }
        card.addView(header);

        TextView bodyTv = new TextView(this);
        bodyTv.setText(body);
        bodyTv.setTextSize(13f);
        bodyTv.setTextColor(
                Color.parseColor("#374151"));
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 0, 0, 8);
        bodyTv.setLayoutParams(bp);
        card.addView(bodyTv);

        TextView senderTv = new TextView(this);
        senderTv.setText("— " + sender);
        senderTv.setTextSize(11f);
        senderTv.setTextColor(
                Color.parseColor("#6B7280"));
        card.addView(senderTv);

        layoutAnnouncements.addView(card);
        scrollAnnouncements.post(() ->
                scrollAnnouncements.fullScroll(
                        View.FOCUS_DOWN));
    }

    void addCommentBubble(String comment, String sender,
                          Long timestamp, boolean isMe) {
        addMessageBubble(comment, sender, timestamp, isMe);
        scrollComments.post(() ->
                scrollComments.fullScroll(View.FOCUS_DOWN));
    }
}