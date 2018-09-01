package io.bbqresearch.dsc.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import io.bbqresearch.dsc.repository.StatusRepository;

public class StatusViewModel extends AndroidViewModel {

    private StatusRepository mRepository;

    private LiveData<String> mDeviceStatus;

    public StatusViewModel(Application application) {
        super(application);
        mRepository = new StatusRepository(application);
        mDeviceStatus = mRepository.getDeviceStatus();
    }

    public LiveData<String> getDeviceStatus() {
        return mDeviceStatus;
    }

    public void enableDeviceStatus(boolean enable) {
        mRepository.enableDeviceStatus(enable);
    }

}