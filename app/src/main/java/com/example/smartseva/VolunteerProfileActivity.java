package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class VolunteerProfileActivity extends AppCompatActivity {

    // Views
    ImageView imgVolunteerPhoto;
    TextView tvVolName, tvVolCity, tvVolEmail, tvVolPhone;
    TextView tvStatTasksDone, tvStatImpact, tvStatRating;
    TextView tvVolAvailDays, tvVolAvailTime;
    TextView tvVolLanguages, tvVolVehicle, tvVolTravel, tvVolExperience, tvVolIDType;
    TextView tvProfileStatusBadge;
    LinearLayout layoutSkillChips, layoutCauseChips;
    Button btnBackProfile, btnCallVolunteer, btnSMSVolunteer;
    Button btnProfileAccept, btnProfileReject;

    // Data
    String volunteerId, taskId;
    String volName, volCity, volEmail, volPhone;
    String volSkills, volAvailDays, volAvailTime;
    String volLanguages, volVehicle, volTravel;
    String volCauses, volIDType, volStatus;
    int volExperience;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);

        db = FirebaseFirestore.getInstance();

        // ── Views ──
        imgVolunteerPhoto    = findViewById(R.id.imgVolunteerPhoto);
        tvVolName            = findViewById(R.id.tvVolName);
        tvVolCity            = findViewById(R.id.tvVolCity);
        tvVolEmail           = findViewById(R.id.tvVolEmail);
        tvVolPhone           = findViewById(R.id.tvVolPhone);
        tvStatTasksDone      = findViewById(R.id.tvStatTasksDone);
        tvStatImpact         = findViewById(R.id.tvStatImpact);
        tvStatRating         = findViewById(R.id.tvStatRating);
        tvVolAvailDays       = findViewById(R.id.tvVolAvailDays);
        tvVolAvailTime       = findViewById(R.id.tvVolAvailTime);
        tvVolLanguages       = findViewById(R.id.tvVolLanguages);
        tvVolVehicle         = findViewById(R.id.tvVolVehicle);
        tvVolTravel          = findViewById(R.id.tvVolTravel);
        tvVolExperience      = findViewById(R.id.tvVolExperience);
        tvVolIDType          = findViewById(R.id.tvVolIDType);
        tvProfileStatusBadge = findViewById(R.id.tvProfileStatusBadge);
        layoutSkillChips     = findViewById(R.id.layoutSkillChips);
        layoutCauseChips     = findViewById(R.id.layoutCauseChips);
        btnBackProfile       = findViewById(R.id.btnBackProfile);
        btnCallVolunteer     = findViewById(R.id.btnCallVolunteer);
        btnSMSVolunteer      = findViewById(R.id.btnSMSVolunteer);
        btnProfileAccept     = findViewById(R.id.btnProfileAccept);
        btnProfileReject     = findViewById(R.id.btnProfileReject);

        // ── Intent Data ──
        Intent intent = getIntent();
        volunteerId  = intent.getStringExtra("volunteerId");
        taskId       = intent.getStringExtra("taskId");

        // Intent data as fallback
        volName      = intent.getStringExtra("name");
        volCity      = intent.getStringExtra("city");
        volSkills    = intent.getStringExtra("skills");
        volAvailDays = intent.getStringExtra("availability");
        volExperience= intent.getIntExtra("experience", 0);
        volStatus    = intent.getStringExtra("status");
        if (volStatus == null) volStatus = "Pending";

        // ✅ Load full profile from Firestore if volunteerId available
        if (volunteerId != null && !volunteerId.isEmpty()) {
            loadVolunteerFromFirestore(volunteerId);
        } else {
            // fallback to intent data
            setDefaults();
            populateUI();
        }

        // ── Listeners ──
        btnBackProfile.setOnClickListener(v -> finish());

        btnCallVolunteer.setOnClickListener(v -> {
            if (volPhone != null && !volPhone.equals("Not provided")) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:+91" + volPhone)));
            } else {
                Toast.makeText(this, "Phone number not available",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnSMSVolunteer.setOnClickListener(v -> {
            if (volPhone != null && !volPhone.equals("Not provided")) {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("sms:+91" + volPhone));
                smsIntent.putExtra("sms_body",
                        "Hello " + volName
                                + "! Regarding your volunteer application on Smart Seva...");
                startActivity(smsIntent);
            } else {
                Toast.makeText(this, "Phone number not available",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnProfileAccept.setOnClickListener(v -> confirmAction("Accept"));
        btnProfileReject.setOnClickListener(v -> confirmAction("Reject"));
    }

    // ═══════════════════════════════════════
    // FIREBASE — Load volunteer profile
    // ═══════════════════════════════════════

    void loadVolunteerFromFirestore(String uid) {
        db.collection("volunteer_users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    volName      = doc.getString("name");
                    volCity      = doc.getString("city");
                    volEmail     = doc.getString("email");
                    volPhone     = doc.getString("phone");
                    volAvailDays = doc.getString("availableDays");
                    volAvailTime = doc.getString("availableTime");
                    volLanguages = doc.getString("languages");
                    volVehicle   = doc.getString("vehicle");
                    volTravel    = doc.getString("travel");
                    volIDType    = doc.getString("idType");

                    // Experience
                    String expStr = doc.getString("experience");
                    if (expStr != null && expStr.contains("1+"))        volExperience = 2;
                    else if (expStr != null && expStr.contains("less")) volExperience = 1;
                    else                                                 volExperience = 0;

                    // Build skills string
                    StringBuilder skills = new StringBuilder();
                    String[] skillKeys   = {"teaching","medical","food",
                            "event","fundraising","technical","socialMedia"};
                    String[] skillLabels = {"Teaching","Medical Help",
                            "Food Distribution","Event Management",
                            "Fundraising","Technical","Social Media"};
                    for (int i = 0; i < skillKeys.length; i++) {
                        Boolean val = doc.getBoolean(skillKeys[i]);
                        if (Boolean.TRUE.equals(val)) {
                            if (skills.length() > 0) skills.append(", ");
                            skills.append(skillLabels[i]);
                        }
                    }
                    volSkills = skills.length() > 0 ? skills.toString() : "Not specified";

                    // Build causes string
                    StringBuilder causes = new StringBuilder();
                    String[] causeKeys   = {"education","environment","animal",
                            "women","health","disaster"};
                    String[] causeLabels = {"Education","Environment","Animal Welfare",
                            "Women Empowerment","Health","Disaster Relief"};
                    for (int i = 0; i < causeKeys.length; i++) {
                        Boolean val = doc.getBoolean(causeKeys[i]);
                        if (Boolean.TRUE.equals(val)) {
                            if (causes.length() > 0) causes.append(", ");
                            causes.append(causeLabels[i]);
                        }
                    }
                    volCauses = causes.length() > 0 ? causes.toString() : "Not specified";

                    setDefaults();

                    // ✅ Load real stats
                    loadVolunteerStats(uid);

                    populateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    setDefaults();
                    populateUI();
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — Load real stats
    // ═══════════════════════════════════════

    void loadVolunteerStats(String uid) {
        db.collection("applications")
                .whereEqualTo("volunteerId", uid)
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(snap -> {
                    int tasksDone = snap.size();
                    int impact    = tasksDone * 50;
                    tvStatTasksDone.setText(String.valueOf(tasksDone));
                    tvStatImpact.setText(String.valueOf(impact));
                    tvStatRating.setText("⭐ " + (tasksDone > 0 ? "4.2" : "New"));
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — Accept / Reject
    // ═══════════════════════════════════════

    void confirmAction(String action) {
        String msg = action.equals("Accept") ?
                "Accept " + volName + " as a volunteer for this task?" :
                "Reject " + volName + "'s application?";

        new android.app.AlertDialog.Builder(this)
                .setTitle(action + " Volunteer")
                .setMessage(msg)
                .setPositiveButton(action, (dialog, which) -> {
                    String newStatus = action.equals("Accept") ? "Accepted" : "Rejected";

                    // ✅ Update application status in Firestore
                    if (taskId != null && volunteerId != null) {
                        db.collection("applications")
                                .whereEqualTo("taskId",      taskId)
                                .whereEqualTo("volunteerId", volunteerId)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    for (QueryDocumentSnapshot doc : snap) {
                                        doc.getReference().update("status", newStatus);
                                    }
                                });
                    }

                    // ✅ Update local UI
                    volStatus = newStatus;
                    tvProfileStatusBadge.setText(volStatus);
                    tvProfileStatusBadge.setBackgroundColor(
                            action.equals("Accept")
                                    ? Color.parseColor("#2E7D32")
                                    : Color.parseColor("#C62828"));

                    btnProfileAccept.setEnabled(false);
                    btnProfileAccept.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    btnProfileReject.setEnabled(false);
                    btnProfileReject.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));

                    Toast.makeText(this,
                            action.equals("Accept")
                                    ? volName + " accepted! ✅"
                                    : volName + " rejected.",
                            Toast.LENGTH_SHORT).show();

                    // ✅ Send SMS notification
                    try {
                        android.telephony.SmsManager sms =
                                android.telephony.SmsManager.getDefault();
                        String smsText = action.equals("Accept")
                                ? "Hello " + volName
                                + "! Your application on Smart Seva has been ACCEPTED. Please be ready!"
                                : "Hello " + volName
                                + "! Your application on Smart Seva was not selected this time. Keep trying!";
                        sms.sendTextMessage("+91" + volPhone, null, smsText, null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    void setDefaults() {
        if (volName == null)      volName      = "Volunteer";
        if (volCity == null)      volCity      = "City not provided";
        if (volEmail == null)     volEmail     = "Not provided";
        if (volPhone == null)     volPhone     = "Not provided";
        if (volSkills == null)    volSkills    = "Not specified";
        if (volAvailDays == null) volAvailDays = "Not specified";
        if (volAvailTime == null) volAvailTime = "Not specified";
        if (volLanguages == null) volLanguages = "Not specified";
        if (volVehicle == null)   volVehicle   = "Not specified";
        if (volTravel == null)    volTravel    = "Not specified";
        if (volCauses == null)    volCauses    = "Not specified";
        if (volIDType == null)    volIDType    = "Aadhaar Card";
    }

    void populateUI() {
        tvVolName.setText(volName);
        tvVolCity.setText("📍 " + volCity);
        tvVolEmail.setText("✉ " + volEmail);
        tvVolPhone.setText("📞 " + volPhone);

        tvStatTasksDone.setText(String.valueOf(volExperience * 3));
        tvStatImpact.setText(String.valueOf(volExperience * 50));
        tvStatRating.setText("⭐ " + (volExperience > 0 ? "4.2" : "New"));

        tvVolAvailDays.setText(volAvailDays);
        tvVolAvailTime.setText(volAvailTime.equals("Not specified") ? "Part-time" : volAvailTime);
        tvVolLanguages.setText(volLanguages);
        tvVolVehicle.setText(volVehicle);
        tvVolTravel.setText(volTravel);
        tvVolIDType.setText("✅ " + volIDType);

        String expText;
        switch (volExperience) {
            case 0:  expText = "No previous experience"; break;
            case 1:  expText = "Less than 1 year";       break;
            default: expText = "1+ years";               break;
        }
        tvVolExperience.setText(expText);

        // Status badge
        tvProfileStatusBadge.setText(volStatus);
        switch (volStatus) {
            case "Accepted":
                tvProfileStatusBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
                btnProfileAccept.setEnabled(false);
                btnProfileAccept.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                btnProfileReject.setEnabled(false);
                btnProfileReject.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                break;
            case "Rejected":
                tvProfileStatusBadge.setBackgroundColor(Color.parseColor("#C62828"));
                btnProfileAccept.setEnabled(false);
                btnProfileAccept.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                btnProfileReject.setEnabled(false);
                btnProfileReject.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                break;
            default:
                tvProfileStatusBadge.setBackgroundColor(Color.parseColor("#F57F17"));
                break;
        }

        // Skill chips
        layoutSkillChips.removeAllViews();
        for (String skill : volSkills.split(",")) {
            addChip(layoutSkillChips, skill.trim(), "#1A1A1A");
        }

        // Cause chips
        layoutCauseChips.removeAllViews();
        for (String cause : volCauses.split(",")) {
            addChip(layoutCauseChips, cause.trim(), "#1565C0");
        }
    }

    void addChip(LinearLayout layout, String text, String color) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(12f);
        chip.setBackgroundColor(Color.parseColor(color));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 12, 8);
        chip.setLayoutParams(params);
        chip.setPadding(24, 10, 24, 10);
        layout.addView(chip);
    }
}