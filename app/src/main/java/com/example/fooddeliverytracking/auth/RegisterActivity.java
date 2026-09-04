package com.example.fooddeliverytracking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.models.User;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private RadioGroup roleGroup;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressIndicator;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        nameInput = findViewById(R.id.editTextName);
        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        roleGroup = findViewById(R.id.radioGroupRole);
        registerButton = findViewById(R.id.buttonRegister);
        progressIndicator = findViewById(R.id.progressRegister);

        registerButton.setOnClickListener(view -> register());
        ((TextView) findViewById(R.id.textLogin)).setOnClickListener(view -> finish());
    }

    private void register() {
        String name = valueOf(nameInput);
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        if (TextUtils.isEmpty(name)) {
            nameInput.setError(getString(R.string.field_required, getString(R.string.full_name)));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.field_required, getString(R.string.email)));
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.password_too_short));
            return;
        }

        String role = roleGroup.getCheckedRadioButtonId() == R.id.radioDriver
                ? Constants.ROLE_DRIVER : Constants.ROLE_CUSTOMER;
        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            FirebaseUser firebaseUser = task.isSuccessful() && task.getResult() != null
                    ? task.getResult().getUser() : null;
            if (firebaseUser == null) {
                setLoading(false);
                String message = task.getException() == null ? "" : task.getException().getMessage();
                Toast.makeText(this, getString(R.string.auth_failed, message), Toast.LENGTH_LONG).show();
                return;
            }

            User user = new User(firebaseUser.getUid(), name, email, role, System.currentTimeMillis());
            FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(firebaseUser.getUid())
                    .setValue(user).addOnCompleteListener(profileTask -> {
                        if (profileTask.isSuccessful()) {
                            String activityName = Constants.ROLE_DRIVER.equals(role)
                                    ? ".driver.DriverDashboardActivity" : ".customer.CustomerHomeActivity";
                            Intent intent = new Intent();
                            intent.setClassName(this, getPackageName() + activityName);
                            startActivity(intent);
                            finishAffinity();
                        } else {
                            setLoading(false);
                            Toast.makeText(this, R.string.profile_save_failed, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
