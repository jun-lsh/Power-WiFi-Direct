package com.kydah.powerwifidirect.networking.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thanosfisherman.wifiutils.WifiUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SoftAccessPoint implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    private static final String serviceType = "_presence._tcp";
    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel wifiP2pChannel = null;
    private String thisSSID, thisPassphrase, thisDeviceID, thisInet;

    private int portNumber;

    private final LocalBroadcastManager localBroadcastManager;
    private final Context context;
    private final BroadcastReceiver broadcastReceiver;

    private final Runnable discoveryRunnable = new Runnable() {
        @Override
        public void run() {
            startServiceDiscovery();
        }
    };

    public SoftAccessPoint(Context context, int portNumber) {
        this.portNumber = portNumber;
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        broadcastReceiver = new WifiDirectBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public void startAP() {
        createGroup(0);
    }


    public void terminateAP() {
        stopServiceDiscovery();
        unregisterService();
        destroyGroup();
        wifiP2pManager = null;
    }

    private void createGroup(int attempts) {
        wifiP2pManager.removeGroup(wifiP2pChannel, null);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        wifiP2pManager.createGroup(wifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
        //        timeout(500);
                System.out.println("Group creation was successful!");
                localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_SUCCESSFUL"));
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Group creation was unsuccessful! Trying again... " + reason);
                if (attempts > 3) {
                    localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_STALLED"));
                    return;
                } else {
                    WifiUtils.withContext(context).disableWifi();
                    WifiUtils.withContext(context).enableWifi();
                    createGroup(attempts + 1);
                }

                localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_UNSUCCESSFUL"));
            }
        });
    }

    private void destroyGroup() {
        wifiP2pManager.removeGroup(wifiP2pChannel, null);
    }

    private void registerService(String instanceName) {
        wifiP2pManager.clearLocalServices(wifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Successfully cleared services, spawning new one...");
                HashMap<String, String> record = new HashMap<>();
                record.put("device_id", thisDeviceID);
                record.put("port_number", String.valueOf(portNumber));
                record.put("ip_address", thisInet);
                WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, serviceType, record);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                wifiP2pManager.addLocalService(wifiP2pChannel, serviceInfo, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        System.out.println("Service creation successful!");
                 //       timeout(500);
                        localBroadcastManager.sendBroadcast(new Intent("SERVICE_CREATION_SUCCESSFUL"));
                        startServiceDiscovery();
                    }

                    @Override
                    public void onFailure(int reason) {
                        System.out.println("Service creation failed... sus... " + reason);
                        localBroadcastManager.sendBroadcast(new Intent("SERVICE_CREATION_UNSUCCESSFUL"));
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Service clearing failed... amogus... " + reason);
//                localBroadcastManager.sendBroadcast(new Intent("SERVICE_CREATION_UNSUCCESSFUL"));
            }
        });
    }

    private void unregisterService() {
        wifiP2pManager.clearLocalServices(wifiP2pChannel, null);
    }

    private void startServiceDiscovery() {
        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, new WifiP2pManager.DnsSdServiceResponseListener() {
            //
//            private boolean prelimInfoFound = false;
//            private boolean recordInfoFound = false;
            private final Intent broadcastIntent = new Intent("SERVICE_SEARCH_PEER_INFO");

            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                WifiP2pDevice wifiDirectDevice) {
                System.out.println("onDnsSdServiceAvailable: instanceName:" + instanceName + ", registrationType: " + registrationType
                        + ", WifiP2pDevice: " + wifiDirectDevice.toString());
                broadcastIntent.putExtra("INSTANCE_NAME", instanceName);
                localBroadcastManager.sendBroadcast(broadcastIntent);
//                if(!prelimInfoFound) {
//                }
//                if(recordInfoFound) localBroadcastManager.sendBroadcast(broadcastIntent);
            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                System.out.println("onDnsSdTxtRecordAvailable: fullDomain: " + fullDomain + ", record: " + record.toString()
                        + ", WifiP2pDevice: " + device.toString());
            }
        });
        wifiP2pManager.clearLocalServices(wifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Successfully cleared local services!");
                localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_CLEARED_SUCCESSFULLY"));
                wifiP2pManager.addServiceRequest(wifiP2pChannel, WifiP2pDnsSdServiceRequest.newInstance(), new ActionListener() {
                    @Override
                    public void onSuccess() {
                      //  timeout(500);
                        System.out.println("addServiceRequest.onSuccess() for requests of type: DnsSdServiceRequest");
                        localBroadcastManager.sendBroadcast(new Intent("SERVICE_REQUEST_ADDED_SUCCESSFULLY"));
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        wifiP2pManager.discoverPeers(wifiP2pChannel, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                System.out.println("Successfully started peer discovery!");
                                localBroadcastManager.sendBroadcast(new Intent("PEER_DISCOVERY_ADDED_SUCCESSFULLY"));
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            return;
                                        }
                                        wifiP2pManager.discoverServices(wifiP2pChannel, new ActionListener() {
                                            @Override
                                            public void onSuccess() {
                                                timeout(500);
                                                System.out.println("Successfully started service discovery!");
                                                localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_ADDED_SUCCESSFULLY"));
                                                new Handler(Looper.getMainLooper()).postDelayed(discoveryRunnable, 20000);
                                            }

                                            @Override
                                            public void onFailure(int reason) {
                                                System.out.println("Unsuccessfully started service discovery!");
                                                localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_ADDED_UNSUCCESSFULLY"));
                                            }
                                        });
                                    }
                                }, 1000);
                            }

                            @Override
                            public void onFailure(int reason) {
                                System.out.println("Failed to start peer discovery! " + reason);
                                localBroadcastManager.sendBroadcast(new Intent("PEER_DISCOVERY_ADDED_UNSUCCESSFULLY"));
                            }
                        });
                    }

                    @Override
                    public void onFailure(int code) {
                        System.out.println("addServiceRequest.onFailure: " + code);
                        localBroadcastManager.sendBroadcast(new Intent("SERVICE_REQUEST_ADDED_UNSUCCESSFULLY"));
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                System.out.println("Unsuccessfully cleared local services! " + reason);
                localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_CLEARED_UNSUCCESSFULLY"));
            }
        });

    }

    private void stopServiceDiscovery(){
        wifiP2pManager.clearServiceRequests(wifiP2pChannel, null);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if(info != null){
            if(info.isGroupOwner){
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            thisInet = info.groupOwnerAddress.getHostAddress().toString();
            wifiP2pManager.requestGroupInfo(wifiP2pChannel, this);
            }
        }
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if(group != null && group.isGroupOwner()){
            thisSSID = group.getNetworkName();
            thisPassphrase = group.getPassphrase();
            thisDeviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            //putting all the information here as im worried about delay between recordInfo and prelimInfo
            System.out.println("Spawning service with parameters: " + thisSSID + " " + thisPassphrase);
            registerService(thisSSID + "/-/" + thisDeviceID + "/-/" + portNumber + "/-/" + thisPassphrase);
        }
    }

    private void timeout(int duration){
        try {
            TimeUnit.MICROSECONDS.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    class WifiDirectBroadcastReceiver extends BroadcastReceiver {

        private final WifiP2pManager.ConnectionInfoListener that;

        WifiDirectBroadcastReceiver(WifiP2pManager.ConnectionInfoListener that){
            this.that = that;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)){
                wifiP2pManager.requestConnectionInfo(wifiP2pChannel, that);
            } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)){
                wifiP2pManager.requestConnectionInfo(wifiP2pChannel, that);
            }
        }
    }

}

