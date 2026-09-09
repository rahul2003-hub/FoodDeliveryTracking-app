package com.example.fooddeliverytracking.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliverytracking.R;
import com.example.fooddeliverytracking.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private CircularProgressIndicator progressIndicator;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        progressIndicator = findViewById(R.id.progressLogin);

        findViewById(R.id.buttonForgotPassword).setOnClickListener(view -> {
            String email = valueOf(emailInput);
            if (TextUtils.isEmpty(email)) {
                emailInput.setError(getString(R.string.field_required, getString(R.string.email)));
                return;
            }
            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task ->
                    Toast.makeText(this, task.isSuccessful() ? getString(R.string.reset_sent)
                            : getString(R.string.auth_failed, task.getException() == null ? "" : task.getException().getMessage()),
                            Toast.LENGTH_LONG).show());
        });
        loginButton.setOnClickListener(view -> login());
        ((TextView) findViewById(R.id.textCreateAccount)).setOnClickListener(view -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void login() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.field_required, getString(R.string.email)));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError(getString(R.string.field_required, getString(R.string.password)));
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                setLoading(false);
                String message = task.getException() == null ? "" : task.getException().getMessage();
                Toast.makeText(this, getString(R.string.auth_failed, message), Toast.LENGTH_LONG).show();
                return;
            }
            readRoleAndRoute(task.getResult().getUser().getUid());
        });
    }

    private void readRoleAndRoute(String uid) {
        FirebaseDatabase.getInstance().getReference(Constants.NODE_USERS).child(uid).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        String selectedRole = ((android.widget.RadioGroup) findViewById(R.id.radioGroupRole))
                                .getCheckedRadioButtonId() == R.id.radioDriver ? Constants.ROLE_DRIVER : Constants.ROLE_CUSTOMER;
                        if (role != null && !selectedRole.equals(role)) {
                            firebaseAuth.signOut();
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, R.string.role_mismatch, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (Constants.ROLE_DRIVER.equals(role)) {
                            openRoleScreen(".driver.DriverDashboardActivity");
                        } else if (Constants.ROLE_CUSTOMER.equals(role)) {
                            openRoleScreen(".customer.CustomerHomeActivity");
                        } else {
                            firebaseAuth.signOut();
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, R.string.profile_not_found, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, getString(R.string.auth_failed,
                                error.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openRoleScreen(String activityName) {
        Intent intent = new Intent();
        intent.setClassName(this, getPackageName() + activityName);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
