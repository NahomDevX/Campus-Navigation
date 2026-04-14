package com.example.campusnavigation.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class AuthActivity extends AppCompatActivity {
    private AuthViewModel viewModel;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        progressBar = findViewById(R.id.authProgress);
        MaterialButton signInButton = findViewById(R.id.signInButton);
        MaterialButton registerButton = findViewById(R.id.registerButton);
        MaterialButton guestButton = findViewById(R.id.guestButton);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        signInButton.setOnClickListener(v -> attemptSignIn());
        registerButton.setOnClickListener(v -> attemptRegister());
        guestButton.setOnClickListener(v -> viewModel.continueAsGuest());
        viewModel.getAuthState().observe(this, state -> {
            progressBar.setVisibility(state.getStatus() == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (state.getStatus() == Resource.Status.SUCCESS && Boolean.TRUE.equals(state.getData())) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (state.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptSignIn() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.enter_email_and_password, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.signIn(email, password);
    }

    private void attemptRegister() {
        String email = valueOf(emailInput);
        String password = valueOf(passwordInput);
        String confirm = valueOf(confirmPasswordInput);
        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.register(email, password);
    }

    private String valueOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
