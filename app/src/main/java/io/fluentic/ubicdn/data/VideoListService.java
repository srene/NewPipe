package io.fluentic.ubicdn.data;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;

//import net.grandcentrix.tray.AppPreferences;

import java.io.File;

//import io.fluentic.ubicdn.ui.ServiceFragment;
import io.fluentic.ubicdn.util.G;


/**
 * Created by srenevic on 03/08/17.
 */

public class VideoListService extends Service {

    public static final String DOWNLOAD_COMPLETED = "download_completed";
    public static final String DOWNLOAD_ERROR = "download_error";
    public static final String NEW_VIDEO_AVAILABLE = "new_video_available";
    private SharedPreferences m_sharedPreferences;

    //private boolean source=false;
    IntentFilter filter;
   // public HashMap<String,String> listVideos;
    BroadcastReceiver mBroadcastReceiver;
    /** debug tag */
    public static final String TAG = VideoListService.class.getName();
    IBinder mBinder = new LocalBinder();
    private Messenger m_videListServiceMessenger = null;

    public static final int START_VIDEOLIST_SERVICE=0;
    public static final int VIDEOLIST_SOURCE=1;
    public static final int VIDEOLIST_NOSOURCE=2;

    DatabaseHandler db;
    @Override
    public IBinder onBind(Intent intent) {

        G.Log(TAG,"Service onBind");
        //return m_videListServiceMessenger.getBinder();
        return mBinder;

    }
     public class LocalBinder extends Binder {
        public VideoListService getServerInstance() {
            return VideoListService.this;
        }
    }

    @Override
    public void onCreate() {
        G.Log(TAG,"onCreate()");
        m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        db = new DatabaseHandler(this);
        // m_videListServiceMessenger = new Messenger(new VideoListService.VideoListServiceMessageHandler());


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        G.Log("Start command");
        startReceiver();
        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {

        G.Log(TAG,"onDestroy()");
        super.onDestroy();
        m_videListServiceMessenger = null;
        this.unregisterReceiver(mBroadcastReceiver);


    }

    public void startVideoListService()
    {
        G.Log("Start");
        startService(new Intent(this, VideoListService.class));
    }
    public void startReceiver()
    {
        G.Log(TAG,"startReceiver()");
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                G.Log(TAG,"onReceive:" + intent);
                G.Log(TAG, "content Action: " + intent.getAction());
                G.Log(TAG, "NEW_VIDEO: " + NotificationService.NEW_VIDEO);
                //hideProgressDialog();

                switch (intent.getAction()) {
                    case NotificationService.NEW_VIDEO:
                        // Get number of bytes downloaded
                        String title = intent.getStringExtra("title");
                        String desc = intent.getStringExtra("desc");
                        String url = intent.getStringExtra("url");
                        G.Log(TAG, "title: " + title + " URL: " + url);
                        G.Log(TAG,"Download "+url+ " desc " + desc + " source "+isSourceDevice());
                        addVideo(title,desc,url);
                        if(isSourceDevice()) download(title,url);
                        break;

                }
            }
        };
        this.registerReceiver(mBroadcastReceiver, NotificationService.getIntentFilter());
       // manager.registerReceiver(mBroadcastReceiver,VideoListService.getIntentFilter());
    }

    private boolean isSourceDevice(){
        //Getting if is source device checkbox enabled from sharedpreferences
       /* final AppPreferences appPreferences = new AppPreferences(this); // this Preference comes for free from the library

        boolean value = appPreferences.getBoolean(ServiceFragment.PREF_UBICDN_SERVICE_SOURCE,false);

        Log.d(TAG,"Source " + value);*/
        return true;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }


    public void addVideo(String title, String desc, String url)
    {
        G.Log(TAG,"Title "+title+ " url "+url);
       // listVideos.put(title,url);
        boolean result=db.addContent(new Content(title,desc,url));
        /**
         * CRUD Operations
         * */
        // Inserting Contacts
        if(result) {
            G.Log("Insert: ", "Inserting " + title);
            //db.addContact(new Contact("Ravi", "9100000000"));
            Intent broadcast = new Intent(NEW_VIDEO_AVAILABLE);
            sendBroadcast(broadcast);
        }
    }

   /* public void setSource(boolean source){
        this.source = source;
    }*/
    public void download(final String title, String url){
        FirebaseApp.initializeApp(this);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        G.Log(TAG, "Trying to download " + title + " from URL: " + url);
        //String url = listVideos.get(title);
        //G.Log(TAG,"Title "+title+ " url "+url);
        try {
            File localFile = new File(getApplicationContext().getFilesDir(),title+".mp4");
            G.Log(TAG, "Local file " + getApplicationContext().getFilesDir(),title+".mp4");

            //File localFile = File.createTempFile("video", ".mp4");
           // File.
            // storageRef = storage.getReferenceFromUrl(url).getFile(localFile);
            //G.Log(TAG,"Title "+title+ " url "+url);

            // Get the task monitoring the download
            FileDownloadTask task = storage.getReferenceFromUrl(url).getFile(localFile);
            //G.Log(TAG,"Title "+title+ " url "+url);

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot state) {
                    G.Log(TAG, "file created!, title: " + title);
                    Intent broadcast = new Intent(DOWNLOAD_COMPLETED);
                    sendBroadcast(broadcast);
                    db.setContentDownloaded(title);
                   // state.getStorage();
                    // storageRef.getFile();
                    //  handleSuccess(state); //call a user defined function to handle the event.
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, "Cannot download file");
                }
            });


        }catch (Exception e){}

        /*File dir = getFilesDir();
        File[] subFiles = dir.listFiles();

        if (subFiles != null)
        {
            for (File file : subFiles)
            {
                G.Log(TAG,file.getName() + " " + Integer.parseInt(String.valueOf(file.length()/1024)));
            }
        }*/
    }



        /*videoRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "file created!");
                Intent broadcast = new Intent(DOWNLOAD_COMPLETED);
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(broadcast);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "Cannot download file");
            }
        });
    }*/

    /**
     * Message handler for the the NFD Service.
     */
    /*private class VideoListServiceMessageHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case VideoListService.START_VIDEOLIST_SERVICE:
                    startVideoListService();
                    break;
                case VideoListService.VIDEOLIST_SOURCE:
                    source = true;
                    G.Log("Source "+source);
                    break;
                case VideoListService.VIDEOLIST_NOSOURCE:
                    source = false;
                    G.Log("Source "+source);

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
    }*/

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DOWNLOAD_COMPLETED);
        filter.addAction(DOWNLOAD_ERROR);
        filter.addAction(NEW_VIDEO_AVAILABLE);
        return filter;
    }

}