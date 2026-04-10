package com.example.smartseva;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;

public class RegisterActivity extends AppCompatActivity {

    Button btnTabNGO, btnTabVolunteer;
    LinearLayout layoutNGO, layoutVolunteer;

    EditText etOrgName, etRegNo, etNGOEmail, etNGOPhone, etNGOAddress, etNGOPassword, etNGOConfirmPassword;
    TextView errOrgName, errRegNo, errNGOEmail, errNGOPhone, errNGOAddress, errNGOPassword, errNGOConfirmPassword;
    Spinner spinnerNGOType;

    EditText etFirstName, etLastName, etVolEmail, etVolPhone, etIDNumber, etVolPassword, etVolConfirmPassword;
    TextView errFirstName, errLastName, errVolEmail, errVolPhone, errIDNumber, errVolPassword, errVolConfirmPassword;
    Spinner spinnerIDType;

    ImageView imgPassportPhoto;
    TextView errPassportPhoto;
    Uri selectedPhotoUri = null;
    private static final int PICK_IMAGE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnTabNGO       = findViewById(R.id.btnTabNGO);
        btnTabVolunteer = findViewById(R.id.btnTabVolunteer);
        layoutNGO       = findViewById(R.id.layoutNGO);
        layoutVolunteer = findViewById(R.id.layoutVolunteer);

        etOrgName             = findViewById(R.id.etOrgName);
        etRegNo               = findViewById(R.id.etRegNo);
        etNGOEmail            = findViewById(R.id.etNGOEmail);
        etNGOPhone            = findViewById(R.id.etNGOPhone);
        etNGOAddress          = findViewById(R.id.etNGOAddress);
        etNGOPassword         = findViewById(R.id.etNGOPassword);
        etNGOConfirmPassword  = findViewById(R.id.etNGOConfirmPassword);
        errOrgName            = findViewById(R.id.errOrgName);
        errRegNo              = findViewById(R.id.errRegNo);
        errNGOEmail           = findViewById(R.id.errNGOEmail);
        errNGOPhone           = findViewById(R.id.errNGOPhone);
        errNGOAddress         = findViewById(R.id.errNGOAddress);
        errNGOPassword        = findViewById(R.id.errNGOPassword);
        errNGOConfirmPassword = findViewById(R.id.errNGOConfirmPassword);
        spinnerNGOType        = findViewById(R.id.spinnerNGOType);

        etFirstName           = findViewById(R.id.etFirstName);
        etLastName            = findViewById(R.id.etLastName);
        etVolEmail            = findViewById(R.id.etVolEmail);
        etVolPhone            = findViewById(R.id.etVolPhone);
        etIDNumber            = findViewById(R.id.etIDNumber);
        etVolPassword         = findViewById(R.id.etVolPassword);
        etVolConfirmPassword  = findViewById(R.id.etVolConfirmPassword);
        errFirstName          = findViewById(R.id.errFirstName);
        errLastName           = findViewById(R.id.errLastName);
        errVolEmail           = findViewById(R.id.errVolEmail);
        errVolPhone           = findViewById(R.id.errVolPhone);
        errIDNumber           = findViewById(R.id.errIDNumber);
        errVolPassword        = findViewById(R.id.errVolPassword);
        errVolConfirmPassword = findViewById(R.id.errVolConfirmPassword);
        spinnerIDType         = findViewById(R.id.spinnerIDType);
        imgPassportPhoto  = findViewById(R.id.imgPassportPhoto);
        errPassportPhoto  = findViewById(R.id.errPassportPhoto);

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> openImagePicker());

        ArrayAdapter<String> ngoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select type","Trust","Society","Section 8 Company","Charitable Organisation","Other"});
        spinnerNGOType.setAdapter(ngoAdapter);

        ArrayAdapter<String> idAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select ID","Aadhaar Card","PAN Card","Passport","Voter ID","Driving Licence"});
        spinnerIDType.setAdapter(idAdapter);

        imgPassportPhoto = findViewById(R.id.imgPassportPhoto);
        errPassportPhoto = findViewById(R.id.errPassportPhoto);

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> openImagePicker());

        btnTabNGO.setOnClickListener(v -> showNGO());
        btnTabVolunteer.setOnClickListener(v -> showVolunteer());

        findViewById(R.id.btnRegisterNGO).setOnClickListener(v -> validateAndRegisterNGO());
        findViewById(R.id.btnRegisterVolunteer).setOnClickListener(v -> validateAndRegisterVolunteer());
    }

    void showNGO() {
        layoutNGO.setVisibility(View.VISIBLE);
        layoutVolunteer.setVisibility(View.GONE);
        btnTabNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        btnTabNGO.setTextColor(Color.WHITE);
        btnTabVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        btnTabVolunteer.setTextColor(Color.parseColor("#1A1A1A"));
    }

    void showVolunteer() {
        layoutNGO.setVisibility(View.GONE);
        layoutVolunteer.setVisibility(View.VISIBLE);
        btnTabVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        btnTabVolunteer.setTextColor(Color.WHITE);
        btnTabNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        btnTabNGO.setTextColor(Color.parseColor("#1A1A1A"));
    }

    void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            selectedPhotoUri = data.getData();

            try {
                android.database.Cursor cursor = getContentResolver().query(
                        selectedPhotoUri, null, null, null, null);
                int sizeIndex = cursor.getColumnIndex(
                        android.provider.OpenableColumns.SIZE);
                cursor.moveToFirst();
                long fileSize = cursor.getLong(sizeIndex);
                cursor.close();

                if (fileSize > 2 * 1024 * 1024) {
                    Toast.makeText(this, "Photo size must be less than 2MB",
                            Toast.LENGTH_SHORT).show();
                    selectedPhotoUri = null;
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            imgPassportPhoto.setImageURI(selectedPhotoUri);
            errPassportPhoto.setText("");
        }
    }

    boolean checkEmpty(EditText et, TextView err, String msg) {
        if (et.getText().toString().trim().isEmpty()) { err.setText(msg); return false; }
        err.setText(""); return true;
    }

    boolean checkEmail(EditText et, TextView err) {
        String v = et.getText().toString().trim();
        if (v.isEmpty()) { err.setText("Email is required"); return false; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches()) {
            err.setText("Enter a valid email address"); return false; }
        err.setText(""); return true;
    }

    boolean checkPhone(EditText et, TextView err) {
        String v = et.getText().toString().trim();
        if (v.length() != 10) { err.setText("Phone must be exactly 10 digits"); return false; }
        if (!v.matches("[6-9][0-9]{9}")) { err.setText("Enter a valid Indian mobile number"); return false; }
        err.setText(""); return true;
    }

    boolean checkPassword(EditText et, TextView err) {
        String v = et.getText().toString();
        if (v.length() < 8) { err.setText("Min 8 characters required"); return false; }
        if (!v.matches(".*\\d.*")) { err.setText("Include at least one number"); return false; }
        err.setText(""); return true;
    }

    boolean checkConfirm(EditText pass, EditText confirm, TextView err) {
        if (!pass.getText().toString().equals(confirm.getText().toString())) {
            err.setText("Passwords do not match"); return false; }
        err.setText(""); return true;
    }

    void validateAndRegisterNGO() {
        boolean ok = true;
        if (!checkEmpty(etOrgName, errOrgName, "Organisation name is required")) ok = false;
        if (!checkEmpty(etRegNo, errRegNo, "Registration number is required")) ok = false;
        if (spinnerNGOType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select NGO type", Toast.LENGTH_SHORT).show(); ok = false; }
        if (!checkEmail(etNGOEmail, errNGOEmail)) ok = false;
        if (!checkPhone(etNGOPhone, errNGOPhone)) ok = false;
        if (!checkEmpty(etNGOAddress, errNGOAddress, "Address is required")) ok = false;
        if (!checkPassword(etNGOPassword, errNGOPassword)) ok = false;
        if (!checkConfirm(etNGOPassword, etNGOConfirmPassword, errNGOConfirmPassword)) ok = false;

        if (ok) {
            // Firebase teammate yahan connect karega
            Toast.makeText(this, "NGO Registered Successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    void validateAndRegisterVolunteer() {
        boolean ok = true;
        if (selectedPhotoUri == null) {
            errPassportPhoto.setText("Please upload your passport photo");
            ok = false;
        }
        if (!checkEmpty(etFirstName, errFirstName, "First name is required")) ok = false;
        if (!checkEmpty(etLastName, errLastName, "Last name is required")) ok = false;
        if (!checkEmail(etVolEmail, errVolEmail)) ok = false;
        if (!checkPhone(etVolPhone, errVolPhone)) ok = false;
        if (spinnerIDType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select ID type", Toast.LENGTH_SHORT).show(); ok = false; }
        if (!checkEmpty(etIDNumber, errIDNumber, "ID number is required")) ok = false;
        if (!checkPassword(etVolPassword, errVolPassword)) ok = false;
        if (!checkConfirm(etVolPassword, etVolConfirmPassword, errVolConfirmPassword)) ok = false;

        if (ok) {

            Toast.makeText(this, "Volunteer Registered Successfully!", Toast.LENGTH_SHORT).show();
        }
    }

}