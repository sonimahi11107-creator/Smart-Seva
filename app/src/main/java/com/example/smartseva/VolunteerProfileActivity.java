package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

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
    String volName, volCity, volEmail, volPhone;
    String volSkills, volAvailDays, volAvailTime;
    String volLanguages, volVehicle, volTravel;
    String volCauses, volIDType, volStatus;
    int volExperience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);

        // ── Views ──
        imgVolunteerPhoto  = findViewById(R.id.imgVolunteerPhoto);
        tvVolName          = findViewById(R.id.tvVolName);
        tvVolCity          = findViewById(R.id.tvVolCity);
        tvVolEmail         = findViewById(R.id.tvVolEmail);
        tvVolPhone         = findViewById(R.id.tvVolPhone);
        tvStatTasksDone    = findViewById(R.id.tvStatTasksDone);
        tvStatImpact       = findViewById(R.id.tvStatImpact);
        tvStatRating       = findViewById(R.id.tvStatRating);
        tvVolAvailDays     = findViewById(R.id.tvVolAvailDays);
        tvVolAvailTime     = findViewById(R.id.tvVolAvailTime);
        tvVolLanguages     = findViewById(R.id.tvVolLanguages);
        tvVolVehicle       = findViewById(R.id.tvVolVehicle);
        tvVolTravel        = findViewById(R.id.tvVolTravel);
        tvVolExperience    = findViewById(R.id.tvVolExperience);
        tvVolIDType        = findViewById(R.id.tvVolIDType);
        tvProfileStatusBadge = findViewById(R.id.tvProfileStatusBadge);
        layoutSkillChips   = findViewById(R.id.layoutSkillChips);
        layoutCauseChips   = findViewById(R.id.layoutCauseChips);
        btnBackProfile     = findViewById(R.id.btnBackProfile);
        btnCallVolunteer   = findViewById(R.id.btnCallVolunteer);
        btnSMSVolunteer    = findViewById(R.id.btnSMSVolunteer);
        btnProfileAccept   = findViewById(R.id.btnProfileAccept);
        btnProfileReject   = findViewById(R.id.btnProfileReject);

        // ── Intent Data ──
        Intent intent = getIntent();
        volName        = intent.getStringExtra("name");
        volCity        = intent.getStringExtra("city");
        volEmail       = intent.getStringExtra("email");
        volPhone       = intent.getStringExtra("phone");
        volSkills      = intent.getStringExtra("skills");
        volAvailDays   = intent.getStringExtra("availability");
        volAvailTime   = intent.getStringExtra("availTime");
        volLanguages   = intent.getStringExtra("languages");
        volVehicle     = intent.getStringExtra("vehicle");
        volTravel      = intent.getStringExtra("travel");
        volCauses      = intent.getStringExtra("causes");
        volIDType      = intent.getStringExtra("idType");
        volStatus      = intent.getStringExtra("status");
        volExperience  = intent.getIntExtra("experience", 0);

        // Defaults
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
        if (volStatus == null)    volStatus    = "Pending";

        // ── Populate UI ──
        populateUI();

        // ── Listeners ──
        btnBackProfile.setOnClickListener(v -> finish());

        btnCallVolunteer.setOnClickListener(v -> {
            if (!volPhone.equals("Not provided")) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:+91" + volPhone));
                startActivity(callIntent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnSMSVolunteer.setOnClickListener(v -> {
            if (!volPhone.equals("Not provided")) {
                Intent smsIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("sms:+91" + volPhone));
                smsIntent.putExtra("sms_body",
                        "Hello " + volName + "! Regarding your volunteer application on Smart Seva...");
                startActivity(smsIntent);
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnProfileAccept.setOnClickListener(v -> confirmAction("Accept"));
        btnProfileReject.setOnClickListener(v -> confirmAction("Reject"));
    }

    // ═══════════════════════════════════════
    // POPULATE UI
    // ═══════════════════════════════════════

    void populateUI() {

        // Header
        tvVolName.setText(volName);
        tvVolCity.setText("📍 " + volCity);
        tvVolEmail.setText("✉ " + volEmail);
        tvVolPhone.setText("📞 " + volPhone);

        // Stats — Firebase teammate real data set karega
        tvStatTasksDone.setText(String.valueOf(volExperience * 3));
        tvStatImpact.setText(String.valueOf(volExperience * 50));
        tvStatRating.setText("⭐ " + (volExperience > 0 ? "4.2" : "New"));

        // Availability
        tvVolAvailDays.setText(volAvailDays);
        tvVolAvailTime.setText(volAvailTime.equals("Not specified") ? "Part-time" : volAvailTime);

        // Details
        tvVolLanguages.setText(volLanguages);
        tvVolVehicle.setText(volVehicle);
        tvVolTravel.setText(volTravel);
        tvVolIDType.setText("✅ " + volIDType);

        String expText;
        switch (volExperience) {
            case 0:  expText = "No previous experience"; break;
            case 1:  expText = "Less than 1 year"; break;
            default: expText = "1+ years"; break;
        }
        tvVolExperience.setText(expText);

        // Status Badge
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

        // Skill Chips
        layoutSkillChips.removeAllViews();
        String[] skills = volSkills.split(",");
        for (String skill : skills) {
            TextView chip = new TextView(this);
            chip.setText(skill.trim());
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(12f);
            chip.setBackgroundColor(Color.parseColor("#1A1A1A"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 8);
            chip.setLayoutParams(params);
            chip.setPadding(24, 10, 24, 10);
            layoutSkillChips.addView(chip);
        }

        // Cause Chips
        layoutCauseChips.removeAllViews();
        String[] causes = volCauses.split(",");
        for (String cause : causes) {
            TextView chip = new TextView(this);
            chip.setText(cause.trim());
            chip.setTextColor(Color.WHITE);
            chip.setTextSize(12f);
            chip.setBackgroundColor(Color.parseColor("#1565C0"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 8);
            chip.setLayoutParams(params);
            chip.setPadding(24, 10, 24, 10);
            layoutCauseChips.addView(chip);
        }
    }

    // ═══════════════════════════════════════
    // ACCEPT / REJECT
    // ═══════════════════════════════════════

    void confirmAction(String action) {
        String msg = action.equals("Accept") ?
                "Accept " + volName + " as a volunteer for this task?" :
                "Reject " + volName + "'s application?";

        new android.app.AlertDialog.Builder(this)
                .setTitle(action + " Volunteer")
                .setMessage(msg)
                .setPositiveButton(action, (dialog, which) -> {
                    volStatus = action.equals("Accept") ? "Accepted" : "Rejected";

                    // Firebase teammate yahan Firestore update karega
                    // SMS bhi bhejega

                    // UI update
                    tvProfileStatusBadge.setText(volStatus);
                    tvProfileStatusBadge.setBackgroundColor(
                            action.equals("Accept") ?
                                    Color.parseColor("#2E7D32") :
                                    Color.parseColor("#C62828"));

                    btnProfileAccept.setEnabled(false);
                    btnProfileAccept.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
                    btnProfileReject.setEnabled(false);
                    btnProfileReject.setBackgroundTintList(
                            ColorStateList.valueOf(Color.parseColor("#AAAAAA")));

                    String toastMsg = action.equals("Accept") ?
                            volName + " accepted! ✅ They will be notified." :
                            volName + "'s application rejected.";
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();

                    // SMS
                    try {
                        android.telephony.SmsManager sms =
                                android.telephony.SmsManager.getDefault();
                        String smsText = action.equals("Accept") ?
                                "Hello " + volName + "! Your application on Smart Seva has been ACCEPTED. Please be ready!" :
                                "Hello " + volName + "! Your application on Smart Seva was not selected this time. Keep trying!";
                        sms.sendTextMessage("+91" + volPhone, null, smsText, null, null);
                    } catch (Exception e) { e.printStackTrace(); }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}