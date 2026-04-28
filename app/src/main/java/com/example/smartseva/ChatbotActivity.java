package com.example.smartseva;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    // UI
    ScrollView scrollChat;
    LinearLayout layoutMessages;
    EditText etMessage;
    Button btnSend;
    TextView tvTyping;
    LinearLayout layoutQuickReplies;

    // Language toggle
    Button btnLangHindi, btnLangEnglish;
    String currentLang = "English"; // default

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    SharedPreferences prefs;

    // Volunteer data
    String volName     = "Volunteer";
    String volCity     = "Raipur";
    String volSkills   = "";
    String volAvail    = "";
    String volRole     = "Volunteer";

    // Chat history for context
    List<Map<String, String>> chatHistory = new ArrayList<>();

    // Gemini
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    // Quick reply suggestions
    String[] quickRepliesEnglish = {
            "Which task should I take?",
            "Show urgent tasks near me",
            "How to apply for a task?",
            "What skills do I need?",
            "How many tasks have I done?"
    };

    String[] quickRepliesHindi = {
            "Mujhe kaun sa task lena chahiye?",
            "Mere paas urgent tasks dikhao",
            "Task ke liye apply kaise karein?",
            "Mujhe kaun si skills chahiye?",
            "Maine kitne tasks kiye hain?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        mAuth  = FirebaseAuth.getInstance();
        db     = FirebaseFirestore.getInstance();
        prefs  = getSharedPreferences("SmartSeva", MODE_PRIVATE);

        // Load volunteer data
        volRole  = prefs.getString("role", "Volunteer");
        volName  = prefs.getString("volunteerName",
                prefs.getString("orgName", "User"));
        volCity  = prefs.getString("city",
                prefs.getString("orgCity", "Raipur"));

        bindViews();
        setListeners();
        loadVolunteerData();
    }

    void bindViews() {
        scrollChat        = findViewById(R.id.scrollChatbot);
        layoutMessages    = findViewById(R.id.layoutChatMessages);
        etMessage         = findViewById(R.id.etChatbotMessage);
        btnSend           = findViewById(R.id.btnChatbotSend);
        tvTyping          = findViewById(R.id.tvTypingIndicator);
        layoutQuickReplies= findViewById(R.id.layoutQuickReplies);
        btnLangHindi      = findViewById(R.id.btnLangHindi);
        btnLangEnglish    = findViewById(R.id.btnLangEnglish);
    }

    void setListeners() {
        findViewById(R.id.btnBackChatbot).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                etMessage.setText("");
            }
        });

        // Language toggle
        btnLangEnglish.setOnClickListener(v -> {
            currentLang = "English";
            setLangActive("English");
            showQuickReplies();
        });

        btnLangHindi.setOnClickListener(v -> {
            currentLang = "Hindi";
            setLangActive("Hindi");
            showQuickReplies();
        });

        // Enter key send
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                etMessage.setText("");
                return true;
            }
            return false;
        });
    }

    void setLangActive(String lang) {
        btnLangEnglish.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(lang.equals("English") ? "#1A1A2E" : "#F3F4F6")));
        btnLangEnglish.setTextColor(Color.parseColor(
                lang.equals("English") ? "#FFFFFF" : "#6B7280"));

        btnLangHindi.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(lang.equals("Hindi") ? "#1A1A2E" : "#F3F4F6")));
        btnLangHindi.setTextColor(Color.parseColor(
                lang.equals("Hindi") ? "#FFFFFF" : "#6B7280"));
    }

    // ══════════════════════════════════════════════════════
    // LOAD VOLUNTEER DATA FROM FIRESTORE
    // ══════════════════════════════════════════════════════

    void loadVolunteerData() {
        if (mAuth.getCurrentUser() == null) {
            showWelcome();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("volunteer_users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        volName  = doc.getString("name") != null
                                ? doc.getString("name") : volName;
                        volCity  = doc.getString("city") != null
                                ? doc.getString("city") : volCity;
                        volAvail = doc.getString("availableDays") != null
                                ? doc.getString("availableDays") : "";

                        // Build skills
                        StringBuilder skills = new StringBuilder();
                        String[] skillKeys   = {"teaching","medical","food",
                                "event","fundraising","technical","socialMedia"};
                        String[] skillLabels = {"Teaching","Medical Help",
                                "Food Distribution","Event Management",
                                "Fundraising","Technical","Social Media"};
                        for (int i = 0; i < skillKeys.length; i++) {
                            if (Boolean.TRUE.equals(doc.getBoolean(skillKeys[i]))) {
                                if (skills.length() > 0) skills.append(", ");
                                skills.append(skillLabels[i]);
                            }
                        }
                        volSkills = skills.toString();
                    }
                    showWelcome();
                    showQuickReplies();
                })
                .addOnFailureListener(e -> {
                    showWelcome();
                    showQuickReplies();
                });
    }

    // ══════════════════════════════════════════════════════
    // WELCOME MESSAGE
    // ══════════════════════════════════════════════════════

    void showWelcome() {
        String welcome = currentLang.equals("Hindi")
                ? "Namaste " + volName + "! 🙏\n\nMain aapka SmartSeva AI assistant hoon. " +
                "Main aapko sahi task dhundne mein, apply karne mein, aur " +
                "volunteer journey mein help kar sakta hoon!\n\nKya poochna chahte hain?"
                : "Hello " + volName + "! 👋\n\nI'm your SmartSeva AI assistant. " +
                "I can help you find the right tasks, apply for opportunities, " +
                "and guide your volunteer journey!\n\nWhat would you like to know?";

        addBotMessage(welcome);
    }

    // ══════════════════════════════════════════════════════
    // QUICK REPLIES
    // ══════════════════════════════════════════════════════

    void showQuickReplies() {
        layoutQuickReplies.removeAllViews();
        String[] replies = currentLang.equals("Hindi")
                ? quickRepliesHindi : quickRepliesEnglish;

        for (String reply : replies) {
            Button chip = new Button(this);
            chip.setText(reply);
            chip.setTextSize(11f);
            chip.setTextColor(Color.parseColor("#1A1A2E"));
            chip.setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor("#E8EAF6")));
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(0, 0, 8, 8);
            chip.setLayoutParams(lp);
            chip.setPadding(24, 8, 24, 8);

            chip.setOnClickListener(v -> {
                sendMessage(reply);
                layoutQuickReplies.removeAllViews();
            });

            layoutQuickReplies.addView(chip);
        }
    }

    // ══════════════════════════════════════════════════════
    // SEND MESSAGE
    // ══════════════════════════════════════════════════════

    void sendMessage(String userMsg) {
        // Show user bubble
        addUserMessage(userMsg);

        // Hide quick replies
        layoutQuickReplies.removeAllViews();

        // Show typing
        tvTyping.setVisibility(View.VISIBLE);
        scrollToBottom();

        // Add to history
        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("text", userMsg);
        chatHistory.add(userTurn);

        // Call Gemini
        executor.execute(() -> {
            try {
                String response = callGemini(userMsg);

                // Add to history
                Map<String, String> botTurn = new HashMap<>();
                botTurn.put("role", "model");
                botTurn.put("text", response);
                chatHistory.add(botTurn);

                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    addBotMessage(response);
                    // Show quick replies again after response
                    showQuickReplies();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvTyping.setVisibility(View.GONE);
                    addBotMessage(currentLang.equals("Hindi")
                            ? "Maafi chahta hoon, abhi kuch problem aa gayi. Thodi der baad try karein."
                            : "Sorry, I encountered an issue. Please try again in a moment.");
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════
    // GEMINI API CALL
    // ══════════════════════════════════════════════════════

    String callGemini(String userMsg) throws Exception {
        // Build system context
        String systemPrompt =
                "You are a helpful AI assistant for SmartSeva, an Indian NGO volunteer platform.\n\n" +
                        "Volunteer Profile:\n" +
                        "- Name: " + volName + "\n" +
                        "- City: " + volCity + "\n" +
                        "- Skills: " + (volSkills.isEmpty() ? "Not specified" : volSkills) + "\n" +
                        "- Availability: " + (volAvail.isEmpty() ? "Not specified" : volAvail) + "\n" +
                        "- Role: " + volRole + "\n\n" +
                        "Available Tasks (from local store):\n" +
                        getTasksSummary() + "\n\n" +
                        "Response language: " + currentLang + "\n" +
                        (currentLang.equals("Hindi")
                                ? "IMPORTANT: Respond in simple Hindi (Hinglish is OK). Keep it friendly and practical.\n"
                                : "IMPORTANT: Respond in clear English. Keep it friendly and practical.\n") +
                        "Keep responses concise (max 4-5 lines). " +
                        "Focus on volunteer task guidance, skill matching, and motivation. " +
                        "If asked about specific tasks, suggest based on volunteer's skills and location.";

        // Build contents array with history
        JSONArray contents = new JSONArray();

        // System context as first user message
        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "user");
        JSONArray sysParts = new JSONArray();
        JSONObject sysPart = new JSONObject();
        sysPart.put("text", systemPrompt);
        sysParts.put(sysPart);
        sysMsg.put("parts", sysParts);
        contents.put(sysMsg);

        // Dummy model ack
        JSONObject ackMsg = new JSONObject();
        ackMsg.put("role", "model");
        JSONArray ackParts = new JSONArray();
        JSONObject ackPart = new JSONObject();
        ackPart.put("text", "Understood! I'll help " + volName + " as their SmartSeva assistant.");
        ackParts.put(ackPart);
        ackMsg.put("parts", ackParts);
        contents.put(ackMsg);

        // Add chat history (last 6 turns max)
        int start = Math.max(0, chatHistory.size() - 6);
        for (int i = start; i < chatHistory.size(); i++) {
            Map<String, String> turn = chatHistory.get(i);
            JSONObject msg = new JSONObject();
            msg.put("role", turn.get("role"));
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", turn.get("text"));
            parts.put(part);
            msg.put("parts", parts);
            contents.put(msg);
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        // HTTP call
        URL url = new URL(GEMINI_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        java.util.Scanner scanner;
        if (conn.getResponseCode() == 200) {
            scanner = new java.util.Scanner(conn.getInputStream());
        } else {
            scanner = new java.util.Scanner(conn.getErrorStream());
        }

        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) sb.append(scanner.nextLine());
        scanner.close();

        JSONObject response = new JSONObject(sb.toString());
        if (!response.has("candidates")) {
            String err = response.has("error")
                    ? response.getJSONObject("error").optString("message")
                    : "API Error";
            throw new Exception(err);
        }

        return response.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();
    }

    // ══════════════════════════════════════════════════════
    // TASK SUMMARY FOR CONTEXT
    // ══════════════════════════════════════════════════════

    String getTasksSummary() {
        List<LocalTaskStore.LocalTask> tasks =
                LocalTaskStore.getInstance().getTasks();
        if (tasks.isEmpty()) return "No tasks currently available.";

        StringBuilder sb = new StringBuilder();
        for (LocalTaskStore.LocalTask t : tasks) {
            sb.append("- ").append(t.title)
                    .append(" (").append(t.category).append(", ")
                    .append(t.urgency).append(", ")
                    .append(t.location).append(")\n");
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════
    // UI — MESSAGE BUBBLES
    // ══════════════════════════════════════════════════════

    void addUserMessage(String text) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.END);
        LinearLayout.LayoutParams wp =
                new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(80, 0, 0, 16);
        wrapper.setLayoutParams(wp);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(14f);
        bubble.setTextColor(Color.WHITE);
        bubble.setBackgroundColor(Color.parseColor("#1A1A2E"));
        bubble.setPadding(28, 16, 28, 16);
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(-2, -2);
        bp.gravity = Gravity.END;
        bubble.setLayoutParams(bp);
        wrapper.addView(bubble);

        layoutMessages.addView(wrapper);
        scrollToBottom();
    }

    void addBotMessage(String text) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.START);
        LinearLayout.LayoutParams wp =
                new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(0, 0, 80, 16);
        wrapper.setLayoutParams(wp);

        // Bot avatar
        TextView avatar = new TextView(this);
        avatar.setText("🤖");
        avatar.setTextSize(20f);
        LinearLayout.LayoutParams ap =
                new LinearLayout.LayoutParams(-2, -2);
        ap.setMargins(0, 0, 10, 0);
        ap.gravity = Gravity.BOTTOM;
        avatar.setLayoutParams(ap);
        wrapper.addView(avatar);

        // Bubble
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(14f);
        bubble.setTextColor(Color.parseColor("#111827"));
        bubble.setBackgroundColor(Color.parseColor("#F3F4F6"));
        bubble.setPadding(28, 16, 28, 16);
        bubble.setLineSpacing(4f, 1f);
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(-2, -2);
        bp.gravity = Gravity.START;
        bubble.setLayoutParams(bp);
        wrapper.addView(bubble);

        layoutMessages.addView(wrapper);
        scrollToBottom();
    }

    void scrollToBottom() {
        scrollChat.post(() ->
                scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}