package com.example.smartseva;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    Button btnLoginNGO, btnLoginVolunteer, btnLogin;
    EditText etLoginEmail, etLoginPassword;
    TextView errLoginEmail, errLoginPassword, tvForgotPassword, tvGoToRegister;
    ProgressBar progressBar;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    String selectedRole = "NGO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM", "Token: " + token);
                        // Token automatically Firestore mein save hoga
                        // jab onNewToken() call hoga — manually chahiye to:
                        // new MyFirebaseMessagingService().saveFCMToken(token);
                    } else {
                        Log.w("FCM", "Token fetch failed", task.getException());
                    }
                });

        // Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        btnLoginNGO       = findViewById(R.id.btnLoginNGO);
        btnLoginVolunteer = findViewById(R.id.btnLoginVolunteer);
        btnLogin          = findViewById(R.id.btnLogin);
        etLoginEmail      = findViewById(R.id.etLoginEmail);
        etLoginPassword   = findViewById(R.id.etLoginPassword);
        errLoginEmail     = findViewById(R.id.errLoginEmail);
        errLoginPassword  = findViewById(R.id.errLoginPassword);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);
        tvGoToRegister    = findViewById(R.id.tvGoToRegister);
        progressBar       = findViewById(R.id.progressBar); // add in XML

        btnLoginNGO.setOnClickListener(v -> setRole("NGO"));
        btnLoginVolunteer.setOnClickListener(v -> setRole("Volunteer"));
        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Auto-login if already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkRoleAndNavigate(currentUser.getUid());
        }
    }

    void setRole(String role) {
        selectedRole = role;
        if (role.equals("NGO")) {
            btnLoginNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
            btnLoginNGO.setTextColor(Color.WHITE);
            btnLoginVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnLoginVolunteer.setTextColor(Color.parseColor("#1A1A1A"));
        } else {
            btnLoginVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
            btnLoginVolunteer.setTextColor(Color.WHITE);
            btnLoginNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnLoginNGO.setTextColor(Color.parseColor("#1A1A1A"));
        }
    }

    void attemptLogin() {
        String email    = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();
        boolean ok      = true;

        // Email validation
        if (email.isEmpty()) {
            errLoginEmail.setText("Email is required");
            ok = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errLoginEmail.setText("Enter a valid email address");
            ok = false;
        } else {
            errLoginEmail.setText("");
        }

        // Password validation
        if (password.isEmpty()) {
            errLoginPassword.setText("Password is required");
            ok = false;
        } else if (password.length() < 8) {
            errLoginPassword.setText("Password must be at least 8 characters");
            ok = false;
        } else {
            errLoginPassword.setText("");
        }

        if (!ok) return;

        setLoading(true);

        // Step 1: Sign in with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        // Step 2: Verify role in Firestore
                        verifyUserRole(uid);
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        // Show friendly messages
                        if (errorMsg.contains("password")) {
                            errLoginPassword.setText("Incorrect password");
                        } else if (errorMsg.contains("no user") || errorMsg.contains("identifier")) {
                            errLoginEmail.setText("No account found with this email");
                        } else {
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Checks Firestore to confirm the user exists in the correct role collection.
     * NGOs  → "ngo_users/{uid}"
     * Volunteers → "volunteer_users/{uid}"
     */
    void verifyUserRole(String uid) {

        // ✅ Check email verified first
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            setLoading(false);
            mAuth.signOut();
            new AlertDialog.Builder(this)
                    .setTitle("📧 Email Not Verified")
                    .setMessage("Please verify your email first.\n\nCheck your inbox and click the verification link sent to:\n" + user.getEmail())
                    .setPositiveButton("Resend Email", (dialog, which) -> {
                        // Sign in temporarily to resend
                        String email    = etLoginEmail.getText().toString().trim();
                        String password = etLoginPassword.getText().toString().trim();
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener(t -> {
                                    if (mAuth.getCurrentUser() != null) {
                                        mAuth.getCurrentUser().sendEmailVerification()
                                                .addOnSuccessListener(unused ->
                                                        Toast.makeText(this,
                                                                "Verification email resent! Check inbox. ✅",
                                                                Toast.LENGTH_LONG).show())
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(this,
                                                                "Failed to resend: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show());
                                    }
                                    mAuth.signOut();
                                });
                    })
                    .setNegativeButton("OK", null)
                    .show();
            return; // ← stops login
        }

        // ── rest of your existing code unchanged ──
        String collection      = selectedRole.equals("NGO") ? "ngo_users" : "volunteer_users";
        String wrongCollection = selectedRole.equals("NGO") ? "volunteer_users" : "ngo_users";

        db.collection(collection).document(uid).get()
                .addOnSuccessListener(document -> {
                    setLoading(false);
                    if (document.exists()) {
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.putExtra("role", selectedRole);
                        intent.putExtra("uid", uid);
                        startActivity(intent);
                        finish();
                    } else {
                        db.collection(wrongCollection).document(uid).get()
                                .addOnSuccessListener(doc2 -> {
                                    mAuth.signOut();
                                    if (doc2.exists()) {
                                        String actualRole = selectedRole.equals("NGO")
                                                ? "Volunteer" : "NGO";
                                        Toast.makeText(this,
                                                "This account is registered as " + actualRole
                                                        + ". Please select the correct role.",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this,
                                                "Account not found. Please register first.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    mAuth.signOut();
                                    Toast.makeText(this,
                                            "Verification failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    mAuth.signOut();
                    Toast.makeText(this,
                            "Error verifying account: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Called on auto-login — checks which collection the user belongs to
     * and navigates accordingly without needing role selection.
     */
    void checkRoleAndNavigate(String uid) {
        db.collection("ngo_users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.putExtra("role", "NGO");
                        intent.putExtra("uid", uid);
                        startActivity(intent);
                        finish();
                    } else {
                        db.collection("volunteer_users").document(uid).get()
                                .addOnSuccessListener(doc2 -> {
                                    if (doc2.exists()) {
                                        Intent intent = new Intent(this, DashboardActivity.class);
                                        intent.putExtra("role", "Volunteer");
                                        intent.putExtra("uid", uid);
                                        startActivity(intent);
                                        finish();
                                    }
                                    // else: orphan auth user, let them log in manually
                                });
                    }
                });
    }

    void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");
        builder.setMessage("Enter your registered email address:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("yourname@example.com");
        input.setPadding(40, 20, 40, 20);
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String resetEmail = input.getText().toString().trim();
            if (resetEmail.isEmpty() ||
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.sendPasswordResetEmail(resetEmail)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this,
                                        "Reset link sent to " + resetEmail,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this,
                                        "Error: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}