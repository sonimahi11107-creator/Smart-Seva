package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;

public class ApplicantsActivity extends AppCompatActivity {

    // Views
    TextView tvApplicantsTaskTitle, tvApplicantsCount;
    TextView tvStatTotal, tvStatPending, tvStatAccepted, tvStatRejected;
    Button btnBackApplicants, btnTabAll, btnTabPending, btnTabAccepted, btnTabRejected;
    EditText etSearchApplicant;
    ListView listApplicants;
    LinearLayout layoutEmptyApplicants;

    // Firebase
    FirebaseFirestore db;

    // Data
    String taskId, taskTitle;
    String currentFilter = "all";
    List<Applicant> allApplicants      = new ArrayList<>();
    List<Applicant> filteredApplicants = new ArrayList<>();
    ApplicantListAdapter adapter;

    // ── Applicant Model ──
    static class Applicant {
        String applicationId; // Firestore document ID
        String volunteerId;
        String name, city, skills, phone, email;
        String appliedTime, status, availability, availTime;
        String languages, vehicle, travel, causes, idType;
        int experience;

        Applicant(String applicationId, String volunteerId,
                  String name, String city, String skills,
                  String phone, String email, String appliedTime,
                  String status, String availability) {
            this.applicationId = applicationId;
            this.volunteerId   = volunteerId;
            this.name          = name;
            this.city          = city;
            this.skills        = skills;
            this.phone         = phone;
            this.email         = email;
            this.appliedTime   = appliedTime;
            this.status        = status;
            this.availability  = availability;
            // defaults
            this.availTime  = "Part-time";
            this.languages  = "Hindi, English";
            this.vehicle    = "Not specified";
            this.travel     = "Not specified";
            this.causes     = "Not specified";
            this.idType     = "Aadhaar Card";
            this.experience = 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        db = FirebaseFirestore.getInstance();

        // ── Views ──
        tvApplicantsTaskTitle = findViewById(R.id.tvApplicantsTaskTitle);
        tvApplicantsCount     = findViewById(R.id.tvApplicantsCount);
        tvStatTotal           = findViewById(R.id.tvStatTotal);
        tvStatPending         = findViewById(R.id.tvStatPending);
        tvStatAccepted        = findViewById(R.id.tvStatAccepted);
        tvStatRejected        = findViewById(R.id.tvStatRejected);
        btnBackApplicants     = findViewById(R.id.btnBackApplicants);
        btnTabAll             = findViewById(R.id.btnTabAll);
        btnTabPending         = findViewById(R.id.btnTabPending);
        btnTabAccepted        = findViewById(R.id.btnTabAccepted);
        btnTabRejected        = findViewById(R.id.btnTabRejected);
        etSearchApplicant     = findViewById(R.id.etSearchApplicant);
        listApplicants        = findViewById(R.id.listApplicants);
        layoutEmptyApplicants = findViewById(R.id.layoutEmptyApplicants);

        // ── Intent Data ──
        taskId    = getIntent().getStringExtra("taskId");
        taskTitle = getIntent().getStringExtra("taskTitle");
        if (taskTitle == null) taskTitle = "Task";
        tvApplicantsTaskTitle.setText(taskTitle);

        // ── Adapter ──
        adapter = new ApplicantListAdapter();
        listApplicants.setAdapter(adapter);

        // ✅ Load from Firestore
        loadApplicantsFromFirestore();

        // ── Listeners ──
        btnBackApplicants.setOnClickListener(v -> finish());

        btnTabAll.setOnClickListener(v -> {
            filterApplicants("all");
            setTabActive(btnTabAll);
        });
        btnTabPending.setOnClickListener(v -> {
            filterApplicants("Pending");
            setTabActive(btnTabPending);
        });
        btnTabAccepted.setOnClickListener(v -> {
            filterApplicants("Accepted");
            setTabActive(btnTabAccepted);
        });
        btnTabRejected.setOnClickListener(v -> {
            filterApplicants("Rejected");
            setTabActive(btnTabRejected);
        });

        etSearchApplicant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchApplicants(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ═══════════════════════════════════════
    // FIREBASE — Load applicants
    // ═══════════════════════════════════════

    void loadApplicantsFromFirestore() {
        if (taskId == null) {
            Toast.makeText(this, "Task ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("applications")
                .whereEqualTo("taskId", taskId)
                .get()
                .addOnSuccessListener(snap -> {
                    allApplicants.clear();

                    if (snap.isEmpty()) {
                        updateStats();
                        filterApplicants("all");
                        return;
                    }

                    // For each application, fetch volunteer profile
                    final int[] remaining = {snap.size()};

                    for (QueryDocumentSnapshot appDoc : snap) {
                        String appId       = appDoc.getId();
                        String volunteerId = appDoc.getString("volunteerId");
                        String status      = appDoc.getString("status");

                        // Get timestamp
                        String appliedTime = "Recently";
                        com.google.firebase.Timestamp ts = appDoc.getTimestamp("appliedAt");
                        if (ts != null) {
                            long diffMs  = System.currentTimeMillis()
                                    - ts.toDate().getTime();
                            long diffHrs = diffMs / (1000 * 60 * 60);
                            if (diffHrs < 1)       appliedTime = "Just now";
                            else if (diffHrs < 24) appliedTime = diffHrs + " hours ago";
                            else                   appliedTime = (diffHrs / 24) + " days ago";
                        }

                        // Fetch volunteer details
                        final String finalAppId      = appId;
                        final String finalStatus     = status != null ? status : "Pending";
                        final String finalAppliedTime= appliedTime;

                        db.collection("volunteer_users").document(volunteerId).get()
                                .addOnSuccessListener(volDoc -> {
                                    String name  = volDoc.getString("name");
                                    String city  = volDoc.getString("city");
                                    String phone = volDoc.getString("phone");
                                    String email = volDoc.getString("email");
                                    String avail = volDoc.getString("availableDays");

                                    // Build skills
                                    StringBuilder skills = new StringBuilder();
                                    String[] skillKeys   = {"teaching","medical","food",
                                            "event","fundraising","technical","socialMedia"};
                                    String[] skillLabels = {"Teaching","Medical Help",
                                            "Food Distribution","Event Management",
                                            "Fundraising","Technical","Social Media"};
                                    for (int i = 0; i < skillKeys.length; i++) {
                                        Boolean val = volDoc.getBoolean(skillKeys[i]);
                                        if (Boolean.TRUE.equals(val)) {
                                            if (skills.length() > 0) skills.append(", ");
                                            skills.append(skillLabels[i]);
                                        }
                                    }

                                    Applicant applicant = new Applicant(
                                            finalAppId,
                                            volunteerId,
                                            name  != null ? name  : "Unknown",
                                            city  != null ? city  : "Unknown",
                                            skills.length() > 0 ? skills.toString() : "Not specified",
                                            phone != null ? phone : "Not provided",
                                            email != null ? email : "Not provided",
                                            finalAppliedTime,
                                            finalStatus,
                                            avail != null ? avail : "Not specified"
                                    );

                                    // Extra fields
                                    applicant.languages = volDoc.getString("languages") != null
                                            ? volDoc.getString("languages") : "Hindi, English";
                                    applicant.vehicle   = volDoc.getString("vehicle") != null
                                            ? volDoc.getString("vehicle") : "Not specified";
                                    applicant.travel    = volDoc.getString("travel") != null
                                            ? volDoc.getString("travel") : "Not specified";
                                    applicant.idType    = volDoc.getString("idType") != null
                                            ? volDoc.getString("idType") : "Aadhaar Card";
                                    applicant.availTime = volDoc.getString("availableTime") != null
                                            ? volDoc.getString("availableTime") : "Part-time";

                                    String expStr = volDoc.getString("experience");
                                    if (expStr != null && expStr.contains("1+"))        applicant.experience = 2;
                                    else if (expStr != null && expStr.contains("less")) applicant.experience = 1;

                                    // Build causes
                                    StringBuilder causes = new StringBuilder();
                                    String[] causeKeys   = {"education","environment","animal",
                                            "women","health","disaster"};
                                    String[] causeLabels = {"Education","Environment","Animal Welfare",
                                            "Women Empowerment","Health","Disaster Relief"};
                                    for (int i = 0; i < causeKeys.length; i++) {
                                        Boolean val = volDoc.getBoolean(causeKeys[i]);
                                        if (Boolean.TRUE.equals(val)) {
                                            if (causes.length() > 0) causes.append(", ");
                                            causes.append(causeLabels[i]);
                                        }
                                    }
                                    applicant.causes = causes.length() > 0
                                            ? causes.toString() : "Not specified";

                                    allApplicants.add(applicant);

                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        // All volunteers loaded
                                        runOnUiThread(() -> {
                                            updateStats();
                                            filterApplicants("all");
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        runOnUiThread(() -> {
                                            updateStats();
                                            filterApplicants("all");
                                        });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading applicants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ═══════════════════════════════════════
    // FIREBASE — Accept applicant
    // ═══════════════════════════════════════

    void acceptApplicant(int position) {
        Applicant a = filteredApplicants.get(position);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Accept Volunteer")
                .setMessage("Accept " + a.name + " for this task?\n\nThey will be notified.")
                .setPositiveButton("Accept", (dialog, which) -> {
                    // ✅ Update Firestore
                    db.collection("applications").document(a.applicationId)
                            .update("status", "Accepted")
                            .addOnSuccessListener(unused -> {
                                a.status = "Accepted";
                                updateStats();
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this,
                                        a.name + " accepted! ✅",
                                        Toast.LENGTH_SHORT).show();

                                // SMS notification
                                sendSMS(a.phone, "Congratulations " + a.name
                                        + "! Your application for '" + taskTitle
                                        + "' has been ACCEPTED. Please be ready!");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // FIREBASE — Reject applicant
    // ═══════════════════════════════════════

    void rejectApplicant(int position) {
        Applicant a = filteredApplicants.get(position);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Reject Application")
                .setMessage("Reject " + a.name + "'s application?")
                .setPositiveButton("Reject", (dialog, which) -> {
                    // ✅ Update Firestore
                    db.collection("applications").document(a.applicationId)
                            .update("status", "Rejected")
                            .addOnSuccessListener(unused -> {
                                a.status = "Rejected";
                                updateStats();
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this,
                                        a.name + " rejected.",
                                        Toast.LENGTH_SHORT).show();

                                sendSMS(a.phone, "Hello " + a.name
                                        + "! Your application for '" + taskTitle
                                        + "' was not selected this time. Keep trying!");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // VIEW PROFILE
    // ═══════════════════════════════════════

    void viewVolunteerProfile(int position) {
        Applicant a = filteredApplicants.get(position);
        Intent intent = new Intent(this, VolunteerProfileActivity.class);
        intent.putExtra("volunteerId", a.volunteerId);  // ✅ real ID
        intent.putExtra("taskId",      taskId);         // ✅ for accept/reject
        intent.putExtra("name",        a.name);
        intent.putExtra("city",        a.city);
        intent.putExtra("skills",      a.skills);
        intent.putExtra("availability",a.availability);
        intent.putExtra("availTime",   a.availTime);
        intent.putExtra("languages",   a.languages);
        intent.putExtra("vehicle",     a.vehicle);
        intent.putExtra("travel",      a.travel);
        intent.putExtra("causes",      a.causes);
        intent.putExtra("idType",      a.idType);
        intent.putExtra("status",      a.status);
        intent.putExtra("experience",  a.experience);
        intent.putExtra("email",       a.email);
        intent.putExtra("phone",       a.phone);
        startActivity(intent);
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    void sendSMS(String phone, String message) {
        try {
            android.telephony.SmsManager sms =
                    android.telephony.SmsManager.getDefault();
            sms.sendTextMessage("+91" + phone, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void filterApplicants(String filter) {
        currentFilter = filter;
        filteredApplicants.clear();
        for (Applicant a : allApplicants) {
            if (filter.equals("all") || a.status.equals(filter))
                filteredApplicants.add(a);
        }
        updateEmptyState();
        adapter.notifyDataSetChanged();
        tvApplicantsCount.setText(filteredApplicants.size() + " applicants");
    }

    void searchApplicants(String query) {
        filteredApplicants.clear();
        for (Applicant a : allApplicants) {
            boolean matchFilter = currentFilter.equals("all")
                    || a.status.equals(currentFilter);
            boolean matchQuery  = query.isEmpty()
                    || a.name.toLowerCase().contains(query.toLowerCase())
                    || a.skills.toLowerCase().contains(query.toLowerCase())
                    || a.city.toLowerCase().contains(query.toLowerCase());
            if (matchFilter && matchQuery) filteredApplicants.add(a);
        }
        updateEmptyState();
        adapter.notifyDataSetChanged();
        tvApplicantsCount.setText(filteredApplicants.size() + " applicants");
    }

    void updateEmptyState() {
        boolean empty = filteredApplicants.isEmpty();
        layoutEmptyApplicants.setVisibility(empty ? View.VISIBLE : View.GONE);
        listApplicants.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    void updateStats() {
        int total = allApplicants.size(), pending = 0, accepted = 0, rejected = 0;
        for (Applicant a : allApplicants) {
            if ("Pending".equals(a.status))  pending++;
            if ("Accepted".equals(a.status)) accepted++;
            if ("Rejected".equals(a.status)) rejected++;
        }
        tvStatTotal.setText(String.valueOf(total));
        tvStatPending.setText(String.valueOf(pending));
        tvStatAccepted.setText(String.valueOf(accepted));
        tvStatRejected.setText(String.valueOf(rejected));
    }

    void setTabActive(Button active) {
        Button[] tabs = {btnTabAll, btnTabPending, btnTabAccepted, btnTabRejected};
        for (Button b : tabs) {
            b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            b.setTextColor(Color.parseColor("#888888"));
        }
        active.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }

    // ═══════════════════════════════════════
    // ADAPTER
    // ═══════════════════════════════════════

    class ApplicantListAdapter extends BaseAdapter {

        @Override public int getCount()          { return filteredApplicants.size(); }
        @Override public Object getItem(int pos) { return filteredApplicants.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_applicant, parent, false);

            Applicant a = filteredApplicants.get(position);

            TextView tvName   = convertView.findViewById(R.id.tvApplicantName);
            TextView tvCity   = convertView.findViewById(R.id.tvApplicantCity);
            TextView tvSkills = convertView.findViewById(R.id.tvApplicantSkills);
            TextView tvTime   = convertView.findViewById(R.id.tvApplicantTime);
            TextView tvStatus = convertView.findViewById(R.id.tvApplicantStatus);
            Button btnView    = convertView.findViewById(R.id.btnViewProfile);
            Button btnAccept  = convertView.findViewById(R.id.btnAccept);
            Button btnReject  = convertView.findViewById(R.id.btnReject);

            tvName.setText(a.name);
            tvCity.setText("📍 " + a.city);
            tvSkills.setText("🛠️ " + a.skills);
            tvTime.setText("🕒 Applied: " + a.appliedTime);
            tvStatus.setText(a.status);

            switch (a.status) {
                case "Accepted":
                    tvStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                    btnAccept.setEnabled(false);
                    btnAccept.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    btnReject.setEnabled(false);
                    btnReject.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    break;
                case "Rejected":
                    tvStatus.setBackgroundColor(Color.parseColor("#C62828"));
                    btnAccept.setEnabled(false);
                    btnAccept.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    btnReject.setEnabled(false);
                    btnReject.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    break;
                default:
                    tvStatus.setBackgroundColor(Color.parseColor("#F57F17"));
                    btnAccept.setEnabled(true);
                    btnAccept.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#2E7D32")));
                    btnReject.setEnabled(true);
                    btnReject.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#C62828")));
            }

            btnView.setOnClickListener(v -> viewVolunteerProfile(position));
            btnAccept.setOnClickListener(v -> acceptApplicant(position));
            btnReject.setOnClickListener(v -> rejectApplicant(position));

            return convertView;
        }
    }
}