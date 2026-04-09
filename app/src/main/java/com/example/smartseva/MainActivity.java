package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnVolunteer, btnOrg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnVolunteer = findViewById(R.id.btnVolunteer);
        btnOrg = findViewById(R.id.btnOrg);

        btnVolunteer.setOnClickListener(v -> openRegister("volunteer"));
<<<<<<< HEAD
        btnOrg.setOnClickListener(v -> openRegister("organization"));
=======
        btnOrg.setOnClickListener(v -> openRegister("ngo"));
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
    }

    private void openRegister(String role) {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
<<<<<<< HEAD
        intent.putExtra("role", role);
        startActivity(intent);
    }
}
=======
        intent.putExtra("type", role); // IMPORTANT: use "type"
        startActivity(intent);
    }
}

>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
