package io.fluentic.ubicdn.data;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

//import org.apache.http.conn.util.InetAddressUtils;

/**
 * Created by srenevic on 17/08/17.
 */
public class NotificationService extends FirebaseMessagingService{

    private static final String TAG = "NotificationService";

    /** Actions **/
    public static final String NEW_VIDEO = "action_download";
   // public static final String DOWNLOAD_COMPLETED = "download_completed";
   // public static final String DOWNLOAD_ERROR = "download_error";


    public class LocalBinder extends Binder {
        public NotificationService getServerInstance() {
            return NotificationService.this;
        }
    }
    //private MyListFragment m_VideoList;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "Got a message from: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        String video="";
        String url="";
        String desc="";
        if(remoteMessage.getNotification() != null) {
            video = remoteMessage.getNotification().getTitle();
            desc = remoteMessage.getNotification().getBody();
        }
        if (remoteMessage.getData().size() > 0) {
            url = remoteMessage.getData().get("url");

        }

        Log.d(TAG,"Title:"+video+" desc:"+desc+" url:"+url);

        if(!video.equals("")&&!url.equals("")&&!desc.equals("")) {
            String action = NEW_VIDEO;

            Intent broadcast = new Intent(action)
                    .putExtra("title", video)
                    .putExtra("desc", desc)
                    .putExtra("url", url);
            sendBroadcast(broadcast);
        }
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NEW_VIDEO);
        return filter;
    }


}
