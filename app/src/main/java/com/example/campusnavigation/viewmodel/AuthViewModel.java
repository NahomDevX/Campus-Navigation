package com.example.campusnavigation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusnavigation.CampusNavigationApp;
import com.example.campusnavigation.data.repository.AuthRepository;
import com.example.campusnavigation.util.Resource;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository = CampusNavigationApp.getInstance().getAuthRepository();
    private final MutableLiveData<Resource<Boolean>> authState = new MutableLiveData<>();

    public LiveData<Resource<Boolean>> getAuthState() {
        return authState;
    }

    public void signIn(String email, String password) {
        authState.setValue(Resource.loading(false));
        authRepository.signIn(email, password)
                .addOnSuccessListener(result -> authState.setValue(Resource.success(true)))
                .addOnFailureListener(error -> authState.setValue(Resource.error(error.getMessage(), false)));
    }

    public void register(String email, String password) {
        authState.setValue(Resource.loading(false));
        authRepository.register(email, password)
                .addOnSuccessListener(result -> authState.setValue(Resource.success(true)))
                .addOnFailureListener(error -> authState.setValue(Resource.error(error.getMessage(), false)));
    }

    public void continueAsGuest() {
        authState.setValue(Resource.loading(false));
        authRepository.signInAnonymously()
                .addOnSuccessListener(result -> authState.setValue(Resource.success(true)))
                .addOnFailureListener(error -> authState.setValue(Resource.error(error.getMessage(), false)));
    }
}
