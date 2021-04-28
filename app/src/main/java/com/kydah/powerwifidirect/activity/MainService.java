package com.kydah.powerwifidirect.activity;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class MainService extends Service {

    private final IBinder binder = new LocalBinder();
    private MainServiceCallbacks mainServiceCallbacks;

    public class LocalBinder extends Binder {
        MainService getService(){
            return MainService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mainServiceCallbacks.clearNetworking();
    }

    public void setCallbacks(MainServiceCallbacks mainServiceCallbacks){
        this.mainServiceCallbacks = mainServiceCallbacks;
    }
}
