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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Role
    String userRole = "NGO"; // "NGO" or "Volunteer" — Firebase teammate set karega

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

    // Sample data
    List<String> taskList = new ArrayList<>();
    List<String> volunteerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

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
        btnNavStats       = findViewById(R.id.btnNavStats);
        btnNavTasks       = findViewById(R.id.btnNavTasks);
        btnNavCreate      = findViewById(R.id.btnNavCreate);
        btnNavVolunteers  = findViewById(R.id.btnNavVolunteers);
        btnNavAvailable   = findViewById(R.id.btnNavAvailable);
        btnNavApplications= findViewById(R.id.btnNavApplications);
        btnNavProfile     = findViewById(R.id.btnNavProfile);
        btnNavImpact      = findViewById(R.id.btnNavImpact);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ── Stats ──
        tvTotalTasks      = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks  = findViewById(R.id.tvCompletedTasks);
        tvUrgentTasks     = findViewById(R.id.tvUrgentTasks);
        tvTotalVolunteers = findViewById(R.id.tvTotalVolunteers);

        // ── Create Task ──
        etTaskTitle            = findViewById(R.id.etTaskTitle);
        etTaskDesc             = findViewById(R.id.etTaskDesc);
        etVolunteersRequired   = findViewById(R.id.etVolunteersRequired);
        etTaskLocation         = findViewById(R.id.etTaskLocation);
        etTaskDate             = findViewById(R.id.etTaskDate);
        errTaskTitle           = findViewById(R.id.errTaskTitle);
        errTaskDesc            = findViewById(R.id.errTaskDesc);
        errVolunteersRequired  = findViewById(R.id.errVolunteersRequired);
        errTaskLocation        = findViewById(R.id.errTaskLocation);
        errTaskDate            = findViewById(R.id.errTaskDate);
        spinnerTaskCategory    = findViewById(R.id.spinnerTaskCategory);
        spinnerUrgency         = findViewById(R.id.spinnerUrgency);
        spinnerSkillRequired   = findViewById(R.id.spinnerSkillRequired);
        imgTaskPreview         = findViewById(R.id.imgTaskPreview);

        // ── Lists ──
        listNGOTasks       = findViewById(R.id.listNGOTasks);
        listVolunteers     = findViewById(R.id.listVolunteers);
        listAvailableTasks = findViewById(R.id.listAvailableTasks);

        // ── Profile ──
        imgVolProfile      = findViewById(R.id.imgVolProfile);
        tvProfileName      = findViewById(R.id.tvProfileName);
        tvProfileCity      = findViewById(R.id.tvProfileCity);
        tvProfileEmail     = findViewById(R.id.tvProfileEmail);
        tvProfileSkills    = findViewById(R.id.tvProfileSkills);
        tvImpactScore      = findViewById(R.id.tvImpactScore);
        tvTasksDone        = findViewById(R.id.tvTasksDone);
        tvHoursContributed = findViewById(R.id.tvHoursContributed);

        // ── Setup ──
        setupSpinners();
        setupSampleData();

        // ── Listeners ──
        etTaskDate.setOnClickListener(v -> showTaskDatePicker());
        findViewById(R.id.btnUploadTaskImage).setOnClickListener(v -> pickTaskImage());
        findViewById(R.id.btnCreateTask).setOnClickListener(v -> validateAndCreateTask());
        findViewById(R.id.btnQuickCreateTask).setOnClickListener(v -> showNGOPanel("create"));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Filter buttons
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> loadTasks("all"));
        findViewById(R.id.btnFilterUrgent).setOnClickListener(v -> loadTasks("urgent"));
        findViewById(R.id.btnFilterActive).setOnClickListener(v -> loadTasks("active"));

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


        // ── Role decide karo ──
        // Firebase teammate yahan role set karega from Firestore
        // Abhi ke liye Intent se role lo
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

        // LocalTaskStore se tasks show
        List<LocalTaskStore.LocalTask> savedTasks =
                LocalTaskStore.getInstance().getTasks();

        if (!savedTasks.isEmpty()) {
            //  existing TaskAdapter / RecyclerView use
            Toast.makeText(this,
                    savedTasks.size() + " community tasks available!",
                    Toast.LENGTH_SHORT).show();
        }
    }

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
                "✅ Form auto-filled by AI! Review and submit.",
                Toast.LENGTH_LONG).show();
    }

    void setupDashboard() {
        if (userRole.equals("NGO")) {
            tvWelcome.setText("NGO Dashboard");
            // Show NGO bottom nav
            btnNavStats.setVisibility(View.VISIBLE);
            btnNavTasks.setVisibility(View.VISIBLE);
            btnNavCreate.setVisibility(View.VISIBLE);
            btnNavVolunteers.setVisibility(View.VISIBLE);
            // Smart Match button NGO Stats panel mein
            findViewById(R.id.btnNavStats).setOnLongClickListener(v -> {
                openSmartMatchNGO();
                return true;
            });
            showNGOPanel("stats");
        } else {
            tvWelcome.setText("Volunteer Dashboard");
            // Show Volunteer bottom nav
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
    void openSmartMatchNGO() {
        Intent intent = new Intent(this, SmartMatchActivity.class);
        intent.putExtra("mode",         "NGO");
        intent.putExtra("taskTitle",    "Food Distribution Drive");
        intent.putExtra("taskSkill",    "Food Distribution");
        intent.putExtra("taskLocation", "Raipur, CG");
        intent.putExtra("taskUrgency",  "🔴 Critical (24 hrs)");
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
        btn.setTextColor(active ? Color.parseColor("#1A1A1A") : Color.parseColor("#888888"));
    }

    // ═══════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════

    void loadStats() {
        // Firebase teammate yahan Firestore se data fetch karega
        tvTotalTasks.setText(String.valueOf(taskList.size()));
        tvCompletedTasks.setText("0");
        tvUrgentTasks.setText("0");
        tvTotalVolunteers.setText(String.valueOf(volunteerList.size()));
    }

    void loadTasks(String filter) {
        // Sample data — Firebase teammate real data load karega
        List<String> filtered = new ArrayList<>();
        for (String t : taskList) {
            if (filter.equals("all") || t.toLowerCase().contains(filter)) {
                filtered.add(t);
            }
        }
        if (filtered.isEmpty()) filtered.add("No tasks found. Create your first task!");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, filtered);
        listNGOTasks.setAdapter(adapter);

        listNGOTasks.setOnItemClickListener((parent, view, position, id) -> {
            String selected = filtered.get(position);
            String[] parts = selected.split("\\|");

            // NGO ke liye → Applicants Screen
            Intent intent = new Intent(this, ApplicantsActivity.class);
            intent.putExtra("taskTitle", parts.length > 0 ? parts[0].trim() : "Task");
            startActivity(intent);
        });
    }

    void loadVolunteers() {
        // Firebase teammate yahan Firestore se volunteers fetch karega
        if (volunteerList.isEmpty()) volunteerList.add("No volunteers registered yet.");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, volunteerList);
        listVolunteers.setAdapter(adapter);
    }

    void loadAvailableTasks() {
        // Sample tasks — Firebase teammate real data load karega
        List<String> available = new ArrayList<>();
        available.add("Food Distribution Drive | 🔴 Critical | Raipur");
        available.add("Tree Plantation | 🟢 Normal | Bilaspur");
        available.add("Medical Camp | 🟡 Moderate | Durg");

        if (taskList.isEmpty()) taskList.addAll(available);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, available);
        listAvailableTasks.setAdapter(adapter);

        // Click → Task Detail
        listAvailableTasks.setOnItemClickListener((parent, view, position, id) -> {
            String selected = available.get(position);
            String[] parts = selected.split("\\|");

            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra("taskTitle",      parts[0].trim());
            intent.putExtra("taskUrgency",    parts.length > 1 ? parts[1].trim() : "Normal");
            intent.putExtra("taskLocation",   parts.length > 2 ? parts[2].trim() : "Raipur");
            intent.putExtra("taskDesc",       "Help the community by participating in this important task. Your contribution will make a real difference!");
            intent.putExtra("taskCategory",   "Community Service");
            intent.putExtra("taskSkill",      "Any Skill");
            intent.putExtra("taskDate",       "20/04/2026");
            intent.putExtra("taskNGO",        "Green Earth Foundation");
            intent.putExtra("taskVolunteers", 10);
            intent.putExtra("alreadyApplied", false);
            startActivity(intent);
        });
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
                new String[]{"Select Category","Food Distribution","Education",
                        "Medical Help","Environment","Disaster Relief","Event","Other"}));

        spinnerUrgency.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Urgency","🔴 Critical (24 hrs)","🟡 Moderate (1 week)","🟢 Normal"}));

        spinnerSkillRequired.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Any Skill","Teaching","Medical","Food Distribution",
                        "Event Management","Fundraising","Technical","Social Media"}));
    }

    // ═══════════════════════════════════════
    // CREATE TASK
    // ═══════════════════════════════════════

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
        if (title.isEmpty()) { errTaskTitle.setText("Task title is required"); ok = false; }
        else errTaskTitle.setText("");

        String desc = etTaskDesc.getText().toString().trim();
        if (desc.isEmpty()) { errTaskDesc.setText("Description is required"); ok = false; }
        else errTaskDesc.setText("");

        if (spinnerTaskCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show(); ok = false; }

        if (spinnerUrgency.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select urgency level", Toast.LENGTH_SHORT).show(); ok = false; }

        String volCount = etVolunteersRequired.getText().toString().trim();
        if (volCount.isEmpty()) { errVolunteersRequired.setText("Required"); ok = false; }
        else errVolunteersRequired.setText("");

        String location = etTaskLocation.getText().toString().trim();
        if (location.isEmpty()) { errTaskLocation.setText("Location is required"); ok = false; }
        else errTaskLocation.setText("");

        String date = etTaskDate.getText().toString().trim();
        if (date.isEmpty()) { errTaskDate.setText("Date is required"); ok = false; }
        else errTaskDate.setText("");

        if (ok) {
            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> task = new HashMap<>();
            task.put("ngoId", userId);
            task.put("title", etTaskTitle.getText().toString().trim());
            task.put("description", etTaskDesc.getText().toString().trim());
            task.put("category", spinnerTaskCategory.getSelectedItem().toString());
            task.put("urgency", spinnerUrgency.getSelectedItem().toString());
            task.put("skill", spinnerSkillRequired.getSelectedItem().toString());
            task.put("volunteersRequired", etVolunteersRequired.getText().toString().trim());
            task.put("location", etTaskLocation.getText().toString().trim());
            task.put("date", etTaskDate.getText().toString().trim());
            task.put("status", "Active");
            task.put("timestamp", System.currentTimeMillis());

            db.collection("tasks")
                    .add(task)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Task Created Successfully! ✅", Toast.LENGTH_LONG).show();

                        // Clear form
                        etTaskTitle.setText("");
                        etTaskDesc.setText("");
                        etVolunteersRequired.setText("");
                        etTaskLocation.setText("");
                        etTaskDate.setText("");

                        showNGOPanel("tasks");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ═══════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════

    void logout() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}