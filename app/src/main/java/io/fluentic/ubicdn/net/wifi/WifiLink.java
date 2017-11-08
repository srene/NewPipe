package io.fluentic.ubicdn.net.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.util.List;

import io.fluentic.ubicdn.net.Link;
import io.fluentic.ubicdn.util.Config;
import io.fluentic.ubicdn.util.G;


/**
 * Created by srenevic on 03/08/17.
 */
public class WifiLink implements Link {


    static final public int ConectionStateNONE = 0;
    static final public int ConectionStatePreConnecting = 1;
    static final public int ConectionStateConnecting = 2;
    static final public int ConectionStateConnected = 3;
    static final public int ConectionStateDisconnected = 4;

    private int  mConectionState = ConectionStateNONE;

    public static final String TAG = WifiLink.class.getName();

    private boolean hadConnection = false;

    WifiLinkListener listener;
    WifiManager wifiManager = null;
    WifiConfiguration wifiConfig = null;
    Context context = null;
    int netId = 0;

    WiFiConnectionReceiver receiver;
    private IntentFilter filter;
    String inetAddress = "";
    boolean connected=false;
    String ssid;
    WifiLink that;


    // create a class member variable.
    WifiManager.WifiLock mWifiLock = null;
    // private boolean m_isServiceConnected = false;
  //  private final Messenger m_clientMessenger = new Messenger(new WifiLink.ClientHandler());


    //WifiDirectHotSpot mWifiDirectHotSpot;
    //WifiDirectServiceDiscovery mWifiDirectServiceDiscovery;


  //  private Messenger m_serviceMessenger = null;




    //WifiP2pManager p2pManager;

    public WifiLink(Context context)///*, WifiDirectHotSpot mWifiDirectHotSpot*/, WifiDirectServiceDiscovery mWifiDirectServiceDiscovery)
   {

        this.context = context;
        this.listener = (WifiLinkListener)context;
        //this.mWifiDirectHotSpot = mWifiDirectHotSpot;
        //this.mWifiDirectServiceDiscovery = mWifiDirectServiceDiscovery;
        filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        //mWifiDirectServiceDiscovery.stop();
        receiver = new WiFiConnectionReceiver();

        //WIFI connection
        this.wifiManager = (WifiManager)this.context.getSystemService(this.context.WIFI_SERVICE);

        that = this;
    //    this.wifiConfig.priority = 100000000;


       // bindService();

    }


    @Override
    public void connect(String SSID, String password){

        G.Log(TAG,"Connect "+connected+" "+mConectionState);
        if(!connected&&(mConectionState==ConectionStateNONE||mConectionState==ConectionStateDisconnected)) {
            G.Log(TAG, "New connection SSID:" + SSID + " Pass:" + password);
            this.wifiConfig = new WifiConfiguration();
            this.wifiConfig.SSID = String.format("\"%s\"", SSID);
            this.wifiConfig.preSharedKey = String.format("\"%s\"", password);
            //   this.wifiManager.disconnect();
            ssid = this.wifiManager.getConnectionInfo().getSSID();
            G.Log(TAG,"Connected to "+ssid);
            List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
            for (WifiConfiguration wifi : wifis) {
                boolean result=false;
                if (wifi.SSID.contains("DIRECT-"))
                    this.wifiManager.removeNetwork(wifi.networkId);
                else
                    result = this.wifiManager.disableNetwork(wifi.networkId);

                Log.i(TAG, "Network " + wifi.SSID+" "+result);

            }

            this.context.registerReceiver(receiver, filter);
            this.netId = this.wifiManager.addNetwork(this.wifiConfig);
            //this.wifiManager.updateNetwork(this.wifiConfig);
            this.wifiManager.enableNetwork(this.netId, false);
            boolean success = this.wifiManager.reconnect();

            //WIFIDIRECT CONNECTION
            //this.p2pManager =  (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
            G.Log(TAG, "Connection success " + success + " ssid:" + this.wifiConfig.SSID + " pass:" + this.wifiConfig.preSharedKey);

            connected = true;
            hadConnection=false;
            //connectionStarted = true;

            holdWifiLock();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    if(!hadConnection)disconnect();
                }
            }, Config.wifiConnectionWaitingTime);
        }
    }

    @Override
    public void disconnect(){
        releaseWifiLock();
        G.Log(TAG,"Disconnect");
        if(connected){
            this.context.unregisterReceiver(receiver);
            this.wifiManager.removeNetwork(this.netId);
            List<WifiConfiguration> wifis = this.wifiManager.getConfiguredNetworks();
            for(WifiConfiguration wifi: wifis)
            {
                boolean attempt = false;
                if(wifi.SSID.equals(ssid))attempt=true;
                boolean result = this.wifiManager.enableNetwork(wifi.networkId, attempt);
                G.Log(TAG,"Wifi enable "+wifi.SSID + " "+result);

            }

          //  this.wifiManager.disconnect();
            //0this.wifiManager.reconnect();
      //      unbindService();
            connected = false;
            mConectionState=0;
            listener.wifiLinkDisconnected(this);
        }

    }

    public void SetInetAddress(String address){
        this.inetAddress = address;
    }

    public String GetInetAddress(){
        return this.inetAddress;
    }

    @Override
    public void sendFrame(byte[] frameData)
    {

    }

    @Override
    public long getNodeId(){
        return 0;
    }

    @Override
    public int getPriority(){
        return 0;

    }

    /***
     * Calling this method will aquire the lock on wifi. This is avoid wifi
     * from going to sleep as long as <code>releaseWifiLock</code> method is called.
     **/
    private void holdWifiLock() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if( mWifiLock == null )
            mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);

        mWifiLock.setReferenceCounted(false);

        if( !mWifiLock.isHeld() )
            mWifiLock.acquire();
    }

    /***
     * Calling this method will release if the lock is already help. After this method is called,
     * the Wifi on the device can goto sleep.
     **/
    private void releaseWifiLock() {

        if( mWifiLock == null )
            Log.w(TAG, "#releaseWifiLock mWifiLock was not created previously");

        if( mWifiLock != null && mWifiLock.isHeld() ){
            mWifiLock.release();
            //mWifiLock = null;
        }

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
                 //   G.Log(TAG,"ConnectionState: " + mConectionState);

                    String conStatus = "";
                    if(mConectionState == WifiLink.ConectionStateNONE) {
                        conStatus = "NONE";
                    }else if(mConectionState == WifiLink.ConectionStatePreConnecting) {
                        conStatus = "PreConnecting";
                    }else if(mConectionState == WifiLink.ConectionStateConnecting) {
                        conStatus = "Connecting";
                    }else if(mConectionState == WifiLink.ConectionStateConnected) {
                        conStatus = "Connected";
                    }else if(mConectionState == WifiLink.ConectionStateDisconnected) {
                        conStatus = "Disconnected";
                        G.Log(TAG,"Had connection "+hadConnection);
                        //wifiManager.removeNetwork(netId);
                        //mWifiDirectServiceDiscovery.start();
                        if(hadConnection)disconnect();

                    }
                    G.Log(TAG, "Status " + conStatus);

                }

                WifiInfo wiffo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                Log.d(TAG,"Wifiinfo "+wiffo);
                if(wiffo!=null&&mConectionState==ConectionStateConnected){
//                    Log.d(TAG,"SSID "+wiffo.getSSID() +" "+wifiConfig.BSSID + " "+ wifiConfig.SSID);

                    if(wiffo.getSSID().equals(wifiConfig.SSID)) {
                        G.Log(TAG, "Ip address: " + wiffo.getIpAddress());
                        G.Log(TAG, "Create face to " + inetAddress);
                        hadConnection=true;
                        G.Log(TAG, "Connected to " + wiffo.getSSID());
                        listener.wifiLinkConnected(that);

               //         sendServiceMessage(UbiCDNService.UBICDN_SERVICE_WIFI_CONNECTED);

                    /*} else {
                        that.wifiManager.reconnect();*/
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
     *//*
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
    }*/

    /**
     * Method that binds the current activity to the UbiCDN Service.
     */
    /*
    private void
    bindService() {
        if (!m_isServiceConnected) {
            // Bind to Service
            context.bindService(new Intent(context, UbiCDNService.class),
                    m_ServiceConnection, Context.BIND_AUTO_CREATE);
            G.Log("ServiceFragment::bindUbiCDNService()");
        }
    }*/

    /**
     * Method that unbinds the current activity from the UbiCDN Service.
     *//*
    private void
    unbindService() {
        if (m_isServiceConnected) {
            // Unbind from Service
            context.unbindService(m_ServiceConnection);
            m_isServiceConnected = false;

            G.Log("ServiceFragment::unbindUbiCDNService()");
        }
    }*/

    /**
     * Client Message Handler.
     *
     * This handler is used to handle messages that are being sent back
     * from the UbiCDN Service to the current application.
     *//*
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
    }*/

    /**
     * Client ServiceConnection to UbiCDN Service.
     *//*
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
    };*/
}
