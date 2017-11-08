package io.fluentic.ubicdn.net.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;

import io.fluentic.ubicdn.data.UbiCDNService;
import io.fluentic.ubicdn.util.G;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION;

/**
 * Created by srenevic on 03/08/17.
 */
public class WifiDirectP2PServiceDiscovery implements WifiP2pManager.ChannelListener{


    Context context;

    WifiDirectP2PServiceDiscovery that = this;
    WifiDirectHotSpot mWifiDirectHotSpot;
    WifiP2PConnection mWifiConnection;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener;
    private WifiP2pManager.PeerListListener peerListListener;

    public static final String TAG = WifiDirectP2PServiceDiscovery.class.getName();

    boolean isRunning;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;


    public WifiDirectP2PServiceDiscovery(Context Context, WifiDirectHotSpot mWifiDirectHotSpot, WifiP2PConnection mWifiConnection) {
        this.context = Context;
        this.mWifiDirectHotSpot = mWifiDirectHotSpot;
        this.mWifiConnection = mWifiConnection;
        p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);


    }


    public void Start() {

        if (!isRunning()) {
            isRunning = true;
            G.Log(TAG, "Service discovery start");
            mWifiConnection = null;
            if (p2p == null) {
                G.Log(TAG, "This device does not support Wi-Fi Direct");
            } else {

                channel = p2p.initialize(this.context, this.context.getMainLooper(), this);

                receiver = new ServiceSearcherReceiver();
                filter = new IntentFilter();
                filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
                filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
                filter.addAction(WIFI_P2P_DISCOVERY_CHANGED_ACTION);
                filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

                this.context.registerReceiver(receiver, filter);

                peerListListener = new WifiP2pManager.PeerListListener() {

                    public void onPeersAvailable(WifiP2pDeviceList peers) {

                        final WifiP2pDeviceList pers = peers;
                        int numm = 0;
                        for (WifiP2pDevice peer : pers.getDeviceList()) {
                            numm++;
                            G.Log(TAG, "found " + numm + ": " + peer.deviceName + " " + peer.deviceAddress);

                        }

                        if (numm > 0) {
                            startServiceDiscovery();
                        } else {
                            startPeerDiscovery();
                        }
                    }
                };

                serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

                    public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                        G.Log("Instance name:" + instanceName + " Service type:" + serviceType + " Device:" + device.deviceName);
                        if (serviceType.startsWith(UbiCDNService.SERVICE_TYPE)) {

                            String[] separated = instanceName.split(":");
                            G.Log(TAG, "found SSID:" + separated[1] + ", pwd:" + separated[2] + "IP: " + separated[3]);

                            if (mWifiConnection == null) {
                                if (mWifiDirectHotSpot != null) {
                                    G.Log(TAG, "Stop Access Point");
                                    mWifiDirectHotSpot.Stop();
                                    mWifiDirectHotSpot = null;
                                }

                                final String networkSSID = separated[1];
                                final String networkPass = separated[2];
                                final String ipAddress = separated[3];
                                G.Log(TAG, "New Connection");

                                mWifiConnection = new WifiP2PConnection(context, channel, device, mWifiDirectHotSpot, that);
                                mWifiConnection.SetInetAddress(ipAddress);
                            }

                        } else {
                            G.Log(TAG, "Not our Service, :" + UbiCDNService.SERVICE_TYPE + "!=" + serviceType + ":");
                        }

                        //startPeerDiscovery();
                    }
                };

                p2p.setDnsSdResponseListeners(channel, serviceListener, null);
                startPeerDiscovery();
            }
        }
    }


    public void Stop() {
        isRunning = false;
        this.context.unregisterReceiver(receiver);
        myServiceState = ServiceState.NONE;
        //cancelAll();
        stopDiscovery();
        stopPeerDiscovery();
    }

    public void Disconnect(){
        if(mWifiConnection!=null)mWifiConnection.Stop();
    }

    @Override
    public void onChannelDisconnected() {
        //
    }

    private void cancelAll()
    {
       /* p2p.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cancel Connect success");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Cancel Connect failed, error code " + reason);
            }
        });*/

        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cancel Local Services Success");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Cancel Local Services failed, error " + reason);
            }
        });

        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cancel Service Requests success");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Cancel Service Requests failed, error code " + reason);
            }
        });
    }
    private void startPeerDiscovery() {
        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                G.Log(TAG,"Started peer discovery");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Starting peer discovery failed, error code " + reason);
            }
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Stopped peer discovery");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Stopping peer discovery failed, error code " + reason);
            }
        });
    }

    private void startServiceDiscovery() {


        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(UbiCDNService.SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            public void onSuccess() {
                G.Log(TAG,"Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                G.Log(TAG,"Started service discovery");
                                myServiceState = ServiceState.DiscoverService;
                            }
                            public void onFailure(int reason) {
                                G.Log(TAG,"Starting service discovery failed, error code " + reason);
                            }
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                G.Log(TAG,"Adding service request failed, error code \" + reason");
                // No point starting service discovery
            }
        });

    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cleared service requests");
            }
            public void onFailure(int reason) {
                G.Log(TAG,"Clearing service requests failed, error code " + reason);
            }
        });
    }

    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                } else {

                }
            }else if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                //WifiP2pDevice device = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);
                //addText("Local device: " + MyP2PHelper.deviceToString(device));
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    persTatu = persTatu + "Stopped.";
                    startPeerDiscovery();
                }else if(state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED){
                    persTatu = persTatu + "Started.";
                }else{
                    persTatu = persTatu + "unknown  " + state;
                }
                G.Log(TAG,persTatu);

            } else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    G.Log(TAG,"Connected");
                    startPeerDiscovery();
                } else{
                    G.Log(TAG,"Disconnected");
                    startPeerDiscovery();
                }
            }
        }
    }

    public boolean isRunning(){
        return isRunning;
    }
}
