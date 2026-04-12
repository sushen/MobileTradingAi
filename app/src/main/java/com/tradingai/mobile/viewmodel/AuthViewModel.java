package com.tradingai.mobile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AuthViewModel extends ViewModel {
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<String> error() {
        return error;
    }

    public void startLoading() {
        loading.setValue(true);
    }

    public void stopLoading() {
        loading.setValue(false);
    }
}
