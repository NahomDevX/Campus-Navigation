package com.example.campusnavigation.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.campusnavigation.R;
import com.example.campusnavigation.ui.main.MainActivity;
import com.example.campusnavigation.util.Resource;
import com.example.campusnavigation.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class sAuthActivity extends AppCompatActivity {
    private static final int MIN_PASSWORD_LENGTH = 6;

    private AuthViewModel viewModel;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private ProgressBar progressBar;
    private MaterialButton signInButton;
    private MaterialButton registerButton;
    private MaterialButton guestButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        progressBar = findViewById(R.id.authProgress);
        signInButton = findViewById(R.id.signInButton);
        registerButton = findViewById(R.id.registerButton);
        guestButton = findViewById(R.id.guestButton);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        signInButton.setOnClickListener(v -> attemptSignIn());
        registerButton.setOnClickListener(v -> attemptRegister());
        guestButton.setOnClickListener(v -> viewModel.continueAsGuest());
        viewModel.getAuthState().observe(this, state -> {
            boolean loading = state.getStatus() == Resource.Status.LOADING;
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            setControlsEnabled(!loading);
            if (state.getStatus() == Resource.Status.SUCCESS && Boolean.TRUE.equals(state.getData())) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (state.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, state.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void attemptSignIn() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        if (!validateCredentials(email, password)) {
            return;
        }
        viewModel.signIn(email, password);
    }

    private void attemptRegister() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        String confirm = valueOf(confirmPasswordInput);
        if (!validateCredentials(email, password)) {
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.register(email, password);
    }

    private boolean validateCredentials(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.enter_email_and_password, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.invalid_email, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setControlsEnabled(boolean enabled) {
        signInButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        guestButton.setEnabled(enabled);
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
