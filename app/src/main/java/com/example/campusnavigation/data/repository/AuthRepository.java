package com.example.campusnavigation.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public Task<?> signIn(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<?> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    public Task<?> signInAnonymously() {
        return auth.signInAnonymously();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
    }
}
