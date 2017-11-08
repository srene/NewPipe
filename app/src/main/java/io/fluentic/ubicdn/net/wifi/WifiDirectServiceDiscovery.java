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
import android.os.Build;
import android.os.Handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.fluentic.ubicdn.data.UbiCDNService;
import io.fluentic.ubicdn.net.Discovery;
import io.fluentic.ubicdn.net.Link;
import io.fluentic.ubicdn.net.LinkListener;
import io.fluentic.ubicdn.util.G;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;

/**
 * Created by srenevic on 03/08/17.
 */
public class WifiDirectServiceDiscovery implements Discovery, WifiP2pManager.ChannelListener{


    Context context;

    WifiDirectServiceDiscovery that = this;
    //WifiDirectHotSpot mWifiDirectHotSpot;
    LinkListener listener;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener;
    private WifiP2pManager.DnsSdTxtRecordListener txtListener;
    private WifiP2pManager.PeerListListener peerListListener;

    public static final String TAG = WifiDirectServiceDiscovery.class.getName();

    boolean isRunning;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;

    public WifiDirectServiceDiscovery(Context Context, /*WifiDirectHotSpot mWifiDirectHotSpot, */LinkListener listener) {
        this.context = Context;
       // this.mWifiDirectHotSpot = mWifiDirectHotSpot;
        this.listener = listener;
        p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);


    }

    public void start(boolean source, int id) {

        if (!isRunning()) {
            isRunning = true;
            final boolean s = source;
            final int identifier = id;
            G.Log(TAG, "Service discovery start");
            //mWifiLink = null;
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

                G.Log("Device name "+getHostName(""));
                if(source)SetPeerName("source");
                else SetPeerName("client");
                G.Log("Device name "+getHostName(""));

                serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

                    public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                        G.Log("Instance name:" + instanceName + " Service type:" + serviceType + " Device:" + device.deviceName);
                        if (serviceType.startsWith(UbiCDNService.SERVICE_TYPE)) {

                            String[] separated = instanceName.split(":");
                         //   G.Log(TAG, "found SSID:" + separated[1] + ", pwd:" + separated[2] + "IP: " + separated[3]);
                         //   listener.btLinkDidReceiveFrame(null,instanceName.getBytes());

                        } else {
                            G.Log(TAG, "Not our Service, :" + UbiCDNService.SERVICE_TYPE + "!=" + serviceType + ":");
                        }

                        //startPeerDiscovery();
                    }
                };


                txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
                    @Override
                        /* Callback includes:
                         * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
                         * record: TXT record dta as a map of key/value pairs.
                         * device: The device running the advertised service.
                         */

                    public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
                        G.Log(TAG, "DnsSdTxtRecord available -" + record.get("available") + " " +record.get("filter"));
                        final HashMap<String, String> buddies = new HashMap<String, String>();
                     //   buddies.put("available", record.get("available").toString());
                    }
                };

                p2p.setDnsSdResponseListeners(channel, serviceListener, txtListener);

                peerListListener = new WifiP2pManager.PeerListListener() {

                    public void onPeersAvailable(WifiP2pDeviceList peers) {

                        final WifiP2pDeviceList pers = peers;
                        int numm = 0;
                        G.Log(TAG, "onPeersAvailable");
                        for (WifiP2pDevice peer : pers.getDeviceList()) {
                            numm++;
                            G.Log(TAG, "found " + numm + ": " + peer.deviceName + " " + peer.deviceAddress);
                            listener.peerDiscovered(peer.deviceName,peer.deviceAddress);

                        }

                        if (numm > 0) {
                            startServiceDiscovery();
                        } else {
                            startPeerDiscovery();
                        }
                    }
                };

            //    if(mWifiDirectHotSpot==null) {



             //   }

                startPeerDiscovery();
            }
        } else {
            G.Log(TAG,"Service already running");
        }
    }


    public void stop() {
        if(isRunning){
            isRunning = false;
            this.context.unregisterReceiver(receiver);
            myServiceState = ServiceState.NONE;
            //cancelAll();
            stopDiscovery();
            stopPeerDiscovery();
        }

    }

    /*public void Disconnect(){
        if(mWifiLink !=null) mWifiLink.Stop();
    }*/

    @Override
    public void onChannelDisconnected() {
        //
       // btLinkDisconnected();
    }

    @Override
    public void transportLinkConnected(Link link){

    }

    @Override
    public void transportLinkDisconnected(Link link){

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

        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance();
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
           // G.Log(TAG, "Action "+action);

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                } else {

                }
            }else if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    p2p.requestPeers(channel, peerListListener);
                }
             /*}else if(WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device2 = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE);
                G.Log(TAG,"Device name "+device2.deviceName);
                //addText("Local device: " + MyP2PHelper.deviceToString(device));*/
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
                //mWifiLink = null;
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

    public static String getHostName(String defValue) {
        try {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, "net.hostname").toString();
        } catch (Exception ex) {
            return defValue;
        }
    }

    public void SetPeerName(String name)
    {
        G.Log(TAG,"Set name "+name);
        try {


            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = p2p.getClass().getMethod(
                    "setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);

            Object arglist[] = new Object[3];
            arglist[0] = channel;
            arglist[1] = name;
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    G.Log(TAG, "setDeviceName succeeded");
                }

                @Override
                public void onFailure(int reason) {
                    G.Log(TAG, "setDeviceName failed");
                }
            };
            setDeviceName.invoke(p2p, arglist);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning(){
        return isRunning;
    }
}
