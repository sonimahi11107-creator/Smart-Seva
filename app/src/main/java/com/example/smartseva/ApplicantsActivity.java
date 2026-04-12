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
import java.util.*;

public class ApplicantsActivity extends AppCompatActivity {

    // Views
    TextView tvApplicantsTaskTitle, tvApplicantsCount;
    TextView tvStatTotal, tvStatPending, tvStatAccepted, tvStatRejected;
    Button btnBackApplicants, btnTabAll, btnTabPending, btnTabAccepted, btnTabRejected;
    EditText etSearchApplicant;
    ListView listApplicants;
    LinearLayout layoutEmptyApplicants;

    // Data
    String taskTitle = "Task";
    String currentFilter = "all";
    List<Applicant> allApplicants = new ArrayList<>();
    List<Applicant> filteredApplicants = new ArrayList<>();
    ApplicantListAdapter adapter;

    // ── Applicant Model ──
    static class Applicant {
        String name, city, skills, appliedTime, status, availability;
        int experience;

        Applicant(String name, String city, String skills,
                  String appliedTime, String status,
                  String availability, int experience) {
            this.name        = name;
            this.city        = city;
            this.skills      = skills;
            this.appliedTime = appliedTime;
            this.status      = status;
            this.availability= availability;
            this.experience  = experience;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        // ── Views ──
        tvApplicantsTaskTitle  = findViewById(R.id.tvApplicantsTaskTitle);
        tvApplicantsCount      = findViewById(R.id.tvApplicantsCount);
        tvStatTotal            = findViewById(R.id.tvStatTotal);
        tvStatPending          = findViewById(R.id.tvStatPending);
        tvStatAccepted         = findViewById(R.id.tvStatAccepted);
        tvStatRejected         = findViewById(R.id.tvStatRejected);
        btnBackApplicants      = findViewById(R.id.btnBackApplicants);
        btnTabAll              = findViewById(R.id.btnTabAll);
        btnTabPending          = findViewById(R.id.btnTabPending);
        btnTabAccepted         = findViewById(R.id.btnTabAccepted);
        btnTabRejected         = findViewById(R.id.btnTabRejected);
        etSearchApplicant      = findViewById(R.id.etSearchApplicant);
        listApplicants         = findViewById(R.id.listApplicants);
        layoutEmptyApplicants  = findViewById(R.id.layoutEmptyApplicants);

        // ── Intent Data ──
        taskTitle = getIntent().getStringExtra("taskTitle");
        if (taskTitle == null) taskTitle = "Task";
        tvApplicantsTaskTitle.setText(taskTitle);

        // ── Sample Data ──
        // Firebase teammate yahan Firestore se real applicants load karega
        loadSampleApplicants();

        // ── Adapter ──
        adapter = new ApplicantListAdapter();
        listApplicants.setAdapter(adapter);

        // ── Update Stats ──
        updateStats();
        filterApplicants("all");

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

        // ── Search ──
        etSearchApplicant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchApplicants(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ═══════════════════════════════════════
    // SAMPLE DATA
    // ═══════════════════════════════════════

    void loadSampleApplicants() {
        allApplicants.clear();
        allApplicants.add(new Applicant(
                "Priya Sharma", "Raipur, CG",
                "Teaching, Medical Help",
                "2 hours ago", "Pending", "Weekends", 2));
        allApplicants.add(new Applicant(
                "Rahul Verma", "Bilaspur, CG",
                "Food Distribution, Event Management",
                "5 hours ago", "Pending", "Both", 1));
        allApplicants.add(new Applicant(
                "Anjali Patel", "Durg, CG",
                "Social Media, Fundraising",
                "1 day ago", "Accepted", "Weekdays", 3));
        allApplicants.add(new Applicant(
                "Sonu Kumar", "Raipur, CG",
                "Technical, Event Management",
                "2 days ago", "Rejected", "Weekends", 0));
        allApplicants.add(new Applicant(
                "Deepika Singh", "Raipur, CG",
                "Teaching, Social Media",
                "3 days ago", "Pending", "Both", 1));
    }

    // ═══════════════════════════════════════
    // FILTER + SEARCH
    // ═══════════════════════════════════════

    void filterApplicants(String filter) {
        currentFilter = filter;
        filteredApplicants.clear();
        for (Applicant a : allApplicants) {
            if (filter.equals("all") || a.status.equals(filter)) {
                filteredApplicants.add(a);
            }
        }
        updateEmptyState();
        adapter.notifyDataSetChanged();
        tvApplicantsCount.setText(filteredApplicants.size() + " applicants");
    }

    void searchApplicants(String query) {
        filteredApplicants.clear();
        for (Applicant a : allApplicants) {
            boolean matchFilter = currentFilter.equals("all") || a.status.equals(currentFilter);
            boolean matchQuery  = query.isEmpty() ||
                    a.name.toLowerCase().contains(query.toLowerCase()) ||
                    a.skills.toLowerCase().contains(query.toLowerCase()) ||
                    a.city.toLowerCase().contains(query.toLowerCase());
            if (matchFilter && matchQuery) filteredApplicants.add(a);
        }
        updateEmptyState();
        adapter.notifyDataSetChanged();
        tvApplicantsCount.setText(filteredApplicants.size() + " applicants");
    }

    void updateEmptyState() {
        if (filteredApplicants.isEmpty()) {
            layoutEmptyApplicants.setVisibility(View.VISIBLE);
            listApplicants.setVisibility(View.GONE);
        } else {
            layoutEmptyApplicants.setVisibility(View.GONE);
            listApplicants.setVisibility(View.VISIBLE);
        }
    }

    void updateStats() {
        int total = allApplicants.size();
        int pending = 0, accepted = 0, rejected = 0;
        for (Applicant a : allApplicants) {
            if (a.status.equals("Pending"))  pending++;
            if (a.status.equals("Accepted")) accepted++;
            if (a.status.equals("Rejected")) rejected++;
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
        active.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }

    // ═══════════════════════════════════════
    // ACCEPT / REJECT
    // ═══════════════════════════════════════

    void acceptApplicant(int position) {
        Applicant a = filteredApplicants.get(position);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Accept Volunteer")
                .setMessage("Accept " + a.name + " for this task?\n\nThey will be notified via SMS.")
                .setPositiveButton("Accept", (dialog, which) -> {
                    // Firebase teammate yahan status update + SMS bhejega
                    a.status = "Accepted";
                    updateStats();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this,
                            a.name + " accepted! ✅", Toast.LENGTH_SHORT).show();

                    // SMS to volunteer
                    try {
                        android.telephony.SmsManager sms =
                                android.telephony.SmsManager.getDefault();
                        sms.sendTextMessage("+91XXXXXXXXXX", null,
                                "Congratulations! Your application for '" + taskTitle +
                                        "' has been accepted by the NGO. Please be ready!",
                                null, null);
                    } catch (Exception e) { e.printStackTrace(); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void rejectApplicant(int position) {
        Applicant a = filteredApplicants.get(position);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Reject Application")
                .setMessage("Reject " + a.name + "'s application?")
                .setPositiveButton("Reject", (dialog, which) -> {
                    // Firebase teammate yahan status update karega
                    a.status = "Rejected";
                    updateStats();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this,
                            a.name + " rejected.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void viewVolunteerProfile(int position) {
        Applicant a = filteredApplicants.get(position);
        Intent intent = new Intent(this, VolunteerProfileActivity.class);
        intent.putExtra("name",         a.name);
        intent.putExtra("city",         a.city);
        intent.putExtra("skills",       a.skills);
        intent.putExtra("availability", a.availability);
        intent.putExtra("availTime",    "Part-time");
        intent.putExtra("languages",    "Hindi, English");
        intent.putExtra("vehicle",      "Yes");
        intent.putExtra("travel",       "Yes");
        intent.putExtra("causes",       "Education, Health");
        intent.putExtra("idType",       "Aadhaar Card");
        intent.putExtra("status",       a.status);
        intent.putExtra("experience",   a.experience);
        intent.putExtra("email",        "volunteer@email.com");
        intent.putExtra("phone",        "9876543210");
        startActivity(intent);
    }
    // ═══════════════════════════════════════
    // ADAPTER
    // ═══════════════════════════════════════

    class ApplicantListAdapter extends BaseAdapter {

        @Override public int getCount() { return filteredApplicants.size(); }
        @Override public Object getItem(int pos) { return filteredApplicants.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        R.layout.item_applicant, parent, false);
            }

            Applicant a = filteredApplicants.get(position);

            TextView tvName    = convertView.findViewById(R.id.tvApplicantName);
            TextView tvCity    = convertView.findViewById(R.id.tvApplicantCity);
            TextView tvSkills  = convertView.findViewById(R.id.tvApplicantSkills);
            TextView tvTime    = convertView.findViewById(R.id.tvApplicantTime);
            TextView tvStatus  = convertView.findViewById(R.id.tvApplicantStatus);
            Button btnView     = convertView.findViewById(R.id.btnViewProfile);
            Button btnAccept   = convertView.findViewById(R.id.btnAccept);
            Button btnReject   = convertView.findViewById(R.id.btnReject);

            tvName.setText(a.name);
            tvCity.setText("📍 " + a.city);
            tvSkills.setText("🛠️ " + a.skills);
            tvTime.setText("🕒 Applied: " + a.appliedTime);
            tvStatus.setText(a.status);

            // Status color
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