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
        authState.postValue(Resource.loading(false));
        String error = authRepository.signIn(email, password);
        postResult(error);
    }

    public void register(String email, String password) {
        authState.postValue(Resource.loading(false));
        String error = authRepository.register(email, password);
        postResult(error);
    }

    public void continueAsGuest() {
        authState.postValue(Resource.loading(false));
        authRepository.signInAsGuest();
        postResult(null);
    }

    private void postResult(String error) {
        if (error == null) {
            authState.postValue(Resource.success(true));
        } else {
            authState.postValue(Resource.error(error, false));
        }
    }
}
