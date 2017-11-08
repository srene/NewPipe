package io.fluentic.ubicdn.net.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.fluentic.ubicdn.data.ContentAdvertisement;
import io.fluentic.ubicdn.data.UbiCDNService;
import io.fluentic.ubicdn.util.G;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION;


/**
 * Created by srenevic on 03/08/17.
 */
public class WifiDirectHotSpot implements ConnectionInfoListener,ChannelListener,GroupInfoListener{

    WifiDirectHotSpot that = this;
    Context context;

    private WifiP2pManager p2p;
    private Channel channel;

    String mNetworkName = "";
    String mPassphrase = "";
    String mInetAddress = "";

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private boolean source;
    private int id;

    public static final String TAG = WifiDirectHotSpot.class.getName();

    boolean started;

    ContentAdvertisement ca;

    public WifiDirectHotSpot(Context Context) {
        this.context = Context;
    }

    public void Start(boolean source, int id, ContentAdvertisement ca) {
        if(!started) {
            this.source = source;
            this.id = id;
            started = true;
            this.ca = ca;
            p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);

            if (p2p == null) {
                G.Log(TAG, "This device does not support Wi-Fi Direct");
            } else {

                channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
                receiver = new AccessPointReceiver();
                filter = new IntentFilter();
                filter.addAction(WIFI_P2P_STATE_CHANGED_ACTION);
                filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
                this.context.registerReceiver(receiver, filter);

                p2p.createGroup(channel, new ActionListener() {
                    public void onSuccess() {
                        G.Log(TAG, "Creating Local Group ");
                    }

                    public void onFailure(int reason) {
                        G.Log(TAG, "Local Group failed, error code " + reason);
                    }
                });
            }

        }
    }

    public void Stop() {
        if(started)
        {
            started=false;
            this.context.unregisterReceiver(receiver);
            stopLocalServices();
            removeGroup();
        }

    }

    public boolean isRunning()
    {
        return started;
    }

    public void removeGroup() {
        p2p.removeGroup(channel,new ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cleared Local Group ");
            }

            public void onFailure(int reason) {
                G.Log(TAG,"Clearing Local Group failed, error code " + reason);
            }
        });
    }

    public String getNetworkName(){
        return mNetworkName;
    }

    public String getPassphrase(){
        return mPassphrase;
    }
    @Override
    public void onChannelDisconnected() {
        // see how we could avoid looping
   //     p2p = (WifiP2pManager) this.context.getSystemService(this.context.WIFI_P2P_SERVICE);
   //     channel = p2p.initialize(this.context, this.context.getMainLooper(), this);
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
       // G.Log(TAG,"onGroupInfoAvailable "+group.getNetworkName());
       // SetNetworkName("ubicdn-source",group);
        try {
            Collection<WifiP2pDevice> devlist = group.getClientList();

            int numm = 0;
            for (WifiP2pDevice peer : group.getClientList()) {
                numm++;
                G.Log(TAG,"Client " + numm + " : "  + peer.deviceName + " " + peer.deviceAddress);
            }

            if(mNetworkName.equals(group.getNetworkName()) && mPassphrase.equals(group.getPassphrase())){
                G.Log(TAG,"Already have local service for " + mNetworkName + " ," + mPassphrase);
            }else {

                mNetworkName = group.getNetworkName();
                mPassphrase = group.getPassphrase();
                String instance="";
                if(source)
                    instance="source:"+id+":"+group.getNetworkName()+":"+group.getPassphrase()+":"+mInetAddress;
                else
                    instance="client:"+id+":"+group.getNetworkName()+":"+group.getPassphrase()+":"+mInetAddress;
                //startLocalService("NI:" + group.getNetworkName() + ":" + group.getPassphrase() + ":" + mInetAddress);
                startLocalService(instance);
            }
        } catch(Exception e) {
            G.Log(TAG,"onGroupInfoAvailable, error: " + e.toString());
        }
    }

    private void startLocalService(String instance) {

        Map<String, String> record = new HashMap<String, String>();
        record.put("available", "visible");
        G.Log(TAG,"Filter "+ca.getFilter()+"size "+ca.getFilter().getBytes().length);
        record.put("filter",ca.getFilter());

        final WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(UbiCDNService.SERVICE_INSTANCE,UbiCDNService.SERVICE_TYPE, record);
        G.Log(TAG,"Add local service :" + instance);

        p2p.clearLocalServices(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                p2p.addLocalService(channel, service,
                        new ActionListener() {

                            @Override
                            public void onSuccess() {
                                // service broadcasting started

                                //   mServiceBroadcastingHandler.postDelayed(mServiceBroadcastingRunnable, SERVICE_BROADCASTING_INTERVAL);
                            }

                            @Override
                            public void onFailure(int error) {
                                // react to failure of adding the local service
                            }
                        });
            }

            @Override
            public void onFailure(int error) {
                // react to failure of clearing the local services
            }
        });
    }

    private void stopLocalServices() {
        mNetworkName = "";
        mPassphrase = "";

        p2p.clearLocalServices(channel, new ActionListener() {
            public void onSuccess() {
                G.Log(TAG,"Cleared local services");
            }

            public void onFailure(int reason) {
                G.Log(TAG,"Cleared local services");
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        try {
            if (info.isGroupOwner) {
                mInetAddress = info.groupOwnerAddress.getHostAddress();
                G.Log(TAG, "inet address" + mInetAddress);
                p2p.requestGroupInfo(channel,this);
            } else {
                G.Log(TAG,"we are client !! group owner address is: " + info.groupOwnerAddress.getHostAddress());
            }
        } catch(Exception e) {
            G.Log(TAG,"onConnectionInfoAvailable, error: " + e.toString());
        }
    }

    /*public void SetNetworkName(String name,WifiP2pGroup group)
    {
        G.Log(TAG,"Set name "+name);
        try {
            Method setNetworkName = group.getClass().getMethod("setPassphrase", new Class[]{String.class});
            setNetworkName.setAccessible(true);
            setNetworkName.invoke(group, name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }*/
    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // startLocalService();
                } else {
                    //stopLocalService();
                    //Todo: Add the state monitoring in higher level, stop & re-start all when happening
                }
            }  else if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    //debug_print("We are connected, will check info now");
                    G.Log(TAG,"We are connected, will check info now");
                    p2p.requestConnectionInfo(channel, that);
                } else{
                    //debug_print("We are DIS-connected");
                    G.Log(TAG,"We are DIS-connected");
                }
            }
        }
    }


}
