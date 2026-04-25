package com.example.smartseva;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    String userRole = "NGO";

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String currentUid;

    // Top Bar
    TextView tvWelcome;

    // Panels
    LinearLayout panelNGOStats, panelNGOTasks, panelCreateTask,
            panelVolunteerList, panelAvailableTasks,
            panelMyApplications, panelProfile, panelImpactScore;

    // Bottom Nav - NGO
    Button btnNavStats, btnNavTasks, btnNavCreate, btnNavVolunteers;
    // Bottom Nav - Volunteer
    Button btnNavAvailable, btnNavApplications, btnNavProfile, btnNavImpact;

    // Stats
    TextView tvTotalTasks, tvCompletedTasks, tvUrgentTasks, tvTotalVolunteers;

    // Create Task fields
    EditText etTaskTitle, etTaskDesc, etVolunteersRequired, etTaskLocation, etTaskDate;
    TextView errTaskTitle, errTaskDesc, errVolunteersRequired, errTaskLocation, errTaskDate;
    Spinner spinnerTaskCategory, spinnerUrgency, spinnerSkillRequired;
    ImageView imgTaskPreview;
    Uri selectedTaskImageUri = null;
    private static final int PICK_TASK_IMAGE = 201;

    // Lists
    ListView listNGOTasks, listVolunteers, listAvailableTasks;

    // Profile
    ImageView imgVolProfile;
    TextView tvProfileName, tvProfileCity, tvProfileEmail, tvProfileSkills;
    TextView tvImpactScore, tvTasksDone, tvHoursContributed;

    // Task data for list clicks
    List<Task> ngoTaskObjects     = new ArrayList<>();
    List<Task> availableTaskObjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        bindViews();
        setupSpinners();

        // Get role from Intent
        String role = getIntent().getStringExtra("role");
        if (role != null) userRole = role;

        // Listeners
        etTaskDate.setOnClickListener(v -> showTaskDatePicker());
        findViewById(R.id.btnUploadTaskImage).setOnClickListener(v -> pickTaskImage());
        findViewById(R.id.btnCreateTask).setOnClickListener(v -> validateAndCreateTask());
        findViewById(R.id.btnQuickCreateTask).setOnClickListener(v -> showNGOPanel("create"));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        findViewById(R.id.btnFilterAll).setOnClickListener(v -> loadNGOTasks("all"));
        findViewById(R.id.btnFilterUrgent).setOnClickListener(v -> loadNGOTasks("urgent"));
        findViewById(R.id.btnFilterActive).setOnClickListener(v -> loadNGOTasks("active"));

        // Bottom nav - NGO
        btnNavStats.setOnClickListener(v -> showNGOPanel("stats"));
        btnNavTasks.setOnClickListener(v -> showNGOPanel("tasks"));
        btnNavCreate.setOnClickListener(v -> showNGOPanel("create"));
        btnNavVolunteers.setOnClickListener(v -> showNGOPanel("volunteers"));

        // Bottom nav - Volunteer
        btnNavAvailable.setOnClickListener(v -> showVolunteerPanel("tasks"));
        btnNavApplications.setOnClickListener(v -> showVolunteerPanel("applications"));
        btnNavProfile.setOnClickListener(v -> showVolunteerPanel("profile"));
        btnNavImpact.setOnClickListener(v -> showVolunteerPanel("impact"));

        // Smart Match
        findViewById(R.id.btnSmartMatchNGO).setOnClickListener(v -> openSmartMatchNGO());
        findViewById(R.id.btnSmartMatchVol).setOnClickListener(v -> openSmartMatchVolunteer());
        findViewById(R.id.btnDataCollection).setOnClickListener(v ->
                startActivity(new Intent(this, DataCollectionActivity.class)));

        // Auto-fill from Data Collection
        if (getIntent().getBooleanExtra("openCreate", false)) {
            autoFillCreateTask();
        }

        setupDashboard();
    }

    // ═══════════════════════════════════════
    // BIND VIEWS
    // ═══════════════════════════════════════

    void bindViews() {
        tvWelcome           = findViewById(R.id.tvWelcome);
        panelNGOStats       = findViewById(R.id.panelNGOStats);
        panelNGOTasks       = findViewById(R.id.panelNGOTasks);
        panelCreateTask     = findViewById(R.id.panelCreateTask);
        panelVolunteerList  = findViewById(R.id.panelVolunteerList);
        panelAvailableTasks = findViewById(R.id.panelAvailableTasks);
        panelMyApplications = findViewById(R.id.panelMyApplications);
        panelProfile        = findViewById(R.id.panelProfile);
        panelImpactScore    = findViewById(R.id.panelImpactScore);
        btnNavStats         = findViewById(R.id.btnNavStats);
        btnNavTasks         = findViewById(R.id.btnNavTasks);
        btnNavCreate        = findViewById(R.id.btnNavCreate);
        btnNavVolunteers    = findViewById(R.id.btnNavVolunteers);
        btnNavAvailable     = findViewById(R.id.btnNavAvailable);
        btnNavApplications  = findViewById(R.id.btnNavApplications);
        btnNavProfile       = findViewById(R.id.btnNavProfile);
        btnNavImpact        = findViewById(R.id.btnNavImpact);
        tvTotalTasks        = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks    = findViewById(R.id.tvCompletedTasks);
        tvUrgentTasks       = findViewById(R.id.tvUrgentTasks);
        tvTotalVolunteers   = findViewById(R.id.tvTotalVolunteers);
        etTaskTitle         = findViewById(R.id.etTaskTitle);
        etTaskDesc          = findViewById(R.id.etTaskDesc);
        etVolunteersRequired= findViewById(R.id.etVolunteersRequired);
        etTaskLocation      = findViewById(R.id.etTaskLocation);
        etTaskDate          = findViewById(R.id.etTaskDate);
        errTaskTitle        = findViewById(R.id.errTaskTitle);
        errTaskDesc         = findViewById(R.id.errTaskDesc);
        errVolunteersRequired = findViewById(R.id.errVolunteersRequired);
        errTaskLocation     = findViewById(R.id.errTaskLocation);
        errTaskDate         = findViewById(R.id.errTaskDate);
        spinnerTaskCategory = findViewById(R.id.spinnerTaskCategory);
        spinnerUrgency      = findViewById(R.id.spinnerUrgency);
        spinnerSkillRequired= findViewById(R.id.spinnerSkillRequired);
        imgTaskPreview      = findViewById(R.id.imgTaskPreview);
        listNGOTasks        = findViewById(R.id.listNGOTasks);
        listVolunteers      = findViewById(R.id.listVolunteers);
        listAvailableTasks  = findViewById(R.id.listAvailableTasks);
        imgVolProfile       = findViewById(R.id.imgVolProfile);
        tvProfileName       = findViewById(R.id.tvProfileName);
        tvProfileCity       = findViewById(R.id.tvProfileCity);
        tvProfileEmail      = findViewById(R.id.tvProfileEmail);
        tvProfileSkills     = findViewById(R.id.tvProfileSkills);
        tvImpactScore       = findViewById(R.id.tvImpactScore);
        tvTasksDone         = findViewById(R.id.tvTasksDone);
        tvHoursContributed  = findViewById(R.id.tvHoursContributed);
    }

    // ═══════════════════════════════════════
    // SETUP DASHBOARD
    // ═══════════════════════════════════════

    void setupDashboard() {
        if (userRole.equals("NGO")) {
            tvWelcome.setText("NGO Dashboard");
            btnNavStats.setVisibility(View.VISIBLE);
            btnNavTasks.setVisibility(View.VISIBLE);
            btnNavCreate.setVisibility(View.VISIBLE);
            btnNavVolunteers.setVisibility(View.VISIBLE);
            btnNavAvailable.setVisibility(View.GONE);
            btnNavApplications.setVisibility(View.GONE);
            btnNavProfile.setVisibility(View.GONE);
            btnNavImpact.setVisibility(View.GONE);
            showNGOPanel("stats");
            loadNGOName(); // ✅ Load NGO name from Firestore
        } else {
            tvWelcome.setText("Volunteer Dashboard");
            btnNavAvailable.setVisibility(View.VISIBLE);
            btnNavApplications.setVisibility(View.VISIBLE);
            btnNavProfile.setVisibility(View.VISIBLE);
            btnNavImpact.setVisibility(View.VISIBLE);
            btnNavStats.setVisibility(View.GONE);
            btnNavTasks.setVisibility(View.GONE);
            btnNavCreate.setVisibility(View.GONE);
            btnNavVolunteers.setVisibility(View.GONE);
            showVolunteerPanel("tasks");
            loadVolunteerProfile(); // ✅ Load volunteer profile from Firestore
        }
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD NGO NAME
    // ═══════════════════════════════════════

    void loadNGOName() {
        db.collection("ngo_users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("orgName");
                        if (name != null) tvWelcome.setText("Welcome, " + name);
                    }
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD STATS (NGO)
    // ═══════════════════════════════════════

    void loadStats() {
        // Total tasks by this NGO
        db.collection("tasks")
                .whereEqualTo("ngoId", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    int total     = snap.size();
                    int completed = 0;
                    int urgent    = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String status  = doc.getString("status");
                        String urgency = doc.getString("urgency");
                        if ("Completed".equals(status)) completed++;
                        if ("Critical".equals(urgency) && !"Completed".equals(status)) urgent++;
                    }
                    tvTotalTasks.setText(String.valueOf(total));
                    tvCompletedTasks.setText(String.valueOf(completed));
                    tvUrgentTasks.setText(String.valueOf(urgent));
                });

        // Total volunteers
        db.collection("volunteer_users").get()
                .addOnSuccessListener(snap ->
                        tvTotalVolunteers.setText(String.valueOf(snap.size())));
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD NGO TASKS
    // ═══════════════════════════════════════

    void loadNGOTasks(String filter) {
        Query query = db.collection("tasks")
                .whereEqualTo("ngoId", currentUid);

        if ("urgent".equals(filter)) {
            query = query.whereEqualTo("urgency", "Critical");
        } else if ("active".equals(filter)) {
            query = query.whereEqualTo("status", "Open");
        }

        query.get().addOnSuccessListener(snap -> {
            ngoTaskObjects.clear();
            List<String> displayList = new ArrayList<>();

            if (snap.isEmpty()) {
                displayList.add("No tasks found. Create your first task!");
            } else {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Task task = doc.toObject(Task.class);
                    task.setTaskId(doc.getId());
                    ngoTaskObjects.add(task);
                    displayList.add(task.getTitle()
                            + " | " + task.getUrgency()
                            + " | " + task.getLocation()
                            + " | " + task.getStatus());
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, displayList);
            listNGOTasks.setAdapter(adapter);

            listNGOTasks.setOnItemClickListener((parent, view, position, id) -> {
                if (position < ngoTaskObjects.size()) {
                    Task selected = ngoTaskObjects.get(position);
                    Intent intent = new Intent(this, ApplicantsActivity.class);
                    intent.putExtra("taskTitle", selected.getTitle());
                    intent.putExtra("taskId",    selected.getTaskId());
                    startActivity(intent);
                }
            });
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error loading tasks: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD VOLUNTEERS (NGO view)
    // ═══════════════════════════════════════

    void loadVolunteers() {
        db.collection("volunteer_users").get()
                .addOnSuccessListener(snap -> {
                    List<String> list = new ArrayList<>();
                    if (snap.isEmpty()) {
                        list.add("No volunteers registered yet.");
                    } else {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String name  = doc.getString("name");
                            String city  = doc.getString("city");
                            String skill = "";
                            // Find first true skill
                            String[] skills = {"teaching","medical","food",
                                    "event","fundraising","technical","socialMedia"};
                            for (String s : skills) {
                                Boolean val = doc.getBoolean(s);
                                if (Boolean.TRUE.equals(val)) { skill = s; break; }
                            }
                            list.add((name != null ? name : "Unknown")
                                    + " | " + (city != null ? city : "")
                                    + " | " + skill);
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, list);
                    listVolunteers.setAdapter(adapter);
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD AVAILABLE TASKS (Volunteer view)
    // ═══════════════════════════════════════

    void loadAvailableTasks() {
        db.collection("tasks")
                .whereEqualTo("status", "Open")
                .get()
                .addOnSuccessListener(snap -> {
                    availableTaskObjects.clear();
                    List<String> list = new ArrayList<>();

                    if (snap.isEmpty()) {
                        list.add("No tasks available right now.");
                    } else {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Task task = doc.toObject(Task.class);
                            task.setTaskId(doc.getId());
                            availableTaskObjects.add(task);
                            list.add(task.getTitle()
                                    + " | " + task.getUrgency()
                                    + " | " + task.getLocation());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, list);
                    listAvailableTasks.setAdapter(adapter);

                    listAvailableTasks.setOnItemClickListener((parent, view, position, id) -> {
                        if (position < availableTaskObjects.size()) {
                            Task selected = availableTaskObjects.get(position);
                            Intent intent = new Intent(this, TaskDetailActivity.class);
                            intent.putExtra("taskId",       selected.getTaskId());
                            intent.putExtra("taskTitle",    selected.getTitle());
                            intent.putExtra("taskDesc",     selected.getDescription());
                            intent.putExtra("taskUrgency",  selected.getUrgency());
                            intent.putExtra("taskLocation", selected.getLocation());
                            intent.putExtra("taskCategory", selected.getCategory());
                            intent.putExtra("taskSkill",    selected.getSkills());
                            intent.putExtra("taskNGO",      selected.getNgoId());
                            startActivity(intent);
                        }
                    });
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ═══════════════════════════════════════
    // FIREBASE — LOAD VOLUNTEER PROFILE
    // ═══════════════════════════════════════

    void loadVolunteerProfile() {
        db.collection("volunteer_users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvProfileName.setText(doc.getString("name"));
                        tvProfileCity.setText(doc.getString("city")
                                + ", " + doc.getString("state"));
                        tvProfileEmail.setText(doc.getString("email"));

                        // Build skills string
                        StringBuilder skills = new StringBuilder();
                        String[] skillKeys = {"teaching","medical","food",
                                "event","fundraising","technical","socialMedia"};
                        String[] skillLabels = {"Teaching","Medical","Food",
                                "Event","Fundraising","Technical","Social Media"};
                        for (int i = 0; i < skillKeys.length; i++) {
                            Boolean val = doc.getBoolean(skillKeys[i]);
                            if (Boolean.TRUE.equals(val)) {
                                if (skills.length() > 0) skills.append(", ");
                                skills.append(skillLabels[i]);
                            }
                        }
                        tvProfileSkills.setText(skills.toString());
                    }
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — CREATE TASK
    // ═══════════════════════════════════════

    void validateAndCreateTask() {
        boolean ok = true;

        String title = etTaskTitle.getText().toString().trim();
        if (title.isEmpty()) { errTaskTitle.setText("Task title is required"); ok = false; }
        else errTaskTitle.setText("");

        String desc = etTaskDesc.getText().toString().trim();
        if (desc.isEmpty()) { errTaskDesc.setText("Description is required"); ok = false; }
        else errTaskDesc.setText("");

        if (spinnerTaskCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show(); ok = false; }
        if (spinnerUrgency.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select urgency", Toast.LENGTH_SHORT).show(); ok = false; }

        String volCount = etVolunteersRequired.getText().toString().trim();
        if (volCount.isEmpty()) { errVolunteersRequired.setText("Required"); ok = false; }
        else errVolunteersRequired.setText("");

        String location = etTaskLocation.getText().toString().trim();
        if (location.isEmpty()) { errTaskLocation.setText("Location is required"); ok = false; }
        else errTaskLocation.setText("");

        String date = etTaskDate.getText().toString().trim();
        if (date.isEmpty()) { errTaskDate.setText("Date is required"); ok = false; }
        else errTaskDate.setText("");

        if (!ok) return;

        // ✅ Save to Firestore
        Map<String, Object> task = new HashMap<>();
        task.put("title",       title);
        task.put("description", desc);
        task.put("category",    spinnerTaskCategory.getSelectedItem().toString());
        task.put("urgency",     spinnerUrgency.getSelectedItem().toString());
        task.put("skills",      spinnerSkillRequired.getSelectedItem().toString());
        task.put("volunteers",  volCount);
        task.put("location",    location);
        task.put("date",        date);
        task.put("ngoId",       currentUid);
        task.put("status",      "Open");
        task.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("tasks").add(task)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this,
                            "Task Created Successfully! ✅",
                            Toast.LENGTH_LONG).show();
                    // Clear form
                    etTaskTitle.setText("");
                    etTaskDesc.setText("");
                    etVolunteersRequired.setText("");
                    etTaskLocation.setText("");
                    etTaskDate.setText("");
                    spinnerTaskCategory.setSelection(0);
                    spinnerUrgency.setSelection(0);
                    imgTaskPreview.setImageResource(R.drawable.ic_add_photo);
                    selectedTaskImageUri = null;
                    showNGOPanel("tasks");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ═══════════════════════════════════════
    // PANEL SWITCHING
    // ═══════════════════════════════════════

    void showNGOPanel(String panel) {
        panelNGOStats.setVisibility(View.GONE);
        panelNGOTasks.setVisibility(View.GONE);
        panelCreateTask.setVisibility(View.GONE);
        panelVolunteerList.setVisibility(View.GONE);
        setNavActive(btnNavStats, false);
        setNavActive(btnNavTasks, false);
        setNavActive(btnNavCreate, false);
        setNavActive(btnNavVolunteers, false);

        switch (panel) {
            case "stats":
                panelNGOStats.setVisibility(View.VISIBLE);
                setNavActive(btnNavStats, true);
                loadStats();
                break;
            case "tasks":
                panelNGOTasks.setVisibility(View.VISIBLE);
                setNavActive(btnNavTasks, true);
                loadNGOTasks("all");
                break;
            case "create":
                panelCreateTask.setVisibility(View.VISIBLE);
                setNavActive(btnNavCreate, true);
                break;
            case "volunteers":
                panelVolunteerList.setVisibility(View.VISIBLE);
                setNavActive(btnNavVolunteers, true);
                loadVolunteers();
                break;
        }
    }

    void showVolunteerPanel(String panel) {
        panelAvailableTasks.setVisibility(View.GONE);
        panelMyApplications.setVisibility(View.GONE);
        panelProfile.setVisibility(View.GONE);
        panelImpactScore.setVisibility(View.GONE);
        setNavActive(btnNavAvailable, false);
        setNavActive(btnNavApplications, false);
        setNavActive(btnNavProfile, false);
        setNavActive(btnNavImpact, false);

        switch (panel) {
            case "tasks":
                panelAvailableTasks.setVisibility(View.VISIBLE);
                setNavActive(btnNavAvailable, true);
                loadAvailableTasks();
                break;
            case "applications":
                panelMyApplications.setVisibility(View.VISIBLE);
                setNavActive(btnNavApplications, true);
                loadMyApplications();
                break;
            case "profile":
                panelProfile.setVisibility(View.VISIBLE);
                setNavActive(btnNavProfile, true);
                loadVolunteerProfile();
                break;
            case "impact":
                panelImpactScore.setVisibility(View.VISIBLE);
                setNavActive(btnNavImpact, true);
                loadImpactScore();
                break;
        }
    }

    // ═══════════════════════════════════════
    // FIREBASE — MY APPLICATIONS (Volunteer)
    // ═══════════════════════════════════════

    void loadMyApplications() {
        db.collection("applications")
                .whereEqualTo("volunteerId", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> list = new ArrayList<>();
                    if (snap.isEmpty()) {
                        list.add("You haven't applied to any tasks yet.");
                    } else {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String taskTitle = doc.getString("taskTitle");
                            String status    = doc.getString("status");
                            list.add((taskTitle != null ? taskTitle : "Task")
                                    + " | " + (status != null ? status : "Pending"));
                        }
                    }
                    // show in a ListView in panelMyApplications
                    // add a ListView with id listMyApplications in your XML
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — IMPACT SCORE (Volunteer)
    // ═══════════════════════════════════════

    void loadImpactScore() {
        db.collection("applications")
                .whereEqualTo("volunteerId", currentUid)
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(snap -> {
                    int completed = snap.size();
                    int score     = completed * 10;
                    tvImpactScore.setText(String.valueOf(score));
                    tvTasksDone.setText(String.valueOf(completed));
                    tvHoursContributed.setText(String.valueOf(completed * 3));
                });
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════

    void setNavActive(Button btn, boolean active) {
        btn.setTextColor(active
                ? Color.parseColor("#1A1A1A")
                : Color.parseColor("#888888"));
    }

    void autoFillCreateTask() {
        showNGOPanel("create");
        String title    = getIntent().getStringExtra("autoFillTitle");
        String desc     = getIntent().getStringExtra("autoFillDesc");
        String location = getIntent().getStringExtra("autoFillLocation");
        String vol      = getIntent().getStringExtra("autoFillVolunteers");
        if (title != null)    etTaskTitle.setText(title);
        if (desc != null)     etTaskDesc.setText(desc);
        if (location != null) etTaskLocation.setText(location);
        if (vol != null)      etVolunteersRequired.setText(vol);
        Toast.makeText(this, "✅ Form auto-filled! Review and submit.",
                Toast.LENGTH_LONG).show();
    }

    void showTaskDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            etTaskDate.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
            errTaskDate.setText("");
        }, cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    void pickTaskImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_TASK_IMAGE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_TASK_IMAGE && res == RESULT_OK
                && data != null && data.getData() != null) {
            selectedTaskImageUri = data.getData();
            imgTaskPreview.setImageURI(selectedTaskImageUri);
        }
    }

    void openSmartMatchNGO() {
        Intent intent = new Intent(this, SmartMatchActivity.class);
        intent.putExtra("mode", "NGO");
        startActivity(intent);
    }

    void openSmartMatchVolunteer() {
        Intent intent = new Intent(this, SmartMatchActivity.class);
        intent.putExtra("mode", "Volunteer");
        startActivity(intent);
    }

    void logout() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void setupSpinners() {
        spinnerTaskCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Category","Food Distribution","Education",
                        "Medical Help","Environment","Disaster Relief","Event","Other"}));
        spinnerUrgency.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Urgency","Critical","Moderate","Normal"}));
        spinnerSkillRequired.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Any Skill","Teaching","Medical","Food Distribution",
                        "Event Management","Fundraising","Technical","Social Media"}));
    }
}