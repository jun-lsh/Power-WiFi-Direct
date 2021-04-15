package com.kydah.powerwifidirect.networking.wifidirect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SoftAccessPoint {

    private static final String serviceType = "_presence._tcp";
    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel wifiP2pChannel = null;
    private LocalBroadcastManager localBroadcastManager;
    private final Context context;


    public SoftAccessPoint(Context context) {
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
    }

    public void createGroup() {
        wifiP2pManager.removeGroup(wifiP2pChannel, null);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        wifiP2pManager.createGroup(wifiP2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Group creation was successful!");
                localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_SUCCESSFUL"));
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Group creation was unsuccessful! Trying again...");
                localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_UNSUCCESSFUL"));
            }
        });
    }

}
