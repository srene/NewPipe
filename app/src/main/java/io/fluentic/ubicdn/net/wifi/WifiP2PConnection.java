package io.fluentic.ubicdn.net.wifi;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import io.fluentic.ubicdn.data.UbiCDNService;
import io.fluentic.ubicdn.util.G;


/**
 * Created by srenevic on 03/08/17.
 */
public class WifiP2PConnection {


    static final public int ConectionStateNONE = 0;
    static final public int ConectionStatePreConnecting = 1;
    static final public int ConectionStateConnecting = 2;
    static final public int ConectionStateConnected = 3;
    static final public int ConectionStateDisconnected = 4;

    private int  mConectionState = ConectionStateNONE;

    public static final String TAG = WifiP2PConnection.class.getName();

    private boolean hadConnection = false;

    private boolean m_isServiceConnected = false;
    private final Messenger m_clientMessenger = new Messenger(new WifiP2PConnection.ClientHandler());

    WifiP2PConnection that = this;
    WifiManager wifiManager = null;
    WifiConfiguration wifiConfig = null;
    Context context = null;
    int netId = 0;

    WifiDirectHotSpot mWifiDirectHotSpot;
    WifiDirectP2PServiceDiscovery mWifiDirectServiceDiscovery;

    WiFiConnectionReceiver receiver;

    private Messenger m_serviceMessenger = null;

    private IntentFilter filter;

    String inetAddress = "";

    boolean connected=false;

    WifiP2pManager p2pManager;

    public WifiP2PConnection(Context Context,Channel channel, WifiP2pDevice device, WifiDirectHotSpot mWifiDirectHotSpot, WifiDirectP2PServiceDiscovery mWifiDirectServiceDiscovery) {
        this.context = Context;

        this.mWifiDirectHotSpot = mWifiDirectHotSpot;
        this.mWifiDirectServiceDiscovery = mWifiDirectServiceDiscovery;
        filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //receiver = new WiFiConnectionReceiver();

        //this.context.registerReceiver(receiver, filter);
       // mWifiDirectServiceDiscovery.Stop();

        //WIFI connection
        /*this.wifiManager = (WifiManager)this.context.getSystemService(this.context.WIFI_SERVICE);

        G.Log(TAG,"New connection SSID:"+SSIS+" Pass:"+password);
        this.wifiConfig = new WifiConfiguration();
        this.wifiConfig.SSID = String.format("\"%s\"", SSIS);
        this.wifiConfig.preSharedKey = String.format("\"%s\"", password);
        this.wifiConfig.priority = 100000000;

     //   this.wifiManager.disconnect();
        this.wifiManager.getConnectionInfo();
        List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
        for(WifiConfiguration wifi: wifis)
        {
            this.wifiManager.disableNetwork(wifi.networkId);
        }

        this.netId = this.wifiManager.addNetwork(this.wifiConfig);
        //this.wifiManager.updateNetwork(this.wifiConfig);
        this.wifiManager.enableNetwork(this.netId, false);
        boolean success = this.wifiManager.reconnect();*/

        //WIFIDIRECT CONNECTION
        this.p2pManager =  (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
        Log.d(TAG, "connectP2p ");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.DISPLAY;
        //config.wps.
        //config.groupOwnerIntent = 15; // I want this device to become the owner
       /* if (serviceRequest != null)
            manager.removeServiceRequest(channel, serviceRequest,
                    new ActionListener() {

                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onFailure(int arg0) {
                        }
                    });
*/

        p2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG,"Connecting to service");
                connected = true;

            }

            @Override
            public void onFailure(int errorCode) {
                Log.d(TAG,"Failed connecting to service");
            }
        });

       // G.Log(TAG,"Connection success "+ success + " ssid:"+this.wifiConfig.SSID + " pass:"+this.wifiConfig.preSharedKey);
        bindService();

    }



    public void Stop(){
        G.Log(TAG,"Stop");
        if(connected){
           // this.context.unregisterReceiver(receiver);
            this.wifiManager.removeNetwork(this.netId);
            List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
            for(WifiConfiguration wifi: wifis)
            {
                this.wifiManager.enableNetwork(wifi.networkId, true);

            }

            this.wifiManager.disconnect();
            this.wifiManager.reconnect();
            unbindService();
            connected = false;
        }

    }

    public void SetInetAddress(String address){
        this.inetAddress = address;
    }

    public String GetInetAddress(){
        return this.inetAddress;
    }

    private class WiFiConnectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        //    G.Log(TAG,"Action: " + action);
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info != null) {

                    if (info.isConnected()) {
                        //hadConnection = true;
                        mConectionState = ConectionStateConnected;
                    }else if(info.isConnectedOrConnecting()) {
                        mConectionState = ConectionStateConnecting;
                    }else {
                        if(hadConnection){
                            mConectionState = ConectionStateDisconnected;
                        }else{
                            mConectionState = ConectionStatePreConnecting;
                        }
                    }

                    G.Log(TAG,"DetailedState: " + info.getDetailedState());
                    G.Log(TAG,"ConnectionState: " + mConectionState);

                    String conStatus = "";
                    if(mConectionState == WifiP2PConnection.ConectionStateNONE) {
                        conStatus = "NONE";
                    }else if(mConectionState == WifiP2PConnection.ConectionStatePreConnecting) {
                        conStatus = "PreConnecting";
                    }else if(mConectionState == WifiP2PConnection.ConectionStateConnecting) {
                        conStatus = "Connecting";
                    }else if(mConectionState == WifiP2PConnection.ConectionStateConnected) {
                        conStatus = "Connected";
                    }else if(mConectionState == WifiP2PConnection.ConectionStateDisconnected) {
                        conStatus = "Disconnected";
                        G.Log(TAG,"Had connection "+hadConnection);
                        //wifiManager.removeNetwork(netId);
                        mWifiDirectServiceDiscovery.Start();

                    }
                    G.Log(TAG, "Status " + conStatus);

                }

                WifiInfo wiffo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                Log.d(TAG,"Wifiinfo "+wiffo);
                if(wiffo!=null&&mConectionState==ConectionStateConnected){
                    Log.d(TAG,"SSID "+wiffo.getSSID() +" "+wifiConfig.BSSID + " "+ wifiConfig.SSID);


                    if(wiffo.getSSID().equals(wifiConfig.SSID)) {
                        G.Log(TAG, "Ip address: " + wiffo.getIpAddress());
                        G.Log(TAG, "Create face to " + inetAddress);
                        hadConnection=true;
                        G.Log(TAG, "Connected to " + wiffo.getSSID());

                        sendServiceMessage(UbiCDNService.UBICDN_SERVICE_WIFI_CONNECTED);

                    } else {
                        that.wifiManager.reconnect();
                    }

                }
            }
        }
    }

    /**
     * Convenience method to send a message to the UbiCDN Service
     * through a Messenger.
     *
     * @param message Message from a set of predefined UbiCDN Service messages.
     */
    private void
    sendServiceMessage(int message) {
        if (m_serviceMessenger == null) {
            G.Log("UbiCDN Service not yet connected");
            return;
        }
        try {
            Message msg = Message.obtain(null, message);
            msg.replyTo = m_clientMessenger;
            m_serviceMessenger.send(msg);
        } catch (RemoteException e) {
            // If Service crashes, nothing to do here
            G.Log("UbiCDN service Disconnected: " + e);
        }
    }

    /**
     * Method that binds the current activity to the UbiCDN Service.
     */
    private void
    bindService() {
        if (!m_isServiceConnected) {
            // Bind to Service
            context.bindService(new Intent(context, UbiCDNService.class),
                    m_ServiceConnection, Context.BIND_AUTO_CREATE);
            G.Log("ServiceFragment::bindUbiCDNService()");
        }
    }

    /**
     * Method that unbinds the current activity from the UbiCDN Service.
     */
    private void
    unbindService() {
        if (m_isServiceConnected) {
            // Unbind from Service
            context.unbindService(m_ServiceConnection);
            m_isServiceConnected = false;

            G.Log("ServiceFragment::unbindUbiCDNService()");
        }
    }

    /**
     * Client Message Handler.
     *
     * This handler is used to handle messages that are being sent back
     * from the UbiCDN Service to the current application.
     */
    private class ClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UbiCDNService.UBICDN_SERVICE_DISCONNECT:
                    G.Log("UbiCDNService:Disconnect");

                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    /**
     * Client ServiceConnection to UbiCDN Service.
     */
    private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
        @Override
        public void
        onServiceConnected(ComponentName className, IBinder service) {
            // Establish Messenger to the Service
            m_serviceMessenger = new Messenger(service);
            m_isServiceConnected = true; // onServiceConnected runs on the main thread

            // Check if UbiCDN  Service is running

            G.Log("m_ServiceConnection::onServiceConnected()");
        }

        @Override
        public void
        onServiceDisconnected(ComponentName componentName) {
            // In event of unexpected disconnection with the Service; Not expecting to get here.
            G.Log("m_ServiceConnection::onServiceDisconnected()");
            m_isServiceConnected = true; // onServiceConnected runs on the main thread

        }
    };
}
