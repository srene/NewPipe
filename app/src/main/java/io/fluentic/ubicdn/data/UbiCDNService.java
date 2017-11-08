package io.fluentic.ubicdn.data;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

//import net.grandcentrix.tray.AppPreferences;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.security.SecurityException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.fluentic.ubicdn.net.Discovery;
import io.fluentic.ubicdn.net.Link;
import io.fluentic.ubicdn.net.LinkListener;
import io.fluentic.ubicdn.net.bluetooth.BtLink;
import io.fluentic.ubicdn.net.bluetooth.BtServiceDiscovery;
import io.fluentic.ubicdn.net.wifi.*;
//import io.fluentic.ubicdn.ui.ServiceFragment;
import io.fluentic.ubicdn.util.Config;
import io.fluentic.ubicdn.util.G;
import io.fluentic.ubicdn.util.MyNfdc;
import io.fluentic.ubicdn.util.dispatch.DispatchQueue;

import static java.lang.Thread.sleep;

/**
 * Created by srenevic on 03/08/17.
 */

public class UbiCDNService extends Service implements OnInterestCallback, OnRegisterFailed, OnData, OnTimeout, LinkListener, WifiLinkListener {

    //Parameters initialization

    //Service type advertised in WiFi Direct
    public static final String SERVICE_INSTANCE = "_ubicdn";
    public static final String SERVICE_TYPE = "_ubicdn._tcp";

    //Connectivity classes
    UbiCDNService that = this;
    //WifiDirectServiceDiscovery mWifiDirectServiceDiscovery = null;
    WifiDirectHotSpot mWifiDirectHotSpot = null;
    Link mWifiLink = null, mBtLink = null;
    Discovery mBluetoothServiceDiscovery, mWifiDirectServiceDiscovery = null;
    DispatchQueue queue;
    //Handler messages
    /** Message to start UbiCDN Service */
    public static final int START_UBICDN_SERVICE = 1;

    /** Message to stop UbiCDN Service */
    public static final int STOP_UBICDN_SERVICE = 2;

    /** Message to indicate that UbiCDN Service is running */
    public static final int UBICDN_SERVICE_RUNNING = 3;

    /** Message to indicate that UbiCDN Service is not running */
    public static final int UBICDN_SERVICE_STOPPED = 4;

    /** Message to indicate that WifiDirect got disconnected */
    public static final int UBICDN_SERVICE_DISCONNECT = 5;

    /** Message to indicate that WifiDirect is connected */
    public static final int UBICDN_SERVICE_WIFI_CONNECTED = 6;

    /** Message to indicate that WifiDirect is connected */
    public static final int UBICDN_SERVICE_BT_CONNECTED= 7;

    /** Message to indicate that WifiDirect is connected */
    public static final int UBICDN_SERVICE_BT_CONNECTIONS = 8;

    /** debug tag */
    public static final String TAG = UbiCDNService.class.getName();

    //While true NDN face is processing events
    public boolean shouldStop=true;

    //Number of interests received
    public int interestCounter=0;

    //Keychain used to create faces
    public static KeyChain keyChain;

    //Database used to store video list information
    DatabaseHandler db;

    /** Messenger to handle messages that are passed to the NfdService */
    private Messenger m_ubicdnServiceMessenger = null;

    /** Flag that denotes if the NFD has been started */
    private boolean m_isUbiCDNStarted = false;

    private boolean m_isBtConnected = false;

    private boolean m_isConnected = false;
    private int     m_btConnections = 0;

    final Handler handler = new Handler();

    public int id;

    //Loading JNI libraries used to run NFD
    static {
        // At least on Galaxy S3 (4.1.1), all shared library dependencies that are located
        // in app's lib folder (not in /system/lib) need to be explicitly loaded.
        // The script https://gist.github.com/cawka/11fe9c23b7a13960330b can be used to
        // calculate proper dependency load list.
        // For example:
        //     cd app/src/main/libs/armeabi-v7a/
        //     bash android-shared-lib-dependencies.sh nfd-wrapper
        System.loadLibrary("crystax");
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("cryptopp_shared");
        System.loadLibrary("boost_system");
        System.loadLibrary("boost_filesystem");
        System.loadLibrary("boost_date_time");
        System.loadLibrary("boost_iostreams");
        System.loadLibrary("boost_program_options");
        System.loadLibrary("boost_chrono");
        System.loadLibrary("boost_random");
        System.loadLibrary("ndn-cxx");
        System.loadLibrary("boost_thread");
        System.loadLibrary("nfd-daemon");
        System.loadLibrary("nfd-wrapper");
  //      System.loadLibrary("netguard");
    }
    /**
     * Native API for starting the NFD.
     *
     * @param params NFD parameters.  Must include 'homePath' with absolute path of the home directory
     *               for the service (ContextWrapper.getFilesDir().getAbsolutePath())
     */
    public native static void
    startNfd(Map<String, String> params);

    /**
     * Native API for stopping the NFD.
     */
    public native static void
    stopNfd();

    public native static List<String>
    getNfdLogModules();


    @Override
    public IBinder onBind(Intent intent) {

        G.Log(TAG,"Service onBind");
        return m_ubicdnServiceMessenger.getBinder();

    }

    @Override
    public void onCreate() {
        G.Log("UbiCDNService::onCreate()");
        try{
            keyChain = buildTestKeyChain();
        }catch (SecurityException e){}
        m_ubicdnServiceMessenger = new Messenger(new UbiCDNService.UbiCDNServiceMessageHandler());
        db = new DatabaseHandler(this);

        long nodeId;
        do
        {
            nodeId = new Random().nextLong();
        } while (nodeId == 0);

        if(nodeId < 0)
            nodeId = -nodeId;
        this.queue = new DispatchQueue();
        mBluetoothServiceDiscovery = new BtServiceDiscovery(234235, nodeId, this, queue, this);
        mWifiDirectServiceDiscovery = new WifiDirectServiceDiscovery(this,  this);
        //mBtLink = new BtLink();
        //mWifiLink = new WifiLink();

    }


    @Override
    public void onDestroy() {

        G.Log("UbiCDNService::onDestroy()");
        serviceStopUbiCDN();
        stopSelf();
        m_ubicdnServiceMessenger = null;
        super.onDestroy();
        //this.unregisterReceiver(receiver);

    }


    /////////////////////////////////////////////////////////////////////////////

    /**
     * Thread safe way of starting the service and updating the
     * started flag.
     */
    private synchronized void
    serviceStartUbiCDN() {

        if (!m_isUbiCDNStarted) {
            m_isUbiCDNStarted = true;
            Random rn = new Random();
            id = rn.nextInt();
            G.Log(TAG,"Started");

            if(mWifiDirectHotSpot==null)
                mWifiDirectHotSpot = new WifiDirectHotSpot(this);

            ContentAdvertisement ca = new ContentAdvertisement();
            for(String content: db.getContentDownloaded())
            {
                G.Log("Content advertisement add element "+content);
                ca.addElement(content);
            }

            mWifiDirectHotSpot.Start(isSourceDevice(),id,ca);

            if(mWifiLink==null)
                mWifiLink = new WifiLink(this);

            G.Log(TAG,"GetPending "+db.getPendingCount());
            mWifiDirectServiceDiscovery.start(isSourceDevice(),id);
            mBluetoothServiceDiscovery.start(isSourceDevice(),id);

            //LocalBroadcastManager.getInstance(this).registerReceiver((mBRReceiver), filter);
            HashMap<String, String> params = new HashMap<>();
            params.put("homePath", getFilesDir().getAbsolutePath());
            Set<Map.Entry<String,String>> e = params.entrySet();

            startNfd(params);

            // Example how to retrieve all available NFD log modules
            List<String> modules = getNfdLogModules();
            for (String module : modules) {
                G.Log(module);
            }
            startService(new Intent(this, UbiCDNService.class));

            //ServiceSinkhole.start("prepared", this);

            if(isSourceDevice())registerPrefix();

           // transport.start();

            G.Log(TAG, "serviceStartUbiCDN()");
        } else {
            G.Log(TAG, "serviceStartUbiCDN(): UbiCDN Service already running!");
        }
    }

    /**
     * Thread safe way of stopping the service and updating the
     * started flag.
     */
    private synchronized void
    serviceStopUbiCDN() {
        if (m_isUbiCDNStarted) {
            m_isUbiCDNStarted = false;

            // TODO: Save NFD and NRD in memory data structures.
            stopNfd();

            /*if(isSourceDevice()){
                mWifiDirectHotSpot.Stop();
            } else {
                mWifiDirectHotSpot.Stop();
                mWifiLink.disconnect();
            }*/
            mWifiDirectHotSpot.Stop();
            if(!isSourceDevice())mWifiLink.disconnect();
            mWifiDirectServiceDiscovery.stop();
            mBluetoothServiceDiscovery.stop();
            //
            stopSelf();
           // ServiceSinkhole.stop("switch off", this, false);

            G.Log(TAG, "serviceStopUbiCDN()");
        }
    }

    private boolean isSourceDevice(){
        //Getting if is source device checkbox enabled from sharedpreferences
   //     final AppPreferences appPreferences = new AppPreferences(this); // this Preference comes for free from the library

   //     boolean value = appPreferences.getBoolean(ServiceFragment.PREF_UBICDN_SERVICE_SOURCE,false);

        //Log.d(TAG,"Source " + value);
        return true;
    }

    /* Creates the face towards the group owner and send interests to the NFD daemon for
       any pending video not received once connected to the WifiDirect network (WifiLink succeed)
      */
    private void createFaceandSend() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(5000);
                    MyNfdc nfdc = new MyNfdc();
                    int faceId = 0;
                    Log.d(TAG,"Create face");
                    faceId = nfdc.faceCreate("tcp://192.168.49.1");
                    //if(!info.isGroupOwner)faceId = nfdc.faceCreate("udp4://"+info.groupOwnerAddress.getHostAddress());
                    //        else faceId = nfdc.faceCreate("udp://"+app.getMyAddress());
                    Log.d(TAG,"Register prefix");
                    nfdc.ribRegisterPrefix(new Name("/ubicdn/video/"), faceId, 0, true, false);
                    nfdc.shutdown();

                    Log.d(TAG,"Localhost face");

                    Face mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                    Log.d(TAG,"Background request");

                    for(String content: db.getPendingContent())
                    {
                        G.Log(TAG,"Pending content "+content);
                        final Name requestName = new Name("/ubicdn/video/"+content);
                        Interest interest = new Interest(requestName);
                        interest.setInterestLifetimeMilliseconds(50000);
                        mFace.expressInterest(interest,that,that);
                        shouldStop=false;
                        interestCounter++;
                    }
                    if(interestCounter==0)mWifiLink.disconnect();
                    while (!shouldStop) {
                        mFace.processEvents();
                    }

                } catch (Exception e) {
                    Log.d(TAG, "Error " + e);
                }

            }
        }).start();
    }

    /**
     * Message handler for the the ubiCDN Service.
     */
    private class UbiCDNServiceMessageHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case UbiCDNService.START_UBICDN_SERVICE:
                   // G.Log(TAG,"Non source start service");
                    //source=false;
                    serviceStartUbiCDN();
                    replyToClient(message, UbiCDNService.UBICDN_SERVICE_RUNNING);
                    break;

                case UbiCDNService.STOP_UBICDN_SERVICE:
                    serviceStopUbiCDN ();
                    replyToClient(message, UbiCDNService.UBICDN_SERVICE_STOPPED);
                    break;

                case UbiCDNService.UBICDN_SERVICE_WIFI_CONNECTED:
                    G.Log(TAG,"Wifi connection completed");
                   // createFaceandSend();
                    break;

                case UbiCDNService.UBICDN_SERVICE_BT_CONNECTED:
                    replyToClient(message,m_isBtConnected ? 0:1 );
                    break;

                case UbiCDNService.UBICDN_SERVICE_BT_CONNECTIONS:
                    replyToClient(message,m_btConnections);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }

        private void
        replyToClient(Message message, int replyMessage) {
            try {
                message.replyTo.send(Message.obtain(null, replyMessage));
            } catch (RemoteException e) {
                // Nothing to do here; It means that client end has been terminated.
            }
        }
    }

    //For any interest received sends the corresponding video back if found in local storage
    public void onInterest(Name name, Interest interest, Face face, long l, InterestFilter filter) {

        // /todo check if the file exists first
        try {
            G.Log("Interest received "+interest.getName());
            String filename = interest.getName().get(2).toEscapedString() + ".mp4";
            File f = new File(getFilesDir() + "/" + filename);
            FileInputStream fis = new FileInputStream(f);
            byte[] bytes = new byte[(int) f.length()];
            try {
                fis.read(bytes);
            } catch (IOException e) {
                G.Log(TAG, e.getMessage());
            }

            Data data = new Data(interest.getName());
            data.setContent(new Blob(bytes));
            G.Log("Get file " + data.getContent().size());
            face.putData(data);
        } catch (FileNotFoundException e) {
            G.Log(TAG, e.getMessage());
        } catch (IOException e){
            G.Log(TAG,e.getMessage());
        }

    }


    //For any data received (video file) save it in the local storage and update the video list db
    public void onData(Interest interest, Data data)
    {
        G.Log("File received "+data.getName() +" "+data.getContent().size());
        Blob b = data.getContent();
        b.getImmutableArray();

        try {
            File f = new File(getFilesDir() + "/" + data.getName().get(2).toEscapedString() + ".mp4");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(b.getImmutableArray());
        }catch (FileNotFoundException e){G.Log(TAG,e.getMessage());
        }catch (IOException e){G.Log(TAG,e.getMessage());}

        db.setContentDownloaded(data.getName().get(2).toEscapedString());

        interestCounter--;
        G.Log("Interest counter "+interestCounter);
        if(interestCounter==0)
        {
            shouldStop=true;
            //mWifiDirectServiceDiscovery.Disconnect();
            //mWifiLink.disconnect();
            serviceStopUbiCDN();
            serviceStartUbiCDN();
        }

    }

    public void onTimeout(Interest interest)
    {
        G.Log("File timeout "+interest.getName());
        interestCounter--;
        if(interestCounter==0)
        {
            shouldStop=true;
            //mWifiDirectServiceDiscovery.Disconnect();
           // mWifiLink.disconnect();
            serviceStopUbiCDN();
            serviceStartUbiCDN();
        }
    }

    public void onRegisterFailed(Name name){
        Log.e(TAG, "Failed to register the data");

    }

    private void registerPrefix(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(1000);
                    Log.i(TAG, "Start produce service thread");

                    final ArrayList<String> prefixData = new ArrayList<>();

                    final Face mFace = new Face("localhost");
                    mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

                    // Register the prefix with the device's address
                    mFace.registerPrefix(new Name("/ubicdn/video/"),that, that);

                    while (true) {
                        // Log.i(TAG, "Service is running");
                        mFace.processEvents();
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }

        }).start();
    }


    @Override
    public void btLinkConnected(Link link)
    {
        m_isBtConnected=true;
        m_btConnections++;
        Log.d(TAG,"btLinkConnected");
        /*if(isSourceDevice()){
            String str = "NI:"+mWifiDirectHotSpot.getNetworkName()+":"+mWifiDirectHotSpot.getPassphrase();
            Log.d(TAG,"Send frame "+str);
            if(!mWifiDirectHotSpot.getNetworkName().equals("")&&!mWifiDirectHotSpot.getPassphrase().equals(""))
                link.sendFrame(str.getBytes());
        } else {
            link.disconnect();
        }*/

        if(isSourceDevice()){
            String str = "source:"+id+":"+mWifiDirectHotSpot.getNetworkName()+":"+mWifiDirectHotSpot.getPassphrase();
            Log.d(TAG,"Send frame "+str);
            link.sendFrame(str.getBytes());
        } else if(mWifiDirectHotSpot.isRunning()){
            String str = "client:"+id+":"+mWifiDirectHotSpot.getNetworkName()+":"+mWifiDirectHotSpot.getPassphrase();
            Log.d(TAG,"Send frame "+str);
            link.sendFrame(str.getBytes());
        }

    } // btLinkConnected()

    @Override
    public void btLinkDisconnected(Link link)
    {
        m_isBtConnected=false;
        Log.d(TAG,"btLinkDisconnected");
    } // btLinkDisconnected()

    @Override
    public void btLinkDidReceiveFrame(Link link, byte[] frameData)
    {
        Log.d(TAG, "btLinkDidReceiveFrame ");
     //   if(link!=null)
     //   {
        Log.d(TAG, "Frame received " + new String(frameData));
        String frame = new String(frameData);
        String[] separated = frame.split(":");

        if(!separated[2].equals("")&&!separated[3].equals("")&&!isSourceDevice()) {
            if ((separated[0].equals("client") && Integer.parseInt(separated[1]) > id)||separated[0].equals("source")) {
                mWifiDirectServiceDiscovery.stop();
                mBluetoothServiceDiscovery.stop();
                mWifiDirectHotSpot.Stop();
                mWifiLink.connect(separated[2], separated[3]);
            }
        }
        /*    if (!separated[2].equals("") && !separated[3].equals("")) {
                mWifiDirectServiceDiscovery.stop();
                mBluetoothServiceDiscovery.stop();
                if(!isSourceDevice())mWifiLink.connect(separated[2], separated[3]);
            }*/
        if(link!=null)
        {
            BtLink btlink = (BtLink) link;
            btlink.notifyDisconnect();
        }
        Log.d(TAG, "Separated " + separated[2] + " " + separated[3]);
      //  }
    } // btLinkDidReceiveFrame


    @Override
    public void wifiLinkConnected(Link link)
    {
        Log.d(TAG,"wifiLinkConnected");
        if(!m_isConnected) {
            m_isConnected = true;
       /* new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(50000);
                    mWifiLink.disconnect();
                    mBluetoothServiceDiscovery.start();
                    mWifiDirectServiceDiscovery.start();
                } catch (Exception e) {}

            }
        }).start();*/
            createFaceandSend();
        }
    } // btLinkConnected()

    @Override
    public void wifiLinkDisconnected(Link link)
    {
        Log.d(TAG,"wifiLinkDisconnected");
        m_isConnected = false;
       /* mWifiDirectHotSpot.Stop();
        mWifiDirectHotSpot.Start();
        mWifiDirectServiceDiscovery.stop();
        mWifiDirectServiceDiscovery.start(isSourceDevice(),id);
        mBluetoothServiceDiscovery.stop();
        mBluetoothServiceDiscovery.start(isSourceDevice(),id);*/
        serviceStopUbiCDN();
        serviceStartUbiCDN();
    } // btLinkDisconnected()


    @Override
    public void peerDiscovered(String name, String address)
    {
        Log.d(TAG,"Discovered "+name+" "+address);
        if(name.contains("source"))
        {
            mWifiDirectHotSpot.Stop();
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    if(m_isUbiCDNStarted) {
                        ContentAdvertisement ca = new ContentAdvertisement();
                        mWifiDirectHotSpot.Start(isSourceDevice(), id, ca);
                    }
                }
            }, Config.sourceDeviceWaitingTime);
        }
    }

    public static KeyChain buildTestKeyChain() throws SecurityException {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        KeyChain keyChain = new KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;
    }



}