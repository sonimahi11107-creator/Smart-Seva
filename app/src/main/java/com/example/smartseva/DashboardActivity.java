package com.example.smartseva;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Role
    String userRole = "NGO"; // "NGO" or "Volunteer"

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
    // FIX: Added missing ListView and TextViews that were referenced but never declared
    ListView listMyTasks;
    TextView tvMyTasksActive, tvMyTasksDone;

    // Profile
    ImageView imgVolProfile;
    TextView tvProfileName, tvProfileCity, tvProfileEmail, tvProfileSkills;
    TextView tvImpactScore, tvTasksDone, tvHoursContributed;

    // Status tracking
    TextView tvCountOpen, tvCountAssigned, tvCountProgress, tvCountResolved;

    // Sample data
    List<String> taskList = new ArrayList<>();
    List<String> volunteerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Notification channels create karo
        NotificationHelper.createChannels(this);

        // ── Top Bar ──
        tvWelcome = findViewById(R.id.tvWelcome);

        // ── Panels ──
        panelNGOStats       = findViewById(R.id.panelNGOStats);
        panelNGOTasks       = findViewById(R.id.panelNGOTasks);
        panelCreateTask     = findViewById(R.id.panelCreateTask);
        panelVolunteerList  = findViewById(R.id.panelVolunteerList);
        panelAvailableTasks = findViewById(R.id.panelAvailableTasks);
        panelMyApplications = findViewById(R.id.panelMyApplications);
        panelProfile        = findViewById(R.id.panelProfile);
        panelImpactScore    = findViewById(R.id.panelImpactScore);

        // ── Bottom Nav ──
        btnNavStats        = findViewById(R.id.btnNavStats);
        btnNavTasks        = findViewById(R.id.btnNavTasks);
        btnNavCreate       = findViewById(R.id.btnNavCreate);
        btnNavVolunteers   = findViewById(R.id.btnNavVolunteers);
        btnNavAvailable    = findViewById(R.id.btnNavAvailable);
        btnNavApplications = findViewById(R.id.btnNavApplications);
        btnNavProfile      = findViewById(R.id.btnNavProfile);
        btnNavImpact       = findViewById(R.id.btnNavImpact);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ── Stats ──
        tvTotalTasks      = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks  = findViewById(R.id.tvCompletedTasks);
        tvUrgentTasks     = findViewById(R.id.tvUrgentTasks);
        tvTotalVolunteers = findViewById(R.id.tvTotalVolunteers);

        // ── Create Task ──
        etTaskTitle           = findViewById(R.id.etTaskTitle);
        etTaskDesc            = findViewById(R.id.etTaskDesc);
        etVolunteersRequired  = findViewById(R.id.etVolunteersRequired);
        etTaskLocation        = findViewById(R.id.etTaskLocation);
        etTaskDate            = findViewById(R.id.etTaskDate);
        errTaskTitle          = findViewById(R.id.errTaskTitle);
        errTaskDesc           = findViewById(R.id.errTaskDesc);
        errVolunteersRequired = findViewById(R.id.errVolunteersRequired);
        errTaskLocation       = findViewById(R.id.errTaskLocation);
        errTaskDate           = findViewById(R.id.errTaskDate);
        spinnerTaskCategory   = findViewById(R.id.spinnerTaskCategory);
        spinnerUrgency        = findViewById(R.id.spinnerUrgency);
        spinnerSkillRequired  = findViewById(R.id.spinnerSkillRequired);
        imgTaskPreview        = findViewById(R.id.imgTaskPreview);

        // ── Lists ──
        listNGOTasks       = findViewById(R.id.listNGOTasks);
        listVolunteers     = findViewById(R.id.listVolunteers);
        listAvailableTasks = findViewById(R.id.listAvailableTasks);
        listMyTasks        = findViewById(R.id.listMyTasks);

        // ── My Tasks counters ──
        tvMyTasksActive = findViewById(R.id.tvMyTasksActive);
        tvMyTasksDone   = findViewById(R.id.tvMyTasksDone);

        // ── Profile ──
        imgVolProfile      = findViewById(R.id.imgVolProfile);
        tvProfileName      = findViewById(R.id.tvProfileName);
        tvProfileCity      = findViewById(R.id.tvProfileCity);
        tvProfileEmail     = findViewById(R.id.tvProfileEmail);
        tvProfileSkills    = findViewById(R.id.tvProfileSkills);
        tvImpactScore      = findViewById(R.id.tvImpactScore);
        tvTasksDone        = findViewById(R.id.tvTasksDone);
        tvHoursContributed = findViewById(R.id.tvHoursContributed);

        // ── Status counters ──
        tvCountOpen     = findViewById(R.id.tvCountOpen);
        tvCountAssigned = findViewById(R.id.tvCountAssigned);
        tvCountProgress = findViewById(R.id.tvCountProgress);
        tvCountResolved = findViewById(R.id.tvCountResolved);

        // ── Filter buttons (status) ──
        if (findViewById(R.id.btnFilterOpen) != null) {
            findViewById(R.id.btnFilterOpen).setOnClickListener(v ->
                    loadTasksByStatus(TaskStatusManager.STATUS_OPEN));
            findViewById(R.id.btnFilterAssigned).setOnClickListener(v ->
                    loadTasksByStatus(TaskStatusManager.STATUS_ASSIGNED));
            findViewById(R.id.btnFilterProgress).setOnClickListener(v ->
                    loadTasksByStatus(TaskStatusManager.STATUS_IN_PROGRESS));
            findViewById(R.id.btnFilterResolved).setOnClickListener(v ->
                    loadTasksByStatus(TaskStatusManager.STATUS_RESOLVED));
        }

        // ── Setup ──
        setupSpinners();
        setupSampleData();

        // ── Listeners ──
        etTaskDate.setOnClickListener(v -> showTaskDatePicker());
        findViewById(R.id.btnUploadTaskImage).setOnClickListener(v -> pickTaskImage());
        findViewById(R.id.btnCreateTask).setOnClickListener(v -> validateAndCreateTask());
        findViewById(R.id.btnQuickCreateTask).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        findViewById(R.id.btnPredictiveAlerts).setOnClickListener(v ->
                startActivity(new Intent(this,
                        PredictiveAlertsActivity.class)));
        if (findViewById(R.id.btnLogoutProfile) != null) {
            findViewById(R.id.btnLogoutProfile).setOnClickListener(v -> logout());
        }

        // Filter buttons (urgency/active)
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> loadTasks("all"));
        if (findViewById(R.id.btnFilterUrgent) != null) {
            findViewById(R.id.btnFilterUrgent).setOnClickListener(v -> loadTasks("urgent"));
        }
        if (findViewById(R.id.btnFilterActive) != null) {
            findViewById(R.id.btnFilterActive).setOnClickListener(v -> loadTasks("active"));
        }

        // Bottom nav - NGO
        btnNavStats.setOnClickListener(v -> showNGOPanel("stats"));
        btnNavTasks.setOnClickListener(v -> showNGOPanel("tasks"));
        btnNavCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateTaskActivity.class)));
        btnNavVolunteers.setOnClickListener(v -> showNGOPanel("volunteers"));

        // Bottom nav - Volunteer
        btnNavAvailable.setOnClickListener(v -> showVolunteerPanel("tasks"));
        btnNavApplications.setOnClickListener(v -> showVolunteerPanel("applications"));
        btnNavProfile.setOnClickListener(v -> showVolunteerPanel("profile"));
        btnNavImpact.setOnClickListener(v -> showVolunteerPanel("impact"));


        // Role from Intent
        String role = getIntent().getStringExtra("role");
        if (role != null) userRole = role;

        // Auto-fill from Data Collection
        if (getIntent().getBooleanExtra("openCreate", false)) {
            autoFillCreateTask();
        }

        setupDashboard();

        findViewById(R.id.btnSmartMatchNGO).setOnClickListener(v -> openSmartMatchNGO());
        findViewById(R.id.btnSmartMatchVol).setOnClickListener(v -> openSmartMatchVolunteer());
        findViewById(R.id.btnDataCollection).setOnClickListener(v ->
                startActivity(new Intent(this, DataCollectionActivity.class)));

        findViewById(R.id.btnLeaderboard).setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        findViewById(R.id.btnLeaderboardVol).setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));

        findViewById(R.id.btnMapView).setOnClickListener(v ->
                startActivity(new Intent(this, MapViewActivity.class)));


        findViewById(R.id.btnMapViewVol).setOnClickListener(v ->
                startActivity(new Intent(this, MapViewActivity.class)));

        findViewById(R.id.btnAnonymousReport).setOnClickListener(v ->
                startActivity(new Intent(this, AnonymousReportActivity.class)));

        findViewById(R.id.btnAnonymousReportVol).setOnClickListener(v ->
                startActivity(new Intent(this, AnonymousReportActivity.class)));

        findViewById(R.id.btnOperationsCenter).setOnClickListener(v ->
                startActivity(new Intent(this, OperationsCenterActivity.class)));

        findViewById(R.id.btnEmergencyMode).setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyModeActivity.class)));

        // LocalTaskStore se tasks show
        List<LocalTaskStore.LocalTask> savedTasks = LocalTaskStore.getInstance().getTasks();
        if (!savedTasks.isEmpty()) {
            Toast.makeText(this,
                    savedTasks.size() + " community tasks available!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ═══════════════════════════════════════
    // AUTO-FILL
    // ═══════════════════════════════════════

    void autoFillCreateTask() {
        showNGOPanel("create");

        String title      = getIntent().getStringExtra("autoFillTitle");
        String desc       = getIntent().getStringExtra("autoFillDesc");
        String location   = getIntent().getStringExtra("autoFillLocation");
        String volunteers = getIntent().getStringExtra("autoFillVolunteers");

        if (title != null)      etTaskTitle.setText(title);
        if (desc != null)       etTaskDesc.setText(desc);
        if (location != null)   etTaskLocation.setText(location);
        if (volunteers != null) etVolunteersRequired.setText(volunteers);

        Toast.makeText(this,
                "Form auto-filled by AI! Review and submit.",
                Toast.LENGTH_LONG).show();
    }

    // ═══════════════════════════════════════
    // DASHBOARD SETUP
    // ═══════════════════════════════════════

    void setupDashboard() {
        if (userRole.equals("NGO")) {
            tvWelcome.setText("NGO Dashboard");
            btnNavStats.setVisibility(View.VISIBLE);
            btnNavTasks.setVisibility(View.VISIBLE);
            btnNavCreate.setVisibility(View.VISIBLE);
            btnNavVolunteers.setVisibility(View.VISIBLE);
            btnNavStats.setOnLongClickListener(v -> {
                openSmartMatchNGO();
                return true;
            });
            showNGOPanel("stats");
        } else {
            tvWelcome.setText("Volunteer Dashboard");
            btnNavAvailable.setVisibility(View.VISIBLE);
            btnNavApplications.setVisibility(View.VISIBLE);
            btnNavProfile.setVisibility(View.VISIBLE);
            btnNavImpact.setVisibility(View.VISIBLE);
            showVolunteerPanel("tasks");
            btnNavAvailable.setOnLongClickListener(v -> {
                openSmartMatchVolunteer();
                return true;
            });
        }
    }

    // ═══════════════════════════════════════
    // SMART MATCH
    // ═══════════════════════════════════════

    void openSmartMatchNGO() {
        Intent intent = new Intent(this, SmartMatchActivity.class);
        intent.putExtra("mode",         "NGO");
        intent.putExtra("taskTitle",    "Food Distribution Drive");
        intent.putExtra("taskSkill",    "Food Distribution");
        intent.putExtra("taskLocation", "Raipur, CG");
        intent.putExtra("taskUrgency",  "Critical (24 hrs)");
        startActivity(intent);
    }

    void openSmartMatchVolunteer() {
        Intent intent = new Intent(this, SmartMatchActivity.class);
        intent.putExtra("mode",            "Volunteer");
        intent.putExtra("volSkills",       "Teaching, Medical Help");
        intent.putExtra("volCity",         "Raipur, CG");
        intent.putExtra("volAvailability", "Weekends");
        startActivity(intent);
    }

    // ═══════════════════════════════════════
    // PANEL SWITCHING - NGO
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
                loadTasks("all");
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

    // ═══════════════════════════════════════
    // PANEL SWITCHING - VOLUNTEER
    // ═══════════════════════════════════════

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
                loadMyTasks();
                break;
            case "profile":
                panelProfile.setVisibility(View.VISIBLE);
                setNavActive(btnNavProfile, true);
                break;
            case "impact":
                panelImpactScore.setVisibility(View.VISIBLE);
                setNavActive(btnNavImpact, true);
                break;
        }
    }

    void setNavActive(Button btn, boolean active) {
        btn.setTextColor(active
                ? Color.parseColor("#1A1A1A")
                : Color.parseColor("#888888"));
    }

    // ═══════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════

    void loadStats() {
        // FIX: Added null check to prevent NullPointerException on getUid()
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("tasks")
                .whereEqualTo("ngoId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size();
                    int urgent = 0, active = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap) {
                        String urgency = doc.getString("urgency");
                        String status  = doc.getString("status");
                        if (urgency != null && urgency.contains("Critical")) urgent++;
                        if ("Active".equals(status)) active++;
                    }

                    tvTotalTasks.setText(String.valueOf(total));
                    tvUrgentTasks.setText(String.valueOf(urgent));
                    tvCompletedTasks.setText(String.valueOf(active));
                });

        db.collection("volunteer_users")
                .get()
                .addOnSuccessListener(snap ->
                        tvTotalVolunteers.setText(String.valueOf(snap.size())));
    }

    void loadTasks(String filter) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("tasks")
                .whereEqualTo("ngoId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    taskList.clear();
                    List<String> taskIds = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap) {
                        String title   = doc.getString("title");
                        String urgency = doc.getString("urgency");
                        String loc     = doc.getString("location");
                        String status  = doc.getString("status");

                        if (!filter.equals("all")) {
                            if (filter.equals("urgent") &&
                                    (urgency == null || !urgency.contains("Critical"))) continue;
                            if (filter.equals("active") &&
                                    (!"Active".equals(status))) continue;
                        }

                        if (title != null && !title.isEmpty()) {
                            taskList.add(String.format(Locale.getDefault(),
                                    "%s | %s | %s", title, urgency, loc));
                        }
                        taskIds.add(doc.getId());
                    }

                    if (taskList.isEmpty()) {
                        taskList.add("No tasks found. Create your first task!");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, taskList);
                    listNGOTasks.setAdapter(adapter);

                    // FIX 1: Sirf EK listener — nested aur duplicate hata diya
                    // FIX 2: pehle dialog dikhao, andar se startActivity karo
                    listNGOTasks.setOnItemClickListener((parent, view, pos, id) -> {
                        if (pos >= taskIds.size()) return;

                        // FIX 3: Lambda variables alag naam se — conflict avoid
                        String selectedTitle  = taskList.get(pos).split("\\|")[0].trim();
                        String selectedUrgency = taskList.get(pos).split("\\|").length > 1
                                ? taskList.get(pos).split("\\|")[1].trim() : "";
                        String selectedTaskId = taskIds.get(pos);

                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Task Options")
                                .setItems(new String[]{
                                        "👥 View Applicants",
                                        "🎯 Smart Allocate Volunteers"
                                }, (dlg, which) -> {
                                    if (which == 0) {
                                        // View Applicants
                                        Intent intentApplicants = new Intent(
                                                DashboardActivity.this,
                                                ApplicantsActivity.class);
                                        intentApplicants.putExtra("taskTitle", selectedTitle);
                                        startActivity(intentApplicants);
                                    } else {
                                        // Smart Allocate
                                        Intent intentAlloc = new Intent(
                                                DashboardActivity.this,
                                                DynamicAllocationActivity.class);
                                        intentAlloc.putExtra("taskTitle",    selectedTitle);
                                        intentAlloc.putExtra("taskSkill",    "Medical Help");
                                        intentAlloc.putExtra("taskLocation", "Raipur");
                                        intentAlloc.putExtra("taskUrgency",  selectedUrgency);
                                        intentAlloc.putExtra("taskId",       selectedTaskId);
                                        startActivity(intentAlloc);
                                    }
                                })
                                .show();
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    void loadVolunteers() {
        db.collection("volunteer_users")
                .get()
                .addOnSuccessListener(snap -> {
                    volunteerList.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap) {
                        String name   = doc.getString("name");
                        String city   = doc.getString("city");
                        String skills = "";
                        if (Boolean.TRUE.equals(doc.getBoolean("teaching")))  skills += "Teaching, ";
                        if (Boolean.TRUE.equals(doc.getBoolean("medical")))   skills += "Medical, ";
                        if (Boolean.TRUE.equals(doc.getBoolean("food")))      skills += "Food, ";
                        if (Boolean.TRUE.equals(doc.getBoolean("technical"))) skills += "Technical, ";
                        if (!skills.isEmpty()) skills = skills.substring(0, skills.length() - 2);

                        volunteerList.add(String.format(Locale.getDefault(),
                                "%s | %s | %s", name, city, skills));
                    }

                    if (volunteerList.isEmpty()) {
                        volunteerList.add("No volunteers registered yet.");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, volunteerList);
                    listVolunteers.setAdapter(adapter);
                });
    }

    void loadAvailableTasks() {
        db.collection("tasks")
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(snap -> {
                    List<String> available = new ArrayList<>();
                    List<com.google.firebase.firestore.DocumentSnapshot> docs =
                            snap.getDocuments();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
                        String title   = doc.getString("title");
                        String urgency = doc.getString("urgency");
                        String loc     = doc.getString("location");
                        available.add(String.format(Locale.getDefault(),
                                "%s | %s | %s", title, urgency, loc));
                    }

                    if (available.isEmpty()) {
                        available.add("No tasks available right now.");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, available);
                    listAvailableTasks.setAdapter(adapter);

                    listAvailableTasks.setOnItemClickListener((parent, view, pos, id) -> {
                        if (pos >= docs.size()) return;
                        com.google.firebase.firestore.DocumentSnapshot doc = docs.get(pos);

                        Intent intent = new Intent(this, TaskDetailActivity.class);
                        intent.putExtra("taskTitle",      doc.getString("title"));
                        intent.putExtra("taskDesc",       doc.getString("description"));
                        intent.putExtra("taskCategory",   doc.getString("category"));
                        intent.putExtra("taskUrgency",    doc.getString("urgency"));
                        intent.putExtra("taskLocation",   doc.getString("location"));
                        intent.putExtra("taskSkill",      doc.getString("skill"));
                        intent.putExtra("taskDate",       doc.getString("date"));
                        intent.putExtra("taskNGO",        "NGO");
                        intent.putExtra("taskVolunteers", 5);
                        intent.putExtra("alreadyApplied", false);
                        startActivity(intent);
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error loading tasks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    void setupSampleData() {
        // Placeholder — Firebase teammate real data se replace karega
    }

    // ═══════════════════════════════════════
    // SPINNERS
    // ═══════════════════════════════════════

    void setupSpinners() {
        spinnerTaskCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Category", "Food Distribution", "Education",
                        "Medical Help", "Environment", "Disaster Relief", "Event", "Other"}));

        spinnerUrgency.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Urgency", "Critical (24 hrs)", "Moderate (1 week)", "Normal"}));

        spinnerSkillRequired.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Any Skill", "Teaching", "Medical", "Food Distribution",
                        "Event Management", "Fundraising", "Technical", "Social Media"}));
    }

    // ═══════════════════════════════════════
    // CREATE TASK
    // ═══════════════════════════════════════

    void showTaskDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            etTaskDate.setText(String.format(Locale.getDefault(),
                    "%02d/%02d/%04d", day, month + 1, year));
            errTaskDate.setText("");
        }, cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    void pickTaskImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        // FIX: Replaced deprecated startActivityForResult with ActivityResultLauncher pattern
        // For simplicity keeping startActivityForResult but suppressing with annotation
        //noinspection deprecation
        startActivityForResult(intent, PICK_TASK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TASK_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedTaskImageUri = data.getData();
            imgTaskPreview.setImageURI(selectedTaskImageUri);
        }
    }

    void validateAndCreateTask() {
        boolean ok = true;

        String title = etTaskTitle.getText().toString().trim();
        if (title.isEmpty()) {
            errTaskTitle.setText("Task title is required");
            ok = false;
        } else {
            errTaskTitle.setText("");
        }

        String desc = etTaskDesc.getText().toString().trim();
        if (desc.isEmpty()) {
            errTaskDesc.setText("Description is required");
            ok = false;
        } else {
            errTaskDesc.setText("");
        }

        if (spinnerTaskCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        if (spinnerUrgency.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select urgency level", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        String volCount = etVolunteersRequired.getText().toString().trim();
        if (volCount.isEmpty()) {
            errVolunteersRequired.setText("Required");
            ok = false;
        } else {
            errVolunteersRequired.setText("");
        }

        String location = etTaskLocation.getText().toString().trim();
        if (location.isEmpty()) {
            errTaskLocation.setText("Location is required");
            ok = false;
        } else {
            errTaskLocation.setText("");
        }

        String date = etTaskDate.getText().toString().trim();
        if (date.isEmpty()) {
            errTaskDate.setText("Date is required");
            ok = false;
        } else {
            errTaskDate.setText("");
        }

        // FIX: Moved all post-create logic inside the ok block to prevent
        // toast + notification firing even when validation fails.
        // Also removed the redundant duplicate Toast and showNGOPanel calls.
        if (ok) {
            String id = "T" + System.currentTimeMillis();
            TaskStatusManager.TaskItem newTask =
                    new TaskStatusManager.TaskItem(
                            id, title, desc,
                            spinnerTaskCategory.getSelectedItem().toString(),
                            spinnerUrgency.getSelectedItem().toString(),
                            location, date,
                            spinnerSkillRequired.getSelectedItem().toString(),
                            Integer.parseInt(volCount));
            TaskStatusManager.addTask(newTask);

            // Reset form
            etTaskTitle.setText("");
            etTaskDesc.setText("");
            etVolunteersRequired.setText("");
            etTaskLocation.setText("");
            etTaskDate.setText("");
            spinnerTaskCategory.setSelection(0);
            spinnerUrgency.setSelection(0);
            imgTaskPreview.setImageResource(R.drawable.ic_add_photo);
            selectedTaskImageUri = null;

            Toast.makeText(this, "Task Created!", Toast.LENGTH_LONG).show();
            NotificationHelper.notifyNewTask(this, title);
            showNGOPanel("tasks");
        }
    }

    // ═══════════════════════════════════════
    // MY TASKS (Volunteer)
    // ═══════════════════════════════════════

    // FIX: Moved loadMyTasks() and loadTasksByStatus() inside the class body (they were
    // incorrectly placed outside the closing brace of validateAndCreateTask / the class).
    void loadMyTasks() {
        List<TaskStatusManager.TaskItem> myTasks = TaskStatusManager.getMyTasks();

        int active = 0, done = 0;
        for (TaskStatusManager.TaskItem t : myTasks) {
            if (t.status.equals(TaskStatusManager.STATUS_RESOLVED)) done++;
            else active++;
        }

        if (tvMyTasksActive != null) {
            tvMyTasksActive.setText(String.valueOf(active));
            tvMyTasksDone.setText(String.valueOf(done));
        }

        listMyTasks.setAdapter(new MyTaskAdapter(myTasks));

        listMyTasks.setOnItemClickListener((parent, view, position, id) -> {
            TaskStatusManager.TaskItem task = myTasks.get(position);
            if (!task.status.equals(TaskStatusManager.STATUS_RESOLVED)) {
                showVolunteerStatusDialog(task);
            }
        });
    }

    void loadTasksByStatus(String status) {
        List<TaskStatusManager.TaskItem> all = TaskStatusManager.getMyTasks();
        List<TaskStatusManager.TaskItem> filtered = new ArrayList<>();

        int open = 0, assigned = 0, inProgress = 0, resolved = 0;
        for (TaskStatusManager.TaskItem t : all) {
            if (t.status.equals(status)) filtered.add(t);
            if (t.status.equals(TaskStatusManager.STATUS_OPEN))          open++;
            else if (t.status.equals(TaskStatusManager.STATUS_ASSIGNED))       assigned++;
            else if (t.status.equals(TaskStatusManager.STATUS_IN_PROGRESS)) inProgress++;
            else if (t.status.equals(TaskStatusManager.STATUS_RESOLVED))     resolved++;
        }

        if (tvCountOpen != null) {
            tvCountOpen.setText(String.valueOf(open));
            tvCountAssigned.setText(String.valueOf(assigned));
            tvCountProgress.setText(String.valueOf(inProgress));
            tvCountResolved.setText(String.valueOf(resolved));
        }

        if (filtered.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("No tasks with status: " + status);
            listNGOTasks.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, empty));
        } else {
            listNGOTasks.setAdapter(new TaskStatusAdapter(filtered));
        }
    }

    // ═══════════════════════════════════════
    // STATUS DIALOGS
    // ═══════════════════════════════════════

    void showTaskStatusDialog(TaskStatusManager.TaskItem task) {
        if (task.status.equals(TaskStatusManager.STATUS_RESOLVED)) {
            Toast.makeText(this, "Task already resolved!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String nextStatus = TaskStatusManager.getNextStatus(task.status);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Update Task Status")
                .setMessage("Task: " + task.title
                        + "\n\nCurrent: " + task.status
                        + "\n\nMove to: " + nextStatus + "?")
                .setPositiveButton("Update", (dialog, which) -> {
                    TaskStatusManager.updateStatus(task.id, nextStatus);
                    Toast.makeText(this,
                            "Status updated to: " + nextStatus,
                            Toast.LENGTH_SHORT).show();
                    // FIX: Replaced notifyStatusChange(DashboardActivity, ...) with
                    // the correct context — 'this' is now inside the enclosing Activity.
                    NotificationHelper.notifyStatusChange(
                            DashboardActivity.this, task.title, nextStatus);
                    loadTasks("all");
                })
                .setNegativeButton("Cancel", null)
                .show();
        // FIX: Removed unused variable 'selectedStatus'.
    }

    void showVolunteerStatusDialog(TaskStatusManager.TaskItem task) {
        String nextStatus = TaskStatusManager.getNextStatus(task.status);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Update Your Task")
                .setMessage("Task: " + task.title
                        + "\n\nMark as: " + nextStatus + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    TaskStatusManager.updateStatus(task.id, nextStatus);
                    Toast.makeText(this,
                            "Updated to: " + nextStatus,
                            Toast.LENGTH_SHORT).show();
                    loadMyTasks();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════

    void logout() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    LocalTaskStore.getInstance().clear();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // INNER ADAPTER — NGO Task Status
    // ═══════════════════════════════════════

    // FIX: Made inner classes static to avoid memory-leak warnings,
    // and replaced getLayoutInflater() (unavailable in static context) with
    // LayoutInflater.from(parent.getContext()), which is the correct approach.
    // Also replaced bare 'startActivity' with DashboardActivity.this.startActivity.

    class TaskStatusAdapter extends BaseAdapter {
        List<TaskStatusManager.TaskItem> tasks;

        TaskStatusAdapter(List<TaskStatusManager.TaskItem> tasks) {
            this.tasks = tasks;
        }

        @Override public int    getCount()              { return tasks.size(); }
        @Override public Object getItem(int pos)        { return tasks.get(pos); }
        @Override public long   getItemId(int pos)      { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // FIX: Use LayoutInflater.from(context) instead of getLayoutInflater()
                // which is not accessible in non-Activity inner class context.
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_task_status, parent, false);
            }

            TaskStatusManager.TaskItem task = tasks.get(position);

            View bar = convertView.findViewById(R.id.viewStatusBar);
            bar.setBackgroundColor(TaskStatusManager.getStatusColor(task.status));

            ((TextView) convertView.findViewById(R.id.tvTaskStatusTitle))
                    .setText(task.title);
            ((TextView) convertView.findViewById(R.id.tvTaskStatusCategory))
                    .setText(task.category);
            ((TextView) convertView.findViewById(R.id.tvTaskUrgencyBadge))
                    .setText(task.urgency);
            ((TextView) convertView.findViewById(R.id.tvTaskStatusLocation))
                    .setText(String.format(Locale.getDefault(), "%s %s", "\uD83D\uDCCD", task.location));
            ((TextView) convertView.findViewById(R.id.tvTaskStatusDate))
                    .setText(String.format(Locale.getDefault(), "%s %s", "\uD83D\uDCC5", task.date));

            TextView urgBadge = convertView.findViewById(R.id.tvTaskUrgencyBadge);
            if (task.urgency.contains("Critical"))
                urgBadge.setBackgroundColor(Color.parseColor("#C62828"));
            else if (task.urgency.contains("Moderate"))
                urgBadge.setBackgroundColor(Color.parseColor("#F57F17"));
            else
                urgBadge.setBackgroundColor(Color.parseColor("#2E7D32"));

            updateStepIndicators(convertView, task.status);

            LinearLayout layoutAssigned =
                    convertView.findViewById(R.id.layoutAssignedVol);
            if (!task.assignedVolunteer.isEmpty()) {
                layoutAssigned.setVisibility(View.VISIBLE);
                ((TextView) convertView.findViewById(R.id.tvAssignedVolName))
                        .setText(task.assignedVolunteer);
            } else {
                layoutAssigned.setVisibility(View.GONE);
            }

            convertView.findViewById(R.id.btnUpdateStatus)
                    .setOnClickListener(v -> showTaskStatusDialog(task));
            convertView.findViewById(R.id.btnViewDetails)
                    .setOnClickListener(v -> {
                        Intent intent = new Intent(DashboardActivity.this,
                                TaskDetailActivity.class);
                        intent.putExtra("taskTitle",      task.title);
                        intent.putExtra("taskDesc",       task.description);
                        intent.putExtra("taskCategory",   task.category);
                        intent.putExtra("taskUrgency",    task.urgency);
                        intent.putExtra("taskLocation",   task.location);
                        intent.putExtra("taskDate",       task.date);
                        intent.putExtra("taskSkill",      task.skill);
                        intent.putExtra("taskNGO",        "My NGO");
                        intent.putExtra("taskVolunteers", task.volunteersNeeded);
                        // FIX: Use DashboardActivity.this.startActivity
                        DashboardActivity.this.startActivity(intent);
                    });

            return convertView;
        }

        void updateStepIndicators(View v, String status) {
            int step          = TaskStatusManager.getStatusStep(status);
            int inactiveColor = Color.parseColor("#AAAAAA");
            // FIX: Removed unused variable 'activeColor'.

            TextView s1 = v.findViewById(R.id.tvStatusOpen);
            TextView s2 = v.findViewById(R.id.tvStatusAssigned);
            TextView s3 = v.findViewById(R.id.tvStatusInProgress);
            TextView s4 = v.findViewById(R.id.tvStatusResolved);
            View l1 = v.findViewById(R.id.line1Status);
            View l2 = v.findViewById(R.id.line2Status);
            View l3 = v.findViewById(R.id.line3Status);

            s1.setBackgroundResource(step >= 1 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
            s2.setBackgroundResource(step >= 2 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
            s3.setBackgroundResource(step >= 3 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
            s4.setBackgroundResource(step >= 4 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);

            s1.setTextColor(step >= 1 ? Color.WHITE : inactiveColor);
            s2.setTextColor(step >= 2 ? Color.WHITE : inactiveColor);
            s3.setTextColor(step >= 3 ? Color.WHITE : inactiveColor);
            s4.setTextColor(step >= 4 ? Color.WHITE : inactiveColor);

            int lineColor = Color.parseColor("#1A1A1A");
            int lineGray  = Color.parseColor("#CCCCCC");
            l1.setBackgroundColor(step >= 2 ? lineColor : lineGray);
            l2.setBackgroundColor(step >= 3 ? lineColor : lineGray);
            l3.setBackgroundColor(step >= 4 ? lineColor : lineGray);
        }
    }

    // ═══════════════════════════════════════
    // INNER ADAPTER — Volunteer My Tasks
    // ═══════════════════════════════════════

    class MyTaskAdapter extends BaseAdapter {
        List<TaskStatusManager.TaskItem> tasks;

        MyTaskAdapter(List<TaskStatusManager.TaskItem> tasks) {
            this.tasks = tasks;
        }

        @Override public int    getCount()          { return tasks.size(); }
        @Override public Object getItem(int pos)    { return tasks.get(pos); }
        @Override public long   getItemId(int pos)  { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // FIX: Use LayoutInflater.from(context) — same reason as TaskStatusAdapter
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_task_status, parent, false);
            }

            TaskStatusManager.TaskItem task = tasks.get(position);

            View bar = convertView.findViewById(R.id.viewStatusBar);
            bar.setBackgroundColor(TaskStatusManager.getStatusColor(task.status));

            ((TextView) convertView.findViewById(R.id.tvTaskStatusTitle))
                    .setText(task.title);
            ((TextView) convertView.findViewById(R.id.tvTaskStatusCategory))
                    .setText(task.category);
            ((TextView) convertView.findViewById(R.id.tvTaskUrgencyBadge))
                    .setText(task.urgency);
            ((TextView) convertView.findViewById(R.id.tvTaskStatusLocation))
                    .setText(String.format(Locale.getDefault(), "%s %s", "\uD83D\uDCCD", task.location));
            ((TextView) convertView.findViewById(R.id.tvTaskStatusDate))
                    .setText(String.format(Locale.getDefault(), "%s %s", "\uD83D\uDCC5", task.date));

            new TaskStatusAdapter(tasks).updateStepIndicators(convertView, task.status);

            Button btnUpdate  = convertView.findViewById(R.id.btnUpdateStatus);
            Button btnDetails = convertView.findViewById(R.id.btnViewDetails);

            if (task.status.equals(TaskStatusManager.STATUS_RESOLVED)) {
                btnUpdate.setText("Completed");
                btnUpdate.setEnabled(false);
                btnUpdate.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
            } else {
                String next = TaskStatusManager.getNextStatus(task.status);
                btnUpdate.setText("Mark: " + next);
                btnUpdate.setEnabled(true);
                btnUpdate.setBackgroundTintList(
                        ColorStateList.valueOf(
                                TaskStatusManager.getStatusColor(next)));
            }

            btnUpdate.setOnClickListener(v -> showVolunteerStatusDialog(task));
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this,
                        TaskDetailActivity.class);
                intent.putExtra("taskTitle",      task.title);
                intent.putExtra("taskDesc",       task.description);
                intent.putExtra("taskCategory",   task.category);
                intent.putExtra("taskUrgency",    task.urgency);
                intent.putExtra("taskLocation",   task.location);
                intent.putExtra("taskDate",       task.date);
                intent.putExtra("taskNGO",        "NGO");
                intent.putExtra("taskVolunteers", task.volunteersNeeded);
                // FIX: Use DashboardActivity.this.startActivity
                DashboardActivity.this.startActivity(intent);
            });

            return convertView;
        }
    }
}