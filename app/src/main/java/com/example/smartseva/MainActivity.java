package com.example.smartseva;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnLoginNGO, btnLoginVolunteer, btnLogin;
    EditText etLoginEmail, etLoginPassword;
    TextView errLoginEmail, errLoginPassword, tvForgotPassword, tvGoToRegister;

    String selectedRole = "NGO"; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLoginNGO       = findViewById(R.id.btnLoginNGO);
        btnLoginVolunteer = findViewById(R.id.btnLoginVolunteer);
        btnLogin          = findViewById(R.id.btnLogin);
        etLoginEmail      = findViewById(R.id.etLoginEmail);
        etLoginPassword   = findViewById(R.id.etLoginPassword);
        errLoginEmail     = findViewById(R.id.errLoginEmail);
        errLoginPassword  = findViewById(R.id.errLoginPassword);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);
        tvGoToRegister    = findViewById(R.id.tvGoToRegister);

        // Tab toggle
        btnLoginNGO.setOnClickListener(v -> setRole("NGO"));
        btnLoginVolunteer.setOnClickListener(v -> setRole("Volunteer"));

        // Login button
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Go to Register
        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
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

        if (ok) {
            // Firebase teammate yahan login verify karega
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
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
                Toast.makeText(this,
                        "Please enter a valid email",
                        Toast.LENGTH_SHORT).show();
            } else {
                // Firebase teammate yahan resetPassword() call karega
                Toast.makeText(this,
                        "Reset link sent to " + resetEmail,
                        Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}