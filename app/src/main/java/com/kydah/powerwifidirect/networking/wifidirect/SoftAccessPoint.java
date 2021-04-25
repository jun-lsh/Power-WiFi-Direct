package com.kydah.powerwifidirect.networking.wifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
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

import com.kydah.powerwifidirect.networking.model.AccessPointData;
import com.kydah.powerwifidirect.networking.model.LegacyPeer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class SoftAccessPoint implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    private static final String serviceType = "_presence._tcp";
    private static final int maxServiceCalls = 6;

    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel wifiP2pChannel = null;
    private WifiP2pManager.PeerListListener peerListListener = null;
    private String thisSSID, thisPassphrase, thisDeviceID, thisInet, thisInstanceName;
    private String groupOwnerID = "";
    private boolean groupOwner = false;
    private boolean joinedAsLC = false;
    private boolean joinedAsGM = false;

    private int serviceDiscoveryCalls = 3;

    private HashMap<String, LegacyPeer> validPeers = new HashMap<>();

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

    private final Runnable serviceRunnable = new Runnable() {
        @Override
        public void run() {
            registerService(thisInstanceName, true);
        }
    };

    public SoftAccessPoint(Context context, int portNumber) {
        this.portNumber = portNumber;
        this.context = context;
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        broadcastReceiver = new WifiDirectBroadcastReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public void startAP() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);


        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Collection<WifiP2pDevice> peersDeviceList = peers.getDeviceList();
                for (WifiP2pDevice device : peersDeviceList) {
                    //System.out.println(peer.deviceAddress);
                    System.out.println("Detected: " + device.deviceName + " " + device.deviceAddress);
                    if (!groupOwner && !joinedAsLC) {
                        if (validPeers.get(device.deviceAddress) != null) {
                            // node will turn into a GM/LC with this operation
                            WifiP2pConfig config = new WifiP2pConfig();
                            System.out.println("Related service has been determined! Attempting connection to GO.");
                            config.deviceAddress = device.deviceAddress;
                            joinedAsLC = true;
                            //unregisterService();
                            //destroyGroup();
                            terminateAP();
//                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                                        return;
//                                    }
//                                    wifiP2pManager.connect(wifiP2pChannel, config, new ActionListener() {
//                                        @Override
//                                        public void onSuccess() {
//                                            System.out.println("Successful connection!");
//                                            stopServiceDiscovery();
//                                            stopPeerDiscovery();
//                                            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("INIT_AS_GM"));
//                                        }
//
//                                        @Override
//                                        public void onFailure(int reason) {
//                                            System.out.println("Unsuccessful connection! " + reason);
//                                        }
//                                    });
//                                }
//                            }, 8000);

                            Intent intent = new Intent("INIT_AS_LC");
                            intent.putExtra("groupOwner", validPeers.get(device.deviceAddress));
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }
                    }
                }
            }
        };
        //startServiceDiscovery();
        WifiDirectAutoAccept proxy = new WifiDirectAutoAccept(context, wifiP2pManager, wifiP2pChannel);
        proxy.intercept(true);
        createGroup();
    }


    public void terminateAP() {
        stopServiceDiscovery();
        unregisterService();
        stopPeerDiscovery();
        destroyGroup();
        wifiP2pManager = null;
    }

    public void createGroup() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(context, context.getMainLooper(), null);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //wifiP2pManager.removeGroup(wifiP2pChannel, null);
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
//                if (attempts > 3) {
//                    localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_STALLED"));
//                    return;
//                } else {
//                    timeout(2500);
//                    createGroup(attempts + 1);
//                }

                localBroadcastManager.sendBroadcast(new Intent("GROUP_CREATION_UNSUCCESSFUL"));
            }
        });
    }

    private void destroyGroup() {
        System.out.println("Removed group!");
        if (wifiP2pManager != null) wifiP2pManager.removeGroup(wifiP2pChannel, null);
    }

    private void registerService(String instanceName, boolean runnable) {
        if (wifiP2pManager == null) return;
        wifiP2pManager.clearLocalServices(wifiP2pChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                System.out.println("Successfully cleared services, spawning new one...");
                HashMap<String, String> record = new HashMap<>();
                record.put("device_id", thisDeviceID);
                record.put("port_number", String.valueOf(portNumber));
                record.put("ip_address", thisInet);
                record.put("passphrase", thisPassphrase);
                record.put("instance_name", thisSSID);

                WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, serviceType, record);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (wifiP2pManager != null)
                    wifiP2pManager.addLocalService(wifiP2pChannel, serviceInfo, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            System.out.println("Service creation successful!");
                            //       timeout(500);
                            localBroadcastManager.sendBroadcast(new Intent("SERVICE_CREATION_SUCCESSFUL"));
                            if (!runnable) startServiceDiscovery();
                            new Handler(Looper.getMainLooper()).postDelayed(serviceRunnable, 60000);
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
        System.out.println("Unregistered service!");
        if (wifiP2pManager != null) wifiP2pManager.clearLocalServices(wifiP2pChannel, null);
    }

    private void startServiceDiscovery() {
        serviceDiscoveryCalls++;
        System.out.println("Calling for service discovery: " + serviceDiscoveryCalls);
        if(serviceDiscoveryCalls == maxServiceCalls){
            if(validPeers.isEmpty()){
                System.out.println("No services detected, will init as a GO");
                groupOwner = true;
                localBroadcastManager.sendBroadcast(new Intent("INIT_AS_GO"));
            }
        }
        if (wifiP2pManager == null) return;
        if (wifiP2pManager != null)
            wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, new WifiP2pManager.DnsSdServiceResponseListener() {
                //
//            private boolean prelimInfoFound = false;
//            private boolean recordInfoFound = false;

                private final Intent broadcastIntent = new Intent("SERVICE_SEARCH_PEER_INFO");

                @Override
                public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                    WifiP2pDevice wifiDirectDevice) {
                    System.out.println("onDnsSdServiceAvailable: instanceName:" + instanceName); //+ ", registrationType: " + registrationType
                    //+ ", WifiP2pDevice: " + wifiDirectDevice.toString());
//                broadcastIntent.putExtra("INSTANCE_NAME", instanceName);
//                localBroadcastManager.sendBroadcast(broadcastIntent);
//                if(!prelimInfoFound) {
//                }
//                if(recordInfoFound) localBroadcastManager.sendBroadcast(broadcastIntent);
                }
            }, new WifiP2pManager.DnsSdTxtRecordListener() {
                private final Intent broadcastIntent = new Intent("SERVICE_SEARCH_PEER_INFO");

                @Override
                public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                    System.out.println("onDnsSdTxtRecordAvailable: fullDomain: " + fullDomain); //+ ", record: " + record.toString()
//                        + ", WifiP2pDevice: " + device.toString());
                    //broadcastIntent.putExtra("INSTANCE_NAME", fullDomain);

                    AccessPointData accessPointData = new AccessPointData((String) record.get("instance_name"),(String) record.get("passphrase"), (String) record.get("ip_address"));
                    LegacyPeer peer = new LegacyPeer(device.deviceAddress, (String) record.get("port_number"), accessPointData);

                    validPeers.put(device.deviceAddress, peer);
                    broadcastIntent.putExtra("PEER_OBJ", peer);
                    localBroadcastManager.sendBroadcast(broadcastIntent);

                }
            });

        if (wifiP2pManager != null)
            wifiP2pManager.addServiceRequest(wifiP2pChannel, WifiP2pDnsSdServiceRequest.newInstance(), new ActionListener() {
                @Override
                public void onSuccess() {
                    //  timeout(500);
                    System.out.println("addServiceRequest.onSuccess() for requests of type: DnsSdServiceRequest");
                    localBroadcastManager.sendBroadcast(new Intent("SERVICE_REQUEST_ADDED_SUCCESSFULLY"));
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (wifiP2pManager != null)
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
                                        if (wifiP2pManager != null)
                                            wifiP2pManager.discoverServices(wifiP2pChannel, new ActionListener() {
                                                @Override
                                                public void onSuccess() {
                                                    timeout(500);
                                                    System.out.println("Successfully started service discovery!");
                                                    localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_ADDED_SUCCESSFULLY"));
                                                    new Handler(Looper.getMainLooper()).postDelayed(discoveryRunnable, 5000);
                                                }

                                                @Override
                                                public void onFailure(int reason) {
                                                    System.out.println("Unsuccessfully started service discovery!");
                                                    localBroadcastManager.sendBroadcast(new Intent("SERVICE_DISCOVERY_ADDED_UNSUCCESSFULLY"));
                                                    startServiceDiscovery();
                                                }
                                            });
                                    }
                                }, 1000);
                            }

                            @Override
                            public void onFailure(int reason) {
                                System.out.println("Failed to start peer discovery! " + reason);
                                localBroadcastManager.sendBroadcast(new Intent("PEER_DISCOVERY_ADDED_UNSUCCESSFULLY"));
                                startServiceDiscovery();
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

    private void stopServiceDiscovery() {
        System.out.println("Cleared service requests!");
        if (wifiP2pManager != null) wifiP2pManager.clearServiceRequests(wifiP2pChannel, null);
    }

    private void stopPeerDiscovery() {
        System.out.println("Cleared peer discovery!");
        if (wifiP2pManager != null) wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, null);
    }

    public void stopDiscovery(){
        stopServiceDiscovery();
        stopPeerDiscovery();
        wifiP2pManager = null;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (info != null) {
            if (info.isGroupOwner) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                thisInet = info.groupOwnerAddress.getHostAddress().toString();
                if(wifiP2pManager != null) wifiP2pManager.requestGroupInfo(wifiP2pChannel, this);
            }
        }
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group != null && group.isGroupOwner()) {
            thisSSID = group.getNetworkName();
            thisPassphrase = group.getPassphrase();
            thisDeviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            //putting all the information here as im worried about delay between recordInfo and prelimInfo
            System.out.println("Spawning service with parameters: " + thisSSID + " " + thisPassphrase + " " + thisInet);
            thisInstanceName = thisSSID + "/-/" + thisPassphrase + "/-/" + portNumber + "/-/" + thisDeviceID + "/-/" + thisInet;
            registerService(thisInstanceName, false);
        }
    }

    private void timeout(int duration) {
        try {
            TimeUnit.MICROSECONDS.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getSSID() {
        return thisSSID;
    }


    class WifiDirectBroadcastReceiver extends BroadcastReceiver {

        private final WifiP2pManager.ConnectionInfoListener that;

        WifiDirectBroadcastReceiver(WifiP2pManager.ConnectionInfoListener that) {
            this.that = that;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (wifiP2pManager != null) {
                if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
                    wifiP2pManager.requestConnectionInfo(wifiP2pChannel, that);
                } else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
                    wifiP2pManager.requestConnectionInfo(wifiP2pChannel, that);
                } else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    wifiP2pManager.requestPeers(wifiP2pChannel, peerListListener);
            }
            }
        }
    }

}

